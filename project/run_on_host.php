<?php
// اسکریپت جامع برای اجرا روی هاست
// اجرا: php run_on_host.php [--auto-create]

$autoCreate = (isset($argv) && in_array('--auto-create', $argv));

function parseConfigConstants($path, $keys = array())
{
    $found = array();
    if (!file_exists($path) || !is_readable($path)) {
        return $found;
    }
    $content = file_get_contents($path);
    foreach ($keys as $k) {
        $pattern = "/define\(\s*['\"]" . preg_quote($k, '/') . "['\"]\s*,\s*(['\"])(.*?)\\1\s*\)/s";
        if (preg_match($pattern, $content, $m)) {
            $found[$k] = $m[2];
        }
    }
    return $found;
}

function checkPhpEnvironment()
{
    echo "PHP version: " . phpversion() . PHP_EOL;
    $exts = array('pdo', 'pdo_mysql', 'pdo_sqlite');
    foreach ($exts as $e) {
        echo sprintf("Extension %-12s : %s\n", $e, extension_loaded($e) ? 'loaded' : 'missing');
    }
}

function tryConnectMySQL($overrides = array())
{
    $host = isset($overrides['DB_HOST']) ? $overrides['DB_HOST'] : (defined('DB_HOST') ? DB_HOST : null);
    $name = isset($overrides['DB_NAME']) ? $overrides['DB_NAME'] : (defined('DB_NAME') ? DB_NAME : null);
    $user = isset($overrides['DB_USER']) ? $overrides['DB_USER'] : (defined('DB_USER') ? DB_USER : null);
    $pass = isset($overrides['DB_PASS']) ? $overrides['DB_PASS'] : (defined('DB_PASS') ? DB_PASS : '');
    $charset = isset($overrides['DB_CHARSET']) ? $overrides['DB_CHARSET'] : (defined('DB_CHARSET') ? DB_CHARSET : 'utf8mb4');

    if (!$host || !$name || !$user) {
        return array(false, 'تنظیمات MySQL ناقص هستند.');
    }
    $dsn = "mysql:host={$host};dbname={$name};charset={$charset}";
    try {
        $options = array(
            PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES   => false,
        );
        $pdo = new PDO($dsn, $user, $pass, $options);
        $pdo->exec("SET NAMES " . $charset);
        return array($pdo, null);
    } catch (PDOException $e) {
        return array(false, $e->getMessage());
    }
}

function tableExists(PDO $pdo, $table)
{
    $driver = $pdo->getAttribute(PDO::ATTR_DRIVER_NAME);
    if ($driver === 'mysql') {
        $stmt = $pdo->prepare("SELECT TABLE_NAME FROM information_schema.tables WHERE table_schema = :schema AND table_name = :t LIMIT 1");
        $schema = defined('DB_NAME') ? DB_NAME : null;
        $stmt->execute(array(':schema' => $schema, ':t' => $table));
        return (bool)$stmt->fetchColumn();
    } elseif ($driver === 'sqlite') {
        $stmt = $pdo->prepare("SELECT name FROM sqlite_master WHERE type='table' AND name = :t LIMIT 1");
        $stmt->execute(array(':t' => $table));
        return (bool)$stmt->fetchColumn();
    }
    return false;
}

function createTablesMySQL(PDO $pdo)
{
    $stmts = array();
    $stmts[] = "CREATE TABLE IF NOT EXISTS users (
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    $stmts[] = "CREATE TABLE IF NOT EXISTS marketers (
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    $stmts[] = "CREATE TABLE IF NOT EXISTS businesses (
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    $stmts[] = "CREATE TABLE IF NOT EXISTS services (
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    $stmts[] = "CREATE TABLE IF NOT EXISTS bookings (
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    try {
        $pdo->beginTransaction();
        foreach ($stmts as $s) {
                $pdo->exec($s);
                echo "Executed: " . substr($s, 0, 40) . "...\n";
        }
        $pdo->commit();
        echo "تمام جداول ساخته شدند یا از قبل موجود بودند.\n";
        return true;
    } catch (PDOException $e) {
        $pdo->rollBack();
        echo "خطا در ایجاد جداول: " . $e->getMessage() . PHP_EOL;
        return false;
    }
}

// اجرا
echo "--- شروع بررسی محیط ---\n";
checkPhpEnvironment();

$configPath = dirname(__FILE__) . '/config.php';
$parsed = parseConfigConstants($configPath, array('DB_HOST', 'DB_NAME', 'DB_USER', 'DB_PASS', 'DB_CHARSET', 'DB_PATH'));
if (!empty($parsed)) {
    echo "تنظیمات از config.php استخراج شد.\n";
} else {
    echo "قادر به خواندن تنظیمات از config.php نبودم؛ لطفاً آن را بررسی کنید.\n";
}

echo "
--- تلاش برای اتصال به MySQL ---\n";
list($pdo, $err) = tryConnectMySQL($parsed);
if ($pdo && $pdo instanceof PDO) {
    echo "اتصال به MySQL موفق بود.\n";
    // بررسی جداول
    $expected = array('users','marketers','businesses','services','bookings');
    $missing = array();
    foreach ($expected as $t) {
        if (!tableExists($pdo, $t)) $missing[] = $t;
    }
    if (empty($missing)) {
        echo "تمام جداول موجود هستند.\n";
    } else {
        echo "جداول مفقود: " . implode(', ', $missing) . "\n";
        if ($autoCreate) {
            echo "--auto-create مشخص شده؛ ساخت جداول...\n";
            createTablesMySQL($pdo);
        } else {
            echo "برای ساخت خودکار جداول، اسکریپت را با فلگ --auto-create اجرا کنید.\n";
        }
    }
    exit(0);
} else {
    echo "اتصال به MySQL شکست خورد: $err\n";
    echo "--- تلاش برای اتصال به SQLite (در صورت وجود DB_PATH) ---\n";
    $dbPath = isset($parsed['DB_PATH']) ? $parsed['DB_PATH'] : (defined('DB_PATH') ? DB_PATH : null);
    if ($dbPath && file_exists($dbPath)) {
        try {
            $pdo = new PDO('sqlite:' . $dbPath);
            $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            echo "اتصال به SQLite موفق بود.\n";
            $expected = array('users','marketers','businesses','services','bookings');
            $missing = array();
            foreach ($expected as $t) {
                if (!tableExists($pdo, $t)) $missing[] = $t;
            }
            if (empty($missing)) {
                echo "تمام جداول موجود هستند (SQLite).\n";
            } else {
                echo "جداول مفقود در SQLite: " . implode(', ', $missing) . "\n";
                echo "ایجاد خودکار جداول SQLite پشتیبانی ناقص است؛ در صورت نیاز از نسخه‌ی MySQL استفاده کنید یا جداول را دستی بسازید.\n";
            }
            exit(0);
        } catch (PDOException $e) {
            echo "اتصال به SQLite ناموفق: " . $e->getMessage() . "\n";
            exit(1);
        }
    } else {
        echo "مسیر SQLite موجود نیست یا تعریف نشده است: " . ($dbPath ?: '(خالی)') . "\n";
        echo "هیچ اتصال پایگاه داده‌ای برقرار نشد؛ لطفاً تنظیمات `config.php` و دسترسی‌ها را بررسی کنید.\n";
        exit(1);
    }
}

?>
