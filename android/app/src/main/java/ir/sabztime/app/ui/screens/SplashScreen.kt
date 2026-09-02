package ir.sabztime.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.ui.Routes

/**
 * صفحه‌ی خوش‌آمد — گرادیان برند + لوگوی سه‌بعدی شیشه‌ای.
 */
@Composable
fun SplashScreen(graph: AppGraph, navController: NavHostController) {
    LaunchedEffect(Unit) {
        val hasToken = graph.restoreSession()
        navController.navigate(if (hasToken) Routes.MAIN else Routes.LOGIN) {
            popUpTo(Routes.SPLASH) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF17B871), Color(0xFF0B7A45), Color(0xFF075C36))
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // لوگوی سه‌بعدی شیشه‌ای
            Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.10f))
                            ),
                            shape = RoundedCornerShape(34.dp)
                        )
                        .shadow(14.dp, RoundedCornerShape(34.dp))
                )
                Icon(
                    Icons.Filled.Spa,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = Color.White
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                "نوبت‌سبز",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "رزرو نوبت، ساده و حرفه‌ای",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(Modifier.height(44.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(30.dp),
                color = Color.White,
                strokeWidth = 3.dp
            )
        }
    }
}
