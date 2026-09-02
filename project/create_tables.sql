-- فایل ایجاد جداول دیتابیس برای سیستم رزرو آنلاین
-- این فایل تمام جداول مورد نیاز را ایجاد می‌کند
-- تاریخ ایجاد: 2025-01-08
-- نسخه: 1.0

-- حذف جداول موجود (در صورت نیاز)
-- DROP TABLE IF EXISTS `daily_schedules`;
-- DROP TABLE IF EXISTS `schedule_overrides`;
-- DROP TABLE IF EXISTS `schedules`;
-- DROP TABLE IF EXISTS `businesses`;
-- DROP TABLE IF EXISTS `users`;

-- جدول کاربران (Users)
-- این جدول اطلاعات کاربران تلگرام را ذخیره می‌کند
CREATE TABLE IF NOT EXISTS `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'شناسه یکتای کاربر',
  `telegram_id` bigint(20) NOT NULL COMMENT 'شناسه کاربر در تلگرام',
  `first_name` varchar(100) NOT NULL COMMENT 'نام کاربر',
  `last_name` varchar(100) DEFAULT NULL COMMENT 'نام خانوادگی کاربر',
  `username` varchar(100) DEFAULT NULL COMMENT 'نام کاربری تلگرام',
  `phone` varchar(20) DEFAULT NULL COMMENT 'شماره تلفن',
  `email` varchar(100) DEFAULT NULL COMMENT 'ایمیل',
  `is_active` tinyint(1) DEFAULT 1 COMMENT 'وضعیت فعال/غیرفعال',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'زمان ایجاد',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'زمان آخرین به‌روزرسانی',
  PRIMARY KEY (`id`),
  UNIQUE KEY `telegram_id` (`telegram_id`),
  KEY `username` (`username`),
  KEY `is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='جدول کاربران';

-- جدول کسب‌وکارها
-- توجه: اگر جدول با ستون user_id وجود دارد، ابتدا آن را به owner_id تغییر دهید
CREATE TABLE IF NOT EXISTS `businesses` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'شناسه یکتای کسب‌وکار',
  `owner_id` int(11) NOT NULL COMMENT 'شناسه مالک کسب‌وکار (ارجاع به جدول users)',
  `name` varchar(255) NOT NULL COMMENT 'نام کسب‌وکار',
  `description` text DEFAULT NULL COMMENT 'توضیحات کسب‌وکار',
  `province` varchar(100) DEFAULT NULL COMMENT 'استان',
  `city` varchar(100) DEFAULT NULL COMMENT 'شهر',
  `address` text DEFAULT NULL COMMENT 'آدرس کسب‌وکار',
  `phone` varchar(20) DEFAULT NULL COMMENT 'شماره تلفن',
  `email` varchar(255) DEFAULT NULL COMMENT 'ایمیل',
  `website` varchar(255) DEFAULT NULL COMMENT 'وب‌سایت',
  `business_type` varchar(100) DEFAULT NULL COMMENT 'نوع کسب‌وکار',
  `slot_duration_minutes` int(11) DEFAULT 30 COMMENT 'مدت زمان هر بازه (دقیقه)',
  `advance_booking_days` int(11) DEFAULT 14 COMMENT 'امکان رزرو از چند روز قبل',
  `is_active` tinyint(1) DEFAULT 1 COMMENT 'وضعیت فعال/غیرفعال',
  `marketer_id` int(11) DEFAULT NULL COMMENT 'شناسه بازاریاب (ارجاع به جدول marketers)',
  `free_reservation_tokens` int(11) DEFAULT 0 COMMENT 'تعداد رزروهای رایگان',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'زمان ایجاد',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'زمان آخرین به‌روزرسانی',
  PRIMARY KEY (`id`),
  KEY `owner_id` (`owner_id`),
  KEY `is_active` (`is_active`),
  KEY `business_type` (`business_type`),
  FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='جدول کسب‌وکارها';

-- اگر جدول با ستون user_id وجود دارد، این دستورات را اجرا کنید:
-- ALTER TABLE businesses DROP FOREIGN KEY IF EXISTS businesses_ibfk_1;
-- ALTER TABLE businesses DROP FOREIGN KEY IF EXISTS fk_businesses_user_id;
-- ALTER TABLE businesses CHANGE user_id owner_id INT(11) NOT NULL COMMENT 'شناسه مالک کسب‌وکار';
-- ALTER TABLE businesses ADD CONSTRAINT fk_businesses_owner_id FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE;

-- جدول زمان‌بندی‌های عمومی (Schedules)
-- این جدول الگوی کلی زمان‌بندی هر کسب‌وکار را ذخیره می‌کند
CREATE TABLE IF NOT EXISTS `schedules` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'شناسه یکتای زمان‌بندی',
  `business_id` int(11) NOT NULL COMMENT 'شناسه کسب‌وکار',
  `day_of_week` tinyint(1) NOT NULL COMMENT 'روز هفته (0=یکشنبه، 1=دوشنبه، ...، 6=شنبه)',
  `start_time` time DEFAULT NULL COMMENT 'زمان شروع کار',
  `end_time` time DEFAULT NULL COMMENT 'زمان پایان کار',
  `is_working` tinyint(1) DEFAULT 1 COMMENT 'آیا در این روز کار می‌کند؟',
  `break_start` time DEFAULT NULL COMMENT 'زمان شروع استراحت',
  `break_end` time DEFAULT NULL COMMENT 'زمان پایان استراحت',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'زمان ایجاد',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'زمان آخرین به‌روزرسانی',
  PRIMARY KEY (`id`),
  UNIQUE KEY `business_day` (`business_id`, `day_of_week`),
  KEY `business_id` (`business_id`),
  KEY `day_of_week` (`day_of_week`),
  FOREIGN KEY (`business_id`) REFERENCES `businesses` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='جدول زمان‌بندی‌های عمومی';

-- جدول تغییرات زمان‌بندی (Schedule Overrides)
-- این جدول تغییرات خاص در روزهای مشخص را ذخیره می‌کند
CREATE TABLE IF NOT EXISTS `schedule_overrides` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'شناسه یکتای تغییر',
  `business_id` int(11) NOT NULL COMMENT 'شناسه کسب‌وکار',
  `override_date` date NOT NULL COMMENT 'تاریخ تغییر',
  `start_time` time DEFAULT NULL COMMENT 'زمان شروع (NULL = تعطیل)',
  `end_time` time DEFAULT NULL COMMENT 'زمان پایان (NULL = تعطیل)',
  `is_working` tinyint(1) DEFAULT 0 COMMENT 'آیا در این روز کار می‌کند؟',
  `reason` varchar(255) DEFAULT NULL COMMENT 'دلیل تغییر',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'زمان ایجاد',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'زمان آخرین به‌روزرسانی',
  PRIMARY KEY (`id`),
  UNIQUE KEY `business_date` (`business_id`, `override_date`),
  KEY `business_id` (`business_id`),
  KEY `override_date` (`override_date`),
  FOREIGN KEY (`business_id`) REFERENCES `businesses` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='جدول تغییرات زمان‌بندی';

-- جدول زمان‌بندی‌های روزانه (Daily Schedules)
-- این جدول برای ذخیره زمان‌بندی‌های تفصیلی روزانه استفاده می‌شود
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='جدول زمان‌بندی‌های روزانه';

-- درج داده‌های نمونه برای تست

-- کاربران نمونه
INSERT IGNORE INTO `users` (`telegram_id`, `first_name`, `last_name`, `username`, `phone`, `email`) VALUES
(123456789, 'علی', 'احمدی', 'ali_ahmadi', '09123456789', 'ali@example.com'),
(987654321, 'فاطمه', 'محمدی', 'fateme_mohammadi', '09987654321', 'fateme@example.com'),
(555666777, 'محمد', 'رضایی', 'mohammad_rezaei', '09555666777', 'mohammad@example.com');

-- کسب‌وکارهای نمونه
INSERT IGNORE INTO `businesses` (`owner_id`, `name`, `description`, `address`, `phone`, `email`, `business_type`, `slot_duration_minutes`, `advance_booking_days`) VALUES
(1, 'آرایشگاه زیبا', 'آرایشگاه زنانه با خدمات کامل', 'تهران، خیابان ولیعصر، پلاک 123', '02112345678', 'info@ziba.com', 'آرایشگاه', 30, 14),
(2, 'کلینیک دندانپزشکی سلامت', 'کلینیک دندانپزشکی با تجهیزات مدرن', 'تهران، خیابان کریمخان، پلاک 456', '02187654321', 'info@salamat.com', 'پزشکی', 45, 30),
(3, 'مرکز ماساژ درمانی', 'ماساژ درمانی و فیزیوتراپی', 'تهران، خیابان شریعتی، پلاک 789', '02155667788', 'info@massage.com', 'درمانی', 60, 7);

-- زمان‌بندی‌های عمومی نمونه (آرایشگاه زیبا - شنبه تا چهارشنبه)
INSERT IGNORE INTO `schedules` (`business_id`, `day_of_week`, `start_time`, `end_time`, `is_working`) VALUES
(1, 6, '09:00:00', '18:00:00', 1), -- شنبه
(1, 0, '09:00:00', '18:00:00', 1), -- یکشنبه
(1, 1, '09:00:00', '18:00:00', 1), -- دوشنبه
(1, 2, '09:00:00', '18:00:00', 1), -- سه‌شنبه
(1, 3, '09:00:00', '18:00:00', 1), -- چهارشنبه
(1, 4, NULL, NULL, 0), -- پنج‌شنبه (تعطیل)
(1, 5, NULL, NULL, 0); -- جمعه (تعطیل)

-- زمان‌بندی‌های عمومی نمونه (کلینیک دندانپزشکی - شنبه تا چهارشنبه)
INSERT IGNORE INTO `schedules` (`business_id`, `day_of_week`, `start_time`, `end_time`, `is_working`, `break_start`, `break_end`) VALUES
(2, 6, '08:00:00', '20:00:00', 1, '12:00:00', '14:00:00'), -- شنبه
(2, 0, '08:00:00', '20:00:00', 1, '12:00:00', '14:00:00'), -- یکشنبه
(2, 1, '08:00:00', '20:00:00', 1, '12:00:00', '14:00:00'), -- دوشنبه
(2, 2, '08:00:00', '20:00:00', 1, '12:00:00', '14:00:00'), -- سه‌شنبه
(2, 3, '08:00:00', '20:00:00', 1, '12:00:00', '14:00:00'), -- چهارشنبه
(2, 4, NULL, NULL, 0, NULL, NULL), -- پنج‌شنبه (تعطیل)
(2, 5, NULL, NULL, 0, NULL, NULL); -- جمعه (تعطیل)

-- تغییرات زمان‌بندی نمونه
INSERT IGNORE INTO `schedule_overrides` (`business_id`, `override_date`, `start_time`, `end_time`, `is_working`, `reason`) VALUES
(1, '2024-12-31', NULL, NULL, 0, 'تعطیلات سال نو'),
(1, '2024-01-15', '10:00:00', '16:00:00', 1, 'ساعت کاری کوتاه‌تر'),
(2, '2024-12-31', NULL, NULL, 0, 'تعطیلات سال نو'),
(2, '2024-01-20', '09:00:00', '15:00:00', 1, 'جلسه آموزشی عصر'),
(3, '2024-01-10', '14:00:00', '22:00:00', 1, 'ساعت کاری عصر');

-- زمان‌بندی‌های روزانه نمونه
INSERT IGNORE INTO `daily_schedules` (`business_id`, `schedule_date`, `start_time`, `end_time`, `is_available`, `notes`) VALUES
(1, '2024-01-15', '09:00:00', '18:00:00', 1, 'روز عادی کاری'),
(1, '2024-01-16', '10:00:00', '17:00:00', 1, 'شروع دیرتر'),
(2, '2024-01-15', '08:00:00', '20:00:00', 1, 'روز پر کار'),
(2, '2024-01-16', NULL, NULL, 0, 'تعطیل اضطراری'),
(3, '2024-01-15', '09:00:00', '21:00:00', 1, 'روز عادی');

-- نمایش اطلاعات ایجاد شده
SELECT 'جداول ایجاد شدند!' as message;
SELECT 'تعداد کاربران:' as info, COUNT(*) as count FROM users;
SELECT 'تعداد کسب‌وکارها:' as info, COUNT(*) as count FROM businesses;
SELECT 'تعداد زمان‌بندی‌ها:' as info, COUNT(*) as count FROM schedules;
SELECT 'تعداد تغییرات:' as info, COUNT(*) as count FROM schedule_overrides;
SELECT 'تعداد زمان‌بندی‌های روزانه:' as info, COUNT(*) as count FROM daily_schedules;

-- پایان فایل