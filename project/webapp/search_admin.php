<?php
// File: /webapp/search_admin.php
// پنل مدیریت و آمار سیستم جستجو

require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/search_cache.php';

// تابع اتصال به پایگاه داده
function getDbConnection() {
    try {
        $pdo = new PDO(
            "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=utf8mb4",
            DB_USER,
            DB_PASS,
            [
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::MYSQL_ATTR_INIT_COMMAND => "SET NAMES utf8mb4"
            ]
        );
        return $pdo;
    } catch (PDOException $e) {
        error_log("Database connection error: " . $e->getMessage());
        return null;
    }
}

$pdo = getDbConnection();
$searchCache = new SearchCache();
$searchLogger = new SearchLogger();
$optimizer = new SearchOptimizer($pdo);

// مدیریت عملیات
$action = $_GET['action'] ?? '';
$message = '';

switch ($action) {
    case 'clear_cache':
        $cleared = $searchCache->clear();
        $message = "کش پاک شد. {$cleared} فایل حذف شد.";
        break;
        
    case 'cleanup_cache':
        $cleaned = $searchCache->cleanup();
        $message = "کش‌های منقضی شده پاک شدند. {$cleaned} فایل حذف شد.";
        break;
        
    case 'optimize_tables':
        $optimized = $optimizer->optimizeTables();
        $message = "جدول‌ها بهینه‌سازی شدند. {$optimized} جدول پردازش شد.";
        break;
        
    case 'create_indexes':
        $created = $optimizer->createOptimalIndexes();
        $message = "ایندکس‌ها ایجاد شدند. {$created} ایندکس پردازش شد.";
        break;
}

// دریافت آمار
$performanceStats = $optimizer->getPerformanceStats();
$searchStats = $searchLogger->getSearchStats(30); // آمار 30 روز گذشته

?>
<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>پنل مدیریت جستجو</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: #f5f7fa;
            color: #333;
            line-height: 1.6;
        }

        .container {
            max-width: 1200px;
            margin: 0 auto;
            padding: 20px;
        }

        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            border-radius: 15px;
            margin-bottom: 30px;
            text-align: center;
        }

        .header h1 {
            font-size: 2.5rem;
            margin-bottom: 10px;
        }

        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }

        .stat-card {
            background: white;
            padding: 25px;
            border-radius: 15px;
            box-shadow: 0 5px 15px rgba(0,0,0,0.08);
            border: 1px solid #e1e5e9;
        }

        .stat-card h3 {
            color: #667eea;
            margin-bottom: 15px;
            font-size: 1.1rem;
        }

        .stat-value {
            font-size: 2rem;
            font-weight: bold;
            color: #333;
            margin-bottom: 5px;
        }

        .stat-label {
            color: #666;
            font-size: 0.9rem;
        }

        .actions-section {
            background: white;
            padding: 30px;
            border-radius: 15px;
            box-shadow: 0 5px 15px rgba(0,0,0,0.08);
            margin-bottom: 30px;
        }

        .actions-section h2 {
            margin-bottom: 20px;
            color: #333;
        }

        .actions-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
        }

        .action-btn {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            padding: 15px 20px;
            border-radius: 10px;
            cursor: pointer;
            text-decoration: none;
            text-align: center;
            font-weight: 600;
            transition: all 0.3s ease;
            display: block;
        }

        .action-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 10px 20px rgba(102, 126, 234, 0.3);
        }

        .action-btn.danger {
            background: linear-gradient(135deg, #ff6b6b 0%, #ee5a52 100%);
        }

        .action-btn.warning {
            background: linear-gradient(135deg, #feca57 0%, #ff9ff3 100%);
        }

        .message {
            background: #d4edda;
            color: #155724;
            padding: 15px;
            border-radius: 10px;
            margin-bottom: 20px;
            border: 1px solid #c3e6cb;
        }

        .popular-section {
            background: white;
            padding: 30px;
            border-radius: 15px;
            box-shadow: 0 5px 15px rgba(0,0,0,0.08);
            margin-bottom: 30px;
        }

        .popular-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 30px;
        }

        .popular-list {
            list-style: none;
        }

        .popular-list li {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 10px 0;
            border-bottom: 1px solid #eee;
        }

        .popular-list li:last-child {
            border-bottom: none;
        }

        .popular-term {
            font-weight: 600;
            color: #333;
        }

        .popular-count {
            background: #667eea;
            color: white;
            padding: 4px 8px;
            border-radius: 12px;
            font-size: 0.8rem;
        }

        .chart-container {
            background: white;
            padding: 30px;
            border-radius: 15px;
            box-shadow: 0 5px 15px rgba(0,0,0,0.08);
            margin-bottom: 30px;
        }

        .performance-metrics {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin-top: 20px;
        }

        .metric {
            text-align: center;
            padding: 20px;
            background: #f8f9fa;
            border-radius: 10px;
        }

        .metric-value {
            font-size: 1.5rem;
            font-weight: bold;
            color: #667eea;
            margin-bottom: 5px;
        }

        .metric-label {
            color: #666;
            font-size: 0.9rem;
        }

        @media (max-width: 768px) {
            .container {
                padding: 10px;
            }

            .header {
                padding: 20px;
            }

            .header h1 {
                font-size: 2rem;
            }

            .stats-grid,
            .actions-grid,
            .popular-grid {
                grid-template-columns: 1fr;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>📊 پنل مدیریت جستجو</h1>
            <p>نظارت و بهینه‌سازی سیستم جستجوی پیشرفته</p>
        </div>

        <?php if ($message): ?>
            <div class="message"><?= htmlspecialchars($message) ?></div>
        <?php endif; ?>

        <!-- آمار کلی -->
        <div class="stats-grid">
            <?php if ($performanceStats): ?>
                <div class="stat-card">
                    <h3>کل کسب‌وکارها</h3>
                    <div class="stat-value"><?= number_format($performanceStats['total_businesses']) ?></div>
                    <div class="stat-label">کسب‌وکار فعال</div>
                </div>
                
                <div class="stat-card">
                    <h3>کل خدمات</h3>
                    <div class="stat-value"><?= number_format($performanceStats['total_services']) ?></div>
                    <div class="stat-label">خدمت ارائه شده</div>
                </div>
                
                <div class="stat-card">
                    <h3>دسته‌بندی‌ها</h3>
                    <div class="stat-value"><?= number_format($performanceStats['total_categories']) ?></div>
                    <div class="stat-label">دسته مختلف</div>
                </div>
                
                <div class="stat-card">
                    <h3>پوشش جغرافیایی</h3>
                    <div class="stat-value"><?= number_format($performanceStats['total_provinces']) ?></div>
                    <div class="stat-label">استان</div>
                </div>
                
                <div class="stat-card">
                    <h3>شهرها</h3>
                    <div class="stat-value"><?= number_format($performanceStats['total_cities']) ?></div>
                    <div class="stat-label">شهر مختلف</div>
                </div>
                
                <div class="stat-card">
                    <h3>حجم پایگاه داده</h3>
                    <div class="stat-value"><?= $performanceStats['database_size_mb'] ?></div>
                    <div class="stat-label">مگابایت</div>
                </div>
            <?php endif; ?>
        </div>

        <!-- عملیات مدیریت -->
        <div class="actions-section">
            <h2>عملیات مدیریت</h2>
            <div class="actions-grid">
                <a href="?action=clear_cache" class="action-btn danger" onclick="return confirm('آیا مطمئن هستید؟')">
                    🗑️ پاک کردن کش
                </a>
                <a href="?action=cleanup_cache" class="action-btn warning">
                    🧹 پاکسازی کش منقضی
                </a>
                <a href="?action=optimize_tables" class="action-btn">
                    ⚡ بهینه‌سازی جدول‌ها
                </a>
                <a href="?action=create_indexes" class="action-btn">
                    📊 ایجاد ایندکس‌ها
                </a>
                <a href="search.html" class="action-btn">
                    🔍 صفحه جستجو
                </a>
                <a href="search_api.php?action=filters" class="action-btn" target="_blank">
                    📋 API فیلترها
                </a>
            </div>
        </div>

        <!-- آمار جستجو -->
        <?php if ($searchStats): ?>
            <div class="chart-container">
                <h2>آمار جستجو (30 روز گذشته)</h2>
                <div class="performance-metrics">
                    <div class="metric">
                        <div class="metric-value"><?= number_format($searchStats['total_searches']) ?></div>
                        <div class="metric-label">کل جستجوها</div>
                    </div>
                    <div class="metric">
                        <div class="metric-value"><?= number_format($searchStats['avg_results'], 1) ?></div>
                        <div class="metric-label">میانگین نتایج</div>
                    </div>
                    <div class="metric">
                        <div class="metric-value"><?= number_format($searchStats['avg_execution_time'] * 1000, 1) ?></div>
                        <div class="metric-label">میانگین زمان (ms)</div>
                    </div>
                    <div class="metric">
                        <div class="metric-value"><?= number_format($searchStats['total_searches'] / 30, 1) ?></div>
                        <div class="metric-label">جستجو در روز</div>
                    </div>
                </div>
            </div>

            <!-- محبوب‌ترین جستجوها -->
            <div class="popular-section">
                <h2>محبوب‌ترین موارد</h2>
                <div class="popular-grid">
                    <div>
                        <h3>جستجوهای محبوب</h3>
                        <ul class="popular-list">
                            <?php foreach (array_slice($searchStats['popular_queries'], 0, 10, true) as $query => $count): ?>
                                <li>
                                    <span class="popular-term"><?= htmlspecialchars($query) ?></span>
                                    <span class="popular-count"><?= $count ?></span>
                                </li>
                            <?php endforeach; ?>
                        </ul>
                    </div>
                    
                    <div>
                        <h3>دسته‌بندی‌های محبوب</h3>
                        <ul class="popular-list">
                            <?php foreach (array_slice($searchStats['popular_categories'], 0, 10, true) as $category => $count): ?>
                                <li>
                                    <span class="popular-term"><?= htmlspecialchars($category) ?></span>
                                    <span class="popular-count"><?= $count ?></span>
                                </li>
                            <?php endforeach; ?>
                        </ul>
                    </div>
                    
                    <div>
                        <h3>مکان‌های محبوب</h3>
                        <ul class="popular-list">
                            <?php foreach (array_slice($searchStats['popular_locations'], 0, 10, true) as $location => $count): ?>
                                <li>
                                    <span class="popular-term"><?= htmlspecialchars($location) ?></span>
                                    <span class="popular-count"><?= $count ?></span>
                                </li>
                            <?php endforeach; ?>
                        </ul>
                    </div>
                </div>
            </div>
        <?php endif; ?>
    </div>

    <script>
        // به‌روزرسانی خودکار صفحه هر 5 دقیقه
        setTimeout(() => {
            window.location.reload();
        }, 300000);
        
        // نمایش زمان آخرین به‌روزرسانی
        document.addEventListener('DOMContentLoaded', () => {
            const now = new Date();
            const timeString = now.toLocaleTimeString('fa-IR');
            document.title = `پنل مدیریت جستجو - ${timeString}`;
        });
    </script>
</body>
</html>