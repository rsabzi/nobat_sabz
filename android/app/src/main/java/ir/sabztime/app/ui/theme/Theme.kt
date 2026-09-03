package ir.sabztime.app.ui.theme

import androidx.compose.material3.MaterialTheme
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
 * تم نوبت‌سبز — «آبی برند». پارامتر darkTheme برای سازگاری با کدهای موجود
 * حفظ شده اما هویت بصری همیشه روشنِ آبی است (طراحی تک‌حالته).
 */
@Composable
fun SabzTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LuxColors,
        typography = SabzTypography,
        shapes = SabzShapes,
        content = content
    )
}
