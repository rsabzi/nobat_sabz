package ir.sabztime.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import ir.sabztime.app.ui.AppNav
import ir.sabztime.app.ui.theme.SabzTheme
import ir.sabztime.app.ui.theme.GoldBgTop

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = (application as SabzApp).graph
        setContent {
            // هویت بصری همیشه «طلایی لوکس»ِ روشن است
            SabzTheme {
                LaunchedEffect(Unit) {
                    // آیکون‌های نوار وضعیت تیره روی زمینه‌ی طلایی روشن
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.isAppearanceLightStatusBars = true
                    controller.isAppearanceLightNavigationBars = true
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GoldBgTop
                ) {
                    AppNav(graph)
                }
            }
        }
    }
}
