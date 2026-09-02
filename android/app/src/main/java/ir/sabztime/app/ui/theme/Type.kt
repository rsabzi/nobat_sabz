package ir.sabztime.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * ─────────────────────────────────────────────────────────────────────────
 *  فونت فارسی حرفه‌ای
 * ─────────────────────────────────────────────────────────────────────────
 *  پیش‌فرض: فونت سیستمی (تا اپ بدون هیچ فایل اضافه‌ای بیلد شود).
 *
 *  برای فعال‌سازی «وزیرمتن» (لایسنس آزاد OFL — بدون محدودیت تجاری):
 *   ۱) فایل‌های زیر را از https://github.com/rastikerdar/vazirmatn دانلود و در
 *      مسیر `app/src/main/res/font/` با همین نام‌ها قرار دهید:
 *        vazirmatn_regular.ttf
 *        vazirmatn_medium.ttf
 *        vazirmatn_bold.ttf
 *   ۲) کامنت بلوک زیر را بردارید و مقدار `AppFontFamily` را جایگزین کنید:
 *
 *      val AppFontFamily: FontFamily = FontFamily(
 *          Font(R.font.vazirmatn_regular, FontWeight.Normal),
 *          Font(R.font.vazirmatn_medium, FontWeight.Medium),
 *          Font(R.font.vazirmatn_medium, FontWeight.SemiBold),
 *          Font(R.font.vazirmatn_bold, FontWeight.Bold)
 *      )
 *
 *      (به import های `androidx.compose.ui.text.font.Font` و `ir.sabztime.app.R` نیاز است)
 *
 *  کل تایپوگرافی از همین یک متغیر تغذیه می‌شود؛ تغییر آن کافی است.
 */
val AppFontFamily: FontFamily = FontFamily.Default

/**
 * تایپوگرافی بهینه برای فارسی:
 *  - letter-spacing صفر (فاصله‌ی حروف در فارسی کلمه را می‌شکند)
 *  - line-height سخاوتمندانه برای خوانایی و حس «لوکس»
 *  - تراز خط بدون فضای اضافه‌ی بالا/پایین
 */
private val trim = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

private val base = TextStyle(
    fontFamily = AppFontFamily,
    letterSpacing = 0.sp,
    lineHeightStyle = trim
)

val SabzTypography = Typography(
    displayLarge = base.copy(fontSize = 36.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold),
    displayMedium = base.copy(fontSize = 31.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold),
    displaySmall = base.copy(fontSize = 27.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),

    headlineLarge = base.copy(fontSize = 25.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    headlineMedium = base.copy(fontSize = 22.sp, lineHeight = 33.sp, fontWeight = FontWeight.Bold),
    headlineSmall = base.copy(fontSize = 20.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),

    titleLarge = base.copy(fontSize = 19.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold),
    titleMedium = base.copy(fontSize = 17.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = base.copy(fontSize = 15.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),

    bodyLarge = base.copy(fontSize = 16.sp, lineHeight = 27.sp, fontWeight = FontWeight.Normal),
    bodyMedium = base.copy(fontSize = 14.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodySmall = base.copy(fontSize = 12.5.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal),

    labelLarge = base.copy(fontSize = 14.5.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    labelMedium = base.copy(fontSize = 12.5.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium),
    labelSmall = base.copy(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
)
