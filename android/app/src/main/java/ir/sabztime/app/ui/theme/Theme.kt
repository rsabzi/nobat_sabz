package ir.sabztime.app.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import ir.sabztime.app.ui.components.DarkGlass
import ir.sabztime.app.ui.components.LightGlass
import ir.sabztime.app.ui.components.LocalGlassColors

val BrandGreen = Color(0xFF128A4C)
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
    secondary = Color(0xFF3E6B52),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDEBD9),
    onSecondaryContainer = Color(0xFF072115),
    tertiary = Accent,
    background = Color(0xFFEFF7F2),
    onBackground = Color(0xFF141B16),
    surface = Color(0xFFF8FCF9),
    onSurface = Color(0xFF141B16),
    surfaceVariant = Color(0xFFDCE9E0),
    onSurfaceVariant = Color(0xFF3F4A43),
    outline = Color(0xFF6E7B72),
    outlineVariant = Color(0xFFC6D4CB),
    error = ErrorRed,
    errorContainer = Color(0xFFFADBD8),
    onError = Color.White,
    surfaceTint = Color.Transparent
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7BDCA4),
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF0F5233),
    onPrimaryContainer = Color(0xFFB7F2CE),
    secondary = Color(0xFFB2CCB9),
    onSecondary = Color(0xFF1E3526),
    secondaryContainer = Color(0xFF24402F),
    onSecondaryContainer = Color(0xFFD5E8DA),
    tertiary = Color(0xFFFFC96B),
    background = Color(0xFF0C1210),
    onBackground = Color(0xFFE0E6E1),
    surface = Color(0xFF111915),
    onSurface = Color(0xFFE0E6E1),
    surfaceVariant = Color(0xFF26322B),
    onSurfaceVariant = Color(0xFFB6C4BA),
    outline = Color(0xFF80907F),
    outlineVariant = Color(0xFF33403A),
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF5C1A17),
    onError = Color(0xFF601410),
    surfaceTint = Color.Transparent
)

/**
 * انتخاب تم برنامه: null = همگام با سیستم، true = تاریک، false = روشن.
 * انتخاب کاربر در SharedPreferences ماندگار می‌شود.
 */
object ThemePrefs {
    private const val PREFS_NAME = "sbz_ui_prefs"
    private const val KEY_DARK = "dark_mode" // system | dark | light

    var darkMode by mutableStateOf<Boolean?>(null)
        private set

    fun load(context: Context) {
        val stored = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DARK, "system")
        darkMode = when (stored) {
            "dark" -> true
            "light" -> false
            else -> null
        }
    }

    fun set(context: Context, value: Boolean?) {
        darkMode = value
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DARK, when (value) {
                true -> "dark"
                false -> "light"
                null -> "system"
            })
            .apply()
    }
}

@Composable
fun SabzTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalGlassColors provides if (darkTheme) DarkGlass else LightGlass) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            content = content
        )
    }
}
