<?php
/**
 * API مدیریت امتیازات مشتریان
 * این فایل شامل توابع مورد نیاز برای مدیریت سیستم امتیازدهی مشتریان است
 */

require_once 'debug_logger.php';
require_once 'woocommerce_config.php'; // این فایل config.php را هم بارگذاری می‌کند (INTERNAL_API_SECRET از آنجا می‌آید)

/**
 * محافظت دسترسی: این API باید یا از پنل ادمین (با سشن معتبر) صدا زده شود،
 * یا به‌صورت داخلی از webhook.php (با سکرت مشترک) — نه از هیچ منبع دیگری.
 */
if (session_status() !== PHP_SESSION_ACTIVE) {
    session_start();
}
$__incomingSecret = $_SERVER['HTTP_X_INTERNAL_API_SECRET'] ?? '';
if ($__incomingSecret === '' && function_exists('getallheaders')) {
    $__headers = getallheaders();
    foreach ($__headers as $__hName => $__hVal) {
        if (strcasecmp($__hName, 'X-Internal-Api-Secret') === 0) { $__incomingSecret = $__hVal; break; }
    }
}
$__isInternalCall = defined('INTERNAL_API_SECRET') && $__incomingSecret !== '' && hash_equals(INTERNAL_API_SECRET, $__incomingSecret);
$__isAdminSession = isset($_SESSION['is_admin']) && $_SESSION['is_admin'] === true;

if (!$__isInternalCall && !$__isAdminSession) {
    header('Content-Type: application/json; charset=utf-8');
    http_response_code(401);
    echo json_encode(['success' => false, 'error' => 'دسترسی غیرمجاز'], JSON_UNESCAPED_UNICODE);
    exit;
}

/**
 * دریافت امتیاز فعلی مشتری
 */
function getCustomerCredit($userId) {
    try {
        $pdo = getBotDatabaseConnection();
        
        $stmt = $pdo->prepare("
            SELECT current_credit, total_earned, total_deducted, last_updated 
            FROM customer_credits 
            WHERE user_id = ?
        ");
        $stmt->execute([$userId]);
        $result = $stmt->fetch();
        
        if (!$result) {
            // اگر رکورد امتیاز وجود ندارد، آن را ایجاد کن
            return createInitialCredit($userId);
        }
        
        return [
            'success' => true,
            'credit' => (int)$result['current_credit'],
            'total_earned' => (int)$result['total_earned'],
            'total_deducted' => (int)$result['total_deducted'],
            'last_updated' => $result['last_updated']
        ];
        
    } catch (Exception $e) {
        debug_log("❌ خطا در دریافت امتیاز مشتری", 'ERROR', [
            'user_id' => $userId,
            'error' => $e->getMessage()
        ]);
        return [
            'success' => false,
            'error' => 'خطا در دریافت امتیاز مشتری'
        ];
    }
}

/**
 * ایجاد امتیاز اولیه برای مشتری جدید
 */
function createInitialCredit($userId) {
    try {
        $pdo = getBotDatabaseConnection();
        
        // بررسی وجود کاربر
        $stmt = $pdo->prepare("SELECT id FROM users WHERE id = ? AND is_active = 1");
        $stmt->execute([$userId]);
        if (!$stmt->fetch()) {
            return [
                'success' => false,
                'error' => 'کاربر یافت نشد'
            ];
        }
        
        $pdo->beginTransaction();
        
        // ایجاد رکورد امتیاز اولیه
        $stmt = $pdo->prepare("
            INSERT INTO customer_credits (user_id, current_credit, total_earned, total_deducted) 
            VALUES (?, 10, 10, 0)
            ON DUPLICATE KEY UPDATE 
                current_credit = VALUES(current_credit),
                total_earned = VALUES(total_earned)
        ");
        $stmt->execute([$userId]);
        
        // ثبت تاریخچه امتیاز اولیه
        $stmt = $pdo->prepare("
            INSERT INTO credit_history (user_id, change_type, credit_change, credit_before, credit_after, reason) 
            VALUES (?, 'initial', 10, 0, 10, 'امتیاز اولیه هنگام ثبت‌نام')
        ");
        $stmt->execute([$userId]);
        
        $pdo->commit();
        
        debug_log("✅ امتیاز اولیه برای مشتری ایجاد شد", 'INFO', [
            'user_id' => $userId,
            'initial_credit' => 10
        ]);
        
        return [
            'success' => true,
            'credit' => 10,
            'total_earned' => 10,
            'total_deducted' => 0,
            'message' => 'امتیاز اولیه ایجاد شد'
        ];
        
    } catch (Exception $e) {
        if (isset($pdo)) {
            $pdo->rollBack();
        }
        debug_log("❌ خطا در ایجاد امتیاز اولیه", 'ERROR', [
            'user_id' => $userId,
            'error' => $e->getMessage()
        ]);
        return [
            'success' => false,
            'error' => 'خطا در ایجاد امتیاز اولیه'
        ];
    }
}

/**
 * اطمینان از وجود رکورد امتیاز مشتری قبل از اعمال تغییر
 */
function ensureCustomerCreditRecord($userId) {
    try {
        $pdo = getBotDatabaseConnection();

        $stmt = $pdo->prepare("SELECT id FROM customer_credits WHERE user_id = ? LIMIT 1");
        $stmt->execute([$userId]);
        if ($stmt->fetch()) {
            return true;
        }

        $stmt = $pdo->prepare("
            INSERT INTO customer_credits (user_id, current_credit, total_earned, total_deducted, total_spent, last_updated, updated_at)
            VALUES (?, 0, 0, 0, 0, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                user_id = VALUES(user_id)
        ");
        $stmt->execute([$userId]);

        return true;
    } catch (Exception $e) {
        debug_log("❌ خطا در ایجاد رکورد امتیاز مشتری", 'ERROR', [
            'user_id' => $userId,
            'error' => $e->getMessage()
        ]);
        return false;
    }
}

/**
 * به‌روزرسانی امتیاز مشتری
 */
function updateCustomerCredit($userId, $creditChange, $changeType, $reason, $bookingId = null, $adminId = null, $businessId = null) {
    $pdo = getBotDatabaseConnection();
    $alreadyInTransaction = $pdo->inTransaction();

    if (!ensureCustomerCreditRecord($userId)) {
        return ['success' => false, 'error' => 'خطا در آماده‌سازی رکورد امتیاز مشتری'];
    }

    // ابتدا تلاش با stored procedure
    try {
        $stmt = $pdo->prepare("\n            CALL UpdateCustomerCredit(?, ?, ?, ?, ?, ?, ?)\n        ");
        $stmt->execute([
            $userId,
            $creditChange,
            $changeType,
            $reason,
            $bookingId,
            $adminId,
            $businessId
        ]);
        if (method_exists($stmt, 'closeCursor')) {
            $stmt->closeCursor();
        }

        $creditInfo = getCustomerCredit($userId);

        debug_log("✅ امتیاز مشتری به‌روزرسانی شد (Stored Procedure)", 'INFO', [
            'user_id' => $userId,
            'credit_change' => $creditChange,
            'change_type' => $changeType,
            'new_credit' => $creditInfo['credit'] ?? 0,
            'reason' => $reason,
            'booking_id' => $bookingId,
            'admin_id' => $adminId,
            'business_id' => $businessId
        ]);

        return [
            'success' => true,
            'new_credit' => $creditInfo['credit'] ?? 0,
            'change_applied' => $creditChange,
            'message' => 'امتیاز با موفقیت به‌روزرسانی شد'
        ];
    } catch (Exception $spEx) {
        debug_log("⚠️ خطا در اجرای Stored Procedure - اجرای حالت fallback", 'WARN', [
            'user_id' => $userId,
            'error' => $spEx->getMessage(),
            'credit_change' => $creditChange,
            'change_type' => $changeType,
            'reason' => $reason,
            'booking_id' => $bookingId,
            'admin_id' => $adminId,
            'business_id' => $businessId
        ]);

        try {
            if (!$alreadyInTransaction && !$pdo->inTransaction()) {
                $pdo->beginTransaction();
            }

            $stmt = $pdo->prepare("SELECT current_credit, total_earned, total_deducted FROM customer_credits WHERE user_id = ? FOR UPDATE");
            $stmt->execute([$userId]);
            $creditRow = $stmt->fetch();

            if (!$creditRow) {
                $stmt = $pdo->prepare("INSERT INTO customer_credits (user_id, current_credit, total_earned, total_deducted) VALUES (?, 0, 0, 0)");
                $stmt->execute([$userId]);
                $stmt = $pdo->prepare("SELECT current_credit, total_earned, total_deducted FROM customer_credits WHERE user_id = ? FOR UPDATE");
                $stmt->execute([$userId]);
                $creditRow = $stmt->fetch();
            }

            $before = (int)$creditRow['current_credit'];
            $after = $before + (int)$creditChange;
            $earnedDelta = $creditChange > 0 ? (int)$creditChange : 0;
            $deductedDelta = $creditChange < 0 ? abs((int)$creditChange) : 0;

            $stmt = $pdo->prepare("\n                UPDATE customer_credits\n                SET current_credit = ?,\n                    total_earned = total_earned + ?,\n                    total_deducted = total_deducted + ?,\n                    last_updated = NOW()\n                WHERE user_id = ?\n            ");
            $stmt->execute([$after, $earnedDelta, $deductedDelta, $userId]);

            $stmt = $pdo->prepare("\n                INSERT INTO credit_history\n                (user_id, change_type, credit_change, credit_before, credit_after, reason, booking_id, admin_id, business_id)\n                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)\n            ");
            $stmt->execute([
                $userId,
                $changeType,
                $creditChange,
                $before,
                $after,
                $reason,
                $bookingId,
                $adminId,
                $businessId
            ]);

            if (!$alreadyInTransaction && $pdo->inTransaction()) {
                $pdo->commit();
            }

            debug_log("✅ امتیاز مشتری به‌روزرسانی شد (Fallback SQL)", 'INFO', [
                'user_id' => $userId,
                'credit_change' => $creditChange,
                'change_type' => $changeType,
                'new_credit' => $after,
                'reason' => $reason,
                'booking_id' => $bookingId,
                'admin_id' => $adminId,
                'business_id' => $businessId
            ]);

            return [
                'success' => true,
                'new_credit' => $after,
                'change_applied' => $creditChange,
                'message' => 'امتیاز با موفقیت به‌روزرسانی شد (fallback)'
            ];
        } catch (Exception $fallbackEx) {
            if ($pdo->inTransaction() && !$alreadyInTransaction) {
                $pdo->rollBack();
            }
            debug_log("❌ خطا در به‌روزرسانی امتیاز (Fallback)", 'ERROR', [
                'user_id' => $userId,
                'credit_change' => $creditChange,
                'change_type' => $changeType,
                'error' => $fallbackEx->getMessage()
            ]);
            return [
                'success' => false,
                'error' => 'خطا در به‌روزرسانی امتیاز'
            ];
        }
    }
}

/**
 * پردازش امتیاز برای لغو رزرو
 */
function processCancellationCredit($userId, $bookingId, $businessId = null) {
    return updateCustomerCredit(
        $userId,
        -1, // کسر 1 امتیاز
        'cancellation',
        'کسر امتیاز به دلیل لغو رزرو',
        $bookingId,
        null,
        $businessId
    );
}

/**
 * پردازش امتیاز برای حضور به موقع
 */
function processAttendanceCredit($userId, $bookingId, $businessId = null, $adminId = null) {
    return updateCustomerCredit(
        $userId,
        1, // اضافه کردن 1 امتیاز
        'attendance',
        'اضافه کردن امتیاز به دلیل حضور به موقع',
        $bookingId,
        $adminId,
        $businessId
    );
}

/**
 * پردازش امتیاز برای عدم حضور
 */
function processNoShowCredit($userId, $bookingId, $businessId = null, $adminId = null) {
    return updateCustomerCredit(
        $userId,
        -1, // کسر 1 امتیاز
        'no_show',
        'کسر امتیاز به دلیل عدم حضور',
        $bookingId,
        $adminId,
        $businessId
    );
}

/**
 * دریافت تاریخچه امتیازات مشتری
 */
function getCustomerCreditHistory($userId, $limit = 20) {
    try {
        $pdo = getBotDatabaseConnection();
        
        $stmt = $pdo->prepare("
            SELECT 
                ch.*,
                u_admin.first_name as admin_name,
                b.name as business_name
            FROM credit_history ch
            LEFT JOIN users u_admin ON ch.admin_id = u_admin.id
            LEFT JOIN businesses b ON ch.business_id = b.id
            WHERE ch.user_id = ?
            ORDER BY ch.created_at DESC
            LIMIT ?
        ");
        $stmt->execute([$userId, $limit]);
        $history = $stmt->fetchAll();
        
        return [
            'success' => true,
            'history' => $history
        ];
        
    } catch (Exception $e) {
        debug_log("❌ خطا در دریافت تاریخچه امتیازات", 'ERROR', [
            'user_id' => $userId,
            'error' => $e->getMessage()
        ]);
        return [
            'success' => false,
            'error' => 'خطا در دریافت تاریخچه امتیازات'
        ];
    }
}

/**
 * دریافت آمار امتیازات مشتری
 */
function getCustomerCreditStats($userId) {
    try {
        $pdo = getBotDatabaseConnection();
        
        $stmt = $pdo->prepare("
            SELECT 
                current_credit,
                total_earned,
                total_deducted,
                (SELECT COUNT(*) FROM credit_history WHERE user_id = ? AND change_type = 'attendance') as total_attendances,
                (SELECT COUNT(*) FROM credit_history WHERE user_id = ? AND change_type = 'no_show') as total_no_shows,
                (SELECT COUNT(*) FROM credit_history WHERE user_id = ? AND change_type = 'cancellation') as total_cancellations
            FROM customer_credits 
            WHERE user_id = ?
        ");
        $stmt->execute([$userId, $userId, $userId, $userId]);
        $stats = $stmt->fetch();
        
        if (!$stats) {
            return createInitialCredit($userId);
        }
        
        return [
            'success' => true,
            'stats' => [
                'current_credit' => (int)$stats['current_credit'],
                'total_earned' => (int)$stats['total_earned'],
                'total_deducted' => (int)$stats['total_deducted'],
                'total_attendances' => (int)$stats['total_attendances'],
                'total_no_shows' => (int)$stats['total_no_shows'],
                'total_cancellations' => (int)$stats['total_cancellations'],
                'attendance_rate' => $stats['total_attendances'] > 0 ? 
                    round(($stats['total_attendances'] / ($stats['total_attendances'] + $stats['total_no_shows'])) * 100, 2) : 0
            ]
        ];
        
    } catch (Exception $e) {
        debug_log("❌ خطا در دریافت آمار امتیازات", 'ERROR', [
            'user_id' => $userId,
            'error' => $e->getMessage()
        ]);
        return [
            'success' => false,
            'error' => 'خطا در دریافت آمار امتیازات'
        ];
    }
}

/**
 * ثبت وضعیت حضور رزرو
 */
function recordBookingAttendance($bookingId, $userId, $businessId, $attendanceStatus, $confirmedBy = null, $notes = '') {
    try {
        $pdo = getBotDatabaseConnection();
        $hadTransaction = $pdo->inTransaction();
        if (!$hadTransaction) {
            $pdo->beginTransaction();
        }
        
        try {
            // ثبت یا به‌روزرسانی وضعیت حضور
            $stmt = $pdo->prepare("
                INSERT INTO booking_attendance 
                (booking_id, user_id, business_id, attendance_status, confirmed_by, confirmation_date, notes) 
                VALUES (?, ?, ?, ?, ?, NOW(), ?)
                ON DUPLICATE KEY UPDATE 
                    attendance_status = VALUES(attendance_status),
                    confirmed_by = VALUES(confirmed_by),
                    confirmation_date = NOW(),
                    notes = VALUES(notes),
                    credit_processed = 0
            ");
            $stmt->execute([$bookingId, $userId, $businessId, $attendanceStatus, $confirmedBy, $notes]);
            
            // پردازش امتیاز بر اساس وضعیت حضور
            $creditResult = null;
            if ($attendanceStatus === 'attended') {
                $creditResult = processAttendanceCredit($userId, $bookingId, $businessId, $confirmedBy);
                $stmt = $pdo->prepare("
                    UPDATE bookings
                    SET status = 'completed', updated_at = NOW()
                    WHERE id = ? AND business_id = ? AND status != 'cancelled'
                ");
                $stmt->execute([$bookingId, $businessId]);
            } elseif ($attendanceStatus === 'no_show') {
                $creditResult = processNoShowCredit($userId, $bookingId, $businessId, $confirmedBy);
                $stmt = $pdo->prepare("
                    UPDATE bookings
                    SET status = 'cancelled', updated_at = NOW()
                    WHERE id = ? AND business_id = ? AND status != 'cancelled'
                ");
                $stmt->execute([$bookingId, $businessId]);
            }
            
            // علامت‌گذاری پردازش امتیاز
            if ($creditResult && $creditResult['success']) {
                $stmt = $pdo->prepare("
                    UPDATE booking_attendance 
                    SET credit_processed = 1 
                    WHERE booking_id = ?
                ");
                $stmt->execute([$bookingId]);
            }
            
            if (!$hadTransaction) {
                $pdo->commit();
            }
            
            debug_log("✅ وضعیت حضور رزرو ثبت شد", 'INFO', [
                'booking_id' => $bookingId,
                'user_id' => $userId,
                'attendance_status' => $attendanceStatus,
                'credit_processed' => $creditResult['success'] ?? false
            ]);
            
            return [
                'success' => true,
                'attendance_recorded' => true,
                'credit_result' => $creditResult,
                'message' => 'وضعیت حضور با موفقیت ثبت شد'
            ];
        } catch (Exception $inner) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }
            throw $inner;
        }
        
    } catch (Exception $e) {
        if (isset($pdo) && $pdo->inTransaction()) {
            $pdo->rollBack();
        }
        debug_log("❌ خطا در ثبت وضعیت حضور", 'ERROR', [
            'booking_id' => $bookingId,
            'user_id' => $userId,
            'error' => $e->getMessage()
        ]);
        return [
            'success' => false,
            'error' => 'خطا در ثبت وضعیت حضور'
        ];
    }
}

/**
 * دریافت اطلاعات امتیاز برای نمایش در رسید
 */
function getCreditInfoForReceipt($userId) {
    $creditInfo = getCustomerCredit($userId);
    
    if (!$creditInfo['success']) {
        return [
            'credit' => 0,
            'status' => 'نامشخص',
            'message' => 'امتیاز قابل نمایش نیست'
        ];
    }
    
    $credit = $creditInfo['credit'];
    $status = '';
    $message = '';
    
    if ($credit >= 8) {
        $status = 'عالی';
        $message = 'مشتری با اعتبار بالا';
    } elseif ($credit >= 5) {
        $status = 'خوب';
        $message = 'مشتری با اعتبار متوسط';
    } elseif ($credit >= 2) {
        $status = 'متوسط';
        $message = 'مشتری نیاز به بهبود اعتبار دارد';
    } else {
        $status = 'ضعیف';
        $message = 'مشتری با اعتبار پایین - نیاز به توجه ویژه';
    }
    
    return [
        'credit' => $credit,
        'status' => $status,
        'message' => $message,
        'total_earned' => $creditInfo['total_earned'],
        'total_deducted' => $creditInfo['total_deducted']
    ];
}

/**
 * API endpoint برای مدیریت امتیازات
 */
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    header('Content-Type: application/json; charset=utf-8');
    $headers = function_exists('getallheaders') ? getallheaders() : [];
    debug_log("📩 دریافت درخواست به customer_credit_api", 'INFO', [
        'method' => $_SERVER['REQUEST_METHOD'] ?? null,
        'uri' => $_SERVER['REQUEST_URI'] ?? null,
        'remote_addr' => $_SERVER['REMOTE_ADDR'] ?? null,
        'content_type' => $headers['Content-Type'] ?? ($_SERVER['CONTENT_TYPE'] ?? null),
    ]);

    $raw = file_get_contents('php://input');
    debug_log("🧾 محتوای خام ورودی", 'INFO', [
        'raw_length' => strlen($raw),
        'raw_preview' => substr($raw, 0, 300)
    ]);

    $input = json_decode($raw, true);
    if (!is_array($input) || empty($input)) {
        // Fallback to form-encoded/multipart POST
        debug_log("ℹ️ ورودی JSON نامعتبر/خالی - استفاده از POST", 'WARN', [
            'post_keys' => array_keys($_POST),
            'post_sample' => array_slice($_POST, 0, 5, true)
        ]);
        $input = $_POST;
    }

    $action = $input['action'] ?? '';
    debug_log("➡️ اکشن دریافتی", 'INFO', [
        'action' => $action,
        'payload_keys' => array_keys($input)
    ]);
    
    switch ($action) {
        case 'get_credit':
            $userId = $input['user_id'] ?? 0;
            echo json_encode(getCustomerCredit($userId), JSON_UNESCAPED_UNICODE);
            break;
            
        case 'update_credit':
            $userId = $input['user_id'] ?? 0;
            $creditChange = $input['credit_change'] ?? 0;
            $changeType = $input['change_type'] ?? '';
            $reason = $input['reason'] ?? '';
            $bookingId = $input['booking_id'] ?? null;
            $adminId = $input['admin_id'] ?? null;
            $businessId = $input['business_id'] ?? null;
            
            echo json_encode(updateCustomerCredit($userId, $creditChange, $changeType, $reason, $bookingId, $adminId, $businessId), JSON_UNESCAPED_UNICODE);
            break;
            
        case 'record_attendance':
            $bookingId = $input['booking_id'] ?? 0;
            $userId = $input['user_id'] ?? 0;
            $businessId = $input['business_id'] ?? 0;
            $attendanceStatus = $input['attendance_status'] ?? '';
            $confirmedBy = $input['confirmed_by'] ?? null;
            $notes = $input['notes'] ?? '';
            
            echo json_encode(recordBookingAttendance($bookingId, $userId, $businessId, $attendanceStatus, $confirmedBy, $notes), JSON_UNESCAPED_UNICODE);
            break;
            
        case 'get_history':
            $userId = $input['user_id'] ?? 0;
            $limit = $input['limit'] ?? 20;
            
            echo json_encode(getCustomerCreditHistory($userId, $limit), JSON_UNESCAPED_UNICODE);
            break;
            
        case 'get_stats':
            $userId = $input['user_id'] ?? 0;
            
            echo json_encode(getCustomerCreditStats($userId), JSON_UNESCAPED_UNICODE);
            break;
            
        default:
            echo json_encode([
                'success' => false,
                'error' => 'عملیات نامعتبر'
            ], JSON_UNESCAPED_UNICODE);
    }
    exit;
}
?>