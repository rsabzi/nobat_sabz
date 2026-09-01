<?php
// Simple test to verify getBotDatabaseConnection() after the patch
require_once __DIR__ . '/webapp/woocommerce_config.php';

try {
    $conn = getBotDatabaseConnection();
    if ($conn instanceof PDO) {
        $stmt = $conn->query('SELECT DATABASE() as db');
        $dbName = $stmt->fetchColumn();
        echo "OK: connected to database: " . ($dbName ?: '(unknown)') . PHP_EOL;
    } else {
        echo "ERROR: getBotDatabaseConnection() did not return a PDO instance." . PHP_EOL;
    }
} catch (Exception $e) {
    echo "Connection failed: " . $e->getMessage() . PHP_EOL;
}
