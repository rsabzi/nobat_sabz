package ir.sabztime.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BrandGreen = Color(0xFF1B7F4D)
val BrandGreenDark = Color(0xFF0E5A33)
val BrandGreenLight = Color(0xFFE6F4EC)
val BrandGreenContainer = Color(0xFFCDEBD9)
val Accent = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFB3261E)

private val LightColors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = BrandGreenContainer,
    onPrimaryContainer = Color(0xFF06331C),
    secondary = Color(0xFF4A6656),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDEBD9),
    onSecondaryContainer = Color(0xFF072115),
    tertiary = Accent,
    background = Color(0xFFF7FAF8),
    onBackground = Color(0xFF161D18),
    surface = Color.White,
    onSurface = Color(0xFF161D18),
    surfaceVariant = Color(0xFFDEE5DE),
    onSurfaceVariant = Color(0xFF424940),
    outline = Color(0xFF727970),
    error = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF98D5B1),
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF0F5233),
    onPrimaryContainer = Color(0xFFB7F2CE),
    secondary = Color(0xFFB2CCB9),
    onSecondary = Color(0xFF1E3526),
    background = Color(0xFF101411),
    onBackground = Color(0xFFE1E3DE),
    surface = Color(0xFF161D18),
    onSurface = Color(0xFFE1E3DE),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC2C9C1),
    error = Color(0xFFF2B8B5)
)

@Composable
fun SabzTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
