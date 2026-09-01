<?php
// File: /webapp/search_cache.php
// سیستم کش و بهینه‌سازی جستجو

class SearchCache {
    private $cacheDir;
    private $cacheExpiry;
    
    public function __construct($cacheDir = 'cache', $cacheExpiry = 3600) {
        $this->cacheDir = __DIR__ . '/' . $cacheDir;
        $this->cacheExpiry = $cacheExpiry;
        
        // ایجاد پوشه کش در صورت عدم وجود
        if (!is_dir($this->cacheDir)) {
            mkdir($this->cacheDir, 0755, true);
        }
    }
    
    /**
     * دریافت کلید کش بر اساس پارامترهای جستجو
     */
    private function getCacheKey($params) {
        ksort($params); // مرتب‌سازی برای ثبات کلید
        return md5(serialize($params));
    }
    
    /**
     * دریافت نتیجه از کش
     */
    public function get($params) {
        $cacheKey = $this->getCacheKey($params);
        $cacheFile = $this->cacheDir . '/' . $cacheKey . '.cache';
        
        if (file_exists($cacheFile)) {
            $cacheData = json_decode(file_get_contents($cacheFile), true);
            
            // بررسی انقضای کش
            if ($cacheData && time() - $cacheData['timestamp'] < $this->cacheExpiry) {
                return $cacheData['data'];
            } else {
                // حذف کش منقضی شده
                unlink($cacheFile);
            }
        }
        
        return null;
    }
    
    /**
     * ذخیره نتیجه در کش
     */
    public function set($params, $data) {
        $cacheKey = $this->getCacheKey($params);
        $cacheFile = $this->cacheDir . '/' . $cacheKey . '.cache';
        
        $cacheData = [
            'timestamp' => time(),
            'data' => $data
        ];
        
        file_put_contents($cacheFile, json_encode($cacheData, JSON_UNESCAPED_UNICODE));
    }
    
    /**
     * پاک کردن کش منقضی شده
     */
    public function cleanup() {
        $files = glob($this->cacheDir . '/*.cache');
        $cleaned = 0;
        
        foreach ($files as $file) {
            $cacheData = json_decode(file_get_contents($file), true);
            if ($cacheData && time() - $cacheData['timestamp'] >= $this->cacheExpiry) {
                unlink($file);
                $cleaned++;
            }
        }
        
        return $cleaned;
    }
    
    /**
     * پاک کردن تمام کش
     */
    public function clear() {
        $files = glob($this->cacheDir . '/*.cache');
        foreach ($files as $file) {
            unlink($file);
        }
        return count($files);
    }
}

/**
 * کلاس بهینه‌سازی جستجو
 */
class SearchOptimizer {
    private $pdo;
    
    public function __construct($pdo) {
        $this->pdo = $pdo;
    }
    
    /**
     * ایجاد ایندکس‌های بهینه برای جستجو
     */
    public function createOptimalIndexes() {
        $indexes = [
            // ایندکس‌های اصلی برای جستجو
            "CREATE INDEX IF NOT EXISTS idx_businesses_active ON businesses(is_active)",
            "CREATE INDEX IF NOT EXISTS idx_businesses_type ON businesses(business_type)",
            "CREATE INDEX IF NOT EXISTS idx_businesses_location ON businesses(province, city)",
            "CREATE INDEX IF NOT EXISTS idx_businesses_name ON businesses(name)",
            "CREATE INDEX IF NOT EXISTS idx_businesses_created ON businesses(created_at)",
            
            // ایندکس‌های ترکیبی برای بهبود عملکرد
            "CREATE INDEX IF NOT EXISTS idx_businesses_active_type ON businesses(is_active, business_type)",
            "CREATE INDEX IF NOT EXISTS idx_businesses_active_location ON businesses(is_active, province, city)",
            "CREATE INDEX IF NOT EXISTS idx_businesses_search ON businesses(is_active, name, business_type)",
            
            // ایندکس‌های متنی برای جستجوی فول‌تکست
            "CREATE FULLTEXT INDEX IF NOT EXISTS idx_businesses_fulltext ON businesses(name, business_type, description, address)",
            
            // ایندکس‌های خدمات
            "CREATE INDEX IF NOT EXISTS idx_services_business ON services(business_id, is_active)",
            "CREATE INDEX IF NOT EXISTS idx_services_name ON services(name)",
            "CREATE FULLTEXT INDEX IF NOT EXISTS idx_services_fulltext ON services(name, description)"
        ];
        
        $created = 0;
        foreach ($indexes as $indexSql) {
            try {
                $this->pdo->exec($indexSql);
                $created++;
            } catch (PDOException $e) {
                error_log("Index creation error: " . $e->getMessage());
            }
        }
        
        return $created;
    }
    
    /**
     * تحلیل عملکرد کوئری
     */
    public function analyzeQuery($sql, $params = []) {
        try {
            $explainSql = "EXPLAIN " . $sql;
            $stmt = $this->pdo->prepare($explainSql);
            $stmt->execute($params);
            return $stmt->fetchAll();
        } catch (PDOException $e) {
            error_log("Query analysis error: " . $e->getMessage());
            return null;
        }
    }
    
    /**
     * بهینه‌سازی جدول‌ها
     */
    public function optimizeTables() {
        $tables = ['businesses', 'services', 'users'];
        $optimized = 0;
        
        foreach ($tables as $table) {
            try {
                $this->pdo->exec("OPTIMIZE TABLE {$table}");
                $optimized++;
            } catch (PDOException $e) {
                error_log("Table optimization error for {$table}: " . $e->getMessage());
            }
        }
        
        return $optimized;
    }
    
    /**
     * آمار عملکرد پایگاه داده
     */
    public function getPerformanceStats() {
        try {
            $stats = [];
            
            // تعداد کل کسب‌وکارها
            $stmt = $this->pdo->query("SELECT COUNT(*) as total FROM businesses WHERE is_active = 1");
            $stats['total_businesses'] = $stmt->fetch()['total'];
            
            // تعداد کل خدمات
            $stmt = $this->pdo->query("SELECT COUNT(*) as total FROM services WHERE is_active = 1");
            $stats['total_services'] = $stmt->fetch()['total'];
            
            // تعداد دسته‌بندی‌ها
            $stmt = $this->pdo->query("SELECT COUNT(DISTINCT business_type) as total FROM businesses WHERE is_active = 1 AND business_type IS NOT NULL");
            $stats['total_categories'] = $stmt->fetch()['total'];
            
            // تعداد استان‌ها
            $stmt = $this->pdo->query("SELECT COUNT(DISTINCT province) as total FROM businesses WHERE is_active = 1 AND province IS NOT NULL");
            $stats['total_provinces'] = $stmt->fetch()['total'];
            
            // تعداد شهرها
            $stmt = $this->pdo->query("SELECT COUNT(DISTINCT city) as total FROM businesses WHERE is_active = 1 AND city IS NOT NULL");
            $stats['total_cities'] = $stmt->fetch()['total'];
            
            // اندازه پایگاه داده
            $stmt = $this->pdo->query("
                SELECT 
                    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS size_mb
                FROM information_schema.tables 
                WHERE table_schema = DATABASE()
            ");
            $stats['database_size_mb'] = $stmt->fetch()['size_mb'];
            
            return $stats;
            
        } catch (PDOException $e) {
            error_log("Performance stats error: " . $e->getMessage());
            return null;
        }
    }
}

/**
 * کلاس مدیریت لاگ جستجو
 */
class SearchLogger {
    private $logFile;
    
    public function __construct($logFile = 'search.log') {
        $this->logFile = __DIR__ . '/' . $logFile;
    }
    
    /**
     * ثبت جستجو
     */
    public function logSearch($params, $resultCount, $executionTime) {
        $logEntry = [
            'timestamp' => date('Y-m-d H:i:s'),
            'query' => $params['query'] ?? '',
            'category' => $params['category'] ?? '',
            'province' => $params['province'] ?? '',
            'city' => $params['city'] ?? '',
            'result_count' => $resultCount,
            'execution_time' => $executionTime,
            'ip' => $_SERVER['REMOTE_ADDR'] ?? 'unknown'
        ];
        
        file_put_contents(
            $this->logFile, 
            json_encode($logEntry, JSON_UNESCAPED_UNICODE) . "\n", 
            FILE_APPEND | LOCK_EX
        );
    }
    
    /**
     * دریافت آمار جستجو
     */
    public function getSearchStats($days = 7) {
        if (!file_exists($this->logFile)) {
            return null;
        }
        
        $lines = file($this->logFile, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
        $cutoffDate = date('Y-m-d H:i:s', strtotime("-{$days} days"));
        
        $stats = [
            'total_searches' => 0,
            'popular_queries' => [],
            'popular_categories' => [],
            'popular_locations' => [],
            'avg_execution_time' => 0,
            'avg_results' => 0
        ];
        
        $executionTimes = [];
        $resultCounts = [];
        $queries = [];
        $categories = [];
        $locations = [];
        
        foreach ($lines as $line) {
            $entry = json_decode($line, true);
            if ($entry && $entry['timestamp'] >= $cutoffDate) {
                $stats['total_searches']++;
                
                if (!empty($entry['query'])) {
                    $queries[] = $entry['query'];
                }
                
                if (!empty($entry['category'])) {
                    $categories[] = $entry['category'];
                }
                
                if (!empty($entry['province'])) {
                    $locations[] = $entry['province'];
                }
                
                $executionTimes[] = $entry['execution_time'];
                $resultCounts[] = $entry['result_count'];
            }
        }
        
        // محاسبه آمار
        if (!empty($executionTimes)) {
            $stats['avg_execution_time'] = array_sum($executionTimes) / count($executionTimes);
        }
        
        if (!empty($resultCounts)) {
            $stats['avg_results'] = array_sum($resultCounts) / count($resultCounts);
        }
        
        // محبوب‌ترین جستجوها
        $stats['popular_queries'] = array_slice(array_count_values($queries), 0, 10, true);
        $stats['popular_categories'] = array_slice(array_count_values($categories), 0, 10, true);
        $stats['popular_locations'] = array_slice(array_count_values($locations), 0, 10, true);
        
        return $stats;
    }
}

?>