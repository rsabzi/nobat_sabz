<?php
session_start();
require_once '../config.php';
require_once 'admin_guard.php'; // محافظت از API

header('Content-Type: application/json; charset=utf-8');

// اتصال دیتابیس: از PDO موجود در config.php استفاده می‌شود (MySQL)
function getDatabase() {
    global $pdo;
    if ($pdo instanceof PDO) {
        return $pdo;
    }
    // تلاش برای اتصال MySQL بر مبنای ثابت‌های config.php
    if (defined('DB_HOST') && defined('DB_NAME') && defined('DB_USER')) {
        try {
            $dsn = "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=" . (defined('DB_CHARSET') ? DB_CHARSET : 'utf8mb4');
            $pdoLocal = new PDO($dsn, DB_USER, defined('DB_PASS') ? DB_PASS : '', [
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::MYSQL_ATTR_INIT_COMMAND => "SET NAMES utf8mb4"
            ]);
            return $pdoLocal;
        } catch (PDOException $e) {
            throw new Exception("MySQL connection failed: " . $e->getMessage());
        }
    }
    throw new Exception('Database connection is not configured');
}

// خواندن متغیرهای ورودی JSON
$input = json_decode(file_get_contents('php://input'), true);
$action = $_GET['action'] ?? $input['action'] ?? '';
$table = $_GET['table'] ?? $input['table'] ?? '';

// جداول مجاز
$allowedTables = [
    'users', 'businesses', 'services', 'bookings', 'marketers', 
    'schedules', 'schedule_overrides', 'daily_schedules', 'specific_time_slots',
    'payments', 'SubscriptionPlans', 'user_subscriptions', 'business_subscriptions',
    'appointments', 'booking_tokens', 'security_tokens', 'user_token_grants',
    'provinces', 'cities', 'business_categories', 'referrals', 'time_slots',
    'user_referral_codes', 'pending_orders', 'failed_attempts', 'ip_restrictions',
    'reservation_tokens', 'subscription_history', 'token_usage_history', 'user_registration_temp'
];

if (!in_array($table, $allowedTables)) {
    http_response_code(400);
    echo json_encode(['error' => 'جدول نامعتبر']);
    exit;
}

try {
    $pdo = getDatabase();
    
    // کلید اصلی جدول را پیدا کنیم
    $primaryKey = 'id';
    try {
        $pragma = $pdo->query("PRAGMA table_info(`$table`)")->fetchAll();
        if (!empty($pragma)) {
            foreach ($pragma as $col) {
                if ((isset($col['pk']) && (int)$col['pk'] === 1)) { $primaryKey = $col['name']; break; }
            }
        } else {
            // MySQL
            $cols = $pdo->query("SHOW COLUMNS FROM `$table`")->fetchAll();
            foreach ($cols as $col) {
                if (($col['Key'] ?? '') === 'PRI') { $primaryKey = $col['Field']; break; }
            }
        }
    } catch (Exception $e) {
        // اگر خطا شد، همان id باقی می‌ماند
    }
    switch($action) {
        case 'list':
            $search = $_GET['search'] ?? '';
            $page = max(1, intval($_GET['page'] ?? 1));
            $limit = min(100, max(10, intval($_GET['limit'] ?? 50)));
            $offset = ($page - 1) * $limit;
            
            // شمارش کل رکوردها
            $countSql = "SELECT COUNT(*) as total FROM `$table`";
            $countParams = [];
            $searchConditions = [];
            
            if ($search) {
                // تلاش برای PRAGMA، در MySQL خطا می‌دهد، پس try/catch
                $columns = [];
                try {
                    $columns = $pdo->query("PRAGMA table_info(`$table`)")->fetchAll();
                } catch (Exception $e) {
                    $columns = [];
                }
                if (empty($columns)) {
                    // MySQL
                    $columns = $pdo->query("SHOW COLUMNS FROM `$table`")->fetchAll();
                    foreach ($columns as $col) {
                        $colName = $col['Field'] ?? $col['name'] ?? null;
                        if ($colName) {
                            $searchConditions[] = "`$colName` LIKE ?";
                            $countParams[] = "%$search%";
                        }
                    }
                } else {
                    // SQLite
                    foreach ($columns as $col) {
                        $colName = $col['name'];
                        $searchConditions[] = "`$colName` LIKE ?";
                        $countParams[] = "%$search%";
                    }
                }
                if (!empty($searchConditions)) {
                    $countSql .= " WHERE " . implode(" OR ", $searchConditions);
                }
            }
            
            $countStmt = $pdo->prepare($countSql);
            $countStmt->execute($countParams);
            $total = $countStmt->fetch()['total'];
            
            // واکشی داده‌ها
            $sql = "SELECT * FROM `$table`";
            if ($search && !empty($searchConditions)) {
                $sql .= " WHERE " . implode(" OR ", $searchConditions);
            }
            $sql .= " LIMIT $limit OFFSET $offset";
            
            $stmt = $pdo->prepare($sql);
            $stmt->execute($countParams);
            $data = $stmt->fetchAll();
            
            echo json_encode([
                'success' => true,
                'data' => $data,
                'total' => $total,
                'page' => $page,
                'limit' => $limit,
                'primaryKey' => $primaryKey
            ]);
            break;
            
        case 'get':
            $id = $input['id'] ?? $_GET['id'] ?? '';
            if (!$id) {
                throw new Exception('شناسه مطلوب است');
            }
            
            $stmt = $pdo->prepare("SELECT * FROM `$table` WHERE `$primaryKey` = ?");
            $stmt->execute([$id]);
            $record = $stmt->fetch();
            
            if (!$record) {
                throw new Exception('رکورد یافت نشد');
            }
            
            echo json_encode(['success' => true, 'data' => $record]);
            break;
            
        case 'create':
            $data = $input['data'] ?? [];
            if (empty($data)) {
                throw new Exception('داده‌های ایجاد مطلوب است');
            }
            
            $columns = array_keys($data);
            $placeholders = array_fill(0, count($columns), '?');
            
            $sql = "INSERT INTO `$table` (`" . implode('`, `', $columns) . "`) VALUES (" . implode(', ', $placeholders) . ")";
            $stmt = $pdo->prepare($sql);
            $stmt->execute(array_values($data));
            
            $newId = $pdo->lastInsertId();
            echo json_encode(['success' => true, 'id' => $newId, 'message' => 'رکورد ایجاد شد']);
            break;
            
        case 'update':
            $id = $input['id'] ?? '';
            $data = $input['data'] ?? [];
            
            if (!$id || empty($data)) {
                throw new Exception('شناسه و داده‌های به‌روزرسانی مطلوب است');
            }
            
            $columns = array_keys($data);
            $setParts = array_map(function($col) { return "`$col` = ?"; }, $columns);
            
            $sql = "UPDATE `$table` SET " . implode(', ', $setParts) . " WHERE `$primaryKey` = ?";
            $params = array_values($data);
            $params[] = $id;
            
            $stmt = $pdo->prepare($sql);
            $stmt->execute($params);
            
            echo json_encode(['success' => true, 'message' => 'رکورد به‌روزرسانی شد']);
            break;
            
        case 'delete':
            $id = $input['id'] ?? $_GET['id'] ?? '';
            if (!$id) {
                throw new Exception('شناسه مطلوب است');
            }
            
            $stmt = $pdo->prepare("DELETE FROM `$table` WHERE `$primaryKey` = ?");
            $stmt->execute([$id]);
            
            echo json_encode(['success' => true, 'message' => 'رکورد حذف شد']);
            break;
            
        case 'columns':
            // واکشی ساختار جدول
            try {
                $stmt = $pdo->query("PRAGMA table_info(`$table`)");
                $columns = $stmt->fetchAll();
                
                if (empty($columns)) {
                    // برای MySQL
                    $stmt = $pdo->query("SHOW COLUMNS FROM `$table`");
                    $columns = $stmt->fetchAll();
                    $columns = array_map(function($col) {
                        return [
                            'name' => $col['Field'],
                            'type' => $col['Type'],
                            'notnull' => $col['Null'] === 'NO' ? 1 : 0,
                            'pk' => $col['Key'] === 'PRI' ? 1 : 0
                        ];
                    }, $columns);
                }
                
                echo json_encode(['success' => true, 'columns' => $columns]);
            } catch (Exception $e) {
                echo json_encode(['success' => true, 'columns' => []]);
            }
            break;

        case 'health':
            // بررسی وضعیت اتصال و چند آمار کلیدی
            $engine = 'mysql';
            $tables = [];
            $tableCounts = [];
            
            // لیست جداول شناخته‌شده
            $tables = [
                'users','businesses','services','bookings','payments','SubscriptionPlans',
                'schedules','schedule_overrides','daily_schedules','specific_time_slots',
                'marketers'
            ];
            foreach ($tables as $t) {
                try {
                    $stmt = $pdo->query("SELECT COUNT(*) AS c FROM `$t`");
                    $row = $stmt->fetch();
                    $tableCounts[$t] = (int)($row['c'] ?? 0);
                } catch (Exception $e) {
                    $tableCounts[$t] = null; // جدول در دسترس نیست یا ساخت نشده
                }
            }
            
            // یک کوئری ساده برای اتصال
            $connOk = true; $connError = null;
            try { $pdo->query("SELECT 1"); } catch (Exception $e) { $connOk = false; $connError = $e->getMessage(); }
            
            echo json_encode([
                'success' => true,
                'engine' => $engine,
                'connection' => $connOk ? 'ok' : 'fail',
                'connection_error' => $connError,
                'tables' => $tableCounts
            ]);
            break;

        case 'logout':
            $_SESSION = [];
            if (ini_get('session.use_cookies')) {
                $params = session_get_cookie_params();
                setcookie(session_name(), '', time() - 42000,
                    $params['path'], $params['domain'],
                    $params['secure'], $params['httponly']
                );
            }
            session_destroy();
            echo json_encode(['success' => true]);
            break;
        
        default:
            throw new Exception('عملیات نامعتبر');
    }
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['error' => $e->getMessage()]);
}
?>