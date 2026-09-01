<?php
/**
 * اسکریپت جامع برای بررسی و درست کردن ساختار دیتابیس
 * اجرا: php project/fix_database_schema.php
 * 
 * نکته: این فایل اکنون فقط برای اجرای مستقل است.
 * جداول به صورت خودکار هنگام شروع ربات از طریق config.php ایجاد می‌شوند.
 */

require_once __DIR__ . '/config.php';
require_once __DIR__ . '/database_init.php';

if (!$pdo) {
    die("❌ خطا: اتصال به دیتابیس موجود نیست\n");
}

// اجرای initialization با verbose mode
echo "\n" . str_repeat("=", 60) . "\n";
echo "🔧 اسکریپت مستقل برای initialization دیتابیس\n";
echo str_repeat("=", 60) . "\n\n";

$result = initDatabase($pdo, true);

if ($result) {
    exit(0); // موفق
} else {
    exit(1); // ناموفق
}

?>
