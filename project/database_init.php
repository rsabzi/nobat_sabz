<?php
/**
 * فایل initialization دیتابیس
 * این فایل تمام جداول مورد نیاز را خودکار بررسی و ایجاد می‌کند
 * مستقلانه اجرا: php project/database_init.php
 * یا خودکار: require_once در config.php
 */

if (!function_exists('initDatabase')) {
    function columnExists($pdo, $table, $column) {
        try {
            $stmt = $pdo->query("SHOW COLUMNS FROM `$table` LIKE '$column'");
            return $stmt->rowCount() > 0;
        } catch (Exception $e) {
            return false;
        }
    }

    function tableExists($pdo, $table) {
        try {
            $stmt = $pdo->query("SHOW TABLES LIKE '$table'");
            return $stmt->rowCount() > 0;
        } catch (Exception $e) {
            return false;
        }
    }

    function ensureCustomerCreditProcedure($pdo) {
        try {
            $stmt = $pdo->query("SHOW PROCEDURE STATUS WHERE Name = 'UpdateCustomerCredit' LIMIT 1");
            if ($stmt && $stmt->fetch()) {
                return true;
            }
        } catch (Exception $e) {
            // fall through to creation
        }

        try {
            $pdo->exec("DROP PROCEDURE IF EXISTS `UpdateCustomerCredit`;
                CREATE PROCEDURE `UpdateCustomerCredit`(
                    IN p_user_id INT,
                    IN p_credit_change INT,
                    IN p_change_type ENUM('attendance','cancellation','no_show','manual_adjustment'),
                    IN p_reason VARCHAR(255),
                    IN p_booking_id INT DEFAULT NULL,
                    IN p_admin_id INT DEFAULT NULL,
                    IN p_business_id INT DEFAULT NULL
                )
                BEGIN
                    DECLARE v_current_credit INT DEFAULT 0;
                    DECLARE v_new_credit INT DEFAULT 0;

                    SELECT `current_credit` INTO v_current_credit
                    FROM `customer_credits`
                    WHERE `user_id` = p_user_id;

                    SET v_new_credit = v_current_credit + p_credit_change;
                    IF v_new_credit < 0 THEN
                        SET v_new_credit = 0;
                    END IF;

                    UPDATE `customer_credits`
                    SET `current_credit` = v_new_credit,
                        `total_earned` = `total_earned` + CASE WHEN p_credit_change > 0 THEN p_credit_change ELSE 0 END,
                        `total_deducted` = `total_deducted` + CASE WHEN p_credit_change < 0 THEN ABS(p_credit_change) ELSE 0 END,
                        `last_updated` = CURRENT_TIMESTAMP
                    WHERE `user_id` = p_user_id;

                    INSERT INTO `credit_history` (
                        `user_id`, `booking_id`, `change_type`, `credit_change`,
                        `credit_before`, `credit_after`, `reason`, `admin_id`, `business_id`
                    ) VALUES (
                        p_user_id, p_booking_id, p_change_type, p_credit_change,
                        v_current_credit, v_new_credit, p_reason, p_admin_id, p_business_id
                    );
                END");
            return true;
        } catch (Exception $e) {
            error_log('Failed to create UpdateCustomerCredit procedure: ' . $e->getMessage());
            return false;
        }
    }

    function initDatabase($pdo, $verbose = false) {
        if (!$pdo) {
            if ($verbose) echo "❌ خطا: اتصال به دیتابیس موجود نیست\n";
            return false;
        }

        if ($verbose) echo "🔍 شروع بررسی ساختار دیتابیس...\n\n";

        try {
            // =============== جدول businesses ===============
            if ($verbose) echo "📋 بررسی جدول businesses...\n";
            
            if (!tableExists($pdo, 'businesses')) {
                if ($verbose) echo "❌ خطا: جدول businesses وجود ندارد. لطفاً create_tables.sql را اجرا کنید.\n";
                return false;
            }
            
            $changes = [];
            
            // بررسی و اضافه کردن marketer_id
            if (!columnExists($pdo, 'businesses', 'marketer_id')) {
                if ($verbose) echo "  🔄 اضافه کردن ستون marketer_id...\n";
                $pdo->exec("ALTER TABLE businesses ADD COLUMN marketer_id INT(11) DEFAULT NULL COMMENT 'شناسه بازاریاب' AFTER advance_booking_days");
                $changes[] = "marketer_id";
                if ($verbose) echo "  ✅ ستون marketer_id اضافه شد\n";
            }
            
            // بررسی و اضافه کردن free_reservation_tokens
            if (!columnExists($pdo, 'businesses', 'free_reservation_tokens')) {
                if ($verbose) echo "  🔄 اضافه کردن ستون free_reservation_tokens...\n";
                $pdo->exec("ALTER TABLE businesses ADD COLUMN free_reservation_tokens INT(11) DEFAULT 0 COMMENT 'تعداد رزروهای رایگان' AFTER marketer_id");
                $changes[] = "free_reservation_tokens";
                if ($verbose) echo "  ✅ ستون free_reservation_tokens اضافه شد\n";
            }
            
            // بررسی و اضافه کردن link_alias
            if (!columnExists($pdo, 'businesses', 'link_alias')) {
                if ($verbose) echo "  🔄 اضافه کردن ستون link_alias...\n";
                $pdo->exec("ALTER TABLE businesses ADD COLUMN link_alias VARCHAR(100) UNIQUE COMMENT 'نام اختصاری برای لینک' AFTER name");
                $changes[] = "link_alias";
                if ($verbose) echo "  ✅ ستون link_alias اضافه شد\n";
            }
            
            // بررسی و اضافه کردن subscription_plan_id
            if (!columnExists($pdo, 'businesses', 'subscription_plan_id')) {
                if ($verbose) echo "  🔄 اضافه کردن ستون subscription_plan_id...\n";
                $pdo->exec("ALTER TABLE businesses ADD COLUMN subscription_plan_id INT DEFAULT NULL COMMENT 'شناسه پلن اشتراک' AFTER free_reservation_tokens");
                $changes[] = "subscription_plan_id";
                if ($verbose) echo "  ✅ ستون subscription_plan_id اضافه شد\n";
            }
            
            // بررسی و اضافه کردن subscription_expiry_date
            if (!columnExists($pdo, 'businesses', 'subscription_expiry_date')) {
                if ($verbose) echo "  🔄 اضافه کردن ستون subscription_expiry_date...\n";
                $pdo->exec("ALTER TABLE businesses ADD COLUMN subscription_expiry_date DATE DEFAULT NULL COMMENT 'تاریخ انقضای اشتراک' AFTER subscription_plan_id");
                $changes[] = "subscription_expiry_date";
                if ($verbose) echo "  ✅ ستون subscription_expiry_date اضافه شد\n";
            }
            
            if (empty($changes)) {
                if ($verbose) echo "  ✓ تمام ستون‌های مورد نیاز وجود دارند\n";
            } else {
                if ($verbose) echo "  ✅ " . count($changes) . " ستون جدید اضافه شد\n";
            }
            
            // =============== جدول marketers ===============
            if ($verbose) echo "\n📋 بررسی جدول marketers...\n";
            
            if (!tableExists($pdo, 'marketers')) {
                if ($verbose) echo "  🔄 ایجاد جدول marketers...\n";
                $pdo->exec("
                    CREATE TABLE IF NOT EXISTS `marketers` (
                        `id` INT(11) PRIMARY KEY AUTO_INCREMENT,
                        `user_id` INT(11) NOT NULL,
                        `marketer_code` VARCHAR(50) UNIQUE,
                        `total_businesses` INT(11) DEFAULT 0,
                        `total_bookings` INT(11) DEFAULT 0,
                        `commission_rate` DECIMAL(5,2) DEFAULT 10.00,
                        `total_commission` DECIMAL(12,2) DEFAULT 0.00,
                        `is_active` TINYINT(1) DEFAULT 1,
                        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        UNIQUE KEY (marketer_code)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                ");
                if ($verbose) echo "  ✅ جدول marketers ایجاد شد\n";
            } else {
                if ($verbose) echo "  ✓ جدول marketers وجود دارد\n";
            }
            
            // =============== جدول user_token_grants ===============
            if ($verbose) echo "\n📋 بررسی جدول user_token_grants...\n";
            
            if (!tableExists($pdo, 'user_token_grants')) {
                if ($verbose) echo "  🔄 ایجاد جدول user_token_grants...\n";
                $pdo->exec("
                    CREATE TABLE IF NOT EXISTS `user_token_grants` (
                        `id` INT(11) PRIMARY KEY AUTO_INCREMENT,
                        `user_id` INT(11) NOT NULL,
                        `tokens_granted` INT(11) DEFAULT 0,
                        `business_id` INT(11),
                        `notes` TEXT,
                        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE SET NULL,
                        KEY (user_id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                ");
                if ($verbose) echo "  ✅ جدول user_token_grants ایجاد شد\n";
            } else {
                if ($verbose) echo "  ✓ جدول user_token_grants وجود دارد\n";
            }
            
            // =============== جدول BusinessReferrals ===============
            if ($verbose) echo "\n📋 بررسی جدول BusinessReferrals...\n";
            
            if (!tableExists($pdo, 'BusinessReferrals')) {
                if ($verbose) echo "  🔄 ایجاد جدول BusinessReferrals...\n";
                $pdo->exec("
                    CREATE TABLE IF NOT EXISTS `BusinessReferrals` (
                        `id` INT(11) PRIMARY KEY AUTO_INCREMENT,
                        `referrer_business_id` INT(11) NOT NULL,
                        `referred_business_id` INT(11) NOT NULL,
                        `reward_tokens_granted` INT(11) DEFAULT 0,
                        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (referrer_business_id) REFERENCES businesses(id) ON DELETE CASCADE,
                        FOREIGN KEY (referred_business_id) REFERENCES businesses(id) ON DELETE CASCADE,
                        UNIQUE KEY (referrer_business_id, referred_business_id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                ");
                if ($verbose) echo "  ✅ جدول BusinessReferrals ایجاد شد\n";
            } else {
                if ($verbose) echo "  ✓ جدول BusinessReferrals وجود دارد\n";
            }
            
            // =============== جدول services ===============
            if ($verbose) echo "\n📋 بررسی جدول services...\n";
            
            if (!tableExists($pdo, 'services')) {
                if ($verbose) echo "  🔄 ایجاد جدول services...\n";
                $pdo->exec("
                    CREATE TABLE IF NOT EXISTS `services` (
                        `id` INT(11) PRIMARY KEY AUTO_INCREMENT,
                        `business_id` INT(11) NOT NULL,
                        `name` VARCHAR(255) NOT NULL,
                        `description` TEXT,
                        `price` DECIMAL(12,2) DEFAULT 0,
                        `duration_minutes` INT(11) DEFAULT 30,
                        `is_active` TINYINT(1) DEFAULT 1,
                        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
                        KEY (business_id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                ");
                if ($verbose) echo "  ✅ جدول services ایجاد شد\n";
            } else {
                if ($verbose) echo "  ✓ جدول services وجود دارد\n";
            }
            
            // =============== جدول bookings ===============
            if ($verbose) echo "\n📋 بررسی جدول bookings...\n";
            
            if (!tableExists($pdo, 'bookings')) {
                if ($verbose) echo "  🔄 ایجاد جدول bookings...\n";
                $pdo->exec("
                    CREATE TABLE IF NOT EXISTS `bookings` (
                        `id` INT(11) PRIMARY KEY AUTO_INCREMENT,
                        `business_id` INT(11) NOT NULL,
                        `user_id` INT(11),
                        `service_id` INT(11),
                        `booking_date` DATE NOT NULL,
                        `start_time` TIME NOT NULL,
                        `end_time` TIME,
                        `customer_name` VARCHAR(255),
                        `customer_phone` VARCHAR(20),
                        `status` VARCHAR(50) DEFAULT 'pending',
                        `notes` TEXT,
                        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
                        FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE SET NULL,
                        KEY (booking_date),
                        KEY (status)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                ");
                if ($verbose) echo "  ✅ جدول bookings ایجاد شد\n";
            } else {
                if ($verbose) echo "  ✓ جدول bookings وجود دارد\n";
            }
            
            // =============== جدول customer_credits ===============
            if ($verbose) echo "\n📋 بررسی جدول customer_credits...\n";
            
            if (!tableExists($pdo, 'customer_credits')) {
                if ($verbose) echo "  🔄 ایجاد جدول customer_credits...\n";
                $pdo->exec("
                    CREATE TABLE IF NOT EXISTS `customer_credits` (
                        `id` INT(11) PRIMARY KEY AUTO_INCREMENT,
                        `user_id` INT(11) NOT NULL,
                        `current_credit` INT(11) DEFAULT 0,
                        `total_earned` INT(11) DEFAULT 0,
                        `total_deducted` INT(11) DEFAULT 0,
                        `total_spent` INT(11) DEFAULT 0,
                        `last_updated` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        UNIQUE KEY (user_id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                ");
                if ($verbose) echo "  ✅ جدول customer_credits ایجاد شد\n";
            } else {
                if ($verbose) echo "  ✓ جدول customer_credits وجود دارد\n";
                foreach (['total_deducted' => 'INT(11) DEFAULT 0', 'last_updated' => 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'total_spent' => 'INT(11) DEFAULT 0'] as $column => $definition) {
                    if (!columnExists($pdo, 'customer_credits', $column)) {
                        if ($verbose) echo "  🔄 اضافه کردن ستون {$column}...\n";
                        $pdo->exec("ALTER TABLE customer_credits ADD COLUMN `$column` $definition");
                        if ($verbose) echo "  ✅ ستون {$column} اضافه شد\n";
                    }
                }
            }

            // =============== جدول credit_history ===============
            if ($verbose) echo "\n📋 بررسی جدول credit_history...\n";
            if (!tableExists($pdo, 'credit_history')) {
                if ($verbose) echo "  🔄 ایجاد جدول credit_history...\n";
                $pdo->exec("
                    CREATE TABLE IF NOT EXISTS `credit_history` (
                        `id` INT(11) PRIMARY KEY AUTO_INCREMENT,
                        `user_id` INT(11) NOT NULL,
                        `booking_id` INT(11) DEFAULT NULL,
                        `admin_id` INT(11) DEFAULT NULL,
                        `business_id` INT(11) DEFAULT NULL,
                        `change_type` VARCHAR(50) NOT NULL,
                        `credit_change` INT(11) NOT NULL DEFAULT 0,
                        `credit_before` INT(11) NOT NULL DEFAULT 0,
                        `credit_after` INT(11) NOT NULL DEFAULT 0,
                        `reason` TEXT DEFAULT NULL,
                        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        KEY `idx_credit_history_user` (`user_id`),
                        KEY `idx_credit_history_booking` (`booking_id`),
                        KEY `idx_credit_history_business` (`business_id`),
                        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
                        FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`) ON DELETE SET NULL,
                        FOREIGN KEY (`admin_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
                        FOREIGN KEY (`business_id`) REFERENCES `businesses` (`id`) ON DELETE SET NULL
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                ");
                if ($verbose) echo "  ✅ جدول credit_history ایجاد شد\n";
            } else {
                if ($verbose) echo "  ✓ جدول credit_history وجود دارد\n";
                foreach (['booking_id' => 'INT(11) DEFAULT NULL', 'admin_id' => 'INT(11) DEFAULT NULL', 'business_id' => 'INT(11) DEFAULT NULL', 'credit_change' => 'INT(11) NOT NULL DEFAULT 0', 'credit_before' => 'INT(11) NOT NULL DEFAULT 0', 'credit_after' => 'INT(11) NOT NULL DEFAULT 0', 'reason' => 'TEXT DEFAULT NULL'] as $column => $definition) {
                    if (!columnExists($pdo, 'credit_history', $column)) {
                        if ($verbose) echo "  🔄 اضافه کردن ستون {$column}...\n";
                        $pdo->exec("ALTER TABLE credit_history ADD COLUMN `$column` $definition");
                        if ($verbose) echo "  ✅ ستون {$column} اضافه شد\n";
                    }
                }
            }

            // =============== روتین امتیاز مشتری ===============
            if (!ensureCustomerCreditProcedure($pdo)) {
                if ($verbose) echo "  ⚠️ ایجاد Procedure UpdateCustomerCredit انجام نشد\n";
            } else if ($verbose) {
                echo "  ✅ Procedure UpdateCustomerCredit آماده است\n";
            }
            
            // =============== جدول specific_time_slots ===============
            if ($verbose) echo "\n📋 بررسی جدول specific_time_slots...\n";
            
            if (!tableExists($pdo, 'specific_time_slots')) {
                if ($verbose) echo "  🔄 ایجاد جدول specific_time_slots...\n";
                $pdo->exec("
                    CREATE TABLE IF NOT EXISTS `specific_time_slots` (
                        `id` INT(11) PRIMARY KEY AUTO_INCREMENT,
                        `business_id` INT(11) NOT NULL,
                        `slot_date` DATE NOT NULL,
                        `start_time` TIME NOT NULL,
                        `end_time` TIME NOT NULL,
                        `slot_duration_minutes` INT(11) DEFAULT 30 COMMENT 'مدت زمان اسلات بر حسب دقیقه',
                        `is_available` TINYINT(1) DEFAULT 1,
                        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
                        KEY (business_id),
                        KEY (slot_date)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                ");
                if ($verbose) echo "  ✅ جدول specific_time_slots ایجاد شد\n";
            } else {
                if ($verbose) echo "  ✓ جدول specific_time_slots وجود دارد\n";
                // اضافه کردن ستون slot_duration_minutes اگر وجود ندارد
                if (!columnExists($pdo, 'specific_time_slots', 'slot_duration_minutes')) {
                    if ($verbose) echo "  🔄 اضافه کردن ستون slot_duration_minutes...\n";
                    $pdo->exec("ALTER TABLE specific_time_slots ADD COLUMN slot_duration_minutes INT(11) DEFAULT 30 COMMENT 'مدت زمان اسلات بر حسب دقیقه' AFTER end_time");
                    if ($verbose) echo "  ✅ ستون slot_duration_minutes اضافه شد\n";
                }
            }
            
            // =============== جدول SubscriptionPlans ===============
            if ($verbose) echo "\n📋 بررسی جدول SubscriptionPlans...\n";
            
            if (!tableExists($pdo, 'SubscriptionPlans')) {
                if ($verbose) echo "  🔄 ایجاد جدول SubscriptionPlans...\n";
                $pdo->exec("
                    CREATE TABLE IF NOT EXISTS `SubscriptionPlans` (
                      `id` INT PRIMARY KEY AUTO_INCREMENT,
                      `name` VARCHAR(255) NOT NULL COMMENT 'نام پلن (مثلاً: پایه، نقره‌ای)',
                      `price` DECIMAL(10, 2) NOT NULL COMMENT 'قیمت پلن به ریال',
                      `reservation_limit` INT NOT NULL COMMENT 'تعداد رزرو مجاز در ماه (-1 برای نامحدود)',
                      `duration_days` INT NOT NULL COMMENT 'مدت اعتبار پلن به روز (مثلاً: 30)',
                      `is_active` TINYINT(1) DEFAULT 1,
                      `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                ");
                if ($verbose) echo "  ✅ جدول SubscriptionPlans ایجاد شد\n";
            } else {
                if ($verbose) echo "  ✓ جدول SubscriptionPlans وجود دارد\n";
            }
            
            // =============== جدول booking_attendance ===============
            if ($verbose) echo "\n📋 بررسی جدول booking_attendance...\n";
            
            if (!tableExists($pdo, 'booking_attendance')) {
                if ($verbose) echo "  🔄 ایجاد جدول booking_attendance...\n";
                $pdo->exec("
                    CREATE TABLE IF NOT EXISTS `booking_attendance` (
                        `id` INT(11) PRIMARY KEY AUTO_INCREMENT,
                        `booking_id` INT(11) NOT NULL,
                        `user_id` INT(11) NOT NULL,
                        `business_id` INT(11) NOT NULL,
                        `attendance_status` VARCHAR(50) DEFAULT 'pending' COMMENT 'pending, attended, no_show, cancelled',
                        `confirmed_by` INT(11) DEFAULT NULL,
                        `confirmation_date` TIMESTAMP NULL DEFAULT NULL,
                        `notes` TEXT,
                        `credit_processed` TINYINT(1) DEFAULT 0,
                        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
                        FOREIGN KEY (confirmed_by) REFERENCES users(id) ON DELETE SET NULL,
                        UNIQUE KEY (booking_id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                ");
                if ($verbose) echo "  ✅ جدول booking_attendance ایجاد شد\n";
            } else {
                if ($verbose) echo "  ✓ جدول booking_attendance وجود دارد\n";
                foreach (['user_id' => 'INT(11) NOT NULL', 'business_id' => 'INT(11) NOT NULL', 'confirmed_by' => 'INT(11) DEFAULT NULL', 'confirmation_date' => 'TIMESTAMP NULL DEFAULT NULL', 'credit_processed' => 'TINYINT(1) DEFAULT 0'] as $column => $definition) {
                    if (!columnExists($pdo, 'booking_attendance', $column)) {
                        if ($verbose) echo "  🔄 اضافه کردن ستون {$column}...\n";
                        $pdo->exec("ALTER TABLE booking_attendance ADD COLUMN `$column` $definition");
                        if ($verbose) echo "  ✅ ستون {$column} اضافه شد\n";
                    }
                }
            }
            
            // =============== جدول schedules ===============
            if ($verbose) echo "\n📋 بررسی جدول schedules...\n";
            
            if (!tableExists($pdo, 'schedules')) {
                if ($verbose) echo "  🔄 ایجاد جدول schedules...\n";
                $pdo->exec("
                    CREATE TABLE IF NOT EXISTS `schedules` (
                      `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'شناسه یکتای زمان‌بندی',
                      `business_id` int(11) NOT NULL COMMENT 'شناسه کسب‌وکار',
                      `day_of_week` tinyint(1) NOT NULL COMMENT 'روز هفته (0=یکشنبه، 1=دوشنبه، ...، 6=شنبه)',
                      `start_time` time DEFAULT NULL COMMENT 'زمان شروع کار',
                      `end_time` time DEFAULT NULL COMMENT 'زمان پایان کار',
                      `is_working` tinyint(1) DEFAULT 1 COMMENT 'آیا در این روز کار می‌کند؟',
                      `is_active` tinyint(1) DEFAULT 1 COMMENT 'آیا فعال است؟',
                      `break_start` time DEFAULT NULL COMMENT 'زمان شروع استراحت',
                      `break_end` time DEFAULT NULL COMMENT 'زمان پایان استراحت',
                      `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'زمان ایجاد',
                      `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'زمان آخرین به‌روزرسانی',
                      PRIMARY KEY (`id`),
                      UNIQUE KEY `business_day` (`business_id`, `day_of_week`),
                      KEY `business_id` (`business_id`),
                      KEY `day_of_week` (`day_of_week`),
                      FOREIGN KEY (`business_id`) REFERENCES `businesses` (`id`) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='جدول زمان‌بندی‌های عمومی'
                ");
                if ($verbose) echo "  ✅ جدول schedules ایجاد شد\n";
            } else {
                if ($verbose) echo "  ✓ جدول schedules وجود دارد\n";
            }
            
            // =============== جدول schedule_overrides ===============
            if ($verbose) echo "\n📋 بررسی جدول schedule_overrides...\n";
            
            if (!tableExists($pdo, 'schedule_overrides')) {
                if ($verbose) echo "  🔄 ایجاد جدول schedule_overrides...\n";
                $pdo->exec("
                    CREATE TABLE IF NOT EXISTS `schedule_overrides` (
                      `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'شناسه یکتای تغییر',
                      `business_id` int(11) NOT NULL COMMENT 'شناسه کسب‌وکار',
                      `override_date` date NOT NULL COMMENT 'تاریخ تغییر',
                      `start_time` time DEFAULT NULL COMMENT 'زمان شروع (NULL = تعطیل)',
                      `end_time` time DEFAULT NULL COMMENT 'زمان پایان (NULL = تعطیل)',
                      `is_working` tinyint(1) DEFAULT 0 COMMENT 'آیا در این روز کار می‌کند؟',
                      `is_day_off` tinyint(1) DEFAULT 0 COMMENT 'روز تعطیل است؟',
                      `reason` varchar(255) DEFAULT NULL COMMENT 'دلیل تغییر',
                      `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'زمان ایجاد',
                      `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'زمان آخرین به‌روزرسانی',
                      PRIMARY KEY (`id`),
                      UNIQUE KEY `business_date` (`business_id`, `override_date`),
                      KEY `business_id` (`business_id`),
                      KEY `override_date` (`override_date`),
                      FOREIGN KEY (`business_id`) REFERENCES `businesses` (`id`) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='جدول تغییرات زمان‌بندی'
                ");
                if ($verbose) echo "  ✅ جدول schedule_overrides ایجاد شد\n";
            } else {
                if ($verbose) echo "  ✓ جدول schedule_overrides وجود دارد\n";
            }
            
            // =============== جدول daily_schedules ===============
            if ($verbose) echo "\n📋 بررسی جدول daily_schedules...\n";
            
            if (!tableExists($pdo, 'daily_schedules')) {
                if ($verbose) echo "  🔄 ایجاد جدول daily_schedules...\n";
                $pdo->exec("
                    CREATE TABLE IF NOT EXISTS `daily_schedules` (
                      `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'شناسه یکتای زمان‌بندی روزانه',
                      `business_id` int(11) NOT NULL COMMENT 'شناسه کسب‌وکار',
                      `schedule_date` date NOT NULL COMMENT 'تاریخ زمان‌بندی',
                      `start_time` time DEFAULT NULL COMMENT 'زمان شروع',
                      `end_time` time DEFAULT NULL COMMENT 'زمان پایان',
                      `is_available` tinyint(1) DEFAULT 1 COMMENT 'آیا قابل رزرو است؟',
                      `notes` text DEFAULT NULL COMMENT 'یادداشت‌ها',
                      `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'زمان ایجاد',
                      `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'زمان آخرین به‌روزرسانی',
                      PRIMARY KEY (`id`),
                      UNIQUE KEY `business_date` (`business_id`, `schedule_date`),
                      KEY `business_id` (`business_id`),
                      KEY `schedule_date` (`schedule_date`),
                      KEY `is_available` (`is_available`),
                      FOREIGN KEY (`business_id`) REFERENCES `businesses` (`id`) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='جدول زمان‌بندی‌های روزانه'
                ");
                if ($verbose) echo "  ✅ جدول daily_schedules ایجاد شد\n";
            } else {
                if ($verbose) echo "  ✓ جدول daily_schedules وجود دارد\n";
            }
            
            if ($verbose) {
                echo "\n" . str_repeat("=", 60) . "\n";
                echo "✅ تمام بررسی‌ها انجام شد و مشکلات برطرف شدند!\n";
                echo "🎉 دیتابیس اکنون برای استفاده آماده است.\n\n";
            }
            
            return true;
            
        } catch (PDOException $e) {
            if ($verbose) {
                echo "\n❌ خطای دیتابیس: " . $e->getMessage() . "\n";
                echo "کد خطا: " . $e->getCode() . "\n";
            }
            error_log("Database init error: " . $e->getMessage());
            return false;
        } catch (Exception $e) {
            if ($verbose) echo "\n❌ خطای عمومی: " . $e->getMessage() . "\n";
            error_log("General init error: " . $e->getMessage());
            return false;
        }
    }
}

// اگر مستقلانه اجرا شود (php database_init.php)
if (php_sapi_name() === 'cli' && basename(__FILE__) === basename($_SERVER['argv'][0] ?? '')) {
    require_once __DIR__ . '/config.php';
    initDatabase($pdo, true);
}
?>
