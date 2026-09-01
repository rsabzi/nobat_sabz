<?php
/**
 * صفحه بازگشت از درگاه زرین‌پال
 * این فایل پس از پرداخت کاربر از زرین‌پال فراخوانی می‌شود
 */

require_once 'zarinpal_api.php';
require_once '../config.php';
require_once 'debug_logger.php';

/**
 * جستجوی بهبود یافته Authority در دیتابیس
 * این تابع انواع مختلف فرمت Authority را پشتیبانی می‌کند
 */
function findPaymentByAuthority($pdo, $authority) {
    error_log("🔍 شروع جستجوی Authority: $authority");
    
    // تست 1: جستجوی دقیق
    $stmt = $pdo->prepare("SELECT * FROM payments WHERE authority = ?");
    $stmt->execute([$authority]);
    $payment = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($payment) {
        error_log("✅ یافت شد با جستجوی دقیق - ID: " . $payment['id']);
        return $payment;
    }
    
    // تست 2: Authority در دیتابیس شامل Authority callback باشد
    $stmt = $pdo->prepare("SELECT * FROM payments WHERE authority LIKE ?");
    $stmt->execute([$authority . '%']);
    $payment = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($payment) {
        error_log("✅ یافت شد با LIKE (دیتابیس شامل callback) - ID: " . $payment['id']);
        return $payment;
    }
    
    // تست 3: Authority callback شامل Authority دیتابیس باشد
    $stmt = $pdo->prepare("SELECT * FROM payments WHERE ? LIKE CONCAT(authority, '%')");
    $stmt->execute([$authority]);
    $payment = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($payment) {
        error_log("✅ یافت شد با LIKE معکوس (callback شامل دیتابیس) - ID: " . $payment['id']);
        return $payment;
    }
    
    // تست 4: جستجوی با 36 کاراکتر اول
    if (strlen($authority) > 36) {
        $shortAuthority = substr($authority, 0, 36);
        $stmt = $pdo->prepare("SELECT * FROM payments WHERE authority LIKE ?");
        $stmt->execute([$shortAuthority . '%']);
        $payment = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if ($payment) {
            error_log("✅ یافت شد با 36 کاراکتر اول - ID: " . $payment['id']);
            return $payment;
        }
    }
    
    error_log("❌ Authority یافت نشد با هیچ روشی");
    return null;
}

// دریافت پارامترها از زرین‌پال
$status = $_GET['Status'] ?? '';
$authority = $_GET['Authority'] ?? '';

error_log("🔍 Callback دریافت شد - Status: $status, Authority: $authority");

// بررسی وضعیت پرداخت
if ($status !== 'OK') {
    error_log("❌ پرداخت ناموفق - Status: $status");
    $errorMessage = "پرداخت ناموفق بود";
    $showRetryButton = true;
} else {
    // جستجوی اطلاعات پرداخت در دیتابیس
    try {
        global $pdo;
        
        if (!$pdo) {
            throw new Exception("اتصال به دیتابیس برقرار نیست");
        }
        
        // جستجوی بهبود یافته Authority
        $paymentInfo = findPaymentByAuthority($pdo, $authority);
        
        error_log("🔍 نتیجه جستجوی Authority: " . ($paymentInfo ? "یافت شد (ID: " . $paymentInfo['id'] . ")" : "یافت نشد"));
        
        if (!$paymentInfo) {
            error_log("❌ اطلاعات پرداخت یافت نشد - Authority: $authority");
            debug_log("❌ اطلاعات پرداخت یافت نشد", 'ERROR', [
                'authority' => $authority,
                'short_authority' => $shortAuthority,
                'status' => $status
            ]);
            $errorMessage = "اطلاعات پرداخت شما در سیستم یافت نشد";
            $showRetryButton = false;
            $paymentNotFound = true;
        } else {
            $amountRial = $paymentInfo['amount'];
            error_log("✅ اطلاعات پرداخت یافت شد - Amount: $amountRial ریال");
            debug_log("✅ اطلاعات پرداخت یافت شد", 'INFO', [
                'authority' => $authority,
                'amount_rial' => $amountRial,
                'payment_info' => $paymentInfo
            ]);
            
            // تأیید پرداخت با زرین‌پال
            debug_log("🔄 شروع تأیید پرداخت با زرین‌پال", 'INFO', [
                'authority' => $authority,
                'amount' => $amountRial
            ]);
            
             $verifyResult = verifyZarinpalPayment($authority, $amountRial);
            
            debug_log("📋 نتیجه تأیید پرداخت زرین‌پال", 'INFO', [
                'verify_result' => $verifyResult,
                'authority' => $authority,
                'amount' => $amountRial
            ]);
            
            if ($verifyResult['success']) {
                error_log("✅ پرداخت تأیید شد - RefID: {$verifyResult['ref_id']}");
                debug_log("✅ پرداخت تأیید شد", 'INFO', [
                    'ref_id' => $verifyResult['ref_id'],
                    'authority' => $authority
                ]);
                
                // فعال‌سازی اشتراک
                $activationResult = activateSubscription($paymentInfo, $verifyResult['ref_id']);
                
                if (isset($activationResult['success']) && $activationResult['success']) {
                    $successMessage = "پرداخت شما با موفقیت انجام شد";
                    $refId = $verifyResult['ref_id'];
                    debug_log("✅ اشتراک فعال شد", 'INFO', [
                        'ref_id' => $refId,
                        'authority' => $authority
                    ]);
                } else {
                    $errorMessage = isset($activationResult['message']) ? $activationResult['message'] : 'خطای نامشخص در فعال‌سازی اشتراک';
                    error_log("❌ خطا در فعال‌سازی اشتراک: " . $errorMessage);
                    debug_log("❌ خطا در فعال‌سازی اشتراک", 'ERROR', [
                        'activation_result' => $activationResult,
                        'authority' => $authority
                    ]);
                    $errorMessage = "خطا در فعال‌سازی اشتراک";
                    $showRetryButton = false;
                }
            } else {
                error_log("❌ تأیید پرداخت ناموفق: " . $verifyResult['error']);
                debug_log("❌ تأیید پرداخت ناموفق", 'ERROR', [
                    'verify_result' => $verifyResult,
                    'authority' => $authority,
                    'amount' => $amountRial,
                    'error_message' => $verifyResult['error'] ?? 'نامشخص'
                ]);
                $errorMessage = "تأیید پرداخت ناموفق بود: " . ($verifyResult['error'] ?? 'خطای نامشخص');
                $showRetryButton = true;
            }
        }
    } catch (Exception $e) {
        error_log("❌ خطا در پردازش callback: " . $e->getMessage());
        $errorMessage = "خطا در پردازش اطلاعات پرداخت";
        $showRetryButton = false;
    }
}



/**
 * فعال‌سازی اشتراک پس از پرداخت موفق
 */
function activateSubscription($paymentInfo, $refId) {
    try {
        global $pdo;
        
        if (!$pdo) {
            throw new Exception("اتصال به دیتابیس برقرار نیست");
        }
        
        // دریافت اطلاعات پلن
        $planStmt = $pdo->prepare("SELECT * FROM SubscriptionPlans WHERE id = ? AND is_active = 1");
        $planStmt->execute([$paymentInfo['plan_id']]);
        $plan = $planStmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$plan) {
            error_log("❌ پلن یافت نشد: " . $paymentInfo['plan_id']);
            return ['success' => false, 'message' => 'پلن اشتراک یافت نشد'];
        }
        
        // محاسبه تاریخ شروع و پایان اشتراک
        $startDate = date('Y-m-d');
        $endDate = date('Y-m-d', strtotime("+{$plan['duration_days']} days"));
        
        // تعداد توکن‌های تخصیص یافته (max_reservations_per_month)
        $tokensAllocated = $plan['max_reservations_per_month'];
        
        $pdo->beginTransaction();
        
        // به‌روزرسانی وضعیت پرداخت
        $updatePaymentStmt = $pdo->prepare("
            UPDATE payments 
            SET status = 'completed', ref_id = ?, updated_at = CURRENT_TIMESTAMP 
            WHERE id = ?
        ");
        $updatePaymentStmt->execute([$refId, $paymentInfo['id']]);
        
        // بررسی آیا قبلاً اشتراک فعال شده است یا نه
        $checkSubscriptionStmt = $pdo->prepare("
            SELECT id FROM business_subscriptions WHERE payment_id = ?
        ");
        $checkSubscriptionStmt->execute([$paymentInfo['id']]);
        $existingSubscription = $checkSubscriptionStmt->fetch();
        
        if (!$existingSubscription) {
            // ایجاد رکورد اشتراک کسب و کار
            $insertSubscriptionStmt = $pdo->prepare("
                INSERT INTO business_subscriptions 
                (business_id, subscription_plan_id, payment_id, start_date, end_date, status, created_at) 
                VALUES (?, ?, ?, ?, ?, 'active', CURRENT_TIMESTAMP)
            ");
            $insertSubscriptionStmt->execute([
                $paymentInfo['business_id'], 
                $paymentInfo['plan_id'], 
                $paymentInfo['id'], 
                $startDate, 
                $endDate
            ]);
            
            // به‌روزرسانی جدول businesses
            $updateBusinessStmt = $pdo->prepare("
                UPDATE businesses 
                SET subscription_plan_id = ?, subscription_expiry_date = ?, free_reservation_tokens = ?, updated_at = CURRENT_TIMESTAMP 
                WHERE id = ?
            ");
            $updateBusinessStmt->execute([
                $paymentInfo['plan_id'], 
                $endDate, 
                $tokensAllocated,
                $paymentInfo['business_id']
            ]);
            
            error_log("✅ اشتراک جدید ایجاد شد - Business: {$paymentInfo['business_id']}, Plan: {$paymentInfo['plan_id']}, Tokens: $tokensAllocated");
        } else {
            error_log("ℹ️ اشتراک قبلاً ایجاد شده بود - Payment ID: {$paymentInfo['id']}");
        }
        
        $pdo->commit();
        
        error_log("✅ اشتراک با موفقیت فعال شد - Business: {$paymentInfo['business_id']}, Plan: {$paymentInfo['plan_id']}");
        return ['success' => true, 'message' => 'اشتراک با موفقیت فعال شد'];
        
    } catch (Exception $e) {
        if (isset($pdo) && $pdo->inTransaction()) {
            $pdo->rollBack();
        }
        error_log("❌ خطا در فعال‌سازی اشتراک: " . $e->getMessage());
        return ['success' => false, 'message' => $e->getMessage()];
    }
}

?>
<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>نتیجه پرداخت</title>
    <style>
        body {
            font-family: 'Tahoma', sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            margin: 0;
            padding: 20px;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .container {
            background: white;
            border-radius: 15px;
            padding: 40px;
            box-shadow: 0 20px 40px rgba(0,0,0,0.1);
            text-align: center;
            max-width: 500px;
            width: 100%;
        }
        .success {
            color: #27ae60;
        }
        .error {
            color: #e74c3c;
        }
        .warning {
            color: #f39c12;
        }
        .icon {
            font-size: 4rem;
            margin-bottom: 20px;
        }
        .message {
            font-size: 1.2rem;
            margin-bottom: 30px;
            line-height: 1.6;
        }
        .details {
            background: #f8f9fa;
            padding: 20px;
            border-radius: 10px;
            margin: 20px 0;
            text-align: right;
        }
        .btn {
            display: inline-block;
            padding: 12px 30px;
            background: #667eea;
            color: white;
            text-decoration: none;
            border-radius: 25px;
            transition: all 0.3s ease;
            margin: 10px;
        }
        .btn:hover {
            background: #5a6fd8;
            transform: translateY(-2px);
        }
        .btn-telegram {
            background: #0088cc;
        }
        .btn-telegram:hover {
            background: #006699;
        }
        .debug-info {
            background: #fff3cd;
            border: 1px solid #ffc107;
            padding: 15px;
            border-radius: 8px;
            margin: 20px 0;
            text-align: right;
            font-size: 0.9rem;
            color: #856404;
        }
    </style>
</head>
<body>
    <div class="container">
        <?php if ($status === 'OK' && !empty($authority)): ?>
            <?php if ($paymentNotFound && !$paymentInfo): ?>
                <!-- اطلاعات پرداخت یافت نشد -->
                <div class="error">
                    <div class="icon">⚠️</div>
                    <h2>خطا در یافتن اطلاعات پرداخت</h2>
                    <div class="message">
                        اطلاعات پرداخت شما در سیستم یافت نشد.
                        <br>
                        لطفاً با پشتیبانی تماس بگیرید.
                    </div>
                    <div class="details">
                        <p><strong>شماره Authority:</strong> <?php echo htmlspecialchars($authority); ?></p>
                        <p><strong>وضعیت:</strong> <?php echo htmlspecialchars($status); ?></p>
                    </div>
                </div>
            <?php elseif ($amountRial < 1000): ?>
                <!-- مبلغ نامعتبر -->
                <div class="error">
                    <div class="icon">❌</div>
                    <h2>خطا در مبلغ پرداخت</h2>
                    <div class="message">
                        مبلغ پرداخت نامعتبر است (کمتر از حداقل مجاز).
                        <br>
                        لطفاً با پشتیبانی تماس بگیرید.
                    </div>
                    <div class="debug-info">
                        <p><strong>⚠️ اطلاعات فنی:</strong></p>
                        <p>Authority: <?php echo htmlspecialchars($authority); ?></p>
                        <p>مبلغ ثبت شده: <?php echo number_format($amountRial); ?> ریال (<?php echo number_format($amountRial/10, 0); ?> تومان)</p>
                    </div>
                </div>
            <?php else: ?>
                <?php
                // تأیید پرداخت با مبلغ استاندارد (ریال)
                error_log("🔍 شروع Verify - Authority: $authority, AmountRial: $amountRial");
                $verification = verifyZarinpalPayment($authority, $amountRial);
                error_log("🔍 نتیجه Verify - Success: " . ($verification['success'] ? 'true' : 'false') . ", Error: " . ($verification['error'] ?? 'none'));
                
                if ($verification['success']) {
                    error_log("✅ پرداخت تأیید شد - RefID: " . $verification['ref_id'] . ", مبلغ: $amountRial ریال");
                } else {
                    error_log("❌ خطا در تأیید پرداخت - Authority: $authority, مبلغ: $amountRial ریال, خطا: " . ($verification['error'] ?? 'نامشخص'));
                }
                ?>
                
                <?php if ($verification['success']): ?>
                    <?php
                    // فعال‌سازی اشتراک پس از تأیید پرداخت
                    $subscriptionActivated = false;
                    if ($paymentInfo) {
                        $subscriptionActivated = activateSubscription($paymentInfo, $verification['ref_id']);
                    }
                    ?>
                    <div class="success">
                        <div class="icon">✅</div>
                        <h2>پرداخت موفق!</h2>
                        <div class="message">
                            پرداخت شما با موفقیت انجام شد<?php echo $subscriptionActivated ? ' و اشتراک شما فعال گردید' : ''; ?>.
                            <?php if (!$subscriptionActivated && $paymentInfo): ?>
                                <br><small style="color: #f39c12;">⚠️ اشتراک در حال پردازش است و به زودی فعال خواهد شد.</small>
                            <?php endif; ?>
                        </div>
                        <div class="details">
                            <p><strong>کد پیگیری:</strong> <?php echo htmlspecialchars($verification['ref_id']); ?></p>
                            <p><strong>شماره Authority:</strong> <?php echo htmlspecialchars($authority); ?></p>
                            <p><strong>مبلغ:</strong> <?php echo number_format($amountRial/10, 0); ?> تومان (<?php echo number_format($amountRial); ?> ریال)</p>
                        </div>
                    </div>
                    
                    <a href="https://t.me/sabztime_bot" class="btn btn-telegram">
                        🤖 بازگشت به ربات تلگرام
                    </a>
                    
                <?php else: ?>
                    <div class="error">
                        <div class="icon">❌</div>
                        <h2>خطا در تأیید پرداخت</h2>
                        <div class="message">
                            متأسفانه در تأیید پرداخت شما خطایی رخ داده است.
                            <br>
                            <strong><?php echo htmlspecialchars($verification['error']); ?></strong>
                        </div>
                        <div class="debug-info">
                            <p><strong>⚠️ اطلاعات فنی برای پشتیبانی:</strong></p>
                            <p>Authority: <?php echo htmlspecialchars($authority); ?></p>
                            <p>مبلغ ارسالی: <?php echo number_format($amountRial); ?> ریال (<?php echo number_format($amountRial/10, 0); ?> تومان)</p>
                            <p>خطا: <?php echo htmlspecialchars($verification['error']); ?></p>
                        </div>
                    </div>
                <?php endif; ?>
            <?php endif; ?>
            
        <?php else: ?>
            <div class="error">
                <div class="icon">❌</div>
                <h2>پرداخت ناموفق</h2>
                <div class="message">
                    پرداخت شما لغو شده یا با خطا مواجه شده است.
                    <br>
                    لطفاً مجدداً تلاش کنید.
                </div>
                <?php if (!empty($authority)): ?>
                    <div class="details">
                        <p><strong>شماره Authority:</strong> <?php echo htmlspecialchars($authority); ?></p>
                        <p><strong>وضعیت:</strong> <?php echo htmlspecialchars($status ?: 'نامشخص'); ?></p>
                    </div>
                <?php endif; ?>
            </div>
        <?php endif; ?>
        
        <a href="subscription.html" class="btn">
            🔄 تلاش مجدد
        </a>
        
        <a href="https://t.me/sabztime_bot" class="btn btn-telegram">
            🤖 بازگشت به ربات
        </a>
    </div>
</body>
</html>