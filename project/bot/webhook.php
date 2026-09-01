<?php
// File: /nobatsabz/bot/webhook.php
// (نسخه ۲.۲ با دکمه مدیریت زمان‌بندی)

require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../webapp/debug_logger.php';
require_once __DIR__ . '/../webapp/woocommerce_config.php';
require_once __DIR__ . '/../webapp/interactive_guide.php';

// =================================================================
// بخش ۱: توابع کمکی (Helper Functions)
// =================================================================

function apiRequest($method, $data = []) {
    // ثبت لاگ درخواست به API تلگرام
    if (function_exists('debug_log')) {
        debug_log('Telegram API request', 'INFO', [
            'method' => $method,
            'data' => $data
        ]);
    }

    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, BOT_API_URL . $method);
    curl_setopt($ch, CURLOPT_POST, 1);
    curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query($data));
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    $result = curl_exec($ch);
    $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curl_error = curl_error($ch);
    curl_close($ch);

    $decoded = json_decode($result, true);

    // ثبت لاگ پاسخ API تلگرام
    if (function_exists('debug_log')) {
        debug_log('Telegram API response', 'INFO', [
            'method' => $method,
            'http_code' => $http_code,
            'error' => $curl_error ?: null,
            'response' => $decoded ?: $result
        ]);
    }

    if ($curl_error) {
        error_log('Curl error: ' . $curl_error);
    }

    return $decoded;
}

function sendMessage($chat_id, $text, $reply_markup = null) {
    $data = ['chat_id' => $chat_id, 'text' => $text, 'parse_mode' => 'HTML'];
    if ($reply_markup) {
        $data['reply_markup'] = json_encode($reply_markup, JSON_UNESCAPED_UNICODE);
    }
    apiRequest('sendMessage', $data);
}

function editMessageText($chat_id, $message_id, $text, $reply_markup = null) {
    $data = ['chat_id' => $chat_id, 'message_id' => $message_id, 'text' => $text, 'parse_mode' => 'HTML'];
    if ($reply_markup) {
        $data['reply_markup'] = json_encode($reply_markup, JSON_UNESCAPED_UNICODE);
    }
    apiRequest('editMessageText', $data);
}

// کیبورد دائمی چسبیده به پایین صفحه (قابل باز/بسته‌شدن با آیکون کیبورد کنار پیام‌رسان تلگرام)
// این یک ReplyKeyboardMarkup جداست و مستقل از دکمه‌های اینلاین فعلی کار می‌کند
function persistentMainMenuKeyboard() {
    return [
        'keyboard' => [
            [['text' => '🏠 منوی اصلی']]
        ],
        'resize_keyboard' => true
    ];
}

function answerCallbackQuery($callback_query_id, $text = '', $show_alert = false) {
    $data = ['callback_query_id' => $callback_query_id];
    if ($text) {
        $data['text'] = $text;
    }
    if ($show_alert) {
        $data['show_alert'] = true;
    }
    apiRequest('answerCallbackQuery', $data);
}

// تنظیم دستورات ربات برای نمایش دائمی در منو
function setBotCommands() {
    $commands = [
        [
            'command' => 'start',
            'description' => '🏠 شروع و منوی اصلی'
        ],
        [
            'command' => 'help',
            'description' => '❓ راهنما و پشتیبانی'
        ],
        [
            'command' => 'search',
            'description' => '🔍 جستجوی سریع کسب‌وکار'
        ]
    ];
    
    $data = [
        'commands' => json_encode($commands, JSON_UNESCAPED_UNICODE)
    ];
    
    $result = apiRequest('setMyCommands', $data);
    
    if (function_exists('debug_log')) {
        debug_log('Bot commands setup', 'INFO', [
            'commands' => $commands,
            'result' => $result
        ]);
    }
    
    return $result;
}

// بررسی عضویت کاربر در کانال اجباری
function isUserSubscribed($user_id) {
    // تلاش با شناسه عددی کانال
    if (defined('REQUIRED_CHANNEL_ID') && REQUIRED_CHANNEL_ID) {
        $res = apiRequest('getChatMember', [
            'chat_id' => REQUIRED_CHANNEL_ID,
            'user_id' => $user_id
        ]);
        if ($res && isset($res['ok']) && $res['ok'] === true) {
            $status = $res['result']['status'] ?? 'left';
            return !in_array($status, ['left', 'kicked']);
        }
    }
    // تلاش با نام کاربری کانال
    if (defined('REQUIRED_CHANNEL_USERNAME') && REQUIRED_CHANNEL_USERNAME) {
        $chat = '@' . REQUIRED_CHANNEL_USERNAME;
        $res2 = apiRequest('getChatMember', [
            'chat_id' => $chat,
            'user_id' => $user_id
        ]);
        if ($res2 && isset($res2['ok']) && $res2['ok'] === true) {
            $status = $res2['result']['status'] ?? 'left';
            return !in_array($status, ['left', 'kicked']);
        }
    }
    return false;
}

// ذخیره پارامترهای referral موقت
function saveReferralParams($user_id, $params) {
    $referral_file = __DIR__ . '/../webapp/cache/referral_params.json';
    $referral_data = [];
    if (file_exists($referral_file)) {
        $referral_data = json_decode(file_get_contents($referral_file), true) ?: [];
    }
    
    $referral_data[$user_id] = [
        'params' => $params,
        'timestamp' => time()
    ];
    
    $cache_dir = dirname($referral_file);
    if (!is_dir($cache_dir)) {
        mkdir($cache_dir, 0755, true);
    }
    
    file_put_contents($referral_file, json_encode($referral_data));
}

// بازیابی پارامترهای referral موقت
function getReferralParams($user_id) {
    $referral_file = __DIR__ . '/../webapp/cache/referral_params.json';
    if (!file_exists($referral_file)) {
        return null;
    }
    
    $referral_data = json_decode(file_get_contents($referral_file), true) ?: [];
    
    if (isset($referral_data[$user_id])) {
        $data = $referral_data[$user_id];
        
        // بررسی انقضای داده (1 ساعت)
        if (time() - $data['timestamp'] > 3600) {
            unset($referral_data[$user_id]);
            file_put_contents($referral_file, json_encode($referral_data));
            return null;
        }
        
        // حذف داده پس از استفاده
        unset($referral_data[$user_id]);
        file_put_contents($referral_file, json_encode($referral_data));
        
        return $data['params'];
    }
    
    return null;
}

// ارسال پیام راهنمای عضویت به کاربر
function sendJoinChannelMessage($chat_id, $referral_params = null) {
    // اگر پارامتر referral وجود دارد، آن را ذخیره کن
    if ($referral_params) {
        saveReferralParams($chat_id, $referral_params);
    }
    
    $channel_link = defined('REQUIRED_CHANNEL_USERNAME') && REQUIRED_CHANNEL_USERNAME
        ? 'https://t.me/' . REQUIRED_CHANNEL_USERNAME
        : 'کانال';
    $text = "🔒 <b>یک قدم تا شروع</b>\n\n";
    $text .= "برای استفاده از ربات لطفاً اول عضو کانال ما شو، بعد دکمهٔ «بررسی عضویت» رو بزن 👇";
    $keyboard = [
        'inline_keyboard' => [
            [['text' => '📢 عضویت در کانال', 'url' => $channel_link]],
            [['text' => '✅ بررسی عضویت', 'callback_data' => 'verify_subscription', 'style' => 'success']]
        ]
    ];
    sendMessage($chat_id, $text, $keyboard);
}

// تابع تعیین سطح کاربر بر اساس امتیاز
function getUserLevel($credit_score) {
    if ($credit_score >= 50) {
        return ['level' => 'الماسی', 'icon' => '💎', 'color' => '#00FFFF'];
    } elseif ($credit_score >= 30) {
        return ['level' => 'طلایی', 'icon' => '🥇', 'color' => '#FFD700'];
    } elseif ($credit_score >= 15) {
        return ['level' => 'نقره‌ای', 'icon' => '🥈', 'color' => '#C0C0C0'];
    } elseif ($credit_score >= 5) {
        return ['level' => 'برنزی', 'icon' => '🥉', 'color' => '#CD7F32'];
    } else {
        return ['level' => 'مبتدی', 'icon' => '🔰', 'color' => '#808080'];
    }
}

function showBusinessBookings($chat_id, $business_id, $business_name, $pdo, $show_all = false, $user_telegram_id = null) {
    try {
        // اجرای حذف خودکار رزروهای منقضی شده
        cleanupExpiredBookings($pdo);

        // دریافت رزروهای کسب‌وکار (همه رزروها یا فقط رزروهای اخیر)
        // برای نمایش بهتر، حداکثر 50 رزرو را نمایش می‌دهیم
        $limit = $show_all ? "LIMIT 50" : "LIMIT 10";
        $dateFilter = $show_all ? "" : "AND b.booking_date >= CURDATE()";

        // اگر user_telegram_id ارسال شده باشد و show_all فعال نباشد، فقط رزروهای آن کاربر را نمایش می‌دهیم
        $userFilter = "";
        $params = [$business_id];
        if ($user_telegram_id && !$show_all) {
            $userFilter = "AND u.telegram_id = ?";
            $params[] = $user_telegram_id;
        }

        // اضافه کردن وضعیت حضور از جدول booking_attendance و امتیاز مشتری از جدول customer_credits
        $bookingsStmt = $pdo->prepare("
            SELECT b.*, u.first_name, u.phone as user_phone, u.telegram_id, ba.attendance_status, 
                   COALESCE(cc.current_credit, 0) as customer_credit
            FROM bookings b
            LEFT JOIN users u ON b.user_id = u.id
            LEFT JOIN booking_attendance ba ON ba.booking_id = b.id
            LEFT JOIN customer_credits cc ON cc.user_id = u.id
            WHERE b.business_id = ? {$userFilter} {$dateFilter}
            ORDER BY b.booking_date DESC, b.start_time DESC
            {$limit}
        ");
        $bookingsStmt->execute($params);
        $bookings = $bookingsStmt->fetchAll();

        // شمارش تعداد فعلی رزروها برای نمایش تعداد (فقط رزروهای فعال)
        $totalStmt = $pdo->prepare("
            SELECT COUNT(*) as total
            FROM bookings b
            WHERE b.business_id = ? AND b.status != 'cancelled'
        ");
        $totalStmt->execute([$business_id]);
        $totalBookings = $totalStmt->fetchColumn();

        if (empty($bookings)) {
            $text = "📭 <b>هنوز نوبتی ثبت نشده</b>\n\n";
            $text .= "برای اولین رزرو:\n";
            $text .= "💇 خدمات سالن رو تعریف کن\n";
            $text .= "🗓 ساعات کاری رو تنظیم کن\n";
            $text .= "🔗 لینک اختصاصی رو با مشتری‌ها به اشتراک بذار";
            sendMessage($chat_id, $text);
            return;
        }

        // پیام هِدِر آمار و دکمه‌های مدیریتی (بدون دکمه‌های حضور)
        $header_text = "📋 <b>نوبت‌های " . htmlspecialchars($business_name) . "</b>\n\n";
        $header_text .= "🔢 مجموع: " . $totalBookings . " نوبت  ·  نمایش: " . count($bookings) . ($show_all ? " (همه)" : " (اخیر)");

        $header_keyboard = [];
        if (!$show_all && $totalBookings > 10) {
            $header_keyboard[] = [['text' => '📊 همه نوبت‌ها (' . $totalBookings . ')', 'callback_data' => 'show_all_bookings_' . $business_id, 'style' => 'primary']];
            $header_keyboard[] = [['text' => '🔄 بروزرسانی', 'callback_data' => 'show_bookings_' . $business_id], ['text' => '📈 آمار', 'callback_data' => 'booking_stats_' . $business_id]];
            $header_keyboard[] = [['text' => '🔙 بازگشت', 'callback_data' => 'manage_business_' . $business_id, 'style' => 'danger'], ['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']];
            $header_text .= "\n\n💡 <i>۱۰ نوبت اخیر نمایش داده شده — برای مشاهدهٔ همه از دکمهٔ بالا استفاده کن</i>";
        } elseif ($show_all) {
            $header_keyboard[] = [['text' => '📋 نوبت‌های اخیر', 'callback_data' => 'show_recent_bookings_' . $business_id], ['text' => '📅 امروز', 'callback_data' => 'today_bookings_' . $business_id]];
            $header_keyboard[] = [['text' => '🔄 بروزرسانی', 'callback_data' => 'show_bookings_' . $business_id], ['text' => '📈 آمار', 'callback_data' => 'booking_stats_' . $business_id]];
            $header_keyboard[] = [['text' => '🔙 بازگشت', 'callback_data' => 'manage_business_' . $business_id, 'style' => 'danger'], ['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']];
        } else {
            $header_keyboard[] = [['text' => '🔄 بروزرسانی', 'callback_data' => 'show_bookings_' . $business_id], ['text' => '📈 آمار', 'callback_data' => 'booking_stats_' . $business_id]];
            $header_keyboard[] = [['text' => '🔙 بازگشت', 'callback_data' => 'manage_business_' . $business_id, 'style' => 'danger'], ['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']];
        }

        sendMessage($chat_id, $header_text, !empty($header_keyboard) ? ['inline_keyboard' => $header_keyboard] : null);

        // ارسال هر رزرو به‌صورت پیام جداگانه با دکمه‌های اختصاصی همان رزرو
        foreach ($bookings as $index => $booking) {
            // تبدیل تاریخ به شمسی و تعیین وضعیت رزرو
            $jalaliDate = convertToJalali($booking['booking_date']);
            $dayName = getDayName($booking['booking_date']);
            $status_icon = $booking['status'] === 'confirmed' ? '✅' : ($booking['status'] === 'cancelled' ? '❌' : '⏳');
            $status_text = $booking['status'] === 'confirmed' ? 'تأیید شده' : ($booking['status'] === 'cancelled' ? 'لغو شده' : 'در انتظار');

            // وضعیت حضور (در صورت وجود)
            $attendance_status = $booking['attendance_status'] ?? null;
            $attendance_shown_text = '';
            if ($attendance_status === 'attended') {
                $attendance_shown_text = "│ 🟢 <b>وضعیت حضور:</b> حضور ثبت شده";
            } elseif ($attendance_status === 'no_show') {
                $attendance_shown_text = "│ 🔴 <b>وضعیت حضور:</b> عدم حضور ثبت شده";
            } elseif ($attendance_status === 'cancelled') {
                $attendance_shown_text = "│ ⚪ <b>وضعیت حضور:</b> لغو شده";
            }

            // امتیاز و سطح مشتری
            $customer_credit = (int)($booking['customer_credit'] ?? 0);
            $user_level = getUserLevel($customer_credit);
            $credit_display = "│ " . $user_level['icon'] . " <b>سطح مشتری:</b> " . $user_level['level'] . " (امتیاز: " . $customer_credit . ")";

            // متن اختصاصی رزرو
            $booking_text = "💇 <b>نوبت " . ($index + 1) . "</b> " . $status_icon . " " . $status_text . "\n\n";
            $booking_text .= "📅 " . $jalaliDate . " (" . $dayName . ")  ·  ⏰ " . substr($booking['start_time'], 0, 5) . " تا " . substr($booking['end_time'], 0, 5) . "\n";
            $booking_text .= "👤 " . htmlspecialchars($booking['customer_name']) . "\n";
            $booking_text .= $credit_display . "\n";
            if (!empty($booking['customer_phone'])) {
                $booking_text .= "📞 " . htmlspecialchars($booking['customer_phone']) . "\n";
            }
            $booking_text .= "🔢 کد رزرو: " . htmlspecialchars($booking['confirmation_code']) . "\n";
            if (!empty($booking['customer_notes'])) {
                $booking_text .= "📝 " . htmlspecialchars($booking['customer_notes']) . "\n";
            }
            if ($attendance_shown_text) {
                $booking_text .= str_replace('│ ', '', $attendance_shown_text) . "\n";
            }

            // دکمه‌های اختصاصی حضور/عدم حضور برای هر رزرو
            $booking_keyboard = null;
            $can_show_attendance_buttons = (
                $booking['status'] === 'confirmed' && (
                    empty($attendance_status) || $attendance_status === 'pending'
                )
            );
            if ($can_show_attendance_buttons) {
                $booking_keyboard = [
                    'inline_keyboard' => [
                        [
                            ['text' => '✅ حاضر شد', 'callback_data' => 'mark_attended_' . $booking['id'] . '_' . $business_id, 'style' => 'success'],
                            ['text' => '❌ حاضر نشد', 'callback_data' => 'mark_no_show_' . $booking['id'] . '_' . $business_id, 'style' => 'danger']
                        ]
                    ]
                ];
            }

            // ارسال پیام رزرو با کیبورد مختص خودش
            sendMessage($chat_id, $booking_text, $booking_keyboard);
        }
    } catch (PDOException $e) {
        error_log("Error showing business bookings: " . $e->getMessage());
        sendMessage($chat_id, "❌ خطایی در دریافت رزروها رخ داد.\n\nلطفاً بعداً دوباره تلاش کنید.");
    }
}

function convertToJalali($gregorianDate) {
    $timestamp = strtotime($gregorianDate);
    $year = date('Y', $timestamp) - 621;
    $month = date('m', $timestamp);
    $day = date('d', $timestamp);
    
    $jalaliMonths = [
        1 => 'فروردین', 2 => 'اردیبهشت', 3 => 'خرداد', 4 => 'تیر',
        5 => 'مرداد', 6 => 'شهریور', 7 => 'مهر', 8 => 'آبان',
        9 => 'آذر', 10 => 'دی', 11 => 'بهمن', 12 => 'اسفند'
    ];
    
    $jalaliMonth = $jalaliMonths[$month] ?? $month;
    return "{$day} {$jalaliMonth} {$year}";
}

function getDayName($date) {
    $dayOfWeek = date('w', strtotime($date));
    $dayNames = [
        0 => 'یکشنبه', 1 => 'دوشنبه', 2 => 'سه‌شنبه', 3 => 'چهارشنبه',
        4 => 'پنج‌شنبه', 5 => 'جمعه', 6 => 'شنبه'
    ];
    return $dayNames[$dayOfWeek] ?? 'نامشخص';
}

function sendBookingReminders($pdo) {
    try {
        // دریافت رزروهایی که نیاز به یادآوری دارند
        $stmt = $pdo->prepare("
            SELECT b.*, u.telegram_id, u.first_name, u.reminder_enabled, u.reminder_minutes,
                   bus.name as business_name, bus.address as business_address
            FROM bookings b
            JOIN users u ON b.user_id = u.id
            JOIN businesses bus ON b.business_id = bus.id
            WHERE b.status = 'confirmed'
            AND u.reminder_enabled = 1
            AND b.booking_date >= CURDATE()
            AND b.reminder_sent = 0
            AND TIMESTAMPDIFF(MINUTE, NOW(), CONCAT(b.booking_date, ' ', b.start_time)) <= u.reminder_minutes
            AND TIMESTAMPDIFF(MINUTE, NOW(), CONCAT(b.booking_date, ' ', b.start_time)) > 0
        ");
        $stmt->execute();
        $bookings = $stmt->fetchAll();
        
        foreach ($bookings as $booking) {
            $reminder_text = "🔔 <b>" . htmlspecialchars($booking['first_name']) . " جان، نوبتت نزدیکه!</b>\n\n";
            $reminder_text .= "⏰ تا " . $booking['reminder_minutes'] . " دقیقهٔ دیگه نوبت داری\n\n";
            $reminder_text .= "💇 " . htmlspecialchars($booking['business_name']) . "\n";
            $reminder_text .= "📅 " . convertToJalali($booking['booking_date']) . "  ·  🕐 " . substr($booking['start_time'], 0, 5) . "\n";
            
            if (!empty($booking['business_address'])) {
                $reminder_text .= "📍 " . htmlspecialchars($booking['business_address']) . "\n";
            }
            
            if (!empty($booking['service_name'])) {
                $reminder_text .= "🛠️ " . htmlspecialchars($booking['service_name']) . "\n";
            }
            
            $reminder_text .= "\n📞 برای تغییر یا لغو، از دکمه‌های زیر استفاده کن.";
            
            $reminder_keyboard = [
                'inline_keyboard' => [
                    [['text' => '📋 جزئیات نوبت', 'callback_data' => 'booking_details_' . $booking['id'], 'style' => 'primary']],
                    [['text' => '❌ لغو نوبت', 'callback_data' => 'cancel_booking_' . $booking['id'], 'style' => 'danger'], ['text' => '📅 نوبت‌های من', 'callback_data' => 'my_customer_bookings']]
                ]
            ];
            
            // ارسال یادآوری
            sendMessage($booking['telegram_id'], $reminder_text, $reminder_keyboard);
            
            // علامت‌گذاری به عنوان ارسال شده
            $updateStmt = $pdo->prepare("UPDATE bookings SET reminder_sent = 1 WHERE id = ?");
            $updateStmt->execute([$booking['id']]);
            
            error_log("Reminder sent for booking ID: " . $booking['id'] . " to user: " . $booking['telegram_id']);
        }
        
        return count($bookings);
        
    } catch (PDOException $e) {
        error_log("Error sending reminders: " . $e->getMessage());
        return 0;
    }
}

function checkAndSendReminders() {
    global $pdo;
    if ($pdo) {
        $sent_count = sendBookingReminders($pdo);
        if ($sent_count > 0) {
            error_log("Sent " . $sent_count . " booking reminders");
        }
    }
}

// تابع حذف خودکار رزروهای منقضی شده
function cleanupExpiredBookings($pdo) {
    if (!$pdo) {
        error_log("❌ عدم دسترسی به پایگاه داده برای حذف رزروهای منقضی");
        return false;
    }
    
    try {
        // ابتدا تعداد رزروهای منقضی شده را بشمار
        $countStmt = $pdo->prepare("
            SELECT COUNT(*) as expired_count 
            FROM bookings 
            WHERE booking_date < DATE_SUB(CURDATE(), INTERVAL 1 DAY)
        ");
        $countStmt->execute();
        $expiredCount = $countStmt->fetchColumn();
        
        // اگر رزرو منقضی وجود نداشت، نیازی به ادامه نیست
        if ($expiredCount == 0) {
            return 0;
        }
        
        // حذف رزروهای منقضی شده (بیش از یک روز گذشته از تاریخ رزرو)
        $cleanupStmt = $pdo->prepare("
            DELETE FROM bookings 
            WHERE booking_date < DATE_SUB(CURDATE(), INTERVAL 1 DAY)
        ");
        $cleanupStmt->execute();
        $deletedCount = $cleanupStmt->rowCount();
        
        if ($deletedCount > 0) {
            error_log("🗑️ حذف خودکار {$deletedCount} رزرو منقضی شده انجام شد");
            
            // ثبت زمان آخرین پاکسازی در فایل
            $cacheDir = __DIR__ . '/../webapp/cache';
            if (!is_dir($cacheDir)) {
                mkdir($cacheDir, 0755, true);
            }
            file_put_contents($cacheDir . '/last_cleanup_count.txt', $deletedCount . '|' . date('Y-m-d H:i:s'));
        }
        
        return $deletedCount;
        
    } catch (PDOException $e) {
        error_log("❌ خطا در حذف رزروهای منقضی: " . $e->getMessage());
        return false;
    }
}

// تابع بررسی و اجرای حذف دوره‌ای
function periodicCleanup($pdo) {
    // بررسی آخرین زمان اجرای cleanup از فایل
    $cleanupFile = __DIR__ . '/../webapp/cache/last_cleanup.txt';
    $lastCleanup = 0;
    
    if (file_exists($cleanupFile)) {
        $lastCleanup = (int)file_get_contents($cleanupFile);
    }
    
    $currentTime = time();
    $sixHours = 6 * 60 * 60; // 6 ساعت به ثانیه
    
    // اگر بیش از 6 ساعت از آخرین cleanup گذشته باشد
    if (($currentTime - $lastCleanup) > $sixHours) {
        $result = cleanupExpiredBookings($pdo);
        
        if ($result !== false) {
            // ذخیره زمان اجرای cleanup
            $cacheDir = dirname($cleanupFile);
            if (!is_dir($cacheDir)) {
                mkdir($cacheDir, 0755, true);
            }
            file_put_contents($cleanupFile, $currentTime);
            error_log("✅ اجرای دوره‌ای cleanup انجام شد - حذف {$result} رزرو");
        }
    }
}


// =================================================================
// بخش ۲: پردازش اصلی درخواست (Main Logic)
// =================================================================

$content = file_get_contents("php://input");
debug_log('📥 ورودی خام وبهوک دریافت شد', 'INFO', [
    'raw_input' => $content,
    'content_length' => strlen($content),
    'content_type' => $_SERVER['CONTENT_TYPE'] ?? 'N/A'
]);
$update = json_decode($content, true);
if ($update === null) {
    debug_log('⚠️ JSON ورودی قابل تجزیه نیست', 'WARNING', [
        'json_last_error' => json_last_error(),
        'json_last_error_msg' => function_exists('json_last_error_msg') ? json_last_error_msg() : 'N/A'
    ]);
}

if (!$update) {
    debug_log('❌ Invalid update در وبهوک - خروجی بدون پردازش', 'ERROR', [
        'raw_input' => $content
    ]);
    http_response_code(200);
    echo json_encode(['status' => 'ignored', 'reason' => 'invalid_update']);
    exit;
}

// اجرای حذف دوره‌ای رزروهای منقضی شده
periodicCleanup($pdo);

// بررسی اطلاع‌رسانی رزرو جدید
if (isset($update['action']) && $update['action'] === 'new_booking_notification') {
    $owner_telegram_id = $update['owner_telegram_id'];
    $business_name = $update['business_name'];
    $customer_name = $update['customer_name'];
    $booking_date = $update['booking_date'];
    $start_time = $update['start_time'];
    $end_time = $update['end_time'];
    $customer_phone = $update['customer_phone'];
    $confirmation_code = $update['confirmation_code'];
    
    $notification_text = "🎉 <b>رزرو جدید!</b>\n\n";
    $notification_text .= "🏢 <b>کسب‌وکار:</b> " . htmlspecialchars($business_name) . "\n";
    $notification_text .= "👤 <b>مشتری:</b> " . htmlspecialchars($customer_name) . "\n";
    $notification_text .= "📅 <b>تاریخ:</b> " . htmlspecialchars($booking_date) . "\n";
    $notification_text .= "⏰ <b>زمان:</b> " . htmlspecialchars($start_time) . " تا " . htmlspecialchars($end_time) . "\n";
    
    if (!empty($customer_phone)) {
        $notification_text .= "📞 <b>تماس:</b> " . htmlspecialchars($customer_phone) . "\n";
    }
    
    // دریافت امتیاز اعتباری مشتری
    $customer_credit = 0;
    $credit_status = '';
    if (isset($update['customer_telegram_id'])) {
        try {
            $pdo = getBotDatabaseConnection();
            
            // ابتدا user_id را از telegram_id پیدا کن
            $user_stmt = $pdo->prepare("SELECT id FROM users WHERE telegram_id = ?");
            $user_stmt->execute([$update['customer_telegram_id']]);
            $user_result = $user_stmt->fetch(PDO::FETCH_ASSOC);
            
            if ($user_result) {
                $user_id = $user_result['id'];
                
                // حالا امتیاز را دریافت کن
                $credit_stmt = $pdo->prepare("SELECT current_credit FROM customer_credits WHERE user_id = ?");
                $credit_stmt->execute([$user_id]);
                $credit_result = $credit_stmt->fetch(PDO::FETCH_ASSOC);
                
                if ($credit_result) {
                    $customer_credit = $credit_result['current_credit'];
                    
                    // تعیین وضعیت امتیاز
                    if ($customer_credit >= 8) {
                        $credit_status = '🟢 ممتاز';
                    } elseif ($customer_credit >= 5) {
                        $credit_status = '🟡 معمولی';
                    } else {
                        $credit_status = '🔴 نیاز به بهبود';
                    }
                } else {
                    // اگر امتیاز وجود نداشت، ایجاد کن
                    $create_credit_stmt = $pdo->prepare("INSERT INTO customer_credits (user_id, current_credit, total_earned, total_deducted) VALUES (?, 10, 10, 0)");
                    $create_credit_stmt->execute([$user_id]);
                    $customer_credit = 10;
                    $credit_status = '🟡 معمولی';
                }
            }
        } catch (Exception $e) {
            error_log("Error fetching customer credit: " . $e->getMessage());
            $customer_credit = 0;
            $credit_status = '❓ نامشخص';
        }
    }
    
    $notification_text .= "🔢 <b>کد رزرو:</b> " . htmlspecialchars($confirmation_code) . "\n";
    $notification_text .= "⭐ <b>امتیاز اعتباری مشتری:</b> " . $customer_credit . " امتیاز " . $credit_status . "\n\n";
    $notification_text .= "💡 رزرو به صورت خودکار تأیید شده است.";
    
    sendMessage($owner_telegram_id, $notification_text);
    
    // پاسخ موفقیت‌آمیز
    http_response_code(200);
    echo json_encode(['status' => 'success', 'message' => 'Notification sent']);
    exit;
}

// بررسی و ارسال یادآوری‌ها
checkAndSendReminders();

if (isset($update['message'])) {
    $message = $update['message'];
    $chat_id = $message['chat']['id'];
    $text = $message['text'] ?? '';
    $user_telegram_id = $message['from']['id'];
    
    if ($pdo) {
        try {
            $stmt = $pdo->prepare(
                "INSERT INTO users (telegram_id, first_name, last_name, username) 
                 VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE 
                    first_name = VALUES(first_name), last_name = VALUES(last_name),
                    username = VALUES(username), updated_at = NOW()"
            );
            $stmt->execute([
                $user_telegram_id, $message['from']['first_name'] ?? '',
                $message['from']['last_name'] ?? '', $message['from']['username'] ?? ''
            ]);
        } catch(PDOException $e) {
            error_log("User save/update error: " . $e->getMessage());
        }
    }
    
    // بررسی وضعیت انتظار ورودی
    $waiting_states_file = __DIR__ . '/../webapp/cache/waiting_states.json';
    $waiting_states = [];
    if (file_exists($waiting_states_file)) {
        $waiting_states = json_decode(file_get_contents($waiting_states_file), true) ?: [];
    }
    
    if (isset($waiting_states[$user_telegram_id])) {
        $state = $waiting_states[$user_telegram_id];
        
        // بررسی انقضای وضعیت (30 دقیقه)
        if (time() - $state['timestamp'] > 1800) {
            unset($waiting_states[$user_telegram_id]);
            file_put_contents($waiting_states_file, json_encode($waiting_states));
        } else {
            // پردازش ورودی بر اساس نوع عمل
            if ($state['action'] === 'create_link' || $state['action'] === 'change_alias') {
                $alias = trim($text);
                
                // اعتبارسنجی نام کاربری
                if (!preg_match('/^[a-zA-Z][a-zA-Z0-9_]{2,19}$/', $alias)) {
                    $error_text = "❌ <b>نام کاربری نامعتبر!</b>\n\n";
                    $error_text .= "⚠️ <b>قوانین نام کاربری:</b>\n";
                    $error_text .= "• فقط حروف انگلیسی، اعداد و خط تیره (_)\n";
                    $error_text .= "• حداقل 3 و حداکثر 20 کاراکتر\n";
                    $error_text .= "• باید با حرف شروع شود\n\n";
                    $error_text .= "✍️ <i>لطفاً نام کاربری معتبر وارد کنید:</i>";
                    
                    $error_keyboard = [
                        'inline_keyboard' => [
                            [['text' => '❌ انصراف', 'callback_data' => 'manage_link_' . $state['business_id']]]
                        ]
                    ];
                    
                    sendMessage($chat_id, $error_text, $error_keyboard);
                    return;
                }
                
                if (!$pdo) {
                    sendMessage($chat_id, "❌ خطا در اتصال به پایگاه داده!");
                    return;
                }
                
                try {
                    // بررسی تکراری نبودن نام کاربری
                    $checkStmt = $pdo->prepare("SELECT id FROM businesses WHERE link_alias = ? AND id != ?");
                    $checkStmt->execute([$alias, $state['business_id']]);
                    
                    if ($checkStmt->fetch()) {
                        $duplicate_text = "❌ <b>نام کاربری تکراری!</b>\n\n";
                        $duplicate_text .= "🔗 نام کاربری <code>" . htmlspecialchars($alias) . "</code> قبلاً استفاده شده است.\n\n";
                        $duplicate_text .= "💡 <i>لطفاً نام کاربری دیگری انتخاب کنید:</i>";
                        
                        $duplicate_keyboard = [
                            'inline_keyboard' => [
                                [['text' => '❌ انصراف', 'callback_data' => 'manage_link_' . $state['business_id']]]
                            ]
                        ];
                        
                        sendMessage($chat_id, $duplicate_text, $duplicate_keyboard);
                        return;
                    }
                    
                    // بررسی مالکیت کسب‌وکار
                    $ownerStmt = $pdo->prepare("SELECT b.name FROM businesses b JOIN users u ON b.owner_id = u.id WHERE b.id = ? AND u.telegram_id = ?");
                    $ownerStmt->execute([$state['business_id'], $user_telegram_id]);
                    $business = $ownerStmt->fetch();
                    
                    if (!$business) {
                        sendMessage($chat_id, "❌ کسب‌وکار یافت نشد!");
                        unset($waiting_states[$user_telegram_id]);
                        file_put_contents($waiting_states_file, json_encode($waiting_states));
                        return;
                    }
                    
                    // بروزرسانی نام کاربری
                    $updateStmt = $pdo->prepare("UPDATE businesses b JOIN users u ON b.owner_id = u.id SET b.link_alias = ? WHERE b.id = ? AND u.telegram_id = ?");
                    $updateStmt->execute([$alias, $state['business_id'], $user_telegram_id]);
                    
                    if ($updateStmt->rowCount() > 0) {
                        $full_link = "https://t.me/" . BOT_USERNAME . "?start=" . $alias;
                        
                        $success_text = "✅ <b>لینک اختصاصی " . ($state['action'] === 'create_link' ? 'ایجاد' : 'بروزرسانی') . " شد!</b>\n\n";
                        $success_text .= "🔗 <b>لینک جدید:</b>\n<code>" . $full_link . "</code>\n\n";
                        $success_text .= "🎯 <b>نام کاربری:</b> " . htmlspecialchars($alias) . "\n\n";
                        $success_text .= "💡 <i>حالا مشتریان می‌توانند از این لینک مستقیماً رزرو کنند!</i>";
                        
                        $success_keyboard = [
                            'inline_keyboard' => [
                                [['text' => '📋 کپی لینک', 'callback_data' => 'copy_invitation_' . $state['business_id']]],
                                [['text' => '📤 اشتراک لینک', 'callback_data' => 'share_invitation_' . $state['business_id']]],
                                [['text' => '🔙 بازگشت', 'callback_data' => 'manage_business_' . $state['business_id'], 'style' => 'danger']]
                            ]
                        ];
                        
                        sendMessage($chat_id, $success_text, $success_keyboard);
                    } else {
                        sendMessage($chat_id, "❌ خطا در " . ($state['action'] === 'create_link' ? 'ایجاد' : 'بروزرسانی') . " لینک!");
                    }
                    
                    // حذف وضعیت انتظار
                    unset($waiting_states[$user_telegram_id]);
                    file_put_contents($waiting_states_file, json_encode($waiting_states));
                    
                } catch (PDOException $e) {
                    error_log("Error processing alias: " . $e->getMessage());
                    sendMessage($chat_id, "❌ خطا در پردازش نام کاربری!");
                    
                    // حذف وضعیت انتظار در صورت خطا
                    unset($waiting_states[$user_telegram_id]);
                    file_put_contents($waiting_states_file, json_encode($waiting_states));
                }
                
                return;
            }
        }
    }
    
    if ($text === '🏠 منوی اصلی') {
        // همون محتوای منوی اصلی (مشابه callback_data === 'main_menu') ولی به‌عنوان پیام جدید
        // چون این پیام از طریق کیبورد دائمی چسبیده به پایین صفحه ارسال شده، نه از روی دکمهٔ اینلاین
        $welcome_text = "💇‍♀️ <b>منوی اصلی</b>\n\n";
        $welcome_text .= "چیکار می‌خوای بکنی؟";
        
        $keyboard = [
            'inline_keyboard' => [
                [['text' => '💇 رزرو نوبت', 'callback_data' => 'user_type_customer', 'style' => 'success'], ['text' => '🏢 مدیریت سالن', 'callback_data' => 'user_type_business_owner']],
                [['text' => '📈 بازاریاب هستم', 'callback_data' => 'user_type_marketer']],
                [['text' => '🔍 جستجوی سریع', 'web_app' => ['url' => BASE_URL . "/webapp/search.html"]], ['text' => 'ℹ️ راهنما', 'callback_data' => 'help_general']]
            ]
        ];
        
        sendMessage($chat_id, $welcome_text, $keyboard);
        return;
    }
    
    if ($text === '/bookings') {
        // نمایش رزروهای کسب‌وکار کاربر
        if (!$pdo) {
            sendMessage($chat_id, "❌ خطایی در اتصال به پایگاه داده رخ داد.\n\nلطفاً بعداً دوباره تلاش کنید.");
            return;
        }
        
        try {
            // دریافت کاربر
            $userStmt = $pdo->prepare("SELECT id FROM users WHERE telegram_id = ?");
            $userStmt->execute([$user_telegram_id]);
            $user = $userStmt->fetch();
            
            if (!$user) {
                sendMessage($chat_id, "❌ اطلاعات کاربری شما یافت نشد.\n\nلطفاً یکبار دستور /start را ارسال کنید.");
                return;
            }
            
            // دریافت کسب‌وکارهای کاربر
            $businessStmt = $pdo->prepare("SELECT id, name FROM businesses WHERE owner_id = ? AND is_active = 1 ORDER BY name");
            $businessStmt->execute([$user['id']]);
            $businesses = $businessStmt->fetchAll();
            
            if (empty($businesses)) {
                $no_business_text = "🏢 <b>هنوز سالنی ثبت نکردی</b>\n\n";
                $no_business_text .= "ثبت سالن فقط چند دقیقه طول می‌کشه — اطلاعات، ساعات کاری و خدماتت رو وارد کن و شروع کن به گرفتن نوبت 💇";
                
                $no_business_keyboard = [
                    'inline_keyboard' => [
                        [['text' => '🆕 ثبت سالن جدید', 'web_app' => ['url' => WEBAPP_URL], 'style' => 'success']],
                        [['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']]
                    ]
                ];
                
                sendMessage($chat_id, $no_business_text, $no_business_keyboard);
                return;
            }
            
            if (count($businesses) === 1) {
                // اگر فقط یک کسب‌وکار دارد، مستقیماً رزروهای آن را نمایش بده
                showBusinessBookings($chat_id, $businesses[0]['id'], $businesses[0]['name'], $pdo);
            } else {
                // اگر چند کسب‌وکار دارد، لیست کسب‌وکارها را نمایش بده
                $text = "💇 <b>کدوم سالن؟</b>\n\nبرای دیدن نوبت‌ها، یکی رو انتخاب کن:";
                
                $keyboard_buttons = [];
                foreach ($businesses as $business) {
                    $keyboard_buttons[] = [['text' => $business['name'], 'callback_data' => 'show_bookings_' . $business['id']]];
                }
                
                $keyboard = ['inline_keyboard' => $keyboard_buttons];
                sendMessage($chat_id, $text, $keyboard);
            }
            
        } catch (PDOException $e) {
            error_log("Error in /bookings command: " . $e->getMessage());
            sendMessage($chat_id, "❌ خطایی در دریافت اطلاعات رخ داد.\n\nلطفاً بعداً دوباره تلاش کنید.");
        }
    }
    elseif (strpos($text, '/start') === 0) {
        // بررسی وجود پارامتر در دستور /start
        $parts = explode(' ', $text, 2);
        $start_param = isset($parts[1]) ? trim($parts[1]) : null;
        
        // گارد عضویت اجباری کانال
        if (!isUserSubscribed($user_telegram_id)) {
            // ذخیره پارامتر referral قبل از ارسال پیام عضویت
            sendJoinChannelMessage($chat_id, $start_param);
            return;
        }

        // ارسال یک‌بارهٔ کیبورد دائمی «🏠 منوی اصلی» چسبیده به پایین صفحه
        // (این کیبورد مستقل از دکمه‌های اینلاین فعلی است و آن‌ها را جایگزین نمی‌کند)
        sendMessage($chat_id, "👇 برای دسترسی سریع، دکمهٔ «🏠 منوی اصلی» همیشه پایین صفحه در دسترسه.", persistentMainMenuKeyboard());

        if ($start_param) {
            // کاربر از طریق لینک دعوت آمده است
            error_log("🔗 کاربر از طریق لینک دعوت آمده: " . $start_param);
            
            if (!$pdo) {
                $error_text = "❌ خطایی در اتصال به پایگاه داده رخ داد.\n\n";
                $error_text .= "لطفاً بعداً دوباره تلاش کنید.";
                sendMessage($chat_id, $error_text);
                return;
            }
            
            try {
                // بررسی اینکه آیا لینک مربوط به بازاریاب است
                if (strpos($start_param, 'marketer_') === 0) {
                    // لینک بازاریابی
                    $marketer_code = substr($start_param, strlen('marketer_'));
                    
                    // جستجوی بازاریاب بر اساس کد
                    $marketerStmt = $pdo->prepare("SELECT m.*, u.first_name FROM marketers m JOIN users u ON m.user_id = u.id WHERE m.marketer_code = ? AND m.is_active = 1");
                    $marketerStmt->execute([$marketer_code]);
                    $marketer = $marketerStmt->fetch();
                    
                    if ($marketer) {
                        // ذخیره کد بازاریاب در session یا متغیر موقت
                        // برای استفاده هنگام ثبت کسب‌وکار
                        
                        $marketer_welcome_text = "👋 <b>خوش اومدی!</b>\n\n";
                        $marketer_welcome_text .= "از طریق <b>" . htmlspecialchars($marketer['first_name']) . "</b> اومدی — عالیه، بذار سالن یا آرایشگاهت رو در چند دقیقه آنلاین کنیم 💇\n\n";
                        $marketer_welcome_text .= "با ثبت سالن می‌تونی نوبت‌ها رو راحت مدیریت کنی، مشتری بیشتری بگیری و گزارش دقیق داشته باشی.";
                        
                        $marketer_keyboard = [
                            'inline_keyboard' => [
                                [['text' => '🏢 ثبت سالن جدید', 'web_app' => ['url' => WEBAPP_URL . '&marketer=' . $marketer_code], 'style' => 'success']],
                                [['text' => '📞 مشاوره رایگان', 'callback_data' => 'marketer_consultation_' . $marketer_code]],
                                [['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']]
                            ]
                        ];
                        
                        sendMessage($chat_id, $marketer_welcome_text, $marketer_keyboard);
                        return;
                    }
                }
                
                // جستجوی کسب‌وکار بر اساس نامک لینک (کد قبلی)
                $businessStmt = $pdo->prepare("SELECT id, name, owner_id FROM businesses WHERE link_alias = ? AND is_active = 1");
                $businessStmt->execute([$start_param]);
                $business = $businessStmt->fetch();
                
                if ($business) {
                    // دریافت اطلاعات کامل کسب‌وکار و مالک
                    $detailStmt = $pdo->prepare("
                        SELECT b.*, u.first_name, u.last_name, u.phone as owner_phone 
                        FROM businesses b 
                        JOIN users u ON b.owner_id = u.id 
                        WHERE b.id = ?
                    ");
                    $detailStmt->execute([$business['id']]);
                    $businessDetail = $detailStmt->fetch();
                    
                    // دریافت خدمات کسب‌وکار
                    $servicesStmt = $pdo->prepare("
                        SELECT name, description, price, duration_minutes 
                        FROM services 
                        WHERE business_id = ? AND is_active = 1 
                        ORDER BY created_at ASC
                    ");
                    $servicesStmt->execute([$business['id']]);
                    $services = $servicesStmt->fetchAll();
                    
                    if ($businessDetail) {
                        // کسب‌وکار یافت شد - نمایش اطلاعات کامل و دکمه رزرو
                        $business_text = "🎯 <b>" . htmlspecialchars($businessDetail['name']) . "</b>\n\n";
                        $business_text .= "خوش آمدید! شما از طریق لینک دعوت این کسب‌وکار وارد شده‌اید.\n\n";
                        
                        // نمایش مشخصات کسب‌وکار
                        $business_text .= "📋 <b>مشخصات کسب‌وکار:</b>\n";
                        
                        // نام و نام خانوادگی مدیر
                        $owner_name = trim($businessDetail['first_name'] . ' ' . $businessDetail['last_name']);
                        if (!empty($owner_name)) {
                            $business_text .= "👤 <b>مدیر:</b> " . htmlspecialchars($owner_name) . "\n";
                        }
                        
                        // آدرس دقیق کسب‌وکار
                        if (!empty($businessDetail['address'])) {
                            $business_text .= "📍 <b>آدرس:</b> " . htmlspecialchars($businessDetail['address']) . "\n";
                        }
                        
                        // دسته‌بندی فعالیت
                        if (!empty($businessDetail['business_type'])) {
                            $business_text .= "🏷️ <b>دسته‌بندی:</b> " . htmlspecialchars($businessDetail['business_type']) . "\n";
                        }
                        
                        // شماره تماس مدیر
                        $contact_phone = !empty($businessDetail['owner_phone']) ? $businessDetail['owner_phone'] : $businessDetail['phone'];
                        if (!empty($contact_phone)) {
                            $business_text .= "📞 <b>تماس:</b> " . htmlspecialchars($contact_phone) . "\n";
                        }
                        
                        // نمایش خدمات ارائه شده
                        if (!empty($services)) {
                            $business_text .= "\n🛠️ <b>خدمات ارائه شده:</b>\n";
                            foreach ($services as $service) {
                                $business_text .= "\n🔹 <b>" . htmlspecialchars($service['name']) . "</b>";
                                
                                // نمایش قیمت و مدت زمان
                                $service_details = [];
                                if (!empty($service['price']) && $service['price'] > 0) {
                                    $service_details[] = number_format($service['price']) . " تومان";
                                }
                                if (!empty($service['duration_minutes'])) {
                                    $service_details[] = $service['duration_minutes'] . " دقیقه";
                                }
                                if (!empty($service_details)) {
                                    $business_text .= " (" . implode(" - ", $service_details) . ")";
                                }
                                
                                // نمایش توضیحات
                                if (!empty($service['description'])) {
                                    $business_text .= "\n   " . htmlspecialchars($service['description']);
                                }
                                $business_text .= "\n";
                            }
                        }
                        
                        $business_text .= "\n💡 برای رزرو نوبت، روی دکمه زیر کلیک کنید:";
                        
                        // ساخت لینک رزرو
                        $booking_url = BASE_URL . "/webapp/booking.html?business_id=" . $business['id'];
                        
                        $keyboard = [
                            'inline_keyboard' => [
                                [['text' => '📅 رزرو نوبت', 'web_app' => ['url' => $booking_url]]],
                                [['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']]
                            ]
                        ];
                        
                        sendMessage($chat_id, $business_text, $keyboard);
                        error_log("✅ کسب‌وکار یافت شد و پیام ارسال شد: " . $business['name']);
                    } else {
                        error_log("❌ خطا در دریافت جزئیات کسب‌وکار");
                        $error_text = "❌ خطایی در دریافت اطلاعات کسب‌وکار رخ داد.\n\n";
                        $error_text .= "لطفاً بعداً دوباره تلاش کنید.";
                        sendMessage($chat_id, $error_text);
                    }
                } else {
                    // کسب‌وکار یافت نشد
                    error_log("❌ کسب‌وکار با نامک " . $start_param . " یافت نشد");
                    
                    $error_text = "❌ متأسفانه کسب‌وکار مورد نظر یافت نشد یا غیرفعال است.\n\n";
                    $error_text .= "ممکن است لینک اشتباه باشد یا کسب‌وکار حذف شده باشد.\n\n";
                    $error_text .= "👇 می‌توانید از منوی اصلی استفاده کنید:";
                    
                    $keyboard = [
                        'inline_keyboard' => [
                            [['text' => '➕ ثبت کسب و کار جدید', 'web_app' => ['url' => WEBAPP_URL]]],
                            [['text' => '📋 مشاهده کسب و کارهای من', 'callback_data' => 'my_businesses']],
                            [['text' => '❓ راهنما', 'callback_data' => 'help']]
                        ]
                    ];
                    
                    sendMessage($chat_id, $error_text, $keyboard);
                }
                
            } catch (PDOException $e) {
                error_log("❌ خطای دیتابیس در پردازش لینک دعوت: " . $e->getMessage());
                
                $error_text = "❌ خطایی در پردازش لینک دعوت رخ داد.\n\n";
                $error_text .= "لطفاً بعداً دوباره تلاش کنید.";
                
                sendMessage($chat_id, $error_text);
            }
        } else {
            // دستور /start معمولی بدون پارامتر - نمایش انتخاب نوع کاربر
            $welcome_text = "💇‍♀️ <b>سلام! خوش اومدی</b>\n\n";
            $welcome_text .= "اینجا نوبت سالن‌های زیبایی و آرایشگاه‌ها رو در چند ثانیه رزرو می‌کنی — بدون تماس، بدون انتظار 🌸\n\n";
            $welcome_text .= "برای ادامه، نقش خودت رو انتخاب کن:";
            
            $keyboard = [
                'inline_keyboard' => [
                    [['text' => '💇 رزرو نوبت (مشتری)', 'callback_data' => 'user_type_customer', 'style' => 'success']],
                    [['text' => '🏢 صاحب سالن/آرایشگاه هستم', 'callback_data' => 'user_type_business_owner', 'style' => 'primary']],
                    [['text' => '📈 بازاریاب هستم', 'callback_data' => 'user_type_marketer'], ['text' => 'ℹ️ راهنما', 'callback_data' => 'help_general']]
                ]
            ];
            
            sendMessage($chat_id, $welcome_text, $keyboard);
        }
    }
    elseif (strpos($text, '/help') === 0) {
        // دستور راهنما
        $help_text = "ℹ️ <b>راهنمای سریع</b>\n\n";
        $help_text .= "💇 <b>مشتری هستی؟</b> سالن مورد نظرت رو جستجو کن، سرویس و زمان رو انتخاب کن، تموم.\n\n";
        $help_text .= "🏢 <b>صاحب سالن هستی؟</b> سالنت رو ثبت کن، ساعات کاری و خدماتت رو تنظیم کن و لینک اختصاصیت رو با مشتری‌ها به اشتراک بذار.\n\n";
        $help_text .= "📈 <b>بازاریابی؟</b> با کد معرف خودت سالن‌ها رو معرفی کن و از هر رزرو کمیسیون بگیر.\n\n";
        $help_text .= "🆘 نیاز به کمک بیشتر داری؟ با پشتیبانی در تماس باش.";
        
        $keyboard = [
            'inline_keyboard' => [
                [['text' => '🔍 جستجوی سریع', 'web_app' => ['url' => BASE_URL . "/webapp/search.html"], 'style' => 'success']],
                [['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']]
            ]
        ];
        
        sendMessage($chat_id, $help_text, $keyboard);
    }
    elseif (strpos($text, '/search') === 0) {
        // دستور جستجو
        $search_text = "🔍 <b>سالن یا آرایشگاه مورد نظرت رو پیدا کن</b>\n\n";
        $search_text .= "می‌تونی بر اساس نام، نوع خدمات یا موقعیت مکانی جستجو کنی و مستقیم نوبت بگیری.";
        
        $keyboard = [
            'inline_keyboard' => [
                [['text' => '🔍 شروع جستجو', 'web_app' => ['url' => BASE_URL . "/webapp/search.html"], 'style' => 'success']],
                [['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']]
            ]
        ];
        
        sendMessage($chat_id, $search_text, $keyboard);
    }
}

if (isset($update['callback_query'])) {
    $callback_query = $update['callback_query'];
    $chat_id = $callback_query['message']['chat']['id'];
    $message_id = $callback_query['message']['message_id'];
    $user_telegram_id = $callback_query['from']['id'];
    $callback_query_id = $callback_query['id'];
    $data = $callback_query['data'];
    
    // پاسخ پیش‌فرض به callback query (برای جلوگیری از loading)
    // برای callback های خاص، پاسخ مجزا ارسال می‌شود
    if (!in_array(substr($data, 0, strpos($data . '_', '_')), ['copy_invitation', 'share_invitation'])) {
        apiRequest('answerCallbackQuery', ['callback_query_id' => $callback_query_id]);
    }

    // گارد عضویت: بررسی عضویت برای اکثر callbackها لازم است، اما دکمه‌های حضور/عدم‌حضور و بررسی عضویت مستثنا هستند
    $is_attendance_action = (strpos($data, 'mark_attended_') === 0) || (strpos($data, 'mark_no_show_') === 0);
    if ($data !== 'verify_subscription' && !$is_attendance_action && !isUserSubscribed($user_telegram_id)) {
        sendJoinChannelMessage($chat_id);
        return;
    }

    // پردازش بررسی عضویت
    if ($data === 'verify_subscription') {
        if (isUserSubscribed($user_telegram_id)) {
            answerCallbackQuery($callback_query_id, '✅ عضویت شما تایید شد!');
            
            // بررسی وجود پارامترهای referral ذخیره شده
            $saved_referral = getReferralParams($user_telegram_id);
            
            if ($saved_referral) {
                // کاربر از طریق لینک دعوت آمده بود، پردازش referral
                error_log("🔗 بازیابی پارامتر referral پس از عضویت: " . $saved_referral);
                
                if (!$pdo) {
                    $error_text = "❌ خطایی در اتصال به پایگاه داده رخ داد.\n\n";
                    $error_text .= "لطفاً بعداً دوباره تلاش کنید.";
                    sendMessage($chat_id, $error_text);
                    return;
                }
                
                try {
                    // بررسی اینکه آیا لینک مربوط به بازاریاب است
                    if (strpos($saved_referral, 'marketer_') === 0) {
                        // لینک بازاریابی
                        $marketer_code = substr($saved_referral, strlen('marketer_'));
                        
                        // جستجوی بازاریاب بر اساس کد
                        $marketerStmt = $pdo->prepare("SELECT m.*, u.first_name FROM marketers m JOIN users u ON m.user_id = u.id WHERE m.marketer_code = ? AND m.is_active = 1");
                        $marketerStmt->execute([$marketer_code]);
                        $marketer = $marketerStmt->fetch();
                        
                        if ($marketer) {
                            $marketer_welcome_text = "👋 <b>خوش اومدی!</b>\n\n";
                            $marketer_welcome_text .= "از طریق <b>" . htmlspecialchars($marketer['first_name']) . "</b> اومدی — عالیه، بذار سالن یا آرایشگاهت رو در چند دقیقه آنلاین کنیم 💇\n\n";
                            $marketer_welcome_text .= "با ثبت سالن می‌تونی نوبت‌ها رو راحت مدیریت کنی، مشتری بیشتری بگیری و گزارش دقیق داشته باشی.";
                            
                            $marketer_keyboard = [
                                'inline_keyboard' => [
                                    [['text' => '🏢 ثبت سالن جدید', 'web_app' => ['url' => WEBAPP_URL . '&marketer=' . $marketer_code], 'style' => 'success']],
                                    [['text' => '📞 مشاوره رایگان', 'callback_data' => 'marketer_consultation_' . $marketer_code]],
                                    [['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']]
                                ]
                            ];
                            
                            sendMessage($chat_id, $marketer_welcome_text, $marketer_keyboard);
                            return;
                        }
                    }
                    
                    // اگر لینک بازاریاب نبود یا بازاریاب پیدا نشد، بررسی سایر انواع لینک‌ها
                    if (strpos($saved_referral, 'business_') === 0) {
                        // لینک کسب‌وکار - پردازش مشابه کد موجود
                        $business_id = substr($saved_referral, strlen('business_'));
                        
                        $businessStmt = $pdo->prepare("SELECT b.*, u.first_name as owner_name FROM businesses b JOIN users u ON b.user_id = u.id WHERE b.id = ? AND b.is_active = 1");
                        $businessStmt->execute([$business_id]);
                        $business = $businessStmt->fetch();
                        
                        if ($business) {
                            $business_welcome_text = "💇 <b>به " . htmlspecialchars($business['name']) . " خوش اومدی</b>\n\n";
                            $business_welcome_text .= "📍 " . htmlspecialchars($business['address']) . "\n";
                            $business_welcome_text .= "📞 " . htmlspecialchars($business['phone']) . "\n";
                            $business_welcome_text .= "🕐 " . htmlspecialchars($business['working_hours']) . "\n\n";
                            $business_welcome_text .= "برای رزرو نوبت، دکمهٔ زیر رو بزن 👇";
                            
                            $business_keyboard = [
                                'inline_keyboard' => [
                                    [['text' => '💇 رزرو نوبت', 'callback_data' => 'book_appointment_' . $business_id, 'style' => 'success']],
                                    [['text' => '📞 تماس', 'url' => 'tel:' . $business['phone']]],
                                    [['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']]
                                ]
                            ];
                            
                            sendMessage($chat_id, $business_welcome_text, $business_keyboard);
                            return;
                        }
                    }
                } catch (Exception $e) {
                    error_log("خطا در پردازش referral پس از عضویت: " . $e->getMessage());
                }
            }
            
            // اگر referral نداشت یا پردازش نشد، پیام خوش‌آمدگویی عادی
            $welcome_text = "💇‍♀️ <b>عضویتت تأیید شد، خوش اومدی!</b>\n\n";
            $welcome_text .= "برای ادامه، نقش خودت رو انتخاب کن:";
            $keyboard = [
                'inline_keyboard' => [
                    [['text' => '💇 رزرو نوبت (مشتری)', 'callback_data' => 'user_type_customer', 'style' => 'success']],
                    [['text' => '🏢 صاحب سالن/آرایشگاه هستم', 'callback_data' => 'user_type_business_owner', 'style' => 'primary']],
                    [['text' => '📈 بازاریاب هستم', 'callback_data' => 'user_type_marketer'], ['text' => 'ℹ️ راهنما', 'callback_data' => 'help_general']]
                ]
            ];
            sendMessage($chat_id, $welcome_text, $keyboard);
        } else {
            answerCallbackQuery($callback_query_id, '❌ هنوز عضو کانال نیستید!', true);
            sendJoinChannelMessage($chat_id);
        }
        return;
    }

    if ($data === 'my_businesses') {
        if (!$pdo) {
            sendMessage($chat_id, "خطایی در اتصال به پایگاه داده رخ داد. لطفاً بعداً تلاش کنید.");
            return;
        }
        
        try {
            $userStmt = $pdo->prepare("SELECT id FROM users WHERE telegram_id = ?");
            $userStmt->execute([$user_telegram_id]);
            $user = $userStmt->fetch();

            if (!$user) {
                $error_text = "⚠️ <b>مشکلی در شناسایی حساب تو پیش اومد</b>\n\n";
                $error_text .= "یک بار دیگه دستور /start رو بفرست. اگه ادامه داشت، با پشتیبانی تماس بگیر.";
                sendMessage($chat_id, $error_text);
                return;
            }
            $internal_user_id = $user['id'];

            $businessStmt = $pdo->prepare("SELECT id, name, is_active FROM businesses WHERE owner_id = ? ORDER BY created_at DESC");
            $businessStmt->execute([$internal_user_id]);
            $businesses = $businessStmt->fetchAll();
            
            $keyboard_buttons = [];
            
            if (empty($businesses)) {
                $text = "🏢 <b>سالن‌های من</b>\n\n";
                $text .= "هنوز سالنی ثبت نکردی. ثبت رایگانه و فقط چند دقیقه طول می‌کشه — بعدش لینک اختصاصی می‌گیری و مشتری‌ها مستقیم ازت نوبت می‌گیرن 💇";
            } else {
                $text = "🏢 <b>سالن‌های من</b> (" . count($businesses) . ")\n\n";
                $text .= "برای مدیریت، روی نام سالن بزن:";
                
                foreach ($businesses as $business) {
                    $status_icon = $business['is_active'] ? '🟢' : '🔴';
                    $status_text = $business['is_active'] ? 'فعال' : 'غیرفعال';
                    $keyboard_buttons[] = [
                        ['text' => $status_icon . " " . htmlspecialchars($business['name']) . " (" . $status_text . ")", 'callback_data' => 'manage_business_' . $business['id']]
                    ];
                }
            }
            
            // دکمه‌های عملیاتی بهینه شده
            $keyboard_buttons[] = [['text' => '➕ ثبت سالن جدید', 'web_app' => ['url' => WEBAPP_URL], 'style' => 'success']];
            $keyboard_buttons[] = [['text' => '🔙 بازگشت', 'callback_data' => 'user_type_business_owner', 'style' => 'danger'], ['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']];
            $keyboard = ['inline_keyboard' => $keyboard_buttons];
            
            editMessageText($chat_id, $message_id, $text, $keyboard);

        } catch (PDOException $e) {
            error_log("Error fetching businesses: " . $e->getMessage());
            sendMessage($chat_id, "خطایی در ارتباط با پایگاه داده رخ داد. لطفاً بعداً تلاش کنید.");
        }
    }
    elseif (strpos($data, 'manage_business_') === 0) {
        $business_id = substr($data, strlen('manage_business_'));

        if (!$pdo) {
            editMessageText($chat_id, $message_id, "خطایی در اتصال به پایگاه داده رخ داد. لطفاً بعداً تلاش کنید.");
            return;
        }

        try {
            $stmt = $pdo->prepare("SELECT b.* FROM businesses b JOIN users u ON b.owner_id = u.id WHERE b.id = ? AND u.telegram_id = ?");
            $stmt->execute([$business_id, $user_telegram_id]);
            $business = $stmt->fetch();

            if (!$business) {
                editMessageText($chat_id, $message_id, "خطا: این کسب و کار یافت نشد یا شما مالک آن نیستید.");
                return;
            }

            $text = "💇 <b>" . htmlspecialchars($business['name']) . "</b>\n\n";
            
            // نمایش وضعیت کسب‌وکار
            $status_icon = $business['is_active'] ? '🟢' : '🔴';
            $status_text = $business['is_active'] ? 'فعال' : 'غیرفعال';
            $text .= $status_icon . " " . $status_text;
            
            // نمایش اطلاعات اساسی
            if (!empty($business['business_type'])) {
                $text .= "  ·  🏷️ " . htmlspecialchars($business['business_type']);
            }
            $text .= "\n";
            if (!empty($business['address'])) {
                $text .= "📍 " . htmlspecialchars(substr($business['address'], 0, 50)) . (strlen($business['address']) > 50 ? '...' : '') . "\n";
            }
            
            $text .= "\n";
            
            // بررسی وجود لینک دعوت
            $invitation_link_text = "";
            $keyboard_buttons = [];
            
            if (!empty($business['link_alias'])) {
                // اگر لینک دعوت وجود دارد
                $full_invitation_link = "https://t.me/" . BOT_USERNAME . "?start=" . $business['link_alias'];
                $invitation_link_text = "🔗 <b>لینک اختصاصی رزرو:</b>\n<code>" . $full_invitation_link . "</code>\n\n✅ مشتری‌ها می‌تونن مستقیم از این لینک نوبت بگیرن.\n\n";
                
                // دکمه‌های کپی و اشتراک‌گذاری لینک
                $keyboard_buttons[] = [
                    ['text' => '📋 کپی لینک', 'callback_data' => 'copy_invitation_' . $business_id],
                    ['text' => '📤 اشتراک لینک', 'callback_data' => 'share_invitation_' . $business_id]
                ];
            } else {
                // اگر لینک دعوت وجود ندارد
                $invitation_link_text = "⚠️ هنوز لینک اختصاصی نساختی — برای گرفتن نوبت آنلاین حتماً یکی بساز.\n\n";
            }
            
            $text .= $invitation_link_text;

            // ساخت لینک ویرایش
            $separator_edit = strpos(WEBAPP_URL_EDIT, '?') === false ? '?' : '&';
            $edit_url = WEBAPP_URL_EDIT . $separator_edit . 'id=' . $business_id;

            // ساخت لینک مدیریت زمان‌بندی
            $separator_schedule = strpos(WEBAPP_URL_SCHEDULE, '?') === false ? '?' : '&';
            $schedule_url = WEBAPP_URL_SCHEDULE . $separator_schedule . 'id=' . $business_id;

            // ساخت لینک مدیریت خدمات
            $separator_services = strpos(WEBAPP_URL_SERVICES, '?') === false ? '?' : '&';
            $services_url = WEBAPP_URL_SERVICES . $separator_services . 'id=' . $business_id;

            // ساخت کیبورد با چیدمان بهبود یافته و رنگ واقعی دکمه‌ها
            $main_buttons = [
                // ردیف اول: عملیات روزانه (مهم‌ترین اکشن‌ها)
                [['text' => '📅 نوبت‌های امروز', 'callback_data' => 'today_bookings_' . $business_id, 'style' => 'primary'], ['text' => '📊 همهٔ نوبت‌ها', 'callback_data' => 'show_bookings_' . $business_id]],
                // ردیف دوم: تنظیمات اصلی
                [['text' => '🕐 ساعات کاری', 'web_app' => ['url' => $schedule_url]], ['text' => '💇 خدمات سالن', 'web_app' => ['url' => $services_url]]],
                // ردیف سوم: مدیریت صف نوبت
                [['text' => '📶 مدیریت صف', 'web_app' => ['url' => BASE_URL . "/webapp/queue.html?business_id=" . $business_id]]],
                // ردیف چهارم: تنظیمات سریع و لینک
                [['text' => '⚡ تنظیمات سریع', 'callback_data' => 'quick_settings_' . $business_id], ['text' => '🔗 مدیریت لینک', 'callback_data' => 'manage_link_' . $business_id]],
                // ردیف پنجم: ویرایش و اشتراک
                [['text' => '✏️ ویرایش اطلاعات', 'web_app' => ['url' => $edit_url]], ['text' => '💳 اشتراک', 'callback_data' => 'manage_subscription_' . $business_id]]
            ];
            
            // اضافه کردن دکمه‌های لینک دعوت (اگر وجود دارد)
            if (!empty($keyboard_buttons)) {
                $all_buttons = array_merge($main_buttons, $keyboard_buttons);
            } else {
                $all_buttons = $main_buttons;
            }
            
            // اضافه کردن دکمه‌های مدیریتی
            $all_buttons[] = [['text' => '🗑️ حذف سالن', 'callback_data' => 'delete_business_' . $business_id, 'style' => 'danger']];
            $all_buttons[] = [['text' => '🔙 بازگشت', 'callback_data' => 'my_businesses', 'style' => 'danger'], ['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']];
            
            $keyboard = [
                'inline_keyboard' => $all_buttons
            ];
            
            editMessageText($chat_id, $message_id, $text, $keyboard);

        } catch (PDOException $e) {
            error_log("Error managing business {$business_id}: " . $e->getMessage());
            editMessageText($chat_id, $message_id, "خطایی در دریافت اطلاعات کسب و کار رخ داد.");
        }
    }
    elseif ($data === 'main_menu') {
        // بازگشت به منوی اصلی
        $welcome_text = "💇‍♀️ <b>منوی اصلی</b>\n\n";
        $welcome_text .= "چیکار می‌خوای بکنی؟";
        
        $keyboard = [
            'inline_keyboard' => [
                [['text' => '💇 رزرو نوبت', 'callback_data' => 'user_type_customer', 'style' => 'success'], ['text' => '🏢 مدیریت سالن', 'callback_data' => 'user_type_business_owner']],
                [['text' => '📈 بازاریاب هستم', 'callback_data' => 'user_type_marketer']],
                [['text' => '🔍 جستجوی سریع', 'web_app' => ['url' => BASE_URL . "/webapp/search.html"]], ['text' => 'ℹ️ راهنما', 'callback_data' => 'help_general']]
            ]
        ];
        
        editMessageText($chat_id, $message_id, $welcome_text, $keyboard);
    }
    elseif (strpos($data, 'copy_invitation_') === 0) {
        // کپی لینک دعوت
        $business_id = substr($data, strlen('copy_invitation_'));
        
        if (!$pdo) {
            answerCallbackQuery($callback_query_id, "❌ خطا در اتصال به پایگاه داده!", true);
            return;
        }
        
        try {
            $stmt = $pdo->prepare("SELECT b.*, u.telegram_id FROM businesses b JOIN users u ON b.owner_id = u.id WHERE b.id = ? AND u.telegram_id = ?");
            $stmt->execute([$business_id, $user_telegram_id]);
            $business = $stmt->fetch();
            
            if (!$business || empty($business['link_alias'])) {
                answerCallbackQuery($callback_query_id, "❌ لینک دعوت یافت نشد!", true);
                return;
            }
            
            $full_invitation_link = "https://t.me/" . BOT_USERNAME . "?start=" . $business['link_alias'];
            
            // ارسال لینک در پیام جداگانه برای کپی آسان
            $copy_message = "🔗 <b>لینک اختصاصی " . htmlspecialchars($business['name']) . "</b>\n\n";
            $copy_message .= "<code>" . $full_invitation_link . "</code>\n\n";
            $copy_message .= "روی لینک بالا نگه‌دار تا کپی بشه 👆";
            
            sendMessage($chat_id, $copy_message);
            answerCallbackQuery($callback_query_id, "✅ لینک ارسال شد!");
            
        } catch (PDOException $e) {
            error_log("Error copying invitation link: " . $e->getMessage());
            answerCallbackQuery($callback_query_id, "❌ خطا در دریافت لینک!", true);
        }
    }
    elseif (strpos($data, 'show_bookings_') === 0) {
        // نمایش رزروهای کسب‌وکار
        $business_id = substr($data, strlen('show_bookings_'));
        
        if (!$pdo) {
            answerCallbackQuery($callback_query_id, "❌ خطا در اتصال به پایگاه داده!", true);
            return;
        }
        
        try {
            // بررسی مالکیت کسب‌وکار
            $stmt = $pdo->prepare("SELECT b.name FROM businesses b JOIN users u ON b.owner_id = u.id WHERE b.id = ? AND u.telegram_id = ?");
            $stmt->execute([$business_id, $user_telegram_id]);
            $business = $stmt->fetch();
            
            if (!$business) {
                answerCallbackQuery($callback_query_id, "❌ کسب‌وکار یافت نشد!", true);
                return;
            }
            
            // نمایش رزروها (فقط اخیر) - بدون فیلتر کاربر برای نمایش تمام رزروهای کسب‌وکار
            showBusinessBookings($chat_id, $business_id, $business['name'], $pdo, false);
            answerCallbackQuery($callback_query_id, "✅ رزروها نمایش داده شد");
            
        } catch (PDOException $e) {
            error_log("Error showing bookings: " . $e->getMessage());
            answerCallbackQuery($callback_query_id, "❌ خطا در دریافت رزروها!", true);
        }
    }
    elseif (strpos($data, 'share_invitation_') === 0) {
        // اشتراک‌گذاری لینک دعوت
        $business_id = substr($data, strlen('share_invitation_'));
        
        if (!$pdo) {
            answerCallbackQuery($callback_query_id, "❌ خطا در اتصال به پایگاه داده!", true);
            return;
        }
        
        try {
            $stmt = $pdo->prepare("SELECT b.*, u.telegram_id FROM businesses b JOIN users u ON b.owner_id = u.id WHERE b.id = ? AND u.telegram_id = ?");
            $stmt->execute([$business_id, $user_telegram_id]);
            $business = $stmt->fetch();
            
            if (!$business || empty($business['link_alias'])) {
                answerCallbackQuery($callback_query_id, "❌ لینک دعوت یافت نشد!", true);
                return;
            }
            
            $full_invitation_link = "https://t.me/" . BOT_USERNAME . "?start=" . $business['link_alias'];
            
            // متن اشتراک‌گذاری
            $share_message = "💇 <b>رزرو نوبت در " . htmlspecialchars($business['name']) . "</b>\n\n";
            $share_message .= "📍 " . htmlspecialchars($business['address']) . "\n";
            $share_message .= "📞 " . htmlspecialchars($business['phone']) . "\n\n";
            $share_message .= "برای رزرو، روی لینک زیر بزن:\n";
            $share_message .= $full_invitation_link;
            
            // دکمه اشتراک‌گذاری
            $share_keyboard = [
                'inline_keyboard' => [
                    [['text' => '📤 اشتراک‌گذاری', 'url' => 'https://t.me/share/url?url=' . urlencode($full_invitation_link) . '&text=' . urlencode("💇 رزرو نوبت در " . $business['name'] . "\n\n" . $full_invitation_link), 'style' => 'primary']]
                ]
            ];
            
            sendMessage($chat_id, $share_message, $share_keyboard);
            answerCallbackQuery($callback_query_id, "✅ پیام اشتراک‌گذاری ارسال شد!");
            
        } catch (PDOException $e) {
            error_log("Error sharing invitation link: " . $e->getMessage());
            answerCallbackQuery($callback_query_id, "❌ خطا در اشتراک‌گذاری لینک!", true);
        }
    }
    elseif (strpos($data, 'delete_business_') === 0) {
        // حذف کسب‌وکار
        $business_id = substr($data, strlen('delete_business_'));
        
        if (!$pdo) {
            answerCallbackQuery($callback_query_id, "❌ خطا در اتصال به پایگاه داده!", true);
            return;
        }
        
        try {
            // دریافت نام کسب‌وکار برای تأیید
            $stmt = $pdo->prepare("SELECT b.name FROM businesses b JOIN users u ON b.owner_id = u.id WHERE b.id = ? AND u.telegram_id = ?");
            $stmt->execute([$business_id, $user_telegram_id]);
            $business = $stmt->fetch();
            
            if (!$business) {
                answerCallbackQuery($callback_query_id, "❌ کسب‌وکار یافت نشد!", true);
                return;
            }
            
            // نمایش پیام تأیید حذف
            $confirm_text = "⚠️ <b>حذف سالن</b>\n\n";
            $confirm_text .= "مطمئنی می‌خوای <b>" . htmlspecialchars($business['name']) . "</b> رو حذف کنی؟\n\n";
            $confirm_text .= "این کار غیرقابل بازگشته و همراهش این‌ها هم حذف می‌شن:\n";
            $confirm_text .= "• اطلاعات سالن\n";
            $confirm_text .= "• همهٔ نوبت‌های ثبت‌شده\n";
            $confirm_text .= "• زمان‌بندی‌ها\n";
            $confirm_text .= "• لینک اختصاصی\n\n";
            $confirm_text .= "💡 اگه فقط می‌خوای موقتاً غیرفعالش کنی، از «ویرایش اطلاعات» استفاده کن.";
            
            $confirm_keyboard = [
                'inline_keyboard' => [
                    [
                        ['text' => '✅ بله، حذف کن', 'callback_data' => 'confirm_delete_' . $business_id, 'style' => 'danger'],
                        ['text' => '❌ انصراف', 'callback_data' => 'manage_business_' . $business_id]
                    ]
                ]
            ];
            
            editMessageText($chat_id, $message_id, $confirm_text, $confirm_keyboard);
            answerCallbackQuery($callback_query_id);
            
        } catch (PDOException $e) {
            error_log("Error in delete business confirmation: " . $e->getMessage());
            answerCallbackQuery($callback_query_id, "❌ خطا در دریافت اطلاعات!", true);
        }
    }
    elseif (strpos($data, 'confirm_delete_') === 0) {
        // تأیید نهایی حذف کسب‌وکار
        $business_id = substr($data, strlen('confirm_delete_'));
        
        if (!$pdo) {
            answerCallbackQuery($callback_query_id, "❌ خطا در اتصال به پایگاه داده!", true);
            return;
        }
        
        try {
            // حذف مستقیم از دیتابیس (بدون فراخوانی API)
            $pdo->beginTransaction();
            
            // بررسی مالکیت کسب‌وکار
            $ownerStmt = $pdo->prepare("
                SELECT b.id, b.name 
                FROM businesses b 
                JOIN users u ON b.owner_id = u.id 
                WHERE b.id = ? AND u.telegram_id = ?
            ");
            $ownerStmt->execute([$business_id, $user_telegram_id]);
            $businessToDelete = $ownerStmt->fetch();
            
            if (!$businessToDelete) {
                $pdo->rollBack();
                answerCallbackQuery($callback_query_id, "❌ کسب‌وکار یافت نشد یا شما مالک آن نیستید!", true);
                return;
            }
            
            // حذف رزروهای مرتبط
            $deleteBookingsStmt = $pdo->prepare("DELETE FROM bookings WHERE business_id = ?");
            $deleteBookingsStmt->execute([$business_id]);
            
            // حذف زمان‌بندی‌های خاص
            $deleteSlotsStmt = $pdo->prepare("DELETE FROM specific_time_slots WHERE business_id = ?");
            $deleteSlotsStmt->execute([$business_id]);
            
            // حذف کسب‌وکار
            $deleteBusinessStmt = $pdo->prepare("DELETE FROM businesses WHERE id = ?");
            $deleteBusinessStmt->execute([$business_id]);
            
            $pdo->commit();
            
            $result = [
                'status' => 'success',
                'business_name' => $businessToDelete['name']
            ];
            
            error_log("✅ کسب‌وکار با موفقیت حذف شد از webhook: " . $businessToDelete['name']);
            
            if ($result && $result['status'] === 'success') {
                // حذف موفقیت‌آمیز
                $success_text = "✅ <b>سالن حذف شد</b>\n\n";
                $success_text .= "<b>" . htmlspecialchars($result['business_name']) . "</b> و تمام اطلاعات مرتبطش حذف شد.";
                
                $back_keyboard = [
                    'inline_keyboard' => [
                        [['text' => '💇 سالن‌های من', 'callback_data' => 'my_businesses']],
                        [['text' => '➕ ثبت سالن جدید', 'web_app' => ['url' => WEBAPP_URL], 'style' => 'success']],
                        [['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']]
                    ]
                ];
                
                editMessageText($chat_id, $message_id, $success_text, $back_keyboard);
                answerCallbackQuery($callback_query_id, "✅ کسب‌وکار حذف شد!");
                
            } else {
                // این بخش دیگر اجرا نمی‌شود چون result همیشه success است
                answerCallbackQuery($callback_query_id, "❌ خطای نامشخص در حذف!", true);
            }
            
        } catch (PDOException $e) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }
            error_log("PDO Error in confirm delete business: " . $e->getMessage());
            answerCallbackQuery($callback_query_id, "❌ خطا در حذف کسب‌وکار از دیتابیس!", true);
        } catch (Exception $e) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }
            error_log("General Error in confirm delete business: " . $e->getMessage());
            answerCallbackQuery($callback_query_id, "❌ خطا در حذف کسب‌وکار!", true);
        }
    }
    elseif ($data === 'user_type_business_owner') {
        // نمایش منوی صاحبان کسب‌وکار
        $business_owner_text = "🏢 <b>پنل سالن/آرایشگاه</b>\n\n";
        $business_owner_text .= "چیکار می‌خوای بکنی؟";
        
        $business_keyboard = [
            'inline_keyboard' => [
                [['text' => '💇 سالن‌های من', 'callback_data' => 'my_businesses', 'style' => 'primary']],
                [['text' => '➕ ثبت سالن جدید', 'web_app' => ['url' => WEBAPP_URL], 'style' => 'success']],
                [['text' => '📅 نوبت‌های امروز', 'callback_data' => 'today_bookings'], ['text' => '📊 آمار و گزارشات', 'callback_data' => 'business_stats']],
                [['text' => 'ℹ️ راهنما', 'callback_data' => 'help_business_owner']],
                [['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']]
            ]
        ];
        
        editMessageText($chat_id, $message_id, $business_owner_text, $business_keyboard);
    }
    elseif ($data === 'user_type_customer') {
        // نمایش منوی مشتریان
        try {
            $pdo = getBotDatabaseConnection();
        } catch (Exception $e) {
            error_log("خطای اتصال دیتابیس در منوی مشتری: " . $e->getMessage());
            $pdo = null;
        }
        if (!$pdo) {
            editMessageText($chat_id, $message_id, "❌ خطا در اتصال به پایگاه داده! لطفاً کمی بعد دوباره تلاش کن.");
            return;
        }
        
        try {
            // دریافت اطلاعات کاربر
            $userStmt = $pdo->prepare("SELECT id FROM users WHERE telegram_id = ?");
            $userStmt->execute([$user_telegram_id]);
            $user = $userStmt->fetch();
            
            $customer_credit = 0;
            $user_level = ['level' => 'مبتدی', 'icon' => '🔰'];
            
            if ($user) {
                // دریافت امتیاز اعتباری مشتری
                $creditStmt = $pdo->prepare("SELECT current_credit FROM customer_credits WHERE user_id = ?");
                $creditStmt->execute([$user['id']]);
                $credit = $creditStmt->fetch();
                
                if ($credit) {
                    $customer_credit = (int)$credit['current_credit'];
                    $user_level = getUserLevel($customer_credit);
                }
            }
            
            $customer_text = "💇 <b>خب، چیکار کنیم؟</b>\n\n";
            $customer_text .= $user_level['icon'] . " سطح تو: <b>" . $user_level['level'] . "</b> (" . $customer_credit . " امتیاز)";
            
            // ساخت لینک جستجو
            $search_url = BASE_URL . "/webapp/search.html";
            
            $customer_keyboard = [
                'inline_keyboard' => [
                    [['text' => '🔍 جستجو و رزرو نوبت', 'web_app' => ['url' => $search_url], 'style' => 'success']],
                    [['text' => '📋 نوبت‌های من', 'callback_data' => 'my_customer_bookings']],
                    [['text' => 'ℹ️ راهنما', 'callback_data' => 'help_customer'], ['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']]
                ]
            ];
            
            editMessageText($chat_id, $message_id, $customer_text, $customer_keyboard);
            
        } catch (PDOException $e) {
            error_log("Error in customer panel: " . $e->getMessage());
            $customer_text = "💇 <b>خب، چیکار کنیم؟</b>\n\n";
            $customer_text .= "🔰 امتیاز شما: در حال بارگذاری...";
            
            // ساخت لینک جستجو
            $search_url = BASE_URL . "/webapp/search.html";
            
            $customer_keyboard = [
                'inline_keyboard' => [
                    [['text' => '🔍 جستجو و رزرو نوبت', 'web_app' => ['url' => $search_url], 'style' => 'success']],
                    [['text' => '📋 نوبت‌های من', 'callback_data' => 'my_customer_bookings']],
                    [['text' => 'ℹ️ راهنما', 'callback_data' => 'help_customer'], ['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']]
                ]
            ];
            
            editMessageText($chat_id, $message_id, $customer_text, $customer_keyboard);
        }
    }
    elseif ($data === 'user_type_marketer') {
        // نمایش منوی بازاریابان
        if (!$pdo) {
            editMessageText($chat_id, $message_id, "خطایی در اتصال به پایگاه داده رخ داد.");
            return;
        }
        
        try {
            // بررسی وجود کاربر در جدول users
            $userStmt = $pdo->prepare("SELECT id, first_name, last_name FROM users WHERE telegram_id = ?");
            $userStmt->execute([$user_telegram_id]);
            $user = $userStmt->fetch();
            
            if (!$user) {
                editMessageText($chat_id, $message_id, "کاربر یافت نشد. لطفاً دوباره /start کنید.");
                return;
            }
            
            // بررسی وجود بازاریاب در جدول marketers
            $marketerStmt = $pdo->prepare("SELECT * FROM marketers WHERE user_id = ?");
            $marketerStmt->execute([$user['id']]);
            $marketer = $marketerStmt->fetch();
            
            if (!$marketer) {
                // ایجاد بازاریاب جدید
                $marketer_code = 'MKT' . str_pad($user['id'], 6, '0', STR_PAD_LEFT) . rand(100, 999);
                
                $insertStmt = $pdo->prepare("
                    INSERT INTO marketers (user_id, marketer_code, total_businesses, total_bookings, commission_rate, total_commission, is_active, created_at, updated_at) 
                    VALUES (?, ?, 0, 0, 10.00, 0.00, 1, NOW(), NOW())
                ");
                $insertStmt->execute([$user['id'], $marketer_code]);
                
                // دریافت اطلاعات بازاریاب جدید
                $marketerStmt->execute([$user['id']]);
                $marketer = $marketerStmt->fetch();
            }
            
            $marketer_text = "📈 <b>پنل بازاریابی</b>\n\n";
            $marketer_text .= "سلام " . htmlspecialchars($user['first_name']) . " جان!\n\n";
            $marketer_text .= "🔑 کد بازاریابی تو: <code>" . $marketer['marketer_code'] . "</code>\n\n";
            $marketer_text .= "🏢 سالن‌های معرفی‌شده: " . $marketer['total_businesses'] . "\n";
            $marketer_text .= "📅 نوبت‌های انجام‌شده: " . $marketer['total_bookings'] . "\n";
            $marketer_text .= "💰 کمیسیون کسب‌شده: " . number_format($marketer['total_commission']) . " تومان";
            
            $marketer_keyboard = [
                'inline_keyboard' => [
                    [['text' => '🔗 لینک اختصاصی من', 'callback_data' => 'my_marketer_link_' . $marketer['id'], 'style' => 'primary']],
                    [['text' => '📊 داشبورد و آمار', 'web_app' => ['url' => BASE_URL . '/webapp/marketer_dashboard.html']]],
                    [['text' => 'ℹ️ راهنمای بازاریابی', 'callback_data' => 'marketer_help'], ['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']]
                ]
            ];
            
            editMessageText($chat_id, $message_id, $marketer_text, $marketer_keyboard);
            
        } catch (PDOException $e) {
            error_log("Error in marketer panel: " . $e->getMessage());
            editMessageText($chat_id, $message_id, "خطایی در دریافت اطلاعات بازاریاب رخ داد.");
        }
    }

    elseif ($data === 'my_bookings') {
        // نمایش رزروهای کاربر (برای صاحبان کسب‌وکار)
        sendMessage($chat_id, "📋 برای مشاهده رزروها، از دستور /bookings استفاده کنید.");
    }
    elseif ($data === 'my_customer_bookings') {
        // نمایش رزروهای مشتری
        if (!$pdo) {
            answerCallbackQuery($callback_query_id, "❌ خطا در اتصال به پایگاه داده!", true);
            return;
        }
        
        try {
            // دریافت رزروهای فعال مشتری (از 24 ساعت گذشته تا آینده)
            $yesterday = date('Y-m-d', strtotime('-1 day'));
            // اصلاح کوئری برای پشتیبانی از کاربران مهمان و تلگرامی
            $bookingsStmt = $pdo->prepare("
                SELECT b.*, bus.name as business_name, bus.address, bus.phone as business_phone,
                       u.first_name as owner_first_name, u.last_name as owner_last_name,
                       s.name as service_name, s.price as service_price,
                       customer.telegram_id as customer_telegram_id
                FROM bookings b
                JOIN businesses bus ON b.business_id = bus.id
                JOIN users u ON bus.owner_id = u.id
                LEFT JOIN users customer ON b.user_id = customer.id
                LEFT JOIN services s ON b.service_id = s.id
                WHERE (customer.telegram_id = ? OR b.customer_phone = (
                    SELECT phone FROM users WHERE telegram_id = ? LIMIT 1
                ))
                 AND b.booking_date >= ?
                ORDER BY b.booking_date ASC, b.start_time ASC
            ");
            // اجرای کوئری با پارامترهای مناسب برای کاربران تلگرامی و مهمان
             $bookingsStmt->execute([$user_telegram_id, $user_telegram_id, $yesterday]);
            $bookings = $bookingsStmt->fetchAll();
            
            if (empty($bookings)) {
                $no_bookings_text = "📭 <b>هنوز نوبتی نداری</b>\n\n";
                $no_bookings_text .= "سالن مورد نظرت رو پیدا کن و در چند ثانیه نوبت بگیر 💇";
                
                $back_keyboard = [
                    'inline_keyboard' => [
                        [['text' => '🔍 جستجوی سالن', 'web_app' => ['url' => BASE_URL . "/webapp/search.html"], 'style' => 'success']],
                        [['text' => '🔙 بازگشت', 'callback_data' => 'user_type_customer', 'style' => 'danger'], ['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']]
                    ]
                ];
                
                editMessageText($chat_id, $message_id, $no_bookings_text, $back_keyboard);
                return;
            }
            
            $bookings_text = "📋 <b>نوبت‌های من</b> (" . count($bookings) . ")\n\n";
            $bookings_text .= "برای جزئیات، روی هر نوبت بزن:";
            
            $booking_buttons = [];
            foreach ($bookings as $index => $booking) {
                if ($index >= 10) break; // نمایش حداکثر 10 رزرو
                
                // تبدیل تاریخ به شمسی
                $jalaliDate = convertToJalali($booking['booking_date']);
                $dayName = getDayName($booking['booking_date']);
                
                // تعیین وضعیت رزرو
                $status_icon = '';
                switch ($booking['status']) {
                    case 'pending': $status_icon = '⏳'; break;
                    case 'confirmed': $status_icon = '✅'; break;
                    case 'cancelled': $status_icon = '❌'; break;
                    case 'completed': $status_icon = '✔️'; break;
                    default: $status_icon = '📋'; break;
                }
                
                $button_text = "{$status_icon} {$jalaliDate} - ⏰ " . substr($booking['start_time'], 0, 5);
                $button_text .= "\n🏢 " . htmlspecialchars($booking['business_name']);
                
                if (strlen($button_text) > 60) {
                    $button_text = substr($button_text, 0, 57) . '...';
                }
                
                $booking_buttons[] = [['text' => $button_text, 'callback_data' => 'view_booking_' . $booking['id']]];
                $booking_buttons[] = [['text' => '📶 وضعیت صف کسب‌وکار', 'web_app' => ['url' => BASE_URL . "/webapp/queue_view.html?business_id=" . $booking['business_id'] . "&booking_id=" . $booking['id'] . "&user_id=" . $user_telegram_id]]];
            }
            
            if (count($bookings) > 10) {
                $bookings_text .= "\n\n💡 <i>فقط ۱۰ نوبت اخیر نمایش داده شده</i>";
            }
            
            $booking_buttons[] = [['text' => '🔍 رزرو نوبت جدید', 'web_app' => ['url' => BASE_URL . "/webapp/search.html"], 'style' => 'success']];
            $booking_buttons[] = [['text' => '🔄 بروزرسانی', 'callback_data' => 'my_customer_bookings'], ['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']];
            $booking_buttons[] = [['text' => '🔙 بازگشت', 'callback_data' => 'user_type_customer', 'style' => 'danger']];
            
            $bookings_keyboard = ['inline_keyboard' => $booking_buttons];
            
            editMessageText($chat_id, $message_id, $bookings_text, $bookings_keyboard);
            
        } catch (PDOException $e) {
            error_log("Error in customer bookings: " . $e->getMessage());
            answerCallbackQuery($callback_query_id, "❌ خطا در دریافت رزروها!", true);
        }
    }
    elseif ($data === 'help_customer') {
        $help_text = "ℹ️ <b>راهنمای رزرو نوبت</b>\n\n";
        $help_text .= "1️⃣ سالن یا آرایشگاه رو با نام، نوع خدمات یا موقعیت جستجو کن\n";
        $help_text .= "2️⃣ خدمت، تاریخ و ساعت مورد نظرت رو انتخاب کن\n";
        $help_text .= "3️⃣ رزرو رو تأیید کن و کد رزروت رو نگه‌دار\n\n";
        $help_text .= "💡 سر وقت حاضر شو — حضور به‌موقع امتیازت رو بالا می‌بره.";
        
        // ساخت لینک جستجو
        $search_url = BASE_URL . "/webapp/search.html";
        
        $help_keyboard = [
            'inline_keyboard' => [
                [['text' => '🔍 شروع جستجو', 'web_app' => ['url' => $search_url], 'style' => 'success']],
                [['text' => '🔙 بازگشت', 'callback_data' => 'user_type_customer', 'style' => 'danger'], ['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']]
            ]
        ];
        
        editMessageText($chat_id, $message_id, $help_text, $help_keyboard);
    }
    elseif ($data === 'customer_profile') {
        // نمایش پروفایل مشتری
        try {
            $pdo = getBotDatabaseConnection();
        } catch (Exception $e) {
            error_log("خطای اتصال دیتابیس در پروفایل مشتری: " . $e->getMessage());
            $pdo = null;
        }
        if (!$pdo) {
            answerCallbackQuery($callback_query_id, "❌ خطا در اتصال به پایگاه داده!", true);
            return;
        }
        
        try {
            // دریافت اطلاعات کاربر
            $userStmt = $pdo->prepare("SELECT id, first_name, last_name, username, phone, created_at FROM users WHERE telegram_id = ?");
            $userStmt->execute([$user_telegram_id]);
            $user = $userStmt->fetch();
            
            if (!$user) {
                answerCallbackQuery($callback_query_id, "❌ اطلاعات کاربری یافت نشد!", true);
                return;
            }
            
            // دریافت امتیاز اعتباری مشتری
            $creditStmt = $pdo->prepare("SELECT current_credit, total_earned, total_deducted, last_updated FROM customer_credits WHERE user_id = ?");
            $creditStmt->execute([$user['id']]);
            $credit = $creditStmt->fetch();
            
            // اگر امتیاز وجود نداشت، ایجاد کن
            if (!$credit) {
                $createCreditStmt = $pdo->prepare("INSERT INTO customer_credits (user_id, current_credit, total_earned, total_deducted) VALUES (?, 10, 10, 0)");
                $createCreditStmt->execute([$user['id']]);
                
                $creditStmt->execute([$user['id']]);
                $credit = $creditStmt->fetch();
            }
            
            // دریافت آمار رزروها
            $bookingStatsStmt = $pdo->prepare("
                SELECT 
                    COUNT(*) as total_bookings,
                    COUNT(CASE WHEN status = 'confirmed' THEN 1 END) as confirmed_bookings,
                    COUNT(CASE WHEN status = 'cancelled' THEN 1 END) as cancelled_bookings
                FROM bookings 
                WHERE customer_telegram_id = ?
            ");
            $bookingStatsStmt->execute([$user_telegram_id]);
            $bookingStats = $bookingStatsStmt->fetch();
            
            // دریافت آمار حضور
            $attendanceStatsStmt = $pdo->prepare("
                SELECT 
                    COUNT(CASE WHEN change_type = 'attendance' THEN 1 END) as total_attendances,
                    COUNT(CASE WHEN change_type = 'no_show' THEN 1 END) as total_no_shows,
                    COUNT(CASE WHEN change_type = 'cancellation' THEN 1 END) as total_cancellations
                FROM credit_history 
                WHERE user_id = ?
            ");
            $attendanceStatsStmt->execute([$user['id']]);
            $attendanceStats = $attendanceStatsStmt->fetch();
            
            $profile_text = "👤 <b>پروفایل من</b>\n\n";
            
            // اطلاعات شخصی
            $profile_text .= "نام: " . htmlspecialchars($user['first_name'] . ' ' . $user['last_name']) . "\n";
            if (!empty($user['username'])) {
                $profile_text .= "نام کاربری: @" . htmlspecialchars($user['username']) . "\n";
            }
            if (!empty($user['phone'])) {
                $profile_text .= "📞 " . htmlspecialchars($user['phone']) . "\n";
            }
            $profile_text .= "🗓 عضویت از: " . date('Y/m/d', strtotime($user['created_at'])) . "\n\n";
            
            // امتیاز اعتباری و سطح کاربری
            $current_credit = (int)$credit['current_credit'];
            $user_level = getUserLevel($current_credit);
            $credit_icon = $credit['current_credit'] >= 8 ? '🟢' : ($credit['current_credit'] >= 5 ? '🟡' : '🔴');
            
            $profile_text .= $user_level['icon'] . " سطح: <b>" . $user_level['level'] . "</b>  " . $credit_icon . " " . $current_credit . " امتیاز\n";
            $profile_text .= "📈 کسب‌شده: " . $credit['total_earned'] . "  ·  📉 کسرشده: " . $credit['total_deducted'] . "\n\n";
            
            // آمار رزروها
            $profile_text .= "📋 مجموع نوبت‌ها: " . $bookingStats['total_bookings'] . "  ·  ✅ تأییدشده: " . $bookingStats['confirmed_bookings'] . "  ·  ❌ لغوشده: " . $bookingStats['cancelled_bookings'] . "\n";
            $profile_text .= "✅ حضور: " . $attendanceStats['total_attendances'] . "  ·  ❌ غیبت: " . $attendanceStats['total_no_shows'] . "\n\n";
            
            // راهنمای امتیازدهی و سطوح کاربری
            $profile_text .= "💡 <b>سطوح:</b> 💎 الماسی ۵۰+  ·  🥇 طلایی ۳۰-۴۹  ·  🥈 نقره‌ای ۱۵-۲۹  ·  🥉 برنزی ۵-۱۴  ·  🔰 مبتدی زیر ۵\n";
            $profile_text .= "<i>حضور به‌موقع +۱، غیبت −۱، لغو نوبت −۱ امتیاز</i>";
            
            $profile_keyboard = [
                'inline_keyboard' => [
                    [['text' => '📋 نوبت‌های من', 'callback_data' => 'my_customer_bookings']],
                    [['text' => '🔄 بروزرسانی', 'callback_data' => 'customer_profile']],
                    [['text' => '🔙 بازگشت', 'callback_data' => 'user_type_customer', 'style' => 'danger']]
                ]
            ];
            
            editMessageText($chat_id, $message_id, $profile_text, $profile_keyboard);
            
        } catch (PDOException $e) {
            error_log("Error in customer profile: " . $e->getMessage());
            answerCallbackQuery($callback_query_id, "خطا در دریافت اطلاعات پروفایل!", true);
        }
    }
    elseif ($data === 'help_business_owner') {
        $help_text = "ℹ️ <b>راه‌اندازی سالن در ۴ قدم</b>\n\n";
        $help_text .= "1️⃣ اطلاعات پایه (نام، آدرس، تلفن) رو ثبت کن\n";
        $help_text .= "2️⃣ ساعات کاری رو تنظیم کن\n";
        $help_text .= "3️⃣ خدمات و قیمت‌هات رو تعریف کن\n";
        $help_text .= "4️⃣ لینک اختصاصی بگیر و با مشتری‌ها به اشتراک بذار\n\n";
        $help_text .= "💡 همهٔ این امکانات رایگانه — و رزروهای جدید بلافاصله به تو اطلاع داده می‌شن.";
        
        $help_keyboard = [
            'inline_keyboard' => [
                [['text' => '🆕 شروع ثبت سالن', 'web_app' => ['url' => WEBAPP_URL], 'style' => 'success']],
                [['text' => '🔙 بازگشت', 'callback_data' => 'user_type_business_owner', 'style' => 'danger'], ['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']]
            ]
        ];
        
        editMessageText($chat_id, $message_id, $help_text, $help_keyboard);
    }
    elseif (strpos($data, 'show_all_bookings_') === 0) {
        // نمایش همه رزروهای کسب‌وکار
        $business_id = substr($data, strlen('show_all_bookings_'));
        
        if (!$pdo) {
            answerCallbackQuery($callback_query_id, "❌ خطا در اتصال به پایگاه داده!", true);
            return;
        }
        
        try {
            // دریافت user_id از telegram_id
            $userStmt = $pdo->prepare("SELECT id FROM users WHERE telegram_id = ?");
            $userStmt->execute([$user_telegram_id]);
            $user = $userStmt->fetch();
            
            if (!$user) {
                answerCallbackQuery($callback_query_id, "❌ اطلاعات کاربری یافت نشد!", true);
                return;
            }
            
            // بررسی مالکیت کسب‌وکار
            $businessStmt = $pdo->prepare("SELECT name FROM businesses WHERE id = ? AND owner_id = ?");
            $businessStmt->execute([$business_id, $user['id']]);
            $business = $businessStmt->fetch();
            
            if (!$business) {
                answerCallbackQuery($callback_query_id, "❌ کسب‌وکار یافت نشد یا شما مالک آن نیستید!", true);
                return;
            }
            
            showBusinessBookings($chat_id, $business_id, $business['name'], $pdo, true);
            answerCallbackQuery($callback_query_id, "📋 همه رزروها نمایش داده شد");
            
        } catch (PDOException $e) {
            error_log("Error showing all bookings: " . $e->getMessage());
            answerCallbackQuery($callback_query_id, "❌ خطا در نمایش رزروها!", true);
        }
    }
    elseif (strpos($data, 'show_recent_bookings_') === 0) {
        // نمایش رزروهای اخیر کسب‌وکار
        $business_id = substr($data, strlen('show_recent_bookings_'));
        
        if (!$pdo) {
            answerCallbackQuery($callback_query_id, "❌ خطا در اتصال به پایگاه داده!", true);
            return;
        }
        
        try {
            // دریافت user_id از telegram_id
            $userStmt = $pdo->prepare("SELECT id FROM users WHERE telegram_id = ?");
            $userStmt->execute([$user_telegram_id]);
            $user = $userStmt->fetch();
            
            if (!$user) {
                answerCallbackQuery($callback_query_id, "❌ اطلاعات کاربری یافت نشد!", true);
                return;
            }
            
            // بررسی مالکیت کسب‌وکار
            $businessStmt = $pdo->prepare("SELECT name FROM businesses WHERE id = ? AND owner_id = ?");
            $businessStmt->execute([$business_id, $user['id']]);
            $business = $businessStmt->fetch();
            
            if (!$business) {
                answerCallbackQuery($callback_query_id, "❌ کسب‌وکار یافت نشد یا شما مالک آن نیستید!", true);
                return;
            }
            
            showBusinessBookings($chat_id, $business_id, $business['name'], $pdo, false);
            answerCallbackQuery($callback_query_id, "📋 رزروهای اخیر نمایش داده شد");
            
        } catch (PDOException $e) {
            error_log("Error showing recent bookings: " . $e->getMessage());
            answerCallbackQuery($callback_query_id, "❌ خطا در نمایش رزروها!", true);
        }
    }
    elseif (strpos($data, 'view_booking_') === 0) {
        // نمایش جزئیات رزرو مشتری
        $booking_id = substr($data, strlen('view_booking_'));
        
        if (!$pdo) {
            answerCallbackQuery($callback_query_id, "❌ خطا در اتصال به پایگاه داده!", true);
            return;
        }
        
        try {
            // دریافت جزئیات رزرو
            $bookingStmt = $pdo->prepare("
                SELECT b.*, bus.name as business_name, bus.address, bus.phone as business_phone,
                       bus.business_type, u.first_name as owner_first_name, u.last_name as owner_last_name,
                       s.name as service_name, s.description as service_description, s.price as service_price
                FROM bookings b
                JOIN businesses bus ON b.business_id = bus.id
                JOIN users u ON bus.owner_id = u.id
                JOIN users customer ON b.user_id = customer.id
                LEFT JOIN services s ON b.service_id = s.id
                WHERE b.id = ? AND customer.telegram_id = ?
            ");
            $bookingStmt->execute([$booking_id, $user_telegram_id]);
            $booking = $bookingStmt->fetch();
            
            if (!$booking) {
                answerCallbackQuery($callback_query_id, "❌ رزرو یافت نشد!", true);
                return;
            }
            
            // تبدیل تاریخ به شمسی
            $jalaliDate = convertToJalali($booking['booking_date']);
            $dayName = getDayName($booking['booking_date']);
            
            $booking_detail_text = "💇 <b>جزئیات نوبت</b>\n\n";
            $booking_detail_text .= "🏢 " . htmlspecialchars($booking['business_name']);
            
            if (!empty($booking['business_type'])) {
                $booking_detail_text .= "  ·  🏷️ " . htmlspecialchars($booking['business_type']);
            }
            $booking_detail_text .= "\n";
            
            $booking_detail_text .= "📅 " . $jalaliDate . " (" . $dayName . ")  ·  ⏰ " . substr($booking['start_time'], 0, 5) . " تا " . substr($booking['end_time'], 0, 5) . " (" . $booking['duration_minutes'] . " دقیقه)\n";
            
            // اطلاعات خدمت
            if (!empty($booking['service_name'])) {
                $booking_detail_text .= "🛠️ " . htmlspecialchars($booking['service_name']);
                if (!empty($booking['service_price'])) {
                    $booking_detail_text .= "  ·  💰 " . number_format($booking['service_price']) . " تومان";
                }
                $booking_detail_text .= "\n";
                if (!empty($booking['service_description'])) {
                    $booking_detail_text .= "📄 " . htmlspecialchars($booking['service_description']) . "\n";
                }
            }
            
            $booking_detail_text .= "👤 " . htmlspecialchars($booking['customer_name']);
            if (!empty($booking['customer_phone'])) {
                $booking_detail_text .= "  ·  📞 " . htmlspecialchars($booking['customer_phone']);
            }
            $booking_detail_text .= "\n";
            
            // دریافت امتیاز اعتباری مشتری
            $customer_credit = 0;
            try {
                $credit_stmt = $pdo->prepare("SELECT current_credit FROM customer_credits WHERE user_id = ?");
                $credit_stmt->execute([$user_id]);
                $credit_result = $credit_stmt->fetch(PDO::FETCH_ASSOC);
                if ($credit_result) {
                    $customer_credit = $credit_result['current_credit'];
                }
            } catch (Exception $e) {
                error_log("Error fetching customer credit: " . $e->getMessage());
            }
            
            $booking_detail_text .= "⭐ امتیاز تو: " . $customer_credit . "\n";
            $booking_detail_text .= "🔢 کد نوبت: <code>" . htmlspecialchars($booking['confirmation_code']) . "</code>\n";
            
            // وضعیت رزرو
            $status_icon = $booking['status'] === 'confirmed' ? '✅' : ($booking['status'] === 'cancelled' ? '❌' : '⏳');
            $status_text = $booking['status'] === 'confirmed' ? 'تأیید شده' : ($booking['status'] === 'cancelled' ? 'لغو شده' : 'در انتظار');
            $booking_detail_text .= $status_icon . " " . $status_text . "\n";
            
            if (!empty($booking['customer_notes'])) {
                $booking_detail_text .= "📝 " . htmlspecialchars($booking['customer_notes']) . "\n";
            }
            
            $booking_detail_text .= "\n📍 " . htmlspecialchars($booking['address']) . "\n";
            $booking_detail_text .= "📞 " . htmlspecialchars($booking['business_phone']) . "\n";
            
            $detail_keyboard = [
                'inline_keyboard' => []
            ];
            
            // بررسی امکان لغو رزرو (یک ساعت قبل از شروع)
            if ($booking['status'] !== 'cancelled') {
                $booking_datetime = $booking['booking_date'] . ' ' . $booking['start_time'];
                $booking_timestamp = strtotime($booking_datetime);
                $current_timestamp = time();
                $time_diff_hours = ($booking_timestamp - $current_timestamp) / 3600;
                
                if ($time_diff_hours >= 1) {
                    $detail_keyboard['inline_keyboard'][] = [['text' => '❌ لغو نوبت', 'callback_data' => 'cancel_booking_' . $booking['id'], 'style' => 'danger']];
                    $booking_detail_text .= "\n💡 <i>تا یک ساعت قبل از موعد می‌تونی لغوش کنی</i>";
                } else {
                    $booking_detail_text .= "\n⏰ <i>زمان لغو گذشته (حداقل یک ساعت قبل از موعد)</i>";
                }
            }
            
            $detail_keyboard['inline_keyboard'][] = [['text' => '📶 وضعیت صف', 'web_app' => ['url' => BASE_URL . "/webapp/queue_view.html?business_id=" . $booking['business_id'] . "&booking_id=" . $booking['id'] . "&user_id=" . $user_telegram_id]]];
            $detail_keyboard['inline_keyboard'][] = [['text' => '🔙 بازگشت به نوبت‌ها', 'callback_data' => 'my_customer_bookings']];
            
            editMessageText($chat_id, $message_id, $booking_detail_text, $detail_keyboard);
            
        } catch (PDOException $e) {
            error_log("Error viewing booking details: " . $e->getMessage());
            answerCallbackQuery($callback_query_id, "❌ خطا در دریافت جزئیات رزرو!", true);
        }
    }


     elseif ($data === 'help_general') {
        $help_text = "ℹ️ <b>راهنمای کلی</b>\n\n";
        $help_text .= "اینجا می‌تونی نوبت سالن‌های زیبایی و آرایشگاه‌ها رو رزرو کنی، یا اگه صاحب سالنی، سالنت رو آنلاین کنی و نوبت‌هات رو مدیریت کنی.\n\n";
        $help_text .= "💇 مشتری: جستجو کن، سرویس و زمان انتخاب کن، تموم\n";
        $help_text .= "🏢 صاحب سالن: ثبت رایگان، ساعات کاری و خدمات رو تنظیم کن\n\n";
        $help_text .= "🔒 اطلاعاتت محفوظه و پشتیبانی همیشه در دسترسه.";
        
        $help_keyboard = [
            'inline_keyboard' => [
                [['text' => '💇 رزرو نوبت', 'callback_data' => 'user_type_customer', 'style' => 'success'], ['text' => '🏢 صاحب سالن', 'callback_data' => 'user_type_business_owner']],
                [['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']]
            ]
        ];
        
        editMessageText($chat_id, $message_id, $help_text, $help_keyboard);
    }
    elseif ($data === 'help') {
        $help_text = "📚 <b>راهنمای سامانه نوبت‌دهی</b>\n\n";
        $help_text .= "این سامانه برای مدیریت آسان کسب‌وکار و رزرو نوبت طراحی شده است.\n\n";
        $help_text .= "💡 برای راهنمای کامل، از منوی اصلی استفاده کنید.";
        
        $help_keyboard = [
            'inline_keyboard' => [
                [['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']]
            ]
        ];
        
        editMessageText($chat_id, $message_id, $help_text, $help_keyboard);
    }
    elseif ($data === 'interactive_guide') {
        // راهنمای تعاملی
        $guide_step = InteractiveGuide::getUserGuideProgress($user_telegram_id);
        $step_data = InteractiveGuide::getGuideStep($guide_step);
        
        if ($step_data) {
            $guide_message = InteractiveGuide::generateGuideMessage($step_data, $user_telegram_id);
            $guide_keyboard = InteractiveGuide::generateGuideKeyboard($step_data, $guide_step);
            
            editMessageText($chat_id, $message_id, $guide_message, $guide_keyboard);
        } else {
            editMessageText($chat_id, $message_id, "❌ خطا در بارگذاری راهنمای تعاملی!");
        }
    }
    elseif (strpos($data, 'guide_') === 0) {
        // پردازش اقدامات راهنمای تعاملی
        $action_parts = explode('_', $data, 3);
        if (count($action_parts) >= 2) {
            $action = $action_parts[1];
            $step_id = isset($action_parts[2]) ? $action_parts[2] : null;
            
            switch ($action) {
                case 'next':
                    if ($step_id) {
                        InteractiveGuide::saveUserGuideProgress($user_telegram_id, $step_id);
                        $step_data = InteractiveGuide::getGuideStep($step_id);
                        
                        if ($step_data) {
                            $guide_message = InteractiveGuide::generateGuideMessage($step_data, $user_telegram_id);
                            $guide_keyboard = InteractiveGuide::generateGuideKeyboard($step_data, $step_id);
                            
                            editMessageText($chat_id, $message_id, $guide_message, $guide_keyboard);
                        }
                    }
                    break;
                    
                case 'prev':
                    $current_step = InteractiveGuide::getUserGuideProgress($user_telegram_id);
                    $steps = array_keys(InteractiveGuide::GUIDE_STEPS);
                    $current_index = array_search($current_step, $steps);
                    
                    if ($current_index > 0) {
                        $prev_step = $steps[$current_index - 1];
                        InteractiveGuide::saveUserGuideProgress($user_telegram_id, $prev_step);
                        $step_data = InteractiveGuide::getGuideStep($prev_step);
                        
                        if ($step_data) {
                            $guide_message = InteractiveGuide::generateGuideMessage($step_data, $user_telegram_id);
                            $guide_keyboard = InteractiveGuide::generateGuideKeyboard($step_data, $prev_step);
                            
                            editMessageText($chat_id, $message_id, $guide_message, $guide_keyboard);
                        }
                    }
                    break;
                    
                case 'reset':
                    InteractiveGuide::resetUserGuide($user_telegram_id);
                    $step_data = InteractiveGuide::getGuideStep('welcome');
                    
                    if ($step_data) {
                        $guide_message = InteractiveGuide::generateGuideMessage($step_data, $user_telegram_id);
                        $guide_keyboard = InteractiveGuide::generateGuideKeyboard($step_data, 'welcome');
                        
                        editMessageText($chat_id, $message_id, $guide_message, $guide_keyboard);
                    }
                    break;
                    
                case 'complete':
                    InteractiveGuide::saveUserGuideProgress($user_telegram_id, 'completed');
                    $completion_text = "🎉 <b>تبریک، راهنما تموم شد!</b>\n\n";
                    $completion_text .= "حالا آماده‌ای که سالنت رو کامل مدیریت کنی. هر وقت خواستی، می‌تونی دوباره راهنما رو ببینی.";
                    
                    $completion_keyboard = [
                        'inline_keyboard' => [
                            [['text' => '💇 سالن‌های من', 'callback_data' => 'my_businesses', 'style' => 'success']],
                            [['text' => '🔄 شروع مجدد راهنما', 'callback_data' => 'guide_reset']],
                            [['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']]
                        ]
                    ];
                    
                    editMessageText($chat_id, $message_id, $completion_text, $completion_keyboard);
                    break;
            }
        }
    }
    elseif (strpos($data, 'manage_link_') === 0) {
        // مدیریت لینک اختصاصی
        $business_id = substr($data, strlen('manage_link_'));
        
        // پاک کردن waiting state در صورت وجود
        $waiting_states_file = __DIR__ . '/../webapp/cache/waiting_states.json';
        if (file_exists($waiting_states_file)) {
            $waiting_states = json_decode(file_get_contents($waiting_states_file), true) ?: [];
            if (isset($waiting_states[$user_telegram_id])) {
                unset($waiting_states[$user_telegram_id]);
                file_put_contents($waiting_states_file, json_encode($waiting_states));
            }
        }
        
        if (!$pdo) {
            answerCallbackQuery($callback_query_id, "❌ خطا در اتصال به پایگاه داده!", true);
            return;
        }
        
        try {
            // بررسی مالکیت کسب‌وکار
            $stmt = $pdo->prepare("SELECT b.* FROM businesses b JOIN users u ON b.owner_id = u.id WHERE b.id = ? AND u.telegram_id = ?");
            $stmt->execute([$business_id, $user_telegram_id]);
            $business = $stmt->fetch();
            
            if (!$business) {
                answerCallbackQuery($callback_query_id, "❌ کسب‌وکار یافت نشد!", true);
                return;
            }
            
            $link_text = "🔗 <b>لینک اختصاصی " . htmlspecialchars($business['name']) . "</b>\n\n";
            
            if (!empty($business['link_alias'])) {
                // لینک موجود است
                $full_link = "https://t.me/" . BOT_USERNAME . "?start=" . $business['link_alias'];
                $link_text .= "✅ فعال:\n<code>" . $full_link . "</code>\n\n";
                $link_text .= "نام کاربری: " . htmlspecialchars($business['link_alias']) . "\n\n";
                $link_text .= "مشتری‌ها می‌تونن مستقیم از این لینک نوبت بگیرن.";
                
                $link_keyboard = [
                    'inline_keyboard' => [
                        [['text' => '📋 کپی لینک', 'callback_data' => 'copy_invitation_' . $business_id], ['text' => '📤 اشتراک لینک', 'callback_data' => 'share_invitation_' . $business_id]],
                        [['text' => '✏️ تغییر نام کاربری', 'callback_data' => 'change_alias_' . $business_id]],
                        [['text' => '🗑️ حذف لینک', 'callback_data' => 'delete_link_' . $business_id, 'style' => 'danger']],
                        [['text' => '🔙 بازگشت', 'callback_data' => 'manage_business_' . $business_id, 'style' => 'danger']]
                    ]
                ];
            } else {
                // لینک وجود ندارد
                $link_text .= "هنوز لینک اختصاصی نساختی. با ساختنش:\n\n";
                $link_text .= "• مشتری‌ها مستقیم به صفحهٔ رزروت می‌رسن\n";
                $link_text .= "• نوبت‌ها و فروش بیشتر می‌شه\n";
                $link_text .= "• معرفی سالنت راحت‌تر می‌شه\n\n";
                $link_text .= "کاملاً رایگان و سریعه — فقط یک نام کاربری دلخواه انتخاب کن.";
                
                $link_keyboard = [
                    'inline_keyboard' => [
                        [['text' => '🆕 ایجاد لینک اختصاصی', 'callback_data' => 'create_link_' . $business_id, 'style' => 'success']],
                        [['text' => '🔙 بازگشت', 'callback_data' => 'manage_business_' . $business_id, 'style' => 'danger']]
                    ]
                ];
            }
            
            editMessageText($chat_id, $message_id, $link_text, $link_keyboard);
            
        } catch (PDOException $e) {
             error_log("Error managing link: " . $e->getMessage());
             answerCallbackQuery($callback_query_id, "❌ خطا در مدیریت لینک!", true);
         }
     }
     elseif (strpos($data, 'create_link_') === 0) {
         // ایجاد لینک اختصاصی جدید
         $business_id = substr($data, strlen('create_link_'));
         
         if (!$pdo) {
             answerCallbackQuery($callback_query_id, "❌ خطا در اتصال به پایگاه داده!", true);
             return;
         }
         
         try {
             // بررسی مالکیت کسب‌وکار
             $stmt = $pdo->prepare("SELECT b.name FROM businesses b JOIN users u ON b.owner_id = u.id WHERE b.id = ? AND u.telegram_id = ?");
             $stmt->execute([$business_id, $user_telegram_id]);
             $business = $stmt->fetch();
             
             if (!$business) {
                 answerCallbackQuery($callback_query_id, "❌ کسب‌وکار یافت نشد!", true);
                 return;
             }
             
             $create_text = "🆕 <b>ساخت لینک اختصاصی</b>\n\n";
             $create_text .= "نام کاربری دلخواهت رو بفرست:\n\n";
             $create_text .= "• فقط حروف انگلیسی، عدد و خط تیره (_)\n";
             $create_text .= "• بین ۳ تا ۲۰ کاراکتر، نباید با عدد شروع بشه یا تکراری باشه\n\n";
             $create_text .= "مثال: <code>salon_niloofar</code>، <code>zibaii_narges</code>";
             
             // ذخیره وضعیت انتظار ورودی
             $waiting_states_file = __DIR__ . '/../webapp/cache/waiting_states.json';
             $waiting_states = [];
             if (file_exists($waiting_states_file)) {
                 $waiting_states = json_decode(file_get_contents($waiting_states_file), true) ?: [];
             }
             $waiting_states[$user_telegram_id] = [
                 'action' => 'create_link',
                 'business_id' => $business_id,
                 'timestamp' => time()
             ];
             
             $cache_dir = dirname($waiting_states_file);
             if (!is_dir($cache_dir)) {
                 mkdir($cache_dir, 0755, true);
             }
             file_put_contents($waiting_states_file, json_encode($waiting_states));
             
             $create_keyboard = [
                 'inline_keyboard' => [
                     [['text' => '❌ انصراف', 'callback_data' => 'manage_link_' . $business_id]]
                 ]
             ];
             
             editMessageText($chat_id, $message_id, $create_text, $create_keyboard);
             
         } catch (PDOException $e) {
             error_log("Error creating link: " . $e->getMessage());
             answerCallbackQuery($callback_query_id, "❌ خطا در ایجاد لینک!", true);
         }
     }
     elseif (strpos($data, 'change_alias_') === 0) {
         // تغییر نام کاربری لینک
         $business_id = substr($data, strlen('change_alias_'));
         
         if (!$pdo) {
             answerCallbackQuery($callback_query_id, "❌ خطا در اتصال به پایگاه داده!", true);
             return;
         }
         
         try {
             // بررسی مالکیت کسب‌وکار
             $stmt = $pdo->prepare("SELECT b.name, b.link_alias FROM businesses b JOIN users u ON b.owner_id = u.id WHERE b.id = ? AND u.telegram_id = ?");
             $stmt->execute([$business_id, $user_telegram_id]);
             $business = $stmt->fetch();
             
             if (!$business) {
                 answerCallbackQuery($callback_query_id, "❌ کسب‌وکار یافت نشد!", true);
                 return;
             }
             
             $change_text = "✏️ <b>تغییر نام کاربری لینک</b>\n\n";
             $change_text .= "فعلی: <code>" . htmlspecialchars($business['link_alias']) . "</code>\n\n";
             $change_text .= "نام کاربری جدید رو بفرست:\n";
             $change_text .= "• فقط حروف انگلیسی، عدد و خط تیره (_)\n";
             $change_text .= "• بین ۳ تا ۲۰ کاراکتر، نباید با عدد شروع بشه یا تکراری باشه";
             
             // ذخیره وضعیت انتظار ورودی
             $waiting_states_file = __DIR__ . '/../webapp/cache/waiting_states.json';
             $waiting_states = [];
             if (file_exists($waiting_states_file)) {
                 $waiting_states = json_decode(file_get_contents($waiting_states_file), true) ?: [];
             }
             $waiting_states[$user_telegram_id] = [
                 'action' => 'change_alias',
                 'business_id' => $business_id,
                 'timestamp' => time()
             ];
             
             $cache_dir = dirname($waiting_states_file);
             if (!is_dir($cache_dir)) {
                 mkdir($cache_dir, 0755, true);
             }
             file_put_contents($waiting_states_file, json_encode($waiting_states));
             
             $change_keyboard = [
                 'inline_keyboard' => [
                     [['text' => '❌ انصراف', 'callback_data' => 'manage_link_' . $business_id]]
                 ]
             ];
             
             editMessageText($chat_id, $message_id, $change_text, $change_keyboard);
             
         } catch (PDOException $e) {
             error_log("Error changing alias: " . $e->getMessage());
             answerCallbackQuery($callback_query_id, "❌ خطا در تغییر نام کاربری!", true);
         }
     }
     elseif (strpos($data, 'delete_link_') === 0) {
         // حذف لینک اختصاصی
         $business_id = substr($data, strlen('delete_link_'));
         
         if (!$pdo) {
             answerCallbackQuery($callback_query_id, "❌ خطا در اتصال به پایگاه داده!", true);
             return;
         }
         
         try {
             // بررسی مالکیت کسب‌وکار
             $stmt = $pdo->prepare("SELECT b.name, b.link_alias FROM businesses b JOIN users u ON b.owner_id = u.id WHERE b.id = ? AND u.telegram_id = ?");
             $stmt->execute([$business_id, $user_telegram_id]);
             $business = $stmt->fetch();
             
             if (!$business) {
                 answerCallbackQuery($callback_query_id, "❌ کسب‌وکار یافت نشد!", true);
                 return;
             }
             
             $delete_text = "🗑️ <b>حذف لینک اختصاصی</b>\n\n";
             $delete_text .= "مطمئنی می‌خوای این لینک رو حذف کنی؟\n\n";
             $delete_text .= "<code>" . htmlspecialchars($business['link_alias']) . "</code>\n\n";
             $delete_text .= "بعد از حذف، لینک دیگه کار نمی‌کنه و مشتری‌ها نمی‌تونن ازش استفاده کنن. هر وقت خواستی می‌تونی یکی جدید بسازی.";
             
             $delete_keyboard = [
                 'inline_keyboard' => [
                     [['text' => '✅ بله، حذف کن', 'callback_data' => 'confirm_delete_link_' . $business_id, 'style' => 'danger']],
                     [['text' => '❌ انصراف', 'callback_data' => 'manage_link_' . $business_id]]
                 ]
             ];
             
             editMessageText($chat_id, $message_id, $delete_text, $delete_keyboard);
             
         } catch (PDOException $e) {
             error_log("Error deleting link: " . $e->getMessage());
             answerCallbackQuery($callback_query_id, "❌ خطا در حذف لینک!", true);
         }
     }
     elseif (strpos($data, 'confirm_delete_link_') === 0) {
         // تأیید حذف لینک
         $business_id = substr($data, strlen('confirm_delete_link_'));
         
         if (!$pdo) {
             answerCallbackQuery($callback_query_id, "❌ خطا در اتصال به پایگاه داده!", true);
             return;
         }
         
         try {
             // بررسی مالکیت و حذف لینک
             $stmt = $pdo->prepare("UPDATE businesses b JOIN users u ON b.owner_id = u.id SET b.link_alias = NULL WHERE b.id = ? AND u.telegram_id = ?");
             $stmt->execute([$business_id, $user_telegram_id]);
             
             if ($stmt->rowCount() > 0) {
                 $success_text = "✅ <b>لینک حذف شد</b>\n\n";
                 $success_text .= "هر وقت خواستی می‌تونی یکی جدید بسازی.";
                 
                 $success_keyboard = [
                     'inline_keyboard' => [
                         [['text' => '🆕 ایجاد لینک جدید', 'callback_data' => 'create_link_' . $business_id, 'style' => 'success']],
                         [['text' => '🔙 بازگشت', 'callback_data' => 'manage_business_' . $business_id, 'style' => 'danger']]
                     ]
                 ];
                 
                 editMessageText($chat_id, $message_id, $success_text, $success_keyboard);
                 answerCallbackQuery($callback_query_id, "✅ لینک حذف شد!");
             } else {
                 answerCallbackQuery($callback_query_id, "❌ خطا در حذف لینک!", true);
             }
             
         } catch (PDOException $e) {
             error_log("Error confirming delete link: " . $e->getMessage());
             answerCallbackQuery($callback_query_id, "❌ خطا در حذف لینک!", true);
         }
     }
     elseif (strpos($data, 'cancel_booking_') === 0) {
         // لغو رزرو توسط مشتری
         $booking_id = substr($data, strlen('cancel_booking_'));
         error_log("🚀 === شروع cancel_booking در webhook ===" );
         error_log("📋 Booking ID: " . $booking_id);
         error_log("👤 User Telegram ID: " . $user_telegram_id);
         
         if (!$pdo) {
             error_log("❌ خطا در اتصال به پایگاه داده در webhook");
             answerCallbackQuery($callback_query_id, "❌ خطا در اتصال به پایگاه داده!", true);
             return;
         }
         
         try {
             // دریافت business_id از دیتابیس
             $businessStmt = $pdo->prepare("SELECT business_id FROM bookings WHERE id = ?");
             $businessStmt->execute([$booking_id]);
             $booking_info = $businessStmt->fetch();
             
             if (!$booking_info) {
                 error_log("❌ رزرو با ID " . $booking_id . " یافت نشد");
                 answerCallbackQuery($callback_query_id, "❌ رزرو یافت نشد!", true);
                 return;
             }
             
             error_log("✅ Business ID دریافت شد: " . $booking_info['business_id']);
             
             // درخواست لغو رزرو از API
             $api_url = BASE_URL . '/webapp/api.php';
             $post_data = json_encode([
                 'action' => 'cancel_booking',
                 'booking_id' => $booking_id,
                 'business_id' => $booking_info['business_id'],
                 'user_id' => $user_telegram_id
             ]);
             
             error_log("📤 ارسال داده‌ها به API: " . $post_data);
             
             $context = stream_context_create([
                 'http' => [
                     'method' => 'POST',
                     'header' => "Content-Type: application/json\r\n" .
                               "Content-Length: " . strlen($post_data) . "\r\n",
                     'content' => $post_data,
                     'timeout' => 30
                 ]
             ]);
             
             $response = @file_get_contents($api_url, false, $context);
             $result = $response ? json_decode($response, true) : null;
             
             if ($result && $result['status'] === 'success') {
                 $success_text = "✅ <b>نوبت لغو شد</b>\n\n";
                 $success_text .= "🔢 کد نوبت: " . htmlspecialchars($result['confirmation_code']) . "\n";
                 $success_text .= "⏰ زمان آزادشده: " . htmlspecialchars($result['freed_time']) . "\n\n";
                 $success_text .= "می‌تونی الان یه زمان دیگه رزرو کنی.";
                 
                 $success_keyboard = [
                     'inline_keyboard' => [
                         [['text' => '🔍 رزرو نوبت جدید', 'web_app' => ['url' => BASE_URL . "/webapp/search.html"], 'style' => 'success']],
                         [['text' => '📋 نوبت‌های من', 'callback_data' => 'my_customer_bookings']],
                         [['text' => '🏠 منوی اصلی', 'callback_data' => 'main_menu', 'style' => 'primary']]
                     ]
                 ];
                 
                 editMessageText($chat_id, $message_id, $success_text, $success_keyboard);
                 answerCallbackQuery($callback_query_id, "✅ رزرو لغو شد!");
                 
             } else {
                 $error_message = $result['message'] ?? 'خطای نامشخص در لغو رزرو';
                 answerCallbackQuery($callback_query_id, "❌ " . $error_message, true);
             }
             
         } catch (Exception $e) {
             error_log("Error cancelling booking: " . $e->getMessage());
             answerCallbackQuery($callback_query_id, "❌ خطا در لغو رزرو!", true);
         }
     }
     elseif (strpos($data, 'manage_subscription_') === 0) {
         $business_id = substr($data, strlen('manage_subscription_'));
         
         if (!$pdo) {
             editMessageText($chat_id, $message_id, "خطایی در اتصال به پایگاه داده رخ داد. لطفاً بعداً تلاش کنید.");
             return;
         }
         
         try {
             // بررسی مالکیت کسب‌وکار
             $ownerStmt = $pdo->prepare("SELECT b.* FROM businesses b JOIN users u ON b.owner_id = u.id WHERE b.id = ? AND u.telegram_id = ?");
             $ownerStmt->execute([$business_id, $user_telegram_id]);
             $business = $ownerStmt->fetch();
             
             if (!$business) {
                 editMessageText($chat_id, $message_id, "خطا: این کسب و کار یافت نشد یا شما مالک آن نیستید.");
                 return;
             }
             
             // درخواست اطلاعات اشتراک از API
             $api_url = BASE_URL . '/webapp/api.php';
             $post_data = json_encode([
                 'action' => 'get_subscription_status',
                 'business_id' => $business_id
             ]);
             
             $context = stream_context_create([
                 'http' => [
                     'method' => 'POST',
                     'header' => "Content-Type: application/json\r\n" .
                               "Content-Length: " . strlen($post_data) . "\r\n",
                     'content' => $post_data,
                     'timeout' => 30
                 ]
             ]);
             
             $response = @file_get_contents($api_url, false, $context);
             
             // Debug log برای بررسی درخواست
             error_log("API Request URL: " . $api_url);
             error_log("API Request Data: " . $post_data);
             error_log("API Response: " . ($response ?: 'No response'));
             
             $result = $response ? json_decode($response, true) : null;
             
             if ($result && $result['status'] === 'success') {
                 $subscription_data = $result;
                 
                 $text = "💳 <b>اشتراک " . htmlspecialchars($business['name']) . "</b>\n\n";
                 
                 // نمایش توکن‌های رایگان
                 $free_tokens = $subscription_data['free_tokens_remaining'] ?? 0;
                 $text .= "🎫 توکن رزرو رایگان: " . $free_tokens . "\n";
                 
                 // نمایش وضعیت اشتراک
                 if (!empty($subscription_data['subscription_plan'])) {
                     $text .= "📋 پلن فعلی: " . htmlspecialchars($subscription_data['subscription_plan']) . "\n";
                     if (!empty($subscription_data['expiry_date'])) {
                         $text .= "📅 انقضا: " . $subscription_data['expiry_date'] . "\n";
                     }
                 } else {
                     $text .= "📋 پلن فعلی: رایگان\n";
                 }
                 
                 // نمایش کد معرف
                 $referral_code = $subscription_data['referral_code'] ?? $business['link_alias'];
                 if (!empty($referral_code)) {
                     $text .= "\n🔗 کد معرف تو: <code>" . $referral_code . "</code>\n";
                     $text .= "با اشتراک‌گذاری این کد، به ازای هر سالن جدیدی که ثبت‌نام کنه، ۵۰ توکن رایگان می‌گیری 🎁";
                 }
                 
                 // ساخت لینک صفحه اشتراک
                 $separator = strpos(BASE_URL, '?') === false ? '?' : '&';
                 $subscription_url = BASE_URL . '/webapp/subscription.html' . $separator . 'id=' . $business_id;
                 
                 $keyboard = [
                     'inline_keyboard' => [
                         [['text' => '🛒 خرید/تمدید اشتراک', 'web_app' => ['url' => $subscription_url], 'style' => 'success']],
                         [['text' => '🔙 بازگشت', 'callback_data' => 'manage_business_' . $business_id, 'style' => 'danger']]
                     ]
                 ];
                 
                 editMessageText($chat_id, $message_id, $text, $keyboard);
                 
             } else {
                 editMessageText($chat_id, $message_id, "خطا در دریافت اطلاعات اشتراک. لطفاً بعداً تلاش کنید.");
             }
             
         } catch (Exception $e) {
             error_log("Error managing subscription: " . $e->getMessage());
             editMessageText($chat_id, $message_id, "خطایی در دریافت اطلاعات اشتراک رخ داد.");
         }
     }
     // Handler های جدید برای میانبرها
     elseif (strpos($data, 'today_bookings_') === 0) {
         $business_id = substr($data, strlen('today_bookings_'));
         // نمایش رزروهای امروز
         if (!$pdo) {
             editMessageText($chat_id, $message_id, "خطایی در اتصال به پایگاه داده رخ داد.");
             return;
         }
         
         try {
             $stmt = $pdo->prepare("SELECT b.name FROM businesses b JOIN users u ON b.owner_id = u.id WHERE b.id = ? AND u.telegram_id = ?");
             $stmt->execute([$business_id, $user_telegram_id]);
             $business = $stmt->fetch();
             
             if ($business) {
                 showBusinessBookings($chat_id, $business_id, $business['name'], $pdo, false, null);
             } else {
                 editMessageText($chat_id, $message_id, "کسب‌وکار یافت نشد.");
             }
         } catch (Exception $e) {
             editMessageText($chat_id, $message_id, "خطا در دریافت رزروها.");
         }
     }
     elseif (strpos($data, 'quick_settings_') === 0) {
         $business_id = substr($data, strlen('quick_settings_'));
         // منوی تنظیمات سریع
         $quick_text = "⚡ <b>تنظیمات سریع</b>\n\nچی رو می‌خوای تنظیم کنی؟";
         
         $quick_keyboard = [
             'inline_keyboard' => [
                 [['text' => '🔄 فعال/غیرفعال سالن', 'callback_data' => 'toggle_business_' . $business_id], ['text' => '🔗 لینک اختصاصی', 'callback_data' => 'manage_link_' . $business_id]],
                 [['text' => '📊 آمار سریع', 'callback_data' => 'quick_stats_' . $business_id], ['text' => '🔔 تنظیم اعلان', 'callback_data' => 'notification_settings_' . $business_id]],
                 [['text' => '🔙 بازگشت', 'callback_data' => 'manage_business_' . $business_id, 'style' => 'danger']]
             ]
         ];
         
         editMessageText($chat_id, $message_id, $quick_text, $quick_keyboard);
     }
     elseif ($data === 'today_bookings') {
         // رزروهای امروز برای صاحب کسب‌وکار
         $today_text = "📅 <b>نوبت‌های امروز</b>\n\nاول سالنت رو انتخاب کن تا نوبت‌های امروزش رو ببینی:";
         
         $today_keyboard = [
             'inline_keyboard' => [
                 [['text' => '💇 انتخاب سالن', 'callback_data' => 'my_businesses', 'style' => 'primary']],
                 [['text' => '🔙 بازگشت', 'callback_data' => 'user_type_business_owner', 'style' => 'danger']]
             ]
         ];
         
         editMessageText($chat_id, $message_id, $today_text, $today_keyboard);
     }
     elseif ($data === 'business_stats') {
         // آمار کلی کسب‌وکارها
         $stats_text = "📈 <b>آمار و گزارش</b>\n\nاول سالنت رو انتخاب کن تا آمار کاملش رو ببینی:";
         
         $stats_keyboard = [
             'inline_keyboard' => [
                 [['text' => '💇 انتخاب سالن', 'callback_data' => 'my_businesses', 'style' => 'primary']],
                 [['text' => '🔙 بازگشت', 'callback_data' => 'user_type_business_owner', 'style' => 'danger']]
             ]
         ];
         
         editMessageText($chat_id, $message_id, $stats_text, $stats_keyboard);
     }
     elseif ($data === 'today_customer_bookings') {
         // رزروهای امروز برای مشتری
         if (!$pdo) {
             editMessageText($chat_id, $message_id, "خطایی در اتصال به پایگاه داده رخ داد.");
             return;
         }
         
         try {
             // دریافت اطلاعات کاربر
             $userStmt = $pdo->prepare("SELECT id FROM users WHERE telegram_id = ?");
             $userStmt->execute([$user_telegram_id]);
             $user = $userStmt->fetch();
             
             if (!$user) {
                 editMessageText($chat_id, $message_id, "کاربر یافت نشد. لطفاً دوباره /start کنید.");
                 return;
             }
             
             // دریافت رزروهای امروز
             $today = date('Y-m-d');
             $bookingsStmt = $pdo->prepare("
                 SELECT b.*, bus.name as business_name, bus.address as business_address
                 FROM bookings b
                 JOIN businesses bus ON b.business_id = bus.id
                 WHERE b.user_id = ? AND b.booking_date = ? AND b.status != 'cancelled'
                 ORDER BY b.start_time ASC
             ");
             $bookingsStmt->execute([$user['id'], $today]);
             $todayBookings = $bookingsStmt->fetchAll();
             
             $today_customer_text = "📅 <b>نوبت‌های امروز</b>\n\n";
             
             if (empty($todayBookings)) {
                 $today_customer_text .= "📭 امروز نوبتی نداری.";
             } else {
                 foreach ($todayBookings as $index => $booking) {
                     $status_icon = $booking['status'] === 'confirmed' ? '✅' : '⏳';
                     $status_text = $booking['status'] === 'confirmed' ? 'تأیید شده' : 'در انتظار';
                     
                     $today_customer_text .= ($index + 1) . ". " . $status_icon . " <b>" . htmlspecialchars($booking['business_name']) . "</b>  ·  🕐 " . substr($booking['start_time'], 0, 5) . "  ·  " . $status_text . "\n";
                     
                     if (!empty($booking['business_address'])) {
                         $address_short = strlen($booking['business_address']) > 30 ? 
                             substr($booking['business_address'], 0, 30) . '...' : 
                             $booking['business_address'];
                         $today_customer_text .= "📍 " . htmlspecialchars($address_short);
                     }
                     
                     if (!empty($booking['service_name'])) {
                         $today_customer_text .= "  ·  🛠️ " . htmlspecialchars($booking['service_name']);
                     }
                     
                     $today_customer_text .= "\n\n";
                 }
                 
                 $today_customer_text .= "🔔 یادآوری قبل از موعد برات می‌فرستیم.";
             }
             
             $today_customer_keyboard = [
                 'inline_keyboard' => [
                     [['text' => '📋 همهٔ نوبت‌ها', 'callback_data' => 'my_customer_bookings'], ['text' => '🔍 رزرو جدید', 'web_app' => ['url' => BASE_URL . "/webapp/search.html"]]],
                     [['text' => '🔔 تنظیم یادآوری', 'callback_data' => 'reminder_settings'], ['text' => '🔄 بروزرسانی', 'callback_data' => 'today_customer_bookings']],
                     [['text' => '🔙 بازگشت', 'callback_data' => 'user_type_customer', 'style' => 'danger']]
                 ]
             ];
             
             editMessageText($chat_id, $message_id, $today_customer_text, $today_customer_keyboard);
             
         } catch (PDOException $e) {
             error_log("Error fetching today's customer bookings: " . $e->getMessage());
             editMessageText($chat_id, $message_id, "خطایی در دریافت رزروهای امروز رخ داد.");
         }
     }
     elseif ($data === 'booking_reminders') {
         // یادآوری‌های رزرو
         $reminders_text = "🔔 <b>یادآوری نوبت</b>\n\nمی‌تونی زمان یادآوری رو تنظیم کنی:";
         
         $reminders_keyboard = [
             'inline_keyboard' => [
                 [['text' => '🔔 فعال/غیرفعال', 'callback_data' => 'toggle_reminders'], ['text' => '⏰ تنظیم زمان', 'callback_data' => 'set_reminder_time']],
                 [['text' => '🔙 بازگشت', 'callback_data' => 'user_type_customer', 'style' => 'danger']]
             ]
         ];
         
         editMessageText($chat_id, $message_id, $reminders_text, $reminders_keyboard);
      }
      elseif ($data === 'reminder_settings') {
          // تنظیمات یادآوری
          if (!$pdo) {
              editMessageText($chat_id, $message_id, "خطایی در اتصال به پایگاه داده رخ داد.");
              return;
          }
          
          try {
              // دریافت تنظیمات فعلی کاربر
              $userStmt = $pdo->prepare("SELECT id, reminder_enabled, reminder_minutes FROM users WHERE telegram_id = ?");
              $userStmt->execute([$user_telegram_id]);
              $user = $userStmt->fetch();
              
              if (!$user) {
                  editMessageText($chat_id, $message_id, "کاربر یافت نشد.");
                  return;
              }
              
              $reminder_enabled = $user['reminder_enabled'] ?? 1;
              $reminder_minutes = $user['reminder_minutes'] ?? 30;
              
              $settings_text = "🔔 <b>تنظیم یادآوری</b>\n\n";
              $settings_text .= "وضعیت: " . ($reminder_enabled ? '✅ فعال' : '❌ غیرفعال') . "\n";
              $settings_text .= "⏰ " . $reminder_minutes . " دقیقه قبل از موعد";
              
              $settings_keyboard = [
                  'inline_keyboard' => [
                      [['text' => ($reminder_enabled ? '🔕 غیرفعال کردن' : '🔔 فعال کردن'), 'callback_data' => 'toggle_reminder_' . $user['id']]],
                      [['text' => '۱۵ دقیقه قبل', 'callback_data' => 'set_reminder_15_' . $user['id']], ['text' => '۳۰ دقیقه قبل', 'callback_data' => 'set_reminder_30_' . $user['id']]],
                      [['text' => '۶۰ دقیقه قبل', 'callback_data' => 'set_reminder_60_' . $user['id']], ['text' => '۱۲۰ دقیقه قبل', 'callback_data' => 'set_reminder_120_' . $user['id']]],
                      [['text' => '🔙 بازگشت', 'callback_data' => 'user_type_customer', 'style' => 'danger']]
                  ]
              ];
              
              editMessageText($chat_id, $message_id, $settings_text, $settings_keyboard);
              
          } catch (PDOException $e) {
              error_log("Error in reminder settings: " . $e->getMessage());
              editMessageText($chat_id, $message_id, "خطایی در دریافت تنظیمات رخ داد.");
          }
      }
      elseif (strpos($data, 'toggle_reminder_') === 0) {
          $user_id = substr($data, strlen('toggle_reminder_'));
          
          if (!$pdo) {
              answerCallbackQuery($callback_query_id, "خطا در اتصال به پایگاه داده!", true);
              return;
          }
          
          try {
              // تغییر وضعیت یادآوری
              $stmt = $pdo->prepare("UPDATE users SET reminder_enabled = NOT COALESCE(reminder_enabled, 1) WHERE id = ? AND telegram_id = ?");
              $stmt->execute([$user_id, $user_telegram_id]);
              
              if ($stmt->rowCount() > 0) {
                  answerCallbackQuery($callback_query_id, "✅ تنظیمات یادآوری تغییر کرد!");
                  // بازگشت به صفحه تنظیمات
                  $data = 'reminder_settings';
                  goto process_callback;
              } else {
                  answerCallbackQuery($callback_query_id, "❌ خطا در تغییر تنظیمات!", true);
              }
          } catch (PDOException $e) {
              error_log("Error toggling reminder: " . $e->getMessage());
              answerCallbackQuery($callback_query_id, "❌ خطا در تغییر تنظیمات!", true);
          }
      }
      elseif (strpos($data, 'set_reminder_') === 0) {
          $parts = explode('_', $data);
          $minutes = $parts[2];
          $user_id = $parts[3];
          
          if (!$pdo) {
              answerCallbackQuery($callback_query_id, "خطا در اتصال به پایگاه داده!", true);
              return;
          }
          
          try {
              // تنظیم زمان یادآوری
              $stmt = $pdo->prepare("UPDATE users SET reminder_minutes = ?, reminder_enabled = 1 WHERE id = ? AND telegram_id = ?");
              $stmt->execute([$minutes, $user_id, $user_telegram_id]);
              
              if ($stmt->rowCount() > 0) {
                  answerCallbackQuery($callback_query_id, "✅ زمان یادآوری به " . $minutes . " دقیقه تنظیم شد!");
                  // بازگشت به صفحه تنظیمات
                  $data = 'reminder_settings';
                  goto process_callback;
              } else {
                  answerCallbackQuery($callback_query_id, "❌ خطا در تنظیم زمان!", true);
              }
          } catch (PDOException $e) {
              error_log("Error setting reminder time: " . $e->getMessage());
              answerCallbackQuery($callback_query_id, "❌ خطا در تنظیم زمان!", true);
          }
      }
      elseif (strpos($data, 'my_marketer_link_') === 0) {
          $marketer_id = substr($data, strlen('my_marketer_link_'));
          
          if (!$pdo) {
              answerCallbackQuery($callback_query_id, "خطا در اتصال به پایگاه داده!", true);
              return;
          }
          
          try {
              // دریافت اطلاعات بازاریاب
              $marketerStmt = $pdo->prepare("SELECT m.*, u.first_name FROM marketers m JOIN users u ON m.user_id = u.id WHERE m.id = ? AND u.telegram_id = ?");
              $marketerStmt->execute([$marketer_id, $user_telegram_id]);
              $marketer = $marketerStmt->fetch();
              
              if (!$marketer) {
                  answerCallbackQuery($callback_query_id, "بازاریاب یافت نشد!", true);
                  return;
              }
              
              // ایجاد لینک اختصاصی بازاریاب
              $marketer_link = "https://t.me/" . BOT_USERNAME . "?start=marketer_" . $marketer['marketer_code'];
              
              $link_text = "🔗 <b>لینک اختصاصی بازاریابی</b>\n\n";
              $link_text .= "سلام " . htmlspecialchars($marketer['first_name']) . " جان!\n\n";
              $link_text .= "🔑 کد بازاریابی: <code>" . $marketer['marketer_code'] . "</code>\n";
              $link_text .= "🔗 لینک تو:\n<code>" . $marketer_link . "</code>\n\n";
              $link_text .= "این لینک رو برای صاحبان سالن بفرست. با کلیک روش، سالن آن‌ها به اسم تو ثبت می‌شه و از هر نوبتش کمیسیون می‌گیری.";
              
              $link_keyboard = [
                  'inline_keyboard' => [
                      [['text' => '📋 کپی لینک', 'callback_data' => 'copy_marketer_link_' . $marketer['marketer_code'], 'style' => 'primary']],
                      [['text' => '🎨 ارسال بنر حرفه‌ای', 'callback_data' => 'send_marketer_banner_' . $marketer['id']]],
                      [['text' => '📊 مشاهده آمار', 'callback_data' => 'marketer_dashboard_' . $marketer['id']]],
                      [['text' => '🔙 بازگشت', 'callback_data' => 'user_type_marketer', 'style' => 'danger']]
                  ]
              ];
              
              editMessageText($chat_id, $message_id, $link_text, $link_keyboard);
              
          } catch (PDOException $e) {
              error_log("Error in marketer link: " . $e->getMessage());
              answerCallbackQuery($callback_query_id, "خطا در دریافت لینک!", true);
          }
      }
      elseif (strpos($data, 'send_marketer_banner_') === 0) {
          $marketer_id = substr($data, strlen('send_marketer_banner_'));
          
          if (!$pdo) {
              answerCallbackQuery($callback_query_id, "خطا در اتصال به پایگاه داده!", true);
              return;
          }
          
          try {
              // دریافت اطلاعات بازاریاب
              $marketerStmt = $pdo->prepare("SELECT m.*, u.first_name FROM marketers m JOIN users u ON m.user_id = u.id WHERE m.id = ? AND u.telegram_id = ?");
              $marketerStmt->execute([$marketer_id, $user_telegram_id]);
              $marketer = $marketerStmt->fetch();
              
              if (!$marketer) {
                  answerCallbackQuery($callback_query_id, "بازاریاب یافت نشد!", true);
                  return;
              }
              
              // ایجاد بنر حرفه‌ای
              $marketer_link = "https://t.me/" . BOT_USERNAME . "?start=marketer_" . $marketer['marketer_code'];
              
              $banner_text = "💇 <b>سالن یا آرایشگاهت رو آنلاین کن</b>\n\n";
              $banner_text .= "رزرو نوبت آسان، پنل مدیریت حرفه‌ای، اعلان‌های خودکار و گزارش کامل — کاملاً رایگان شروع کن.\n\n";
              $banner_text .= "👇 برای ثبت‌نام رو لینک بزن:\n";
              $banner_text .= $marketer_link;
              
              // ارسال بنر به کاربر
              sendMessage($chat_id, $banner_text);
              answerCallbackQuery($callback_query_id, "✅ بنر ارسال شد! می‌تونی فوروارد کنی.");
              
          } catch (PDOException $e) {
              error_log("Error sending marketer banner: " . $e->getMessage());
              answerCallbackQuery($callback_query_id, "خطا در ارسال بنر!", true);
          }
      }
      elseif (strpos($data, 'marketer_dashboard_') === 0) {
          $marketer_id = substr($data, strlen('marketer_dashboard_'));
          
          if (!$pdo) {
              answerCallbackQuery($callback_query_id, "خطا در اتصال به پایگاه داده!", true);
              return;
          }
          
          try {
              // دریافت اطلاعات بازاریاب
              $marketerStmt = $pdo->prepare("SELECT m.*, u.first_name FROM marketers m JOIN users u ON m.user_id = u.id WHERE m.id = ? AND u.telegram_id = ?");
              $marketerStmt->execute([$marketer_id, $user_telegram_id]);
              $marketer = $marketerStmt->fetch();
              
              if (!$marketer) {
                  answerCallbackQuery($callback_query_id, "بازاریاب یافت نشد!", true);
                  return;
              }
              
              // دریافت کسب‌وکارهای معرفی شده
              $businessesStmt = $pdo->prepare("
                  SELECT b.*, COUNT(bk.id) as total_bookings
                  FROM businesses b
                  LEFT JOIN bookings bk ON b.id = bk.business_id
                  WHERE b.marketer_id = ?
                  GROUP BY b.id
                  ORDER BY b.created_at DESC
              ");
              $businessesStmt->execute([$marketer_id]);
              $businesses = $businessesStmt->fetchAll();
              
              $dashboard_text = "📊 <b>داشبورد بازاریابی</b>\n\n";
              $dashboard_text .= "سلام " . htmlspecialchars($marketer['first_name']) . " جان!\n\n";
              $dashboard_text .= "🔑 کد: <code>" . $marketer['marketer_code'] . "</code>\n";
              $dashboard_text .= "🏢 سالن‌های معرفی‌شده: " . count($businesses) . "\n";
              $dashboard_text .= "💰 کمیسیون: " . number_format($marketer['total_commission']) . " تومان\n";
              $dashboard_text .= "📅 مجموع نوبت‌ها: " . $marketer['total_bookings'] . "\n\n";
              
              if (empty($businesses)) {
                  $dashboard_text .= "📭 هنوز سالنی معرفی نکردی — لینک اختصاصی‌ت رو با صاحبان سالن به اشتراک بذار.";
              } else {
                  $dashboard_text .= "<b>سالن‌های معرفی‌شده:</b>\n";
                  
                  foreach ($businesses as $index => $business) {
                      $status_icon = $business['is_active'] ? '🟢' : '🔴';
                      $dashboard_text .= ($index + 1) . ". " . $status_icon . " <b>" . htmlspecialchars($business['name']) . "</b>  ·  📊 " . $business['total_bookings'] . " نوبت  ·  " . date('Y/m/d', strtotime($business['created_at'])) . "\n";
                  }
                  
                  $max_bookings = max(array_column($businesses, 'total_bookings'));
                  if ($max_bookings > 0) {
                      $dashboard_text .= "\n";
                      foreach ($businesses as $business) {
                          $percentage = ($business['total_bookings'] / $max_bookings) * 10;
                          $bar = str_repeat('█', (int)$percentage) . str_repeat('░', 10 - (int)$percentage);
                          $dashboard_text .= "▫️ " . substr($business['name'], 0, 15) . ": " . $bar . " (" . $business['total_bookings'] . ")\n";
                      }
                  }
              }
              
              $dashboard_keyboard = [
                  'inline_keyboard' => [
                      [['text' => '🔗 لینک اختصاصی', 'callback_data' => 'my_marketer_link_' . $marketer['id'], 'style' => 'primary']],
                      [['text' => '🔄 بروزرسانی آمار', 'callback_data' => 'marketer_dashboard_' . $marketer['id']]],
                      [['text' => '🔙 بازگشت', 'callback_data' => 'user_type_marketer', 'style' => 'danger']]
                  ]
              ];
              
              editMessageText($chat_id, $message_id, $dashboard_text, $dashboard_keyboard);
              
          } catch (PDOException $e) {
              error_log("Error in marketer dashboard: " . $e->getMessage());
              answerCallbackQuery($callback_query_id, "خطا در دریافت داشبورد!", true);
          }
      }
      
      // پردازش callback های حضور و غیاب
      if (strpos($data, 'mark_attended_') === 0) {
    // callback_data format: mark_attended_{booking_id}_{business_id}
    $callback_parts = str_replace('mark_attended_', '', $data);
    $parts = explode('_', $callback_parts);
    if (count($parts) >= 2) {
        $booking_id = $parts[0];
        $business_id = $parts[1];
        // پاسخ فوری به کلیک دکمه حضور
        answerCallbackQuery($callback_query_id, "⏳ در حال ثبت حضور...", false);
              
              // بررسی مالکیت کسب‌وکار
              try {
                  $userStmt = $pdo->prepare("SELECT id FROM users WHERE telegram_id = ?");
                  $userStmt->execute([$user_telegram_id]);
                  $user = $userStmt->fetch();
                  
                  if (!$user) {
                      answerCallbackQuery($callback_query_id, "❌ کاربر یافت نشد!", true);
                      return;
                  }
                  
                  $businessStmt = $pdo->prepare("SELECT * FROM businesses WHERE id = ? AND owner_id = ?");
$businessStmt->execute([$business_id, $user['id']]);
$business = $businessStmt->fetch();
                  
                  if (!$business) {
                      answerCallbackQuery($callback_query_id, "❌ کسب‌وکار یافت نشد یا شما مالک آن نیستید!", true);
                      return;
                  }
                  
                  // دریافت اطلاعات رزرو
                  $bookingStmt = $pdo->prepare("SELECT b.* FROM bookings b WHERE b.id = ? AND b.business_id = ?");
$bookingStmt->execute([$booking_id, $business_id]);
$booking = $bookingStmt->fetch();
                  
                  if (!$booking) {
                      answerCallbackQuery($callback_query_id, "❌ رزرو یافت نشد!", true);
                      return;
                  }
                  
                  // ارسال درخواست به API
                  $api_url = BASE_URL . '/webapp/customer_credit_api.php';
                  $post_data = [
                      'action' => 'record_attendance',
                      'booking_id' => $booking_id,
                      'user_id' => $booking['user_id'],
                      'business_id' => $business_id,
                      'attendance_status' => 'attended',
                      'confirmed_by' => $user['id']
                  ];
                  debug_log('📤 ارسال درخواست حضور به API', 'INFO', [
                      'url' => $api_url,
                      'payload' => $post_data
                  ]);
                  
                  $ch = curl_init();
                  curl_setopt($ch, CURLOPT_URL, $api_url);
                  curl_setopt($ch, CURLOPT_POST, true);
                  curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($post_data));
                  curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json', 'X-Internal-Api-Secret: ' . INTERNAL_API_SECRET]);
                  curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
                  curl_setopt($ch, CURLOPT_TIMEOUT, 30);
                  
                  $response = curl_exec($ch);
                  $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
                  $curl_error = curl_error($ch);
                  curl_close($ch);
                  debug_log('📨 پاسخ API حضور', 'INFO', [
                      'http_code' => $http_code,
                      'response' => $response,
                      'curl_error' => $curl_error
                  ]);
                  if ($curl_error) {
                      error_log('Attendance API CURL error: ' . $curl_error);
                  }
                  
                  if ($http_code === 200 && $response) {
                      $result = json_decode($response, true);
                      if ($result && $result['success']) {
                          answerCallbackQuery($callback_query_id, "✅ حضور مشتری ثبت شد! امتیاز اعتبار +1 شد");
                          
                          // بروزرسانی پیام
                          $updated_text = "✅ <b>حضور ثبت شد</b>\n\n";
                          $updated_text .= "👤 " . htmlspecialchars($booking['customer_name']) . "\n";
                          $updated_text .= "📅 " . $booking['booking_date'] . "  ·  ⏰ " . $booking['start_time'] . " - " . $booking['end_time'] . "\n";
                          $updated_text .= "⭐ امتیاز مشتری +۱ شد";
                          
                          editMessageText($chat_id, $message_id, $updated_text, ['inline_keyboard' => []]);
                      } else {
                          $error_msg = $result['error'] ?? ($result['message'] ?? 'خطای نامشخص');
                          error_log("Attendance API error (attended): " . $error_msg . "; response=" . $response);
                          answerCallbackQuery($callback_query_id, "❌ " . $error_msg, true);
                      }
                  } else {
                      answerCallbackQuery($callback_query_id, "❌ خطا در ثبت حضور!", true);
                  }
                  
              } catch (PDOException $e) {
                  error_log("Error in mark_attended: " . $e->getMessage());
                  answerCallbackQuery($callback_query_id, "❌ خطا در اتصال به پایگاه داده!", true);
              }
          }
      }
      
      if (strpos($data, 'mark_no_show_') === 0) {
    // callback_data format: mark_no_show_{booking_id}_{business_id}
    $callback_parts = str_replace('mark_no_show_', '', $data);
    $parts = explode('_', $callback_parts);
    if (count($parts) >= 2) {
        $booking_id = $parts[0];
        $business_id = $parts[1];
        // پاسخ فوری به کلیک دکمه عدم حضور
        answerCallbackQuery($callback_query_id, "⏳ در حال ثبت عدم حضور...", false);
              
              // بررسی مالکیت کسب‌وکار
              try {
                  $userStmt = $pdo->prepare("SELECT id FROM users WHERE telegram_id = ?");
                  $userStmt->execute([$user_telegram_id]);
                  $user = $userStmt->fetch();
                  
                  if (!$user) {
                      answerCallbackQuery($callback_query_id, "❌ کاربر یافت نشد!", true);
                      return;
                  }
                  
                  $businessStmt = $pdo->prepare("SELECT * FROM businesses WHERE id = ? AND owner_id = ?");
$businessStmt->execute([$business_id, $user['id']]);
$business = $businessStmt->fetch();
                  
                  if (!$business) {
                      answerCallbackQuery($callback_query_id, "❌ کسب‌وکار یافت نشد یا شما مالک آن نیستید!", true);
                      return;
                  }
                  
                  // دریافت اطلاعات رزرو
                  $bookingStmt = $pdo->prepare("SELECT b.* FROM bookings b WHERE b.id = ? AND b.business_id = ?");
$bookingStmt->execute([$booking_id, $business_id]);
$booking = $bookingStmt->fetch();
                  
                  if (!$booking) {
                      answerCallbackQuery($callback_query_id, "❌ رزرو یافت نشد!", true);
                      return;
                  }
                  
                  // ارسال درخواست به API
                  $api_url = BASE_URL . '/webapp/customer_credit_api.php';
                  $post_data = [
                      'action' => 'record_attendance',
                      'booking_id' => $booking_id,
                      'user_id' => $booking['user_id'],
                      'business_id' => $business_id,
                      'attendance_status' => 'no_show',
                      'confirmed_by' => $user['id']
                  ];
                  debug_log('📤 ارسال درخواست عدم حضور به API', 'INFO', [
                      'url' => $api_url,
                      'payload' => $post_data
                  ]);
                  
                  $ch = curl_init();
                  curl_setopt($ch, CURLOPT_URL, $api_url);
                  curl_setopt($ch, CURLOPT_POST, true);
                  curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($post_data));
                  curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json', 'X-Internal-Api-Secret: ' . INTERNAL_API_SECRET]);
                  curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
                  curl_setopt($ch, CURLOPT_TIMEOUT, 30);
                  
                  $response = curl_exec($ch);
                  $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
                  $curl_error = curl_error($ch);
                  curl_close($ch);
                  debug_log('📨 پاسخ API عدم حضور', 'INFO', [
                      'http_code' => $http_code,
                      'response' => $response,
                      'curl_error' => $curl_error
                  ]);
                  if ($curl_error) {
                      error_log('No-show API CURL error: ' . $curl_error);
                  }
                  
                  if ($http_code === 200 && $response) {
                      $result = json_decode($response, true);
                      if ($result && $result['success']) {
                          answerCallbackQuery($callback_query_id, "❌ عدم حضور مشتری ثبت شد! امتیاز اعتبار -1 شد");
                          
                          // بروزرسانی پیام
                          $updated_text = "❌ <b>عدم حضور ثبت شد</b>\n\n";
                          $updated_text .= "👤 " . htmlspecialchars($booking['customer_name']) . "\n";
                          $updated_text .= "📅 " . $booking['booking_date'] . "  ·  ⏰ " . $booking['start_time'] . " - " . $booking['end_time'] . "\n";
                          $updated_text .= "⭐ امتیاز مشتری −۱ شد";
                          
                          editMessageText($chat_id, $message_id, $updated_text, ['inline_keyboard' => []]);
                      } else {
                          $error_msg = $result['error'] ?? ($result['message'] ?? 'خطای نامشخص');
                          error_log("Attendance API error (no_show): " . $error_msg . "; response=" . $response);
                          answerCallbackQuery($callback_query_id, "❌ " . $error_msg, true);
                      }
                  } else {
                      answerCallbackQuery($callback_query_id, "❌ خطا در ثبت عدم حضور!", true);
                  }
                  
              } catch (PDOException $e) {
                  error_log("Error in mark_no_show: " . $e->getMessage());
                  answerCallbackQuery($callback_query_id, "❌ خطا در اتصال به پایگاه داده!", true);
              }
          }
      }
      
      process_callback:
}

// تنظیم دستورات ربات در صورت عدم وجود
if (!isset($_GET['skip_commands'])) {
    setBotCommands();
}
?>