<?php
// File: /nobatsabz/webapp/api.php - API execution check
// (نسخه ۴.۳ با سیستم مدیریت خطاهای بهبود یافته)

// بارگذاری سیستم لاگ‌گذاری و مدیریت خطا
require_once __DIR__ . '/debug_logger.php';
require_once __DIR__ . '/error_handler.php';
debug_log('شروع پردازش درخواست API', 'INFO');

// بارگذاری فایل‌های ضروری
try {
    require_once __DIR__ . '/../config.php';
    debug_log('فایل config.php بارگذاری شد', 'INFO');
    // احراز هویت اپلیکیشن موبایل (توکن‌دار) — فقط مسیر افزوده، منطق موجود تغییر نکرده است
    require_once __DIR__ . '/mobile_auth.php';
    debug_log('فایل mobile_auth.php بارگذاری شد', 'INFO');
    //require_once __DIR__ . '/includes/auth.php';
    debug_log('فایل auth.php بارگذاری شد', 'INFO');
    
    // بررسی اتصال پایگاه داده
    if ($pdo) {
        debug_log('اتصال به پایگاه داده موفق', 'INFO');
    } else {
        debug_log('اتصال به پایگاه داده ناموفق - ادامه بدون دیتابیس', 'WARNING');
    }
} catch (Exception $e) {
    debug_log('خطا در بارگذاری فایل‌های ضروری: ' . $e->getMessage(), 'ERROR');
    ErrorHandler::sendError('SYSTEM_ERROR', 'خطا در بارگذاری تنظیمات سرور.');
}

// =================================================================
// بخش ۱: توابع کمکی
// =================================================================

// تابع حذف خودکار رزروهای منقضی شده
function cleanupExpiredBookings($pdo) {
    if (!$pdo) {
        debug_log("❌ عدم دسترسی به پایگاه داده برای حذف رزروهای منقضی", 'WARNING');
        return false;
    }
    
    try {
        $cleanupStmt = $pdo->prepare("
            DELETE FROM bookings 
            WHERE booking_date < DATE_SUB(CURDATE(), INTERVAL 1 DAY)
        ");
        $cleanupStmt->execute();
        $deletedCount = $cleanupStmt->rowCount();
        
        if ($deletedCount > 0) {
            debug_log("🗑️ حذف خودکار {$deletedCount} رزرو منقضی شده انجام شد", 'INFO');
        }
        
        return $deletedCount;
        
    } catch (PDOException $e) {
        debug_log("❌ خطا در حذف رزروهای منقضی: " . $e->getMessage(), 'ERROR');
        return false;
    }
}

// تابع بررسی و اجرای حذف دوره‌ای
function periodicCleanup($pdo) {
    // بررسی آخرین زمان اجرای cleanup از فایل
    $cleanupFile = __DIR__ . '/cache/last_cleanup.txt';
    $lastCleanup = 0;
    
    if (file_exists($cleanupFile)) {
        $lastCleanup = (int)file_get_contents($cleanupFile);
    }
    
    $currentTime = time();
    $sixHours = 6 * 60 * 60; // 6 ساعت به ثانیه
    
    // اگر بیش از 6 ساعت از آخرین cleanup گذشته باشد
    if (($currentTime - $lastCleanup) > $sixHours) {
        $result = cleanupExpiredBookings($pdo);
        
        if ($result !== false) {
            // ذخیره زمان اجرای cleanup
            $cacheDir = dirname($cleanupFile);
            if (!is_dir($cacheDir)) {
                mkdir($cacheDir, 0755, true);
            }
            file_put_contents($cleanupFile, $currentTime);
            debug_log("✅ اجرای دوره‌ای cleanup انجام شد - حذف {$result} رزرو", 'INFO');
        }
    }
}

function send_json_response($status, $message, $data = [], $http_code = 200) {
    debug_log("ارسال پاسخ: {$status} - {$message}", $status === 'error' ? 'ERROR' : 'INFO', ['http_code' => $http_code, 'data' => $data]);
    
    header('Content-Type: application/json; charset=utf-8');
    http_response_code($http_code);
    
    $response = [
        'status' => $status,
        'message' => $message
    ];
    
    if (!empty($data)) {
        $response = array_merge($response, $data);
    }
    
    echo json_encode($response, JSON_UNESCAPED_UNICODE);
    exit;
}

// محاسبه تعداد توکن‌های مورد نیاز بر اساس مدت زمان رزرو
function calculateTokensForDuration($duration_minutes) {
    debug_log("محاسبه توکن برای مدت زمان: {$duration_minutes} دقیقه", 'INFO');
    
    // بررسی صحت ورودی
    if (!is_numeric($duration_minutes) || $duration_minutes <= 0) {
        debug_log("مدت زمان نامعتبر: {$duration_minutes}", 'ERROR');
        return 1; // حداقل 1 توکن در صورت خطا
    }
    
    $duration = (int)$duration_minutes;
    
    // منطق محاسبه توکن بر اساس بازه‌های زمانی
    if ($duration >= 5 && $duration <= 15) {
        debug_log("بازه 5-15 دقیقه: 1 توکن", 'INFO');
        return 1;
    } elseif ($duration >= 20 && $duration <= 45) {
        debug_log("بازه 20-45 دقیقه: 2 توکن", 'INFO');
        return 2;
    } elseif ($duration >= 50 && $duration <= 90) {
        debug_log("بازه 50-90 دقیقه: 3 توکن", 'INFO');
        return 3;
    } else {
        // موارد خارج از بازه‌های تعریف شده
        if ($duration < 5) {
            debug_log("مدت زمان کمتر از 5 دقیقه: 1 توکن (حداقل)", 'WARNING');
            return 1;
        } elseif ($duration > 15 && $duration < 20) {
            debug_log("بازه 16-19 دقیقه: 1 توکن (نزدیک به بازه اول)", 'INFO');
            return 1;
        } elseif ($duration > 45 && $duration < 50) {
            debug_log("بازه 46-49 دقیقه: 2 توکن (نزدیک به بازه دوم)", 'INFO');
            return 2;
        } else {
            // بیش از 90 دقیقه
            debug_log("مدت زمان بیش از 90 دقیقه: 3 توکن (حداکثر)", 'INFO');
            return 3;
        }
    }
}

// ارسال پیام تلگرام با لاگ‌گذاری بهبود یافته
function sendTelegramMessage($chatId, $message) {
    error_log("📤 تلاش برای ارسال پیام به Chat ID: {$chatId}");
    error_log("📝 متن پیام: " . substr($message, 0, 100) . (strlen($message) > 100 ? '...' : ''));
    
    $botToken = BOT_TOKEN;
    $url = "https://api.telegram.org/bot{$botToken}/sendMessage";
    
    $data = [
        'chat_id' => $chatId,
        'text' => $message,
        'parse_mode' => 'HTML'
    ];
    
    // استفاده از cURL به جای file_get_contents برای حل مشکل SSL
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query($data));
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 30);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, false);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Content-Type: application/x-www-form-urlencoded'
    ]);
    
    $result = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError = curl_error($ch);
    curl_close($ch);
    
    if ($result === FALSE || !empty($curlError)) {
        error_log("❌ خطا در ارسال پیام تلگرام به {$chatId} - cURL Error: {$curlError}, HTTP Code: {$httpCode}");
        return false;
    }
    
    error_log("📡 HTTP Response Code: {$httpCode}");
    
    $response = json_decode($result, true);
    
    if (isset($response['ok']) && $response['ok'] === true) {
        error_log("✅ پیام با موفقیت به {$chatId} ارسال شد");
        return true;
    } else {
        error_log("❌ خطا در ارسال پیام تلگرام به {$chatId}: " . json_encode($response));
        return false;
    }
}

// تبدیل تاریخ میلادی به شمسی (دقیق بر اساس الگوریتم to_jalali)
function convertToJalali($gregorianDate) {
    // استفاده از تابع دقیق‌تر برای تبدیل
    $j = to_jalali($gregorianDate);
    if (!$j) return '';
    list($jy, $jm, $jd) = explode('/', $j);

    // نام ماه‌های شمسی
    $jalaliMonths = [
        1 => 'فروردین', 2 => 'اردیبهشت', 3 => 'خرداد', 4 => 'تیر',
        5 => 'مرداد', 6 => 'شهریور', 7 => 'مهر', 8 => 'آبان',
        9 => 'آذر', 10 => 'دی', 11 => 'بهمن', 12 => 'اسفند'
    ];

    $monthName = $jalaliMonths[(int)$jm] ?? (string)$jm;

    // خروجی با فرمت «سال ماه روز» برای خوانایی بهتر در پیام‌ها
    return sprintf('%04d %s %02d', (int)$jy, $monthName, (int)$jd);
}

// دریافت نام روز هفته
function getDayName($input) {
    $dayNames = [
        0 => 'یکشنبه', 1 => 'دوشنبه', 2 => 'سه‌شنبه', 3 => 'چهارشنبه',
        4 => 'پنج‌شنبه', 5 => 'جمعه', 6 => 'شنبه'
    ];

    // اگر ورودی عدد شاخص روز هفته باشد (۰ تا ۶)، مستقیماً نگاشت کن
    if ((is_int($input) || (is_numeric($input) && (int)$input == $input)) && (int)$input >= 0 && (int)$input <= 6) {
        return $dayNames[(int)$input] ?? 'نامشخص';
    }

    // در غیر این صورت ورودی را تاریخ در نظر بگیر
    $timestamp = strtotime($input);
    if ($timestamp === false) {
        return 'نامشخص';
    }
    $dayOfWeek = (int)date('w', $timestamp);
    return $dayNames[$dayOfWeek] ?? 'نامشخص';
}

// تابع بررسی امکان دریافت رزرو برای کسب‌وکار
function canBusinessReceiveBooking($pdo, $business_id) {
    if (!$pdo) {
        debug_log('عدم دسترسی به پایگاه داده برای بررسی ظرفیت رزرو', 'WARNING');
        return true; // در حالت تست اجازه می‌دهیم
    }
    
    try {
        // دریافت اطلاعات کسب‌وکار
        $stmt = $pdo->prepare("
            SELECT 
                free_reservation_tokens,
                subscription_plan_id,
                subscription_expiry_date
            FROM businesses 
            WHERE id = ?
        ");
        $stmt->execute([$business_id]);
        $business = $stmt->fetch();
        
        if (!$business) {
            debug_log('کسب‌وکار یافت نشد برای بررسی ظرفیت رزرو', 'ERROR', ['business_id' => $business_id]);
            return false;
        }
        
        // بررسی توکن‌های رایگان
        if ($business['free_reservation_tokens'] > 0) {
            debug_log('کسب‌وکار دارای توکن رایگان است', 'INFO', [
                'business_id' => $business_id,
                'free_tokens' => $business['free_reservation_tokens']
            ]);
            return true;
        }
        
        // بررسی اشتراک فعال
        if ($business['subscription_plan_id'] && $business['subscription_expiry_date']) {
            $today = date('Y-m-d');
            if ($business['subscription_expiry_date'] >= $today) {
                debug_log('کسب‌وکار دارای اشتراک فعال است', 'INFO', [
                    'business_id' => $business_id,
                    'plan_id' => $business['subscription_plan_id'],
                    'expiry_date' => $business['subscription_expiry_date']
                ]);
                return true;
            }
        }
        
        debug_log('کسب‌وکار فاقد ظرفیت رزرو است', 'WARNING', [
            'business_id' => $business_id,
            'free_tokens' => $business['free_reservation_tokens'],
            'subscription_plan_id' => $business['subscription_plan_id'],
            'subscription_expiry_date' => $business['subscription_expiry_date']
        ]);
        
        return false;
        
    } catch (PDOException $e) {
        debug_log('خطا در بررسی ظرفیت رزرو کسب‌وکار', 'ERROR', [
            'business_id' => $business_id,
            'error' => $e->getMessage()
        ]);
        return false;
    }
}

function validateTelegramData($initData) {
    error_log("🔐 === شروع اعتبارسنجی تلگرام ===");
    error_log("🔧 DEV_MODE: " . (DEV_MODE ? 'فعال' : 'غیرفعال'));
    error_log("📥 InitData دریافتی: " . ($initData ? 'موجود (' . strlen($initData) . ' کاراکتر)' : 'خالی'));
    
    if (DEV_MODE) {
        error_log("🧪 حالت توسعه - بازگشت کاربر تست");
        return ['id' => 123456789, 'first_name' => 'Test'];
    }
    
    if (empty($initData)) {
        error_log("❌ InitData خالی است");
        return false;
    }
    
    $data_check_arr = [];
    parse_str(urldecode($initData), $data_check_arr);
    error_log("📋 داده‌های تجزیه شده: " . json_encode(array_keys($data_check_arr), JSON_UNESCAPED_UNICODE));
    
    if (!isset($data_check_arr['hash'])) {
        error_log("❌ Hash موجود نیست");
        return false;
    }
    
    $received_hash = $data_check_arr['hash'];
    error_log("🔑 Hash دریافتی: " . substr($received_hash, 0, 10) . "...");
    
    unset($data_check_arr['hash']);
    ksort($data_check_arr);
    
    $data_check_string = implode("\n", array_map(fn($k, $v) => "$k=$v", array_keys($data_check_arr), $data_check_arr));
    error_log("📝 رشته بررسی: " . substr($data_check_string, 0, 100) . "...");
    
    $secret_key = hash_hmac('sha256', BOT_TOKEN, 'WebAppData', true);
    $calculated_hash = hash_hmac('sha256', $data_check_string, $secret_key);
    error_log("🔑 Hash محاسبه شده: " . substr($calculated_hash, 0, 10) . "...");
    
    if (hash_equals($calculated_hash, $received_hash)) {
        error_log("✅ Hash معتبر است");
        $user = json_decode($data_check_arr['user'], true);
        error_log("👤 کاربر تأیید شده: " . json_encode($user, JSON_UNESCAPED_UNICODE));
        return $user;
    }
    
    error_log("❌ Hash نامعتبر است");
    return false;
}

function authenticate_user() {
    error_log("🔐 === شروع احراز هویت کاربر ===");
    
    // --- احراز هویت اپلیکیشن موبایل (X-App-Token) — فقط برای اندروید ---
    if (function_exists('mobile_auth_from_request')) {
        $mobileUser = mobile_auth_from_request();
        if ($mobileUser) {
            $GLOBALS['MOBILE_APP_USER'] = $mobileUser;
            error_log("📱 احراز هویت موبایل موفق - telegram_id: " . $mobileUser['telegram_id']);
            return [
                'id' => $mobileUser['telegram_id'],
                'first_name' => $mobileUser['first_name'],
                'last_name' => $mobileUser['last_name'] ?? '',
                'username' => $mobileUser['username'] ?? '',
                'is_mobile' => true
            ];
        }
    }
    
    // شروع session اگر شروع نشده باشد
    if (session_status() == PHP_SESSION_NONE) {
        session_start();
    }
    
    // بررسی session اول
    if (isset($_SESSION['user_id']) && isset($_SESSION['telegram_id'])) {
        error_log("🎫 احراز هویت از طریق Session - User ID: " . $_SESSION['user_id'] . ", Telegram ID: " . $_SESSION['telegram_id']);
        return [
            'id' => $_SESSION['telegram_id'],
            'first_name' => $_SESSION['first_name'] ?? 'کاربر',
            'last_name' => $_SESSION['last_name'] ?? '',
            'username' => $_SESSION['username'] ?? ''
        ];
    }
    
    // بررسی Header
    $initData = $_SERVER['HTTP_X_TELEGRAM_INIT_DATA'] ?? '';
    error_log("📡 InitData از Header: " . ($initData ? 'موجود (' . strlen($initData) . ' کاراکتر)' : 'خالی'));
    
    // اگر در Header نبود، از Body بخوان
    if (empty($initData)) {
        error_log("🔍 بررسی Body برای InitData...");
        $requestBody = json_decode(file_get_contents('php://input'), true);
        
        if (isset($requestBody['telegram_init_data'])) {
            $initData = $requestBody['telegram_init_data'];
            error_log("📦 InitData از Body: موجود (" . strlen($initData) . " کاراکتر)");
        } else {
            error_log("❌ InitData در Body یافت نشد");
        }
    }
    
    // اگر initData وجود ندارد، authentication را رد کن
    if (empty($initData)) {
        error_log("⚠️ هیچ InitData یافت نشد - ادامه بدون authentication");
        return null; // بدون authentication ادامه بده
    }
    
    error_log("🔐 شروع اعتبارسنجی تلگرام...");
    $telegramUser = validateTelegramData($initData);
    
    if (!$telegramUser || !isset($telegramUser['id'])) {
        error_log("❌ احراز هویت ناموفق - کاربر نامعتبر");
        error_log("🔍 TelegramUser: " . json_encode($telegramUser, JSON_UNESCAPED_UNICODE));
        ErrorHandler::sendError('AUTH_FAILED');
    }
    
    error_log("✅ احراز هویت موفق - کاربر ID: " . $telegramUser['id']);
    return $telegramUser;
}

function to_gregorian($jalali_date_str) { 
    if (empty($jalali_date_str)) return null;
    // Handle both / and - as separators
    $normalized_date = str_replace('/', '-', $jalali_date_str);
    $parts = explode('-', $normalized_date);
    if (count($parts) !== 3) return null; // Invalid format

    list($j_y, $j_m, $j_d) = array_map('intval', $parts);

    $g_days_in_month = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]; 
    $j_days_in_month = [31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29]; 
    $jy = $j_y - 979; 
    $jm = $j_m - 1; 
    $jd = $j_d - 1; 
    $j_day_no = 365 * $jy + floor($jy / 33) * 8 + floor(($jy % 33 + 3) / 4); 
    for ($i = 0; $i < $jm; ++$i) $j_day_no += $j_days_in_month[$i]; 
    $j_day_no += $jd; 
    $g_day_no = $j_day_no + 79; 
    $gy = 1600 + 400 * floor($g_day_no / 146097); 
    $g_day_no %= 146097; 
    $leap = true; 
    if ($g_day_no >= 36525) { 
        $g_day_no--; 
        $gy += 100 * floor($g_day_no / 36524); 
        $g_day_no %= 36524; 
        if ($g_day_no >= 365) $g_day_no++; 
        else $leap = false; 
    } 
    $gy += 4 * floor($g_day_no / 1461); 
    $g_day_no %= 1461; 
    if ($g_day_no >= 366) { 
        $leap = false; 
        $g_day_no--; 
        $gy += floor($g_day_no / 365); 
        $g_day_no %= 365; 
    } 
    for ($i = 0; $g_day_no >= $g_days_in_month[$i] + ($i == 1 && $leap); $i++) $g_day_no -= $g_days_in_month[$i] + ($i == 1 && $leap); 
    $gm = $i + 1; 
    $gd = $g_day_no + 1; 
    return sprintf('%04d-%02d-%02d', $gy, $gm, $gd); 
}
function to_jalali($g_date_str) { if (empty($g_date_str)) return null; list($g_y, $g_m, $g_d) = explode('-', $g_date_str); $g_y = (int)$g_y; $g_m = (int)$g_m; $g_d = (int)$g_d; $g_days_in_month = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]; $j_days_in_month = [31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29]; $gy = $g_y - 1600; $gm = $g_m - 1; $gd = $g_d - 1; $g_day_no = 365 * $gy + floor(($gy + 3) / 4) - floor(($gy + 99) / 100) + floor(($gy + 399) / 400); for ($i = 0; $i < $gm; ++$i) $g_day_no += $g_days_in_month[$i]; if ($gm > 1 && (($gy % 4 == 0 && $gy % 100 != 0) || ($gy % 400 == 0))) ++$g_day_no; $g_day_no += $gd; $j_day_no = $g_day_no - 79; $j_np = floor($j_day_no / 12053); $j_day_no %= 12053; $jy = 979 + 33 * $j_np + 4 * floor($j_day_no / 1461); $j_day_no %= 1461; if ($j_day_no >= 366) { $jy += floor(($j_day_no - 1) / 365); $j_day_no = ($j_day_no - 1) % 365; } for ($i = 0; $i < 11 && $j_day_no >= $j_days_in_month[$i]; ++$i) $j_day_no -= $j_days_in_month[$i]; $jm = $i + 1; $jd = $j_day_no + 1; return sprintf('%04d/%02d/%02d', $jy, $jm, $jd); }

// تابع getDayName قبلاً تعریف شده است

// =================================================================
// بخش ۲: مسیریابی و پردازش درخواست
// =================================================================

error_log("🌟 === شروع پردازش درخواست API ===");
error_log("📡 Method: " . $_SERVER['REQUEST_METHOD']);
error_log("🌐 URL: " . ($_SERVER['REQUEST_URI'] ?? 'نامشخص'));
// بررسی وجود تابع getallheaders برای سازگاری با CLI
if (function_exists('getallheaders')) {
    error_log("📋 Headers: " . json_encode(getallheaders(), JSON_UNESCAPED_UNICODE));
} else {
    error_log("📋 Headers: CLI mode - headers not available");
}

$method = $_SERVER['REQUEST_METHOD'];
error_log("🔐 شروع احراز هویت...");
$telegramUser = authenticate_user();

// پردازش کاربر در دیتابیس (اختیاری)
$internal_user_id = null;

if ($telegramUser) {
    error_log("👤 === پردازش کاربر در دیتابیس ===");
    error_log("🔍 جستجوی کاربر با Telegram ID: " . $telegramUser['id']);
    
    // بررسی وجود اتصال دیتابیس
    if (!$pdo) {
        error_log("❌ اتصال دیتابیس موجود نیست - ادامه بدون دیتابیس");
        $internal_user_id = 999999; // کاربر پیش‌فرض برای حالت بدون دیتابیس
    } else {
        try {
            $userStmt = $pdo->prepare("SELECT id FROM users WHERE telegram_id = ?");
            $userStmt->execute([$telegramUser['id']]);
            $user = $userStmt->fetch();
            
            if (!$user) {
                error_log("🆕 کاربر جدید - ایجاد رکورد...");
                $insertUserStmt = $pdo->prepare("INSERT INTO users (telegram_id, first_name, last_name, username) VALUES (?, ?, ?, ?)");
                $insertUserStmt->execute([
                    $telegramUser['id'], 
                    $telegramUser['first_name'] ?? 'N/A', 
                    $telegramUser['last_name'] ?? '', 
                    $telegramUser['username'] ?? ''
                ]);
                $internal_user_id = $pdo->lastInsertId();
                error_log("✅ کاربر جدید ایجاد شد با ID: " . $internal_user_id);
            } else {
                $internal_user_id = $user['id'];
                error_log("✅ کاربر موجود یافت شد با ID: " . $internal_user_id);
            }
        } catch (PDOException $e) {
            error_log("❌ خطای دیتابیس در احراز هویت کاربر: " . $e->getMessage());
            send_json_response('error', 'Database error during user authentication.', [], 500);
        }
    }
} else {
    error_log("⚠️ === بدون احراز هویت - ادامه عملیات ===");
    // برای عملیات عمومی که نیاز به کاربر ندارند
    $internal_user_id = null;
}

error_log("🚀 === شروع مسیریابی درخواست ===");
error_log("📍 Method: " . $method . ", User ID: " . $internal_user_id);

switch ($method) {
    case 'GET':
        error_log("📥 پردازش درخواست GET");
        handle_get_request($pdo, $internal_user_id);
        break;
    case 'POST':
        error_log("📤 پردازش درخواست POST");
        handle_post_request($pdo, $internal_user_id);
        break;
    default:
        error_log("❌ Method نامعتبر: " . $method);
        send_json_response('error', 'Invalid request method.', [], 405);
}

// =================================================================
// بخش ۳: توابع پردازشگر
// =================================================================

function handle_get_request($pdo, $internal_user_id) {
    error_log("📥 === شروع handle_get_request ===");
    error_log("👤 User ID: " . $internal_user_id);
    error_log("🔍 GET Parameters: " . json_encode($_GET, JSON_UNESCAPED_UNICODE));
    
    $action = $_GET['action'] ?? '';
    // برای سازگاری با درخواست‌های مختلف، هم id و هم business_id را بررسی می‌کنیم
    $business_id = $_GET['id'] ?? $_GET['business_id'] ?? null;
    
    // موبایل: در صف‌ها، از شناسه کاربر احرازشده استفاده کن (نه پارامتر کلاینت)
    if (!empty($GLOBALS['MOBILE_APP_USER']['telegram_id']) &&
        in_array($action, ['get_active_queue', 'get_customer_queue_info'], true)) {
        $_GET['user_id'] = $GLOBALS['MOBILE_APP_USER']['telegram_id'];
    }
    
    error_log("🎯 Action: " . ($action ?: 'خالی'));
    error_log("🏢 Business ID: " . ($business_id ?: 'خالی'));
    
    // برای action های خاص که نیاز به business_id ندارند، این بررسی را رد می‌کنیم
    if (!$business_id && !in_array($action, ['health_check', 'test', 'get_bot_info', 'get_business'])) {
        error_log("❌ Business ID موجود نیست");
        send_json_response('error', 'Business ID is required.', [], 400);
    }

    if ($action === 'get_business_for_edit') {
        // بررسی وجود اتصال دیتابیس
        if (!$pdo) {
            error_log("❌ دیتابیس در دسترس نیست - بازگشت داده‌های تست");
            $test_business = [
                'id' => $business_id,
                'name' => 'کسب و کار تست',
                'description' => 'این یک کسب و کار تست است',
                'owner_id' => $internal_user_id
            ];
            send_json_response('success', 'Test business data fetched.', ['data' => $test_business]);
        }
        
        // منطق واکشی اطلاعات کسب و کار برای صفحه ویرایش
        try {
            $stmt = $pdo->prepare("SELECT * FROM businesses WHERE id = ? AND owner_id = ?");
            $stmt->execute([$business_id, $internal_user_id]);
            $business = $stmt->fetch();
            if ($business) {
                send_json_response('success', 'Business data fetched.', ['data' => $business]);
            } else {
                send_json_response('error', 'Business not found or access denied.', [], 404);
            }
        } catch (PDOException $e) {
            error_log("API GET Error: " . $e->getMessage());
            send_json_response('error', 'Server error while fetching business data.', [], 500);
        }
    }
    
    if ($action === 'get_schedule') {
        // بررسی وجود اتصال دیتابیس
        if (!$pdo) {
            error_log("❌ دیتابیس در دسترس نیست - بازگشت داده‌های تست برای schedule");
            $test_schedule_data = [
                'slot_duration_minutes' => 30,
                'schedules' => [],
                'overrides' => []
            ];
            send_json_response('success', 'Test schedule data fetched.', ['data' => $test_schedule_data]);
        }
        
        try {
            $businessStmt = $pdo->prepare("SELECT slot_duration_minutes FROM businesses WHERE id = ? AND owner_id = ?");
            $businessStmt->execute([$business_id, $internal_user_id]);
            $business = $businessStmt->fetch();
            if (!$business) {
                send_json_response('error', 'Business not found or access denied.', [], 404);
            }

            $scheduleStmt = $pdo->prepare("SELECT * FROM schedules WHERE business_id = ?");
            $scheduleStmt->execute([$business_id]);
            $schedules = $scheduleStmt->fetchAll();

            // بازیابی تاریخ‌های خاص و گروه‌بندی بازه‌های زمانی
            $overrideStmt = $pdo->prepare("SELECT * FROM schedule_overrides WHERE business_id = ? ORDER BY override_date ASC, start_time ASC");
            $overrideStmt->execute([$business_id]);
            $overrideRows = $overrideStmt->fetchAll();
            
            // گروه‌بندی بازه‌های زمانی بر اساس تاریخ
            $overrides = [];
            $groupedOverrides = [];
            
            foreach ($overrideRows as $row) {
                $jalali_date = to_jalali($row['override_date']);
                $date_key = $row['override_date'];
                
                if (!isset($groupedOverrides[$date_key])) {
                    $groupedOverrides[$date_key] = [
                        'jalali_date' => $jalali_date,
                        'override_date' => $row['override_date'],
                        'is_day_off' => $row['is_day_off'],
                        'time_slots' => []
                    ];
                }
                
                // اگر روز تعطیل نیست و زمان‌های شروع و پایان موجود است، آن را به بازه‌های زمانی اضافه کن
                if (!$row['is_day_off'] && $row['start_time'] && $row['end_time']) {
                    $groupedOverrides[$date_key]['time_slots'][] = [
                        'start_time' => $row['start_time'],
                        'end_time' => $row['end_time']
                    ];
                }
            }
            
            // تبدیل آرایه گروه‌بندی شده به آرایه ساده
            foreach ($groupedOverrides as $override) {
                $overrides[] = $override;
            }

            $response_data = [
                'slot_duration_minutes' => $business['slot_duration_minutes'],
                'schedules' => $schedules,
                'overrides' => $overrides
            ];
            send_json_response('success', 'Schedule data fetched.', ['data' => $response_data]);

        } catch (PDOException $e) {
            send_json_response('error', 'Server error while fetching schedule data.', [], 500);
        }
    }
    
    // اضافه کردن action جدید برای تقویم شمسی
    if ($action === 'get_specific_schedule') {
        // بررسی وجود اتصال دیتابیس
        if (!$pdo) {
            error_log("❌ دیتابیس در دسترس نیست - بازگشت داده‌های تست برای specific schedule");
            $test_specific_data = [
                'slot_duration_minutes' => 30,
                'specific_dates' => []
            ];
            send_json_response('success', 'Test specific schedule data fetched.', ['data' => $test_specific_data]);
        }
        
        try {
            $businessStmt = $pdo->prepare("SELECT slot_duration_minutes FROM businesses WHERE id = ? AND owner_id = ?");
            $businessStmt->execute([$business_id, $internal_user_id]);
            $business = $businessStmt->fetch();
            if (!$business) {
                send_json_response('error', 'Business not found or access denied.', [], 404);
            }

            // بازیابی تاریخ‌های خاص و گروه‌بندی بازه‌های زمانی از جدول specific_time_slots
            error_log("📊 === شروع بازیابی تاریخ‌های خاص از specific_time_slots ===");
            error_log("🔍 Business ID: " . $business_id);
            
            $slotsStmt = $pdo->prepare("SELECT * FROM specific_time_slots WHERE business_id = ? ORDER BY slot_date ASC, start_time ASC");
            $slotsStmt->execute([$business_id]);
            $slotsRows = $slotsStmt->fetchAll();

            if (empty($slotsRows)) {
                send_json_response('success', 'No specific dates found for this business.', ['data' => ['specific_dates' => []]]);
                return; // Stop execution if no slots are found
            }
            
            error_log("📋 تعداد اسلات‌های یافت شده: " . count($slotsRows));
            
            if (count($slotsRows) > 0) {
                error_log("📝 نمونه اسلات اول: " . json_encode($slotsRows[0], JSON_UNESCAPED_UNICODE));
            }
            
            // گروه‌بندی اسلات‌های زمانی بر اساس تاریخ
            $specificDates = [];
            $groupedDates = [];
            
            error_log("🔄 شروع گروه‌بندی اسلات‌ها...");
            
            foreach ($slotsRows as $index => $row) {
                error_log("📅 پردازش اسلات " . ($index + 1) . ": " . json_encode($row, JSON_UNESCAPED_UNICODE));
                
                $date_key = $row['slot_date'];
                error_log("📆 کلید تاریخ: " . $date_key);
                
                if (!isset($groupedDates[$date_key])) {
                    $isWorking = $row['is_available'] == 1; // اسلات‌های موجود به معنای روز کاری است
                    error_log("🆕 ایجاد گروه جدید برای تاریخ " . $date_key . " - وضعیت کاری: " . ($isWorking ? 'کاری' : 'تعطیل'));
                    
                    $groupedDates[$date_key] = [
                        'date' => $row['slot_date'],
                        'is_working' => true, // اگر اسلات وجود دارد، روز کاری است
                        'time_slots' => []
                    ];
                }
                
                // اضافه کردن اسلات زمانی
                if ($row['start_time'] && $row['end_time']) {
                    error_log("⏰ اضافه کردن اسلات زمانی: " . $row['start_time'] . " - " . $row['end_time']);
                    
                    $groupedDates[$date_key]['time_slots'][] = [
                        'start_time' => $row['start_time'],
                        'end_time' => $row['end_time']
                    ];
                } else {
                    error_log("⚠️ اسلات زمانی اضافه نشد - start_time: " . $row['start_time'] . ", end_time: " . $row['end_time']);
                }
            }
            
            error_log("📊 تعداد گروه‌های ایجاد شده: " . count($groupedDates));
            
            // تبدیل آرایه گروه‌بندی شده به آرایه ساده
            foreach ($groupedDates as $dateKey => $dateData) {
                error_log("📋 اضافه کردن تاریخ " . $dateKey . " با " . count($dateData['time_slots']) . " اسلات زمانی");
                $specificDates[] = $dateData;
            }
            
            error_log("📊 تعداد نهایی تاریخ‌های خاص: " . count($specificDates));

            $response_data = [
                'slot_duration_minutes' => $business['slot_duration_minutes'],
                'specific_dates' => $specificDates
            ];
            
            error_log("📤 داده‌های پاسخ نهایی: " . json_encode($response_data, JSON_UNESCAPED_UNICODE));
            error_log("📊 === پایان بازیابی تاریخ‌های خاص ===");
            
            send_json_response('success', 'Specific schedule data fetched.', ['data' => $response_data]);

        } catch (PDOException $e) {
            send_json_response('error', 'Server error while fetching specific schedule data.', [], 500);
        }
    }
    
    if ($action === 'get_services') {
        // دریافت لیست خدمات کسب‌وکار
        if (!$pdo) {
            error_log("❌ دیتابیس در دسترس نیست - بازگشت داده‌های تست برای services");
            $test_services_data = [
                'services' => []
            ];
            send_json_response('success', 'Test services data fetched.', ['data' => $test_services_data]);
        }
        
        try {
            error_log("📊 === شروع بازیابی خدمات ===");
            error_log("🔍 Business ID: " . $business_id);
            
            // بررسی وجود جدول services
            $tableCheck = $pdo->query("SHOW TABLES LIKE 'services'");
            if ($tableCheck->rowCount() == 0) {
                // ایجاد جدول services
                $createTableSQL = "
                CREATE TABLE `services` (
                  `id` int(11) NOT NULL AUTO_INCREMENT,
                  `business_id` int(11) NOT NULL COMMENT 'شناسه کسب‌وکار',
                  `name` varchar(255) NOT NULL COMMENT 'نام خدمت',
                  `description` text DEFAULT NULL COMMENT 'توضیحات خدمت',
                  `price` decimal(10,0) DEFAULT NULL COMMENT 'قیمت خدمت (تومان)',
                  `duration_minutes` int(11) DEFAULT NULL COMMENT 'مدت زمان خدمت (دقیقه)',
                  `is_active` tinyint(1) DEFAULT 1 COMMENT 'فعال/غیرفعال',
                  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'تاریخ ایجاد',
                  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'تاریخ به‌روزرسانی',
                  PRIMARY KEY (`id`),
                  KEY `business_id` (`business_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='جدول خدمات کسب‌وکارها';
                ";
                $pdo->exec($createTableSQL);
                error_log("✅ جدول services ایجاد شد در get_services");
            }
            
            $businessStmt = $pdo->prepare("SELECT id FROM businesses WHERE id = ? AND owner_id = ?");
            $businessStmt->execute([$business_id, $internal_user_id]);
            $business = $businessStmt->fetch();
            if (!$business) {
                send_json_response('error', 'Business not found or access denied.', [], 404);
            }
            
            // دریافت خدمات از جدول services
            $stmt = $pdo->prepare("
                SELECT 
                    id,
                    name,
                    description,
                    price,
                    duration_minutes,
                    is_active,
                    created_at
                FROM services 
                WHERE business_id = ? 
                ORDER BY created_at DESC
            ");
            
            $stmt->execute([$business_id]);
            $services = $stmt->fetchAll(PDO::FETCH_ASSOC);
            
            error_log("📋 تعداد خدمات یافت شده: " . count($services));
            error_log("📊 === پایان بازیابی خدمات ===");
            
            send_json_response('success', 'Services data fetched.', ['data' => ['services' => $services]]);
            
        } catch (PDOException $e) {
            error_log("❌ خطا در get_services: " . $e->getMessage());
            send_json_response('error', 'Server error while fetching services data.', [], 500);
        }
    }
    
    // دریافت اطلاعات کسب‌وکار برای صفحه رزرو (بدون نیاز به احراز هویت مالک)
    if ($action === 'get_business') {
        error_log("🏢 get_business action detected for booking page");
        error_log("🔍 Business ID: " . ($business_id ?? 'NULL'));
        
        if (!$pdo) {
            error_log("❌ دیتابیس در دسترس نیست - بازگشت داده‌های تست");
            $test_business = [
                'id' => $business_id,
                'name' => 'کسب و کار تست',
                'address' => 'آدرس تست',
                'business_type' => 'تست',
                'phone' => '09123456789',
                'owner_name' => 'مدیر تست'
            ];
            error_log("✅ Returning test business data: " . json_encode($test_business));
            send_json_response('success', 'Test business data fetched.', ['data' => $test_business]);
        }
        
        try {
            $stmt = $pdo->prepare("
                SELECT b.*, u.first_name, u.last_name, u.phone as owner_phone,
                       CONCAT(u.first_name, ' ', u.last_name) as owner_name
                FROM businesses b 
                JOIN users u ON b.owner_id = u.id 
                WHERE b.id = ? AND b.is_active = 1
            ");
            $stmt->execute([$business_id]);
            $business = $stmt->fetch();
            
            if ($business) {
                // دریافت خدمات کسب‌وکار
                $servicesStmt = $pdo->prepare("
                    SELECT id, name, description, price, duration_minutes, is_active
                    FROM services 
                    WHERE business_id = ? AND is_active = 1
                    ORDER BY created_at ASC
                ");
                $servicesStmt->execute([$business_id]);
                $services = $servicesStmt->fetchAll();
                
                $business['services'] = $services;
                
                send_json_response('success', 'Business data fetched.', ['data' => $business]);
            } else {
                send_json_response('error', 'Business not found or inactive.', [], 404);
            }
        } catch (PDOException $e) {
            error_log("API GET Error: " . $e->getMessage());
            send_json_response('error', 'Server error while fetching business data.', [], 500);
        }
    }
    elseif ($action === 'get_active_queue') {
        if (!$pdo) {
            send_json_response('error', 'Database connection required for this operation.', [], 500);
        }

        $business_id = $_GET['business_id'] ?? null;
        $viewer_user_id = isset($_GET['user_id']) ? (int)$_GET['user_id'] : null;
        $viewer_phone = $_GET['customer_phone'] ?? null;
        $viewer_booking_id = isset($_GET['booking_id']) ? (int)$_GET['booking_id'] : null;
        if (empty($business_id)) {
            send_json_response('error', 'شناسه کسب‌وکار الزامی است.', [], 400);
        }

        // --- احراز هویت: یا صاحب کسب‌وکار احراز هویت‌شده (از initData)، یا مشتری با پارامترهای شناسایی معتبر ---
        $isOwner = false;
        if (!empty($internal_user_id)) {
            try {
                $ownerCheckStmt = $pdo->prepare("SELECT owner_id FROM businesses WHERE id = ?");
                $ownerCheckStmt->execute([$business_id]);
                $ownerRow = $ownerCheckStmt->fetch(PDO::FETCH_ASSOC);
                $isOwner = $ownerRow && (int)$ownerRow['owner_id'] === (int)$internal_user_id;
            } catch (PDOException $e) {
                // نادیده گرفتن؛ در ادامه به‌عنوان غیرمالک با پارامترهای viewer بررسی می‌شود
            }
        }

        $enforceViewer = !empty($viewer_user_id) || !empty($viewer_phone) || !empty($viewer_booking_id);

        if (!$isOwner && !$enforceViewer) {
            send_json_response('error', 'دسترسی غیرمجاز.', [], 401);
        }

        try {
            // اطمینان از وجود ستون queue_status در جدول bookings
            try {
                $colStmt = $pdo->prepare("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bookings' AND COLUMN_NAME = 'queue_status'");
                $colStmt->execute();
                if ($colStmt->fetchColumn() == 0) {
                    $pdo->exec("ALTER TABLE bookings ADD COLUMN queue_status ENUM('waiting','in_progress','completed') NOT NULL DEFAULT 'waiting', ADD COLUMN progress_updated_at DATETIME NULL");
                }
            } catch (PDOException $e) {
                // نادیده گرفتن در صورت عدم پشتیبانی از INFORMATION_SCHEMA (مانند SQLite)
            }

            // میانگین زمان خدمات تکمیل‌شده در ۳۰ روز اخیر
            $avgStmt = $pdo->prepare("SELECT COALESCE(AVG(duration_minutes), 0) FROM bookings WHERE business_id = ? AND status != 'cancelled' AND queue_status = 'completed' AND booking_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)");
            $avgStmt->execute([$business_id]);
            $avgDuration = (float)$avgStmt->fetchColumn();
            if ($avgDuration <= 0) {
                // fallback به متوسط زمان خدمت تعریف‌شده در services
                $svcAvgStmt = $pdo->prepare("SELECT COALESCE(AVG(duration_minutes), 0) FROM services WHERE business_id = ? AND is_active = 1");
                $svcAvgStmt->execute([$business_id]);
                $avgDuration = (float)$svcAvgStmt->fetchColumn();
                if ($avgDuration <= 0) $avgDuration = 20; // پیش‌فرض
            }

            // مدت استاندارد اسلات کسب‌وکار
            $bizStmt = $pdo->prepare("SELECT slot_duration_minutes FROM businesses WHERE id = ?");
            $bizStmt->execute([$business_id]);
            $biz = $bizStmt->fetch(PDO::FETCH_ASSOC);
            $slotDuration = (int)($biz['slot_duration_minutes'] ?? 0);
            if ($slotDuration <= 0) $slotDuration = max(5, (int)round($avgDuration));

            if ($enforceViewer) {
                if (!empty($viewer_booking_id)) {
                    $chkStmt = $pdo->prepare("SELECT b.id, b.customer_phone, u.telegram_id FROM bookings b LEFT JOIN users u ON b.user_id = u.id WHERE b.id = ? AND b.business_id = ? AND b.status != 'cancelled' AND b.queue_status IN ('waiting','in_progress')");
                    $chkStmt->execute([$viewer_booking_id, $business_id]);
                    $chk = $chkStmt->fetch(PDO::FETCH_ASSOC);
                    if (!$chk) {
                        send_json_response('error', 'NO_ACTIVE_BOOKING', [], 403);
                    }
                    $telegramMatch = (!empty($viewer_user_id) && (int)$chk['telegram_id'] === (int)$viewer_user_id);
                    $phoneMatch = (!empty($viewer_phone) && $viewer_phone === $chk['customer_phone']);
                    if (!$telegramMatch && !$phoneMatch) {
                        send_json_response('error', 'NO_ACTIVE_BOOKING', [], 403);
                    }
                } else {
                    $existStmt = $pdo->prepare("SELECT COUNT(*) FROM bookings b LEFT JOIN users u ON b.user_id = u.id WHERE b.business_id = ? AND b.status != 'cancelled' AND b.booking_date >= CURDATE() AND b.queue_status IN ('waiting','in_progress') AND ((? IS NOT NULL AND u.telegram_id = ?) OR (? IS NOT NULL AND b.customer_phone = ?))");
                    $existStmt->execute([$business_id, $viewer_user_id, $viewer_user_id, $viewer_phone, $viewer_phone]);
                    $exists = (int)$existStmt->fetchColumn();
                    if ($exists === 0) {
                        send_json_response('error', 'NO_ACTIVE_BOOKING', [], 403);
                    }
                }
            }

            // دریافت صف فعال
            $queueStmt = $pdo->prepare("SELECT b.id, b.user_id, u.telegram_id as user_telegram_id, b.customer_name, b.customer_phone, b.booking_date, b.start_time, b.end_time, b.duration_minutes, b.status, b.queue_status, b.progress_updated_at FROM bookings b LEFT JOIN users u ON b.user_id = u.id WHERE b.business_id = ? AND b.status != 'cancelled' AND b.booking_date >= CURDATE() AND b.queue_status IN ('waiting','in_progress') ORDER BY b.booking_date ASC, b.start_time ASC, b.id ASC");
            $queueStmt->execute([$business_id]);
            $rows = $queueStmt->fetchAll(PDO::FETCH_ASSOC);

            $inProgress = [];
            $waiting = [];
            foreach ($rows as $r) {
                if (($r['queue_status'] ?? 'waiting') === 'in_progress') $inProgress[] = $r; else $waiting[] = $r;
            }
            $current = count($inProgress) ? $inProgress[0] : null;
            $remainingCurrent = 0;
            if ($current) {
                $dur = (float)($current['duration_minutes'] ?? 0);
                if ($dur <= 0) $dur = $avgDuration;
                $elapsed = 0;
                if (!empty($current['progress_updated_at'])) {
                    $elapsed = max(0, (int)round((time() - strtotime($current['progress_updated_at'])) / 60));
                }
                $remainingCurrent = max(0, (int)round($dur - $elapsed));
                if ($remainingCurrent <= 0) {
                    $remainingCurrent = $slotDuration;
                }
            }

            $ordered = [];
            if ($current) $ordered[] = $current;
            foreach ($waiting as $w) $ordered[] = $w;

            $resultRows = [];
            $pos = 0;
            $waitingIndex = 0;
            foreach ($ordered as $row) {
                $pos++;
                $etaMinutes = 0;
                if (($row['queue_status'] ?? 'waiting') === 'waiting') {
                    $etaMinutes = max($slotDuration, (int)round($remainingCurrent + $waitingIndex * $avgDuration));
                    $waitingIndex++;
                }
                // فقط صاحب کسب‌وکار یا خودِ صاحب رزرو شمارهٔ تماس را می‌بیند؛ بقیهٔ کاربران در صف فقط نام و موقعیت را می‌بینند
                $isSelfRow = (!empty($viewer_user_id) && (int)($row['user_telegram_id'] ?? 0) === (int)$viewer_user_id)
                    || (!empty($viewer_phone) && ($row['customer_phone'] ?? null) === $viewer_phone);
                $canSeePhone = $isOwner || $isSelfRow;
                $resultRows[] = [
                    'id' => (int)$row['id'],
                    'customer_name' => $row['customer_name'],
                    'customer_phone' => $canSeePhone ? $row['customer_phone'] : null,
                    'booking_date' => $row['booking_date'],
                    'start_time' => $row['start_time'],
                    'end_time' => $row['end_time'],
                    'status' => $row['status'],
                    'queue_status' => $row['queue_status'] ?? 'waiting',
                    'user_id' => isset($row['user_id']) ? (int)$row['user_id'] : null,
                    'user_telegram_id' => isset($row['user_telegram_id']) ? (int)$row['user_telegram_id'] : null,
                    'position' => $pos,
                    'eta_minutes' => $etaMinutes
                ];
            }

            send_json_response('success', 'صف فعال بازیابی شد.', [
                'queue' => $resultRows,
                'avg_duration_minutes' => $avgDuration
            ]);
        } catch (PDOException $e) {
            error_log('Error in get_active_queue: ' . $e->getMessage());
            send_json_response('error', 'خطا در دریافت صف فعال.', [], 500);
        }
    }
    elseif ($action === 'get_customer_queue_info') {
        if (!$pdo) {
            send_json_response('error', 'Database connection required for this operation.', [], 500);
        }

        $business_id = $_GET['business_id'] ?? null;
        $user_id = $_GET['user_id'] ?? null;
        $customer_phone = $_GET['customer_phone'] ?? null;
        if (empty($business_id)) {
            send_json_response('error', 'شناسه کسب‌وکار الزامی است.', [], 400);
        }

        try {
            // میانگین مدت‌زمان تکمیل
            $avgStmt = $pdo->prepare("SELECT COALESCE(AVG(duration_minutes), 0) FROM bookings WHERE business_id = ? AND status != 'cancelled' AND queue_status = 'completed' AND booking_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)");
            $avgStmt->execute([$business_id]);
            $avgDuration = (float)$avgStmt->fetchColumn();
            if ($avgDuration <= 0) {
                $svcAvgStmt = $pdo->prepare("SELECT COALESCE(AVG(duration_minutes), 0) FROM services WHERE business_id = ? AND is_active = 1");
                $svcAvgStmt->execute([$business_id]);
                $avgDuration = (float)$svcAvgStmt->fetchColumn();
                if ($avgDuration <= 0) $avgDuration = 20;
            }

            // مدت استاندارد اسلات کسب‌وکار (برای محاسبه زمان تقریبی)
            $bizStmt = $pdo->prepare("SELECT slot_duration_minutes FROM businesses WHERE id = ?");
            $bizStmt->execute([$business_id]);
            $biz = $bizStmt->fetch(PDO::FETCH_ASSOC);
            $slotDuration = (int)($biz['slot_duration_minutes'] ?? 0);
            if ($slotDuration <= 0) $slotDuration = max(5, (int)round($avgDuration));

            // صف فعال
            $queueStmt = $pdo->prepare("SELECT b.id, b.user_id, u.telegram_id as user_telegram_id, b.customer_phone, b.queue_status, b.progress_updated_at, b.duration_minutes FROM bookings b LEFT JOIN users u ON b.user_id = u.id WHERE b.business_id = ? AND b.status != 'cancelled' AND b.booking_date >= CURDATE() AND b.queue_status IN ('waiting','in_progress') ORDER BY b.booking_date ASC, b.start_time ASC, b.id ASC");
            $queueStmt->execute([$business_id]);
            $rows = $queueStmt->fetchAll(PDO::FETCH_ASSOC);

            $inProgress = [];
            $waiting = [];
            foreach ($rows as $r) {
                if (($r['queue_status'] ?? 'waiting') === 'in_progress') $inProgress[] = $r; else $waiting[] = $r;
            }
            $current = count($inProgress) ? $inProgress[0] : null;
            $remainingCurrent = 0;
            if ($current) {
                $dur = (float)($current['duration_minutes'] ?? 0);
                if ($dur <= 0) $dur = $avgDuration;
                $elapsed = 0;
                if (!empty($current['progress_updated_at'])) {
                    $elapsed = max(0, (int)round((time() - strtotime($current['progress_updated_at'])) / 60));
                }
                $remainingCurrent = max(0, (int)round($dur - $elapsed));
                if ($remainingCurrent <= 0) {
                    $remainingCurrent = $slotDuration;
                }
            }

            $position = null;
            $eta = null;
            $foundId = null;
            $posCounter = 0;
            if ($current) {
                $posCounter++;
                if (!empty($user_id) && !empty($current['user_telegram_id']) && (int)$current['user_telegram_id'] === (int)$user_id) {
                    $position = 1;
                    $eta = 0;
                    $foundId = (int)$current['id'];
                } elseif (!empty($customer_phone) && !empty($current['customer_phone']) && $current['customer_phone'] === $customer_phone) {
                    $position = 1;
                    $eta = 0;
                    $foundId = (int)$current['id'];
                }
            }

            $waitingIndex = 0;
            foreach ($waiting as $row) {
                $posCounter++;
                $isMatch = false;
                if (!empty($user_id) && !empty($row['user_telegram_id']) && (int)$row['user_telegram_id'] === (int)$user_id) $isMatch = true;
                elseif (!empty($customer_phone) && !empty($row['customer_phone']) && $row['customer_phone'] === $customer_phone) $isMatch = true;
                if ($isMatch && $position === null) {
                    $position = $posCounter;
                    $eta = max($slotDuration, (int)round($remainingCurrent + $waitingIndex * $avgDuration));
                    $foundId = (int)$row['id'];
                }
                $waitingIndex++;
            }

            send_json_response('success', 'اطلاعات صف کاربر بازیابی شد.', [
                'position' => $position,
                'eta_minutes' => $eta,
                'booking_id' => $foundId,
                'avg_duration_minutes' => $avgDuration,
                'total_active' => count($rows)
            ]);
        } catch (PDOException $e) {
            error_log('Error in get_customer_queue_info: ' . $e->getMessage());
            send_json_response('error', 'خطا در دریافت اطلاعات صف.', [], 500);
        }
    }
    
    // دریافت زمان‌های در دسترس برای رزرو
    if ($action === 'get_available_slots') {
        error_log("🎯 get_available_slots action detected");
        error_log("📅 Date parameter: " . ($_GET['date'] ?? 'NULL'));
        error_log("🏢 Business ID: " . ($business_id ?? 'NULL'));
        
        $date = $_GET['date'] ?? null;
        
        if (!$date) {
            error_log("❌ Date parameter missing");
            send_json_response('error', 'Date parameter is required.', [], 400);
        }
        
        if (!$pdo) {
            error_log("❌ دیتابیس در دسترس نیست - بازگشت داده‌های تست");
            $test_slots = [
                ['start_time' => '09:00', 'end_time' => '09:30'],
                ['start_time' => '09:30', 'end_time' => '10:00'],
                ['start_time' => '10:00', 'end_time' => '10:30'],
                ['start_time' => '10:30', 'end_time' => '11:00'],
                ['start_time' => '11:00', 'end_time' => '11:30'],
                ['start_time' => '14:00', 'end_time' => '14:30'],
                ['start_time' => '14:30', 'end_time' => '15:00'],
                ['start_time' => '15:00', 'end_time' => '15:30']
            ];
            error_log("✅ Returning test slots: " . json_encode($test_slots));
            send_json_response('success', 'Test slots fetched.', ['data' => $test_slots]);
        }
        
        try {
            // بررسی وجود کسب‌وکار
            $businessStmt = $pdo->prepare("SELECT slot_duration_minutes FROM businesses WHERE id = ? AND is_active = 1");
            $businessStmt->execute([$business_id]);
            $business = $businessStmt->fetch();
            
            if (!$business) {
                send_json_response('error', 'Business not found or inactive.', [], 404);
            }
            
            $slotDuration = $business['slot_duration_minutes'] ?? 30;
            
            // دریافت زمان‌بندی روز هفته
            $dayOfWeek = date('w', strtotime($date)); // 0=یکشنبه، 6=شنبه
            error_log("📅 Day of week for {$date}: {$dayOfWeek}");
            
            $scheduleStmt = $pdo->prepare("
                SELECT start_time, end_time 
                FROM schedules 
                WHERE business_id = ? AND day_of_week = ? AND is_active = 1
            ");
            $scheduleStmt->execute([$business_id, $dayOfWeek]);
            $schedule = $scheduleStmt->fetch();
            error_log("📋 Schedule found: " . ($schedule ? json_encode($schedule) : 'NULL'));
            
            // بررسی تغییرات خاص روز
            $overrideStmt = $pdo->prepare("
                SELECT start_time, end_time, is_day_off 
                FROM schedule_overrides 
                WHERE business_id = ? AND override_date = ?
            ");
            $overrideStmt->execute([$business_id, $date]);
            $override = $overrideStmt->fetch();
            error_log("📋 Override found: " . ($override ? json_encode($override) : 'NULL'));
            
            // بررسی اسلات‌های خاص از جدول specific_time_slots
            $specificSlotsStmt = $pdo->prepare("
                SELECT start_time, end_time, is_available 
                FROM specific_time_slots 
                WHERE business_id = ? AND slot_date = ? AND is_available = 1
                ORDER BY start_time ASC
            ");
            $specificSlotsStmt->execute([$business_id, $date]);
            $specificSlots = $specificSlotsStmt->fetchAll();
            error_log("📋 Specific slots found: " . count($specificSlots) . " slots");
            
            // اگر اسلات‌های خاص وجود دارد، از آنها استفاده کن
            if (!empty($specificSlots)) {
                error_log("✅ Using specific time slots");
                $slots = [];
                foreach ($specificSlots as $slot) {
                    $slotStart = substr($slot['start_time'], 0, 5); // HH:MM format
                    $slotEnd = substr($slot['end_time'], 0, 5);
                    
                    // بررسی رزرو موجود برای این اسلات خاص
                    $bookingStmt = $pdo->prepare("
                        SELECT id FROM bookings 
                        WHERE business_id = ? AND booking_date = ? AND start_time = ? AND status != 'cancelled'
                    ");
                    $bookingStmt->execute([$business_id, $date, $slotStart]);
                    $isBooked = $bookingStmt->fetch() !== false;
                    
                    // فقط اسلات‌های رزرو نشده را اضافه کن
                    if (!$isBooked) {
                        $slots[] = [
                            'start_time' => $slotStart,
                            'end_time' => $slotEnd
                        ];
                    } else {
                        error_log("🚫 Slot {$slotStart}-{$slotEnd} is already booked, excluding from available slots");
                    }
                }
                
                error_log("📊 Total available slots after filtering: " . count($slots));
                send_json_response('success', 'Available slots fetched from specific schedule.', ['data' => $slots]);
                return;
            }
            
            // اگر تغییر خاص وجود دارد، از آن استفاده کن
            if ($override) {
                if ($override['is_day_off']) {
                    error_log("🚫 Day off according to override");
                    send_json_response('success', 'No slots available - day off.', ['data' => []]);
                    return;
                }
                $workStart = $override['start_time'];
                $workEnd = $override['end_time'];
                error_log("⏰ Using override schedule: {$workStart} - {$workEnd}");
            } elseif ($schedule) {
                $workStart = $schedule['start_time'];
                $workEnd = $schedule['end_time'];
                error_log("⏰ Using regular schedule: {$workStart} - {$workEnd}");
            } else {
                error_log("❌ No schedule found for business {$business_id} on day {$dayOfWeek}");
                send_json_response('success', 'No schedule found for this day.', ['data' => []]);
                return;
            }
            
            // تولید اسلات‌های زمانی (فقط زمان‌های آینده)
            error_log("🔧 Generating time slots from {$workStart} to {$workEnd} with {$slotDuration} min duration");
            $slots = [];
            $currentTime = strtotime($workStart);
            $endTime = strtotime($workEnd);
            
            if ($currentTime === false || $endTime === false) {
                error_log("❌ Invalid time format: start={$workStart}, end={$workEnd}");
                send_json_response('error', 'Invalid schedule time format.', [], 500);
                return;
            }
            
            // اگر تاریخ امروز است، زمان شروع را به زمان فعلی تنظیم کن
            $today = date('Y-m-d');
            $currentDateTime = time();
            if ($date === $today) {
                $currentHour = date('H:i');
                $minStartTime = strtotime($currentHour);
                if ($minStartTime > $currentTime) {
                    $currentTime = $minStartTime;
                    // گرد کردن به نزدیک‌ترین اسلات
                    $remainder = $currentTime % ($slotDuration * 60);
                    if ($remainder > 0) {
                        $currentTime += ($slotDuration * 60) - $remainder;
                    }
                }
                error_log("📅 امروز است - زمان شروع تنظیم شد به: " . date('H:i', $currentTime));
            }
            
            $slotCount = 0;
            while ($currentTime < $endTime) {
                $slotStart = date('H:i', $currentTime);
                $slotEnd = date('H:i', $currentTime + ($slotDuration * 60));
                
                // بررسی رزرو موجود
                $bookingStmt = $pdo->prepare("
                    SELECT id FROM bookings 
                    WHERE business_id = ? AND booking_date = ? AND start_time = ? AND status != 'cancelled'
                ");
                $bookingStmt->execute([$business_id, $date, $slotStart]);
                $isBooked = $bookingStmt->fetch() !== false;
                
                // اضافه کردن اسلات اگر در دسترس باشد
                if (!$isBooked && ($currentTime + ($slotDuration * 60)) <= $endTime) {
                    $slots[] = [
                        'start_time' => $slotStart,
                        'end_time' => $slotEnd
                    ];
                    $slotCount++;
                }
                
                $currentTime += ($slotDuration * 60);
            }
            
            error_log("✅ Generated {$slotCount} available slots");
            
            send_json_response('success', 'Available slots fetched.', ['data' => $slots]);
            
        } catch (PDOException $e) {
            error_log("❌ PDO Error in get_available_slots: " . $e->getMessage());
            error_log("📍 Error Code: " . $e->getCode());
            error_log("📍 Error File: " . $e->getFile() . " Line: " . $e->getLine());
            error_log("🔍 Business ID: " . $business_id . ", Date: " . $date);
            debug_log('خطای PDO در get_available_slots', 'ERROR', [
                'error_message' => $e->getMessage(),
                'error_code' => $e->getCode(),
                'business_id' => $business_id,
                'date' => $date,
                'file' => $e->getFile(),
                'line' => $e->getLine()
            ]);
            send_json_response('error', 'Server error while fetching available slots.', [], 500);
        } catch (Exception $e) {
            error_log("❌ General Error in get_available_slots: " . $e->getMessage());
            error_log("📍 Error File: " . $e->getFile() . " Line: " . $e->getLine());
            debug_log('خطای عمومی در get_available_slots', 'ERROR', [
                'error_message' => $e->getMessage(),
                'business_id' => $business_id,
                'date' => $date,
                'file' => $e->getFile(),
                'line' => $e->getLine()
            ]);
            send_json_response('error', 'Server error while fetching available slots.', [], 500);
        }
    }

    // دریافت تاریخ‌های موجود برای رزرو
    if ($action === 'get_available_dates') {
        error_log("📅 get_available_dates action detected");
        error_log("🏢 Business ID: " . ($business_id ?? 'NULL'));
        
        if (!$pdo) {
            error_log("❌ دیتابیس در دسترس نیست - بازگشت داده‌های تست");
            $today = date('Y-m-d');
            $test_dates = [
                ['date' => $today, 'jalali_date' => convertToJalali($today), 'day_name' => getDayName($today)],
                ['date' => date('Y-m-d', strtotime($today . ' +1 day')), 'jalali_date' => convertToJalali(date('Y-m-d', strtotime($today . ' +1 day'))), 'day_name' => getDayName(date('Y-m-d', strtotime($today . ' +1 day')))],
                ['date' => date('Y-m-d', strtotime($today . ' +2 days')), 'jalali_date' => convertToJalali(date('Y-m-d', strtotime($today . ' +2 days'))), 'day_name' => getDayName(date('Y-m-d', strtotime($today . ' +2 days')))]
            ];
            send_json_response('success', 'Test dates fetched.', ['data' => $test_dates]);
        }
        
        try {
            // بررسی وجود کسب‌وکار
            $businessStmt = $pdo->prepare("SELECT id FROM businesses WHERE id = ? AND is_active = 1");
            $businessStmt->execute([$business_id]);
            $business = $businessStmt->fetch();
            
            if (!$business) {
                send_json_response('error', 'Business not found or inactive.', [], 404);
            }
            
            // دریافت تاریخ‌های موجود از specific_time_slots (فقط امروز به بعد)
            $today = date('Y-m-d');
            $datesStmt = $pdo->prepare("
                SELECT DISTINCT slot_date, COUNT(*) as slot_count
                FROM specific_time_slots 
                WHERE business_id = ? AND slot_date >= ? AND is_available = 1
                GROUP BY slot_date 
                ORDER BY slot_date ASC
                LIMIT 30
            ");
            $datesStmt->execute([$business_id, $today]);
            $availableDates = $datesStmt->fetchAll();
            
            // اگر تاریخ خاصی وجود ندارد، از schedules استفاده کن (فقط امروز به بعد)
            if (empty($availableDates)) {
                $dates = [];
                
                // تولید 30 روز آینده بر اساس schedules (شروع از امروز)
                for ($i = 0; $i < 30; $i++) {
                    $checkDate = date('Y-m-d', strtotime($today . ' +' . $i . ' days'));
                    $dayOfWeek = date('w', strtotime($checkDate));
                    
                    error_log("🔍 بررسی تاریخ: {$checkDate} (روز هفته: {$dayOfWeek})");
                    
                    // بررسی وجود schedule برای این روز
                    $scheduleStmt = $pdo->prepare("
                        SELECT id FROM schedules 
                        WHERE business_id = ? AND day_of_week = ? AND is_working = 1
                    ");
                    $scheduleStmt->execute([$business_id, $dayOfWeek]);
                    
                    if ($scheduleStmt->fetch()) {
                        $dates[] = [
                            'date' => $checkDate,
                            'jalali_date' => to_jalali($checkDate),
                            'day_name' => getDayName($dayOfWeek)
                        ];
                    }
                }
                
                send_json_response('success', 'Available dates from schedule.', ['data' => $dates]);
            } else {
                // تبدیل تاریخ‌های موجود به فرمت مناسب
                $dates = [];
                foreach ($availableDates as $dateRow) {
                    $dates[] = [
                        'date' => $dateRow['slot_date'],
                        'jalali_date' => to_jalali($dateRow['slot_date']),
                        'day_name' => getDayName(date('w', strtotime($dateRow['slot_date']))),
                        'slot_count' => $dateRow['slot_count']
                    ];
                }
                
                send_json_response('success', 'Available dates fetched.', ['data' => $dates]);
            }
            
        } catch (PDOException $e) {
            error_log("❌ PDO Error in get_available_dates: " . $e->getMessage());
            send_json_response('error', 'Server error while fetching available dates.', [], 500);
        }
    }
    
    if ($action === 'get_bot_info') {
        // دریافت اطلاعات ربات
        send_json_response('success', 'Bot info retrieved successfully.', [
            'bot_username' => BOT_USERNAME
        ]);
    }
    
    // لیست کسب‌وکارهای کاربر احرازشده (برای پنل مالک در اپلیکیشن موبایل)
    if ($action === 'get_my_businesses') {
        if (!$pdo) {
            send_json_response('error', 'Database connection required for this operation.', [], 500);
        }
        if (empty($internal_user_id)) {
            send_json_response('error', 'احراز هویت الزامی است.', [], 401);
        }
        try {
            $stmt = $pdo->prepare("
                SELECT b.id, b.name, b.business_type, b.province, b.city, b.address, b.phone,
                       b.is_active, b.slot_duration_minutes, b.advance_booking_days,
                       b.link_alias, b.subscription_plan_id, b.subscription_expiry_date,
                       b.free_reservation_tokens,
                       (SELECT COUNT(*) FROM services s WHERE s.business_id = b.id AND s.is_active = 1) AS services_count,
                       (SELECT COUNT(*) FROM bookings bk WHERE bk.business_id = b.id AND bk.status != 'cancelled') AS bookings_count,
                       sp.name AS subscription_plan_name
                FROM businesses b
                LEFT JOIN SubscriptionPlans sp ON sp.id = b.subscription_plan_id
                WHERE b.owner_id = ?
                ORDER BY b.created_at DESC
            ");
            $stmt->execute([$internal_user_id]);
            $businesses = $stmt->fetchAll();
            send_json_response('success', 'لیست کسب‌وکارها دریافت شد.', [
                'businesses' => $businesses,
                'total_count' => count($businesses)
            ]);
        } catch (PDOException $e) {
            error_log('Error in get_my_businesses: ' . $e->getMessage());
            send_json_response('error', 'خطا در دریافت لیست کسب‌وکارها.', [], 500);
        }
    }
    
    send_json_response('error', 'Invalid GET action.', [], 400);
} 

function handle_post_request($pdo, $internal_user_id) {
    error_log("🚀 === شروع handle_post_request ===");
    error_log("👤 Internal User ID: " . $internal_user_id);
    
    // اجرای حذف دوره‌ای رزروهای منقضی شده
    periodicCleanup($pdo);
    
    // خواندن داده‌های ورودی
    $rawInput = file_get_contents('php://input');
    error_log("📥 داده‌های خام دریافتی: " . $rawInput);
    error_log("📏 طول داده‌های خام: " . strlen($rawInput));
    
    if (empty($rawInput)) {
        error_log("⚠️ داده‌های POST خالی است - احتمالاً مشکل در ارسال درخواست");
        send_json_response('error', 'No data received in POST request.', [], 400);
    }
    
    $formData = json_decode($rawInput, true);
    
    if (!$formData) {
        error_log("❌ خطا در تجزیه JSON - داده‌های نامعتبر");
        error_log("🔍 JSON Error: " . json_last_error_msg());
        error_log("🔍 Raw Input: " . $rawInput);
        send_json_response('error', 'Invalid JSON data received: ' . json_last_error_msg(), [], 400);
    }
    
    error_log("✅ JSON تجزیه شد با موفقیت");
    error_log("📦 داده‌های تجزیه شده: " . json_encode($formData, JSON_UNESCAPED_UNICODE));

    // *** تعیین اکشن - ابتدا از formData، سپس از URL parameter ***
    $action = $formData['action'] ?? $_GET['action'] ?? 'update_business';
    
    // موبایل: شناسه کاربر همیشه از نشست احرازشده گرفته می‌شود — هرگز از بدنه درخواست
    if (!empty($GLOBALS['MOBILE_APP_USER']['telegram_id'])) {
        $formData['user_id'] = $GLOBALS['MOBILE_APP_USER']['telegram_id'];
        $_GET['user_id'] = $GLOBALS['MOBILE_APP_USER']['telegram_id'];
    }
    
    error_log("🎯 Action تعیین شده: " . $action);
    error_log("📦 formData['action']: " . ($formData['action'] ?? 'NULL'));
    error_log("🌐 _GET['action']: " . ($_GET['action'] ?? 'NULL'));
    error_log("📋 کل formData: " . json_encode($formData, JSON_UNESCAPED_UNICODE));
    error_log("🔍 کل _GET: " . json_encode($_GET, JSON_UNESCAPED_UNICODE));
    
    // ==========================================================
    // === کد جدید برای اضافه شدن - شروع ===
    // ==========================================================
    // برای عملیاتی که حتما نیاز به مالک مشخص دارند، احراز هویت را چک می‌کنیم
    $owner_required_actions = ['add_business', 'update_business', 'update_schedule', 'update_specific_schedule', 'update_services'];
    if (in_array($action, $owner_required_actions)) {
        if (empty($internal_user_id)) {
            error_log("❌ کاربر احراز هویت نشده برای انجام عملیات: {$action}");
            ErrorHandler::sendError('AUTH_REQUIRED');
        }
    }
    // ==========================================================
    // === کد جدید برای اضافه شدن - پایان ===
    // ==========================================================
    
    // بررسی هر دو نام پارامتر برای سازگاری (businessId و business_id)
    $business_id = $formData['businessId'] ?? $formData['business_id'] ?? $_GET['business_id'] ?? $_GET['id'] ?? null;
    
    error_log("🎯 Action: " . $action);
    error_log("🏢 Business ID: " . ($business_id ?? 'NULL'));
    error_log("📦 formData['businessId']: " . ($formData['businessId'] ?? 'NULL'));
    error_log("📦 formData['business_id']: " . ($formData['business_id'] ?? 'NULL'));
    error_log("🌐 _GET['business_id']: " . ($_GET['business_id'] ?? 'NULL'));
    error_log("🌐 _GET['id']: " . ($_GET['id'] ?? 'NULL'));
    
    // برای add_business نیازی به business_id نیست، چون کسب‌وکار جدید ایجاد می‌شود
    // get_bot_info، get_subscription_plans، get_marketer_dashboard و get_user_bookings نیازی به business_id ندارند
    $exempt_actions = ['add_business', 'get_bot_info', 'get_subscription_plans', 'get_marketer_dashboard', 'get_user_bookings'];
    error_log("📝 اکشن‌های معاف از business_id: " . json_encode($exempt_actions, JSON_UNESCAPED_UNICODE));
    error_log("✅ آیا action معاف است؟ " . (in_array($action, $exempt_actions) ? 'بله' : 'خیر'));
    
    if (!in_array($action, $exempt_actions) && !$business_id) {
        error_log("❌ Business ID موجود نیست برای action: " . $action);
        send_json_response('error', 'Business ID is required.', [], 400);
    }
    
    // بررسی وجود اتصال دیتابیس
    if (!$pdo) {
        error_log("❌ اتصال دیتابیس موجود نیست در POST");
        if (DEV_MODE) {
            error_log("🧪 DEV_MODE: رد کردن بررسی مالکیت");
            // در حالت توسعه، مالکیت را تأیید می‌کنیم
        } else {
            // برای create_booking، اجازه ادامه بدهیم تا بخش تست اجرا شود
            if ($action !== 'create_booking') {
                send_json_response('error', 'Database connection failed.', [], 500);
            }
        }
    } else {
        // برای add_business، create_booking، cancel_booking، get_bot_info، get_subscription_status، get_subscription_plans، get_marketer_dashboard و get_user_bookings نیازی به بررسی مالکیت نیست
        if ($action !== 'add_business' && $action !== 'create_booking' && $action !== 'cancel_booking' && $action !== 'get_bot_info' && $action !== 'get_subscription_status' && $action !== 'get_subscription_plans' && $action !== 'get_marketer_dashboard' && $action !== 'get_user_bookings') {
            try {
                error_log("🔍 بررسی مالکیت کسب‌وکار...");
                $checkStmt = $pdo->prepare("SELECT id FROM businesses WHERE id = ? AND owner_id = ?");
            $checkStmt->execute([$business_id, $internal_user_id]);
            $businessOwner = $checkStmt->fetchColumn();
            
            if ($businessOwner === false) {
                error_log("❌ دسترسی رد شد - کسب‌وکار متعلق به کاربر نیست");
                error_log("🔍 Business ID: " . $business_id . ", User ID: " . $internal_user_id);
                send_json_response('error', 'Access Denied.', [], 403);
            }
            
            error_log("✅ مالکیت کسب‌وکار تأیید شد");
            
            } catch (PDOException $e) {
                error_log("❌ خطای دیتابیس در بررسی مالکیت: " . $e->getMessage());
                send_json_response('error', 'Server error during ownership verification.', [], 500);
            }
        } else {
            error_log("🆕 " . $action . ": رد کردن بررسی مالکیت");
        }
    }

    // *** استفاده از ساختار if/elseif برای جداسازی کامل منطق ***
    if ($action === 'add_business') {
        // منطق ایجاد کسب‌وکار جدید
        if (!$pdo) {
            error_log("❌ اتصال دیتابیس موجود نیست برای add_business");
            send_json_response('error', 'Database connection required for this operation.', [], 500);
        }
        
        error_log("🆕 شروع ایجاد کسب‌وکار جدید");
        
        // اعتبارسنجی فیلدهای ضروری
        $required_fields = ['name', 'type', 'phone', 'province', 'city'];
        foreach ($required_fields as $field) {
            if (empty($formData[$field]) || trim($formData[$field]) === '') {
                error_log("❌ فیلد ضروری خالی است: " . $field);
                send_json_response('error', "فیلد {$field} الزامی است.", [], 400);
            }
        }
        
        // اعتبارسنجی شماره تلفن (باید با 09 شروع شود و 11 رقم باشد)
        $phone = trim($formData['phone']);
        if (!preg_match('/^09[0-9]{9}$/', $phone)) {
            error_log("❌ شماره تلفن نامعتبر: " . $phone);
            send_json_response('error', 'شماره تلفن باید با 09 شروع شده و 11 رقم باشد.', [], 400);
        }
        
        try {
            // شروع تراکنش دیتابیس
            $pdo->beginTransaction();
            
            // بررسی تعداد کسب‌وکارهای موجود کاربر
            $businessCountStmt = $pdo->prepare("SELECT COUNT(*) FROM businesses WHERE owner_id = ?");
            $businessCountStmt->execute([$internal_user_id]);
            $businessCount = $businessCountStmt->fetchColumn();
            
            // اگر کاربر بیش از یک کسب‌وکار دارد، باید اشتراک پرو داشته باشد
            if ($businessCount >= 1) {
                $subscriptionStmt = $pdo->prepare("
                    SELECT subscription_plan_id, subscription_expiry_date 
                    FROM businesses 
                    WHERE owner_id = ? AND subscription_plan_id IS NOT NULL AND subscription_expiry_date >= CURDATE() 
                    LIMIT 1
                ");
                $subscriptionStmt->execute([$internal_user_id]);
                $hasActiveSubscription = $subscriptionStmt->fetch();
                
                if (!$hasActiveSubscription) {
                    $pdo->rollBack();
                    error_log("❌ کاربر {$internal_user_id} تلاش کرد کسب‌وکار دوم بدون اشتراک اضافه کند");
                    send_json_response('error', 'برای افزودن بیش از یک کسب‌وکار، باید اشتراک نسخه پرو را خریداری کنید.', [], 403);
                }
            }
            
            // بررسی کد بازاریاب
            $marketer_id = null;
            $marketer_code = trim($formData['marketer_code'] ?? '');
            if (!empty($marketer_code)) {
                try {
                    // جستجوی بازاریاب بر اساس کد
                    $marketerStmt = $pdo->prepare("SELECT id FROM marketers WHERE marketer_code = ? AND is_active = 1");
                    $marketerStmt->execute([$marketer_code]);
                    $marketer = $marketerStmt->fetch();
                    
                    if ($marketer) {
                        $marketer_id = $marketer['id'];
                        error_log("✅ کد بازاریاب معتبر پیدا شد: {$marketer_code} - ID: {$marketer_id}");
                    } else {
                        error_log("❌ کد بازاریاب نامعتبر: {$marketer_code}");
                    }
                } catch (Exception $e) {
                    error_log("❌ خطا در بررسی کد بازاریاب: " . $e->getMessage());
                }
            }
            
            // === راه‌حل اول: بررسی تعداد کسب‌وکارهای موجود کاربر ===
            $businessCountStmt = $pdo->prepare("
                SELECT COUNT(*) as business_count 
                FROM businesses 
                WHERE owner_id = ?
            ");
            $businessCountStmt->execute([$internal_user_id]);
            $businessCount = $businessCountStmt->fetch()['business_count'];
            error_log("🏢 تعداد کسب‌وکارهای موجود کاربر {$internal_user_id}: {$businessCount}");
            
            // محاسبه مجموع توکن‌های اعطا شده کاربر از جدول user_token_grants
            error_log("🔍 شروع محاسبه توکن برای کاربر {$internal_user_id}");
            
            // ابتدا بررسی می‌کنیم که آیا جدول user_token_grants وجود دارد یا نه
            try {
                $checkTableStmt = $pdo->query("SHOW TABLES LIKE 'user_token_grants'");
                $tableExists = $checkTableStmt->rowCount() > 0;
                error_log("📋 وضعیت جدول user_token_grants: " . ($tableExists ? "موجود است ✅" : "وجود ندارد ❌"));
                
                if (!$tableExists) {
                    error_log("⚠️ جدول user_token_grants وجود ندارد - نیاز به بررسی دقیق‌تر!");
                    $total_granted_tokens = 0; // اگر جدول وجود ندارد، فرض می‌کنیم هیچ توکنی اعطا نشده
                } else {
                    $totalGrantedTokensStmt = $pdo->prepare("
                        SELECT COALESCE(SUM(tokens_granted), 0) as total_granted_tokens 
                        FROM user_token_grants 
                        WHERE user_id = ?
                    ");
                    $totalGrantedTokensStmt->execute([$internal_user_id]);
                    $tokenData = $totalGrantedTokensStmt->fetch();
                    $total_granted_tokens = $tokenData['total_granted_tokens'] ?? 0;
                    error_log("💰 توکن‌های اعطا شده قبلی کاربر {$internal_user_id}: {$total_granted_tokens}");
                }
            } catch (Exception $e) {
                error_log("❌ خطا در بررسی جدول user_token_grants: " . $e->getMessage());
                $total_granted_tokens = 0; // در صورت خطا، فرض می‌کنیم هیچ توکنی اعطا نشده
            }
            
            // === راه‌حل دوم: منطق جدید اعطای توکن ===
            $max_allowed_tokens = 50;
            
            // حالت 1: کاربر اولین کسب‌وکار خود را ایجاد می‌کند و هنوز توکن نگرفته
            if ($businessCount == 0 && $total_granted_tokens == 0) {
                $free_tokens = $max_allowed_tokens;
                error_log("✅ اولین کسب‌وکار کاربر {$internal_user_id} - اعطای {$free_tokens} توکن رایگان");
            } 
            // حالت 2: کاربر قبلاً کسب‌وکار داشته اما به دلیلی توکن کمتر از حد مجاز گرفته
            else if ($businessCount == 0 && $total_granted_tokens < $max_allowed_tokens) {
                $free_tokens = $max_allowed_tokens - $total_granted_tokens;
                error_log("⚠️ کاربر {$internal_user_id} اولین کسب‌وکار اما قبلاً {$total_granted_tokens} توکن گرفته - اعطای {$free_tokens} توکن باقی‌مانده");
            } 
            // حالت 3: کاربر قبلاً کسب‌وکار داشته - بدون توکن جدید
            else {
                $free_tokens = 0;
                error_log("❌ کاربر {$internal_user_id} قبلاً {$businessCount} کسب‌وکار ایجاد کرده و {$total_granted_tokens} توکن دریافت کرده - بدون توکن جدید");
            }
            
            error_log("📊 محاسبه نهایی توکن کاربر {$internal_user_id}: حداکثر={$max_allowed_tokens}, اعطا شده قبلی={$total_granted_tokens}, تعداد کسب‌وکار={$businessCount}, اعطا شده جدید={$free_tokens}");
            
            if ($free_tokens > 0) {
                error_log("✅ کاربر {$internal_user_id} - توکن‌های اعطا شده قبلی: {$total_granted_tokens}, توکن‌های اعطا شده جدید: {$free_tokens}");
            } else {
                error_log("⚠️ کاربر {$internal_user_id} - بدون اعطای توکن جدید (قبلاً کسب‌وکار داشته یا به حد مجاز رسیده)");
            }
            
            // درج کسب‌وکار جدید در دیتابیس
            // توجه: از business_type به جای type استفاده می‌کنیم تا با ساختار دیتابیس هماهنگ باشد
            $sql = "INSERT INTO businesses (owner_id, name, business_type, phone, province, city, address, slot_duration_minutes, advance_booking_days, marketer_id, free_reservation_tokens, created_at) 
                    VALUES (:owner_id, :name, :business_type, :phone, :province, :city, :address, :slot_duration_minutes, :advance_booking_days, :marketer_id, :free_tokens, NOW())";
            
            $stmt = $pdo->prepare($sql);
            $result = $stmt->execute([
                ':owner_id' => $internal_user_id,
                ':name' => trim($formData['name']),
                ':business_type' => trim($formData['type']), // کلید business_type با مقدار فیلد type پر می‌شود
                ':phone' => $phone,
                ':province' => trim($formData['province']),
                ':city' => trim($formData['city']),
                ':address' => trim($formData['address'] ?? ''), // آدرس اختیاری است
                ':slot_duration_minutes' => 30, // مقدار پیش‌فرض 30 دقیقه
                ':advance_booking_days' => 14, // مقدار پیش‌فرض 14 روز
                ':marketer_id' => $marketer_id, // شناسه بازاریاب
                ':free_tokens' => $free_tokens // توکن‌های رایگان بر اساس وضعیت کاربر
            ]);
            
            if ($result) {
                // دریافت ID کسب‌وکار جدید
                $new_business_id = $pdo->lastInsertId();
                error_log("✅ کسب‌وکار جدید با موفقیت ایجاد شد - ID: " . $new_business_id);
                
                // ثبت توکن‌های اعطا شده در جدول user_token_grants برای ردیابی دائمی
                if ($free_tokens > 0) {
                    try {
                        error_log("💾 تلاش برای ثبت {$free_tokens} توکن در جدول user_token_grants برای کاربر {$internal_user_id}");
                        
                        $grantStmt = $pdo->prepare("
                            INSERT INTO user_token_grants (user_id, tokens_granted, business_id, notes) 
                            VALUES (?, ?, ?, ?)
                        ");
                        $grantResult = $grantStmt->execute([
                            $internal_user_id,
                            $free_tokens,
                            $new_business_id,
                            'توکن‌های رایگان برای کسب‌وکار: ' . trim($formData['name'])
                        ]);
                        
                        if ($grantResult) {
                            error_log("✅ {$free_tokens} توکن با موفقیت در جدول user_token_grants ثبت شد برای کاربر {$internal_user_id}");
                        } else {
                            error_log("❌ خطا در ثبت توکن‌ها در جدول user_token_grants");
                        }
                    } catch (Exception $e) {
                        error_log("❌ خطا در ثبت توکن‌ها در جدول user_token_grants: " . $e->getMessage());
                        error_log("⚠️ توکن‌ها در جدول businesses ثبت شده‌اند اما در user_token_grants ثبت نشده‌اند!");
                    }
                } else {
                    error_log("⚠️ هیچ توکن رایگانی برای ثبت در user_token_grants وجود ندارد");
                }
                
                // پردازش کد معرف (اختیاری)
                $referral_code = trim($formData['referral_code'] ?? '');
                if (!empty($referral_code)) {
                    try {
                        // جستجوی کسب‌وکار معرف
                        $referrerStmt = $pdo->prepare("SELECT id, owner_id FROM businesses WHERE link_alias = ? AND is_active = 1");
                        $referrerStmt->execute([$referral_code]);
                        $referrer = $referrerStmt->fetch();
                        
                        if ($referrer) {
                            // بررسی اینکه کاربر از کد معرف خودش استفاده نکرده باشد
                            if ($referrer['owner_id'] == $internal_user_id) {
                                error_log("❌ کاربر {$internal_user_id} تلاش کرد از کد معرف خودش استفاده کند: {$referral_code}");
                                debug_log('تلاش برای استفاده از کد معرف خودی', 'WARNING', [
                                    'user_id' => $internal_user_id,
                                    'referral_code' => $referral_code,
                                    'referrer_business_id' => $referrer['id']
                                ]);
                                // ادامه بدون پردازش معرفی - کسب‌وکار ایجاد می‌شود اما توکن رایگان داده نمی‌شود
                            } else {
                                // ثبت معرفی در جدول BusinessReferrals
                                $referralStmt = $pdo->prepare("
                                    INSERT INTO BusinessReferrals (referrer_business_id, referred_business_id, reward_tokens_granted, created_at) 
                                    VALUES (?, ?, 50, NOW())
                                ");
                                $referralStmt->execute([$referrer['id'], $new_business_id]);
                                
                                // افزایش توکن‌های رایگان کسب‌وکار معرف
                                $updateTokenStmt = $pdo->prepare("UPDATE businesses SET free_reservation_tokens = free_reservation_tokens + 50 WHERE id = ?");
                                $updateTokenStmt->execute([$referrer['id']]);
                                
                                // ارسال نوتیفیکیشن به صاحب کسب‌وکار معرف
                                if ($referrer['owner_id']) {
                                    $ownerStmt = $pdo->prepare("SELECT telegram_id FROM users WHERE id = ?");
                                    $ownerStmt->execute([$referrer['owner_id']]);
                                    $owner = $ownerStmt->fetch();
                                    
                                    if ($owner && $owner['telegram_id']) {
                                        $congratsMessage = "🎉 تبریک! شما یک کسب‌وکار جدید معرفی کردید!\n\n";
                                        $congratsMessage .= "💰 50 توکن رزرو رایگان به حساب شما اضافه شد.\n";
                                        $congratsMessage .= "🏢 کسب‌وکار معرفی شده: " . trim($formData['name']) . "\n\n";
                                        $congratsMessage .= "✨ با هر معرفی موفق، 50 توکن رایگان دریافت می‌کنید!";
                                        
                                        sendTelegramMessage($owner['telegram_id'], $congratsMessage);
                                    }
                                }
                                
                                debug_log('معرفی موفق ثبت شد', 'INFO', [
                                    'referrer_business_id' => $referrer['id'],
                                    'referred_business_id' => $new_business_id,
                                    'referral_code' => $referral_code
                                ]);
                            }
                        } else {
                            debug_log('کد معرف نامعتبر', 'WARNING', ['referral_code' => $referral_code]);
                        }
                    } catch (PDOException $e) {
                        debug_log('خطا در پردازش کد معرف', 'ERROR', ['error' => $e->getMessage()]);
                        // ادامه می‌دهیم چون معرفی اختیاری است
                    }
                }
                
                // به‌روزرسانی آمار بازاریاب در صورت وجود
                if ($marketer_id) {
                    try {
                        $updateMarketerStmt = $pdo->prepare("UPDATE marketers SET total_businesses = total_businesses + 1, updated_at = NOW() WHERE id = ?");
                        $updateMarketerStmt->execute([$marketer_id]);
                        
                        // ارسال نوتیفیکیشن به بازاریاب
                        $marketerUserStmt = $pdo->prepare("SELECT u.telegram_id, u.first_name FROM marketers m JOIN users u ON m.user_id = u.id WHERE m.id = ?");
                        $marketerUserStmt->execute([$marketer_id]);
                        $marketerUser = $marketerUserStmt->fetch();
                        
                        if ($marketerUser && $marketerUser['telegram_id']) {
                            $marketerMessage = "🎉 <b>تبریک!</b> 🎉\n\n";
                            $marketerMessage .= "👋 سلام " . htmlspecialchars($marketerUser['first_name']) . " عزیز!\n\n";
                            $marketerMessage .= "🏢 یک کسب‌وکار جدید از طریق لینک اختصاصی شما ثبت شد:\n";
                            $marketerMessage .= "📝 <b>نام کسب‌وکار:</b> " . htmlspecialchars(trim($formData['name'])) . "\n";
                            $marketerMessage .= "🏷️ <b>نوع:</b> " . htmlspecialchars(trim($formData['type'])) . "\n";
                            $marketerMessage .= "📍 <b>مکان:</b> " . htmlspecialchars(trim($formData['city'])) . ", " . htmlspecialchars(trim($formData['province'])) . "\n\n";
                            $marketerMessage .= "💰 <b>کمیسیون شما:</b> از هر رزرو انجام شده کمیسیون دریافت خواهید کرد!\n\n";
                            $marketerMessage .= "📊 برای مشاهده آمار کامل به داشبورد بازاریاب مراجعه کنید.";
                            
                            sendTelegramMessage($marketerUser['telegram_id'], $marketerMessage);
                        }
                        
                        error_log("✅ آمار بازاریاب {$marketer_id} به‌روزرسانی شد - کسب‌وکار جدید: {$new_business_id}");
                    } catch (Exception $e) {
                        error_log("❌ خطا در به‌روزرسانی آمار بازاریاب: " . $e->getMessage());
                        // ادامه می‌دهیم چون این بخش اختیاری است
                    }
                }
                
                // تایید تراکنش
                $pdo->commit();
                
                // ارسال پاسخ موفقیت‌آمیز همراه با ID کسب‌وکار جدید
                send_json_response('success', 'کسب‌وکار شما با موفقیت ثبت شد!', [
                    'business_id' => $new_business_id,
                    'message' => 'اطلاعات کسب‌وکار در سیستم ذخیره شد و می‌توانید برنامه کاری خود را تنظیم کنید.'
                ]);
            } else {
                throw new Exception('خطا در درج داده‌ها');
            }
            
        } catch (PDOException $e) {
            // در صورت خطا، تراکنش را لغو کن
            $pdo->rollBack();
            error_log("❌ خطای دیتابیس در ایجاد کسب‌وکار: " . $e->getMessage());
            
            // بررسی نوع خطا برای ارائه پیام مناسب
            if ($e->getCode() == '23000') {
                // خطای تکراری (Duplicate entry)
                send_json_response('error', 'کسب‌وکاری با این مشخصات قبلاً ثبت شده است.', [], 409);
            } else {
                $errorMsg = DEV_MODE ? 'خطایی در سرور هنگام ثبت کسب‌وکار رخ داد: ' . $e->getMessage() : 'خطایی در سرور هنگام ثبت کسب‌وکار رخ داد.';
                send_json_response('error', $errorMsg, [], 500);
            }
        } catch (Exception $e) {
            // در صورت خطای عمومی، تراکنش را لغو کن
            $pdo->rollBack();
            error_log("❌ خطای عمومی در ایجاد کسب‌وکار: " . $e->getMessage());
            send_json_response('error', 'خطایی در ثبت کسب‌وکار رخ داد.', [], 500);
        }
    }
    elseif ($action === 'update_schedule') {
        if (!$pdo) {
            error_log("❌ اتصال دیتابیس موجود نیست برای update_schedule");
            send_json_response('error', 'Database connection required for this operation.', [], 500);
        }
        
        // ولیدیشن ورودی‌های ضروری
        if (empty($formData['slot_duration_minutes']) || !is_numeric($formData['slot_duration_minutes'])) {
            send_json_response('error', 'مدت زمان اسلات معتبر نیست.', [], 400);
        }
        
        if (empty($formData['schedules']) || !is_array($formData['schedules'])) {
            send_json_response('error', 'داده‌های برنامه کاری معتبر نیست.', [], 400);
        }
        
        try {
            $pdo->beginTransaction();

            $updateBusinessStmt = $pdo->prepare("UPDATE businesses SET slot_duration_minutes = ? WHERE id = ?");
            $updateBusinessStmt->execute([$formData['slot_duration_minutes'], $business_id]);

            $upsertSql = "INSERT INTO schedules (business_id, day_of_week, start_time, end_time, is_active) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE start_time = VALUES(start_time), end_time = VALUES(end_time), is_active = VALUES(is_active)";
            $upsertStmt = $pdo->prepare($upsertSql);
            foreach ($formData['schedules'] as $day) {
                $upsertStmt->execute([
                    $business_id, $day['day_of_week'],
                    $day['is_active'] ? $day['start_time'] : null,
                    $day['is_active'] ? $day['end_time'] : null,
                    $day['is_active'] ? 1 : 0
                ]);
            }

            $deleteOverridesStmt = $pdo->prepare("DELETE FROM schedule_overrides WHERE business_id = ?");
            $deleteOverridesStmt->execute([$business_id]);

            // پردازش تاریخ‌های خاص با پشتیبانی از چندین بازه زمانی
            if (!empty($formData['overrides'])) {
                $insertOverrideSql = "INSERT INTO schedule_overrides (business_id, override_date, start_time, end_time, is_day_off) VALUES (?, ?, ?, ?, ?)";
                $insertOverrideStmt = $pdo->prepare($insertOverrideSql);
                
                foreach ($formData['overrides'] as $override) {
                    $gregorian_date = to_gregorian($override['jalali_date']);
                    
                    if ($override['is_day_off']) {
                        // اگر روز تعطیل است، فقط یک رکورد با is_day_off = 1 ذخیره می‌کنیم
                        $insertOverrideStmt->execute([
                            $business_id, $gregorian_date, null, null, 1
                        ]);
                    } else {
                        // اگر روز کاری است، هر بازه زمانی را جداگانه ذخیره می‌کنیم
                        if (isset($override['time_slots']) && is_array($override['time_slots'])) {
                            foreach ($override['time_slots'] as $slot) {
                                $insertOverrideStmt->execute([
                                    $business_id, $gregorian_date,
                                    $slot['start_time'], $slot['end_time'], 0
                                ]);
                            }
                        } else {
                            // پشتیبانی از فرمت قدیمی (برای سازگاری با نسخه‌های قبلی)
                            if (isset($override['start_time']) && isset($override['end_time'])) {
                                $insertOverrideStmt->execute([
                                    $business_id, $gregorian_date,
                                    $override['start_time'], $override['end_time'], 0
                                ]);
                            }
                        }
                    }
                }
            }

            $pdo->commit();
            send_json_response('success', 'برنامه کاری با موفقیت ذخیره شد!');

        } catch (Exception $e) {
            $pdo->rollBack();
            error_log("API Update Schedule Error: " . $e->getMessage());
            $errorMsg = DEV_MODE ? 'خطایی در ذخیره برنامه کاری رخ داد: ' . $e->getMessage() : 'خطایی در ذخیره برنامه کاری رخ داد.';
            send_json_response('error', $errorMsg, [], 500);
        }
    } 
    elseif ($action === 'update_specific_schedule') {
        debug_log('شروع عملیات update_specific_schedule', 'INFO', ['business_id' => $business_id, 'user_id' => $internal_user_id]);
        debug_log('داده‌های دریافتی برای update_specific_schedule', 'DEBUG', $formData);
        
        error_log("🚀 === شروع update_specific_schedule ===");
        error_log("📋 Business ID: " . $business_id);
        error_log("📦 داده‌های دریافتی: " . json_encode($formData, JSON_UNESCAPED_UNICODE));
        
        if (!$pdo) {
            error_log("❌ اتصال دیتابیس موجود نیست برای update_specific_schedule");
            if (DEV_MODE) {
                error_log("🧪 DEV_MODE: شبیه‌سازی موفقیت");
                send_json_response('success', 'برنامه کاری تقویم با موفقیت ذخیره شد! (DEV MODE)');
            } else {
                send_json_response('error', 'Database connection required for this operation.', [], 500);
            }
        }
        
        // ولیدیشن ورودی‌های ضروری
        if (empty($formData['slot_duration_minutes']) || !is_numeric($formData['slot_duration_minutes'])) {
            send_json_response('error', 'مدت زمان اسلات معتبر نیست.', [], 400);
        }
        
        try {
            error_log("🔄 شروع تراکنش دیتابیس");
            $pdo->beginTransaction();

            // بررسی وجود کسب‌وکار
            $checkBusinessStmt = $pdo->prepare("SELECT id FROM businesses WHERE id = ?");
            $checkBusinessStmt->execute([$business_id]);
            $businessExists = $checkBusinessStmt->fetch();
            
            if (!$businessExists) {
                error_log("❌ کسب‌وکار با ID " . $business_id . " یافت نشد");
                throw new Exception("کسب‌وکار یافت نشد");
            }
            error_log("✅ کسب‌وکار تأیید شد");

            // به‌روزرسانی مدت زمان اسلات
            $slotDuration = $formData['slot_duration_minutes'];
            error_log("⏰ به‌روزرسانی مدت زمان اسلات به: " . $slotDuration);
            
            $updateBusinessStmt = $pdo->prepare("UPDATE businesses SET slot_duration_minutes = ? WHERE id = ?");
            $updateResult = $updateBusinessStmt->execute([$slotDuration, $business_id]);
            
            if ($updateResult) {
                error_log("✅ مدت زمان اسلات به‌روزرسانی شد");
            } else {
                error_log("❌ خطا در به‌روزرسانی مدت زمان اسلات");
            }

            // حذف تمام تاریخ‌های خاص قبلی از هر دو جدول
            error_log("🗑️ حذف تاریخ‌های خاص قبلی از schedule_overrides...");
            $deleteOverridesStmt = $pdo->prepare("DELETE FROM schedule_overrides WHERE business_id = ?");
            $deleteResult = $deleteOverridesStmt->execute([$business_id]);
            $deletedOverrideRows = $deleteOverridesStmt->rowCount();
            
            error_log("🗑️ حذف تاریخ‌های خاص قبلی از daily_schedules...");
            $deleteDailyStmt = $pdo->prepare("DELETE FROM daily_schedules WHERE business_id = ?");
            $deleteDailyResult = $deleteDailyStmt->execute([$business_id]);
            $deletedDailyRows = $deleteDailyStmt->rowCount();
            
            error_log("📊 تعداد ردیف‌های حذف شده از schedule_overrides: " . $deletedOverrideRows);
            error_log("📊 تعداد ردیف‌های حذف شده از daily_schedules: " . $deletedDailyRows);

            // پردازش تاریخ‌های خاص جدید
            if (isset($formData['specific_dates']) && is_array($formData['specific_dates'])) {
                error_log("📅 شروع پردازش تاریخ‌های جدید...");
                error_log("📊 تعداد تاریخ‌های دریافتی: " . count($formData['specific_dates']));
                
                $insertedRecords = 0;
                foreach ($formData['specific_dates'] as $index => $dateData) {
                    error_log("📅 پردازش تاریخ " . ($index + 1) . ": " . json_encode($dateData, JSON_UNESCAPED_UNICODE));
                    
                    $gregorian_date = $dateData['date']; // تاریخ میلادی از فرانت‌اند
                    error_log("📆 تاریخ: " . $gregorian_date);
                    error_log("💼 وضعیت کاری: " . ($dateData['isWorking'] ? 'کاری' : 'تعطیل'));
                    
                    if (!$dateData['isWorking']) {
                        // اگر روز تعطیل است - هیچ اسلاتی ذخیره نمی‌کنیم
                        error_log("🚫 روز تعطیل - هیچ اسلاتی ذخیره نمی‌شود");
                    } else {
                        // اگر روز کاری است - تمام اسلات‌ها در specific_time_slots ذخیره می‌شوند
                        error_log("💼 پردازش روز کاری...");
                        
                        if (isset($dateData['time_slots']) && is_array($dateData['time_slots']) && !empty($dateData['time_slots'])) {
                            error_log("⏰ تعداد اسلات‌های زمانی: " . count($dateData['time_slots']));
                            
                            // ابتدا تمام اسلات‌های قبلی این تاریخ را حذف می‌کنیم
                            $deleteOldSlotsStmt = $pdo->prepare("DELETE FROM specific_time_slots WHERE business_id = ? AND slot_date = ?");
                            $deleteOldSlotsStmt->execute([$business_id, $gregorian_date]);
                            
                            // حالا تمام اسلات‌های جدید را در جدول specific_time_slots ذخیره می‌کنیم
                            error_log("📋 درج تمام اسلات‌ها در specific_time_slots...");
                            debug_log('INFO', 'شروع درج اسلات‌ها در specific_time_slots', [
                                'business_id' => $business_id,
                                'date' => $gregorian_date,
                                'total_slots' => count($dateData['time_slots'])
                            ]);
                            
                            $insertSlotStmt = $pdo->prepare("INSERT INTO specific_time_slots (business_id, slot_date, start_time, end_time, slot_duration_minutes) VALUES (?, ?, ?, ?, ?)");
                            
                            foreach ($dateData['time_slots'] as $index => $slot) {
                                error_log("🕐 درج اسلات " . ($index + 1) . ": " . $slot['start_time'] . " - " . $slot['end_time']);
                                
                                try {
                                    $insertSlotResult = $insertSlotStmt->execute([
                                        $business_id, 
                                        $gregorian_date,
                                        $slot['start_time'], 
                                        $slot['end_time'], 
                                        $formData['slot_duration_minutes'] ?? 30
                                    ]);
                                    
                                    if ($insertSlotResult) {
                                        error_log("✅ اسلات " . ($index + 1) . " درج شد");
                                        debug_log('INFO', 'اسلات با موفقیت درج شد', [
                                            'business_id' => $business_id,
                                            'date' => $gregorian_date,
                                            'start_time' => $slot['start_time'],
                                            'end_time' => $slot['end_time'],
                                            'slot_number' => $index + 1,
                                            'duration_minutes' => $formData['slot_duration_minutes'] ?? 30
                                        ]);
                                        $insertedRecords++;
                                    } else {
                                        error_log("❌ خطا در درج اسلات " . ($index + 1));
                                        debug_log('ERROR', 'خطا در درج اسلات', [
                                            'business_id' => $business_id,
                                            'date' => $gregorian_date,
                                            'start_time' => $slot['start_time'],
                                            'end_time' => $slot['end_time'],
                                            'slot_number' => $index + 1
                                        ]);
                                    }
                                } catch (PDOException $e) {
                                    error_log("❌ خطا در درج اسلات " . ($index + 1) . ": " . $e->getMessage());
                                    debug_log('ERROR', 'خطای PDO در درج اسلات', [
                                        'business_id' => $business_id,
                                        'date' => $gregorian_date,
                                        'start_time' => $slot['start_time'],
                                        'end_time' => $slot['end_time'],
                                        'slot_number' => $index + 1,
                                        'error_message' => $e->getMessage(),
                                        'error_code' => $e->getCode()
                                    ]);
                                    // ادامه به اسلات بعدی
                                }
                            }
                        } else {
                            error_log("⚠️ روز کاری بدون اسلات زمانی - هیچ رکوردی درج نمی‌شود");
                        }
                    }
                }
                
                error_log("📊 تعداد کل رکوردهای درج شده: " . $insertedRecords);
            } else {
                error_log("📅 هیچ تاریخ خاصی برای پردازش وجود ندارد");
            }

            // اگر هیچ تاریخ خاصی وجود نداشت، یک پیام موفقیت‌آمیز ارسال کن
            if (empty($formData['specific_dates'])) {
                $pdo->commit();
                send_json_response('success', 'هیچ تاریخ خاصی برای ذخیره وجود نداشت.');
                return;
            }

            error_log("✅ تراکنش تأیید می‌شود...");
            $pdo->commit();
            error_log("✅ تراکنش با موفقیت تأیید شد");
            
            debug_log('عملیات update_specific_schedule با موفقیت انجام شد', 'INFO', [
                'business_id' => $business_id,
                'inserted_records' => $insertedRecords ?? 0,
                'user_id' => $internal_user_id
            ]);
            
            error_log("📤 ارسال پاسخ موفقیت");
            send_json_response('success', 'برنامه کاری تقویم با موفقیت ذخیره شد!');

        } catch (Exception $e) {
            debug_log('خطا در update_specific_schedule', 'ERROR', [
                'error_message' => $e->getMessage(),
                'file' => $e->getFile(),
                'line' => $e->getLine(),
                'business_id' => $business_id,
                'user_id' => $internal_user_id,
                'stack_trace' => $e->getTraceAsString()
            ]);
            
            error_log("❌ خطا در update_specific_schedule: " . $e->getMessage());
            error_log("📍 فایل: " . $e->getFile() . " خط: " . $e->getLine());
            error_log("🔍 Stack trace: " . $e->getTraceAsString());
            
            $pdo->rollBack();
            error_log("🔄 تراکنش لغو شد");
            
            error_log("📤 ارسال پاسخ خطا");
            $errorMsg = DEV_MODE ? 'خطایی در ذخیره برنامه کاری تقویم رخ داد: ' . $e->getMessage() : 'خطایی در ذخیره برنامه کاری تقویم رخ داد.';
            send_json_response('error', $errorMsg, [], 500);
        }
        
        debug_log('پایان عملیات update_specific_schedule', 'INFO');
        error_log("🏁 === پایان update_specific_schedule ===");
    }
    elseif ($action === 'update_business') {
        try {
            // اصلاح دستور UPDATE - استفاده از business_type به جای type
            $sql = "UPDATE businesses SET name = :name, business_type = :type, phone = :phone, province = :province, city = :city, address = :address WHERE id = :id AND owner_id = :owner_id";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([
                ':name' => trim($formData['name']), 
                ':type' => trim($formData['type']), // مقدار فیلد type در ستون business_type ذخیره می‌شود
                ':phone' => trim($formData['phone']), 
                ':province' => trim($formData['province']),
                ':city' => trim($formData['city']), 
                ':address' => trim($formData['address'] ?? ''),
                ':id' => $business_id, 
                ':owner_id' => $internal_user_id
            ]);
            if ($stmt->rowCount() > 0) {
                send_json_response('success', 'تغییرات با موفقیت ذخیره شد!');
            } else {
                send_json_response('success', 'تغییری برای ذخیره وجود نداشت.');
            }
        } catch (PDOException $e) {
            error_log("API Update Business Error: " . $e->getMessage());
            $errorMsg = DEV_MODE ? 'خطایی در سرور هنگام ویرایش کسب و کار رخ داد: ' . $e->getMessage() : 'خطایی در سرور هنگام ویرایش کسب و کار رخ داد.';
            send_json_response('error', $errorMsg, [], 500);
        }
    }
    elseif ($action === 'update_link_alias') {
        // منطق به‌روزرسانی نامک لینک کسب‌وکار
        if (!$pdo) {
            error_log("❌ اتصال دیتابیس موجود نیست برای update_link_alias");
            send_json_response('error', 'Database connection required for this operation.', [], 500);
        }
        
        error_log("🔗 شروع به‌روزرسانی نامک لینک");
        
        $alias = $formData['link_alias'] ?? null;
        
        // اعتبارسنجی نامک لینک
        if (empty($alias)) {
            error_log("❌ نامک لینک خالی است");
            send_json_response('error', 'نامک لینک نمی‌تواند خالی باشد.', [], 400);
        }

        // بررسی فرمت نامک لینک (فقط حروف انگلیسی، اعداد و خط زیر، بین ۳ تا ۵۰ کاراکتر)
        if (!preg_match('/^[a-zA-Z0-9_]{3,50}$/', $alias)) {
            error_log("❌ فرمت نامک لینک نامعتبر: " . $alias);
            send_json_response('error', 'نامک لینک فقط میتواند شامل حروف انگلیسی، اعداد و خط زیر (_) و بین ۳ تا ۵۰ کاراکتر باشد.', [], 400);
        }
        
        try {
            // شروع تراکنش دیتابیس
            $pdo->beginTransaction();
            
            // بررسی تکراری نبودن نامک (برای سایر کسب‌وکارها)
            $checkStmt = $pdo->prepare("SELECT id FROM businesses WHERE link_alias = ? AND id != ?");
            $checkStmt->execute([$alias, $business_id]);
            $existingBusiness = $checkStmt->fetch();
            
            if ($existingBusiness) {
                error_log("❌ نامک لینک تکراری: " . $alias);
                $pdo->rollBack();
                send_json_response('error', 'این نامک لینک قبلاً استفاده شده است. لطفاً نامک دیگری انتخاب کنید.', [], 409);
            }
            
            // به‌روزرسانی نامک لینک در دیتابیس
            $updateStmt = $pdo->prepare("UPDATE businesses SET link_alias = ? WHERE id = ? AND owner_id = ?");
            $updateResult = $updateStmt->execute([$alias, $business_id, $internal_user_id]);
            
            if ($updateResult && $updateStmt->rowCount() > 0) {
                // تایید تراکنش
                $pdo->commit();
                error_log("✅ نامک لینک با موفقیت به‌روزرسانی شد: " . $alias);
                
                // ایجاد لینک کامل برای نمایش به کاربر
                $full_link = "https://webnovo.ir/start/" . $alias;
                
                send_json_response('success', 'نامک لینک با موفقیت ذخیره شد!', [
                    'link_alias' => $alias,
                    'full_link' => $full_link,
                    'message' => 'حالا می‌توانید از این لینک برای دعوت مشتریان استفاده کنید.'
                ]);
            } else {
                $pdo->rollBack();
                error_log("❌ خطا در به‌روزرسانی نامک لینک یا عدم تغییر");
                send_json_response('error', 'خطا در ذخیره نامک لینک یا تغییری اعمال نشد.', [], 500);
            }
            
        } catch (PDOException $e) {
            // در صورت خطا، تراکنش را لغو کن
            $pdo->rollBack();
            error_log("❌ خطای دیتابیس در به‌روزرسانی نامک لینک: " . $e->getMessage());
            
            // بررسی نوع خطا برای ارائه پیام مناسب
            if ($e->getCode() == '23000') {
                // خطای تکراری (Duplicate entry)
                send_json_response('error', 'این نامک لینک قبلاً استفاده شده است.', [], 409);
            } else {
                $errorMsg = DEV_MODE ? 'خطایی در سرور هنگام ذخیره نامک لینک رخ داد: ' . $e->getMessage() : 'خطایی در سرور هنگام ذخیره نامک لینک رخ داد.';
                send_json_response('error', $errorMsg, [], 500);
            }
        } catch (Exception $e) {
            // در صورت خطای عمومی، تراکنش را لغو کن
            $pdo->rollBack();
            error_log("❌ خطای عمومی در به‌روزرسانی نامک لینک: " . $e->getMessage());
            send_json_response('error', 'خطایی در ذخیره نامک لینک رخ داد.', [], 500);
        }
    }
    elseif ($action === 'update_services') {
        // 🛠️ پردازش ایجاد/ویرایش/حذف خدمات برای یک کسب‌وکار
        if (!$pdo) {
            error_log("❌ اتصال دیتابیس موجود نیست برای update_services");
            send_json_response('error', 'Database connection required for this operation.', [], 500);
        }
        if (!isset($formData['services']) || !is_array($formData['services'])) {
            send_json_response('error', 'ورودی services نامعتبر است.', [], 400);
        }
        try {
            // بررسی وجود جدول services
            $tableCheck = $pdo->query("SHOW TABLES LIKE 'services'");
            if ($tableCheck->rowCount() == 0) {
                // ایجاد جدول services
                $createTableSQL = "
                CREATE TABLE `services` (
                  `id` int(11) NOT NULL AUTO_INCREMENT,
                  `business_id` int(11) NOT NULL COMMENT 'شناسه کسب‌وکار',
                  `name` varchar(255) NOT NULL COMMENT 'نام خدمت',
                  `description` text DEFAULT NULL COMMENT 'توضیحات خدمت',
                  `price` decimal(10,0) DEFAULT NULL COMMENT 'قیمت خدمت (تومان)',
                  `duration_minutes` int(11) DEFAULT NULL COMMENT 'مدت زمان خدمت (دقیقه)',
                  `is_active` tinyint(1) DEFAULT 1 COMMENT 'فعال/غیرفعال',
                  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'تاریخ ایجاد',
                  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'تاریخ به‌روزرسانی',
                  PRIMARY KEY (`id`),
                  KEY `business_id` (`business_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='جدول خدمات کسب‌وکارها';
                ";
                $pdo->exec($createTableSQL);
                error_log("✅ جدول services ایجاد شد");
            }
            
            $pdo->beginTransaction();
            // تایید مالکیت کسب‌وکار برای اطمینان از مجاز بودن کاربر
            $checkStmt = $pdo->prepare("SELECT id FROM businesses WHERE id = ? AND owner_id = ?");
            $checkStmt->execute([$business_id, $internal_user_id]);
            if (!$checkStmt->fetchColumn()) {
                throw new Exception('کسب‌وکار یافت نشد یا دسترسی مجاز نیست.');
            }
            // آماده‌سازی کوئری‌ها
            $insertStmt = $pdo->prepare("INSERT INTO services (business_id, name, description, price, duration_minutes, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())");
            $updateStmt = $pdo->prepare("UPDATE services SET name = ?, description = ?, price = ?, duration_minutes = ?, is_active = ?, updated_at = NOW() WHERE id = ? AND business_id = ?");
            $deleteStmt = $pdo->prepare("DELETE FROM services WHERE id = ? AND business_id = ?");
            foreach ($formData['services'] as $index => $svc) {
                $id = isset($svc['id']) ? intval($svc['id']) : null;
                $deleted = !empty($svc['deleted']);
                $name = isset($svc['name']) ? trim($svc['name']) : '';
                $desc = isset($svc['description']) ? trim($svc['description']) : null;
                $price = (isset($svc['price']) && is_numeric($svc['price'])) ? floatval($svc['price']) : 0;
                $duration = isset($svc['duration_minutes']) ? intval($svc['duration_minutes']) : 0;
                $is_active = (isset($svc['is_active']) && ($svc['is_active'] === true || $svc['is_active'] === 1 || $svc['is_active'] === '1')) ? 1 : 0;
                if ($deleted) {
                    if ($id) { $deleteStmt->execute([$id, $business_id]); }
                    continue;
                }
                if ($name === '' || $duration <= 0 || $duration > 1440 || $price < 0) {
                    $pdo->rollBack();
                    send_json_response('error', 'داده‌های سرویس نامعتبر است (نام اجباری، مدت > 0 و قیمت >= 0).', [], 400);
                }
                if ($id) {
                    $updateStmt->execute([$name, $desc, $price, $duration, $is_active, $id, $business_id]);
                } else {
                    $insertStmt->execute([$business_id, $name, $desc, $price, $duration, $is_active]);
                }
            }
            // دریافت لیست به‌روز خدمات
            $fetchStmt = $pdo->prepare("SELECT id, name, description, price, duration_minutes, is_active, created_at FROM services WHERE business_id = ? ORDER BY id DESC");
            $fetchStmt->execute([$business_id]);
            $services = $fetchStmt->fetchAll(PDO::FETCH_ASSOC);
            $pdo->commit();
            send_json_response('success', 'خدمات با موفقیت ذخیره شد!', ['data' => ['services' => $services]]);
        } catch (PDOException $e) {
            if ($pdo->inTransaction()) { $pdo->rollBack(); }
            error_log('❌ خطای دیتابیس در update_services: ' . $e->getMessage());
            $errorMsg = DEV_MODE ? ('خطای دیتابیس: ' . $e->getMessage()) : 'خطای سرور هنگام ذخیره خدمات.';
            send_json_response('error', $errorMsg, [], 500);
        } catch (Exception $e) {
            if ($pdo->inTransaction()) { $pdo->rollBack(); }
            error_log('❌ خطای عمومی در update_services: ' . $e->getMessage());
            send_json_response('error', $e->getMessage(), [], 400);
        }
    }
    
    // ایجاد رزرو جدید
    if ($action === 'get_bot_info') {
        // دریافت اطلاعات ربات
        send_json_response('success', 'Bot info retrieved successfully.', [
            'bot_username' => BOT_USERNAME
        ]);
    }
    else if ($action === 'delete_business') {
        error_log("🗑️ delete_business action detected");
        error_log("🏢 Business ID: " . ($business_id ?? 'NULL'));
        
        if (!$pdo) {
            send_json_response('error', 'Database connection failed.', [], 500);
        }
        
        if (!$business_id) {
            send_json_response('error', 'Business ID is required.', [], 400);
        }
        
        try {
            $pdo->beginTransaction();
            
            // بررسی مالکیت کسب‌وکار
            $ownerStmt = $pdo->prepare("
                SELECT b.id, b.name 
                FROM businesses b 
                JOIN users u ON b.owner_id = u.id 
                WHERE b.id = ? AND u.telegram_id = ?
            ");
            $ownerStmt->execute([$business_id, $telegramUser['id'] ?? null]);
            $business = $ownerStmt->fetch();
            
            if (!$business) {
                $pdo->rollBack();
                send_json_response('error', 'کسب‌وکار یافت نشد یا شما مالک آن نیستید.', [], 403);
            }
            
            // حذف رزروهای مرتبط
            $deleteBookingsStmt = $pdo->prepare("DELETE FROM bookings WHERE business_id = ?");
            $deleteBookingsStmt->execute([$business_id]);
            
            // حذف زمان‌بندی‌های خاص
            $deleteSlotsStmt = $pdo->prepare("DELETE FROM specific_time_slots WHERE business_id = ?");
            $deleteSlotsStmt->execute([$business_id]);
            
            // حذف کسب‌وکار
            $deleteBusinessStmt = $pdo->prepare("DELETE FROM businesses WHERE id = ?");
            $deleteBusinessStmt->execute([$business_id]);
            
            $pdo->commit();
            
            error_log("✅ کسب‌وکار با موفقیت حذف شد: " . $business['name']);
            send_json_response('success', 'کسب‌وکار با موفقیت حذف شد.', [
                'business_name' => $business['name']
            ]);
            
        } catch (PDOException $e) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }
            error_log("❌ PDO Error in delete_business: " . $e->getMessage());
            send_json_response('error', 'خطا در حذف کسب‌وکار. لطفاً دوباره تلاش کنید.', [], 500);
        } catch (Exception $e) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }
            error_log("❌ General Error in delete_business: " . $e->getMessage());
            send_json_response('error', 'خطای سرور در حذف کسب‌وکار.', [], 500);
        }
    }

    elseif ($action === 'create_booking') {
        error_log("🔍 بررسی وضعیت دیتابیس: " . ($pdo ? 'موجود' : 'غیرموجود'));
        if (!$pdo) {
            error_log("❌ دیتابیس در دسترس نیست - شبیه‌سازی رزرو موفق");
            $confirmation_code = 'TEST' . rand(1000, 9999);
            
            // حتی در حالت تست، نوتیفیکیشن را تست کنیم
            $rawInput = file_get_contents('php://input');
            $formData = json_decode($rawInput, true);
            $user_id = $formData['user_id'] ?? null;
            
            error_log("📢 تست نوتیفیکیشن - User ID: " . ($user_id ?? 'NULL'));
            
            if (!empty($user_id) && $user_id > 0) {
                $testMessage = "✅ تست رزرو موفق!\n\n🔢 کد رزرو: {$confirmation_code}\n\n⚠️ این یک پیام تست است.";
                $result = sendTelegramMessage($user_id, $testMessage);
                error_log("📤 نتیجه ارسال پیام تست: " . ($result ? 'موفق' : 'ناموفق'));
            }
            
            send_json_response('success', 'Test booking created successfully.', [
                'booking_id' => rand(1, 1000),
                'confirmation_code' => $confirmation_code
            ]);
        }
        
     // در فایل api.php، داخل if ($action === 'create_booking')

        try {
            // مرحله ۱: خواندن داده‌ها فقط از formData
            // این بخش اصلاح شده تا اطمینان حاصل شود داده‌ها از JSON خوانده می‌شوند
            error_log("📥 داده‌های دریافتی: " . json_encode($formData, JSON_UNESCAPED_UNICODE));
            
            $booking_business_id = $formData['business_id'] ?? null;
            $booking_date        = $formData['date'] ?? null;
            $start_time          = $formData['start_time'] ?? null;
            $end_time            = $formData['end_time'] ?? null;
            $customer_name       = trim($formData['customer_name'] ?? '');
            $customer_phone      = trim($formData['customer_phone'] ?? '');
            $customer_notes      = trim($formData['customer_notes'] ?? '');
            $user_id             = $formData['user_id'] ?? null; // شناسه تلگرام کاربر
            
            // بررسی ظرفیت رزرو کسب‌وکار
            if (!canBusinessReceiveBooking($pdo, $booking_business_id)) {
                send_json_response('error', 'ظرفیت رزرو این کسب‌وکار به پایان رسیده است.', [], 403);
            }
            
            error_log("🔍 متغیرهای استخراج شده:");
            error_log("   - Business ID: " . ($booking_business_id ?? 'NULL'));
            error_log("   - Date: " . ($booking_date ?? 'NULL'));
            error_log("   - Start Time: " . ($start_time ?? 'NULL'));
            error_log("   - End Time: " . ($end_time ?? 'NULL'));
            error_log("   - Customer Name: " . ($customer_name ?: 'EMPTY'));
            error_log("   - User ID: " . ($user_id ?? 'NULL'));

            // مرحله ۲: اعتبارسنجی قوی‌تر داده‌ها
            if (empty($booking_business_id) || empty($booking_date) || empty($start_time) || empty($end_time) || empty($customer_name)) {
                // یک لاگ دقیق‌تر برای عیب‌یابی ثبت می‌کنیم
                debug_log('خطا: داده‌های ضروری برای رزرو ارسال نشده است.', 'ERROR', $formData);
                send_json_response('error', 'اطلاعات ارسالی ناقص است. لطفاً تمام فیلدهای لازم را پر کنید.', [], 400);
            }
            
            // ادامه کد بدون تغییر باقی می‌ماند...
             
             // بررسی وجود کسب‌وکار
            $businessStmt = $pdo->prepare("SELECT id, slot_duration_minutes FROM businesses WHERE id = ? AND is_active = 1");
            $businessStmt->execute([$booking_business_id]);
            $business = $businessStmt->fetch();
            
            if (!$business) {
                send_json_response('error', 'کسب‌وکار یافت نشد یا غیرفعال است.', [], 404);
            }
            
            // محاسبه مدت زمان رزرو
            $start_timestamp = strtotime($start_time);
            $end_timestamp = strtotime($end_time);
            $duration_minutes = ($end_timestamp - $start_timestamp) / 60;
            
            // بررسی صحت مدت زمان رزرو
            if ($duration_minutes <= 0) {
                debug_log('مدت زمان رزرو نامعتبر', 'ERROR', [
                    'start_time' => $start_time,
                    'end_time' => $end_time,
                    'duration_minutes' => $duration_minutes
                ]);
                send_json_response('error', 'زمان پایان باید بعد از زمان شروع باشد.', [], 400);
            }
            
            // بررسی حداقل مدت زمان رزرو (حداقل 5 دقیقه)
            if ($duration_minutes < 5) {
                debug_log('مدت زمان رزرو کمتر از حداقل', 'ERROR', [
                    'duration_minutes' => $duration_minutes,
                    'minimum_required' => 5
                ]);
                send_json_response('error', 'حداقل مدت زمان رزرو 5 دقیقه است.', [], 400);
            }
            
            // بررسی حداکثر مدت زمان رزرو (حداکثر 4 ساعت = 240 دقیقه)
            if ($duration_minutes > 240) {
                debug_log('مدت زمان رزرو بیش از حداکثر', 'ERROR', [
                    'duration_minutes' => $duration_minutes,
                    'maximum_allowed' => 240
                ]);
                send_json_response('error', 'حداکثر مدت زمان رزرو 4 ساعت (240 دقیقه) است.', [], 400);
            }
            
            // === بررسی اولیه توکن‌های مورد نیاز ===
            $requiredTokens = calculateTokensForDuration($duration_minutes);
            debug_log("بررسی اولیه توکن‌ها", 'INFO', [
                'duration_minutes' => $duration_minutes,
                'required_tokens' => $requiredTokens,
                'business_id' => $booking_business_id
            ]);
            
            // بررسی توکن‌های موجود قبل از شروع transaction
            $tokenCheckStmt = $pdo->prepare("SELECT free_reservation_tokens FROM businesses WHERE id = ?");
            $tokenCheckStmt->execute([$booking_business_id]);
            $tokenCheckData = $tokenCheckStmt->fetch();
            
            if (!$tokenCheckData || $tokenCheckData['free_reservation_tokens'] < $requiredTokens) {
                $availableTokens = $tokenCheckData ? $tokenCheckData['free_reservation_tokens'] : 0;
                debug_log('توکن‌های کافی موجود نیست', 'ERROR', [
                    'business_id' => $booking_business_id,
                    'available_tokens' => $availableTokens,
                    'required_tokens' => $requiredTokens,
                    'duration_minutes' => $duration_minutes
                ]);
                send_json_response('error', "توکن‌های رایگان کافی نیست.\n\nبرای این رزرو ({$duration_minutes} دقیقه) {$requiredTokens} توکن نیاز است اما شما {$availableTokens} توکن دارید.\n\n💡 برای خرید توکن بیشتر با پشتیبانی تماس بگیرید.", [], 400);
            }
            
            debug_log("✅ توکن‌های کافی موجود است", 'INFO', [
                'available_tokens' => $tokenCheckData['free_reservation_tokens'],
                'required_tokens' => $requiredTokens
            ]);
            
            // بررسی محدودیت رزرو روزانه (هر کاربر تلگرام فقط یک رزرو در روز از هر کسب‌وکار)
            if (!empty($user_id)) {
                $dailyLimitStmt = $pdo->prepare("
                    SELECT COUNT(*) as daily_count 
                    FROM bookings b
                    JOIN users u ON b.user_id = u.id
                    WHERE u.telegram_id = ? AND b.business_id = ? AND b.booking_date = ? AND b.status != 'cancelled'
                ");
                $dailyLimitStmt->execute([$user_id, $booking_business_id, $booking_date]);
                $dailyCount = $dailyLimitStmt->fetchColumn();
                
                if ($dailyCount > 0) {
                     error_log("❌ محدودیت رزرو روزانه: کاربر {$user_id} قبلاً در تاریخ {$booking_date} از کسب‌وکار {$booking_business_id} رزرو کرده است");
                     send_json_response('error', '⚠️ شما قبلاً در این تاریخ یک زمان رزرو کرده‌اید.\n\nبه دلیل محدودیت سیستم، هر کاربر روزانه فقط یک بار می‌تواند از هر کسب‌وکار رزرو کند.\n\n💡 می‌توانید رزرو قبلی خود را لغو کرده و زمان جدیدی انتخاب کنید.', [], 400);
                 }
                error_log("✅ بررسی محدودیت رزرو روزانه: مجاز است");
            }
            
            // بررسی عدم تداخل با رزرو موجود
            $conflictStmt = $pdo->prepare("
                SELECT id FROM bookings 
                WHERE business_id = ? AND booking_date = ? 
                AND ((start_time <= ? AND end_time > ?) OR (start_time < ? AND end_time >= ?))
                AND status != 'cancelled'
            ");
            $conflictStmt->execute([
                $booking_business_id, $booking_date, 
                $start_time, $start_time, $end_time, $end_time
            ]);
            
            if ($conflictStmt->fetch()) {
                send_json_response('error', 'این زمان قبلاً رزرو شده است.', [], 409);
            }
            
            // ایجاد کد تأیید
            $confirmation_code = strtoupper(substr(md5(uniqid()), 0, 6));
            
            // === مرحله ۳: بررسی و ایجاد کاربر ===
            error_log("👤 === شروع بررسی و ایجاد کاربر ===" );
            error_log("🔍 User ID دریافتی: " . ($user_id ?? 'NULL'));
            error_log("👤 نام مشتری: " . $customer_name);
            error_log("📞 شماره تلفن مشتری: " . $customer_phone);
            error_log("📊 نوع کاربر: " . (empty($user_id) ? 'مهمان' : 'تلگرامی'));
            
            $booking_user_id = null;
            
            if (!empty($user_id)) {
                // کاربر تلگرام - بررسی وجود در پایگاه داده
                error_log("🔍 بررسی وجود کاربر تلگرام با ID: " . $user_id);
                
                // بررسی وجود جدول users و در صورت عدم وجود، استفاده از wp_users
                $userTable = 'users';
                try {
                    $checkTableStmt = $pdo->prepare("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users'");
                    $checkTableStmt->execute();
                    if ($checkTableStmt->fetchColumn() == 0) {
                        $userTable = 'wp_users';
                        error_log("⚠️ جدول users وجود ندارد - استفاده از wp_users");
                    }
                } catch (PDOException $e) {
                    error_log("❌ خطا در بررسی جدول users: " . $e->getMessage());
                }
                
                if ($userTable === 'wp_users') {
                    $userStmt = $pdo->prepare("SELECT ID as id, user_login as first_name, user_email as phone FROM wp_users WHERE ID = ?");
                    $userStmt->execute([$user_id]);
                } else {
                    $userStmt = $pdo->prepare("SELECT id, first_name, phone, telegram_id FROM users WHERE telegram_id = ?");
                    $userStmt->execute([$user_id]);
                }
                $existingUser = $userStmt->fetch();
                
                error_log("🔍 نتیجه جستجوی کاربر: " . ($existingUser ? 'یافت شد' : 'یافت نشد'));
                if ($existingUser) {
                    error_log("📋 اطلاعات کاربر موجود: " . json_encode($existingUser, JSON_UNESCAPED_UNICODE));
                }
                
                if ($existingUser) {
                    // کاربر موجود است
                    $booking_user_id = $existingUser['id'];
                    error_log("✅ کاربر تلگرام موجود پیدا شد:");
                    error_log("   - User ID: " . $booking_user_id);
                    error_log("   - نام: " . ($existingUser['first_name'] ?? 'نامشخص'));
                    error_log("   - تلفن: " . ($existingUser['phone'] ?? 'نامشخص'));
                } else {
                    // کاربر جدید - ایجاد رکورد جدید
                    error_log("🆕 کاربر تلگرام جدید - ایجاد رکورد در پایگاه داده");
                    
                    try {
                        if ($userTable === 'wp_users') {
                            // ایجاد کاربر در جدول wp_users
                            $insertUserStmt = $pdo->prepare("INSERT INTO wp_users (user_login, user_pass, user_nicename, user_email, user_registered) VALUES (?, ?, ?, ?, NOW())");
                             $username = 'telegram_' . $user_id;
                             $hashedPassword = password_hash('temp_pass_' . $user_id, PASSWORD_DEFAULT);
                             $insertResult = $insertUserStmt->execute([$username, $hashedPassword, $username, $customer_phone]);
                        } else {
                            // ایجاد کاربر در جدول users
                            $insertUserStmt = $pdo->prepare("INSERT INTO users (telegram_id, first_name, phone, created_at) VALUES (?, ?, ?, NOW())");
                            $insertResult = $insertUserStmt->execute([$user_id, $customer_name, $customer_phone]);
                        }
                        
                        if ($insertResult) {
                            $booking_user_id = $pdo->lastInsertId();
                            error_log("✅ کاربر تلگرام جدید با موفقیت ایجاد شد در جدول {$userTable}:");
                            error_log("   - User ID: " . $booking_user_id);
                            error_log("   - Telegram ID: " . $user_id);
                            error_log("   - نام: " . $customer_name);
                            error_log("   - تلفن: " . $customer_phone);
                        } else {
                            error_log("❌ خطا در ایجاد کاربر تلگرام جدید");
                            throw new Exception('خطا در ایجاد کاربر جدید در پایگاه داده.');
                        }
                    } catch (PDOException $e) {
                        error_log("❌ خطای PDO در ایجاد کاربر تلگرام: " . $e->getMessage());
                        throw new Exception('خطا در ایجاد کاربر جدید: ' . $e->getMessage());
                    }
                }
            } else {
                // کاربر مهمان - ایجاد کاربر جدید با telegram_id منحصر به فرد
                error_log("🎭 کاربر مهمان - ایجاد رکورد جدید");
                
                try {
                    // بررسی وجود جدول users برای کاربر مهمان
                    $userTable = 'users';
                    try {
                        $checkTableStmt = $pdo->prepare("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users'");
                        $checkTableStmt->execute();
                        if ($checkTableStmt->fetchColumn() == 0) {
                            $userTable = 'wp_users';
                            error_log("⚠️ جدول users وجود ندارد - استفاده از wp_users برای کاربر مهمان");
                        }
                    } catch (PDOException $e) {
                        error_log("❌ خطا در بررسی جدول users: " . $e->getMessage());
                    }
                    
                    // تولید شناسه منحصر به فرد برای کاربر مهمان
                    $guest_id = time() * 1000 + rand(100, 999);
                    error_log("🔢 تولید شناسه مهمان: " . $guest_id);
                    
                    if ($userTable === 'wp_users') {
                        // ایجاد کاربر مهمان در جدول wp_users
                        $username = 'guest_' . $guest_id;
                        $hashedPassword = password_hash('guest_pass_' . $guest_id, PASSWORD_DEFAULT);
                        $insertGuestUserStmt = $pdo->prepare("INSERT INTO wp_users (user_login, user_pass, user_nicename, user_email, user_registered) VALUES (?, ?, ?, ?, NOW())");
                        $insertGuestResult = $insertGuestUserStmt->execute([$username, $hashedPassword, $username, $customer_phone]);
                    } else {
                        // ایجاد کاربر مهمان در جدول users
                        $guest_telegram_id = -$guest_id; // عدد منفی برای تشخیص کاربران مهمان
                        $insertGuestUserStmt = $pdo->prepare("INSERT INTO users (telegram_id, first_name, phone, created_at) VALUES (?, ?, ?, NOW())");
                        $insertGuestResult = $insertGuestUserStmt->execute([$guest_telegram_id, $customer_name, $customer_phone]);
                    }
                    
                    if ($insertGuestResult) {
                        $booking_user_id = $pdo->lastInsertId();
                        error_log("✅ کاربر مهمان جدید با موفقیت ایجاد شد در جدول {$userTable}:");
                        error_log("   - User ID: " . $booking_user_id);
                        error_log("   - Guest ID: " . $guest_id);
                        error_log("   - نام: " . $customer_name);
                        error_log("   - تلفن: " . $customer_phone);
                    } else {
                        error_log("❌ خطا در ایجاد کاربر مهمان");
                        throw new Exception('خطا در ایجاد کاربر مهمان در پایگاه داده.');
                    }
                } catch (PDOException $e) {
                    error_log("❌ خطای PDO در ایجاد کاربر مهمان: " . $e->getMessage());
                    throw new Exception('خطا در ایجاد کاربر مهمان: ' . $e->getMessage());
                }
            }
            
            // بررسی نهایی وجود booking_user_id
            if (empty($booking_user_id) || !is_numeric($booking_user_id) || $booking_user_id <= 0) {
                error_log("❌ booking_user_id نامعتبر: " . ($booking_user_id ?? 'NULL'));
                throw new Exception('خطا در ایجاد یا شناسایی کاربر. لطفاً دوباره تلاش کنید.');
            }
            
            error_log("✅ کاربر نهایی تأیید شد - User ID: " . $booking_user_id);
            error_log("👤 === پایان بررسی و ایجاد کاربر ===" );
            
            // ثبت رزرو
            $pdo->beginTransaction();
            
            // اطمینان از وجود ستون queue_status
            try {
                $colStmt = $pdo->prepare("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bookings' AND COLUMN_NAME = 'queue_status'");
                $colStmt->execute();
                if ($colStmt->fetchColumn() == 0) {
                    $pdo->exec("ALTER TABLE bookings ADD COLUMN queue_status ENUM('waiting','in_progress','completed') NOT NULL DEFAULT 'waiting', ADD COLUMN progress_updated_at DATETIME NULL");
                }
            } catch (PDOException $e) {
                // نادیده گرفتن در محیط‌هایی که پشتیبانی نمی‌شود
            }

            $insertBookingStmt = $pdo->prepare("
                INSERT INTO bookings (
                    business_id, user_id, booking_date, start_time, end_time, 
                    duration_minutes, customer_name, customer_phone, customer_notes, 
                    status, confirmation_code, queue_status, progress_updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'confirmed', ?, 'waiting', NOW())
            ");
            
            $insertBookingStmt->execute([
                $booking_business_id, $booking_user_id, $booking_date, 
                $start_time, $end_time, $duration_minutes, 
                $customer_name, $customer_phone, $customer_notes, 
                $confirmation_code
            ]);
            
            $booking_id = $pdo->lastInsertId();
            
            // محاسبه و کاهش توکن‌های رایگان بر اساس مدت زمان رزرو
            $requiredTokens = calculateTokensForDuration($duration_minutes);
            debug_log("توکن‌های مورد نیاز برای رزرو: {$requiredTokens}", 'INFO', [
                'duration_minutes' => $duration_minutes,
                'required_tokens' => $requiredTokens
            ]);
            
            $tokenStmt = $pdo->prepare("SELECT free_reservation_tokens FROM businesses WHERE id = ?");
            $tokenStmt->execute([$booking_business_id]);
            $tokenData = $tokenStmt->fetch();
            
            if ($tokenData && $tokenData['free_reservation_tokens'] >= $requiredTokens) {
                $updateTokenStmt = $pdo->prepare("UPDATE businesses SET free_reservation_tokens = free_reservation_tokens - ? WHERE id = ?");
                $updateTokenStmt->execute([$requiredTokens, $booking_business_id]);
                debug_log('توکن‌های رایگان کسب‌وکار کاهش یافت', 'INFO', [
                    'business_id' => $booking_business_id,
                    'tokens_deducted' => $requiredTokens,
                    'remaining_tokens' => $tokenData['free_reservation_tokens'] - $requiredTokens
                ]);
            } elseif ($tokenData && $tokenData['free_reservation_tokens'] > 0) {
                // توکن‌های موجود کافی نیست
                debug_log('توکن‌های موجود کافی نیست', 'ERROR', [
                    'business_id' => $booking_business_id,
                    'available_tokens' => $tokenData['free_reservation_tokens'],
                    'required_tokens' => $requiredTokens
                ]);
                $pdo->rollback();
                send_json_response('error', "توکن‌های رایگان کافی نیست. برای این رزرو {$requiredTokens} توکن نیاز است اما شما {$tokenData['free_reservation_tokens']} توکن دارید.", [], 400);
            } else {
                // هیچ توکن رایگانی موجود نیست
                debug_log('هیچ توکن رایگانی موجود نیست', 'ERROR', [
                    'business_id' => $booking_business_id,
                    'required_tokens' => $requiredTokens
                ]);
                $pdo->rollback();
                send_json_response('error', "توکن رایگان کافی نیست. برای این رزرو {$requiredTokens} توکن نیاز است.", [], 400);
            }
            
            $pdo->commit();
            
            // === سیستم اطلاع‌رسانی ===
            error_log("📢 === شروع ارسال اطلاع‌رسانی‌ها ===" );
            error_log("🔍 Business ID: {$booking_business_id}, User ID: {$user_id}");
            
            try {
                // دریافت اطلاعات کامل کسب‌وکار برای نوتیفیکیشن
                $businessStmt = $pdo->prepare("
                    SELECT b.*, u.first_name as owner_first_name, u.telegram_id as owner_telegram_id 
                    FROM businesses b 
                    LEFT JOIN users u ON b.owner_id = u.id 
                    WHERE b.id = ?
                ");
                $businessStmt->execute([$booking_business_id]);
                $businessInfo = $businessStmt->fetch();
                
                if ($businessInfo) {
                    error_log("📋 اطلاعات کسب‌وکار دریافت شد: " . $businessInfo['name']);
                    error_log("👤 Owner Telegram ID: " . ($businessInfo['owner_telegram_id'] ?? 'NULL'));
                    
                    // تنظیم متن‌های پیام
                    error_log("📅 تبدیل تاریخ: {$booking_date}");
                    $jalaliDate = convertToJalali($booking_date);
                    $dayName = getDayName($booking_date);
                    error_log("📅 تاریخ شمسی: {$jalaliDate}, روز: {$dayName}");
                    
                    // دریافت امتیاز اعتباری مشتری
                    $customer_credit = 0;
                    $credit_status = '❓ نامشخص';
                    try {
                        if (!empty($booking_user_id)) {
                            $creditStmt = $pdo->prepare("SELECT current_credit FROM customer_credits WHERE user_id = ?");
                            $creditStmt->execute([$booking_user_id]);
                            $creditResult = $creditStmt->fetch(PDO::FETCH_ASSOC);
                            
                            if ($creditResult) {
                                $customer_credit = $creditResult['current_credit'];
                                
                                // تعیین وضعیت امتیاز
                                if ($customer_credit >= 8) {
                                    $credit_status = '🟢 ممتاز';
                                } elseif ($customer_credit >= 5) {
                                    $credit_status = '🟡 معمولی';
                                } else {
                                    $credit_status = '🔴 نیاز به بهبود';
                                }
                            } else {
                                // اگر امتیاز وجود نداشت، ایجاد کن
                                $createCreditStmt = $pdo->prepare("INSERT INTO customer_credits (user_id, current_credit, total_earned, total_deducted) VALUES (?, 10, 10, 0)");
                                $createCreditStmt->execute([$booking_user_id]);
                                $customer_credit = 10;
                                $credit_status = '🟡 معمولی';
                            }
                        }
                    } catch (Exception $e) {
                        error_log("Error fetching customer credit: " . $e->getMessage());
                        $customer_credit = 0;
                        $credit_status = '❓ نامشخص';
                    }

                    // پیام برای صاحب کسب‌وکار
                    $ownerMessage = "🔔 رزرو جدید!\n\n";
                    $ownerMessage .= "📅 تاریخ: {$jalaliDate} ({$dayName})\n";
                    $ownerMessage .= "⏰ ساعت: {$start_time} تا {$end_time}\n";
                    $ownerMessage .= "👤 نام مشتری: {$customer_name}\n";
                    $ownerMessage .= "📞 شماره تماس: {$customer_phone}\n";
                    if (!empty($customer_notes)) {
                        $ownerMessage .= "📝 توضیحات: {$customer_notes}\n";
                    }
                    $ownerMessage .= "🔢 کد رزرو: {$confirmation_code}\n";
                    $ownerMessage .= "⭐ امتیاز اعتباری مشتری: {$customer_credit} امتیاز {$credit_status}\n";
                    $ownerMessage .= "🏢 کسب‌وکار: {$businessInfo['name']}";
                    
                    // پیام رسید برای کاربر
                    $customerMessage = "✅ رزرو شما تأیید شد!\n\n";
                    $customerMessage .= "🔢 کد رزرو: {$confirmation_code}\n";
                    $customerMessage .= "📅 تاریخ: {$jalaliDate} ({$dayName})\n";
                    $customerMessage .= "⏰ ساعت: {$start_time} تا {$end_time}\n\n";
                    $customerMessage .= "🏢 کسب‌وکار: {$businessInfo['name']}\n";
                    if (!empty($businessInfo['phone'])) {
                        $customerMessage .= "📞 تماس: {$businessInfo['phone']}\n";
                    }
                    if (!empty($businessInfo['address'])) {
                        $customerMessage .= "📍 آدرس: {$businessInfo['address']}\n";
                    }
                    $customerMessage .= "\n⚠️ لطفاً در زمان تعیین شده حضور داشته باشید.";
                    
                    // ارسال پیام به صاحب کسب‌وکار
                    if (!empty($businessInfo['owner_telegram_id'])) {
                        $ownerResult = sendTelegramMessage($businessInfo['owner_telegram_id'], $ownerMessage);
                        if ($ownerResult) {
                            error_log("✅ پیام به صاحب کسب‌وکار ارسال شد");
                        } else {
                            error_log("❌ خطا در ارسال پیام به صاحب کسب‌وکار");
                        }
                    } else {
                        error_log("⚠️ شناسه تلگرام صاحب کسب‌وکار موجود نیست");
                    }
                    
                    // ارسال رسید به کاربر - بازیابی telegram_id از پایگاه داده
                    try {
                        // دریافت telegram_id کاربر از پایگاه داده
                        $userTelegramStmt = $pdo->prepare("SELECT telegram_id FROM users WHERE id = ?");
                        $userTelegramStmt->execute([$booking_user_id]);
                        $userTelegramData = $userTelegramStmt->fetch();
                        
                        if ($userTelegramData && !empty($userTelegramData['telegram_id']) && $userTelegramData['telegram_id'] > 0) {
                            $customer_telegram_id = $userTelegramData['telegram_id'];
                            error_log("📤 تلاش برای ارسال رسید به کاربر تلگرامی: {$customer_telegram_id}");
                            $customerResult = sendTelegramMessage($customer_telegram_id, $customerMessage);
                            if ($customerResult) {
                                error_log("✅ رسید به کاربر ارسال شد");
                            } else {
                                error_log("❌ خطا در ارسال رسید به کاربر");
                            }
                        } else {
                            error_log("ℹ️ کاربر مهمان یا telegram_id نامعتبر - رسید ارسال نمی‌شود");
                        }
                    } catch (Exception $e) {
                        error_log("❌ خطا در بازیابی telegram_id کاربر: " . $e->getMessage());
                    }
                    
                } else {
                    error_log("❌ اطلاعات کسب‌وکار یافت نشد");
                }
                
            } catch (Exception $e) {
                error_log("❌ خطا در ارسال اطلاع‌رسانی: " . $e->getMessage());
                // ادامه فرآیند حتی در صورت خطا در نوتیفیکیشن
            }
            
            error_log("📢 === پایان ارسال اطلاع‌رسانی‌ها ===" );

            send_json_response('success', 'رزرو شما با موفقیت ثبت شد!', [
                'booking_id' => $booking_id,
                'confirmation_code' => $confirmation_code,
                'success' => true
            ]);
            
        } catch (PDOException $e) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }
            // لاگ کردن خطای واقعی برای عیب‌یابی بهتر
            debug_log('خطای PDOException در create_booking', 'ERROR', ['error_message' => $e->getMessage()]);
            send_json_response('error', 'خطا در ثبت رزرو. لطفاً دوباره تلاش کنید.', [], 500);
        } catch (Exception $e) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }
            debug_log('خطای Exception در create_booking', 'ERROR', ['error_message' => $e->getMessage()]);
            send_json_response('error', $e->getMessage(), [], 400);
        }
    }
    elseif ($action === 'advance_queue') {
        if (!$pdo) {
            send_json_response('error', 'Database connection required for this operation.', [], 500);
        }

        $business_id = $formData['business_id'] ?? null;
        if (empty($business_id)) {
            send_json_response('error', 'شناسه کسب‌وکار الزامی است.', [], 400);
        }

        // بررسی مالکیت کسب‌وکار
        try {
            $ownerStmt = $pdo->prepare("SELECT owner_id FROM businesses WHERE id = ?");
            $ownerStmt->execute([$business_id]);
            $owner = $ownerStmt->fetch();
            if (!$owner || (int)$owner['owner_id'] !== (int)$internal_user_id) {
                send_json_response('error', 'دسترسی غیرمجاز.', [], 403);
            }
        } catch (PDOException $e) {
            send_json_response('error', 'خطا در بررسی مالکیت.', [], 500);
        }

        try {
            $pdo->beginTransaction();

            // تکمیل رزروی که در حال انجام است
            $currentStmt = $pdo->prepare("SELECT id FROM bookings WHERE business_id = ? AND status != 'cancelled' AND queue_status = 'in_progress' ORDER BY booking_date ASC, start_time ASC, id ASC LIMIT 1");
            $currentStmt->execute([$business_id]);
            $current = $currentStmt->fetch(PDO::FETCH_ASSOC);
            if ($current) {
                $completeStmt = $pdo->prepare("UPDATE bookings SET queue_status = 'completed', progress_updated_at = NOW() WHERE id = ?");
                $completeStmt->execute([$current['id']]);
            }

            // انتخاب نفر بعدی در صف برای شروع
            $nextStmt = $pdo->prepare("SELECT id FROM bookings WHERE business_id = ? AND status != 'cancelled' AND queue_status = 'waiting' ORDER BY booking_date ASC, start_time ASC, id ASC LIMIT 1");
            $nextStmt->execute([$business_id]);
            $next = $nextStmt->fetch(PDO::FETCH_ASSOC);
            if ($next) {
                $startStmt = $pdo->prepare("UPDATE bookings SET queue_status = 'in_progress', progress_updated_at = NOW() WHERE id = ?");
                $startStmt->execute([$next['id']]);
            }

            $pdo->commit();
            send_json_response('success', 'صف به‌روزرسانی شد.', [
                'completed_booking_id' => $current['id'] ?? null,
                'next_booking_id' => $next['id'] ?? null
            ]);
        } catch (PDOException $e) {
            if ($pdo->inTransaction()) $pdo->rollBack();
            error_log('Error in advance_queue: ' . $e->getMessage());
            send_json_response('error', 'خطا در بروزرسانی صف.', [], 500);
        }
    }
    elseif ($action === 'complete_booking') {
        if (!$pdo) {
            send_json_response('error', 'Database connection required for this operation.', [], 500);
        }

        $business_id = $formData['business_id'] ?? null;
        $booking_id = $formData['booking_id'] ?? null;
        if (empty($business_id) || empty($booking_id)) {
            send_json_response('error', 'شناسه کسب‌وکار و رزرو الزامی است.', [], 400);
        }

        try {
            $ownerStmt = $pdo->prepare("SELECT owner_id FROM businesses WHERE id = ?");
            $ownerStmt->execute([$business_id]);
            $owner = $ownerStmt->fetch();
            if (!$owner || (int)$owner['owner_id'] !== (int)$internal_user_id) {
                send_json_response('error', 'دسترسی غیرمجاز.', [], 403);
            }

            $pdo->beginTransaction();

            $updateStmt = $pdo->prepare("UPDATE bookings SET queue_status = 'completed', progress_updated_at = NOW() WHERE id = ? AND business_id = ? AND status != 'cancelled'");
            $updateStmt->execute([$booking_id, $business_id]);

            $hasCurrentStmt = $pdo->prepare("SELECT COUNT(*) FROM bookings WHERE business_id = ? AND status != 'cancelled' AND queue_status = 'in_progress'");
            $hasCurrentStmt->execute([$business_id]);
            $hasCurrent = (int)$hasCurrentStmt->fetchColumn();

            if ($hasCurrent === 0) {
                $nextStmt = $pdo->prepare("SELECT id FROM bookings WHERE business_id = ? AND status != 'cancelled' AND queue_status = 'waiting' ORDER BY booking_date ASC, start_time ASC, id ASC LIMIT 1");
                $nextStmt->execute([$business_id]);
                $next = $nextStmt->fetch(PDO::FETCH_ASSOC);
                if ($next) {
                    $startStmt = $pdo->prepare("UPDATE bookings SET queue_status = 'in_progress', progress_updated_at = NOW() WHERE id = ?");
                    $startStmt->execute([$next['id']]);
                }
            }

            $pdo->commit();
            send_json_response('success', 'رزرو تکمیل شد.', [
                'booking_id' => (int)$booking_id
            ]);
        } catch (PDOException $e) {
            if ($pdo->inTransaction()) $pdo->rollBack();
            error_log('Error in complete_booking: ' . $e->getMessage());
            send_json_response('error', 'خطا در تکمیل رزرو.', [], 500);
        }
    }
    elseif ($action === 'get_subscription_status') {
        if (!$pdo) {
            send_json_response('error', 'Database connection required for this operation.', [], 500);
        }
        
        $business_id = $formData['business_id'] ?? null;
        
        if (empty($business_id)) {
            send_json_response('error', 'شناسه کسب‌وکار الزامی است.', [], 400);
        }
        
        try {
            // دریافت اطلاعات اشتراک و توکن‌های کسب‌وکار
            $stmt = $pdo->prepare("
                SELECT 
                    b.free_reservation_tokens,
                    b.subscription_plan_id,
                    b.subscription_expiry_date,
                    b.link_alias,
                    sp.name as subscription_plan_name
                FROM businesses b
                LEFT JOIN SubscriptionPlans sp ON b.subscription_plan_id = sp.id
                WHERE b.id = ?
            ");
            $stmt->execute([$business_id]);
            $business = $stmt->fetch();
            
            if (!$business) {
                send_json_response('error', 'کسب‌وکار یافت نشد.', [], 404);
            }
            
            // تنظیم داده‌های پاسخ
            $responseData = [
                'free_tokens_remaining' => (int)$business['free_reservation_tokens'],
                'subscription_plan' => $business['subscription_plan_name'],
                'expiry_date' => $business['subscription_expiry_date'],
                'referral_code' => $business['link_alias']
            ];
            
            send_json_response('success', 'اطلاعات اشتراک با موفقیت دریافت شد.', $responseData);
            
        } catch (PDOException $e) {
            debug_log('خطا در دریافت وضعیت اشتراک', 'ERROR', ['error' => $e->getMessage()]);
            send_json_response('error', 'خطا در دریافت اطلاعات اشتراک.', [], 500);
        }
    }
    elseif ($action === 'get_subscription_plans') {
        if (!$pdo) {
            send_json_response('error', 'Database connection required for this operation.', [], 500);
        }
        
        try {
            // بررسی وجود جدول SubscriptionPlans
            $tableCheck = $pdo->query("SHOW TABLES LIKE 'SubscriptionPlans'");
            if ($tableCheck->rowCount() == 0) {
                // ایجاد جدول SubscriptionPlans
                $createTableSQL = "
                CREATE TABLE `SubscriptionPlans` (
                  `id` INT PRIMARY KEY AUTO_INCREMENT,
                  `name` VARCHAR(255) NOT NULL COMMENT 'نام پلن (مثلاً: پایه، نقره‌ای)',
                  `price` DECIMAL(10, 2) NOT NULL COMMENT 'قیمت پلن به ریال',
                  `reservation_limit` INT NOT NULL COMMENT 'تعداد رزرو مجاز در ماه (-1 برای نامحدود)',
                  `duration_days` INT NOT NULL COMMENT 'مدت اعتبار پلن به روز (مثلاً: 30)',
                  `is_active` TINYINT(1) DEFAULT 1,
                  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                ";
                $pdo->exec($createTableSQL);
                
                // بررسی وجود پلن‌های پیش‌فرض
                $checkPlansStmt = $pdo->query("SELECT COUNT(*) FROM SubscriptionPlans");
                $planCount = $checkPlansStmt->fetchColumn();
                
                if ($planCount == 0) {
                    // درج پلن‌های پیش‌فرض فقط اگر هیچ پلنی وجود نداشته باشد
                    $insertPlansSQL = "
                    INSERT INTO `SubscriptionPlans` (`name`, `price`, `reservation_limit`, `duration_days`, `is_active`) VALUES
                    ('پکیج اولیه', 450000.00, 150, 30, 1),
                    ('پکیج متوسط', 900000.00, 300, 30, 1),
                    ('پکیج پیشرفته', 1500000.00, 500, 30, 1);
                    ";
                    $pdo->exec($insertPlansSQL);
                    error_log("✅ پلن‌های پیش‌فرض درج شدند");
                } else {
                    error_log("ℹ️ پلن‌های پیش‌فرض قبلاً موجود هستند (" . $planCount . " پلن)");
                }
                
                error_log("✅ جدول SubscriptionPlans ایجاد شد و پلن‌های پیش‌فرض اضافه شدند");
            } else {
                // اگر جدول وجود دارد، پلن‌های تکراری را حذف کن
                $cleanupStmt = $pdo->prepare("
                    DELETE p1 FROM SubscriptionPlans p1
                    INNER JOIN SubscriptionPlans p2 
                    WHERE p1.id > p2.id AND p1.name = p2.name AND p1.price = p2.price
                ");
                $cleanupStmt->execute();
                $deletedCount = $cleanupStmt->rowCount();
                if ($deletedCount > 0) {
                    error_log("🗑️ حذف " . $deletedCount . " پلن تکراری انجام شد");
                }
            }
            
            // ایجاد جدول payments
            $paymentsTableCheck = $pdo->query("SHOW TABLES LIKE 'payments'");
            if ($paymentsTableCheck->rowCount() == 0) {
                $createPaymentsTableSQL = "
                CREATE TABLE `payments` (
                  `id` INT PRIMARY KEY AUTO_INCREMENT,
                  `business_id` VARCHAR(255) NOT NULL COMMENT 'شناسه کسب و کار',
                  `plan_id` INT NOT NULL COMMENT 'شناسه پلن خریداری شده',
                  `amount` DECIMAL(10, 2) NOT NULL COMMENT 'مبلغ پرداخت به ریال',
                  `authority` VARCHAR(64) NOT NULL UNIQUE COMMENT 'شناسه تراکنش زرین‌پال',
                  `ref_id` VARCHAR(50) NULL COMMENT 'شماره پیگیری زرین‌پال',
                  `status` ENUM('pending', 'completed', 'failed', 'cancelled') DEFAULT 'pending' COMMENT 'وضعیت پرداخت',
                  `payment_date` TIMESTAMP NULL COMMENT 'تاریخ پرداخت موفق',
                  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  INDEX `idx_authority` (`authority`),
                  INDEX `idx_business_id` (`business_id`),
                  INDEX `idx_status` (`status`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                ";
                $pdo->exec($createPaymentsTableSQL);
                error_log("✅ جدول payments ایجاد شد");
            }
            
            // ایجاد جدول user_subscriptions
            $subscriptionsTableCheck = $pdo->query("SHOW TABLES LIKE 'user_subscriptions'");
            if ($subscriptionsTableCheck->rowCount() == 0) {
                $createSubscriptionsTableSQL = "
                CREATE TABLE `user_subscriptions` (
                  `id` INT PRIMARY KEY AUTO_INCREMENT,
                  `business_id` VARCHAR(255) NOT NULL COMMENT 'شناسه کسب و کار',
                  `plan_id` INT NOT NULL COMMENT 'شناسه پلن',
                  `payment_id` INT NOT NULL COMMENT 'شناسه پرداخت',
                  `tokens_allocated` INT NOT NULL COMMENT 'تعداد توکن‌های تخصیص یافته',
                  `tokens_used` INT DEFAULT 0 COMMENT 'تعداد توکن‌های استفاده شده',
                  `start_date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'تاریخ شروع اشتراک',
                  `end_date` TIMESTAMP NOT NULL COMMENT 'تاریخ پایان اشتراک',
                  `is_active` TINYINT(1) DEFAULT 1 COMMENT 'وضعیت فعال بودن اشتراک',
                  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  FOREIGN KEY (`payment_id`) REFERENCES `payments`(`id`) ON DELETE CASCADE,
                  FOREIGN KEY (`plan_id`) REFERENCES `SubscriptionPlans`(`id`) ON DELETE CASCADE,
                  INDEX `idx_business_id` (`business_id`),
                  INDEX `idx_active_subscription` (`business_id`, `is_active`, `end_date`),
                  INDEX `idx_plan_id` (`plan_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                ";
                $pdo->exec($createSubscriptionsTableSQL);
                error_log("✅ جدول user_subscriptions ایجاد شد");
            }
            
            // اضافه کردن فیلدهای اشتراک به جدول businesses
            try {
                // بررسی وجود فیلد subscription_plan_id
                $checkField1 = $pdo->query("SHOW COLUMNS FROM businesses LIKE 'subscription_plan_id'");
                if ($checkField1->rowCount() == 0) {
                    $pdo->exec("ALTER TABLE businesses ADD COLUMN subscription_plan_id INT DEFAULT NULL COMMENT 'شناسه پلن اشتراک فعال'");
                    error_log("✅ فیلد subscription_plan_id به جدول businesses اضافه شد");
                }
                
                // بررسی وجود فیلد subscription_expiry_date
                $checkField2 = $pdo->query("SHOW COLUMNS FROM businesses LIKE 'subscription_expiry_date'");
                if ($checkField2->rowCount() == 0) {
                    $pdo->exec("ALTER TABLE businesses ADD COLUMN subscription_expiry_date DATE DEFAULT NULL COMMENT 'تاریخ انقضای اشتراک'");
                    error_log("✅ فیلد subscription_expiry_date به جدول businesses اضافه شد");
                }
                
                // بررسی وجود فیلد free_reservation_tokens
                $checkField3 = $pdo->query("SHOW COLUMNS FROM businesses LIKE 'free_reservation_tokens'");
                if ($checkField3->rowCount() == 0) {
                    $pdo->exec("ALTER TABLE businesses ADD COLUMN free_reservation_tokens INT DEFAULT 5 COMMENT 'تعداد توکن‌های رایگان رزرو'");
                    error_log("✅ فیلد free_reservation_tokens به جدول businesses اضافه شد");
                }
                
                // اضافه کردن ایندکس‌ها
                try {
                    $pdo->exec("CREATE INDEX IF NOT EXISTS idx_businesses_subscription_plan ON businesses(subscription_plan_id)");
                    $pdo->exec("CREATE INDEX IF NOT EXISTS idx_businesses_subscription_expiry ON businesses(subscription_expiry_date)");
                    error_log("✅ ایندکس‌های اشتراک به جدول businesses اضافه شدند");
                } catch (PDOException $e) {
                    error_log("⚠️ خطا در ایجاد ایندکس‌های اشتراک: " . $e->getMessage());
                }
                
            } catch (PDOException $e) {
                error_log("❌ خطا در اضافه کردن فیلدهای اشتراک: " . $e->getMessage());
            }
            
            // دریافت پلن‌های فعال
            $stmt = $pdo->prepare("SELECT * FROM SubscriptionPlans WHERE is_active = 1 ORDER BY price ASC");
            $stmt->execute();
            $plans = $stmt->fetchAll();
            
            send_json_response('success', 'پلن‌های اشتراک با موفقیت دریافت شد.', ['plans' => $plans]);
            
        } catch (PDOException $e) {
            debug_log('خطا در دریافت پلن‌های اشتراک', 'ERROR', ['error' => $e->getMessage()]);
            send_json_response('error', 'خطا در دریافت پلن‌های اشتراک.', [], 500);
        }
    }
    elseif ($action === 'cancel_booking') {
        error_log("🚀 === شروع cancel_booking در API ===" );
        error_log("📥 داده‌های دریافتی: " . json_encode($formData, JSON_UNESCAPED_UNICODE));
        error_log("📦 داده‌های دریافتی: " . json_encode($formData, JSON_UNESCAPED_UNICODE));
        
        if (!$pdo) {
            error_log("❌ اتصال دیتابیس موجود نیست");
            send_json_response('error', 'Database connection required for this operation.', [], 500);
        }
        
        $booking_id = $formData['booking_id'] ?? null;
        $confirmation_code = $formData['confirmation_code'] ?? null;
        $user_id = $formData['user_id'] ?? null;
        
        error_log("📋 Booking ID: " . ($booking_id ?? 'NULL'));
        error_log("🔢 Confirmation Code: " . ($confirmation_code ?? 'NULL'));
        error_log("👤 User ID: " . ($user_id ?? 'NULL'));
        
        if (empty($booking_id) && empty($confirmation_code)) {
            error_log("❌ هیچ شناسه‌ای ارسال نشده");
            send_json_response('error', 'شناسه رزرو یا کد تأیید الزامی است.', [], 400);
        }
        
        if (empty($user_id)) {
            error_log("❌ شناسه کاربر خالی است");
            send_json_response('error', 'شناسه کاربر الزامی است.', [], 400);
        }
        
        try {
            $pdo->beginTransaction();
            
            // یافتن رزرو بر اساس شناسه یا کد تأیید
            // user_id در این context همان telegram_id است
            if (!empty($booking_id)) {
                $bookingStmt = $pdo->prepare("
                    SELECT b.*, u.telegram_id, bus.name as business_name, bus.owner_id
                    FROM bookings b
                    JOIN users u ON b.user_id = u.id
                    JOIN businesses bus ON b.business_id = bus.id
                    WHERE b.id = ? AND u.telegram_id = ? AND b.status != 'cancelled'
                ");
                $bookingStmt->execute([$booking_id, $user_id]);
            } else {
                $bookingStmt = $pdo->prepare("
                    SELECT b.*, u.telegram_id, bus.name as business_name, bus.owner_id
                    FROM bookings b
                    JOIN users u ON b.user_id = u.id
                    JOIN businesses bus ON b.business_id = bus.id
                    WHERE b.confirmation_code = ? AND u.telegram_id = ? AND b.status != 'cancelled'
                ");
                $bookingStmt->execute([$confirmation_code, $user_id]);
            }
            
            error_log("🔍 جستجوی رزرو - Booking ID: " . ($booking_id ?? 'NULL') . ", User Telegram ID: " . $user_id);
            
            $booking = $bookingStmt->fetch();
            
            error_log("📊 نتیجه جستجوی رزرو: " . ($booking ? 'یافت شد' : 'یافت نشد'));
            if ($booking) {
                error_log("📋 اطلاعات رزرو: " . json_encode($booking, JSON_UNESCAPED_UNICODE));
            }
            
            if (!$booking) {
                error_log("❌ رزرو یافت نشد - لغو تراکنش");
                $pdo->rollBack();
                send_json_response('error', 'رزرو یافت نشد یا قبلاً لغو شده است.', [], 404);
            }
            
            // بررسی زمان لغو (حداقل یک ساعت قبل از شروع رزرو)
            $booking_datetime = $booking['booking_date'] . ' ' . $booking['start_time'];
            $booking_timestamp = strtotime($booking_datetime);
            $current_timestamp = time();
            $time_diff_hours = ($booking_timestamp - $current_timestamp) / 3600;
            
            if ($time_diff_hours < 1) {
                $pdo->rollBack();
                send_json_response('error', 'امکان لغو رزرو تا یک ساعت قبل از زمان رزرو وجود دارد.', [], 400);
            }
            
            // لغو رزرو
            $cancelStmt = $pdo->prepare("UPDATE bookings SET status = 'cancelled', updated_at = NOW() WHERE id = ?");
            $cancelResult = $cancelStmt->execute([$booking['id']]);
            
            if (!$cancelResult) {
                $pdo->rollBack();
                send_json_response('error', 'خطا در لغو رزرو.', [], 500);
            }
            
            // کسر یک امتیاز از اعتبار مشتری به دلیل لغو رزرو
            try {
                // بررسی وجود رکورد اعتباری مشتری
                $creditCheckStmt = $pdo->prepare("SELECT id, current_credit FROM customer_credits WHERE user_id = ?");
                $creditCheckStmt->execute([$booking['user_id']]);
                $creditRecord = $creditCheckStmt->fetch();
                
                if ($creditRecord) {
                    // کسر یک امتیاز (حداقل صفر)
                    $newCredit = max(0, $creditRecord['current_credit'] - 1);
                    $updateCreditStmt = $pdo->prepare("UPDATE customer_credits SET current_credit = ?, updated_at = NOW() WHERE user_id = ?");
                    $updateCreditStmt->execute([$newCredit, $booking['user_id']]);
                    
                    // ثبت تاریخچه تغییر امتیاز
                    $historyStmt = $pdo->prepare("
                        INSERT INTO credit_history (user_id, booking_id, change_type, credit_change, credit_before, credit_after, reason, created_at) 
                        VALUES (?, ?, 'cancellation', ?, ?, ?, 'booking_cancellation', NOW())
                    ");
                    $historyStmt->execute([$booking['user_id'], $booking['id'], -1, $creditRecord['current_credit'], $newCredit]);
                    
                    error_log("✅ کسر امتیاز: کاربر {$booking['user_id']} - امتیاز قبلی: {$creditRecord['current_credit']} - امتیاز جدید: {$newCredit}");
                } else {
                    // ایجاد رکورد اعتباری جدید با امتیاز صفر (چون یک امتیاز کسر شده)
                    $createCreditStmt = $pdo->prepare("
                        INSERT INTO customer_credits (user_id, current_credit, total_earned, total_spent, created_at, updated_at) 
                        VALUES (?, 0, 0, 1, NOW(), NOW())
                    ");
                    $createCreditStmt->execute([$booking['user_id']]);
                    
                    // ثبت تاریخچه تغییر امتیاز
                    $historyStmt = $pdo->prepare("
                        INSERT INTO credit_history (user_id, booking_id, change_type, credit_change, credit_before, credit_after, reason, created_at) 
                        VALUES (?, ?, 'cancellation', ?, ?, ?, 'booking_cancellation', NOW())
                    ");
                    $historyStmt->execute([$booking['user_id'], $booking['id'], -1, 0, 0]);
                    
                    error_log("✅ ایجاد رکورد اعتباری جدید: کاربر {$booking['user_id']} - امتیاز: 0 (کسر شده به دلیل لغو)");
                }
            } catch (PDOException $creditError) {
                error_log("⚠️ خطا در کسر امتیاز اعتباری: " . $creditError->getMessage());
                // ادامه پردازش حتی در صورت خطا در کسر امتیاز
            }
            
            $pdo->commit();
            
            // ارسال اطلاع‌رسانی لغو رزرو
            try {
                $jalaliDate = convertToJalali($booking['booking_date']);
                $dayName = getDayName($booking['booking_date']);
                
                // پیام برای کاربر
                $customerMessage = "❌ رزرو شما لغو شد\n\n";
                $customerMessage .= "🔢 کد رزرو: {$booking['confirmation_code']}\n";
                $customerMessage .= "📅 تاریخ: {$jalaliDate} ({$dayName})\n";
                $customerMessage .= "⏰ ساعت: {$booking['start_time']} تا {$booking['end_time']}\n";
                $customerMessage .= "🏢 کسب‌وکار: {$booking['business_name']}\n\n";
                $customerMessage .= "✅ اکنون می‌توانید زمان دیگری را رزرو کنید.";
                
                sendTelegramMessage($user_id, $customerMessage);
                
                // پیام برای صاحب کسب‌وکار
                if (!empty($booking['owner_id'])) {
                    $ownerTelegramStmt = $pdo->prepare("SELECT telegram_id FROM users WHERE id = ?");
                    $ownerTelegramStmt->execute([$booking['owner_id']]);
                    $ownerTelegram = $ownerTelegramStmt->fetch();
                    
                    if ($ownerTelegram && !empty($ownerTelegram['telegram_id'])) {
                        $ownerMessage = "🔔 رزرو لغو شد\n\n";
                        $ownerMessage .= "📅 تاریخ: {$jalaliDate} ({$dayName})\n";
                        $ownerMessage .= "⏰ ساعت: {$booking['start_time']} تا {$booking['end_time']}\n";
                        $ownerMessage .= "👤 نام مشتری: {$booking['customer_name']}\n";
                        $ownerMessage .= "🔢 کد رزرو: {$booking['confirmation_code']}\n";
                        $ownerMessage .= "🏢 کسب‌وکار: {$booking['business_name']}";
                        
                        sendTelegramMessage($ownerTelegram['telegram_id'], $ownerMessage);
                    }
                }
                
            } catch (Exception $e) {
                error_log("❌ خطا در ارسال اطلاع‌رسانی لغو رزرو: " . $e->getMessage());
            }
            
            send_json_response('success', 'رزرو با موفقیت لغو شد.', [
                'booking_id' => $booking['id'],
                'confirmation_code' => $booking['confirmation_code'],
                'freed_time' => $booking['start_time'] . ' - ' . $booking['end_time']
            ]);
            
        } catch (PDOException $e) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }
            error_log("❌ خطا در cancel_booking: " . $e->getMessage());
            send_json_response('error', 'خطا در لغو رزرو. لطفاً دوباره تلاش کنید.', [], 500);
        } catch (Exception $e) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }
            error_log("❌ خطای عمومی در cancel_booking: " . $e->getMessage());
            send_json_response('error', 'خطای سرور در لغو رزرو.', [], 500);
         }
     }
     elseif ($action === 'get_user_bookings') {
         if (!$pdo) {
             send_json_response('error', 'Database connection required for this operation.', [], 500);
         }
         
         $user_id = $formData['user_id'] ?? null;
         
         if (empty($user_id)) {
             send_json_response('error', 'شناسه کاربر الزامی است.', [], 400);
         }
         
         try {
             // دریافت رزروهای کاربر؛ پیش‌فرض: رزروهای فعال/آینده، با include_history: همه (برای سابقه)
             $includeHistory = !empty($formData['include_history']);
             $bookingsStmt = $pdo->prepare("
                 SELECT 
                     b.id,
                     b.business_id,
                     b.confirmation_code,
                     b.booking_date,
                     b.start_time,
                     b.end_time,
                     b.customer_name,
                     b.customer_phone,
                     b.customer_notes,
                     b.status,
                     b.created_at,
                     bus.name as business_name,
                     bus.address as business_address,
                     bus.phone as business_phone
                 FROM bookings b
                 JOIN users u ON b.user_id = u.id
                 JOIN businesses bus ON b.business_id = bus.id
                 WHERE u.telegram_id = ? 
                 " . ($includeHistory
                     ? ""
                     : "AND b.status != 'cancelled'
                     AND (b.booking_date > CURDATE() OR (b.booking_date = CURDATE() AND b.start_time > CURTIME()))"
                 ) . "
                 ORDER BY b.booking_date " . ($includeHistory ? "DESC" : "ASC") . ", b.start_time " . ($includeHistory ? "DESC" : "ASC") . "
             ");
             $bookingsStmt->execute([$user_id]);
             $bookings = $bookingsStmt->fetchAll();
             
             // تبدیل تاریخ‌ها به شمسی و اضافه کردن اطلاعات اضافی
             foreach ($bookings as &$booking) {
                 $booking['jalali_date'] = convertToJalali($booking['booking_date']);
                 $booking['day_name'] = getDayName($booking['booking_date']);
                 
                 // بررسی امکان لغو (یک ساعت قبل از شروع)
                 $booking_datetime = $booking['booking_date'] . ' ' . $booking['start_time'];
                 $booking_timestamp = strtotime($booking_datetime);
                 $current_timestamp = time();
                 $time_diff_hours = ($booking_timestamp - $current_timestamp) / 3600;
                 
                 $booking['can_cancel'] = $time_diff_hours >= 1;
                 $booking['time_until_booking'] = round($time_diff_hours, 1);
             }
             
             send_json_response('success', 'لیست رزروها با موفقیت دریافت شد.', [
                 'bookings' => $bookings,
                 'total_count' => count($bookings)
             ]);
             
         } catch (PDOException $e) {
             error_log("❌ خطا در get_user_bookings: " . $e->getMessage());
             send_json_response('error', 'خطا در دریافت لیست رزروها.', [], 500);
         }
     }
     elseif ($action === 'get_marketer_dashboard') {
         // دریافت اطلاعات داشبورد بازاریاب
         debug_log("🏢 شروع دریافت اطلاعات داشبورد بازاریاب", 'INFO');
         debug_log("🔍 internal_user_id: " . ($internal_user_id ?? 'NULL'), 'INFO');
         
         if (!$pdo) {
             debug_log("❌ اتصال پایگاه داده موجود نیست", 'ERROR');
             send_json_response('error', 'Database connection required.', [], 500);
         }
         
         // بررسی marketer_code در درخواست
         $marketer_code = $formData['marketer_code'] ?? null;
         debug_log("🔍 marketer_code: " . ($marketer_code ?? 'NULL'), 'INFO');
         
         // بررسی احراز هویت کاربر یا وجود marketer_code
         if (!$internal_user_id && !$marketer_code) {
             debug_log("❌ کاربر احراز هویت نشده و marketer_code ارائه نشده", 'ERROR');
             send_json_response('error', 'احراز هویت ناموفق یا کد بازاریاب مورد نیاز است.', [], 401);
         }
         
         try {
             // دریافت اطلاعات بازاریاب
             if ($marketer_code) {
                 debug_log("🔍 جستجوی بازاریاب با marketer_code: " . $marketer_code, 'INFO');
                 $marketerStmt = $pdo->prepare("SELECT * FROM marketers WHERE marketer_code = ? AND is_active = 1");
                 $marketerStmt->execute([$marketer_code]);
             } else {
                 debug_log("🔍 جستجوی بازاریاب با user_id: " . $internal_user_id, 'INFO');
                 $marketerStmt = $pdo->prepare("SELECT * FROM marketers WHERE user_id = ? AND is_active = 1");
                 $marketerStmt->execute([$internal_user_id]);
             }
             $marketer = $marketerStmt->fetch();
             
             debug_log("📊 نتیجه جستجوی بازاریاب: " . ($marketer ? 'یافت شد' : 'یافت نشد'), 'INFO');
             if ($marketer) {
                 debug_log("👤 اطلاعات بازاریاب: ID=" . $marketer['id'] . ", کد بازاریاب=" . $marketer['marketer_code'], 'INFO');
             }
             
             if (!$marketer) {
                 debug_log("❌ بازاریاب یافت نشد", 'ERROR');
                 send_json_response('error', 'بازاریاب یافت نشد.', [], 404);
             }
             
             // دریافت کسب‌وکارهای معرفی شده با محاسبه کمیسیون صحیح
             $businessesStmt = $pdo->prepare("
                 SELECT 
                     b.*,
                     COUNT(bk.id) as total_bookings,
                     COALESCE(SUM(CASE WHEN bk.status != 'cancelled' THEN (m.commission_rate * 1000) END), 0) as commission_earned
                 FROM businesses b
                 JOIN marketers m ON b.marketer_id = m.id
                 LEFT JOIN bookings bk ON b.id = bk.business_id
                 WHERE b.marketer_id = ?
                 GROUP BY b.id
                 ORDER BY b.created_at DESC
             ");
             $businessesStmt->execute([$marketer['id']]);
             $businesses = $businessesStmt->fetchAll();
             
             // محاسبه آمار کلی
             $totalBusinesses = count($businesses);
             $totalBookings = array_sum(array_column($businesses, 'total_bookings'));
             $totalCommission = array_sum(array_column($businesses, 'commission_earned'));
             
             // آمار ماهانه (ماه جاری)
             $currentMonth = date('Y-m');
             $monthlyBusinessesStmt = $pdo->prepare("
                 SELECT COUNT(*) as count 
                 FROM businesses 
                 WHERE marketer_id = ? AND DATE_FORMAT(created_at, '%Y-%m') = ?
             ");
             $monthlyBusinessesStmt->execute([$marketer['id'], $currentMonth]);
             $monthlyBusinesses = $monthlyBusinessesStmt->fetchColumn();
             
             $monthlyBookingsStmt = $pdo->prepare("
                 SELECT COUNT(*) as count 
                 FROM bookings bk
                 JOIN businesses b ON bk.business_id = b.id
                 WHERE b.marketer_id = ? AND DATE_FORMAT(bk.created_at, '%Y-%m') = ?
             ");
             $monthlyBookingsStmt->execute([$marketer['id'], $currentMonth]);
             $monthlyBookings = $monthlyBookingsStmt->fetchColumn();
             
             $monthlyCommissionStmt = $pdo->prepare("
                 SELECT COALESCE(SUM(m.commission_rate * 1000), 0) as commission
                 FROM bookings bk
                 JOIN businesses b ON bk.business_id = b.id
                 JOIN marketers m ON b.marketer_id = m.id
                 WHERE b.marketer_id = ? AND bk.status != 'cancelled' AND DATE_FORMAT(bk.created_at, '%Y-%m') = ?
             ");
             $monthlyCommissionStmt->execute([$marketer['id'], $currentMonth]);
             $monthlyCommission = $monthlyCommissionStmt->fetchColumn();
             
             // آمار درآمد 6 ماه گذشته
             $monthlyRevenueStmt = $pdo->prepare("
                 SELECT 
                     DATE_FORMAT(bk.created_at, '%Y-%m') as month,
                     COALESCE(SUM(m.commission_rate * 1000), 0) as revenue
                 FROM bookings bk
                 JOIN businesses b ON bk.business_id = b.id
                 JOIN marketers m ON b.marketer_id = m.id
                 WHERE b.marketer_id = ? AND bk.status != 'cancelled' AND bk.created_at >= DATE_SUB(NOW(), INTERVAL 6 MONTH)
                 GROUP BY DATE_FORMAT(bk.created_at, '%Y-%m')
                 ORDER BY month ASC
             ");
             $monthlyRevenueStmt->execute([$marketer['id']]);
             $monthlyRevenue = $monthlyRevenueStmt->fetchAll();
             
             // تبدیل به آرایه 6 ماهه
             $revenueData = [];
             for ($i = 5; $i >= 0; $i--) {
                 $month = date('Y-m', strtotime("-$i months"));
                 $found = false;
                 foreach ($monthlyRevenue as $data) {
                     if ($data['month'] === $month) {
                         $revenueData[] = (float)$data['revenue'];
                         $found = true;
                         break;
                     }
                 }
                 if (!$found) {
                     $revenueData[] = 0;
                 }
             }
             
             // توزیع انواع کسب‌وکار
             $businessTypesStmt = $pdo->prepare("
                 SELECT business_type, COUNT(*) as count
                 FROM businesses
                 WHERE marketer_id = ?
                 GROUP BY business_type
             ");
             $businessTypesStmt->execute([$marketer['id']]);
             $businessTypesData = $businessTypesStmt->fetchAll();
             
             $businessTypes = [];
             foreach ($businessTypesData as $type) {
                 $businessTypes[$type['business_type']] = (int)$type['count'];
             }
             
             // آماده‌سازی پاسخ
             $dashboardData = [
                 'total_businesses' => $totalBusinesses,
                 'total_bookings' => $totalBookings,
                 'total_commission' => $totalCommission,
                 'commission_rate' => (float)$marketer['commission_rate'],
                 'monthly_businesses' => $monthlyBusinesses,
                 'monthly_bookings' => $monthlyBookings,
                 'monthly_commission' => $monthlyCommission,
                 'monthly_revenue' => $revenueData,
                 'business_types' => $businessTypes,
                 'businesses' => $businesses
             ];
             
             send_json_response('success', 'اطلاعات داشبورد با موفقیت دریافت شد.', $dashboardData);
             
         } catch (PDOException $e) {
             debug_log("❌ خطای PDO در داشبورد بازاریاب: " . $e->getMessage(), 'ERROR');
             debug_log("📍 خط خطا: " . $e->getLine() . " در فایل: " . $e->getFile(), 'ERROR');
             error_log("Error in marketer dashboard: " . $e->getMessage());
             send_json_response('error', 'خطا در دریافت اطلاعات داشبورد.', [], 500);
         } catch (Exception $e) {
             debug_log("❌ خطای عمومی در داشبورد بازاریاب: " . $e->getMessage(), 'ERROR');
             debug_log("📍 خط خطا: " . $e->getLine() . " در فایل: " . $e->getFile(), 'ERROR');
             error_log("General error in marketer dashboard: " . $e->getMessage());
             send_json_response('error', 'خطا در دریافت اطلاعات داشبورد.', [], 500);
         }
     }
     elseif ($action === 'purchase_plan') {
         // خرید پکیج اشتراک
         debug_log("💰 شروع فرآیند خرید پکیج", 'INFO');
         
         if (!$pdo) {
             debug_log("❌ اتصال پایگاه داده موجود نیست", 'ERROR');
             send_json_response('error', 'Database connection required.', [], 500);
         }
         
         $business_id = $formData['business_id'] ?? null;
         $plan_id = $formData['plan_id'] ?? null;
         
         if (!$business_id || !$plan_id) {
             send_json_response('error', 'شناسه کسب‌وکار و پکیج الزامی است.', [], 400);
         }
         
         try {
             // دریافت اطلاعات پکیج
             $planStmt = $pdo->prepare("SELECT * FROM SubscriptionPlans WHERE id = ?");
             $planStmt->execute([$plan_id]);
             $plan = $planStmt->fetch();
             
             if (!$plan) {
                 send_json_response('error', 'پکیج مورد نظر یافت نشد.', [], 404);
             }
             
             // دریافت اطلاعات کسب‌وکار
             $businessStmt = $pdo->prepare("SELECT * FROM businesses WHERE id = ?");
             $businessStmt->execute([$business_id]);
             $business = $businessStmt->fetch();
             
             if (!$business) {
                 send_json_response('error', 'کسب‌وکار یافت نشد.', [], 404);
             }
             
             // در حال حاضر فقط پیام نمایش داده می‌شود
             // در آینده می‌توان سیستم پرداخت واقعی را پیاده‌سازی کرد
             $priceInToman = $plan['price'] / 10; // تبدیل از ریال به تومان برای نمایش
             $message = "برای خرید پکیج \"{$plan['name']}\" به قیمت " . number_format($priceInToman) . " تومان، لطفاً با پشتیبانی تماس بگیرید.\n\n";
             $message .= "مشخصات پکیج:\n";
             $message .= "• نام: {$plan['name']}\n";
             $message .= "• قیمت: " . number_format($priceInToman) . " تومان\n";
             $message .= "• تعداد رزرو: " . ($plan['reservation_limit'] == -1 ? 'نامحدود' : $plan['reservation_limit']) . "\n";
             $message .= "• مدت: {$plan['duration_days']} روز\n\n";
             $message .= "📞 برای تکمیل خرید با ادمین ربات در تلگرام تماس بگیرید.";
             
             send_json_response('info', $message, [
                 'plan' => $plan,
                 'business' => $business
             ]);
             
         } catch (PDOException $e) {
             debug_log("❌ خطای PDO در خرید پکیج: " . $e->getMessage(), 'ERROR');
             error_log("Error in purchase plan: " . $e->getMessage());
             send_json_response('error', 'خطا در فرآیند خرید پکیج.', [], 500);
         } catch (Exception $e) {
             debug_log("❌ خطای عمومی در خرید پکیج: " . $e->getMessage(), 'ERROR');
             error_log("General error in purchase plan: " . $e->getMessage());
             send_json_response('error', 'خطا در فرآیند خرید پکیج.', [], 500);
         }
     }
     else {
         send_json_response('error', 'Invalid action specified.', [], 400);
     }
}
?>
