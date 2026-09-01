<?php
// File: /bot/setup_commands.php
// اسکریپت تنظیم دستورات ربات برای نمایش دائمی در منو

// تنظیمات ربات تلگرام (بدون نیاز به پایگاه داده)
define('BOT_TOKEN', '8332279975:AAFtyAuvjC7Pii3SVvbs-vYz6YIcjRkUPXw');
define('BOT_API_URL', 'https://api.telegram.org/bot' . BOT_TOKEN . '/');

// تابع ارسال درخواست به API تلگرام
function apiRequest($method, $parameters) {
    $url = BOT_API_URL . $method;
    
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query($parameters));
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_TIMEOUT, 30);
    
    $response = curl_exec($ch);
    $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    if ($response === false) {
        return ['ok' => false, 'error' => 'cURL Error'];
    }
    
    $decoded = json_decode($response, true);
    return $decoded ?: ['ok' => false, 'error' => 'Invalid JSON response'];
}

// تنظیم دستورات ربات
function setBotCommands() {
    $commands = [
        [
            'command' => 'start',
            'description' => '🏠 شروع و منوی اصلی'
        ],
        [
            'command' => 'help',
            'description' => '❓ راهنما و پشتیبانی'
        ],
        [
            'command' => 'search',
            'description' => '🔍 جستجوی سریع کسب‌وکار'
        ]
    ];
    
    $data = [
        'commands' => json_encode($commands, JSON_UNESCAPED_UNICODE)
    ];
    
    $result = apiRequest('setMyCommands', $data);
    
    echo "🤖 تنظیم دستورات ربات:\n";
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n";
    
    if ($result && isset($result['ok']) && $result['ok']) {
        echo "✅ دستورات با موفقیت تنظیم شدند!\n\n";
        echo "📋 دستورات تنظیم شده:\n";
        foreach ($commands as $cmd) {
            echo "• /{$cmd['command']} - {$cmd['description']}\n";
        }
        echo "\n💡 این دستورات اکنون در منوی پایین چت کاربران نمایش داده می‌شوند.\n";
    } else {
        echo "❌ خطا در تنظیم دستورات!\n";
        echo "📄 پاسخ API: " . json_encode($result, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT) . "\n";
    }
    
    return $result;
}

// اجرای تنظیم دستورات
if (php_sapi_name() === 'cli' || isset($_GET['setup'])) {
    setBotCommands();
} else {
    echo "برای تنظیم دستورات، ?setup=1 را به URL اضافه کنید.";
}
?>