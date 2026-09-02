package ir.sabztime.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.core.SabzException
import ir.sabztime.app.core.UiState
import ir.sabztime.app.core.classify
import ir.sabztime.app.data.NotificationItem
import ir.sabztime.app.ui.components.EmptyState
import ir.sabztime.app.ui.components.ErrorState
import ir.sabztime.app.ui.theme.GlassBackground
import ir.sabztime.app.ui.components.GlassCard
import ir.sabztime.app.ui.components.GlassTopBar
import ir.sabztime.app.ui.components.IconTile
import ir.sabztime.app.ui.components.TilePalette
import ir.sabztime.app.util.Jalali
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationsViewModel(private val graph: AppGraph) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<NotificationItem>>>(UiState.Loading)
    val state: StateFlow<UiState<List<NotificationItem>>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                val list = graph.repository.notifications()
                _state.value = if (list.isEmpty()) UiState.Empty else UiState.Success(list)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val s = classify(e)
                _state.value = UiState.Error(s.message, s.kind)
            }
        }
    }
}

private fun notificationIcon(type: String): ImageVector = when (type) {
    "booking_cancelled" -> Icons.Filled.Close
    "booking_completed" -> Icons.Filled.CheckCircle
    "booking_in_progress" -> Icons.Filled.AccessTime
    "credit_change" -> Icons.Filled.Star
    else -> Icons.Filled.CalendarMonth
}

private fun notificationPalette(type: String): TilePalette = when (type) {
    "booking_cancelled" -> TilePalette.ERROR
    "credit_change" -> TilePalette.AMBER
    else -> TilePalette.GREEN
}

@Composable
fun NotificationsScreen(graph: AppGraph, navController: NavHostController) {
    val viewModel: NotificationsViewModel = viewModel { NotificationsViewModel(graph) }
    val state by viewModel.state.collectAsState()

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                GlassTopBar(
                    title = "اعلان‌ها",
                    onBack = { navController.popBackStack() },
                    actions = {
                        IconButton(onClick = viewModel::load) {
                            Icon(Icons.Filled.Refresh, contentDescription = "تازه‌سازی")
                        }
                    }
                )
            }
        ) { padding ->
            when (val s = state) {
                is UiState.Loading -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                is UiState.Error -> ErrorState(s.message, s.kind, onRetry = viewModel::load)
                UiState.Empty -> EmptyState(
                    icon = "🔔",
                    title = "اعلانی ندارید",
                    subtitle = "رویدادهای رزرو و امتیاز شما اینجا نمایش داده می‌شود."
                )
                is UiState.Success -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(s.data, key = { "${it.type}-${it.refId}-${it.createdAt}" }) { n ->
                        GlassCard(shape = RoundedCornerShape(24.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconTile(
                                    icon = notificationIcon(n.type),
                                    tileSize = 44.dp,
                                    palette = notificationPalette(n.type),
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        n.title,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        n.body,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!n.createdAt.isNullOrBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            Jalali.formatLong(n.createdAt.take(10)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}
