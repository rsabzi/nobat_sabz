package ir.sabztime.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.core.SabzException
import ir.sabztime.app.core.UiState
import ir.sabztime.app.core.classify
import ir.sabztime.app.data.BookingSummary
import ir.sabztime.app.ui.Routes
import ir.sabztime.app.ui.components.EmptyState
import ir.sabztime.app.ui.components.ErrorState
import ir.sabztime.app.ui.theme.GlassBackground
import ir.sabztime.app.ui.components.GlassCard
import ir.sabztime.app.ui.components.GlassTopBar
import ir.sabztime.app.ui.components.GradientButton
import ir.sabztime.app.ui.components.NotificationIconButton
import ir.sabztime.app.ui.components.SegmentedControl
import ir.sabztime.app.ui.components.StatusChip
import ir.sabztime.app.util.Jalali
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BookingsUiState(
    val upcoming: UiState<List<BookingSummary>> = UiState.Loading,
    val history: UiState<List<BookingSummary>> = UiState.Loading,
    val tab: Int = 0
)

class BookingsViewModel(private val graph: AppGraph) : ViewModel() {
    private val _state = MutableStateFlow(BookingsUiState())
    val state: StateFlow<BookingsUiState> = _state.asStateFlow()

    init {
        refreshAll()
    }

    fun selectTab(t: Int) {
        _state.value = _state.value.copy(tab = t)
    }

    fun refreshAll() {
        refreshUpcoming()
        refreshHistory()
    }

    fun refreshUpcoming() {
        viewModelScope.launch {
            _state.value = _state.value.copy(upcoming = UiState.Loading)
            try {
                val list = graph.repository.myBookings(includeHistory = false)
                _state.value = _state.value.copy(
                    upcoming = if (list.isEmpty()) UiState.Empty else UiState.Success(list)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val s = classify(e)
                _state.value = _state.value.copy(upcoming = UiState.Error(s.message, s.kind))
            }
        }
    }

    fun refreshHistory() {
        viewModelScope.launch {
            _state.value = _state.value.copy(history = UiState.Loading)
            try {
                val list = graph.repository.myBookings(includeHistory = true)
                _state.value = _state.value.copy(
                    history = if (list.isEmpty()) UiState.Empty else UiState.Success(list)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val s = classify(e)
                _state.value = _state.value.copy(history = UiState.Error(s.message, s.kind))
            }
        }
    }
}

@Composable
fun BookingsScreen(
    graph: AppGraph,
    navController: NavHostController,
    onGoHome: (() -> Unit)? = null
) {
    val viewModel: BookingsViewModel = viewModel { BookingsViewModel(graph) }
    val state by viewModel.state.collectAsState()

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                GlassTopBar(
                    title = "نوبت‌های من",
                    actions = { NotificationIconButton { navController.navigate(Routes.NOTIFICATIONS) } }
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Spacer(Modifier.height(4.dp))
                SegmentedControl(
                    options = listOf("نوبت‌های پیش رو", "سابقه"),
                    selected = state.tab,
                    onSelect = viewModel::selectTab,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(10.dp))

                val ui = if (state.tab == 0) state.upcoming else state.history
                when (ui) {
                    is UiState.Loading -> Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                    is UiState.Error -> ErrorState(ui.message, ui.kind, onRetry = viewModel::refreshAll)
                    UiState.Empty -> EmptyState(
                        icon = "📅",
                        title = if (state.tab == 0) "نوبت فعالی ندارید" else "سابقه‌ای ثبت نشده",
                        subtitle = "از بخش جستجو، کسب‌وکار موردنظر خود را پیدا و نوبت رزرو کنید.",
                        action = {
                            if (onGoHome != null) {
                                GradientButton(onClick = onGoHome) {
                                    Text("جستجوی کسب‌وکار", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    )
                    is UiState.Success -> LazyColumn(
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(ui.data, key = { it.id }) { b ->
                            BookingCard(b, onClick = { navController.navigate(Routes.booking(b.id)) })
                        }
                        item { ir.sabztime.app.ui.components.BottomBarSpacer() }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingCard(b: BookingSummary, onClick: () -> Unit, badge: (@Composable () -> Unit)? = null) {
    val parts = Jalali.parts(b.bookingDate)
    GlassCard(onClick = onClick, shape = RoundedCornerShape(25.dp), pattern = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // بلوک تاریخ
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                ir.sabztime.app.ui.theme.GoldLight,
                                ir.sabztime.app.ui.theme.GoldDim
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        parts?.let { Jalali.persianDigits(it.third) } ?: "—",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ir.sabztime.app.ui.theme.MidnightDeep
                    )
                    Text(
                        parts?.let { Jalali.monthName(it.second) } ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = ir.sabztime.app.ui.theme.MidnightDeep
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    b.businessName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${Jalali.persianDigits(b.startTime)} تا ${Jalali.persianDigits(b.endTime)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "کد: ${Jalali.persianDigits(b.confirmationCode)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (badge != null) badge() else {
                StatusChip(statusText(b.status), statusColor(b.status), statusContainer(b.status))
            }
        }
    }
}

fun statusText(status: String): String = when (status) {
    "confirmed" -> "تأیید شده"
    "cancelled" -> "لغو شده"
    "completed" -> "انجام شده"
    "pending" -> "در انتظار"
    else -> "ثبت شده"
}

@Composable
fun statusColor(status: String): Color = when (status) {
    "cancelled" -> ir.sabztime.app.ui.theme.DangerRose
    "completed" -> ir.sabztime.app.ui.theme.SuccessEmerald
    else -> ir.sabztime.app.ui.theme.GoldLight
}

@Composable
fun statusContainer(status: String): Color = when (status) {
    "cancelled" -> ir.sabztime.app.ui.theme.DangerRoseDeep.copy(alpha = 0.35f)
    "completed" -> ir.sabztime.app.ui.theme.SuccessEmeraldDeep.copy(alpha = 0.30f)
    else -> ir.sabztime.app.ui.theme.GoldDeep.copy(alpha = 0.28f)
}
