<?php
/**
 * API زرین‌پال برای پرداخت مستقیم
 * این فایل شامل توابع مورد نیاز برای ایجاد تراکنش و هدایت به درگاه زرین‌پال است
 */

require_once 'debug_logger.php';

// تنظیمات زرین‌پال
define('ZARINPAL_MERCHANT_ID', '9d13bea5-2995-4ee4-905b-84419bf862f9'); // شناسه پذیرنده - باید از پنل زرین‌پال دریافت شود
define('ZARINPAL_SANDBOX', false); // true برای تست، false برای محیط واقعی
define('ZARINPAL_CALLBACK_URL', 'https://webnovo.ir/project/webapp/zarinpal_callback.php'); // آدرس بازگشت

// URL های API زرین‌پال
if (ZARINPAL_SANDBOX) {
    define('ZARINPAL_REQUEST_URL', 'https://sandbox.zarinpal.com/pg/rest/WebGate/PaymentRequest.json');
    define('ZARINPAL_VERIFY_URL', 'https://sandbox.zarinpal.com/pg/rest/WebGate/PaymentVerification.json');
    define('ZARINPAL_GATEWAY_URL', 'https://sandbox.zarinpal.com/pg/StartPay/');
} else {
    define('ZARINPAL_REQUEST_URL', 'https://api.zarinpal.com/pg/v4/payment/request.json');
    define('ZARINPAL_VERIFY_URL', 'https://api.zarinpal.com/pg/v4/payment/verify.json');
    define('ZARINPAL_GATEWAY_URL', 'https://www.zarinpal.com/pg/StartPay/');
}

/**
 * ایجاد درخواست پرداخت در زرین‌پال
 */
function createZarinpalPayment($amount, $description, $email = '', $mobile = '') {
    
    // آماده‌سازی metadata فقط در صورت وجود مقادیر معتبر
    $metadata = [];
    if (!empty($email) && is_string($email)) {
        $metadata['email'] = (string)$email;
    }
    if (!empty($mobile) && is_string($mobile)) {
        $metadata['mobile'] = (string)$mobile;
    }
    
    $data = [
        'merchant_id' => ZARINPAL_MERCHANT_ID,
        'amount' => $amount, // مبلغ به ریال
        'description' => $description,
        'callback_url' => ZARINPAL_CALLBACK_URL
    ];
    
    // اضافه کردن metadata فقط در صورت وجود داده
    if (!empty($metadata)) {
        $data['metadata'] = $metadata;
    }

    $jsonData = json_encode($data);
    $ch = curl_init();

    curl_setopt_array($ch, [
        CURLOPT_URL => ZARINPAL_REQUEST_URL,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_POST => true,
        CURLOPT_POSTFIELDS => $jsonData,
        CURLOPT_HTTPHEADER => [
            'Content-Type: application/json',
            'Content-Length: ' . strlen($jsonData)
        ],
        CURLOPT_TIMEOUT => 30,
        CURLOPT_SSL_VERIFYPEER => false
    ]);

    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $error = curl_error($ch);
    curl_close($ch);

    if ($error) {
        return [
            'success' => false,
            'error' => 'خطا در اتصال به زرین‌پال: ' . $error
        ];
    }

    $result = json_decode($response, true);

    if ($httpCode === 200 && isset($result['data']['code']) && $result['data']['code'] == 100) {
        return [
            'success' => true,
            'authority' => $result['data']['authority'],
            'payment_url' => ZARINPAL_GATEWAY_URL . $result['data']['authority']
        ];
    } else {
        $errorMessage = isset($result['errors']) ? implode(', ', $result['errors']) : 'خطای نامشخص';
        return [
            'success' => false,
            'error' => 'خطا در ایجاد تراکنش: ' . $errorMessage
        ];
    }
}

/**
 * تأیید پرداخت زرین‌پال
 */
function verifyZarinpalPayment($authority, $amount) {
    // تبدیل مبلغ به عدد صحیح برای جلوگیری از خطای زرین‌پال
    $amount = (int) $amount;
    
    debug_log("🔄 شروع تأیید پرداخت زرین‌پال", 'INFO', [
        'authority' => $authority,
        'amount' => $amount,
        'merchant_id' => ZARINPAL_MERCHANT_ID
    ]);
    
    $data = [
        'merchant_id' => ZARINPAL_MERCHANT_ID,
        'authority' => $authority,
        'amount' => $amount
    ];

    $jsonData = json_encode($data);
    
    debug_log("📤 ارسال درخواست به زرین‌پال", 'INFO', [
        'url' => ZARINPAL_VERIFY_URL,
        'data' => $data,
        'json_data' => $jsonData
    ]);
    
    $ch = curl_init();

    curl_setopt_array($ch, [
        CURLOPT_URL => ZARINPAL_VERIFY_URL,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_POST => true,
        CURLOPT_POSTFIELDS => $jsonData,
        CURLOPT_HTTPHEADER => [
            'Content-Type: application/json',
            'Content-Length: ' . strlen($jsonData)
        ],
        CURLOPT_TIMEOUT => 30,
        CURLOPT_SSL_VERIFYPEER => false
    ]);

    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $error = curl_error($ch);
    curl_close($ch);

    debug_log("📥 پاسخ از زرین‌پال دریافت شد", 'INFO', [
        'http_code' => $httpCode,
        'curl_error' => $error,
        'response' => $response,
        'authority' => $authority
    ]);

    if ($error) {
        debug_log("❌ خطا در اتصال به زرین‌پال", 'ERROR', [
            'curl_error' => $error,
            'authority' => $authority
        ]);
        return [
            'success' => false,
            'error' => 'خطا در اتصال به زرین‌پال: ' . $error
        ];
    }

    $result = json_decode($response, true);
    
    debug_log("🔍 تجزیه پاسخ زرین‌پال", 'INFO', [
        'parsed_result' => $result,
        'authority' => $authority,
        'http_code' => $httpCode
    ]);

    if ($httpCode === 200 && isset($result['data']['code']) && ($result['data']['code'] == 100 || $result['data']['code'] == 101)) {
        debug_log("✅ تأیید پرداخت موفق", 'INFO', [
            'ref_id' => $result['data']['ref_id'],
            'authority' => $authority,
            'code' => $result['data']['code'],
            'message' => $result['data']['message'] ?? ''
        ]);
        return [
            'success' => true,
            'ref_id' => $result['data']['ref_id'],
            'card_hash' => $result['data']['card_hash'] ?? '',
            'card_pan' => $result['data']['card_pan'] ?? ''
        ];
    } else {
        $errorMessage = isset($result['errors']) ? implode(', ', $result['errors']) : 'خطای نامشخص';
        debug_log("❌ تأیید پرداخت ناموفق", 'ERROR', [
            'error_message' => $errorMessage,
            'result' => $result,
            'authority' => $authority,
            'http_code' => $httpCode,
            'expected_code' => 100,
            'actual_code' => $result['data']['code'] ?? 'نامشخص'
        ]);
        return [
            'success' => false,
            'error' => 'خطا در تأیید پرداخت: ' . $errorMessage
        ];
    }
}

/**
 * دریافت قیمت پلن بر اساس ID از دیتابیس
 */
function getPlanPrice($planId) {
    try {
        // اتصال به دیتابیس
        require_once 'woocommerce_config.php';
        $pdo = getBotDatabaseConnection();
        
        $stmt = $pdo->prepare("SELECT price FROM SubscriptionPlans WHERE id = ? AND is_active = 1");
        $stmt->execute([$planId]);
        $plan = $stmt->fetch();
        
        return $plan ? (int)$plan['price'] : 0;
    } catch (Exception $e) {
        error_log("خطا در دریافت قیمت پلن: " . $e->getMessage());
        return 0;
    }
}

/**
 * دریافت نام پلن بر اساس ID از دیتابیس
 */
function getPlanName($planId) {
    try {
        // اتصال به دیتابیس
        require_once 'woocommerce_config.php';
        $pdo = getBotDatabaseConnection();
        
        $stmt = $pdo->prepare("SELECT name FROM SubscriptionPlans WHERE id = ? AND is_active = 1");
        $stmt->execute([$planId]);
        $plan = $stmt->fetch();
        
        return $plan ? $plan['name'] : 'پکیج نامشخص';
    } catch (Exception $e) {
        error_log("خطا در دریافت نام پلن: " . $e->getMessage());
        return 'پکیج نامشخص';
    }
}
?>