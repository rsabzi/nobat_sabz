<?php
/**
 * پیکربندی ووکامرس
 * این فایل شامل تنظیمات اتصال به ووکامرس و دیتابیس است
 */

// تنظیمات ووکامرس API
define('WOOCOMMERCE_URL', 'https://webnovo.ir'); // آدرس سایت شما
define('WOOCOMMERCE_CONSUMER_KEY', 'ck_095b43512b7feb0a2d12192a77549aa94d47b043'); // کلید مصرف‌کننده - باید از پنل ووکامرس دریافت شود
define('WOOCOMMERCE_CONSUMER_SECRET', 'cs_c31ae45d3a6eb4785f00a436bffb9d5f00ebcb49'); // رمز مصرف‌کننده - باید از پنل ووکامرس دریافت شود
define('WOOCOMMERCE_API_VERSION', 'wc/v3'); // نسخه API

// تنظیمات دیتابیس ربات — ابتدا فایل config.php اصلی را بارگذاری می‌کنیم تا از مقادیر مرکزی استفاده شود
require_once __DIR__ . '/../config.php';

// اگر می‌خواهیم ربات از دیتابیس مرکزی استفاده کند، BOT_DB_* را بر اساس DB_* تعیین می‌کنیم
define('BOT_DB_HOST', DB_HOST);
define('BOT_DB_NAME', DB_NAME);
define('BOT_DB_USER', DB_USER);
define('BOT_DB_PASS', DB_PASS);
define('BOT_DB_CHARSET', DB_CHARSET);

// تنظیمات امنیتی
define('WEBHOOK_SECRET', 'wc_webhook_secret_' . hash('sha256', 'webnovo_woocommerce_2024')); // کلید مخفی webhook
define('SECURITY_SALT', 'security_salt_' . hash('sha256', 'webnovo_security_2024')); // نمک امنیتی
define('MAX_FAILED_ATTEMPTS', 5); // حداکثر تلاش ناموفق
define('BLOCK_DURATION', 3600); // مدت مسدودی IP (ثانیه)
define('TOKEN_EXPIRY', 900); // انقضای توکن امنیتی (ثانیه - 15 دقیقه)

// تنظیمات تلگرام (توکن از config.php مرکزی نیز در دسترس است)
define('TELEGRAM_BOT_TOKEN', BOT_TOKEN);
define('TELEGRAM_API_URL', 'https://api.telegram.org/bot' . TELEGRAM_BOT_TOKEN);

// تنظیمات پرداخت
define('PAYMENT_SUCCESS_REDIRECT', 'https://t.me/sabztime_bot'); // هدایت پس از پرداخت موفق
define('PAYMENT_CANCEL_REDIRECT', 'https://webnovo.ir/project/webapp/subscription.html?status=cancelled'); // هدایت پس از لغو پرداخت

// تنظیمات لاگ
define('LOG_ENABLED', true);
define('LOG_FILE', __DIR__ . '/logs/woocommerce.log');
define('LOG_LEVEL', 'DEBUG'); // DEBUG, INFO, WARNING, ERROR

// تنظیمات محیط
define('ENVIRONMENT', 'development'); // development, staging, production

// SKU محصولات ووکامرس (باید با فایل woocommerce_setup.php هماهنگ باشد)
define('WOOCOMMERCE_SKUS', [
    1 => 'subscription-basic',    // پکیج اولیه
    2 => 'subscription-medium',   // پکیج متوسط
    3 => 'subscription-advanced' // پکیج پیشرفته
]);

// تنظیمات اضافی
define('ORDER_EXPIRY_HOURS', 1); // انقضای سفارش (ساعت)
define('CLEANUP_INTERVAL_HOURS', 24); // فاصله پاکسازی (ساعت)

/**
 * تابع دریافت اتصال دیتابیس
 */
function getBotDatabaseConnection() {
    static $pdo_bot = null;

    if ($pdo_bot === null) {
        // آماده‌سازی گزینه‌های PDO مشترک
        $optionsCommon = [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES => false,
        ];

        // تلاش اولیه با مقادیر BOT_DB_*
        try {
            $dsnBot = "mysql:host=" . BOT_DB_HOST . ";dbname=" . BOT_DB_NAME . ";charset=" . BOT_DB_CHARSET;
            $optionsBot = $optionsCommon;
            if (defined('PDO::MYSQL_ATTR_INIT_COMMAND')) {
                $optionsBot[PDO::MYSQL_ATTR_INIT_COMMAND] = "SET NAMES " . BOT_DB_CHARSET;
            }

            $pdo_bot = new PDO($dsnBot, BOT_DB_USER, BOT_DB_PASS, $optionsBot);
            error_log("BOT DB connection successful: " . BOT_DB_NAME);
            return $pdo_bot;
        } catch (PDOException $e) {
            // لاگ خطای اتصال حساب BOT_DB
            error_log("BOT DB connection failed: " . $e->getMessage());

            // اگر اتصال اصلی از config.php موجود است، از آن fallback کن
            global $pdo;
            if (isset($pdo) && $pdo instanceof PDO) {
                error_log("Falling back to main DB connection (DB_NAME: " . DB_NAME . ") for bot operations.");
                $pdo_bot = $pdo;
                return $pdo_bot;
            }

            // تلاش ثانویه مستقیم با مقادیر DB_* در صورت عدم وجود $pdo
            try {
                $dsnMain = "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=" . DB_CHARSET;
                $optionsMain = $optionsCommon;
                if (defined('PDO::MYSQL_ATTR_INIT_COMMAND')) {
                    $optionsMain[PDO::MYSQL_ATTR_INIT_COMMAND] = "SET NAMES " . DB_CHARSET;
                }
                $pdo_bot = new PDO($dsnMain, DB_USER, DB_PASS, $optionsMain);
                error_log("Connected to main DB as fallback: " . DB_NAME);
                return $pdo_bot;
            } catch (PDOException $e2) {
                error_log("Fallback main DB connection failed: " . $e2->getMessage());
                throw new Exception("خطا در اتصال به دیتابیس");
            }
        }
    }

    return $pdo_bot;
}

/**
 * تابع لاگ‌گیری
 */
function logMessage($level, $message, $context = []) {
    if (!LOG_ENABLED) return;
    
    $logLevels = ['DEBUG' => 0, 'INFO' => 1, 'WARNING' => 2, 'ERROR' => 3];
    $currentLevel = $logLevels[LOG_LEVEL] ?? 1;
    $messageLevel = $logLevels[$level] ?? 1;
    
    if ($messageLevel < $currentLevel) return;
    
    $timestamp = date('Y-m-d H:i:s');
    $contextStr = !empty($context) ? ' | Context: ' . json_encode($context, JSON_UNESCAPED_UNICODE) : '';
    $logEntry = "[{$timestamp}] [{$level}] {$message}{$contextStr}" . PHP_EOL;
    
    // ایجاد پوشه لاگ در صورت عدم وجود
    $logDir = dirname(LOG_FILE);
    if (!is_dir($logDir)) {
        mkdir($logDir, 0755, true);
    }
    
    file_put_contents(LOG_FILE, $logEntry, FILE_APPEND | LOCK_EX);
}

/**
 * تابع ارسال درخواست HTTP
 */
function makeHttpRequest($url, $method = 'GET', $data = null, $headers = []) {
    $ch = curl_init();
    
    curl_setopt_array($ch, [
        CURLOPT_URL => $url,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_TIMEOUT => 30,
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_SSL_VERIFYPEER => ENVIRONMENT === 'production',
        CURLOPT_USERAGENT => 'WooCommerce-Bot-Integration/1.0'
    ]);
    
    if ($method === 'POST') {
        curl_setopt($ch, CURLOPT_POST, true);
        if ($data) {
            curl_setopt($ch, CURLOPT_POSTFIELDS, $data);
        }
    } elseif ($method === 'PUT') {
        curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'PUT');
        if ($data) {
            curl_setopt($ch, CURLOPT_POSTFIELDS, $data);
        }
    }
    
    if (!empty($headers)) {
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    }
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $error = curl_error($ch);
    
    curl_close($ch);
    
    if ($error) {
        throw new Exception("HTTP Request Error: " . $error);
    }
    
    return [
        'status_code' => $httpCode,
        'body' => $response
    ];
}

/**
 * تابع تولید توکن امنیتی
 */
function generateSecurityToken($businessId, $planId) {
    $data = $businessId . '|' . $planId . '|' . time() . '|' . SECURITY_SALT;
    return hash('sha256', $data);
}

/**
 * تابع اعتبارسنجی توکن امنیتی
 */
function validateSecurityToken($token, $businessId, $planId) {
    try {
        $pdo = getBotDatabaseConnection();
        $stmt = $pdo->prepare("
            SELECT id FROM security_tokens 
            WHERE token = ? AND business_id = ? AND plan_id = ? 
            AND used = 0 AND expires_at > NOW()
        ");
        $stmt->execute([$token, $businessId, $planId]);
        
        return $stmt->fetch() !== false;
    } catch (Exception $e) {
        logMessage('ERROR', 'Token validation failed', ['error' => $e->getMessage()]);
        return false;
    }
}

/**
 * تابع دریافت IP کاربر
 */
function getUserIP() {
    $ipKeys = ['HTTP_CF_CONNECTING_IP', 'HTTP_X_FORWARDED_FOR', 'HTTP_X_FORWARDED', 'HTTP_X_CLUSTER_CLIENT_IP', 'HTTP_FORWARDED_FOR', 'HTTP_FORWARDED', 'REMOTE_ADDR'];
    
    foreach ($ipKeys as $key) {
        if (array_key_exists($key, $_SERVER) && !empty($_SERVER[$key])) {
            $ips = explode(',', $_SERVER[$key]);
            $ip = trim($ips[0]);
            
            if (filter_var($ip, FILTER_VALIDATE_IP, FILTER_FLAG_NO_PRIV_RANGE | FILTER_FLAG_NO_RES_RANGE)) {
                return $ip;
            }
        }
    }
    
    return $_SERVER['REMOTE_ADDR'] ?? '0.0.0.0';
}

// بررسی وجود توابع مورد نیاز
if (!function_exists('curl_init')) {
    throw new Exception('cURL extension is required');
}

if (!class_exists('PDO')) {
    throw new Exception('PDO extension is required');
}
?>