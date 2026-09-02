package ir.sabztime.app.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
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
import ir.sabztime.app.data.QueueItem
import ir.sabztime.app.data.QueueResponse
import ir.sabztime.app.data.SubscriptionStatusResponse
import ir.sabztime.app.ui.Routes
import ir.sabztime.app.ui.components.EmptyState
import ir.sabztime.app.ui.components.ErrorState
import ir.sabztime.app.ui.theme.GlassBackground
import ir.sabztime.app.ui.components.GlassCard
import ir.sabztime.app.ui.components.GlassTopBar
import ir.sabztime.app.ui.components.GradientButton
import ir.sabztime.app.ui.components.IconTile
import ir.sabztime.app.ui.components.InfoRow
import ir.sabztime.app.ui.components.StatusChip
import ir.sabztime.app.ui.components.TilePalette
import ir.sabztime.app.util.Jalali
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OwnerState(
    val queue: UiState<QueueResponse> = UiState.Loading,
    val subscription: SubscriptionStatusResponse? = null,
    val actionBusy: Boolean = false,
    val actionMessage: String? = null,
    val actionError: Boolean = false
)

class OwnerBusinessViewModel(private val graph: AppGraph, private val businessId: Int) : ViewModel() {
    private val _state = MutableStateFlow(OwnerState())
    val state: StateFlow<OwnerState> = _state.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        refreshQueue()
        refreshSubscription()
    }

    fun refreshQueue() {
        _state.value = _state.value.copy(queue = UiState.Loading)
        viewModelScope.launch {
            try {
                val q = graph.repository.activeQueue(businessId)
                _state.value = _state.value.copy(
                    queue = if (q.queue.isEmpty()) UiState.Empty else UiState.Success(q)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val s = classify(e)
                _state.value = _state.value.copy(queue = UiState.Error(s.message, s.kind))
            }
        }
    }

    fun refreshSubscription() {
        viewModelScope.launch {
            try {
                val s = graph.repository.subscriptionStatus(businessId)
                _state.value = _state.value.copy(subscription = s)
            } catch (e: Exception) {
                // اختیاری
            }
        }
    }

    fun advance() {
        _state.value = _state.value.copy(actionBusy = true, actionMessage = null, actionError = false)
        viewModelScope.launch {
            try {
                graph.repository.advanceQueue(businessId)
                _state.value = _state.value.copy(actionBusy = false, actionMessage = "نوبت بعدی فراخوانی شد. ✅", actionError = false)
                refreshQueue()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val s = classify(e)
                _state.value = _state.value.copy(actionBusy = false, actionMessage = s.message, actionError = true)
            }
        }
    }

    fun complete(bookingId: Int) {
        _state.value = _state.value.copy(actionBusy = true, actionMessage = null, actionError = false)
        viewModelScope.launch {
            try {
                graph.repository.completeBooking(businessId, bookingId)
                _state.value = _state.value.copy(actionBusy = false, actionMessage = "رزرو تکمیل شد. ✅", actionError = false)
                refreshQueue()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val s = classify(e)
                _state.value = _state.value.copy(actionBusy = false, actionMessage = s.message, actionError = true)
            }
        }
    }
}

@Composable
fun OwnerBusinessScreen(graph: AppGraph, businessId: Int, navController: NavHostController) {
    val viewModel: OwnerBusinessViewModel = viewModel(key = "own-$businessId") {
        OwnerBusinessViewModel(graph, businessId)
    }
    val state by viewModel.state.collectAsState()

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                GlassTopBar(
                    title = "مدیریت کسب‌وکار",
                    onBack = { navController.popBackStack() },
                    actions = {
                        IconButton(onClick = viewModel::refreshAll) {
                            Icon(Icons.Filled.Refresh, contentDescription = "تازه‌سازی")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // اشتراک
                    state.subscription?.let { s ->
                        GlassCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconTile(
                                    icon = Icons.Filled.Star,
                                    tileSize = 44.dp,
                                    palette = TilePalette.AMBER,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "اشتراک و ظرفیت",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            InfoRow("نوبت‌های رایگان باقی‌مانده", Jalali.persianDigits(s.freeTokensRemaining))
                            InfoRow("پلن فعلی", s.subscriptionPlan ?: "—")
                            InfoRow("انقضا", Jalali.formatLong(s.expiryDate))
                            s.referralCode?.let {
                                InfoRow("لینک اختصاصی", "@$it")
                            }
                            Spacer(Modifier.height(10.dp))
                            GradientButton(
                                onClick = { navController.navigate(Routes.subscription(businessId)) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("مشاهده پلن‌ها و خرید اشتراک", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "صف امروز",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        GradientButton(
                            onClick = viewModel::advance,
                            enabled = !state.actionBusy
                        ) {
                            Text("نوبت بعدی", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                state.actionMessage?.let { msg ->
                    item {
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.actionError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                when (val q = state.queue) {
                    is UiState.Loading -> item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    is UiState.Error -> item { ErrorState(q.message, q.kind, onRetry = viewModel::refreshQueue) }
                    UiState.Empty -> item {
                        EmptyState(
                            icon = "🗓️",
                            title = "صف امروز خالی است",
                            subtitle = "رزروهای امروز اینجا نمایش داده می‌شوند.",
                            action = {
                                GradientButton(onClick = viewModel::refreshQueue) {
                                    Text("تازه‌سازی")
                                }
                            }
                        )
                    }
                    is UiState.Success -> items(q.data.queue, key = { it.id }) { item ->
                        QueueRow(item, busy = state.actionBusy, onComplete = { viewModel.complete(item.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueRow(item: QueueItem, busy: Boolean, onComplete: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val inProgress = item.queueStatus == "in_progress"
    GlassCard(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(
                icon = if (inProgress) Icons.Filled.Work else Icons.Filled.Queue,
                tileSize = 44.dp,
                selected = inProgress,
                contentDescription = null
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "شماره ${Jalali.persianDigits(item.position)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = cs.primary
                )
                Text(item.customerName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                item.customerPhone?.let {
                    Text(Jalali.persianDigits(it), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
            }
            StatusChip(
                if (inProgress) "در حال خدمت" else "در انتظار",
                if (inProgress) cs.onPrimaryContainer else cs.onSurfaceVariant,
                if (inProgress) cs.primaryContainer else cs.surfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        InfoRow("زمان", "${Jalali.persianDigits(item.startTime)} تا ${Jalali.persianDigits(item.endTime)}")
        InfoRow("زمان تقریبی انتظار", Jalali.minutesToPersian(item.etaMinutes))
        Spacer(Modifier.height(8.dp))
        GradientButton(
            onClick = onComplete,
            enabled = !busy && item.queueStatus != "completed",
            palette = if (inProgress) TilePalette.AMBER else TilePalette.GREEN,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (inProgress) "تکمیل و رهاسازی" else "فراخوانی", fontWeight = FontWeight.Bold)
        }
    }
}
