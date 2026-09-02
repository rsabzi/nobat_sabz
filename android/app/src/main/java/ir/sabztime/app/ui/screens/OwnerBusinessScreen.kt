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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.core.SabzException
import ir.sabztime.app.core.UiState
import ir.sabztime.app.data.QueueItem
import ir.sabztime.app.data.QueueResponse
import ir.sabztime.app.data.SubscriptionStatusResponse
import ir.sabztime.app.data.Service
import ir.sabztime.app.ui.components.EmptyState
import ir.sabztime.app.ui.components.ErrorState
import ir.sabztime.app.ui.components.InfoRow
import ir.sabztime.app.ui.components.SectionCard
import ir.sabztime.app.ui.components.StatusChip
import ir.sabztime.app.util.Jalali
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OwnerState(
    val queue: UiState<QueueResponse> = UiState.Loading,
    val subscription: SubscriptionStatusResponse? = null,
    val actionBusy: Boolean = false,
    val actionMessage: String? = null
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
            } catch (e: Exception) {
                val sabz = e as? SabzException ?: SabzException.Network(e)
                _state.value = _state.value.copy(queue = UiState.Error(sabz.toFriendlyMessage(), sabz.kind))
            }
        }
    }

    fun refreshSubscription() {
        viewModelScope.launch {
            try {
                val s = graph.repository.subscriptionStatus(businessId)
                _state.value = _state.value.copy(subscription = s)
            } catch (e: Exception) { /* اختیاری */ }
        }
    }

    fun addService(name: String, price: Long, durationMinutes: Int, onDone: () -> Unit) {
        _state.value = _state.value.copy(actionBusy = true, actionMessage = null)
        viewModelScope.launch {
            try {
                val existing = graph.repository.businessDetails(businessId)?.services.orEmpty()
                graph.repository.updateServices(
                    businessId,
                    existing + Service(id = 0, name = name, price = price.toString(), durationMinutes = durationMinutes)
                )
                _state.value = _state.value.copy(actionBusy = false, actionMessage = "خدمت جدید ذخیره شد. ✅")
                onDone()
            } catch (e: Exception) {
                val sabz = e as? SabzException ?: SabzException.Network(e)
                _state.value = _state.value.copy(actionBusy = false, actionMessage = sabz.toFriendlyMessage())
            }
        }
    }

    fun advance() {
        _state.value = _state.value.copy(actionBusy = true, actionMessage = null)
        viewModelScope.launch {
            try {
                graph.repository.advanceQueue(businessId)
                _state.value = _state.value.copy(actionBusy = false, actionMessage = "صف به‌روزرسانی شد. ✅")
                refreshQueue()
            } catch (e: Exception) {
                val sabz = e as? SabzException ?: SabzException.Network(e)
                _state.value = _state.value.copy(actionBusy = false, actionMessage = sabz.toFriendlyMessage())
            }
        }
    }

    fun complete(bookingId: Int) {
        _state.value = _state.value.copy(actionBusy = true, actionMessage = null)
        viewModelScope.launch {
            try {
                graph.repository.completeBooking(businessId, bookingId)
                _state.value = _state.value.copy(actionBusy = false, actionMessage = "رزرو تکمیل شد. ✅")
                refreshQueue()
            } catch (e: Exception) {
                val sabz = e as? SabzException ?: SabzException.Network(e)
                _state.value = _state.value.copy(actionBusy = false, actionMessage = sabz.toFriendlyMessage())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerBusinessScreen(graph: AppGraph, businessId: Int, navController: NavHostController) {
    val viewModel: OwnerBusinessViewModel = viewModel(key = "own-$businessId") { OwnerBusinessViewModel(graph, businessId) }
    val state by viewModel.state.collectAsState()
    var showServiceDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مدیریت کسب‌وکار", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // اشتراک
                state.subscription?.let { s ->
                    SectionCard {
                        Text("اشتراک و ظرفیت", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        InfoRow("نوبت‌های رایگان باقی‌مانده", Jalali.persianDigits(s.freeTokensRemaining))
                        InfoRow("پلن فعلی", s.subscriptionPlan ?: "—")
                        InfoRow("انقضا", Jalali.formatLong(s.expiryDate))
                        s.referralCode?.let {
                            InfoRow("لینک اختصاصی", "@${it}")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { navController.navigate(ir.sabztime.app.ui.Routes.subscription(businessId)) }, modifier = Modifier.fillMaxWidth()) {
                            Text("مشاهده پلن‌ها و خرید اشتراک")
                        }
                    }
                }
            }

            item {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("خدمات کسب‌وکار", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("خدمت و مدت زمان قابل رزرو را مستقیماً از اپ اضافه کنید.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = { showServiceDialog = true }) { Text("افزودن خدمت") }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("صف امروز", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Button(
                        onClick = viewModel::advance,
                        enabled = !state.actionBusy
                    ) { Text("نوبت بعدی ⏭") }
                }
            }

            state.actionMessage?.let {
                item { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }

            when (val q = state.queue) {
                is UiState.Loading -> item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> item { ErrorState(q.message, q.kind, onRetry = viewModel::refreshQueue) }
                UiState.Empty -> item {
                    EmptyState(
                        icon = "🗓",
                        title = "صف امروز خالی است",
                        subtitle = "رزروهای امروز اینجا نمایش داده می‌شوند.",
                        action = { Button(onClick = viewModel::refreshQueue) { Text("تازه‌سازی") } }
                    )
                }
                is UiState.Success -> items(q.data.queue, key = { it.id }) { item ->
                    QueueRow(item, busy = state.actionBusy, onComplete = { viewModel.complete(item.id) })
                }
            }
        }
    }

    if (showServiceDialog) {
        AddServiceDialog(
            onDismiss = { showServiceDialog = false },
            onSubmit = { name, price, duration ->
                viewModel.addService(name, price, duration) { showServiceDialog = false }
            }
        )
    }
}

@Composable
private fun AddServiceDialog(onDismiss: () -> Unit, onSubmit: (String, Long, Int) -> Unit) {
    var name by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var price by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var duration by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("30") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن خدمت") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("نام خدمت") }, singleLine = true)
                OutlinedTextField(price, { price = it }, label = { Text("قیمت (تومان)") }, singleLine = true)
                OutlinedTextField(duration, { duration = it }, label = { Text("مدت (دقیقه)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(name, price.toLongOrNull() ?: 0, duration.toIntOrNull() ?: 30) },
                enabled = name.isNotBlank() && (duration.toIntOrNull() ?: 0) > 0
            ) { Text("ذخیره خدمت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun QueueRow(item: QueueItem, busy: Boolean, onComplete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (item.queueStatus == "in_progress") MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "#${Jalali.persianDigits(item.position)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(0.dp))
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(item.customerName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    item.customerPhone?.let {
                        Text(Jalali.persianDigits(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                StatusChip(
                    if (item.queueStatus == "in_progress") "در حال خدمت" else "در انتظار",
                    if (item.queueStatus == "in_progress") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    if (item.queueStatus == "in_progress") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            InfoRow("زمان", "${Jalali.persianDigits(item.startTime)} تا ${Jalali.persianDigits(item.endTime)}")
            InfoRow("زمان تقریبی انتظار", Jalali.minutesToPersian(item.etaMinutes))
            item.customerPhone?.let {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onComplete,
                    enabled = !busy && item.queueStatus != "completed",
                    modifier = Modifier.fillMaxWidth()
                ) { Text("تکمیل و رهاسازی") }
            }
        }
    }
}
