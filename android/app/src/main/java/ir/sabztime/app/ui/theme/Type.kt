package ir.sabztime.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * تایپوگرافی بهینه برای فارسی:
 *  - بدون letter-spacing (که در فارسی کلمه‌ها را باز می‌کند)
 *  - line-height مناسب برای خوانایی
 */
private val base = TextStyle(letterSpacing = 0.sp)

val SabzTypography = Typography(
    displayLarge = base.copy(fontSize = 34.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold),
    displayMedium = base.copy(fontSize = 30.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    displaySmall = base.copy(fontSize = 26.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),

    headlineLarge = base.copy(fontSize = 24.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    headlineMedium = base.copy(fontSize = 22.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    headlineSmall = base.copy(fontSize = 20.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),

    titleLarge = base.copy(fontSize = 19.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = base.copy(fontSize = 17.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = base.copy(fontSize = 15.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),

    bodyLarge = base.copy(fontSize = 16.sp, lineHeight = 26.sp, fontWeight = FontWeight.Normal),
    bodyMedium = base.copy(fontSize = 15.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodySmall = base.copy(fontSize = 13.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),

    labelLarge = base.copy(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    labelMedium = base.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelSmall = base.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal)
)

/** خانواده فونت پیش‌فرض (سیستمی) — قابل تعویض با فونت فارسی دلخواه */
val AppFontFamily: FontFamily = FontFamily.Default
