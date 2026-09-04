<?php
// File: /webapp/search_api.php
// API جستجوی پیشرفته کسب‌وکارها

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

require_once __DIR__ . '/../config.php';

// اطمینان از دسترسی به متغیر $pdo
if (!isset($pdo) || $pdo === null) {
    try {
        // استفاده از SQLite به جای MySQL
        $db_path = __DIR__ . '/../database/booking_system.db';
        if (file_exists($db_path)) {
            $pdo = new PDO("sqlite:" . $db_path);
            $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
        } else {
            error_log("SQLite database not found: " . $db_path);
            $pdo = null;
        }
    } catch (PDOException $e) {
        error_log("Database Connection Failed in search_api.php: " . $e->getMessage());
        $pdo = null;
    }
}

// تابع جستجوی پیشرفته
function advancedSearch($params) {
    global $pdo;
    $startTime = microtime(true);
    
    // بررسی اتصال دیتابیس
    if (!$pdo) {
        // در صورت عدم اتصال، از داده‌های نمونه استفاده کن
        return searchWithSampleData($params);
    }
    
    // پارامترهای جستجو
    $query = isset($params['query']) ? trim($params['query']) : '';
    $category = isset($params['category']) ? trim($params['category']) : '';
    $province = isset($params['province']) ? trim($params['province']) : '';
    $city = isset($params['city']) ? trim($params['city']) : '';
    $sortBy = isset($params['sort']) ? $params['sort'] : 'relevance';
    $page = isset($params['page']) ? max(1, intval($params['page'])) : 1;
    $limit = isset($params['limit']) ? min(50, max(1, intval($params['limit']))) : 20;
    
    try {
        // ساخت کوئری پایه
        $sql = "SELECT b.*, u.first_name as owner_name, u.last_name as owner_last_name,
                       COUNT(s.id) as services_count
                FROM businesses b 
                LEFT JOIN users u ON b.owner_id = u.id 
                LEFT JOIN services s ON b.id = s.business_id AND s.is_active = 1
                WHERE b.is_active = 1";
        
        $params_array = [];
        
        // اضافه کردن شرایط جستجو با حل مشکل PDO parameter
        if (!empty($query)) {
            // تمیز کردن و آماده‌سازی کلمه جستجو
            $cleanQuery = trim($query);
            
            // تقسیم کلمات برای جستجوی چندکلمه‌ای
            $words = preg_split('/\s+/', $cleanQuery);
            $searchConditions = [];
            
            foreach ($words as $index => $word) {
                if (!empty(trim($word))) {
                    $wordParam = 'word_' . $index;
                    $searchConditions[] = "(
                        b.name LIKE :{$wordParam}_name OR 
                        b.description LIKE :{$wordParam}_desc OR 
                        b.business_type LIKE :{$wordParam}_type OR
                        b.address LIKE :{$wordParam}_addr OR
                        s.name LIKE :{$wordParam}_service
                    )";
                    
                    // تعریف پارامترهای جداگانه برای هر فیلد
                    $params_array[":{$wordParam}_name"] = '%' . $word . '%';
                    $params_array[":{$wordParam}_desc"] = '%' . $word . '%';
                    $params_array[":{$wordParam}_type"] = '%' . $word . '%';
                    $params_array[":{$wordParam}_addr"] = '%' . $word . '%';
                    $params_array[":{$wordParam}_service"] = '%' . $word . '%';
                }
            }
            
            if (!empty($searchConditions)) {
                $sql .= " AND (" . implode(' AND ', $searchConditions) . ")";
            }
        }
        
        if (!empty($category)) {
            $sql .= " AND b.business_type = :category";
            $params_array[':category'] = $category;
        }
        
        if (!empty($province)) {
            $sql .= " AND b.province = :province";
            $params_array[':province'] = $province;
        }
        
        if (!empty($city)) {
            $sql .= " AND b.city = :city";
            $params_array[':city'] = $city;
        }
        
        $sql .= " GROUP BY b.id";
        
        // مرتب‌سازی بهبود یافته
        switch ($sortBy) {
            case 'name':
                $sql .= " ORDER BY b.name COLLATE NOCASE ASC";
                break;
            case 'newest':
                $sql .= " ORDER BY b.created_at DESC";
                break;
            case 'oldest':
                $sql .= " ORDER BY b.created_at ASC";
                break;
            case 'services':
                $sql .= " ORDER BY services_count DESC, b.name COLLATE NOCASE ASC";
                break;
            default: // relevance
                if (!empty($query)) {
                    // امتیازدهی بر اساس میزان تطابق
                    $relevanceScore = "
                        (CASE 
                            WHEN LOWER(b.name) = LOWER(:relevance_exact) THEN 100
                            WHEN LOWER(b.name) LIKE LOWER(:relevance_start) THEN 80
                            WHEN LOWER(b.business_type) = LOWER(:relevance_exact_type) THEN 70
                            WHEN LOWER(b.name) LIKE LOWER(:relevance_contain) THEN 60
                            WHEN LOWER(b.business_type) LIKE LOWER(:relevance_contain_type) THEN 50
                            WHEN LOWER(b.description) LIKE LOWER(:relevance_contain_desc) THEN 40
                            WHEN LOWER(b.address) LIKE LOWER(:relevance_contain_addr) THEN 30
                            ELSE 10 
                        END)";
                    
                    $sql .= " ORDER BY $relevanceScore DESC, services_count DESC, b.created_at DESC";
                    
                    // پارامترهای امتیازدهی
                    $params_array[':relevance_exact'] = $cleanQuery;
                    $params_array[':relevance_start'] = $cleanQuery . '%';
                    $params_array[':relevance_contain'] = '%' . $cleanQuery . '%';
                    $params_array[':relevance_exact_type'] = $cleanQuery;
                    $params_array[':relevance_contain_type'] = '%' . $cleanQuery . '%';
                    $params_array[':relevance_contain_desc'] = '%' . $cleanQuery . '%';
                    $params_array[':relevance_contain_addr'] = '%' . $cleanQuery . '%';
                } else {
                    $sql .= " ORDER BY b.created_at DESC";
                }
                break;
        }
        
        // شمارش کل نتایج با بهبود
        $count_sql = "SELECT COUNT(DISTINCT b.id) as total 
                      FROM businesses b 
                      LEFT JOIN users u ON b.owner_id = u.id 
                      LEFT JOIN services s ON b.id = s.business_id AND s.is_active = 1
                      WHERE b.is_active = 1";
        
        $count_params = [];
        
        if (!empty($query)) {
            $words = preg_split('/\s+/', $cleanQuery);
            $countConditions = [];
            
            foreach ($words as $index => $word) {
                if (!empty(trim($word))) {
                    $wordParam = 'count_word_' . $index;
                    $countConditions[] = "(
                        b.name LIKE :{$wordParam}_name OR 
                        b.description LIKE :{$wordParam}_desc OR 
                        b.business_type LIKE :{$wordParam}_type OR
                        b.address LIKE :{$wordParam}_addr OR
                        s.name LIKE :{$wordParam}_service
                    )";
                    
                    $count_params[":{$wordParam}_name"] = '%' . $word . '%';
                    $count_params[":{$wordParam}_desc"] = '%' . $word . '%';
                    $count_params[":{$wordParam}_type"] = '%' . $word . '%';
                    $count_params[":{$wordParam}_addr"] = '%' . $word . '%';
                    $count_params[":{$wordParam}_service"] = '%' . $word . '%';
                }
            }
            
            if (!empty($countConditions)) {
                $count_sql .= " AND (" . implode(' AND ', $countConditions) . ")";
            }
        }
        
        if (!empty($category)) {
            $count_sql .= " AND b.business_type = :count_category";
            $count_params[':count_category'] = $category;
        }
        if (!empty($province)) {
            $count_sql .= " AND b.province = :count_province";
            $count_params[':count_province'] = $province;
        }
        if (!empty($city)) {
            $count_sql .= " AND b.city = :count_city";
            $count_params[':count_city'] = $city;
        }
        
        $count_stmt = $pdo->prepare($count_sql);
        $count_stmt->execute($count_params);
        $totalCount = $count_stmt->fetch()['total'];
        
        // صفحه‌بندی
        // نکته: MySQL 8 عدد داخل کوتیشن را در LIMIT/OFFSET نمی‌پذیرد؛
        // مقادیر به‌صورت int امن درج می‌شوند (نه bind رشته‌ای)
        $offset = ($page - 1) * $limit;
        $sql .= " LIMIT " . (int)$limit . " OFFSET " . (int)$offset;
        
        // اجرای کوئری اصلی
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params_array);
        $businesses = $stmt->fetchAll();
        
        // دریافت خدمات برای هر کسب‌وکار
        foreach ($businesses as &$business) {
            $business['owner_name'] = trim($business['owner_name'] . ' ' . ($business['owner_last_name'] ?? ''));
            unset($business['owner_last_name']);
            
            // دریافت خدمات
            $services_sql = "SELECT name FROM services WHERE business_id = :business_id AND is_active = 1";
            $services_stmt = $pdo->prepare($services_sql);
            $services_stmt->execute([':business_id' => $business['id']]);
            $services = $services_stmt->fetchAll(PDO::FETCH_COLUMN);
            $business['services'] = $services;
        }
        
        // تولید پیشنهادات کلمه در صورت عدم یافتن نتیجه یا نتایجه کم
        $suggestions = [];
        if ($totalCount < 3 && !empty($query)) {
            $suggestions = generateWordSuggestions($query, $pdo);
        }
        
        return [
            'success' => true,
            'data' => $businesses,
            'pagination' => [
                'current_page' => $page,
                'per_page' => $limit,
                'total' => $totalCount,
                'total_pages' => ceil($totalCount / $limit),
                'has_next' => $page < ceil($totalCount / $limit),
                'has_prev' => $page > 1
            ],
            'filters' => [
                'query' => $query,
                'category' => $category,
                'province' => $province,
                'city' => $city,
                'sort' => $sortBy
            ],
            'search_term' => $query,
            'suggestions' => $suggestions,
            'execution_time' => round((microtime(true) - $startTime) * 1000, 2) . 'ms'
        ];
        
    } catch (PDOException $e) {
        error_log("Search Error: " . $e->getMessage());
        // در صورت خطا، از داده‌های نمونه استفاده کن
        return searchWithSampleData($params);
    }
}

// تابع تولید پیشنهادات کلمه هوشمند
function generateWordSuggestions($query, $pdo) {
    $suggestions = [];
    
    try {
        // جستجوی کلمات مشابه در نام کسب‌وکارها
        $similar_sql = "SELECT DISTINCT b.name, b.business_type, b.description
                        FROM businesses b 
                        WHERE b.is_active = 1 
                        AND (
                            b.name LIKE :fuzzy_query OR 
                            b.business_type LIKE :fuzzy_query OR
                            b.description LIKE :fuzzy_query
                        )
                        LIMIT 10";
        
        $fuzzy_param = '%' . substr($query, 0, -1) . '%'; // حذف آخرین حرف برای جستجوی فازی
        
        $stmt = $pdo->prepare($similar_sql);
        $stmt->execute([':fuzzy_query' => $fuzzy_param]);
        $similar_results = $stmt->fetchAll();
        
        // استخراج کلمات کلیدی
        $keywords = [];
        foreach ($similar_results as $result) {
            $text = $result['name'] . ' ' . $result['business_type'] . ' ' . $result['description'];
            $words = preg_split('/\s+/', $text);
            
            foreach ($words as $word) {
                $word = trim($word);
                if (strlen($word) > 2 && !in_array($word, $keywords)) {
                    // محاسبه شباهت با کلمه جستجو شده
                    $similarity = calculateSimilarity($query, $word);
                    if ($similarity > 0.3) { // حداقل 30% شباهت
                        $keywords[] = $word;
                    }
                }
            }
        }
        
        // پیشنهادات رایج
        $common_suggestions = [
            'آرایشگاه' => ['سالن زیبایی', 'آرایش و پیرایش', 'زیبایی'],
            'رستوران' => ['غذا', 'فست فود', 'کافه'],
            'کلینیک' => ['پزشکی', 'درمانگاه', 'دندانپزشکی'],
            'فروشگاه' => ['مغازه', 'خرید', 'لوازم']
        ];
        
        // اضافه کردن پیشنهادات رایج
        foreach ($common_suggestions as $key => $values) {
            if (stripos($key, $query) !== false || stripos($query, $key) !== false) {
                $suggestions = array_merge($suggestions, $values);
            }
        }
        
        // ترکیب و تمیز کردن پیشنهادات
        $suggestions = array_merge($suggestions, array_slice($keywords, 0, 5));
        $suggestions = array_unique($suggestions);
        $suggestions = array_slice($suggestions, 0, 8); // حداکثر 8 پیشنهاد
        
    } catch (Exception $e) {
        error_log("Suggestions Error: " . $e->getMessage());
    }
    
    return $suggestions;
}

// تابع محاسبه شباهت کلمات
function calculateSimilarity($str1, $str2) {
    $str1 = strtolower($str1);
    $str2 = strtolower($str2);
    
    // محاسبه فاصله Levenshtein
    $distance = levenshtein($str1, $str2);
    $maxLength = max(strlen($str1), strlen($str2));
    
    if ($maxLength == 0) return 1;
    
    return 1 - ($distance / $maxLength);
}

// تابع جستجو با داده‌های نمونه (fallback)
function searchWithSampleData($params) {
    $startTime = microtime(true);
    
    // پارامترهای جستجو
    $query = isset($params['query']) ? trim($params['query']) : '';
    $category = isset($params['category']) ? trim($params['category']) : '';
    $province = isset($params['province']) ? trim($params['province']) : '';
    $city = isset($params['city']) ? trim($params['city']) : '';
    $sortBy = isset($params['sort']) ? $params['sort'] : 'relevance';
    $page = isset($params['page']) ? max(1, intval($params['page'])) : 1;
    $limit = isset($params['limit']) ? min(50, max(1, intval($params['limit']))) : 20;
    
    // داده‌های نمونه برای تست
    $sampleBusinesses = [
        [
            'id' => 1,
            'name' => 'رستوران سنتی پارس',
            'business_type' => 'رستوران',
            'description' => 'رستوران سنتی با غذاهای محلی و فضای دنج',
            'address' => 'خیابان ولیعصر، پلاک 123',
            'phone' => '021-12345678',
            'city' => 'تهران',
            'province' => 'تهران',
            'created_at' => '2024-01-15 10:30:00',
            'owner_name' => 'احمد محمدی',
            'services_count' => 3,
            'services' => ['غذای ایرانی', 'کباب', 'خورشت']
        ],
        [
            'id' => 2,
            'name' => 'آرایشگاه زیبا',
            'business_type' => 'آرایشگاه',
            'description' => 'آرایشگاه زنانه با خدمات کامل زیبایی',
            'address' => 'میدان انقلاب، خیابان کریمخان',
            'phone' => '021-87654321',
            'city' => 'تهران',
            'province' => 'تهران',
            'created_at' => '2024-02-10 14:20:00',
            'owner_name' => 'فاطمه احمدی',
            'services_count' => 4,
            'services' => ['کوتاهی مو', 'رنگ مو', 'فر مو', 'آرایش']
        ],
        [
            'id' => 3,
            'name' => 'کلینیک دندانپزشکی نور',
            'business_type' => 'کلینیک',
            'description' => 'کلینیک دندانپزشکی با تجهیزات مدرن',
            'address' => 'خیابان شریعتی، نرسیده به پل صدر',
            'phone' => '021-55667788',
            'city' => 'تهران',
            'province' => 'تهران',
            'created_at' => '2024-01-20 09:15:00',
            'owner_name' => 'دکتر علی رضایی',
            'services_count' => 5,
            'services' => ['جرم‌گیری', 'عصب‌کشی', 'کامپوزیت', 'ایمپلنت', 'ارتودنسی']
        ]
    ];
    
    // فیلتر کردن نتایج با پشتیبانی از جستجوی چندکلمه‌ای
    $filteredBusinesses = array_filter($sampleBusinesses, function($business) use ($query, $category, $province, $city) {
        $matchQuery = empty($query);
        
        if (!empty($query)) {
            $words = preg_split('/\s+/', $query);
            $matchQuery = true;
            
            foreach ($words as $word) {
                $word = trim($word);
                if (!empty($word)) {
                    $wordMatch = stripos($business['name'], $word) !== false || 
                                stripos($business['business_type'], $word) !== false || 
                                stripos($business['description'], $word) !== false ||
                                stripos($business['address'], $word) !== false;
                    
                    if (!$wordMatch) {
                        $matchQuery = false;
                        break;
                    }
                }
            }
        }
            
        $matchCategory = empty($category) || $business['business_type'] === $category;
        $matchProvince = empty($province) || $business['province'] === $province;
        $matchCity = empty($city) || $business['city'] === $city;
        
        return $matchQuery && $matchCategory && $matchProvince && $matchCity;
    });
    
    // مرتب‌سازی
    switch ($sortBy) {
        case 'name':
            usort($filteredBusinesses, function($a, $b) {
                return strcmp($a['name'], $b['name']);
            });
            break;
        case 'newest':
            usort($filteredBusinesses, function($a, $b) {
                return strtotime($b['created_at']) - strtotime($a['created_at']);
            });
            break;
        case 'oldest':
            usort($filteredBusinesses, function($a, $b) {
                return strtotime($a['created_at']) - strtotime($b['created_at']);
            });
            break;
        case 'services':
            usort($filteredBusinesses, function($a, $b) {
                return $b['services_count'] - $a['services_count'];
            });
            break;
    }
    
    // صفحه‌بندی
    $totalCount = count($filteredBusinesses);
    $offset = ($page - 1) * $limit;
    $businesses = array_slice($filteredBusinesses, $offset, $limit);
    
    // تولید پیشنهادات برای داده‌های نمونه
    $suggestions = [];
    if ($totalCount < 3 && !empty($query)) {
        $suggestions = ['رستوران', 'آرایشگاه', 'کلینیک', 'دندانپزشکی', 'زیبایی'];
    }
    
    return [
        'success' => true,
        'data' => $businesses,
        'pagination' => [
            'current_page' => $page,
            'per_page' => $limit,
            'total' => $totalCount,
            'total_pages' => ceil($totalCount / $limit),
            'has_next' => $page < ceil($totalCount / $limit),
            'has_prev' => $page > 1
        ],
        'filters' => [
            'query' => $query,
            'category' => $category,
            'province' => $province,
            'city' => $city,
            'sort' => $sortBy
        ],
        'search_term' => $query,
        'suggestions' => $suggestions,
        'execution_time' => round((microtime(true) - $startTime) * 1000, 2) . 'ms'
    ];
}

// تابع دریافت فیلترهای موجود
function getAvailableFilters() {
    global $pdo;
    
    if (!$pdo) {
        return [
            'success' => true,
            'filters' => [
                'provinces' => [
                    ['province' => 'تهران', 'count' => 5],
                    ['province' => 'اصفهان', 'count' => 2]
                ],
                'categories' => [
                    ['business_type' => 'رستوران', 'count' => 2],
                    ['business_type' => 'آرایشگاه', 'count' => 2],
                    ['business_type' => 'کلینیک', 'count' => 2]
                ]
            ]
        ];
    }
    
    try {
        $provinces_sql = "SELECT province, COUNT(*) as count 
                          FROM businesses 
                          WHERE is_active = 1 AND province IS NOT NULL AND province != '' 
                          GROUP BY province 
                          ORDER BY count DESC, province ASC";
        
        $provinces_stmt = $pdo->prepare($provinces_sql);
        $provinces_stmt->execute();
        $provinces = $provinces_stmt->fetchAll();
        
        $categories_sql = "SELECT business_type, COUNT(*) as count 
                           FROM businesses 
                           WHERE is_active = 1 AND business_type IS NOT NULL AND business_type != '' 
                           GROUP BY business_type 
                           ORDER BY count DESC, business_type ASC";
        
        $categories_stmt = $pdo->prepare($categories_sql);
        $categories_stmt->execute();
        $categories = $categories_stmt->fetchAll();
        
        return [
            'success' => true,
            'filters' => [
                'provinces' => $provinces,
                'categories' => $categories
            ]
        ];
        
    } catch (PDOException $e) {
        error_log("Filters Error: " . $e->getMessage());
        return [
            'success' => false,
            'error' => 'خطا در دریافت فیلترها: ' . $e->getMessage(),
            'filters' => []
        ];
    }
}

// تابع دریافت شهرهای یک استان
function getCitiesByProvince($province) {
    global $pdo;
    
    if (!$pdo) {
        $cities_data = [
            'تهران' => [['city' => 'تهران', 'count' => 5]],
            'اصفهان' => [['city' => 'اصفهان', 'count' => 2]]
        ];
        
        return [
            'success' => true,
            'cities' => $cities_data[$province] ?? []
        ];
    }
    
    if (empty($province)) {
        return [
            'success' => false,
            'error' => 'استان مشخص نشده',
            'cities' => []
        ];
    }
    
    try {
        $sql = "SELECT city, COUNT(*) as count 
                FROM businesses 
                WHERE is_active = 1 AND province = :province AND city IS NOT NULL AND city != '' 
                GROUP BY city 
                ORDER BY count DESC, city ASC";
        
        $stmt = $pdo->prepare($sql);
        $stmt->execute([':province' => $province]);
        $cities = $stmt->fetchAll();
        
        return [
            'success' => true,
            'cities' => $cities
        ];
        
    } catch (PDOException $e) {
        error_log("Cities Error: " . $e->getMessage());
        return [
            'success' => false,
            'error' => 'خطا در دریافت شهرها: ' . $e->getMessage(),
            'cities' => []
        ];
    }
}

// تابع دریافت جزئیات کامل کسب‌وکار
function getBusinessDetails($businessId) {
    global $pdo;
    
    // بررسی اتصال دیتابیس
    if (!$pdo) {
        // در صورت عدم اتصال، از داده‌های نمونه استفاده کن
        return getBusinessDetailsFromSampleData($businessId);
    }
    
    try {
        // دریافت اطلاعات کسب‌وکار
        $stmt = $pdo->prepare("
            SELECT * FROM businesses 
            WHERE id = :business_id
        ");
        $stmt->bindParam(':business_id', $businessId, PDO::PARAM_INT);
        $stmt->execute();
        $business = $stmt->fetch();
        
        if (!$business) {
            return ['success' => false, 'error' => 'کسب‌وکار یافت نشد'];
        }
        
        // دریافت خدمات کسب‌وکار
        $stmt = $pdo->prepare("
            SELECT * FROM services 
            WHERE business_id = :business_id 
            ORDER BY name
        ");
        $stmt->bindParam(':business_id', $businessId, PDO::PARAM_INT);
        $stmt->execute();
        $services = $stmt->fetchAll();
        
        $business['services'] = $services;
        
        return [
            'success' => true,
            'business' => $business
        ];
        
    } catch (PDOException $e) {
        error_log("Business Details Error: " . $e->getMessage());
        return [
            'success' => false,
            'error' => 'خطا در دریافت اطلاعات کسب‌وکار: ' . $e->getMessage()
        ];
    }
}

// تابع دریافت جزئیات کسب‌وکار از داده‌های نمونه
function getBusinessDetailsFromSampleData($businessId) {
    $sampleBusinesses = [
        1 => [
            'id' => 1,
            'name' => 'رستوران سنتی پارس',
            'business_type' => 'رستوران',
            'description' => 'رستوران سنتی با غذاهای اصیل ایرانی و فضای دنج و صمیمی. ما بهترین غذاهای محلی را با کیفیت بالا و قیمت مناسب ارائه می‌دهیم.',
            'address' => 'تهران، خیابان ولیعصر، پلاک 123',
            'phone' => '021-88776655',
            'city' => 'تهران',
            'province' => 'تهران',
            'owner_name' => 'احمد محمدی',
            'services' => [
                [
                    'id' => 1,
                    'name' => 'غذای چلو کباب',
                    'description' => 'چلو کباب کوبیده با برنج ایرانی',
                    'price' => 150000,
                    'duration_minutes' => 30
                ],
                [
                    'id' => 2,
                    'name' => 'خورش قیمه',
                    'description' => 'خورش قیمه سنتی با گوشت تازه',
                    'price' => 120000,
                    'duration_minutes' => 25
                ]
            ]
        ],
        2 => [
            'id' => 2,
            'name' => 'آرایشگاه زیبایی نگین',
            'business_type' => 'آرایشگاه',
            'description' => 'آرایشگاه زنانه با خدمات کامل زیبایی، کوتاهی مو، رنگ و مش، و خدمات پوست و مو.',
            'address' => 'تهران، میدان تجریش، خیابان شریعتی',
            'phone' => '021-22334455',
            'city' => 'تهران',
            'province' => 'تهران',
            'owner_name' => 'مریم احمدی',
            'services' => [
                [
                    'id' => 3,
                    'name' => 'کوتاهی مو',
                    'description' => 'کوتاهی و اصلاح مو با جدیدترین مدل‌ها',
                    'price' => 80000,
                    'duration_minutes' => 45
                ],
                [
                    'id' => 4,
                    'name' => 'رنگ مو',
                    'description' => 'رنگ مو با رنگ‌های طبیعی و مقاوم',
                    'price' => 200000,
                    'duration_minutes' => 120
                ]
            ]
        ],
        3 => [
            'id' => 3,
            'name' => 'کلینیک دندانپزشکی دکتر احمدی',
            'business_type' => 'دندانپزشکی',
            'description' => 'کلینیک دندانپزشکی مجهز با جدیدترین تجهیزات و خدمات کامل دندانپزشکی.',
            'address' => 'اصفهان، خیابان چهارباغ، پلاک 45',
            'phone' => '031-33445566',
            'city' => 'اصفهان',
            'province' => 'اصفهان',
            'owner_name' => 'دکتر علی احمدی',
            'services' => [
                [
                    'id' => 5,
                    'name' => 'معاینه و مشاوره',
                    'description' => 'معاینه کامل دهان و دندان',
                    'price' => 50000,
                    'duration_minutes' => 30
                ],
                [
                    'id' => 6,
                    'name' => 'پر کردن دندان',
                    'description' => 'پر کردن دندان با مواد کامپوزیت',
                    'price' => 150000,
                    'duration_minutes' => 60
                ]
            ]
        ]
    ];
    
    if (isset($sampleBusinesses[$businessId])) {
        return [
            'success' => true,
            'business' => $sampleBusinesses[$businessId]
        ];
    } else {
        return [
            'success' => false,
            'error' => 'کسب‌وکار یافت نشد'
        ];
    }
}

// مدیریت درخواست‌ها
$method = $_SERVER['REQUEST_METHOD'];
$action = isset($_GET['action']) ? $_GET['action'] : 'search';

switch ($action) {
    case 'search':
        if ($method === 'GET') {
            $result = advancedSearch($_GET);
        } elseif ($method === 'POST') {
            $input = json_decode(file_get_contents('php://input'), true);
            $result = advancedSearch($input ?: []);
        } else {
            $result = ['success' => false, 'error' => 'متد نامعتبر'];
        }
        break;

    case 'business_details':
        $businessId = isset($_GET['id']) ? intval($_GET['id']) : 0;
        if ($businessId > 0) {
            $result = getBusinessDetails($businessId);
        } else {
            $result = ['success' => false, 'error' => 'شناسه کسب‌وکار نامعتبر'];
        }
        break;

    case 'filters':
        $result = getAvailableFilters();
        break;
        
    case 'cities':
        $province = isset($_GET['province']) ? $_GET['province'] : '';
        if (!empty($province)) {
            $result = getCitiesByProvince($province);
        } else {
            $result = ['success' => false, 'error' => 'استان مشخص نشده'];
        }
        break;

    default:
        if ($method === 'GET') {
            $result = advancedSearch($_GET);
        } elseif ($method === 'POST') {
            $input = json_decode(file_get_contents('php://input'), true);
            $result = advancedSearch($input ?: []);
        } else {
            $result = ['success' => false, 'error' => 'متد نامعتبر'];
        }
        break;
}

echo json_encode($result, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
?>