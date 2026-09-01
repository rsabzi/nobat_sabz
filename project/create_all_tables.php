<?php
// اسکریپت ایجاد تمام جداول در SQLite
require_once 'config.php';

try {
    // ایجاد جدول users
    $pdo->exec("
            CREATE TABLE IF NOT EXISTS users (
                id INT PRIMARY KEY AUTO_INCREMENT,
                telegram_id BIGINT NOT NULL UNIQUE,
                first_name TEXT NOT NULL,
                last_name TEXT,
                username TEXT,
                phone TEXT,
                email TEXT,
                is_active TINYINT DEFAULT 1,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        ");
    echo "جدول users ایجاد شد.\n";

    // ایجاد جدول marketers
    $pdo->exec("
            CREATE TABLE IF NOT EXISTS marketers (
                id INT PRIMARY KEY AUTO_INCREMENT,
                user_id INT NOT NULL,
                marketer_code VARCHAR(191) NOT NULL UNIQUE,
                total_businesses INT DEFAULT 0,
                total_bookings INT DEFAULT 0,
                commission_rate DOUBLE DEFAULT 0.0,
                total_commission DOUBLE DEFAULT 0.0,
                is_active TINYINT DEFAULT 1,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        ");
    echo "جدول marketers ایجاد شد.\n";

    // ایجاد جدول businesses
    $pdo->exec("
            CREATE TABLE IF NOT EXISTS businesses (
                id INT PRIMARY KEY AUTO_INCREMENT,
                owner_id INT NOT NULL,
                name TEXT NOT NULL,
                description TEXT,
                province TEXT,
                city TEXT,
                address TEXT,
                phone TEXT,
                email TEXT,
                website TEXT,
                business_type TEXT,
                slot_duration_minutes INT DEFAULT 30,
                advance_booking_days INT DEFAULT 14,
                marketer_id INT,
                is_active TINYINT DEFAULT 1,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (marketer_id) REFERENCES marketers(id) ON DELETE SET NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        ");
    echo "جدول businesses ایجاد شد.\n";

    // ایجاد جدول services
    $pdo->exec("
            CREATE TABLE IF NOT EXISTS services (
                id INT PRIMARY KEY AUTO_INCREMENT,
                business_id INT NOT NULL,
                name TEXT NOT NULL,
                description TEXT,
                duration_minutes INT NOT NULL,
                price DOUBLE DEFAULT 0.0,
                is_active TINYINT DEFAULT 1,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        ");
    echo "جدول services ایجاد شد.\n";

    // ایجاد جدول bookings
    $pdo->exec("
            CREATE TABLE IF NOT EXISTS bookings (
                id INT PRIMARY KEY AUTO_INCREMENT,
                business_id INT NOT NULL,
                user_id INT NOT NULL,
                service_id INT,
                booking_date DATE NOT NULL,
                start_time TIME NOT NULL,
                end_time TIME NOT NULL,
                duration_minutes INT NOT NULL,
                customer_name TEXT NOT NULL,
                customer_phone TEXT,
                customer_notes TEXT,
                status VARCHAR(50) DEFAULT 'pending',
                confirmation_code TEXT,
                total_price DOUBLE DEFAULT 0.0,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE SET NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        ");
    echo "جدول bookings ایجاد شد.\n";

    echo "تمام جداول با موفقیت ایجاد شدند!\n";

} catch (PDOException $e) {
    echo "خطا در ایجاد جداول: " . $e->getMessage() . "\n";
}
?>