# 🤖 ربات تلگرام نوبت‌سبز (SabzTime Bot)
## سیستم جامع مدیریت نوبت‌دهی و کسب‌وکار

[![Version](https://img.shields.io/badge/version-2.2-blue.svg)](https://github.com/your-repo/sabztime-bot)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![PHP](https://img.shields.io/badge/PHP-7.4%2B-purple.svg)](https://php.net)
[![MySQL](https://img.shields.io/badge/MySQL-5.7%2B-orange.svg)](https://mysql.com)

---

## 📋 فهرست مطالب

- [معرفی پروژه](#-معرفی-پروژه)
- [ویژگی‌ها](#-ویژگی‌ها)
- [پیش‌نیازها](#-پیش‌نیازها)
- [نصب و راه‌اندازی](#-نصب-و-راه‌اندازی)
- [پیکربندی](#-پیکربندی)
- [استفاده](#-استفاده)
- [API مستندات](#-api-مستندات)
- [عیب‌یابی](#-عیب‌یابی)
- [مشارکت](#-مشارکت)
- [پشتیبانی](#-پشتیبانی)

---

## 🚀 معرفی پروژه

**نوبت‌سبز** یک سیستم جامع مدیریت نوبت‌دهی و کسب‌وکار است که به صورت ربات تلگرام و وب‌اپ پیشرفته ارائه می‌شود. این سیستم به کسب‌وکارها کمک می‌کند تا نوبت‌دهی، مدیریت خدمات، و ارتباط با مشتریان خود را به صورت کاملاً خودکار و حرفه‌ای انجام دهند.

### 🎯 هدف پروژه
- ساده‌سازی فرآیند نوبت‌دهی برای کسب‌وکارها
- ارائه تجربه کاربری عالی در پلتفرم تلگرام
- مدیریت جامع خدمات و زمان‌بندی
- سیستم پرداخت و اشتراک یکپارچه

---

## ✨ ویژگی‌ها

### 🏢 مدیریت کسب‌وکار
- ✅ ثبت و ویرایش اطلاعات کسب‌وکار
- ✅ مدیریت چندین شعبه
- ✅ آپلود تصاویر و گالری
- ✅ مدیریت ساعات کاری
- ✅ تنظیمات پیشرفته نوبت‌دهی

### 🛠️ مدیریت خدمات
- ✅ تعریف خدمات متنوع
- ✅ قیمت‌گذاری انعطاف‌پذیر
- ✅ مدت زمان خدمات
- ✅ دسته‌بندی خدمات
- ✅ خدمات ویژه و تخفیف‌دار

### 📅 سیستم زمان‌بندی
- ✅ تقویم پیشرفته
- ✅ مدیریت اسلات‌های زمانی
- ✅ رزرو آنلاین
- ✅ یادآوری خودکار
- ✅ لغو و تغییر نوبت

### 🔍 جستجوی پیشرفته
- ✅ جستجو بر اساس موقعیت
- ✅ فیلتر بر اساس دسته‌بندی
- ✅ مرتب‌سازی نتایج
- ✅ کش هوشمند
- ✅ جستجوی فول‌تکست

### 💳 سیستم پرداخت
- ✅ ادغام با WooCommerce
- ✅ درگاه ZarinPal
- ✅ پکیج‌های اشتراک
- ✅ مدیریت کیف پول
- ✅ گزارش‌گیری مالی

### 👥 سیستم بازاریابی
- ✅ پنل بازاریاب
- ✅ سیستم کمیسیون
- ✅ لینک‌های ارجاع
- ✅ آمار فروش
- ✅ پرداخت خودکار کمیسیون

### 🛡️ امنیت و کیفیت
- ✅ احراز هویت چندلایه
- ✅ محافظت از SQL Injection
- ✅ رمزنگاری داده‌ها
- ✅ لاگ‌گذاری جامع
- ✅ مانیتورینگ سیستم

---

## 📋 پیش‌نیازها

### سرور
- **PHP**: 7.4 یا بالاتر
- **MySQL**: 5.7 یا بالاتر
- **Apache/Nginx**: با پشتیبانی از .htaccess
- **SSL Certificate**: برای HTTPS
- **cURL**: برای ارتباط با API

### خدمات خارجی
- **ربات تلگرام**: از [@BotFather](https://t.me/botfather)
- **WooCommerce** (اختیاری): برای فروشگاه
- **ZarinPal** (اختیاری): برای پرداخت

### دسترسی‌ها
- **دسترسی SSH**: برای نصب
- **دسترسی FTP/SFTP**: برای آپلود فایل‌ها
- **دسترسی phpMyAdmin**: برای مدیریت دیتابیس

---

## 🔧 نصب و راه‌اندازی

### مرحله 1: دانلود و آپلود
```bash
# دانلود پروژه
git clone https://github.com/your-repo/sabztime-bot.git
cd sabztime-bot

# آپلود به سرور
# فایل‌ها را در root directory وب‌سایت قرار دهید
```

### مرحله 2: ایجاد پایگاه داده
```sql
-- ایجاد دیتابیس
CREATE DATABASE sabztime_bot CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ایجاد کاربر
CREATE USER 'sabztime_user'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON sabztime_bot.* TO 'sabztime_user'@'localhost';
FLUSH PRIVILEGES;
```

### مرحله 3: اجرای اسکریپت‌های SQL
```bash
# اجرای جدول‌های اصلی
php create_all_tables.php

# اجرای جدول‌های WooCommerce (در صورت نیاز)
php create_woocommerce_tables.php

# درج داده‌های نمونه (اختیاری)
php insert_test_data.php
```

### مرحله 4: پیکربندی فایل‌ها
```bash
# کپی و ویرایش فایل پیکربندی
cp config.php.example config.php
cp webapp/woocommerce_config.php.example webapp/woocommerce_config.php

# تنظیم مجوزها
chmod 755 webapp/
chmod 644 webapp/*.php
chmod 777 webapp/cache/
chmod 777 logs/
```

---

## ⚙️ پیکربندی

### فایل config.php
```php
<?php
// تنظیمات ربات تلگرام
define('BOT_TOKEN', 'YOUR_BOT_TOKEN');
define('BOT_USERNAME', 'your_bot_username');

// تنظیمات پایگاه داده
define('DB_HOST', 'localhost');
define('DB_NAME', 'sabztime_bot');
define('DB_USER', 'sabztime_user');
define('DB_PASS', 'your_secure_password');

// آدرس‌های وب‌اپ
define('BASE_URL', 'https://yourdomain.com');
define('WEBAPP_URL', BASE_URL . '/webapp/index.html');

// تنظیمات امنیتی
define('DEV_MODE', false); // false برای تولید
?>
```

### تنظیم Webhook
```bash
# اجرای اسکریپت تنظیم webhook
php bot/set_webhook.php
```

### تست اتصال
```bash
# تست اتصال دیتابیس
php test_db_connection.php

# بررسی وضعیت پروژه
php project_status.php
```

---

## 📱 استفاده

### برای کسب‌وکارها
1. **شروع کار**: `/start` در ربات
2. **ثبت کسب‌وکار**: استفاده از وب‌اپ
3. **مدیریت خدمات**: پنل مدیریت خدمات
4. **تنظیم زمان‌بندی**: تقویم و اسلات‌ها
5. **مشاهده آمار**: گزارش‌های جامع

### برای مشتریان
1. **جستجو**: یافتن کسب‌وکار مناسب
2. **رزرو**: انتخاب زمان و خدمت
3. **پرداخت**: تکمیل فرآیند پرداخت
4. **مدیریت نوبت**: مشاهده و تغییر نوبت‌ها

### برای بازاریابان
1. **ثبت‌نام**: درخواست بازاریابی
2. **دریافت لینک**: لینک اختصاصی
3. **معرفی مشتری**: کسب کمیسیون
4. **مشاهده آمار**: پنل بازاریاب

---

## 📚 API مستندات

### Authentication
```http
POST /webapp/api.php
Content-Type: application/json
Authorization: Bearer {telegram_data}
```

### Endpoints اصلی

#### ثبت کسب‌وکار
```http
POST /webapp/api.php
{
    "action": "add_business",
    "name": "نام کسب‌وکار",
    "business_type": "نوع کسب‌وکار",
    "province": "استان",
    "city": "شهر"
}
```

#### جستجو
```http
GET /webapp/search_api.php?action=search&query=کلمه&city=شهر
```

#### مدیریت خدمات
```http
POST /webapp/api.php
{
    "action": "add_service",
    "business_id": "123",
    "name": "نام خدمت",
    "price": 50000,
    "duration": 30
}
```

### Response Format
```json
{
    "status": "success|error",
    "message": "پیام",
    "data": {},
    "timestamp": "2025-01-27T10:00:00Z"
}
```

---

## 🐛 عیب‌یابی

### مشکلات رایج

#### خطای 500
```bash
# بررسی لاگ‌ها
tail -f webapp/debug.log
tail -f logs/error.log

# بررسی مجوزها
ls -la webapp/
```

#### مشکل اتصال دیتابیس
```bash
# تست اتصال
php test_db_connection.php

# بررسی تنظیمات
cat config.php | grep DB_
```

#### مشکل Webhook
```bash
# تنظیم مجدد webhook
php bot/set_webhook.php

# بررسی وضعیت
curl -X GET "https://api.telegram.org/bot{TOKEN}/getWebhookInfo"
```

### لاگ‌ها
- **خطاهای عمومی**: `logs/error.log`
- **خطاهای دیباگ**: `webapp/debug.log`
- **لاگ جستجو**: `webapp/search.log`
- **لاگ WooCommerce**: `webapp/logs/woocommerce.log`

### ابزارهای مفید
```bash
# مشاهده آمار سیستم
php project_status.php

# پاک‌سازی کش
php -r "array_map('unlink', glob('webapp/cache/*.cache'));"

# بهینه‌سازی دیتابیس
php webapp/search_admin.php
```

---

## 🤝 مشارکت

### نحوه مشارکت
1. Fork کردن پروژه
2. ایجاد branch جدید (`git checkout -b feature/amazing-feature`)
3. Commit تغییرات (`git commit -m 'Add amazing feature'`)
4. Push به branch (`git push origin feature/amazing-feature`)
5. ایجاد Pull Request

### استانداردهای کد
- **PSR-12**: برای PHP
- **UTF-8**: برای تمام فایل‌ها
- **فارسی**: برای کامنت‌ها و مستندات
- **Semantic Versioning**: برای نسخه‌گذاری

### تست
```bash
# اجرای تست‌ها
php tests/run_tests.php

# تست امنیتی
php tests/security_test.php
```

---

## 📞 پشتیبانی

### راه‌های ارتباطی
- **تلگرام**: [@sabztime_support](https://t.me/sabztime_support)
- **ایمیل**: support@sabztime.ir
- **وب‌سایت**: [https://sabztime.ir](https://sabztime.ir)

### مستندات بیشتر
- [راهنمای نصب WooCommerce](WOOCOMMERCE_SETUP_GUIDE.md)
- [راهنمای MySQL](MYSQL_INSTALLATION_GUIDE.md)
- [راهنمای عیب‌یابی](TROUBLESHOOTING.md)
- [تقییم آمادگی بازار](MARKET_READINESS_ASSESSMENT.md)

### FAQ
**س: آیا می‌توان بدون WooCommerce استفاده کرد؟**
ج: بله، سیستم پرداخت ZarinPal مستقل عمل می‌کند.

**س: حداکثر تعداد کسب‌وکار چقدر است؟**
ج: محدودیتی وجود ندارد، بستگی به منابع سرور دارد.

**س: آیا از چند زبان پشتیبانی می‌کند؟**
ج: در حال حاضر فقط فارسی، اما قابلیت توسعه وجود دارد.

---

## 📄 مجوز

این پروژه تحت مجوز MIT منتشر شده است. برای جزئیات بیشتر فایل [LICENSE](LICENSE) را مطالعه کنید.

---

## 🙏 تشکر

از تمامی توسعه‌دهندگان، تست‌کنندگان، و کاربرانی که در بهبود این پروژه مشارکت داشته‌اند، صمیمانه تشکر می‌کنیم.

---

**نسخه**: 2.2  
**آخرین به‌روزرسانی**: 2025-01-27  
**وضعیت**: آماده تولید ✅

---

<div align="center">
  <strong>ساخته شده با ❤️ برای جامعه کسب‌وکارهای ایران</strong>
</div>