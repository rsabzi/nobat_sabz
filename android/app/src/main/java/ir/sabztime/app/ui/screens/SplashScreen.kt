package ir.sabztime.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.ui.Routes
import ir.sabztime.app.ui.components.GoldText
import ir.sabztime.app.ui.components.brushedGoldTint
import ir.sabztime.app.ui.theme.Cream
import ir.sabztime.app.ui.theme.Gold
import ir.sabztime.app.ui.theme.GoldBright
import ir.sabztime.app.ui.theme.GoldDeep
import ir.sabztime.app.ui.theme.GoldLight
import ir.sabztime.app.ui.theme.GlassBackground
import ir.sabztime.app.ui.theme.NavyInk
import ir.sabztime.app.ui.theme.PureWhite
import ir.sabztime.app.ui.theme.goldBorder
import ir.sabztime.app.ui.theme.goldLinePattern
import ir.sabztime.app.ui.theme.neonGlow

/**
 * صفحه‌ی خوش‌آمد — پس‌زمینه‌ی سرمه‌ای→زغالی با نشان طلایی نفس‌کش (Breathing glow).
 */
@Composable
fun SplashScreen(graph: AppGraph, navController: NavHostController) {
    LaunchedEffect(Unit) {
        val hasToken = graph.restoreSession()
        navController.navigate(if (hasToken) Routes.MAIN else Routes.LOGIN) {
            popUpTo(Routes.SPLASH) { inclusive = true }
        }
    }

    val transition = rememberInfiniteTransition(label = "splash")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "pulse"
    )

    GlassBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(120.dp)
                        .scale(pulse)
                        .neonGlow(color = GoldLight, alpha = 0.5f)
                )
                val shape = RoundedCornerShape(36.dp)
                Box(
                    Modifier
                        .size(108.dp)
                        .shadow(24.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
                        .clip(shape)
                        .background(Brush.verticalGradient(listOf(Color(0xFF17234A), NavyInk)))
                        .goldLinePattern(alpha = 0.10f, spacing = 10.dp)
                        .border(goldBorder(0.45f), shape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .brushedGoldTint(
                                Brush.linearGradient(listOf(GoldLight, Gold, GoldDeep, GoldBright))
                            )
                    ) {
                        Icon(
                            Icons.Filled.ContentCut,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(26.dp))
            GoldText(
                text = "نوبت‌سبز",
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "تجربه‌ای لوکس از رزرو نوبت",
                style = MaterialTheme.typography.bodyMedium,
                color = Cream.copy(alpha = 0.75f),
                fontWeight = FontWeight.Normal
            )
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .width(58.dp)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, GoldLight.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
            )
            Spacer(Modifier.height(40.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                color = Gold,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "در حال آماده‌سازی…",
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite.copy(alpha = 0.45f)
            )
        }
    }
}
