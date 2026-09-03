package ir.sabztime.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * طرح رنگ «طلایی لوکس» — هویت بصری روشن و یکدست (Gold Metallic Light).
 * تمام صفحه‌ها از همین طرح تغذیه می‌شوند تا حس طلای برجسته و یکپارچه بماند.
 */
private val LuxColors = lightColorScheme(
    primary = GoldInk,
    onPrimary = Color.White,
    primaryContainer = GoldPale,
    onPrimaryContainer = Cocoa,
    inversePrimary = GoldPale,

    secondary = BurgundyLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6DEE0),
    onSecondaryContainer = Color(0xFF4A0F18),

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
    errorContainer = Color(0xFFF8D7DB),
    onErrorContainer = Color(0xFF5A0E22),

    outline = RingGold.copy(alpha = 0.55f),
    outlineVariant = OutlineFaint,
    scrim = ScrimDeep,

    inverseSurface = BurgundyDeep,
    inverseOnSurface = CreamLight,

    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFBF0),
    surfaceContainer = Color(0xFFFFF4D6),
    surfaceContainerHigh = Color(0xFFFFEFC2),
    surfaceContainerHighest = Color(0xFFFFE9AC)
)

/**
 * تم نوبت‌سبز — «طلایی لوکس». پارامتر darkTheme برای سازگاری با کدهای موجود
 * حفظ شده اما هویت بصری همیشه روشنِ طلایی است (طراحی تک‌حالته).
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
