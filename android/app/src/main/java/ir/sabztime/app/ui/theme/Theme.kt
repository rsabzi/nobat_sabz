package ir.sabztime.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * طرح رنگ «آبی برند» — هویت بصری روشن و یکدست (V3 Blue Brand).
 * تمام صفحه‌ها از همین طرح تغذیه می‌شوند تا حس آبی برند و سبز سیگنال یکپارچه بماند.
 */
private val LuxColors = lightColorScheme(
    primary = GoldInk,
    onPrimary = Color.White,
    primaryContainer = GoldPale,
    onPrimaryContainer = Cocoa,
    inversePrimary = GoldPale,

    secondary = BurgundyLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2EDFB),
    onSecondaryContainer = Color(0xFF062A54),

    tertiary = RingGold,
    onTertiary = Color.White,
    tertiaryContainer = GoldTint,
    onTertiaryContainer = Cocoa,

    background = GoldBgTop,
    onBackground = Cocoa,

    surface = NavyCard,
    onSurface = Cocoa,
    surfaceVariant = NavyCardDeep,
    onSurfaceVariant = CocoaSoft,
    surfaceTint = Gold,

    error = DangerRose,
    onError = Color.White,
    errorContainer = Color(0xFFFDE8E8),
    onErrorContainer = Color(0xFF8E1630),

    outline = RingGold.copy(alpha = 0.55f),
    outlineVariant = OutlineFaint,
    scrim = ScrimDeep,

    inverseSurface = BurgundyDeep,
    inverseOnSurface = CreamLight,

    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7FAFD),
    surfaceContainer = Color(0xFFEFF5FC),
    surfaceContainerHigh = Color(0xFFE4EEF9),
    surfaceContainerHighest = Color(0xFFD9E7F5)
)

/**
 * طرح رنگِ تیره‌ی لوکس — همان هویت آبی/طلایی برند، اما روی زمینه‌ی سرمه‌ایِ عمیق
 * با متنِ روشن. فقط در حالتِ تاریک استفاده می‌شود.
 */
private val LuxDarkColors = darkColorScheme(
    primary = Color(0xFF9CC5FF),
    onPrimary = Color(0xFF0B1220),
    primaryContainer = Color(0xFF2C3A5A),
    onPrimaryContainer = Color(0xFFDCE9FF),

    secondary = Color(0xFF9CC5FF),
    onSecondary = Color(0xFF0B1220),
    secondaryContainer = Color(0xFF24324E),
    onSecondaryContainer = Color(0xFFCFE0FF),

    tertiary = Color(0xFFFFD9A1),
    onTertiary = Color(0xFF3B2800),
    tertiaryContainer = Color(0xFF4A3A10),
    onTertiaryContainer = Color(0xFFFFE4C0),

    background = DarkBg,
    onBackground = DarkOnSurface,

    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = Color(0xFF1E2A3F),
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = Color(0xFF9CC5FF),

    error = Color(0xFFEF8A8A),
    onError = Color(0xFF3B0000),
    errorContainer = Color(0xFF5A1520),
    onErrorContainer = Color(0xFFFFDAD6),

    outline = Color(0xFF4A5870),
    outlineVariant = Color(0xFF2E3B52),
    scrim = Color(0xCC000000),

    inverseSurface = Color(0xFFE6EBF2),
    inverseOnSurface = Color(0xFF1B2230),

    surfaceContainerLowest = Color(0xFF0A1120),
    surfaceContainerLow = Color(0xFF0F1726),
    surfaceContainer = Color(0xFF141C2E),
    surfaceContainerHigh = Color(0xFF1A2436),
    surfaceContainerHighest = Color(0xFF202B42)
)

/**
 * تم نوبت‌سبز — «آبی برند». اگر [darkTheme] درست باشد طرحِ تیره‌ی لوکس
 * ([LuxDarkColors]) و در غیر این صورت طرحِ روشن ([LuxColors]) به‌کار می‌رود.
 */
@Composable
fun SabzTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) LuxDarkColors else LuxColors,
        typography = SabzTypography,
        shapes = SabzShapes,
        content = content
    )
}
