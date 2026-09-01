<?php
/**
 * فایل لاگ‌گذاری اختصاصی برای تشخیص خطاهای HTTP 500
 * این فایل تمام خطاها و عملیات مهم را در فایل debug.log ثبت می‌کند
 * 
 * نحوه استفاده:
 * require_once 'debug_logger.php';
 * debug_log('پیام شما', 'INFO'); // یا 'ERROR' یا 'WARNING'
 */

// تنظیمات لاگ
define('DEBUG_LOG_FILE', __DIR__ . '/debug.log');
define('DEBUG_LOG_ENABLED', true);
define('DEBUG_LOG_MAX_SIZE', 5 * 1024 * 1024); // 5MB حداکثر اندازه فایل لاگ

/**
 * تابع اصلی لاگ‌گذاری
 * @param string $message پیام برای ثبت
 * @param string $level سطح لاگ (INFO, ERROR, WARNING, DEBUG)
 * @param array $context اطلاعات اضافی (اختیاری)
 */
function debug_log($message, $level = 'INFO', $context = []) {
    if (!DEBUG_LOG_ENABLED) {
        return;
    }
    
    // بررسی اندازه فایل و چرخش در صورت نیاز
    rotate_log_if_needed();
    
    // ساخت پیام لاگ
    $timestamp = date('Y-m-d H:i:s');
    $ip = $_SERVER['REMOTE_ADDR'] ?? 'CLI';
    $method = $_SERVER['REQUEST_METHOD'] ?? 'CLI';
    $uri = $_SERVER['REQUEST_URI'] ?? 'N/A';
    
    $log_entry = sprintf(
        "[%s] [%s] [IP:%s] [%s %s] %s",
        $timestamp,
        $level,
        $ip,
        $method,
        $uri,
        $message
    );
    
    // اضافه کردن context در صورت وجود
    if (!empty($context)) {
        $log_entry .= ' | Context: ' . json_encode($context, JSON_UNESCAPED_UNICODE);
    }
    
    $log_entry .= "\n";
    
    // نوشتن در فایل
    file_put_contents(DEBUG_LOG_FILE, $log_entry, FILE_APPEND | LOCK_EX);
}

/**
 * لاگ خطاهای PHP به صورت خودکار
 */
function debug_error_handler($errno, $errstr, $errfile, $errline) {
    $error_types = [
        E_ERROR => 'ERROR',
        E_WARNING => 'WARNING', 
        E_PARSE => 'ERROR',
        E_NOTICE => 'INFO',
        E_CORE_ERROR => 'ERROR',
        E_CORE_WARNING => 'WARNING',
        E_COMPILE_ERROR => 'ERROR',
        E_COMPILE_WARNING => 'WARNING',
        E_USER_ERROR => 'ERROR',
        E_USER_WARNING => 'WARNING',
        E_USER_NOTICE => 'INFO',
        E_STRICT => 'INFO',
        E_RECOVERABLE_ERROR => 'ERROR',
        E_DEPRECATED => 'INFO',
        E_USER_DEPRECATED => 'INFO'
    ];
    
    $level = $error_types[$errno] ?? 'ERROR';
    $message = "PHP {$level}: {$errstr} in {$errfile} on line {$errline}";
    
    debug_log($message, $level);
    
    // ادامه پردازش خطا توسط PHP
    return false;
}

/**
 * لاگ استثناهای PHP
 */
function debug_exception_handler($exception) {
    $message = sprintf(
        "Uncaught Exception: %s in %s on line %d\nStack trace:\n%s",
        $exception->getMessage(),
        $exception->getFile(),
        $exception->getLine(),
        $exception->getTraceAsString()
    );
    
    debug_log($message, 'ERROR');
}

/**
 * لاگ خطاهای مرگبار PHP
 */
function debug_fatal_error_handler() {
    $error = error_get_last();
    if ($error && in_array($error['type'], [E_ERROR, E_PARSE, E_CORE_ERROR, E_COMPILE_ERROR])) {
        $message = sprintf(
            "Fatal Error: %s in %s on line %d",
            $error['message'],
            $error['file'],
            $error['line']
        );
        debug_log($message, 'ERROR');
    }
}

/**
 * چرخش فایل لاگ در صورت بزرگ شدن
 */
function rotate_log_if_needed() {
    if (!file_exists(DEBUG_LOG_FILE)) {
        return;
    }
    
    if (filesize(DEBUG_LOG_FILE) > DEBUG_LOG_MAX_SIZE) {
        $backup_file = DEBUG_LOG_FILE . '.old';
        if (file_exists($backup_file)) {
            unlink($backup_file);
        }
        rename(DEBUG_LOG_FILE, $backup_file);
    }
}

/**
 * لاگ اطلاعات درخواست HTTP
 */
function debug_log_request() {
    $request_data = [
        'method' => $_SERVER['REQUEST_METHOD'] ?? 'N/A',
        'uri' => $_SERVER['REQUEST_URI'] ?? 'N/A',
        'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? 'N/A',
        'post_data' => $_POST,
        'get_data' => $_GET,
        'session_id' => session_id() ?: 'No session'
    ];
    
    debug_log('درخواست HTTP جدید', 'INFO', $request_data);
}

/**
 * لاگ اطلاعات پایگاه داده
 */
function debug_log_database($query, $params = [], $execution_time = null) {
    $context = [
        'query' => $query,
        'params' => $params
    ];
    
    if ($execution_time !== null) {
        $context['execution_time'] = $execution_time . 'ms';
    }
    
    debug_log('اجرای کوئری پایگاه داده', 'DEBUG', $context);
}

/**
 * لاگ خطاهای پایگاه داده
 */
function debug_log_database_error($error_message, $query = '', $params = []) {
    $context = [
        'error' => $error_message,
        'query' => $query,
        'params' => $params
    ];
    
    debug_log('خطای پایگاه داده', 'ERROR', $context);
}

// فعال‌سازی هندلرهای خطا
if (DEBUG_LOG_ENABLED) {
    set_error_handler('debug_error_handler');
    set_exception_handler('debug_exception_handler');
    register_shutdown_function('debug_fatal_error_handler');
    
    // لاگ شروع اسکریپت
    debug_log('شروع اجرای اسکریپت: ' . ($_SERVER['SCRIPT_NAME'] ?? 'Unknown'), 'INFO');
    
    // لاگ درخواست HTTP (فقط برای درخواست‌های وب)
    if (isset($_SERVER['REQUEST_METHOD'])) {
        debug_log_request();
    }
}

?>