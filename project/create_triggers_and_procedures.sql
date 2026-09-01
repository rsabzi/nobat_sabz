-- فایل ایجاد Triggers و Stored Procedures برای سیستم امتیازدهی
-- این فایل باید بعد از اجرای create_customer_credit_system.sql اجرا شود
-- تاریخ ایجاد: 2025-01-08
-- نسخه: 1.0

-- حذف trigger موجود در صورت وجود
DROP TRIGGER IF EXISTS `tr_new_user_initial_credit`;

-- حذف stored procedure موجود در صورت وجود
DROP PROCEDURE IF EXISTS `UpdateCustomerCredit`;

-- حذف view موجود در صورت وجود
DROP VIEW IF EXISTS `customer_credit_report`;

-- ایجاد trigger برای اعطای امتیاز اولیه به کاربران جدید
CREATE TRIGGER `tr_new_user_initial_credit` 
AFTER INSERT ON `users`
FOR EACH ROW
BEGIN
    -- ایجاد رکورد امتیاز اولیه
    INSERT INTO `customer_credits` (`user_id`, `current_credit`, `total_earned`, `total_deducted`)
    VALUES (NEW.id, 10, 10, 0);
    
    -- ثبت تاریخچه امتیاز اولیه
    INSERT INTO `credit_history` (`user_id`, `change_type`, `credit_change`, `credit_before`, `credit_after`, `reason`)
    VALUES (NEW.id, 'initial', 10, 0, 10, 'امتیاز اولیه هنگام ثبت‌نام');
END;

-- ایجاد stored procedure برای به‌روزرسانی امتیاز مشتری
CREATE PROCEDURE `UpdateCustomerCredit`(
    IN p_user_id INT,
    IN p_credit_change INT,
    IN p_change_type ENUM('attendance','cancellation','no_show','manual_adjustment'),
    IN p_reason VARCHAR(255),
    IN p_booking_id INT DEFAULT NULL,
    IN p_admin_id INT DEFAULT NULL,
    IN p_business_id INT DEFAULT NULL
)
BEGIN
    DECLARE v_current_credit INT DEFAULT 0;
    DECLARE v_new_credit INT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    
    START TRANSACTION;
    
    -- دریافت امتیاز فعلی
    SELECT `current_credit` INTO v_current_credit 
    FROM `customer_credits` 
    WHERE `user_id` = p_user_id;
    
    -- محاسبه امتیاز جدید
    SET v_new_credit = v_current_credit + p_credit_change;
    
    -- جلوگیری از منفی شدن امتیاز
    IF v_new_credit < 0 THEN
        SET v_new_credit = 0;
    END IF;
    
    -- به‌روزرسانی امتیاز مشتری
    UPDATE `customer_credits` 
    SET 
        `current_credit` = v_new_credit,
        `total_earned` = `total_earned` + CASE WHEN p_credit_change > 0 THEN p_credit_change ELSE 0 END,
        `total_deducted` = `total_deducted` + CASE WHEN p_credit_change < 0 THEN ABS(p_credit_change) ELSE 0 END,
        `last_updated` = CURRENT_TIMESTAMP
    WHERE `user_id` = p_user_id;
    
    -- ثبت تاریخچه تغییر
    INSERT INTO `credit_history` (
        `user_id`, `booking_id`, `change_type`, `credit_change`, 
        `credit_before`, `credit_after`, `reason`, `admin_id`, `business_id`
    ) VALUES (
        p_user_id, p_booking_id, p_change_type, p_credit_change,
        v_current_credit, v_new_credit, p_reason, p_admin_id, p_business_id
    );
    
    COMMIT;
END;

-- ایجاد view برای گزارش‌گیری آسان
CREATE VIEW `customer_credit_report` AS
SELECT 
    u.id as user_id,
    u.first_name,
    u.last_name,
    u.username,
    u.phone,
    cc.current_credit,
    cc.total_earned,
    cc.total_deducted,
    cc.last_updated,
    (SELECT COUNT(*) FROM credit_history ch WHERE ch.user_id = u.id AND ch.change_type = 'attendance') as total_attendances,
    (SELECT COUNT(*) FROM credit_history ch WHERE ch.user_id = u.id AND ch.change_type = 'no_show') as total_no_shows,
    (SELECT COUNT(*) FROM credit_history ch WHERE ch.user_id = u.id AND ch.change_type = 'cancellation') as total_cancellations
FROM users u
LEFT JOIN customer_credits cc ON u.id = cc.user_id
WHERE u.is_active = 1
ORDER BY cc.current_credit DESC;

-- نمایش اطلاعات ایجاد شده
SELECT 'Triggers و Procedures ایجاد شدند!' as message;

-- پایان فایل