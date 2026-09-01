<?php
/**
 * اسکریپت اضافه کردن ستون‌های گم شده به جدول businesses
 * اجرا: php project/alter_businesses_table.php
 */

require_once __DIR__ . '/config.php';

if (!$pdo) {
    echo "❌ خطا: اتصال به دیتابیس موجود نیست" . PHP_EOL;
    exit(1);
}

try {
    // بررسی وجود ستون marketer_id
    $checkMarketerIdStmt = $pdo->query("SHOW COLUMNS FROM businesses LIKE 'marketer_id'");
    if ($checkMarketerIdStmt->rowCount() === 0) {
        echo "🔄 اضافه کردن ستون marketer_id..." . PHP_EOL;
        $pdo->exec("ALTER TABLE businesses ADD COLUMN marketer_id INT(11) DEFAULT NULL COMMENT 'شناسه بازاریاب (ارجاع به جدول marketers)' AFTER advance_booking_days");
        echo "✅ ستون marketer_id با موفقیت اضافه شد" . PHP_EOL;
    } else {
        echo "✓ ستون marketer_id قبلاً وجود دارد" . PHP_EOL;
    }
    
    // بررسی وجود ستون free_reservation_tokens
    $checkTokensStmt = $pdo->query("SHOW COLUMNS FROM businesses LIKE 'free_reservation_tokens'");
    if ($checkTokensStmt->rowCount() === 0) {
        echo "🔄 اضافه کردن ستون free_reservation_tokens..." . PHP_EOL;
        $pdo->exec("ALTER TABLE businesses ADD COLUMN free_reservation_tokens INT(11) DEFAULT 0 COMMENT 'تعداد رزروهای رایگان' AFTER marketer_id");
        echo "✅ ستون free_reservation_tokens با موفقیت اضافه شد" . PHP_EOL;
    } else {
        echo "✓ ستون free_reservation_tokens قبلاً وجود دارد" . PHP_EOL;
    }
    
    // نمایش ساختار جدول بعد از تغییرات
    echo "\n📋 ساختار جدول businesses بعد از تغییرات:" . PHP_EOL;
    $columnsStmt = $pdo->query("SHOW COLUMNS FROM businesses");
    $columns = $columnsStmt->fetchAll();
    foreach ($columns as $col) {
        echo "  - {$col['Field']}: {$col['Type']}" . PHP_EOL;
    }
    
    echo "\n✅ تمام تغییرات با موفقیت اعمال شد!" . PHP_EOL;
    
} catch (PDOException $e) {
    echo "❌ خطای دیتابیس: " . $e->getMessage() . PHP_EOL;
    exit(1);
} catch (Exception $e) {
    echo "❌ خطا: " . $e->getMessage() . PHP_EOL;
    exit(1);
}
?>
