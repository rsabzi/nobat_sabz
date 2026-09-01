<?php
/**
 * سیستم خرید مستقیم ساده - درگاه زرین‌پال
 */

define('WOOCOMMERCE_URL', 'https://webnovo.ir');

require_once 'zarinpal_api.php';
require_once 'woocommerce_config.php';

header('Content-Type: application/json; charset=utf-8');

// خواندن و بررسی داده‌های ورودی
$input = json_decode(file_get_contents('php://input'), true);
if (!$input || !isset($input['action'])) {
    http_response_code(400);
    exit(json_encode(['status' => 'error', 'message' => 'داده‌های ورودی نامعتبر']));
}

switch ($input['action']) {
    case 'create_direct_order':
        createDirectOrder($input);
        break;
    case 'check_order_status':
        checkOrderStatus($_POST['authority'] ?? '');
        break;
    case 'cancel_order':
        cancelOrder($_POST['authority'] ?? '');
        break;
    default:
        http_response_code(400);
        exit(json_encode(['status' => 'error', 'message' => 'عملیات نامعتبر']));
}

function createDirectOrder($data) {
    // بررسی داده‌های ضروری
    $required = ['business_id', 'plan_id', 'security_token'];
    foreach ($required as $field) {
        if (empty($data[$field])) {
            exit(json_encode(['status' => 'error', 'message' => 'اطلاعات ضروری مشخص نشده']));
        }
    }
    
    // دریافت اطلاعات پلن
    try {
        $pdo = getBotDatabaseConnection();
        $stmt = $pdo->prepare("SELECT price, name FROM SubscriptionPlans WHERE id = ? AND is_active = 1");
        $stmt->execute([$data['plan_id']]);
        $plan = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$plan) {
            exit(json_encode(['status' => 'error', 'message' => 'پلن انتخابی معتبر نیست']));
        }
        
        $amountRial = (int) $plan['price'];
        if ($amountRial < 1000) {
            exit(json_encode(['status' => 'error', 'message' => 'مبلغ باید حداقل 1000 ریال باشد']));
        }
        
    } catch (Exception $e) {
        exit(json_encode(['status' => 'error', 'message' => 'خطا در دریافت اطلاعات پلن']));
    }
    
    // ایجاد سفارش و پرداخت
    $orderId = 'ORDER_' . time() . '_' . rand(1000, 9999);
    $description = "خرید {$plan['name']} - سفارش $orderId";
    $userData = $data['user_data'] ?? [];
    
    $zarinpalResult = createZarinpalPayment($amountRial, $description, 
        $userData['email'] ?? '', $userData['phone'] ?? '');
    
    if (!$zarinpalResult['success']) {
        exit(json_encode(['status' => 'error', 'message' => 'خطا در اتصال به درگاه پرداخت']));
    }
    
    // ذخیره در دیتابیس
    try {
        $stmt = $pdo->prepare("INSERT INTO payments (business_id, plan_id, amount, authority, status, created_at) VALUES (?, ?, ?, ?, 'pending', NOW())");
        $stmt->execute([$data['business_id'], $data['plan_id'], $amountRial, $zarinpalResult['authority']]);
        error_log("✅ رکورد پرداخت ذخیره شد - Authority: {$zarinpalResult['authority']}, Amount: $amountRial ریال");
    } catch (Exception $e) {
        error_log("❌ خطا در ذخیره رکورد پرداخت: " . $e->getMessage());
        exit(json_encode(['status' => 'error', 'message' => 'خطا در ذخیره اطلاعات پرداخت']));
    }
    
    echo json_encode([
        'status' => 'success',
        'order_id' => $orderId,
        'payment_url' => $zarinpalResult['payment_url'],
        'authority' => $zarinpalResult['authority'],
        'amount_rial' => $amountRial,
        'amount_toman_display' => round($amountRial / 10, 2),
        'message' => 'سفارش ایجاد شد - هدایت به زرین‌پال'
    ]);
}

function checkOrderStatus($authority) {
    if (empty($authority)) {
        exit(json_encode(['success' => false, 'error' => 'شناسه Authority مشخص نشده']));
    }
    
    try {
        $pdo = getBotDatabaseConnection();
        $stmt = $pdo->prepare("SELECT status, ref_id FROM payments WHERE authority = ?");
        $stmt->execute([$authority]);
        $payment = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if ($payment) {
            echo json_encode([
                'success' => true,
                'status' => $payment['status'],
                'ref_id' => $payment['ref_id'],
                'message' => $payment['status'] === 'completed' ? 'پرداخت موفق' : 'در انتظار پرداخت'
            ]);
        } else {
            echo json_encode(['success' => false, 'error' => 'سفارش یافت نشد']);
        }
    } catch (Exception $e) {
        echo json_encode(['success' => false, 'error' => 'خطا در بررسی وضعیت']);
    }
}

function cancelOrder($authority) {
    if (empty($authority)) {
        exit(json_encode(['success' => false, 'error' => 'شناسه Authority مشخص نشده']));
    }
    
    try {
        $pdo = getBotDatabaseConnection();
        $stmt = $pdo->prepare("UPDATE payments SET status = 'cancelled' WHERE authority = ? AND status = 'pending'");
        $stmt->execute([$authority]);
        echo json_encode(['success' => true, 'message' => 'سفارش لغو شد']);
    } catch (Exception $e) {
        echo json_encode(['success' => false, 'error' => 'خطا در لغو سفارش']);
    }
}
?>