<?php
// File: error_handler.php
// سیستم مدیریت خطاهای بهبود یافته برای ربات سبزتایم

require_once __DIR__ . '/debug_logger.php';

class ErrorHandler {
    
    // کدهای خطای استاندارد
    const ERROR_CODES = [
        // خطاهای احراز هویت
        'AUTH_FAILED' => 1001,
        'AUTH_REQUIRED' => 1002,
        'TOKEN_EXPIRED' => 1003,
        'INVALID_USER' => 1004,
        
        // خطاهای پایگاه داده
        'DB_CONNECTION_FAILED' => 2001,
        'DB_QUERY_FAILED' => 2002,
        'RECORD_NOT_FOUND' => 2003,
        'DUPLICATE_ENTRY' => 2004,
        
        // خطاهای کسب‌وکار
        'BUSINESS_NOT_FOUND' => 3001,
        'BUSINESS_INACTIVE' => 3002,
        'ACCESS_DENIED' => 3003,
        'INSUFFICIENT_TOKENS' => 3004,
        
        // خطاهای رزرو
        'BOOKING_CONFLICT' => 4001,
        'INVALID_TIME_SLOT' => 4002,
        'BOOKING_EXPIRED' => 4003,
        'DAILY_LIMIT_EXCEEDED' => 4004,
        
        // خطاهای ولیدیشن
        'VALIDATION_FAILED' => 5001,
        'REQUIRED_FIELD_MISSING' => 5002,
        'INVALID_FORMAT' => 5003,
        'INVALID_RANGE' => 5004,
        
        // خطاهای سیستم
        'SYSTEM_ERROR' => 6001,
        'SERVICE_UNAVAILABLE' => 6002,
        'RATE_LIMIT_EXCEEDED' => 6003,
        'MAINTENANCE_MODE' => 6004
    ];
    
    // پیام‌های خطای کاربرپسند به فارسی
    const USER_MESSAGES = [
        // خطاهای احراز هویت
        'AUTH_FAILED' => [
            'title' => '🔐 خطا در احراز هویت',
            'message' => 'احراز هویت شما ناموفق بود. لطفاً ربات را مجدداً راه‌اندازی کرده و دوباره تلاش کنید.',
            'action' => 'برای حل این مشکل، روی دکمه /start کلیک کنید.'
        ],
        'AUTH_REQUIRED' => [
            'title' => '🚫 نیاز به احراز هویت',
            'message' => 'برای استفاده از این قابلیت باید وارد حساب کاربری خود شوید.',
            'action' => 'لطفاً ابتدا وارد شوید.'
        ],
        'TOKEN_EXPIRED' => [
            'title' => '⏰ جلسه منقضی شده',
            'message' => 'جلسه شما منقضی شده است. لطفاً مجدداً وارد شوید.',
            'action' => 'روی دکمه /start کلیک کنید تا مجدداً وارد شوید.'
        ],
        'INVALID_USER' => [
            'title' => '👤 کاربر نامعتبر',
            'message' => 'اطلاعات کاربری شما نامعتبر است.',
            'action' => 'لطفاً با پشتیبانی تماس بگیرید.'
        ],
        
        // خطاهای پایگاه داده
        'DB_CONNECTION_FAILED' => [
            'title' => '🔌 خطا در اتصال',
            'message' => 'در حال حاضر امکان اتصال به سرور وجود ندارد.',
            'action' => 'لطفاً چند دقیقه دیگر مجدداً تلاش کنید.'
        ],
        'DB_QUERY_FAILED' => [
            'title' => '💾 خطا در پردازش اطلاعات',
            'message' => 'خطایی در پردازش درخواست شما رخ داد.',
            'action' => 'لطفاً دوباره تلاش کنید یا با پشتیبانی تماس بگیرید.'
        ],
        'RECORD_NOT_FOUND' => [
            'title' => '🔍 یافت نشد',
            'message' => 'اطلاعات مورد نظر یافت نشد.',
            'action' => 'لطفاً از صحت اطلاعات وارد شده اطمینان حاصل کنید.'
        ],
        'DUPLICATE_ENTRY' => [
            'title' => '📋 تکراری',
            'message' => 'این اطلاعات قبلاً ثبت شده است.',
            'action' => 'لطفاً اطلاعات متفاوتی وارد کنید.'
        ],
        
        // خطاهای کسب‌وکار
        'BUSINESS_NOT_FOUND' => [
            'title' => '🏪 کسب‌وکار یافت نشد',
            'message' => 'کسب‌وکار مورد نظر یافت نشد یا دسترسی به آن ندارید.',
            'action' => 'لطفاً از صحت اطلاعات اطمینان حاصل کنید.'
        ],
        'BUSINESS_INACTIVE' => [
            'title' => '⏸️ کسب‌وکار غیرفعال',
            'message' => 'این کسب‌وکار در حال حاضر غیرفعال است.',
            'action' => 'لطفاً با مالک کسب‌وکار تماس بگیرید.'
        ],
        'ACCESS_DENIED' => [
            'title' => '🚫 عدم دسترسی',
            'message' => 'شما مجوز دسترسی به این بخش را ندارید.',
            'action' => 'برای کسب اطلاعات بیشتر با پشتیبانی تماس بگیرید.'
        ],
        'INSUFFICIENT_TOKENS' => [
            'title' => '🪙 توکن ناکافی',
            'message' => 'توکن‌های رایگان شما کافی نیست.',
            'action' => 'برای خرید توکن بیشتر با پشتیبانی تماس بگیرید.'
        ],
        
        // خطاهای رزرو
        'BOOKING_CONFLICT' => [
            'title' => '⚠️ تداخل در رزرو',
            'message' => 'این زمان قبلاً رزرو شده است یا با رزرو دیگری تداخل دارد.',
            'action' => 'لطفاً زمان دیگری انتخاب کنید.'
        ],
        'INVALID_TIME_SLOT' => [
            'title' => '🕐 زمان نامعتبر',
            'message' => 'زمان انتخابی معتبر نیست یا در دسترس نمی‌باشد.',
            'action' => 'لطفاً از زمان‌های موجود انتخاب کنید.'
        ],
        'BOOKING_EXPIRED' => [
            'title' => '⏰ رزرو منقضی',
            'message' => 'زمان این رزرو گذشته است.',
            'action' => 'لطفاً رزرو جدیدی ایجاد کنید.'
        ],
        'DAILY_LIMIT_EXCEEDED' => [
            'title' => '📅 محدودیت روزانه',
            'message' => 'شما امروز قبلاً از این کسب‌وکار رزرو کرده‌اید.',
            'action' => 'می‌توانید رزرو قبلی را لغو کرده یا فردا مجدداً تلاش کنید.'
        ],
        
        // خطاهای ولیدیشن
        'VALIDATION_FAILED' => [
            'title' => '❌ اطلاعات نامعتبر',
            'message' => 'اطلاعات وارد شده معتبر نیست.',
            'action' => 'لطفاً اطلاعات را بررسی کرده و دوباره تلاش کنید.'
        ],
        'REQUIRED_FIELD_MISSING' => [
            'title' => '📝 فیلد الزامی',
            'message' => 'برخی از فیلدهای الزامی پر نشده است.',
            'action' => 'لطفاً تمام فیلدهای مورد نیاز را پر کنید.'
        ],
        'INVALID_FORMAT' => [
            'title' => '📋 فرمت نامعتبر',
            'message' => 'فرمت اطلاعات وارد شده صحیح نیست.',
            'action' => 'لطفاً اطلاعات را با فرمت صحیح وارد کنید.'
        ],
        'INVALID_RANGE' => [
            'title' => '📏 خارج از محدوده',
            'message' => 'مقدار وارد شده خارج از محدوده مجاز است.',
            'action' => 'لطفاً مقدار معتبری وارد کنید.'
        ],
        
        // خطاهای سیستم
        'SYSTEM_ERROR' => [
            'title' => '⚙️ خطای سیستم',
            'message' => 'خطای غیرمنتظره‌ای در سیستم رخ داد.',
            'action' => 'لطفاً چند دقیقه دیگر مجدداً تلاش کنید.'
        ],
        'SERVICE_UNAVAILABLE' => [
            'title' => '🚧 سرویس در دسترس نیست',
            'message' => 'این سرویس در حال حاضر در دسترس نمی‌باشد.',
            'action' => 'لطفاً بعداً مجدداً تلاش کنید.'
        ],
        'RATE_LIMIT_EXCEEDED' => [
            'title' => '🚦 محدودیت سرعت',
            'message' => 'تعداد درخواست‌های شما از حد مجاز بیشتر است.',
            'action' => 'لطفاً چند دقیقه صبر کرده و سپس مجدداً تلاش کنید.'
        ],
        'MAINTENANCE_MODE' => [
            'title' => '🔧 حالت تعمیر',
            'message' => 'سیستم در حال تعمیر و نگهداری است.',
            'action' => 'لطفاً بعداً مجدداً تلاش کنید.'
        ]
    ];
    
    /**
     * ارسال پاسخ خطای استاندارد
     */
    public static function sendError($errorCode, $customMessage = null, $data = [], $httpCode = null) {
        $code = self::ERROR_CODES[$errorCode] ?? 9999;
        $userMessage = self::USER_MESSAGES[$errorCode] ?? self::USER_MESSAGES['SYSTEM_ERROR'];
        
        // استفاده از پیام سفارشی در صورت وجود
        if ($customMessage) {
            $userMessage['message'] = $customMessage;
        }
        
        // تعیین کد HTTP مناسب
        if (!$httpCode) {
            $httpCode = self::getHttpCodeForError($errorCode);
        }
        
        // لاگ خطا
        debug_log("Error occurred: {$errorCode}", 'ERROR', [
            'error_code' => $code,
            'message' => $userMessage['message'],
            'data' => $data
        ]);
        
        // ارسال پاسخ JSON
        header('Content-Type: application/json; charset=utf-8');
        http_response_code($httpCode);
        
        $response = [
            'status' => 'error',
            'error_code' => $code,
            'error_type' => $errorCode,
            'title' => $userMessage['title'],
            'message' => $userMessage['message'],
            'action' => $userMessage['action'],
            'timestamp' => date('Y-m-d H:i:s')
        ];
        
        if (!empty($data)) {
            $response['data'] = $data;
        }
        
        echo json_encode($response, JSON_UNESCAPED_UNICODE);
        exit;
    }
    
    /**
     * تعیین کد HTTP مناسب برای هر نوع خطا
     */
    private static function getHttpCodeForError($errorCode) {
        $httpCodes = [
            // خطاهای احراز هویت - 401
            'AUTH_FAILED' => 401,
            'AUTH_REQUIRED' => 401,
            'TOKEN_EXPIRED' => 401,
            'INVALID_USER' => 401,
            
            // خطاهای دسترسی - 403
            'ACCESS_DENIED' => 403,
            'INSUFFICIENT_TOKENS' => 403,
            
            // خطاهای یافت نشدن - 404
            'RECORD_NOT_FOUND' => 404,
            'BUSINESS_NOT_FOUND' => 404,
            
            // خطاهای تداخل - 409
            'DUPLICATE_ENTRY' => 409,
            'BOOKING_CONFLICT' => 409,
            'DAILY_LIMIT_EXCEEDED' => 409,
            
            // خطاهای ولیدیشن - 400
            'VALIDATION_FAILED' => 400,
            'REQUIRED_FIELD_MISSING' => 400,
            'INVALID_FORMAT' => 400,
            'INVALID_RANGE' => 400,
            'INVALID_TIME_SLOT' => 400,
            'BOOKING_EXPIRED' => 400,
            
            // خطاهای سرور - 500
            'DB_CONNECTION_FAILED' => 500,
            'DB_QUERY_FAILED' => 500,
            'SYSTEM_ERROR' => 500,
            
            // خطاهای سرویس - 503
            'SERVICE_UNAVAILABLE' => 503,
            'BUSINESS_INACTIVE' => 503,
            'MAINTENANCE_MODE' => 503,
            
            // محدودیت سرعت - 429
            'RATE_LIMIT_EXCEEDED' => 429
        ];
        
        return $httpCodes[$errorCode] ?? 500;
    }
    
    /**
     * ارسال پیام خطا در تلگرام
     */
    public static function sendTelegramError($chatId, $errorCode, $customMessage = null) {
        $userMessage = self::USER_MESSAGES[$errorCode] ?? self::USER_MESSAGES['SYSTEM_ERROR'];
        
        if ($customMessage) {
            $userMessage['message'] = $customMessage;
        }
        
        $text = $userMessage['title'] . "\n\n" . 
                $userMessage['message'] . "\n\n" . 
                "💡 " . $userMessage['action'];
        
        // ارسال پیام به تلگرام (نیاز به تابع sendMessage)
        if (function_exists('sendMessage')) {
            sendMessage($chatId, $text);
        }
        
        // لاگ خطا
        debug_log("Telegram error sent: {$errorCode}", 'ERROR', [
            'chat_id' => $chatId,
            'message' => $userMessage['message']
        ]);
    }
    
    /**
     * بررسی و ولیدیشن فیلدهای الزامی
     */
    public static function validateRequiredFields($data, $requiredFields) {
        $missingFields = [];
        
        foreach ($requiredFields as $field) {
            if (!isset($data[$field]) || empty($data[$field])) {
                $missingFields[] = $field;
            }
        }
        
        if (!empty($missingFields)) {
            $fieldNames = [
                'business_id' => 'شناسه کسب‌وکار',
                'service_id' => 'شناسه سرویس',
                'booking_date' => 'تاریخ رزرو',
                'start_time' => 'زمان شروع',
                'end_time' => 'زمان پایان',
                'customer_name' => 'نام مشتری',
                'customer_phone' => 'شماره تلفن',
                'name' => 'نام',
                'phone' => 'شماره تلفن',
                'address' => 'آدرس'
            ];
            
            $persianFields = array_map(function($field) use ($fieldNames) {
                return $fieldNames[$field] ?? $field;
            }, $missingFields);
            
            $message = 'فیلدهای زیر الزامی هستند: ' . implode('، ', $persianFields);
            self::sendError('REQUIRED_FIELD_MISSING', $message);
        }
        
        return true;
    }
    
    /**
     * ولیدیشن شماره تلفن
     */
    public static function validatePhoneNumber($phone) {
        if (!preg_match('/^09\d{9}$/', $phone)) {
            self::sendError('INVALID_FORMAT', 'شماره تلفن باید با 09 شروع شده و 11 رقم باشد.');
        }
        return true;
    }
    
    /**
     * ولیدیشن محدوده زمانی
     */
    public static function validateTimeRange($startTime, $endTime, $minDuration = 5, $maxDuration = 240) {
        $start = strtotime($startTime);
        $end = strtotime($endTime);
        
        if ($end <= $start) {
            self::sendError('INVALID_RANGE', 'زمان پایان باید بعد از زمان شروع باشد.');
        }
        
        $duration = ($end - $start) / 60; // به دقیقه
        
        if ($duration < $minDuration) {
            self::sendError('INVALID_RANGE', "حداقل مدت زمان رزرو {$minDuration} دقیقه است.");
        }
        
        if ($duration > $maxDuration) {
            self::sendError('INVALID_RANGE', "حداکثر مدت زمان رزرو {$maxDuration} دقیقه است.");
        }
        
        return true;
    }
}