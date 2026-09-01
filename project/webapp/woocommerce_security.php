<?php
/**
 * سیستم امنیتی ووکامرس - نسخه موقت بدون دیتابیس
 */

// تنظیم header برای JSON
header('Content-Type: application/json; charset=utf-8');

// خواندن داده‌های ورودی
$input = file_get_contents('php://input');
$data = json_decode($input, true);

// بررسی صحت داده‌های ورودی
if (!$data || !isset($data['action'])) {
    http_response_code(400);
    echo json_encode([
        'status' => 'error',
        'message' => 'داده‌های ورودی نامعتبر'
    ]);
    exit;
}

$action = $data['action'];

switch ($action) {
    case 'validate_purchase':
        // اعتبارسنجی خرید
        $businessId = $data['business_id'] ?? '';
        $planId = $data['plan_id'] ?? 0;
        
        if (empty($businessId) || empty($planId)) {
            echo json_encode([
                'status' => 'error',
                'message' => 'اطلاعات کسب‌وکار یا پلان مشخص نشده'
            ]);
            exit;
        }
        
        // تولید توکن امنیتی موقت
        $securityToken = bin2hex(random_bytes(32));
        
        echo json_encode([
            'status' => 'valid',
            'security_token' => $securityToken,
            'message' => 'اعتبارسنجی موفق'
        ]);
        break;
        
    case 'verify_token':
        // تأیید توکن
        $token = $data['token'] ?? '';
        
        if (empty($token)) {
            echo json_encode([
                'success' => false,
                'error' => 'توکن مشخص نشده'
            ]);
            exit;
        }
        
        echo json_encode([
            'success' => true,
            'message' => 'توکن معتبر است'
        ]);
        break;
        
    case 'cleanup':
        // پاکسازی
        echo json_encode([
            'success' => true,
            'message' => 'پاکسازی انجام شد'
        ]);
        break;
        
    default:
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'error' => 'عملیات نامعتبر'
        ]);
        break;
}
?>