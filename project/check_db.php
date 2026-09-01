<?php
// اسکریپت بررسی اتصال به دیتابیس و وجود جداول ضروری
// اجرا: php check_db.php

// بارگذاری تنظیمات از فایل پیکربندی (اگر فایل وجود نداشت، از استخراج دستی استفاده خواهد شد)
if (file_exists(dirname(__FILE__) . '/config.php')) {
    require_once dirname(__FILE__) . '/config.php';
}

// لیست جداول مورد انتظار
$expectedTables = array('users', 'marketers', 'businesses', 'services', 'bookings');

// توابع کمکی
function parseConfigConstants($path, $keys = array())
{
    $found = array();
    if (!file_exists($path) || !is_readable($path)) {
        return $found;
    }
    $content = file_get_contents($path);
    foreach ($keys as $k) {
        $pattern = "/define\(\s*['\"]" . preg_quote($k, '/') . "['\"]\s*,\s*(['\"])(.*?)\\1\s*\)/s";
        if (preg_match($pattern, $content, $m)) {
            $found[$k] = $m[2];
        }
    }
    return $found;
}

function tryConnectMySQL($overrides = array())
{
    $host = isset($overrides['DB_HOST']) ? $overrides['DB_HOST'] : (defined('DB_HOST') ? DB_HOST : null);
    $name = isset($overrides['DB_NAME']) ? $overrides['DB_NAME'] : (defined('DB_NAME') ? DB_NAME : null);
    $user = isset($overrides['DB_USER']) ? $overrides['DB_USER'] : (defined('DB_USER') ? DB_USER : null);
    $pass = isset($overrides['DB_PASS']) ? $overrides['DB_PASS'] : (defined('DB_PASS') ? DB_PASS : '');
    $charset = isset($overrides['DB_CHARSET']) ? $overrides['DB_CHARSET'] : (defined('DB_CHARSET') ? DB_CHARSET : 'utf8mb4');

    if (!$host || !$name || !$user) {
        return array(false, 'تنظیمات MySQL موجود نیست (DB_HOST/DB_NAME/DB_USER).');
    }

    $dsn = "mysql:host={$host};dbname={$name};charset={$charset}";
    try {
        $options = array(
            PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES   => false,
        );
        $pdo = new PDO($dsn, $user, $pass, $options);
        $pdo->exec("SET NAMES " . $charset);
        return array($pdo, null);
    } catch (PDOException $e) {
        return array(false, "اتصال MySQL ناموفق: " . $e->getMessage());
    }
}

function tryConnectSQLite($overrides = array())
{
    $path = isset($overrides['DB_PATH']) ? $overrides['DB_PATH'] : (defined('DB_PATH') ? DB_PATH : null);
    if (!$path) {
        return array(false, 'مسیر SQLite (DB_PATH) تعریف نشده است.');
    }
    if (!file_exists($path)) {
        return array(false, "فایل SQLite پیدا نشد: $path"); 
    }
    try {
        $pdo = new PDO('sqlite:' . $path);
        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        return array($pdo, null);
    } catch (PDOException $e) {
        return array(false, "اتصال SQLite ناموفق: " . $e->getMessage());
    }
}

function tableExists(PDO $pdo, $table)
{
    $driver = $pdo->getAttribute(PDO::ATTR_DRIVER_NAME);
    if ($driver === 'mysql') {
        $stmt = $pdo->prepare("SELECT TABLE_NAME FROM information_schema.tables WHERE table_schema = :schema AND table_name = :t LIMIT 1");
        $schema = defined('DB_NAME') ? DB_NAME : null;
        $stmt->execute(array(':schema' => $schema, ':t' => $table));
        return (bool)$stmt->fetchColumn();
    } elseif ($driver === 'sqlite') {
        $stmt = $pdo->prepare("SELECT name FROM sqlite_master WHERE type='table' AND name = :t LIMIT 1");
        $stmt->execute(array(':t' => $table));
        return (bool)$stmt->fetchColumn();
    } else {
        // برای درایورهای دیگر تلاش می‌کنیم با پرس‌وجوی ساده
        try {
            $res = $pdo->query("SELECT 1 FROM " . $table . " LIMIT 1");
            return $res !== false;
        } catch (Exception $e) {
            return false;
        }
    }
}

// شروع بررسی
$results = array();

// ابتدا تلاش برای اتصال MySQL
$configPath = dirname(__FILE__) . '/config.php';
$overrides = array();
if (!defined('DB_HOST') || !defined('DB_NAME') || !defined('DB_USER') || !defined('DB_CHARSET') || !defined('DB_PATH')) {
    $parsed = parseConfigConstants($configPath, array('DB_HOST', 'DB_NAME', 'DB_USER', 'DB_PASS', 'DB_CHARSET', 'DB_PATH'));
    if (!empty($parsed)) {
        $overrides = $parsed;
    }
}
list($pdoConn, $err) = tryConnectMySQL($overrides);
if ($pdoConn && $pdoConn instanceof PDO) {
    echo "اتصال به MySQL برقرار شد.\n";
    $pdo = $pdoConn;
    $results['connection'] = 'mysql';
} else {
    echo "اتصال به MySQL برقرار نشد: $err\n";
    // سپس تلاش برای SQLite
    list($pdoConn2, $err2) = tryConnectSQLite($overrides);
    if ($pdoConn2 && $pdoConn2 instanceof PDO) {
        echo "ارتباط با SQLite برقرار شد.\n";
        $pdo = $pdoConn2;
        $results['connection'] = 'sqlite';
    } else {
        echo "اتصال به SQLite هم برقرار نشد: $err2\n";
        echo "هیچ اتصال پایگاه داده‌ای برقرار نیست. لطفاً تنظیمات در `config.php` را بررسی کنید.\n";
        exit(1);
    }
}

// بررسی وجود جداول
$missing = array();
foreach ($expectedTables as $t) {
    $exists = tableExists($pdo, $t);
    if ($exists) {
        echo "جدول $t: موجود ✅\n";
    } else {
        echo "جدول $t: یافت نشد ❌\n";
        $missing[] = $t;
    }
}

if (count($missing) === 0) {
    echo "تمام جداول مورد انتظار موجود هستند.\n";
    exit(0);
} else {
    echo "جداول مفقود: " . implode(', ', $missing) . "\n";
    echo "برای ایجاد جداول می‌توانید از `create_all_tables.php` استفاده کنید یا اسکریپت ایجاد جدول را اجرا نمایید.\n";
    exit(2);
}

?>
