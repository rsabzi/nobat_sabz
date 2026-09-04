package ir.sabztime.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.ui.components.DefaultNavItems
import ir.sabztime.app.ui.components.LuxBottomBar
import ir.sabztime.app.ui.theme.GlassBackground

/**
 * صفحه‌ی اصلی — سه تب (خانه، نوبت‌ها، پروفایل) روی زمینه‌ی رادیال طلایی،
 * با نوار پیمایش شناورِ فلز طلایی (فقط آیکون، بدون برچسب).
 *
 * نوار پایین «شناور» است: محتوا زیر آن ادامه دارد و هر صفحه در انتهای خود
 * فضای خالی (BottomBarSpacer) می‌گذارد تا چیزی پشت نوار پنهان نشود.
 */
@Composable
fun MainScreen(graph: AppGraph, navController: NavHostController) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
        ) { _ ->
            Box(Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        (fadeIn(tween(260)) togetherWith fadeOut(tween(160)))
                    },
                    label = "tabs"
                ) { tab ->
                    when (tab) {
                        0 -> HomeScreen(graph, navController)
                        1 -> BookingsScreen(graph, navController, onGoHome = { selectedTab = 0 })
                        else -> ProfileScreen(graph, navController)
                    }
                }

                LuxBottomBar(
                    items = DefaultNavItems,
                    selected = selectedTab,
                    onSelect = { selectedTab = it },
                    modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
                )
            }
        }
    }
}
