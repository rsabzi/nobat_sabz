package ir.sabztime.app.ui.screens

import androidx.compose.foundation.clickable
import ir.sabztime.app.ui.theme.LocalSabzDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.ui.theme.GlassBackground
import ir.sabztime.app.ui.components.IconTile

private data class TabItem(val label: String, val icon: ImageVector)

/**
 * صفحه‌ی اصلی با سه تب (خانه، نوبت‌ها، پروفایل)
 * و نوار پایین شناور شیشه‌ای با آیکون‌های سه‌بعدی.
 */
@Composable
fun MainScreen(graph: AppGraph, navController: NavHostController) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        TabItem("خانه", Icons.Filled.Home),
        TabItem("نوبت‌ها", Icons.Filled.CalendarMonth),
        TabItem("پروفایل", Icons.Filled.AccountCircle)
    )
    val dark = LocalSabzDarkTheme.current
    val barShape = RoundedCornerShape(26.dp)

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // در Material3 1.2 نوار ناوبری «shape» ندارد؛ گوشه‌ها و سایه
                    // با clip + shadow روی خودِ Box اعمال می‌شوند.
                    Box(
                        Modifier
                            .shadow(10.dp, barShape)
                            .clip(barShape)
                    ) {
                        NavigationBar(
                            containerColor = if (dark) Color.White.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.85f),
                            tonalElevation = 0.dp
                        ) {
                            tabs.forEachIndexed { index, tab ->
                                val selected = selectedTab == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { selectedTab = index },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        IconTile(
                                            icon = tab.icon,
                                            tileSize = 40.dp,
                                            selected = selected,
                                            contentDescription = tab.label
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            tab.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (selectedTab) {
                    0 -> HomeScreen(graph, navController)
                    1 -> BookingsScreen(graph, navController, onGoHome = { selectedTab = 0 })
                    2 -> ProfileScreen(graph, navController)
                }
            }
        }
    }
}
