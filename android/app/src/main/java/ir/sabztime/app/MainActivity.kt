package ir.sabztime.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import ir.sabztime.app.core.ThemeMode
import ir.sabztime.app.ui.AppNav
import ir.sabztime.app.ui.theme.SabzTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = (application as SabzApp).graph
        setContent {
            // حالت پوسته از انتخاب کاربر (یا سیستم) خوانده می‌شود
            val themeMode by graph.themeManager.mode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            SabzTheme(darkTheme = darkTheme) {
                LaunchedEffect(darkTheme) {
                    // رنگ آیکون‌های نوار وضعیت متناسب با تم
                    WindowCompat.getInsetsController(window, window.decorView)
                        .isAppearanceLightStatusBars = !darkTheme
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNav(graph)
                }
            }
        }
    }
}
