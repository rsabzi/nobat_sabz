<?php
session_start();
require_once '../config.php';

// احراز هویت ادمین با استفاده از تنظیمات سراسری در config.php
$ADMIN_USERNAME = defined('ADMIN_USERNAME') ? ADMIN_USERNAME : (getenv('ADMIN_USERNAME') ?: 'admin');
$ADMIN_PASSWORD = defined('ADMIN_PASSWORD') ? ADMIN_PASSWORD : (getenv('ADMIN_PASSWORD') ?: 'change-this-strong-pass');

// اگر قبلا وارد شده باشد
if (isset($_SESSION['is_admin']) && $_SESSION['is_admin'] === true) {
    header('Location: admin_dashboard.php');
    exit;
}

$error = null;
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $username = $_POST['username'] ?? '';
    $password = $_POST['password'] ?? '';
    if ($username === $ADMIN_USERNAME && hash_equals($ADMIN_PASSWORD, $password)) {
        $_SESSION['is_admin'] = true;
        $_SESSION['admin_username'] = $username;
        header('Location: admin_dashboard.php');
        exit;
    } else {
        $error = 'نام کاربری یا رمز عبور نادرست است';
    }
}
?>
<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ورود مدیر</title>
    <style>
        body{font-family:Segoe UI,Tahoma,Geneva,Verdana,sans-serif;background:linear-gradient(135deg,#141e30,#243b55);min-height:100vh;display:flex;align-items:center;justify-content:center;margin:0}
        .box{background:#fff;padding:2rem;border-radius:12px;box-shadow:0 20px 50px rgba(0,0,0,.2);width:100%;max-width:400px}
        h1{margin:0 0 1rem;color:#222;font-size:1.4rem;text-align:center}
        .error{background:#fee;color:#b00;padding:.75rem;border-radius:8px;margin-bottom:1rem;text-align:center}
        .field{margin-bottom:1rem}
        label{display:block;margin-bottom:.5rem;color:#444}
        input{width:100%;padding:.75rem;border:2px solid #ddd;border-radius:8px;font-size:1rem}
        input:focus{outline:none;border-color:#243b55}
        button{width:100%;padding:.8rem;background:#243b55;color:#fff;border:none;border-radius:8px;font-size:1rem;cursor:pointer}
        button:hover{background:#1b2a41}
    </style>
</head>
<body>
    <div class="box">
        <h1>ورود مدیر</h1>
        <?php if ($error): ?><div class="error"><?php echo htmlspecialchars($error); ?></div><?php endif; ?>
        <form method="post">
            <div class="field">
                <label>نام کاربری</label>
                <input type="text" name="username" required>
            </div>
            <div class="field">
                <label>رمز عبور</label>
                <input type="password" name="password" required>
            </div>
            <button type="submit">ورود</button>
        </form>
    </div>
</body>
</html>
