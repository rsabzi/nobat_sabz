<?php
// File: webapp/privacy.php — سیاست حریم خصوصی نوبت‌سبز
// صفحه‌ی مستقل (بدون وابستگی به دیتابیس) — الزام انتشار در گوگل‌پلی و کافه‌بازار
header('Content-Type: text/html; charset=utf-8');
$updated = '۱۴۰۵/۰۶/۱۳';
?>
<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>حریم خصوصی — نوبت‌سبز</title>
<style>
  :root { --brand:#0B4F9E; --brand-dark:#083A76; --green:#15803D; --bg:#F5F7FA; --text:#1B2430; }
  * { box-sizing: border-box; }
  body { margin:0; font-family: Vazirmatn, Tahoma, "Segoe UI", sans-serif; background:var(--bg); color:var(--text); line-height:2; }
  .hero { background:linear-gradient(135deg, #1E6FE0, var(--brand) 55%, var(--brand-dark)); color:#fff; padding:40px 20px 32px; text-align:center; }
  .hero h1 { margin:0 0 6px; font-size:1.6rem; }
  .hero p { margin:0; opacity:.85; font-size:.95rem; }
  main { max-width:760px; margin:-18px auto 40px; padding:0 16px; }
  section { background:#fff; border-radius:18px; padding:20px 22px; margin:14px 0; box-shadow:0 6px 24px rgba(11,79,158,.08); }
  h2 { color:var(--brand); font-size:1.1rem; margin:0 0 8px; display:flex; align-items:center; gap:8px; }
  ul { margin:8px 0; padding-right:20px; }
  li { margin:4px 0; }
  .ok { color:var(--green); font-weight:bold; }
  .updated { text-align:center; color:#6B7686; font-size:.85rem; margin-top:18px; }
</style>
</head>
<body>
<div class="hero">
  <h1>🔒 سیاست حریم خصوصی</h1>
  <p>نوبت‌سبز — رزرو نوبت، ساده و حرفه‌ای</p>
</div>
<main>
  <section>
    <h2>۱) چه داده‌هایی از شما دریافت می‌کنیم؟</h2>
    <ul>
      <li><b>نام و شماره موبایل</b> — برای ساخت حساب و شناسایی شما هنگام ورود.</li>
      <li><b>اطلاعات نوبت‌ها</b> — نام و شماره‌ای که هنگام رزرو وارد می‌کنید تا صاحب کسب‌وکار بتواند نوبت شما را مدیریت کند.</li>
      <li><b>داده‌های فنی</b> — نوع دستگاه و شناسه‌ی نشست (توکن) برای نگه‌داشتن ورود شما.</li>
    </ul>
    <p>ما <b>هیچ‌گاه</b> رمز عبور شما را به صورت خام ذخیره نمی‌کنیم و به هیچ بانک اطلاعاتی، کارت بانکی یا مخاطبین گوشی شما دسترسی نداریم.</p>
  </section>

  <section>
    <h2>۲) چرا این داده‌ها را نگه می‌داریم؟</h2>
    <ul>
      <li>اجرا و شخصی‌سازی سرویس رزرو نوبت</li>
      <li>ارسال یادآور نوبت‌های پیش‌رو</li>
      <li>پشتیبانی و رسیدگی به مشکلات</li>
    </ul>
  </section>

  <section>
    <h2>۳) این داده‌ها با کسی به اشتراک گذاشته می‌شود؟</h2>
    <p><span class="ok">خیر.</span> داده‌های شما فقط برای اجرای سرویس استفاده می‌شود؛ فروخته یا اجاره داده نمی‌شود. فقط صاحبِ همان کسب‌وکاری که در آن نوبت گرفته‌اید، اطلاعات نوبت شما را می‌بیند.</p>
  </section>

  <section>
    <h2>۴) تا کی نگه‌داری می‌شود و حق حذف شما</h2>
    <p>داده‌ها تا زمانی که حساب شما فعال باشد نگهداری می‌شوند. شما هر لحظه می‌توانید از مسیر
    <b>پروفایل ← حذف حساب کاربری</b> در خود اپ، حساب و <b>تمام</b> داده‌های مرتبط (نوبت‌ها، کسب‌وکارها و امتیازها) را برای همیشه حذف کنید. این عمل قابل بازگشت نیست.</p>
  </section>

  <section>
    <h2>۵) امنیت</h2>
    <ul>
      <li>رمز عبور با الگوریتم استاندارد یک‌طرفه (bcrypt) ذخیره می‌شود؛ حتی خود ما هم نمی‌توانیم آن را بخوانیم.</li>
      <li>ارتباط اپ و سرور همیشه رمزنگاری‌شده (HTTPS) است.</li>
      <li>توکن نشست روی دستگاه شما در محیط امن (Android Keystore) نگهداری می‌شود.</li>
    </ul>
  </section>

  <section>
    <h2>۶) تماس با ما</h2>
    <p>هر سؤال یا درخواستی درباره‌ی داده‌های خود دارید، از طریق ربات تلگرام
    <b><a href="https://t.me/Sabztime_bot" style="color:var(--brand)">@Sabztime_bot</a></b> یا پشتیبانی اپ با ما در ارتباط باشید.</p>
  </section>

  <p class="updated">آخرین به‌روزرسانی: <?php echo $updated; ?></p>
</main>
</body>
</html>
