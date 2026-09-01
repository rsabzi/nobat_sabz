<?php
/**
 * mobile_auth.php — احراز هویت اپلیکیشن اندروید «نوبت‌سبز»
 *
 * این فایل هم‌زمان دو نقش دارد:
 *  1) یک API مستقل برای ثبت‌نام/ورود/خروج کاربران اپلیکیشن (موبایل)
 *     (اکشن‌ها: register, login, logout, me, update_profile, change_password, get_notifications)
 *  2) کتابخانه کمکی برای webapp/api.php — تابع mobile_auth_from_request()
 *     تا تمام اکشن‌های موجود (رزرو، لغو، صف، اشتراک، داشبورد بازاریاب و ...)
 *     با توکن موبایل هم به‌صورت امن قابل استفاده باشند.
 *
 * امنیت:
 *  - رمز عبور فقط به‌صورت هش (password_hash) ذخیره می‌شود.
 *  - توکن نشست ۳۲ بایت تصادفی است و فقط هش SHA-256 آن در دیتابیس می‌ماند.
 *  - هیچ سکریتی در کد/کلاینت وجود ندارد.
 *  - هیچ تغییری در منطق موجود api.php اعمال نشده؛ فقط یک مسیر احراز هویت اضافه شده است.
 */

if (!defined('BOT_TOKEN')) {
    require_once __DIR__ . '/../config.php';
}

// ---------------------------------------------------------------------
// توابع کمکی (guarded تا با include مکرر تداخل نشود)
// ---------------------------------------------------------------------

if (!function_exists('mobile_auth_ensure_tables')) {
    function mobile_auth_ensure_tables($pdo) {
        if (!$pdo) return false;
        static $_done = false;
        if ($_done) return true;
        try {
            $pdo->exec("
                CREATE TABLE IF NOT EXISTS `app_users` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT,
                  `phone` VARCHAR(20) NOT NULL,
                  `password_hash` VARCHAR(255) NOT NULL,
                  `full_name` VARCHAR(100) NOT NULL,
                  `linked_user_id` INT NULL,
                  `telegram_id` BIGINT NOT NULL,
                  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
                  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_phone` (`phone`),
                  UNIQUE KEY `uk_telegram` (`telegram_id`),
                  KEY `idx_linked_user` (`linked_user_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            ");
            $pdo->exec("
                CREATE TABLE IF NOT EXISTS `app_sessions` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT,
                  `app_user_id` BIGINT NOT NULL,
                  `token_hash` CHAR(64) NOT NULL,
                  `device` VARCHAR(200) NULL,
                  `expires_at` DATETIME NOT NULL,
                  `last_seen_at` DATETIME NULL,
                  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_token` (`token_hash`),
                  KEY `idx_app_user` (`app_user_id`),
                  KEY `idx_expires` (`expires_at`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            ");
            $_done = true;
            return true;
        } catch (Exception $e) {
            error_log('mobile_auth: ensure tables failed: ' . $e->getMessage());
            return false;
        }
    }
}

if (!function_exists('mobile_auth_json')) {
    function mobile_auth_json($status, $message, $data = [], $http_code = 200) {
        if (!headers_sent()) {
            header('Content-Type: application/json; charset=utf-8');
            http_response_code($http_code);
        }
        $response = ['status' => $status, 'message' => $message];
        if (!empty($data)) $response = array_merge($response, $data);
        echo json_encode($response, JSON_UNESCAPED_UNICODE);
        exit;
    }
}

if (!function_exists('mobile_normalize_phone')) {
    function mobile_normalize_phone($phone) {
        $phone = trim((string)$phone);
        $fa = ['۰','۱','۲','۳','۴','۵','۶','۷','۸','۹','٠','١','٢','٣','٤','٥','٦','٧','٨','٩'];
        $en = ['0','1','2','3','4','5','6','7','8','9','0','1','2','3','4','5','6','7','8','9'];
        $phone = str_replace($fa, $en, $phone);
        $phone = preg_replace('/\D/', '', $phone);
        if (strlen($phone) === 12 && strpos($phone, '98') === 0) $phone = '0' . substr($phone, 2);
        if (strlen($phone) === 10 && strpos($phone, '9') === 0) $phone = '0' . $phone;
        if (strlen($phone) === 11 && strpos($phone, '09') === 0) return $phone;
        return null;
    }
}

if (!function_exists('mobile_auth_build_token')) {
    function mobile_auth_build_token() {
        return bin2hex(random_bytes(32));
    }
}

if (!function_exists('mobile_auth_create_session')) {
    function mobile_auth_create_session($pdo, $appUserId, $device = null) {
        $token = mobile_auth_build_token();
        $tokenHash = hash('sha256', $token);
        $expiresAt = date('Y-m-d H:i:s', time() + (30 * 24 * 3600));
        $stmt = $pdo->prepare(
            "INSERT INTO app_sessions (app_user_id, token_hash, device, expires_at) VALUES (?, ?, ?, ?)"
        );
        $stmt->execute([$appUserId, $tokenHash, $device ? mb_substr($device, 0, 200) : null, $expiresAt]);
        return ['token' => $token, 'expires_at' => $expiresAt];
    }
}

if (!function_exists('mobile_auth_cleanup_sessions')) {
    function mobile_auth_cleanup_sessions($pdo) {
        try { $pdo->exec("DELETE FROM app_sessions WHERE expires_at < NOW() - INTERVAL 7 DAY"); } catch (Exception $e) {}
    }
}

/**
 * اعتبارسنجی توکن از هدر X-App-Token.
 * خروجی: آرایه کاربر یا null
 *  [
 *    'app_user_id' => ...,
 *    'telegram_id' => ...   (شناسه تلگرامی синтетیک برای سازگاری با api.php),
 *    'internal_user_id' => ... (شناسه در جدول users),
 *    'first_name' => ..., 'last_name' => ..., 'username' => ...
 *  ]
 */
if (!function_exists('mobile_auth_from_request')) {
    function mobile_auth_from_request() {
        global $pdo;
        static $_cached = null;
        $_cached = null; // هر درخواست یک‌بار ارزیابی شود

        if (!$pdo || !function_exists('mobile_auth_ensure_tables')) return null;
        mobile_auth_ensure_tables($pdo);

        $token = '';
        foreach ($_SERVER as $key => $value) {
            if (strcasecmp($key, 'HTTP_X_APP_TOKEN') === 0) { $token = $value; break; }
        }
        if ($token === '' && !empty($_GET['app_token'])) $token = $_GET['app_token'];
        if ($token === '' || strlen($token) < 32 || strlen($token) > 128) return null;

        $tokenHash = hash('sha256', $token);
        try {
            $stmt = $pdo->prepare(
                "SELECT s.id AS session_id, s.app_user_id, s.expires_at,
                        u.id AS linked_user_id, u.telegram_id, u.first_name, u.last_name, u.username, u.phone,
                        a.is_active, a.full_name
                 FROM app_sessions s
                 JOIN app_users a ON a.id = s.app_user_id
                 LEFT JOIN users u ON u.id = a.linked_user_id
                 WHERE s.token_hash = ? AND s.expires_at > NOW() AND a.is_active = 1
                 LIMIT 1"
            );
            $stmt->execute([$tokenHash]);
            $row = $stmt->fetch();
            if (!$row) return null;
            if ($row['linked_user_id'] === null || $row['telegram_id'] === null) return null;

            // تمدید خودکار نشست (sliding) و آخرین بازدید
            $stmt2 = $pdo->prepare(
                "UPDATE app_sessions
                 SET last_seen_at = NOW(),
                     expires_at = IF(expires_at < DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), expires_at)
                 WHERE id = ?"
            );
            $stmt2->execute([$row['session_id']]);
            mobile_auth_cleanup_sessions($pdo);

            return [
                'app_user_id'        => (int)$row['app_user_id'],
                'telegram_id'        => (int)$row['telegram_id'],
                'internal_user_id'   => (int)$row['linked_user_id'],
                'first_name'         => $row['full_name'] ?: ($row['first_name'] ?: 'کاربر'),
                'last_name'          => $row['last_name'] ?? '',
                'username'           => $row['username'] ?? '',
                'phone'              => $row['phone'] ?? ''
            ];
        } catch (Exception $e) {
            error_log('mobile_auth: resolve failed: ' . $e->getMessage());
            return null;
        }
    }
}

if (!function_exists('mobile_auth_require')) {
    /** احراز هویت اجباری برای مسیرهای محدود mobile_auth.php */
    function mobile_auth_require() {
        global $pdo;
        $user = mobile_auth_from_request();
        if (!$user) {
            mobile_auth_json('error', 'نشست شما منقضی شده است. لطفاً دوباره وارد شوید.', ['code' => 'AUTH_REQUIRED'], 401);
        }
        return $user;
    }
}

if (!function_exists('mobile_auth_register_user')) {
    /**
     * ساخت app_user + کاربر متناظر در جدول users (با telegram_id مصنوعی منفی)
     * تا تمام منطق موجود (رزرو، اعتبار، صف و ...) بدون تغییر کار کند.
     */
    function mobile_auth_register_user($pdo, $phone, $password, $name) {
        mobile_auth_ensure_tables($pdo);

        $stmt = $pdo->prepare("SELECT id FROM app_users WHERE phone = ?");
        $stmt->execute([$phone]);
        if ($stmt->fetch()) return ['error' => 'این شماره تلفن قبلاً ثبت شده است.'];

        $pdo->beginTransaction();
        try {
            $passwordHash = password_hash($password, PASSWORD_DEFAULT);

            $syntheticBase = -9000000000000000000; // بزرگ‌تر از هر شناسه تلگرام واقعی
            $insertUser = $pdo->prepare(
                "INSERT INTO users (telegram_id, first_name, last_name, username, phone, is_active, created_at) 
                 VALUES (?, ?, '', 'app_user', ?, 1, NOW())"
            );
            // ابتدا با مقدار موقت یکتا درج می‌کنیم تا id بگیریم، سپس telegram_id نهایی را می‌سازیم.
            // مقدار موقت تصادفی است تا ثبت‌نام‌های هم‌زمان با هم تداخل (unique key) نداشته باشند.
            $tempTelegramId = $syntheticBase + random_int(1, 2000000000);
            $insertUser->execute([$tempTelegramId, $name, $phone]);
            $linkedUserId = (int)$pdo->lastInsertId();
            $telegramId = $syntheticBase + $linkedUserId;
            $updateUser = $pdo->prepare("UPDATE users SET telegram_id = ? WHERE id = ?");
            $updateUser->execute([$telegramId, $linkedUserId]);

            $insertApp = $pdo->prepare(
                "INSERT INTO app_users (phone, password_hash, full_name, linked_user_id, telegram_id) VALUES (?, ?, ?, ?, ?)"
            );
            $insertApp->execute([$phone, $passwordHash, $name, $linkedUserId, $telegramId]);
            $appUserId = (int)$pdo->lastInsertId();

            $pdo->commit();
            return ['app_user_id' => $appUserId, 'linked_user_id' => $linkedUserId, 'telegram_id' => $telegramId];
        } catch (Exception $e) {
            if ($pdo->inTransaction()) $pdo->rollBack();
            error_log('mobile_auth: register failed: ' . $e->getMessage());
            return ['error' => 'خطا در ثبت‌نام. لطفاً دوباره تلاش کنید.'];
        }
    }
}

if (!function_exists('mobile_auth_get_profile_aggregate')) {
    /** اطلاعات تجمیعی پروفایل برای صفحه اصلی اپلیکیشن */
    function mobile_auth_get_profile_aggregate($pdo, $user) {
        $internalUserId = $user['internal_user_id'];

        // --- امتیاز اعتباری مشتری ---
        $credit = null;
        try {
            $pdo->exec("
                CREATE TABLE IF NOT EXISTS `customer_credits` (
                  `id` INT NOT NULL AUTO_INCREMENT,
                  `user_id` INT NOT NULL,
                  `current_credit` INT NOT NULL DEFAULT 10,
                  `total_earned` INT NOT NULL DEFAULT 0,
                  `total_deducted` INT NOT NULL DEFAULT 0,
                  `last_updated` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_user` (`user_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            ");
            $stmt = $pdo->prepare("SELECT current_credit, total_earned, total_deducted, last_updated FROM customer_credits WHERE user_id = ?");
            $stmt->execute([$internalUserId]);
            $credit = $stmt->fetch();
            if (!$credit) {
                $pdo->prepare("INSERT IGNORE INTO customer_credits (user_id, current_credit, total_earned, total_deducted) VALUES (?, 10, 10, 0)")
                    ->execute([$internalUserId]);
                $stmt = $pdo->prepare("SELECT current_credit, total_earned, total_deducted, last_updated FROM customer_credits WHERE user_id = ?");
                $stmt->execute([$internalUserId]);
                $credit = $stmt->fetch();
            }
            if ($credit) $credit['current_credit'] = (int)$credit['current_credit'];
        } catch (Exception $e) { error_log('mobile me credit: ' . $e->getMessage()); }

        // --- کسب‌وکارهای کاربر (مالک) ---
        $businesses = [];
        try {
            $stmt = $pdo->prepare("
                SELECT b.id, b.name, b.business_type, b.province, b.city, b.is_active,
                       b.free_reservation_tokens, b.subscription_plan_id, b.subscription_expiry_date,
                       b.link_alias, b.slot_duration_minutes,
                       (SELECT COUNT(*) FROM services s WHERE s.business_id = b.id AND s.is_active = 1) AS services_count
                FROM businesses b
                WHERE b.owner_id = ? AND b.is_active = 0 OR b.owner_id = ? AND b.is_active = 1
                GROUP BY b.id
                ORDER BY b.created_at DESC
            ");
            // جدا کردن شرایط AND/OR با پرانتز
            $stmt = $pdo->prepare("
                SELECT b.id, b.name, b.business_type, b.province, b.city, b.is_active,
                       b.free_reservation_tokens, b.subscription_plan_id, b.subscription_expiry_date,
                       b.link_alias, b.slot_duration_minutes,
                       (SELECT COUNT(*) FROM services s WHERE s.business_id = b.id AND s.is_active = 1) AS services_count
                FROM businesses b
                WHERE b.owner_id = ?
                ORDER BY b.created_at DESC
            ");
            $stmt->execute([$internalUserId]);
            $businesses = $stmt->fetchAll();
        } catch (Exception $e) { error_log('mobile me businesses: ' . $e->getMessage()); }

        // --- بازاریاب ---
        $marketer = null;
        try {
            $stmt = $pdo->prepare("
                SELECT id, marketer_code, total_businesses, total_bookings, commission_rate, total_commission
                FROM marketers WHERE user_id = ? AND is_active = 1 LIMIT 1
            ");
            $stmt->execute([$internalUserId]);
            $marketer = $stmt->fetch();
        } catch (Exception $e) { error_log('mobile me marketer: ' . $e->getMessage()); }

        // --- آمار رزرو کاربر ---
        $bookingStats = ['total' => 0, 'upcoming' => 0, 'cancelled' => 0, 'completed' => 0];
        try {
            $stmt = $pdo->prepare("SELECT b.status, b.booking_date, b.start_time FROM bookings b WHERE b.user_id = ?");
            $stmt->execute([$internalUserId]);
            $rows = $stmt->fetchAll();
            $bookingStats['total'] = count($rows);
            foreach ($rows as $r) {
                if ($r['status'] === 'cancelled') { $bookingStats['cancelled']++; continue; }
                $ts = strtotime($r['booking_date'] . ' ' . $r['start_time']);
                if ($ts && $ts >= time()) $bookingStats['upcoming']++;
                else $bookingStats['completed']++;
            }
        } catch (Exception $e) { error_log('mobile me stats: ' . $e->getMessage()); }

        // اطمینان از عددی‌بودن فیلدها در JSON (PDO گاهی رشته برمی‌گرداند)
        $castInt = function (array $row, array $keys): array {
            foreach ($keys as $k) {
                if (array_key_exists($k, $row) && $row[$k] !== null) $row[$k] = (int)$row[$k];
            }
            return $row;
        };
        $credit = is_array($credit)
            ? $castInt($credit, ['current_credit', 'total_earned', 'total_deducted'])
            : null;
        $businesses = array_map(function ($b) use ($castInt) {
            return $castInt((array)$b, ['id', 'is_active', 'slot_duration_minutes', 'subscription_plan_id', 'free_reservation_tokens', 'services_count']);
        }, is_array($businesses) ? $businesses : []);
        if (is_array($marketer)) {
            $marketer = $castInt($marketer, ['id', 'total_businesses', 'total_bookings', 'commission_rate', 'total_commission']);
        }
        $bookingStats = array_map('intval', is_array($bookingStats) ? $bookingStats : []);

        return [
            'user' => [
                'app_user_id'   => (int)$user['app_user_id'],
                'name'          => $user['first_name'],
                'phone'         => $user['phone'],
                'telegram_id'   => (int)$user['telegram_id']
            ],
            'credit'       => $credit,
            'businesses'   => $businesses,
            'marketer'     => $marketer,
            'booking_stats'=> $bookingStats
        ];
    }
}

if (!function_exists('mobile_auth_get_notifications')) {
    /** اعلان‌های داخلی اپ: رویدادهای رزرو و تغییر امتیاز اعتباری */
    function mobile_auth_get_notifications($pdo, $user, $limit = 30) {
        $internalUserId = $user['internal_user_id'];
        $items = [];

        try {
            $stmt = $pdo->prepare("
                SELECT b.id, b.status, b.queue_status, b.booking_date, b.start_time, b.end_time,
                       b.confirmation_code, b.created_at, bus.name AS business_name
                FROM bookings b
                JOIN businesses bus ON bus.id = b.business_id
                WHERE b.user_id = ?
                ORDER BY b.created_at DESC
                LIMIT ?
            ");
            $stmt->bindValue(1, $internalUserId, PDO::PARAM_INT);
            $stmt->bindValue(2, min(50, max(1, $limit)), PDO::PARAM_INT);
            $stmt->execute();
            foreach ($stmt->fetchAll() as $r) {
                $type = 'booking_created'; $title = 'رزرو جدید'; $icon = '📅';
                if ($r['status'] === 'cancelled') { $type = 'booking_cancelled'; $title = 'رزرو لغو شد'; $icon = '❌'; }
                elseif (($r['queue_status'] ?? '') === 'completed') { $type = 'booking_completed'; $title = 'رزرو انجام شد'; $icon = '✅'; }
                elseif (($r['queue_status'] ?? '') === 'in_progress') { $type = 'booking_in_progress'; $title = 'نوبت شما آغاز شد'; $icon = '⏳'; }
                $items[] = [
                    'type' => $type, 'icon' => $icon, 'title' => $title,
                    'body' => $r['business_name'] . ' — ' . $r['booking_date'] . ' ' . $r['start_time'] . ' (' . $r['confirmation_code'] . ')',
                    'ref_id' => (int)$r['id'], 'created_at' => $r['created_at']
                ];
            }
        } catch (Exception $e) { error_log('mobile notifications: ' . $e->getMessage()); }

        try {
            $stmt = $pdo->prepare("
                SELECT credit_change, change_type, reason, created_at
                FROM credit_history
                WHERE user_id = ?
                ORDER BY created_at DESC
                LIMIT 20
            ");
            $stmt->execute([$internalUserId]);
            foreach ($stmt->fetchAll() as $r) {
                $delta = (int)($r['credit_change'] ?? 0);
                $items[] = [
                    'type' => 'credit_change', 'icon' => $delta >= 0 ? '⭐' : '➖',
                    'title' => $delta >= 0 ? "افزایش امتیاز (+{$delta})" : "کاهش امتیاز ({$delta})",
                    'body' => ($r['reason'] ?? 'تغییر امتیاز اعتباری'),
                    'ref_id' => null, 'created_at' => $r['created_at']
                ];
            }
        } catch (Exception $e) { /* جدول credit_history ممکن است نباشد */ }

        usort($items, fn($a, $b) => strcmp($b['created_at'] ?? '', $a['created_at'] ?? ''));
        return array_slice($items, 0, $limit);
    }
}

// ---------------------------------------------------------------------
// مسیریابی — فقط وقتی مستقیماً صدا زده شود (نه وقتی از api.php include شود)
// ---------------------------------------------------------------------
$__selfName = basename($_SERVER['SCRIPT_NAME'] ?? '');
if ($__selfName === 'mobile_auth.php') {
    header('Content-Type: application/json; charset=utf-8');
    header('Cache-Control: no-store');

    if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
        http_response_code(204);
        exit;
    }

    if (!mobile_auth_ensure_tables($pdo)) {
        mobile_auth_json('error', 'خطا در آماده‌سازی دیتابیس. لطفاً بعداً تلاش کنید.', [], 500);
    }

    $rawInput = file_get_contents('php://input');
    $input = json_decode($rawInput ?: '[]', true);
    if (!is_array($input)) $input = [];
    $action = $input['action'] ?? ($_GET['action'] ?? '');

    switch ($action) {
        case 'register': {
            $phone = mobile_normalize_phone($input['phone'] ?? '');
            $password = trim((string)($input['password'] ?? ''));
            $name = trim((string)($input['name'] ?? ''));
            $device = trim((string)($input['device'] ?? ''));

            if (!$phone) mobile_auth_json('error', 'شماره موبایل معتبر نیست.', [], 400);
            if (strlen($password) < 6) mobile_auth_json('error', 'رمز عبور باید حداقل ۶ کاراکتر باشد.', [], 400);
            if (mb_strlen($name) < 2) mobile_auth_json('error', 'نام و نام خانوادگی را وارد کنید.', [], 400);

            $result = mobile_auth_register_user($pdo, $phone, $password, $name);
            if (!empty($result['error'])) mobile_auth_json('error', $result['error'], [], 409);

            $session = mobile_auth_create_session($pdo, $result['app_user_id'], $device);
            mobile_auth_json('success', 'ثبت‌نام با موفقیت انجام شد.', [
                'token' => $session['token'], 'expires_at' => $session['expires_at'],
                'user' => ['name' => $name, 'phone' => $phone]
            ]);
            break;
        }

        case 'login': {
            $phone = mobile_normalize_phone($input['phone'] ?? '');
            $password = trim((string)($input['password'] ?? ''));
            $device = trim((string)($input['device'] ?? ''));

            if (!$phone || $password === '') mobile_auth_json('error', 'شماره موبایل و رمز عبور را وارد کنید.', [], 400);

            $stmt = $pdo->prepare("SELECT id, password_hash, full_name, is_active FROM app_users WHERE phone = ? LIMIT 1");
            $stmt->execute([$phone]);
            $appUser = $stmt->fetch();
            if (!$appUser || (int)$appUser['is_active'] !== 1 || !password_verify($password, $appUser['password_hash'])) {
                mobile_auth_json('error', 'شماره موبایل یا رمز عبور اشتباه است.', [], 401);
            }

            $session = mobile_auth_create_session($pdo, $appUser['id'], $device);
            mobile_auth_json('success', 'ورود با موفقیت انجام شد.', [
                'token' => $session['token'], 'expires_at' => $session['expires_at'],
                'user' => ['name' => $appUser['full_name'], 'phone' => $phone]
            ]);
            break;
        }

        case 'logout': {
            $user = mobile_auth_require();
            try {
                $stmt = $pdo->prepare("DELETE FROM app_sessions WHERE app_user_id = ?");
                $stmt->execute([$user['app_user_id']]);
            } catch (Exception $e) {}
            mobile_auth_json('success', 'خروج با موفقیت انجام شد.');
            break;
        }

        case 'me': {
            $user = mobile_auth_require();
            mobile_auth_json('success', 'اطلاعات کاربر دریافت شد.', mobile_auth_get_profile_aggregate($pdo, $user));
            break;
        }

        case 'update_profile': {
            $user = mobile_auth_require();
            $name = trim((string)($input['name'] ?? ''));
            $newPhone = mobile_normalize_phone($input['phone'] ?? '');

            if (mb_strlen($name) < 2) mobile_auth_json('error', 'نام و نام خانوادگی را وارد کنید.', [], 400);
            if (!$newPhone) mobile_auth_json('error', 'شماره موبایل معتبر نیست.', [], 400);

            // شماره جدید نباید متعلق به کاربر دیگری باشد
            $stmt = $pdo->prepare("SELECT id FROM app_users WHERE phone = ? AND id != ?");
            $stmt->execute([$newPhone, $user['app_user_id']]);
            if ($stmt->fetch()) mobile_auth_json('error', 'این شماره تلفن متعلق به حساب دیگری است.', [], 409);

            $pdo->prepare("UPDATE app_users SET full_name = ?, phone = ? WHERE id = ?")
                ->execute([$name, $newPhone, $user['app_user_id']]);
            $pdo->prepare("UPDATE users SET first_name = ?, phone = ? WHERE id = ? AND telegram_id = ?")
                ->execute([$name, $newPhone, $user['internal_user_id'], $user['telegram_id']]);

            mobile_auth_json('success', 'پروفایل با موفقیت به‌روزرسانی شد.', [
                'user' => ['name' => $name, 'phone' => $newPhone]
            ]);
            break;
        }

        case 'change_password': {
            $user = mobile_auth_require();
            $current = trim((string)($input['current_password'] ?? ''));
            $newPassword = trim((string)($input['new_password'] ?? ''));
            if (strlen($newPassword) < 6) mobile_auth_json('error', 'رمز جدید باید حداقل ۶ کاراکتر باشد.', [], 400);

            $stmt = $pdo->prepare("SELECT password_hash FROM app_users WHERE id = ?");
            $stmt->execute([$user['app_user_id']]);
            $row = $stmt->fetch();
            if (!$row || !password_verify($current, $row['password_hash'])) {
                mobile_auth_json('error', 'رمز عبور فعلی اشتباه است.', [], 401);
            }
            $pdo->prepare("UPDATE app_users SET password_hash = ? WHERE id = ?")
                ->execute([password_hash($newPassword, PASSWORD_DEFAULT), $user['app_user_id']]);

            // خروج از همه نشست‌های دیگر (نسخه فعلی امن‌تر)
            $pdo->prepare("DELETE FROM app_sessions WHERE app_user_id = ?")->execute([$user['app_user_id']]);
            mobile_auth_json('success', 'رمز عبور تغییر کرد. لطفاً دوباره وارد شوید.');
            break;
        }

        case 'get_notifications': {
            $user = mobile_auth_require();
            $limit = (int)($input['limit'] ?? 30);
            mobile_auth_json('success', 'اعلان‌ها دریافت شد.', [
                'notifications' => mobile_auth_get_notifications($pdo, $user, $limit)
            ]);
            break;
        }

        default:
            mobile_auth_json('error', 'عمل نامعتبر.', [], 400);
    }
}
