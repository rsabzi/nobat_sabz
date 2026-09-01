<?php
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, X-Telegram-Init-Data');

// Handle preflight requests for CORS
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

// --- Database Connection ---
$pdo = null;
try {
    // ابتدا سعی کن از config_local.php استفاده کنی
    if (file_exists(__DIR__ . '/config.php')) {
        require_once __DIR__ . '/config.php';
        
        // بررسی اتصال دیتابیس از config_local.php
        if (!$pdo) {
            throw new Exception('متغیر $pdo از config_local.php در دسترس نیست');
        }
        
        // تست اتصال با یک کوئری ساده
        $pdo->query("SELECT 1");
        
    } elseif (file_exists(__DIR__ . '/../config.php')) {
        require_once __DIR__ . '/../config.php';
        
        // بررسی اتصال دیتابیس از config.php
        if (!$pdo) {
            throw new Exception('متغیر $pdo از config.php در دسترس نیست');
        }
        
        // تست اتصال با یک کوئری ساده
        $pdo->query("SELECT 1");
        
    } else {
        throw new Exception('هیچ فایل تنظیمات موجود نیست');
    }
    
} catch (Exception $e) {
    // اگر فایل‌های config کار نکردند، سعی کن مستقیماً متصل شو
    try {
        $dsn = "mysql:host=localhost;dbname=webnovoi_reza;charset=utf8mb4";
        $options = [
            PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES   => false,
        ];
        $pdo = new PDO($dsn, 'webnovoi_reza', 'jHLSa[-4]srP', $options);
        $pdo->exec("SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci");
        
    } catch (PDOException $dbError) {
        http_response_code(500);
        echo json_encode([
            'status' => 'error', 
            'message' => 'خطا در اتصال به دیتابیس: ' . $dbError->getMessage(),
            'debug' => [
                'config_error' => $e->getMessage(),
                'db_error' => $dbError->getMessage()
            ]
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

// --- Input Processing ---
$input = json_decode(file_get_contents('php://input'), true);
$action = $input['action'] ?? $_POST['action'] ?? '';
$marketer_code = $input['marketer_code'] ?? $_POST['marketer_code'] ?? null;

if ($action !== 'get_marketer_dashboard' && $action !== 'getDashboardData') {
    http_response_code(400);
    echo json_encode(['status' => 'error', 'message' => 'عمل نامعتبر'], JSON_UNESCAPED_UNICODE);
    exit;
}

if (empty($marketer_code)) {
    http_response_code(400);
    echo json_encode(['status' => 'error', 'message' => 'کد بازاریاب ارسال نشده است. لطفاً از طریق لینک صحیح وارد شوید.'], JSON_UNESCAPED_UNICODE);
    exit;
}


try {
    // --- 1. Fetch Marketer Info ---
    $marketer = null;
    
    // ابتدا سعی کن با کد بازاریاب معمولی پیدا کنی
    $marketerStmt = $pdo->prepare("SELECT m.*, u.first_name, u.last_name FROM marketers m JOIN users u ON m.user_id = u.id WHERE m.marketer_code = ? AND m.is_active = 1");
    $marketerStmt->execute([$marketer_code]);
    $marketer = $marketerStmt->fetch();
    
    // اگر پیدا نشد و کد با TG_ شروع می‌شود، سعی کن با user_id تلگرام پیدا کنی
    if (!$marketer && strpos($marketer_code, 'TG_') === 0) {
        $telegram_user_id = substr($marketer_code, 3); // حذف TG_ از ابتدا
        
        // جستجو بر اساس telegram_user_id
        $telegramStmt = $pdo->prepare("SELECT m.*, u.first_name, u.last_name FROM marketers m JOIN users u ON m.user_id = u.id WHERE u.telegram_user_id = ? AND m.is_active = 1");
        $telegramStmt->execute([$telegram_user_id]);
        $marketer = $telegramStmt->fetch();
        
        // اگر هنوز پیدا نشد، یک بازاریاب موقت ایجاد کن
        if (!$marketer) {
            // ایجاد یک بازاریاب موقت برای تست
            $marketer = [
                'id' => 999,
                'marketer_code' => $marketer_code,
                'commission_rate' => 10.0,
                'first_name' => 'کاربر تلگرام',
                'last_name' => $telegram_user_id,
                'is_active' => 1,
                'created_at' => date('Y-m-d H:i:s')
            ];
            
            error_log("DEBUG: Created temporary marketer for Telegram user ID: " . $telegram_user_id);
        }
    }
    
    if (!$marketer) {
        http_response_code(404);
        echo json_encode(['status' => 'error', 'message' => 'بازاریاب با کد ' . htmlspecialchars($marketer_code) . ' یافت نشد. لطفاً ابتدا در سیستم ثبت‌نام کنید.'], JSON_UNESCAPED_UNICODE);
        exit;
    }

    // --- 2. Fetch Businesses and Calculate Stats (Simplified Logic) ---
    // Debug: Log marketer info
    error_log("DEBUG: Marketer ID = " . $marketer['id'] . ", Code = " . $marketer['marketer_code']);
    
    // Debug: Check all businesses in database
    $allBusinessesStmt = $pdo->prepare("SELECT id, name, marketer_id FROM businesses ORDER BY id");
    $allBusinessesStmt->execute();
    $allBusinesses = $allBusinessesStmt->fetchAll();
    error_log("DEBUG: Total businesses in database: " . count($allBusinesses));
    foreach ($allBusinesses as $biz) {
        error_log("DEBUG: All Business - ID: " . $biz['id'] . ", Name: " . $biz['name'] . ", Marketer ID: " . $biz['marketer_id']);
    }
    
    $businessesStmt = $pdo->prepare("
        SELECT 
            b.id, b.name, b.business_type, b.city, b.province, b.is_active, b.created_at, b.marketer_id,
            (SELECT COUNT(*) FROM bookings WHERE business_id = b.id AND status != 'cancelled') as total_bookings
        FROM businesses b
        WHERE b.marketer_id = ?
        ORDER BY b.created_at DESC
    ");
    $businessesStmt->execute([$marketer['id']]);
    $businesses = $businessesStmt->fetchAll();
    
    // Debug: Log business results
    error_log("DEBUG: Found " . count($businesses) . " businesses for marketer " . $marketer['id']);
    foreach ($businesses as $business) {
        error_log("DEBUG: Business - ID: " . $business['id'] . ", Name: " . $business['name'] . ", Marketer ID: " . $business['marketer_id']);
    }

    // --- 3. Calculate Commissions and Totals in PHP ---
    // ** (اصلاح کلیدی) ** کمیسیون از دیتابیس خوانده می‌شود
    $commission_rate = isset($marketer['commission_rate']) ? (float)$marketer['commission_rate'] : 0.0;
    // فرض بر این است که نرخ کمیسیون یک عدد است که باید در ۱۰۰۰ ضرب شود (مانند 10.00)
    $commission_per_booking = $commission_rate * 1000;
    
    foreach ($businesses as &$business) {
        $business['commission_earned'] = $business['total_bookings'] * $commission_per_booking;
    }
    unset($business); 

    $totalBusinesses = count($businesses);
    $totalBookings = array_sum(array_column($businesses, 'total_bookings'));
    $totalCommission = array_sum(array_column($businesses, 'commission_earned'));

    // --- 4. Calculate Monthly Stats ---
    $oneMonthAgo = date('Y-m-d H:i:s', strtotime('-1 month'));
    $monthlyBusinesses = 0;
    foreach ($businesses as $business) {
        if ($business['created_at'] >= $oneMonthAgo) {
            $monthlyBusinesses++;
        }
    }
    
    $monthlyQuery = $pdo->prepare("
        SELECT COUNT(bk.id) as count
        FROM bookings bk
        JOIN businesses b ON bk.business_id = b.id
        WHERE b.marketer_id = ? AND bk.created_at >= ? AND bk.status != 'cancelled'
    ");
    $monthlyQuery->execute([$marketer['id'], $oneMonthAgo]);
    $monthlyBookings = $monthlyQuery->fetchColumn();
    $monthlyCommission = $monthlyBookings * $commission_per_booking;

    // --- 5. Prepare Chart Data ---
    // Monthly Revenue for the last 6 months
    $revenueStmt = $pdo->prepare("
        SELECT 
            DATE_FORMAT(bk.created_at, '%Y-%m') as month,
            COUNT(bk.id) as booking_count
        FROM bookings bk
        JOIN businesses b ON bk.business_id = b.id
        WHERE b.marketer_id = ? AND bk.created_at >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH) AND bk.status != 'cancelled'
        GROUP BY DATE_FORMAT(bk.created_at, '%Y-%m')
        ORDER BY month ASC
    ");
    $revenueStmt->execute([$marketer['id']]);
    $revenueDataRaw = $revenueStmt->fetchAll();

    $monthly_revenue = [];
    for ($i = 5; $i >= 0; $i--) {
        $monthKey = date('Y-m', strtotime("-$i months"));
        $foundData = array_values(array_filter($revenueDataRaw, fn($d) => $d['month'] === $monthKey));
        $monthly_revenue[] = $foundData ? (float)($foundData[0]['booking_count'] * $commission_per_booking) : 0;
    }

    // Business Types Distribution
    $typeStmt = $pdo->prepare("SELECT business_type, COUNT(*) as count FROM businesses WHERE marketer_id = ? GROUP BY business_type");
    $typeStmt->execute([$marketer['id']]);
    $typesDataRaw = $typeStmt->fetchAll();
    $business_types = array_column($typesDataRaw, 'count', 'business_type');

    // --- 6. Fetch Recent Bookings ---
    $recentBookingsStmt = $pdo->prepare("
        SELECT bk.*, b.name as business_name, b.business_type
        FROM bookings bk
        JOIN businesses b ON bk.business_id = b.id
        WHERE b.marketer_id = ?
        ORDER BY bk.created_at DESC
        LIMIT 5
    ");
    $recentBookingsStmt->execute([$marketer['id']]);
    $recentBookings = $recentBookingsStmt->fetchAll();

    // --- 7. Construct Final JSON Response ---
    $dashboardData = [
        'status' => 'success',
        'message' => 'اطلاعات با موفقیت دریافت شد',
        'marketer_info' => [
            'name' => trim($marketer['first_name'] . ' ' . $marketer['last_name']),
            'code' => $marketer['marketer_code']
        ],
        'total_businesses' => $totalBusinesses,
        'total_bookings' => (int)$totalBookings,
        'total_commission' => (float)$totalCommission,
        'commission_rate' => (float)$marketer['commission_rate'],
        'monthly_businesses' => $monthlyBusinesses,
        'monthly_bookings' => (int)$monthlyBookings,
        'monthly_commission' => (float)$monthlyCommission,
        'monthly_revenue' => $monthly_revenue,
        'business_types' => $business_types,
        'businesses' => $businesses,
        'recent_bookings' => $recentBookings
    ];
    
    echo json_encode($dashboardData, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'status' => 'error',
        'message' => 'خطا در پردازش اطلاعات: ' . $e->getMessage()
    ], JSON_UNESCAPED_UNICODE);
}
?>

