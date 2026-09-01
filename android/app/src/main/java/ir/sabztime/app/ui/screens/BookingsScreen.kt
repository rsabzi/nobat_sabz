package ir.sabztime.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.core.SabzException
import ir.sabztime.app.core.UiState
import ir.sabztime.app.core.toSabzException
import ir.sabztime.app.data.BookingSummary
import ir.sabztime.app.ui.Routes
import androidx.compose.foundation.layout.width
import ir.sabztime.app.ui.components.EmojiBadge3D
import ir.sabztime.app.ui.components.EmptyState
import ir.sabztime.app.ui.components.ErrorState
import ir.sabztime.app.ui.components.GlassCard
import ir.sabztime.app.ui.components.StatusChip
import ir.sabztime.app.util.Jalali
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
            } catch (e: Exception) {
                val sabz = e.toSabzException()
                _state.value = _state.value.copy(upcoming = UiState.Error(sabz.toFriendlyMessage(), sabz.kind))
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
            } catch (e: Exception) {
                val sabz = e.toSabzException()
                _state.value = _state.value.copy(history = UiState.Error(sabz.toFriendlyMessage(), sabz.kind))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(graph: AppGraph, navController: NavHostController) {
    val viewModel: BookingsViewModel = viewModel { BookingsViewModel(graph) }
    val state by viewModel.state.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("نوبت‌های من", fontWeight = FontWeight.Bold) },
                actions = { NotificationBadgeButton { navController.navigate(Routes.NOTIFICATIONS) } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = state.tab) {
                Tab(selected = state.tab == 0, onClick = { viewModel.selectTab(0) }, text = { Text("نوبت‌های پیش رو") })
                Tab(selected = state.tab == 1, onClick = { viewModel.selectTab(1) }, text = { Text("سابقه") })
            }

            val ui = if (state.tab == 0) state.upcoming else state.history
            when (ui) {
                is UiState.Loading -> CircularProgressIndicator(Modifier.padding(40.dp).align(Alignment.CenterHorizontally))
                is UiState.Error -> ErrorState(ui.message, ui.kind, onRetry = viewModel::refreshAll)
                UiState.Empty -> EmptyState(
                    icon = "📅",
                    title = if (state.tab == 0) "نوبت فعالی ندارید" else "سابقه‌ای ثبت نشده",
                    subtitle = "از بخش جستجو، کسب‌وکار موردنظر خود را پیدا و نوبت رزرو کنید.",
                    action = {
                        androidx.compose.material3.Button(onClick = { viewModel.selectTab(0); navController.navigate(Routes.MAIN) }) {
                            Text("جستجوی کسب‌وکار")
                        }
                    }
                )
                is UiState.Success -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(ui.data, key = { it.id }) { b ->
                        BookingCard(b, onClick = { navController.navigate(Routes.booking(b.id)) })
                    }
                }
            }
        }
    }
}

@Composable
fun BookingCard(b: BookingSummary, onClick: () -> Unit, badge: (@Composable () -> Unit)? = null) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge3D("🗓", size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(b.businessName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${Jalali.formatLong(b.bookingDate)} — ${Jalali.persianDigits(b.startTime)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "کد: ${Jalali.persianDigits(b.confirmationCode)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (badge != null) badge()
            StatusChip(statusText(b.status), statusColor(b.status), statusContainer(b.status))
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
fun statusColor(status: String): androidx.compose.ui.graphics.Color = when (status) {
    "cancelled" -> androidx.compose.material3.MaterialTheme.colorScheme.error
    "completed" -> androidx.compose.ui.graphics.Color(0xFF4A66C4)
    else -> androidx.compose.material3.MaterialTheme.colorScheme.primary
}

@Composable
fun statusContainer(status: String): androidx.compose.ui.graphics.Color = when (status) {
    "cancelled" -> androidx.compose.material3.MaterialTheme.colorScheme.errorContainer
    "completed" -> androidx.compose.ui.graphics.Color(0xFFE0E7FF)
    else -> androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
}
