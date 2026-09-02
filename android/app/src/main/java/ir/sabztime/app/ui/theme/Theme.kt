package ir.sabztime.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** رنگ‌های هویت بصری نوبت‌سبز؛ سبز عمیق برای اعتماد و کهربایی برای تأکیدهای کوتاه. */
val BrandGreen = Color(0xFF087B5B)
val BrandGreenDark = Color(0xFF00513B)
val BrandGreenLight = Color(0xFFE4F5EF)
val BrandGreenContainer = Color(0xFFC4EBDD)
val Accent = Color(0xFFFFB547)
val ErrorRed = Color(0xFFBA1A1A)
val Ink = Color(0xFF17211D)
val MutedInk = Color(0xFF53625B)
val Canvas = Color(0xFFF7FAF8)

private val LightColors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = BrandGreenContainer,
    onPrimaryContainer = Color(0xFF002116),
    secondary = Color(0xFF4B6358),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0E8D9),
    onSecondaryContainer = Color(0xFF082016),
    tertiary = Color(0xFF775A00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE08D),
    onTertiaryContainer = Color(0xFF251A00),
    background = Canvas,
    onBackground = Ink,
    surface = Color(0xFFFFFFFF),
    onSurface = Ink,
    surfaceVariant = Color(0xFFDEE8E1),
    onSurfaceVariant = MutedInk,
    surfaceContainerHighest = Color(0xFFEAF1EC),
    outline = Color(0xFF74827B),
    outlineVariant = Color(0xFFC4CEC7),
    error = ErrorRed,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BDABD),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF00513B),
    onPrimaryContainer = Color(0xFFA7F7D7),
    secondary = Color(0xFFB2CCBE),
    onSecondary = Color(0xFF1E352B),
    secondaryContainer = Color(0xFF344B40),
    onSecondaryContainer = Color(0xFFCEE9D9),
    tertiary = Color(0xFFFFDB7E),
    onTertiary = Color(0xFF3E2E00),
    tertiaryContainer = Color(0xFF5A4300),
    onTertiaryContainer = Color(0xFFFFE08D),
    background = Color(0xFF101512),
    onBackground = Color(0xFFE0E5E0),
    surface = Color(0xFF161D19),
    onSurface = Color(0xFFE0E5E0),
    surfaceVariant = Color(0xFF3E4A43),
    onSurfaceVariant = Color(0xFFBECAC2),
    outline = Color(0xFF89968E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun SabzTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
