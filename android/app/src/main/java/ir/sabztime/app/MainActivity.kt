package ir.sabztime.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import ir.sabztime.app.ui.AppNav
import ir.sabztime.app.ui.theme.SabzTheme
import ir.sabztime.app.ui.theme.ThemePrefs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemePrefs.load(this)
        enableEdgeToEdge()
        val graph = (application as SabzApp).graph
        setContent {
            val dark = ThemePrefs.darkMode ?: isSystemInDarkTheme()
            // نوارهای سیستم همیشه هماهنگ با تم فعال برنامه
            SideEffect {
                val style = if (dark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    )
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            SabzTheme(darkTheme = dark) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .consumeWindowInsets(WindowInsets.safeDrawing),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNav(graph)
                }
            }
        }
    }
}
