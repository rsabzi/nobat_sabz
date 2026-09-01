package ir.sabztime.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import ir.sabztime.app.data.BookingSummary
import ir.sabztime.app.data.CustomerQueueInfoResponse
import ir.sabztime.app.ui.components.ErrorState
import ir.sabztime.app.ui.components.InfoRow
import ir.sabztime.app.ui.components.LoadingState
import ir.sabztime.app.ui.components.SectionCard
import ir.sabztime.app.ui.components.StatusChip
import ir.sabztime.app.ui.components.collectAsStateSafe
import ir.sabztime.app.util.Jalali
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BookingDetailState(
    val booking: UiState<BookingSummary> = UiState.Loading,
    val queue: CustomerQueueInfoResponse? = null,
    val cancelBusy: Boolean = false,
    val cancelled: Boolean = false,
    val cancelError: String? = null
)

class BookingDetailViewModel(private val graph: AppGraph, private val bookingId: Int) : ViewModel() {
    private val _state = MutableStateFlow(BookingDetailState())
    val state: StateFlow<BookingDetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = _state.value.copy(booking = UiState.Loading)
        viewModelScope.launch {
            try {
                val all = graph.repository.myBookings(includeHistory = true)
                val b = all.find { it.id == bookingId }
                if (b == null) {
                    _state.value = _state.value.copy(booking = UiState.Error("رزرو پیدا نشد."))
                    return@launch
                }
                _state.value = _state.value.copy(booking = UiState.Success(b))
                if (b.status != "cancelled") loadQueue()
            } catch (e: Exception) {
                val sabz = e as? SabzException ?: SabzException.Network(e)
                _state.value = _state.value.copy(booking = UiState.Error(sabz.toFriendlyMessage(), sabz.kind))
            }
        }
    }

    fun loadQueue() {
        val b = (_state.value.booking as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            try {
                val q = graph.repository.myQueueInfo(b.id)
                _state.value = _state.value.copy(queue = q)
            } catch (e: Exception) {
                // صف در دسترس نبود — غیرضروری است
            }
        }
    }

    fun cancel() {
        _state.value = _state.value.copy(cancelBusy = true, cancelError = null)
        viewModelScope.launch {
            try {
                val r = graph.repository.cancelBooking(bookingId)
                _state.value = _state.value.copy(cancelBusy = false, cancelled = true)
            } catch (e: Exception) {
                val sabz = e as? SabzException ?: SabzException.Network(e)
                _state.value = _state.value.copy(cancelBusy = false, cancelError = sabz.toFriendlyMessage())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(graph: AppGraph, bookingId: Int, navController: NavHostController) {
    val viewModel: BookingDetailViewModel = viewModel(key = "bk-$bookingId") { BookingDetailViewModel(graph, bookingId) }
    val state by viewModel.state.collectAsStateSafe()
    var confirmCancel by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("جزئیات نوبت", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Filled.Refresh, contentDescription = "تازه‌سازی")
                    }
                }
            )
        }
    ) { padding ->
        when (val b = state.booking) {
            is UiState.Loading -> LoadingState(Modifier.padding(padding))
            is UiState.Error -> ErrorState(b.message, b.kind, onRetry = viewModel::load, modifier = Modifier.padding(padding))
            is UiState.Success -> {
                val booking = b.data
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(booking.businessName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            StatusChip(statusText(booking.status), statusColor(booking.status), statusContainer(booking.status))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${Jalali.formatLong(booking.bookingDate)} — ${booking.dayName ?: Jalali.weekdayName(booking.bookingDate)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "ساعت ${Jalali.persianDigits(booking.startTime)} تا ${Jalali.persianDigits(booking.endTime)}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    SectionCard {
                        InfoRow("کد رزرو", Jalali.persianDigits(booking.confirmationCode))
                        InfoRow("نام مشتری", booking.customerName)
                        InfoRow("شماره تماس", Jalali.persianDigits(booking.customerPhone))
                        if (!booking.customerNotes.isNullOrBlank()) {
                            InfoRow("توضیحات", booking.customerNotes)
                        }
                        if (!booking.businessPhone.isNullOrBlank()) {
                            InfoRow("تلفن کسب‌وکار", Jalali.persianDigits(booking.businessPhone))
                        }
                        if (!booking.businessAddress.isNullOrBlank()) {
                            InfoRow("آدرس", booking.businessAddress)
                        }
                    }

                    // وضعیت صف
                    state.queue?.let { q ->
                        if (q.position != null || q.totalActive > 0) {
                            SectionCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("وضعیت صف", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.weight(1f))
                                    IconButton(onClick = viewModel::loadQueue) {
                                        Icon(Icons.Filled.Refresh, contentDescription = "بروزرسانی صف", modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                if (q.position != null) {
                                    InfoRow("جایگاه شما", "${Jalali.persianDigits(q.position)} از ${Jalali.persianDigits(q.totalActive)}")
                                    q.etaMinutes?.let { InfoRow("زمان تقریبی", "حدود ${Jalali.minutesToPersian(it)}") }
                                } else {
                                    Text(
                                        "هنوز نوبت شما در صف فعال دیده نشده است.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    state.cancelError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }

                    if (state.cancelled) {
                        SectionCard {
                            Text("❌ این نوبت لغو شد", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                            Text(
                                "زمان آزادشده برای رزرو مجدد در دسترس است.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                                Text("بستن")
                            }
                        }
                    } else if (booking.canCancel && booking.status != "cancelled") {
                        OutlinedButton(
                            onClick = { confirmCancel = true },
                            enabled = !state.cancelBusy,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            if (state.cancelBusy) CircularProgressIndicator(Modifier.size(20.dp))
                            else Text("لغو نوبت", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "لغو فقط تا یک ساعت قبل از شروع نوبت ممکن است و ۱ امتیاز از اعتبار شما کسر می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("لغو نوبت") },
            text = { Text("آیا مطمئن هستید؟ این نوبت لغو می‌شود و یک امتیاز از اعتبار شما کسر خواهد شد.") },
            confirmButton = {
                Button(
                    onClick = { confirmCancel = false; viewModel.cancel() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("بله، لغو کن") }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("منصرف شدم") } }
        )
    }
}
