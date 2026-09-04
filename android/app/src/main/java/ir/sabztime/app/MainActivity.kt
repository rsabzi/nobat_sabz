package ir.sabztime.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import ir.sabztime.app.core.ThemeMode
import ir.sabztime.app.ui.AppNav
import ir.sabztime.app.ui.theme.LocalLuxPalette
import ir.sabztime.app.ui.theme.LuxPaletteDark
import ir.sabztime.app.ui.theme.LuxPaletteLight
import ir.sabztime.app.ui.theme.SabzTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = (application as SabzApp).graph
        setContent {
            // حالت پوسته از انتخاب کاربر (یا سیستم) خوانده می‌شود
            val themeMode by graph.themeManager.mode.collectAsState()
            val dark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val palette = if (dark) LuxPaletteDark else LuxPaletteLight
            // جهت صفحه صریحاً راست‌به‌چپ (RTL) است
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalLuxPalette provides palette
            ) {
                SabzTheme(darkTheme = dark) {
                    LaunchedEffect(dark) {
                        // آیکون‌های نوار وضعیت متناسب با تم
                        val controller = WindowCompat.getInsetsController(window, window.decorView)
                        controller.isAppearanceLightStatusBars = !dark
                        controller.isAppearanceLightNavigationBars = !dark
                    }
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = palette.bgTop
                    ) {
                        AppNav(graph)
                    }
                }
            }
        }
    }
}
