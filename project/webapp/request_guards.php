<?php
/**
 * =================================================================
 *  فهرست مرجع: اکشن‌هایی که واقعاً به «شناسه‌ی کسب‌وکار» نیاز دارند.
 * =================================================================
 *  پیش از این، API برعکس عمل می‌کرد: هر اکشنی که در فهرست کوچکِ
 *  «معاف‌ها» نبود، اجباراً business_id می‌خواست. در نتیجه هر اکشن جدیدی
 *  که بعداً اضافه می‌شد (مثل get_my_businesses، cancel_booking و
 *  register_marketer) با خطای «Business ID is required.» رد می‌شد.
 *
 *  اکنون فهرست به‌صورت «مثبت» تعریف شده است: فقط اکشن‌های زیر شناسه‌ی
 *  کسب‌وکار لازم دارند و بقیه آزادانه پردازش می‌شوند.
 */
function actions_requiring_business_id() {
    return [
        // --- GET ---
        'get_business',
        'get_business_for_edit',
        'get_schedule',
        'get_specific_schedule',
        'get_services',
        'get_available_slots',
        'get_available_dates',
        'get_active_queue',
        'get_customer_queue_info',
        'get_business_bookings',
        // --- POST ---
        'update_business',
        'update_schedule',
        'update_specific_schedule',
        'update_services',
        'update_link_alias',
        'delete_business',
        'create_booking',
        'get_subscription_status',
        'purchase_plan',
        'advance_queue',
        'complete_booking',
        'record_attendance',
    ];
}

/** آیا این اکشن به شناسه‌ی کسب‌وکار نیاز دارد؟ */
function action_requires_business_id($action) {
    return in_array($action, actions_requiring_business_id(), true);
}

/**
 * اعتبارسنجی شناسه‌ی کسب‌وکار برای اکشن جاری.
 * فقط وقتی اجباری است که اکشن واقعاً به آن نیاز داشته باشد.
 * خروجی: شناسه‌ی عددی معتبر یا null (برای اکشن‌های بدون کسب‌وکار).
 */
function is_valid_business_id($business_id) {
    return $business_id !== null
        && $business_id !== ''
        && preg_match('/^[1-9][0-9]*$/', (string)$business_id) === 1;
}

function require_business_id_if_needed($action, $business_id) {
    if (!action_requires_business_id($action)) {
        return is_valid_business_id($business_id) ? (int)$business_id : null;
    }
    if (!is_valid_business_id($business_id)) {
        error_log("❌ Business ID لازم است ولی معتبر نیست برای action: " . $action);
        send_json_response('error', 'Business ID is required.', [], 400);
    }
    return (int)$business_id;
}
