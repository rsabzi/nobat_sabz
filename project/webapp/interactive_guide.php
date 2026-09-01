<?php
// File: interactive_guide.php
// سیستم راهنمای تعاملی برای کاربران ربات سبزتایم

require_once __DIR__ . '/debug_logger.php';

class InteractiveGuide {
    
    // مراحل راهنمای تعاملی
    const GUIDE_STEPS = [
        'welcome' => [
            'title' => '🎉 خوش آمدید به سبزتایم!',
            'content' => 'سبزتایم یک سیستم نوبت‌دهی هوشمند است که به شما کمک می‌کند تا کسب‌وکار خود را بهتر مدیریت کنید.',
            'actions' => [
                ['text' => '▶️ شروع راهنما', 'callback' => 'guide_business_setup'],
                ['text' => '⏭️ رد کردن راهنما', 'callback' => 'guide_skip']
            ],
            'next' => 'business_setup'
        ],
        
        'business_setup' => [
            'title' => '🏪 راه‌اندازی کسب‌وکار',
            'content' => 'برای شروع، ابتدا باید کسب‌وکار خود را ثبت کنید. این شامل نام، آدرس، شماره تلفن و سایر اطلاعات مهم است.',
            'demo_data' => [
                'name' => 'آرایشگاه زیبا',
                'address' => 'تهران، خیابان ولیعصر، پلاک 123',
                'phone' => '09123456789'
            ],
            'actions' => [
                ['text' => '➕ ثبت کسب‌وکار جدید', 'callback' => 'add_business'],
                ['text' => '📖 ادامه راهنما', 'callback' => 'guide_services_setup'],
                ['text' => '⬅️ مرحله قبل', 'callback' => 'guide_welcome']
            ],
            'next' => 'services_setup'
        ],
        
        'services_setup' => [
            'title' => '⚙️ تنظیم سرویس‌ها',
            'content' => 'حالا باید سرویس‌هایی که ارائه می‌دهید را تعریف کنید. هر سرویس شامل نام، مدت زمان و قیمت است.',
            'demo_data' => [
                'services' => [
                    ['name' => 'کوتاهی مو', 'duration' => 30, 'price' => 50000],
                    ['name' => 'رنگ مو', 'duration' => 90, 'price' => 150000],
                    ['name' => 'فر مو', 'duration' => 60, 'price' => 80000]
                ]
            ],
            'actions' => [
                ['text' => '🛠️ تنظیم سرویس‌ها', 'callback' => 'setup_services'],
                ['text' => '📖 ادامه راهنما', 'callback' => 'guide_schedule_setup'],
                ['text' => '⬅️ مرحله قبل', 'callback' => 'guide_business_setup']
            ],
            'next' => 'schedule_setup'
        ],
        
        'schedule_setup' => [
            'title' => '📅 تنظیم برنامه کاری',
            'content' => 'برای اینکه مشتریان بتوانند نوبت بگیرند، باید ساعات کاری خود را تعیین کنید.',
            'demo_data' => [
                'schedule' => [
                    'saturday' => ['start' => '09:00', 'end' => '18:00'],
                    'sunday' => ['start' => '09:00', 'end' => '18:00'],
                    'monday' => ['start' => '09:00', 'end' => '18:00'],
                    'tuesday' => ['start' => '09:00', 'end' => '18:00'],
                    'wednesday' => ['start' => '09:00', 'end' => '18:00'],
                    'thursday' => ['start' => '09:00', 'end' => '14:00'],
                    'friday' => ['closed' => true]
                ]
            ],
            'actions' => [
                ['text' => '⏰ تنظیم برنامه کاری', 'callback' => 'setup_schedule'],
                ['text' => '📖 ادامه راهنما', 'callback' => 'guide_booking_test'],
                ['text' => '⬅️ مرحله قبل', 'callback' => 'guide_services_setup']
            ],
            'next' => 'booking_test'
        ],
        
        'booking_test' => [
            'title' => '🧪 تست رزرو',
            'content' => 'حالا می‌توانید سیستم رزرو خود را تست کنید. یک رزرو آزمایشی ایجاد کنید تا مطمئن شوید همه چیز درست کار می‌کند.',
            'actions' => [
                ['text' => '🔗 دریافت لینک رزرو', 'callback' => 'get_booking_link'],
                ['text' => '📱 تست رزرو', 'callback' => 'test_booking'],
                ['text' => '📖 ادامه راهنما', 'callback' => 'guide_marketing'],
                ['text' => '⬅️ مرحله قبل', 'callback' => 'guide_schedule_setup']
            ],
            'next' => 'marketing'
        ],
        
        'marketing' => [
            'title' => '📢 بازاریابی و اشتراک‌گذاری',
            'content' => 'برای جذب مشتری، لینک رزرو خود را در شبکه‌های اجتماعی، وبسایت یا کارت ویزیت خود قرار دهید.',
            'tips' => [
                '📱 لینک را در استوری اینستاگرام قرار دهید',
                '💬 در گروه‌های تلگرام مرتبط به کسب‌وکارتان اشتراک بگذارید',
                '🌐 در وبسایت خود قرار دهید',
                '📇 در کارت ویزیت خود چاپ کنید'
            ],
            'actions' => [
                ['text' => '🔗 کپی لینک رزرو', 'callback' => 'copy_booking_link'],
                ['text' => '📊 مشاهده آمار', 'callback' => 'view_stats'],
                ['text' => '📖 ادامه راهنما', 'callback' => 'guide_advanced_features'],
                ['text' => '⬅️ مرحله قبل', 'callback' => 'guide_booking_test']
            ],
            'next' => 'advanced_features'
        ],
        
        'advanced_features' => [
            'title' => '🚀 قابلیت‌های پیشرفته',
            'content' => 'سبزتایم قابلیت‌های پیشرفته‌ای دارد که می‌تواند کسب‌وکار شما را به سطح بالاتری برساند.',
            'features' => [
                '📊 گزارش‌گیری تفصیلی',
                '💰 مدیریت مالی و درآمد',
                '📱 اعلان‌های خودکار',
                '🎯 بازاریابی هدفمند',
                '⭐ سیستم امتیازدهی',
                '🔄 رزرو مکرر'
            ],
            'actions' => [
                ['text' => '📊 مشاهده داشبورد', 'callback' => 'view_dashboard'],
                ['text' => '💎 خرید اشتراک پرو', 'callback' => 'upgrade_pro'],
                ['text' => '📖 پایان راهنما', 'callback' => 'guide_complete'],
                ['text' => '⬅️ مرحله قبل', 'callback' => 'guide_marketing']
            ],
            'next' => 'complete'
        ],
        
        'complete' => [
            'title' => '🎊 تبریک! راه‌اندازی کامل شد',
            'content' => 'شما با موفقیت سیستم نوبت‌دهی خود را راه‌اندازی کردید. حالا می‌توانید از تمام قابلیت‌های سبزتایم استفاده کنید.',
            'next_steps' => [
                '✅ کسب‌وکار شما آماده دریافت رزرو است',
                '📱 لینک رزرو را با مشتریان خود به اشتراک بگذارید',
                '📊 آمار و گزارش‌های خود را بررسی کنید',
                '💬 در صورت نیاز با پشتیبانی تماس بگیرید'
            ],
            'actions' => [
                ['text' => '🏠 بازگشت به منوی اصلی', 'callback' => 'main_menu'],
                ['text' => '📞 تماس با پشتیبانی', 'callback' => 'contact_support'],
                ['text' => '🔄 شروع مجدد راهنما', 'callback' => 'guide_welcome']
            ]
        ]
    ];
    
    // نکات مفید برای هر مرحله
    const TIPS = [
        'business_setup' => [
            '💡 نام کسب‌وکار خود را واضح و قابل فهم انتخاب کنید',
            '📍 آدرس دقیق وارد کنید تا مشتریان راحت‌تر شما را پیدا کنند',
            '📞 شماره تلفن فعال وارد کنید'
        ],
        'services_setup' => [
            '⏱️ مدت زمان واقعی هر سرویس را وارد کنید',
            '💰 قیمت‌ها را به‌روز نگه دارید',
            '📝 توضیحات مختصر برای هر سرویس اضافه کنید'
        ],
        'schedule_setup' => [
            '🕐 ساعات کاری واقعی خود را وارد کنید',
            '⏰ زمان استراحت را در نظر بگیرید',
            '📅 تعطیلات خود را مشخص کنید'
        ]
    ];
    
    /**
     * دریافت محتوای مرحله راهنما
     */
    public static function getGuideStep($step) {
        return self::GUIDE_STEPS[$step] ?? null;
    }
    
    /**
     * تولید پیام راهنمای تعاملی
     */
    public static function generateGuideMessage($step, $userData = []) {
        $guideStep = self::getGuideStep($step);
        if (!$guideStep) {
            return null;
        }
        
        $message = "🎯 " . $guideStep['title'] . "\n\n";
        $message .= $guideStep['content'] . "\n\n";
        
        // اضافه کردن داده‌های نمونه
        if (isset($guideStep['demo_data'])) {
            $message .= "📋 مثال:\n";
            foreach ($guideStep['demo_data'] as $key => $value) {
                if (is_array($value)) {
                    $message .= "• " . ucfirst($key) . ":\n";
                    foreach ($value as $item) {
                        if (is_array($item)) {
                            $message .= "  - " . implode(' | ', $item) . "\n";
                        } else {
                            $message .= "  - " . $item . "\n";
                        }
                    }
                } else {
                    $message .= "• " . ucfirst($key) . ": " . $value . "\n";
                }
            }
            $message .= "\n";
        }
        
        // اضافه کردن ویژگی‌ها
        if (isset($guideStep['features'])) {
            $message .= "🌟 قابلیت‌ها:\n";
            foreach ($guideStep['features'] as $feature) {
                $message .= $feature . "\n";
            }
            $message .= "\n";
        }
        
        // اضافه کردن نکات
        if (isset($guideStep['tips'])) {
            $message .= "💡 نکات مفید:\n";
            foreach ($guideStep['tips'] as $tip) {
                $message .= $tip . "\n";
            }
            $message .= "\n";
        }
        
        // اضافه کردن مراحل بعدی
        if (isset($guideStep['next_steps'])) {
            $message .= "🎯 مراحل بعدی:\n";
            foreach ($guideStep['next_steps'] as $nextStep) {
                $message .= $nextStep . "\n";
            }
            $message .= "\n";
        }
        
        // اضافه کردن نکات مرحله
        if (isset(self::TIPS[$step])) {
            $message .= "💡 نکات این مرحله:\n";
            foreach (self::TIPS[$step] as $tip) {
                $message .= $tip . "\n";
            }
            $message .= "\n";
        }
        
        return $message;
    }
    
    /**
     * تولید کیبورد تعاملی
     */
    public static function generateGuideKeyboard($step) {
        $guideStep = self::getGuideStep($step);
        if (!$guideStep || !isset($guideStep['actions'])) {
            return null;
        }
        
        $keyboard = [];
        foreach ($guideStep['actions'] as $action) {
            $keyboard[] = [
                [
                    'text' => $action['text'],
                    'callback_data' => $action['callback']
                ]
            ];
        }
        
        return [
            'inline_keyboard' => $keyboard
        ];
    }
    
    /**
     * ذخیره وضعیت راهنما برای کاربر
     */
    public static function saveUserGuideProgress($userId, $step, $completed = false) {
        $progressFile = __DIR__ . "/cache/guide_progress_{$userId}.json";
        
        $progress = [
            'user_id' => $userId,
            'current_step' => $step,
            'completed' => $completed,
            'timestamp' => time(),
            'steps_completed' => []
        ];
        
        // بارگذاری پیشرفت قبلی
        if (file_exists($progressFile)) {
            $existingProgress = json_decode(file_get_contents($progressFile), true);
            if ($existingProgress) {
                $progress['steps_completed'] = $existingProgress['steps_completed'] ?? [];
            }
        }
        
        // اضافه کردن مرحله فعلی به مراحل تکمیل شده
        if (!in_array($step, $progress['steps_completed'])) {
            $progress['steps_completed'][] = $step;
        }
        
        // ایجاد پوشه cache در صورت عدم وجود
        $cacheDir = dirname($progressFile);
        if (!is_dir($cacheDir)) {
            mkdir($cacheDir, 0755, true);
        }
        
        file_put_contents($progressFile, json_encode($progress, JSON_UNESCAPED_UNICODE));
        
        debug_log("Guide progress saved for user {$userId}", 'INFO', [
            'step' => $step,
            'completed' => $completed
        ]);
    }
    
    /**
     * دریافت وضعیت راهنما برای کاربر
     */
    public static function getUserGuideProgress($userId) {
        $progressFile = __DIR__ . "/cache/guide_progress_{$userId}.json";
        
        if (!file_exists($progressFile)) {
            return null;
        }
        
        $progress = json_decode(file_get_contents($progressFile), true);
        return $progress;
    }
    
    /**
     * بررسی اینکه آیا کاربر راهنما را تکمیل کرده یا نه
     */
    public static function isGuideCompleted($userId) {
        $progress = self::getUserGuideProgress($userId);
        return $progress && ($progress['completed'] ?? false);
    }
    
    /**
     * ریست کردن راهنما برای کاربر
     */
    public static function resetUserGuide($userId) {
        $progressFile = __DIR__ . "/cache/guide_progress_{$userId}.json";
        
        if (file_exists($progressFile)) {
            unlink($progressFile);
        }
        
        debug_log("Guide reset for user {$userId}", 'INFO');
    }
    
    /**
     * دریافت آمار کلی راهنما
     */
    public static function getGuideStats() {
        $cacheDir = __DIR__ . '/cache';
        $stats = [
            'total_users' => 0,
            'completed_users' => 0,
            'active_users' => 0,
            'step_stats' => []
        ];
        
        if (!is_dir($cacheDir)) {
            return $stats;
        }
        
        $files = glob($cacheDir . '/guide_progress_*.json');
        
        foreach ($files as $file) {
            $progress = json_decode(file_get_contents($file), true);
            if (!$progress) continue;
            
            $stats['total_users']++;
            
            if ($progress['completed'] ?? false) {
                $stats['completed_users']++;
            }
            
            // کاربران فعال (آخرین فعالیت کمتر از 7 روز)
            if ((time() - ($progress['timestamp'] ?? 0)) < (7 * 24 * 60 * 60)) {
                $stats['active_users']++;
            }
            
            // آمار مراحل
            $currentStep = $progress['current_step'] ?? 'unknown';
            $stats['step_stats'][$currentStep] = ($stats['step_stats'][$currentStep] ?? 0) + 1;
        }
        
        return $stats;
    }
}