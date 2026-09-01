<?php
// اسکریپت تست اتصال با مقادیر BOT_DB_* (برای دیباگ محلی روی هاست)
// اجرا: php check_bot_db.php
// هشدار: پس از اجرا این فایل را حذف یا خارج از مسیر عمومی نگه دارید.

function parseConfigConstants($path, $keys = array()) {
    $found = array();
    if (!file_exists($path) || !is_readable($path)) return $found;
    $content = file_get_contents($path);
    foreach ($keys as $k) {
        $pattern = '/define\(\s*[\"\']' . preg_quote($k, '/') . '[\"\']\s*,\s*([\"\'])(.*?)\\1\s*\)/s';
        if (preg_match($pattern, $content, $m)) {
            $found[$k] = $m[2];
        }
    }
    return $found;
}

$cfgPath = __DIR__ . '/webapp/woocommerce_config.php';
$keys = array('BOT_DB_HOST','BOT_DB_NAME','BOT_DB_USER','BOT_DB_PASS','BOT_DB_CHARSET');
$parsed = parseConfigConstants($cfgPath, $keys);

if (empty($parsed)) {
    echo "خطا: نتونستم مقادیر BOT_DB_* رو از webapp/woocommerce_config.php بخونم.\n";
    exit(2);
}

echo "استفاده از مقادیر BOT_DB_* برای تست اتصال:\n";
echo "HOST: " . ($parsed['BOT_DB_HOST'] ?? '(not set)') . "\n";
echo "DBNAME: " . ($parsed['BOT_DB_NAME'] ?? '(not set)') . "\n";
echo "USER: " . ($parsed['BOT_DB_USER'] ?? '(not set)') . "\n";
echo "CHARSET: " . ($parsed['BOT_DB_CHARSET'] ?? 'utf8mb4') . "\n";

$host = $parsed['BOT_DB_HOST'];
$name = $parsed['BOT_DB_NAME'];
$user = $parsed['BOT_DB_USER'];
$pass = $parsed['BOT_DB_PASS'];
$charset = isset($parsed['BOT_DB_CHARSET']) ? $parsed['BOT_DB_CHARSET'] : 'utf8mb4';

if (!$host || !$name || !$user) {
    echo "تنظیمات ناقص‌اند (HOST/NAME/USER).\n";
    exit(2);
}

$dsn = "mysql:host={$host};dbname={$name};charset={$charset}";
try {
    $options = array(
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES => false,
    );
    $pdo = new PDO($dsn, $user, $pass, $options);
    $pdo->exec("SET NAMES " . $charset);
    echo "اتصال MySQL با BOT_DB_* موفق بود.\n";
    // بررسی اینکه آیا دیتابیس موجود است و قابل انتخاب است
    $stmt = $pdo->query("SELECT DATABASE() as db");
    $cur = $stmt->fetch();
    echo "پایگاه داده فعلی: " . ($cur['db'] ?? '(unknown)') . "\n";
    exit(0);
} catch (PDOException $e) {
    echo "اتصال ناموفق — PDOException: " . $e->getMessage() . "\n";
    exit(1);
}

?>
