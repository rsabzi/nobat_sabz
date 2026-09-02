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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = (application as SabzApp).graph
        setContent {
            // هویت بصری «پرایم» همیشه تیره‌ی لوکس است
            SabzTheme {
                LaunchedEffect(Unit) {
                    // آیکون‌های نوار وضعیت روشن روی پس‌زمینه‌ی سرمه‌ای تیره
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.isAppearanceLightStatusBars = false
                    controller.isAppearanceLightNavigationBars = false
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ir.sabztime.app.ui.theme.Midnight
                ) {
                    AppNav(graph)
                }
            }
        }
    }
}
