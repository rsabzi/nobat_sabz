<?php
session_start();
require_once '../config.php';

// اگر کاربر قبلاً وارد شده، به داشبورد هدایت شود
if (isset($_SESSION['user_id'])) {
    header('Location: marketer_dashboard.html');
    exit;
}

// پردازش ورود
if ($_POST['action'] === 'login') {
    $telegram_id = $_POST['telegram_id'];
    
    try {
        // جستجوی کاربر
        $stmt = $pdo->prepare("SELECT * FROM users WHERE telegram_id = ?");
        $stmt->execute([$telegram_id]);
        $user = $stmt->fetch();
        
        if ($user) {
            // تنظیم session
            $_SESSION['user_id'] = $user['id'];
            $_SESSION['telegram_id'] = $user['telegram_id'];
            $_SESSION['first_name'] = $user['first_name'];
            $_SESSION['last_name'] = $user['last_name'];
            $_SESSION['username'] = $user['username'];
            
            // هدایت به داشبورد
            header('Location: marketer_dashboard.html');
            exit;
        } else {
            $error = "کاربر یافت نشد!";
        }
    } catch (PDOException $e) {
        $error = "خطا در اتصال به پایگاه داده: " . $e->getMessage();
    }
}
?>
<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ورود به سیستم</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #1E6FE0 0%, #0B4F9E 100%);
            margin: 0;
            padding: 0;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .login-container {
            background: white;
            padding: 2rem;
            border-radius: 10px;
            box-shadow: 0 10px 25px rgba(0,0,0,0.1);
            width: 100%;
            max-width: 400px;
        }
        .login-title {
            text-align: center;
            color: #333;
            margin-bottom: 2rem;
            font-size: 1.5rem;
        }
        .form-group {
            margin-bottom: 1rem;
        }
        label {
            display: block;
            margin-bottom: 0.5rem;
            color: #555;
            font-weight: 500;
        }
        input[type="text"] {
            width: 100%;
            padding: 0.75rem;
            border: 2px solid #ddd;
            border-radius: 5px;
            font-size: 1rem;
            transition: border-color 0.3s;
        }
        input[type="text"]:focus {
            outline: none;
            border-color: #1E6FE0;
        }
        .btn {
            width: 100%;
            padding: 0.75rem;
            background: linear-gradient(135deg, #1E6FE0 0%, #0B4F9E 100%);
            color: white;
            border: none;
            border-radius: 5px;
            font-size: 1rem;
            cursor: pointer;
            transition: transform 0.2s;
        }
        .btn:hover {
            transform: translateY(-2px);
        }
        .error {
            background: #fee;
            color: #c33;
            padding: 0.75rem;
            border-radius: 5px;
            margin-bottom: 1rem;
            text-align: center;
        }
        .test-info {
            background: #e8f5e8;
            color: #2d5a2d;
            padding: 1rem;
            border-radius: 5px;
            margin-bottom: 1rem;
            font-size: 0.9rem;
        }
    </style>
</head>
<body>
    <div class="login-container">
        <h1 class="login-title">ورود به داشبورد بازاریاب</h1>
        
        <div class="test-info">
            <strong>اطلاعات تست:</strong><br>
            شناسه تلگرام: 123456789<br>
            نام: علی احمدی
        </div>
        
        <?php if (isset($error)): ?>
            <div class="error"><?php echo htmlspecialchars($error); ?></div>
        <?php endif; ?>
        
        <form method="POST">
            <input type="hidden" name="action" value="login">
            
            <div class="form-group">
                <label for="telegram_id">شناسه تلگرام:</label>
                <input type="text" id="telegram_id" name="telegram_id" value="123456789" required>
            </div>
            
            <button type="submit" class="btn">ورود</button>
        </form>
    </div>
</body>
</html>