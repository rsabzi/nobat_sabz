package ir.sabztime.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * طرح رنگ «لوکس» — هویت بصری واحد و تک‌حالته (Dark Premium).
 * تمام صفحه‌ها از همین طرح تغذیه می‌شوند تا حس یکدست و گران‌قیمت حفظ شود.
 */
private val LuxColors = darkColorScheme(
    primary = Gold,
    onPrimary = MidnightDeep,
    primaryContainer = GoldDeep,
    onPrimaryContainer = GoldLight,
    inversePrimary = GoldBright,

    secondary = Burgundy,
    onSecondary = Cream,
    secondaryContainer = BurgundyDeep,
    onSecondaryContainer = Cream,

    tertiary = GoldBright,
    onTertiary = MidnightDeep,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = GoldLight,

    background = Midnight,
    onBackground = Cream,

    surface = NavyCard,
    onSurface = Cream,
    surfaceVariant = NavyCardDeep,
    onSurfaceVariant = TextMuted,
    surfaceTint = Gold,

    error = DangerRose,
    onError = PureWhite,
    errorContainer = DangerRoseDeep,
    onErrorContainer = RoseContainerText,

    outline = GoldDim,
    outlineVariant = OutlineFaint,
    scrim = ScrimDeep,

    inverseSurface = Cream,
    inverseOnSurface = MidnightDeep,

    surfaceContainerLowest = CharcoalDeep,
    surfaceContainerLow = Midnight,
    surfaceContainer = NavyCardDeep,
    surfaceContainerHigh = NavyCard,
    surfaceContainerHighest = MidnightSoft
)

/**
 * تم نوبت‌سبز — «پرایم». پارامتر darkTheme برای سازگاری با کدهای موجود
 * حفظ شده اما هویت بصری همیشه تیره‌ی لوکس است (طراحی تک‌حالته).
 */
@Composable
fun SabzTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LuxColors,
        typography = SabzTypography,
        shapes = SabzShapes,
        content = content
    )
}
