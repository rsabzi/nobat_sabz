<?php
// File: /nobatsabz/bot/set_webhook.php
// این اسکریپت را فقط یک بار اجرا کنید تا ربات شما به تلگرام متصل شود.

// بارگذاری تنظیمات اصلی
require_once __DIR__ . '/../config.php';

// تابع ارسال درخواست به API تلگرام (مشابه تابع موجود در webhook.php)
function apiRequest($method, $data = []) {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, BOT_API_URL . $method);
    curl_setopt($ch, CURLOPT_POST, 1);
    curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query($data));
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    $result = curl_exec($ch);
    curl_close($ch);
    return json_decode($result, true);
}

// داده‌هایی که برای تنظیم وبهوک به تلگرام ارسال می‌شود
$params = [
    'url' => WEBHOOK_URL
];

// ارسال درخواست به تلگرام
$response = apiRequest('setWebhook', $params);

// نمایش نتیجه به کاربر
header('Content-Type: text/plain; charset=utf-8');
if ($response['ok']) {
    echo "✅ وبهوک با موفقیت تنظیم شد!\n";
    echo "ربات شما اکنون به آدرس زیر متصل است:\n";
    echo WEBHOOK_URL . "\n\n";
    echo "پاسخ کامل تلگرام:\n";
    print_r($response);
} else {
    echo "❌ خطا در تنظیم وبهوک!\n";
    echo "کد خطا: " . ($response['error_code'] ?? 'N/A') . "\n";
    echo "توضیحات: " . ($response['description'] ?? 'Unknown error') . "\n\n";
    echo "نکات عیب‌یابی:\n";
    echo "1. مطمئن شوید آدرس WEBHOOK_URL در فایل config.php صحیح و با https شروع می‌شود.\n";
    echo "2. مطمئن شوید فایل webhook.php روی سرور شما قابل دسترسی است.\n";
    echo "3. توکن ربات (BOT_TOKEN) را بررسی کنید.\n";
}

?>