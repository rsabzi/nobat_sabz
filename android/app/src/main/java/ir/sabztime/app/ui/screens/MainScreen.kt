package ir.sabztime.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.ui.components.AuroraBackdrop
import ir.sabztime.app.ui.components.IconBadge3D
import ir.sabztime.app.ui.components.glass

private data class TabItem(val label: String, val icon: ImageVector)

@Composable
fun MainScreen(graph: AppGraph, navController: NavHostController) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        TabItem("خانه", Icons.Filled.Home),
        TabItem("نوبت‌ها", Icons.AutoMirrored.Filled.List),
        TabItem("پروفایل", Icons.Filled.AccountCircle)
    )
    val glass = MaterialTheme.glass

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.weight(1f)) {
                AuroraBackdrop(Modifier.matchParentSize())
                Box(Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        0 -> HomeScreen(graph, navController)
                        1 -> BookingsScreen(graph, navController)
                        2 -> ProfileScreen(graph, navController)
                    }
                }
            }

            // نوار پایین شیشه‌ای با نشانگر سه‌بعدی برای تب فعال
            HorizontalDivider(
                thickness = 1.dp,
                color = glass.strokeTop
            )
            NavigationBar(
                containerColor = glass.navBar,
                tonalElevation = 0.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = selectedTab == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = index },
                        icon = {
                            if (selected) {
                                IconBadge3D(
                                    tab.icon,
                                    size = 40.dp,
                                    contentDescription = tab.label
                                )
                            } else {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.label,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        label = {
                            Text(
                                tab.label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationBadgeButton(onClick: () -> Unit) {
    androidx.compose.material3.IconButton(onClick = onClick) {
        Icon(
            Icons.Filled.Notifications,
            contentDescription = "اعلان‌ها",
            tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
        )
    }
}
