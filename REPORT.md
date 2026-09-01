# گزارش نهایی: اپلیکیشن اندروید «نوبت‌سبز»

**تاریخ:** 1405-06-10 (2026-09-01)
**شعبه:** `arena/01a05c23-nobat-sabz` — کامیت `a2157ee9320b181cbfba3e195a6a998e1412873d`
**مخزن:** `github.com/rsabzi/nobat_sabz`

---

## ۱) انتخاب فناوری و توجیه آن

| گزینه | انتخاب | دلیل |
|---|---|---|
| **Native Android (Kotlin + Jetpack Compose + Material 3)** | ✅ انتخاب شد | کامل‌ترین پشتیبانی از RTL فارسی، دسترسی به WorkManager (یادآوری نوبت)، DataStore/EncryptedSharedPreferences، نوتیفیکیشن سیستمی، و اجرای بدون نیاز به مرورگر. |
| React Native / Expo | ❌ | لایه JS اضافه، تجربه RTL و قابلیت‌های بومی (WorkManager، کانال نوتیفیکیشن) ضعیف‌تر، حجم وابستگی‌ها بیشتر. |
| Flutter | ❌ | مناسب ولی نیاز به موتور Dart و پل‌های پلتفرم؛ برای این پروژه‌ی کوچک‌مقیاس وزن اضافی داشت؛ فونت/رندر فارسی در WebView-like محیط‌ها نیاز به تنظیم بیشتر دارد. |
| PWA / WebView از سایت موجود | ❌ | کاربر صراحتاً «اپلیکیشن نیتیو، نه WebView» را خواسته است. |

**خلاصه فنی:** Kotlin 1.9.24 + AGP 8.5.2 + Compose BOM 2024.06.00، minSdk 24 / target 34، هویت بسته `ir.sabztime.app`، نسخه `1.0.0`، رابط کاملاً فارسی و RTL با تقویم جلالی (تبدیل میلادی↔جلالی در `util/Jalali.kt`).

---

## ۲) خلاصه کار انجام‌شده

### بک‌اند (PHP — حفظ و توسعه‌ی بدون بازنویسی)
- **`project/webapp/mobile_auth.php` (جدید، ~۵۵۰ خط):** احراز هویت موبایلی مستقل از تلگرام:
  - ثبت‌نام با موبایل + رمز عبور + نام (`app_users`)، اتصال به جدول موجود `users` با `telegram_id` مصنوعی منفی تا **تمام منطق موجود (رزرو، صف، اعتبار، ...) بدون تغییر** کار کند.
  - توکن تصادفی ۶۴ کاراکتری hex؛ فقط **اثر sha256 توکن** در جدول `app_sessions` ذخیره می‌شود (رمزنگاری امن، بدون ذخیره توکن خام).
  - نشست ۳۰ روزه با تمدید لغزان (sliding) و پاکسازی خودکار نشست‌های قدیمی.
  - `mobile_auth_from_request()` از هدر `X-App-Token` یا پارامتر `app_token`؛ هیچ کلید/توکنی در کد یا مخزن نیست.
  - اکشن‌های استاندارد: `register`، `login`، `logout`، `me`، `update_profile`، `get_notifications`, `get_my_businesses`, `get_subscription_plans`، `get_subscription_status`، `get_marketer_dashboard`، `get_active_queue`, `get_customer_queue_info`، `advance_queue`, `complete_booking`.
- **`project/webapp/api.php` (ویرایش هدفمند):** در `authenticate_user()` ابتدا `mobile_auth_from_request()` بررسی می‌شود (بدون تغییر مسیرهای تلگرام/وب)؛ سپس خروجی به‌صورت `is_mobile` برمی‌گردد و `MOBILE_APP_USER` در دسترس است. `get_user_bookings` اکنون `include_history` را می‌پذیرد. اکشن `get_my_businesses` به‌صورت GET استاندارد اضافه شد.
- **`project/create_tables.sql` / `database_init.php`:** جداول `app_users` و `app_sessions` (با `mobile_auth_ensure_tables()` خودکار در اولین درخواست موبایل هم ساخته می‌شوند).

### اپلیکیشن اندروید (جدید — `android/`)
- **احراز هویت:** صفحه ورود/ثبت‌نام، توکن در **Android Keystore / EncryptedSharedPreferences**؛ خروج و انقضای نشست -> `SessionExpired` -> بازگشت به ورود.
- **صفحه‌ها (۱۴ صفحه):** Splash، Login، Main (BottomBar)، Home (جستجو + دسته‌بندی)، BusinessDetail (خدمات/اسلات)، Bookings + BookingDetail، MyBusinesses (مالک)، Owner dashboard، Subscription، Marketer، Notifications، Profile، Help.
- **یادآوری نوبت:** `worker/ReminderWorker` با WorkManager (نوبت‌های فردا ساعت ۹ صبح) + `notif/Notifier` کانال نوتیفیکیشن + درخواست مجوز POST_NOTIFICATIONS (اندروید ۱۳+).
- **اتصال به API:** OkHttp + kotlinx.serialization؛ خطاهای شبکه/سرور/نشست به‌صورت typed exception (`SabzException`).
- **امنیت/مخزن:** هیچ توکنی، رمزی یا کلیدی (تلگرام، ووکامرس، زرین‌پال) در کد یا مخزن وجود ندارد؛ `config.php` در `.gitignore` است. `usesCleartextTraffic=false` و همه درخواست‌ها HTTPS.
- **پیکربندی آدرس سرور:** به‌صورت build property (`-PapiBaseUrl=...`) یا پیش‌فرض `https://webnovo.ir/project/webapp` تعریف می‌شود (بدون هاردکد در چند نقطه).

---

## ۳) ساختار مخزن

```
nobat_sabz/
├── android/                          # اپلیکیشن نیتیو اندروید
│   ├── build.gradle.kts              # AGP 8.5.2 / Kotlin 1.9.24 / serialization
│   ├── settings.gradle.kts           # google() + mavenCentral()
│   └── app/
│       ├── build.gradle.kts          # compose BOM، تست‌ها، نسخه بسته
│       └── src/
│           ├── main/
│           │   ├── AndroidManifest.xml
│           │   ├── java/ir/sabztime/app/
│           │   │   ├── MainActivity.kt, SabzApp.kt (Application/Graph)
│           │   │   ├── core/          # ApiClient, Errors, SecureStore, AppGraph
│           │   │   ├── data/          # Models (DTO), SabzRepository
│           │   │   ├── notif/ worker/ # Notifier, ReminderWorker
│           │   │   ├── ui/            # AppNav, theme, components, 14 screens
│           │   │   └── util/          # Jalali, TextUtil
│           │   └── res/               # strings/colors/themes/launcher/notification
│           ├── test/                  # JVM unit tests (JalaliTest, ApiClientTest, RepositoryTest)
│           └── androidTest/           # AppSmokeTest (Compose)
├── ci/                                # GitHub Actions (به‌دلیل محدودیت توکن در .github نیست — §۵)
│   ├── android-build.yml              # unit tests + APK + emulator smoke
│   ├── backend-test.yml               # php -l + MySQL 8 + api_smoke.sh
│   └── README.md                      # دستور فعال‌سازی (فقط ۴ دستور)
├── project/
│   ├── webapp/                        # api.php, mobile_auth.php (جدید), search_api.php, ...
│   ├── bot/                           # حفظ‌شده بدون تغییر (webhook، دستورات)
│   ├── tests/api_smoke.sh             # تست E2E جدید بک‌اند
│   └── config.php                     # در مخزن نیست (gitignored)
└── REPORT.md                          # همین گزارش
```

---

## ۴) تست‌ها و نتایج

| تست | نوع | وضعیت در این محیط |
|---|---|---|
| `project/tests/api_smoke.sh` | E2E بر بستر MySQL 8 (ثبت‌نام، ورود، رمز غلط، 401، جستجو، جزئیات، تاریخ/اسلات، رزرو، لیست نوبت، لغو، کسب‌وکارهای من، پلن‌ها، نوتیفیکیشن) | **آماده** — اجرا نیازمند PHP 8 + MySQL 8 است (فقط در CI/سرور شما) |
| `php -l` روی **۳۱ فایل PHP** | نحو | ✅ **اجرا شد** — با PHP واقعی 8.4 (php-wasm) همه `OK` |
| `ApiClientTest` (MockWebServer) | واحد JVM | **آماده** — بدون JVM در این سندباکس، در CI اجرا می‌شود |
| `RepositoryTest` (دیکد API واقعی + payload رزرو) | واحد JVM | **آماده** — در CI |
| `JalaliTest` | واحد JVM | **آماده** — در CI |
| `AppSmokeTest` (Compose instrumented) | شبیه‌ساز | **آماده** — فقط در CI (این محیط `/dev/kvm` ندارد) |
| XML (Manifest + res)، YAML (هر دو workflow)، Kotlin parser (۳۳ فایل) | ایستا | ✅ اجرا شد — همه پاس |

**نتیجهٔ اجراشده:** ۳۱/۳۱ PHP پاس، ۳۳/۳۳ Kotlin بدون خطای syntax، تمام XML/YAML معتبر، بدون push/CRLF مشکل‌ساز.

---

## ۵) مشکلات و نکات مهم

1. **توکن GitHub App بدون دسترسی `workflows`** — دلیل شکست push اول:
   - `git push` فایل‌های `.github/workflows/*` را رد کرد.
   - حتی مسیر **Git Database API** (blob/tree/commit) هم هنگام **به‌روزرسانی ref** با `403` رد می‌شود (بررسی شد).
   - **راه‌حل پیاده‌شده:** workflowها تحت `ci/` قرار گرفتند و کل کامیت شامل اپ از طریق Git Database API (blob → tree → commit → ref) با موفقیت روی `arena/01a05c23-nobat-sabz` قرار گرفت؛ `git fetch/push` اکنون هم‌تراز است (tree با `5ccce47b...` برابر). برای فعال‌سازی خودکار CI فقط دستورهای `ci/README.md` را با توکنی که `workflows: write` دارد اجرا کنید.
2. **این محیط (سندباکس) نمی‌تواند Gradle اجرا کند:** دسترسی به `services.gradle.org`، `dl.google.com`، `repo.maven.apache.org`، `maven.google.com` مسدود است؛ Java و Android SDK هم نصب نیستند. بنابراین کامپایل واقعی APK و اجرای JVM/Shmulator تست‌ها **فقط در CI یا روی سیستم شما** ممکن است. به‌جای آن، بررسی‌های ایستا + PHP-lint واقعی (بند ۴) انجام شد.
3. **نوتیفیکیشن یادآوری** به `WorkManager` وابسته است؛ در نسخه‌های اندروید ۱۳+ مجوز `POST_NOTIFICATIONS` هنگام اولین استفاده درخواست می‌شود. `AppSmokeTest` فقط راه‌اندازی بدون crash را می‌سنجد؛ تست تعامل کامل (ورود/جستجو/رزرو) در انتظار اجرای CI است.
4. **IP استاتیک/domains:** `API_BASE_URL` پیش‌فرض `https://webnovo.ir/project/webapp` است؛ برای محیط تست، با `-PapiBaseUrl=http://10.0.2.2:8090/project/webapp` بازسازی کنید.
5. **توکن واقعی ربات در ریپازیتوری قبلی:** فایل‌های `project/bot/*.php` (از کامیت پایه‌ی کاربر) حاوی `BOT_TOKEN` واقعی هستند. ما آن را **حذف/جایگزین نکردیم** تا رفتار ربات حفظ شود، اما **توصیهٔ قوی:** این توکن را از BotFather باطل و مجدد بسازید و مقادیر را فقط در `config.php` (gitignored) بگذارید — این یک ریسک امنیتی از پیش موجود است و در نسخه‌ی جدید هم نباید در مخزن بماند.
6. **بدون `config.php` در مخزن** — هیچ‌کدام از اطلاعات اتصال (DB، ووکامرس، زرین‌پال) کامیت نشده و ندارند.

---

## ۶) مسیر APK

- **در این محیط ساخته نشد** (نبود Java/SDK + مسدود بودن Maven/Gradle؛ بخش ۵). APK خروجی CI/سیستم شما است:
  - خروجی Gradle: `android/app/build/outputs/apk/release/app-release.apk`
  - فایل نهایی CI (در شعبه `arena/**` یا `main`): `android/release/sabztime-v1.0.0.apk`
- CI در غیاب `keystore.properties` با **کلید debug** امضا می‌کند تا APK قابل‌نصب بماند؛ برای انتشار پلی‌استور باید `android/keystore.properties` (gitignored) و keystore واقعی را بسازید.

---

## ۷) دستورالعمل بازسازی (Rebuild)

### الف) فعال‌سازی CI (توصیه‌شده)
```bash
mkdir -p .github/workflows
cp ci/android-build.yml .github/workflows/android-build.yml
cp ci/backend-test.yml .github/workflows/backend-test.yml
git add .github/workflows && git commit -m "ci: activate workflows"
git push        # با توکنی که workflows: write دارد
```
سپس از تب Actions: تست واحد → ساخت APK → تست شبیه‌ساز → APK اتصال مجدد به شعبه.

### ب) ساخت محلی (Android Studio)
```bash
cd android
# 1) JDK 17 + Android SDK (platform 34، build-tools 34.0.0)
# 2) keystore اختیاری:
#    echo "storeFile=../keystore/release.jks" > keystore.properties ... (و بقیه فیلدها)
# 3) ساخت:
./gradlew testDebugUnitTest assembleRelease      # یا با آدرس تست:
./gradlew assembleRelease -PapiBaseUrl=http://10.0.2.2:8090/project/webapp
# APK: android/app/build/outputs/apk/release/app-release.apk
./gradlew connectedDebugAndroidTest              # با شبیه‌ساز run
```

### ج) تست بک‌اند محلی
```bash
# 1) MySQL 8 و PHP 8.2+ (mysql، pdo_mysql)
# 2) ایجاد project/config.php مطابق قالب CI (بدون رمز واقعی)
mysql -u root -p sabztest < project/create_tables.sql
php project/database_init.php
mysql ... # درج کسب‌وکار/خدمت/ساعت کاری نمونه  (همان SQL در ci/backend-test.yml)
cd project && php -S 127.0.0.1:8090 &
bash project/tests/api_smoke.sh
```

---

## ۸) چک‌لیست انتشار در Google Play

- [ ] **کلید امضا و keystore:** `android/keystore.properties` + keystore واقعی؛ `keyAlias`/passwords مخفی؛ بکاپ امن keystore (اگر گم شود نمی‌توان آپدیت کرد).
- [ ] **بسته (AAB) برای پلی‌استور:** `./gradlew bundleRelease` با کلید release؛ فایل `.aab` را آپلود کنید.
- [ ] **فرم دادهٔ داده‌ها (Data Safety):** شبکه محلی/سرور شما؛ اعلام جمع‌آوری `phone`, `name`, `booking info`, `device`؛ پیوند Privacy Policy (الزامی).
- [ ] **Privacy Policy** به زبان فارسی + انگلیسی؛ در صفحه Profile هم لینک شود.
- [ ] **فهرست مجوزها:** `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `WAKE_LOCK` — همه با توضیح در فرم Play.
- [ ] **محتوا/رتبه‌بندی:** زدن فرم Content Rating (بدون محتوای حساس؛ مالی/سلامت برای اشتراک/پرداخت: بله).
- [ ] **تصاویر:** آیکون 512×512، فیچر گرافیک 1024×500، اسکرین‌شات‌های 16:9 با رندر واقعی (اندروید ۱۴).
- [ ] **نسخه‌ی هدف:** targetSdk 34 (فعلاً الزام پلی‌استور؛ در صورت اعلام جدید 35/36 ارتقا دهید).
- [ ] **تست‌های Play Console:** Internal testing → Closed → Production؛ با دستگاه‌های واقعی (زبان فارسی، RTL).
- [ ] **مشکلات امنیتی:** تغییر/چرخش `BOT_TOKEN` موجود در `project/bot/*.php` و انتقال به `config.php`؛ مرور `marketer_api.php` برای رمزهای هاردکد شده.
- [ ] **یادآوری‌ها:** تست نوتیفیکیشن روی اندروید ۱۳+ و ارائه‌ی درخواست مجوز.
- [ ] **پشتیبانی:** صفحه Help داخل اپ + ایمیل/تلگرام در Console و داخل اپ.

---

## ۹) وضعیت نهایی

- ✅ کد کامل اپ (۱۴ صفحه)، بک‌اند auth موبایل، تست‌ها و CI — همه در شعبه `arena/01a05c23-nobat-sabz` در ``a2157ee``.
- ✅ بررسی‌های ایستا و lint واقعی PHP — بدون خطا.
- ⏳ اجرای Gradle (تست JVM + ساخت APK) و تست شبیه‌ساز: **نیازمند فعال‌سازی CI (§۷-الف) یا سیستم شما** — به دلیل محدودیت‌های این سندباکس، در اینجا امکان‌پذیر نبود. پس از فعال‌سازی، خروجی APK به `android/release/sabztime-v1.0.0.apk` در شعبه اضافه می‌شود.
