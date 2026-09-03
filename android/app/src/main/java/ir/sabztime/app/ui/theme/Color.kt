package ir.sabztime.app.ui.theme

import androidx.compose.ui.graphics.Color

// ===========================================================================
// سیستم طراحی «نوبت‌سبز» — نسخه ۳ (پالت برند واحد)
// ---------------------------------------------------------------------------
// قواعد:
//  - آبی عمیق = برند، اعتماد، ناوبری و CTA اصلی  (زبان جهانی رزرو: Fresha/Booking)
//  - سبز = فقط «سیگنال»: نوبت آزاد، تأیید، موفقیت
//  - کهربایی = هشدار / اشتراک / امتیاز   |   قرمز = خطا / لغو
//  - خنثی سرد ۶۰٪ + آبی ۳۰٪ + سیگنال ۱۰٪ (قاعده ۶۰-۳۰-۱۰)
// ===========================================================================

// ---------------------------------------------------------------------------
// توکن‌های برند
// ---------------------------------------------------------------------------
val BrandPrimary = Color(0xFF0B4F9E)       // آبی عمیق اعتماد — کنتراست سفید ≈ 8:1
val BrandPrimaryDark = Color(0xFF08418A)   // حالت فشرده/گرادیان
val BrandPrimaryBright = Color(0xFF1E6FE0) // هایلایت گرادیان (فقط دکمه‌های بزرگ)
val BrandContainer = Color(0xFFE2EDFB)     // پس‌زمینه انتخاب/چیپ برند
val OnBrandContainer = Color(0xFF062A54)

// سبزِ سیگنال — فقط برای «نوبت آزاد / موفقیت / تأیید»
val SignalGreen = Color(0xFF15803D)
val SignalGreenBright = Color(0xFF16A34A)
val SignalGreenContainer = Color(0xFFE7F4EC)
val OnSignalGreenContainer = Color(0xFF0A3B1E)

// هشدار (کهربایی)
val SignalAmber = Color(0xFFB45309)
val SignalAmberContainer = Color(0xFFFEF3C7)
val OnSignalAmberContainer = Color(0xFF5A3A00)

// خطا
val SignalError = Color(0xFFC02525)
val SignalErrorContainer = Color(0xFFFDE8E8)
val OnSignalErrorContainer = Color(0xFF650D0D)

// ---------------------------------------------------------------------------
// تم روشن
// ---------------------------------------------------------------------------
val LightPrimary = BrandPrimary
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = BrandContainer
val LightOnPrimaryContainer = OnBrandContainer
val LightSecondary = Color(0xFF52606F)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFDDE5F0)
val LightOnSecondaryContainer = Color(0xFF1B2734)
val LightTertiary = Color(0xFF8A5A00)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFE3B3)
val LightOnTertiaryContainer = Color(0xFF3B2700)
val LightError = SignalError
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = SignalErrorContainer
val LightOnErrorContainer = OnSignalErrorContainer
val LightBackground = Color(0xFFF5F7FA)
val LightOnBackground = Color(0xFF111827)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF111827)
val LightSurfaceVariant = Color(0xFFE7ECF2)
val LightOnSurfaceVariant = Color(0xFF5B6472)
val LightOutline = Color(0xFF7A8593)
val LightOutlineVariant = Color(0xFFD3DAE3)
val LightScrim = Color(0xFF000000)
val LightInverseSurface = Color(0xFF1F2937)
val LightInverseOnSurface = Color(0xFFF2F4F7)
val LightInversePrimary = Color(0xFF9CC5FF)

val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF7F9FC)
val LightSurfaceContainer = Color(0xFFEEF2F7)
val LightSurfaceContainerHigh = Color(0xFFE9EDF3)
val LightSurfaceContainerHighest = Color(0xFFE3E8EF)

// ---------------------------------------------------------------------------
// تم تاریک
// ---------------------------------------------------------------------------
val DarkPrimary = Color(0xFF9CC5FF)
val DarkOnPrimary = Color(0xFF06284D)
val DarkPrimaryContainer = Color(0xFF0A3D77)
val DarkOnPrimaryContainer = Color(0xFFD6E6FF)
val DarkSecondary = Color(0xFFB8C4D2)
val DarkOnSecondary = Color(0xFF24303D)
val DarkSecondaryContainer = Color(0xFF3B4855)
val DarkOnSecondaryContainer = Color(0xFFD3E0EE)
val DarkTertiary = Color(0xFFFFC766)
val DarkOnTertiary = Color(0xFF3B2A00)
val DarkTertiaryContainer = Color(0xFF594200)
val DarkOnTertiaryContainer = Color(0xFFFFE3B3)
val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)
val DarkBackground = Color(0xFF0B1220)
val DarkOnBackground = Color(0xFFE6EBF2)
val DarkSurface = Color(0xFF121A29)
val DarkOnSurface = Color(0xFFE6EBF2)
val DarkSurfaceVariant = Color(0xFF3D4756)
val DarkOnSurfaceVariant = Color(0xFF9AA7B8)
val DarkOutline = Color(0xFF8B96A5)
val DarkOutlineVariant = Color(0xFF3D4756)
val DarkScrim = Color(0xFF000000)
val DarkInverseSurface = Color(0xFFE6EBF2)
val DarkInverseOnSurface = Color(0xFF1E2530)
val DarkInversePrimary = BrandPrimary

val DarkSurfaceContainerLowest = Color(0xFF0A101D)
val DarkSurfaceContainerLow = Color(0xFF151D2C)
val DarkSurfaceContainer = Color(0xFF1A2333)
val DarkSurfaceContainerHigh = Color(0xFF243043)
val DarkSurfaceContainerHighest = Color(0xFF2D3A4E)

// ---------------------------------------------------------------------------
// گرادیان‌های کامپوننت (کاشی‌های سه‌بعدی / دکمه‌ها)
// ---------------------------------------------------------------------------

/** گرادیان اصلی (برند آبی) — حالت روشن */
val GlassGradientLight: List<Color> = listOf(Color(0xFF1E6FE0), Color(0xFF0B4F9E))

/** گرادیان اصلی — حالت تاریک */
val GlassGradientDark: List<Color> = listOf(Color(0xFF5C9BFF), Color(0xFF1D53A8))

/** گرادیان غیرفعال / خنثی */
val GlassGradientMutedLight: List<Color> = listOf(Color(0xFFAFC0D2), Color(0xFF8FA3B8))
val GlassGradientMutedDark: List<Color> = listOf(Color(0xFF4A5563), Color(0xFF3A4452))

/** گرادیان سیگنال سبز — فقط «نوبت آزاد / موفقیت / انتخاب» */
val GlassGradientGreenLight: List<Color> = listOf(Color(0xFF43C37E), Color(0xFF15803D))
val GlassGradientGreenDark: List<Color> = listOf(Color(0xFF6FDD9B), Color(0xFF1E8A50))

/** گرادیان آیکون‌های خطایی */
val GlassGradientErrorLight: List<Color> = listOf(Color(0xFFF28B82), Color(0xFFC62828))
val GlassGradientErrorDark: List<Color> = listOf(Color(0xFFEF9A93), Color(0xFFB3362B))

/** گرادیان آیکون‌های هشدار/امتیاز */
val GlassGradientAmberLight: List<Color> = listOf(Color(0xFFFFB74D), Color(0xFFEF8C00))
val GlassGradientAmberDark: List<Color> = listOf(Color(0xFFFFC766), Color(0xFFB26A00))
