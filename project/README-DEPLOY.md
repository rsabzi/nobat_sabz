# راهنمای جایگذاری فایل‌ها روی هاست

ساختار این فایل فشرده دقیقاً همان ساختار پوشه‌های پروژه‌ی شماست. فقط محتوای هرکدام
را جایگزین فایل هم‌نامش روی هاست کنید (بهتر است قبلش یک نسخهٔ پشتیبان از فایل‌های
فعلی بگیرید).

```
config.php              → /project/config.php   (جایگزین فایل فعلی)
bot/webhook.php         → /project/bot/webhook.php
webapp/api.php          → /project/webapp/api.php
webapp/customer_credit_api.php → /project/webapp/customer_credit_api.php
webapp/search.html      → /project/webapp/search.html
webapp/booking.html     → /project/webapp/booking.html
webapp/index.html       → /project/webapp/index.html
webapp/edit.html        → /project/webapp/edit.html
webapp/services.html    → /project/webapp/services.html
webapp/schedule.html    → /project/webapp/schedule.html
webapp/subscription.html→ /project/webapp/subscription.html
```

مسیر دقیق روی سرور شما ممکن است با `/project` شروع نشود — همان مسیری که
`BASE_URL` در `config.php` فعلی‌تان به آن اشاره می‌کند را مبنا قرار دهید.

## ⚠️ نکتهٔ حیاتی: این ۱۱ فایل باید همزمان جایگزین شوند

اگر فقط بعضی از فایل‌ها را عوض کنید، ممکن است چیزی خراب شود:

- **`config.php` را حتماً با `bot/webhook.php` و `webapp/customer_credit_api.php`
  با هم عوض کنید.** این سه فایل یک سکرت مشترک جدید (`INTERNAL_API_SECRET`) دارند؛
  اگر یکی را جا بیندازید، ثبت حضور/غیاب مشتری در ربات با خطا مواجه می‌شود.
- بقیهٔ فایل‌های `.html` مستقل از هم هستند و می‌شود هرکدام را جدا هم جایگزین کرد،
  ولی برای یکدستی طراحی بهتر است همه با هم بروند.

## چه چیزی در هرکدام تغییر کرد (خلاصه)

| فایل | نوع تغییر |
|---|---|
| `config.php` | + ثابت جدید `INTERNAL_API_SECRET` |
| `bot/webhook.php` | بازطراحی کامل UI (رنگ دکمه‌ها طبق Bot API 9.4، متن‌ها) + ارسال سکرت داخلی |
| `webapp/api.php` | رفع نشت اطلاعات مشتری در `get_active_queue` |
| `webapp/customer_credit_api.php` | افزودن احراز هویت (قبلاً کاملاً باز بود) |
| `webapp/search.html` | بازطراحی کامل UI/UX |
| `webapp/booking.html` | بازطراحی کامل UI/UX (جریان گام‌به‌گام) |
| `webapp/index.html` | بازطراحی کامل UI/UX + حذف لاگ‌های دیباگ اضافی |
| `webapp/edit.html` | بازطراحی کامل UI/UX + رفع باگ نام بات و دادهٔ ناقص استان/شهر |
| `webapp/services.html` | بازطراحی کامل UI/UX |
| `webapp/schedule.html` | بازطراحی کامل UI/UX + حذف کتابخانه‌های بلااستفاده |
| `webapp/subscription.html` | بازطراحی کامل UI/UX |

## کاری که این‌جا انجام نشد (هنوز یادتان باشد)

چرخاندن سکرت‌های افشاشده (توکن بات، پسورد دیتابیس، پسورد ادمین، کلیدهای ووکامرس) —
این کد نیست، باید روی پنل هاست/BotFather/ووکامرس خودتان انجام شود.
