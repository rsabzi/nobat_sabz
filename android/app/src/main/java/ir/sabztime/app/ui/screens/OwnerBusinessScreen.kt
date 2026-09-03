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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.core.UiState
import ir.sabztime.app.core.classify
import ir.sabztime.app.data.BusinessDetails
import ir.sabztime.app.data.BusinessBookingsResponse
import ir.sabztime.app.data.BusinessBooking
import ir.sabztime.app.data.DaySchedule
import ir.sabztime.app.data.QueueItem
import ir.sabztime.app.data.QueueResponse
import ir.sabztime.app.data.ScheduleData
import ir.sabztime.app.data.Service
import ir.sabztime.app.ui.Routes
import ir.sabztime.app.ui.components.EmptyState
import ir.sabztime.app.ui.components.ErrorState
import ir.sabztime.app.ui.components.businessTypeIcon
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
    val actionBusy: Boolean = false,
    val actionMessage: String? = null,
    val actionError: Boolean = false
)

class OwnerBusinessViewModel(private val graph: AppGraph, private val businessId: Int) : ViewModel() {
    private val _state = MutableStateFlow(OwnerState())
    val state: StateFlow<OwnerState> = _state.asStateFlow()

    private val _services = MutableStateFlow<List<Service>>(emptyList())
    val services: StateFlow<List<Service>> = _services.asStateFlow()

    private val _business = MutableStateFlow<BusinessDetails?>(null)
    val business: StateFlow<BusinessDetails?> = _business.asStateFlow()

    private val _schedule = MutableStateFlow<ScheduleData?>(null)
    val schedule: StateFlow<ScheduleData?> = _schedule.asStateFlow()

    private val _bookings = MutableStateFlow<UiState<BusinessBookingsResponse>>(UiState.Loading)
    val bookings: StateFlow<UiState<BusinessBookingsResponse>> = _bookings.asStateFlow()

    private val _bookingsAll = MutableStateFlow(false)
    val bookingsAll: StateFlow<Boolean> = _bookingsAll.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        loadBusiness()
        refreshQueue()
        refreshServices()
        loadSchedule()
        refreshBookings()
    }

    fun loadBusiness() {
        viewModelScope.launch {
            try {
                _business.value = graph.repository.businessForEdit(businessId)
            } catch (e: Exception) {
                // اختیاری؛ صفحه با داده‌های دیگر هم کاربردی می‌ماند
            }
        }
    }

    fun refreshServices() {
        viewModelScope.launch {
            try {
                _services.value = graph.repository.businessDetails(businessId)?.services.orEmpty()
            } catch (e: Exception) {
                // اختیاری
            }
        }
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

    fun loadSchedule() {
        viewModelScope.launch {
            try {
                _schedule.value = graph.repository.getSchedule(businessId)
            } catch (e: Exception) {
                // اختیاری
            }
        }
    }

    fun refreshBookings() {
        _bookings.value = UiState.Loading
        viewModelScope.launch {
            try {
                val r = graph.repository.businessBookings(businessId, _bookingsAll.value)
                _bookings.value =
                    if (r.bookings.isEmpty()) UiState.Empty else UiState.Success(r)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val s = classify(e)
                _bookings.value = UiState.Error(s.message, s.kind)
            }
        }
    }

    fun setBookingsAll(all: Boolean) {
        if (_bookingsAll.value == all) return
        _bookingsAll.value = all
        refreshBookings()
    }

    // --- ویرایش/حذف کسب‌وکار ---

    fun editBusiness(name: String, type: String, phone: String, province: String, city: String, address: String) {
        _state.value = _state.value.copy(actionBusy = true, actionMessage = null, actionError = false)
        viewModelScope.launch {
            try {
                graph.repository.updateBusiness(businessId, name, type, phone, province, city, address)
                _state.value = _state.value.copy(actionBusy = false, actionMessage = "تغییرات کسب‌وکار ذخیره شد. ✅", actionError = false)
                loadBusiness()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val s = classify(e)
                _state.value = _state.value.copy(actionBusy = false, actionMessage = s.message, actionError = true)
            }
        }
    }

    fun deleteBusiness(onDone: () -> Unit) {
        _state.value = _state.value.copy(actionBusy = true, actionMessage = null, actionError = false)
        viewModelScope.launch {
            try {
                graph.repository.deleteBusiness(businessId)
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val s = classify(e)
                _state.value = _state.value.copy(actionBusy = false, actionMessage = s.message, actionError = true)
            }
        }
    }

    // --- برنامهٔ کاری ---

    fun saveSchedule(days: List<DaySchedule>, slotDurationMinutes: Int) {
        _state.value = _state.value.copy(actionBusy = true, actionMessage = null, actionError = false)
        viewModelScope.launch {
            try {
                graph.repository.saveSchedule(businessId, slotDurationMinutes, days)
                _state.value = _state.value.copy(actionBusy = false, actionMessage = "برنامهٔ کاری ذخیره شد. ✅", actionError = false)
                loadSchedule()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val s = classify(e)
                _state.value = _state.value.copy(actionBusy = false, actionMessage = s.message, actionError = true)
            }
        }
    }

    // --- لینک اختصاصی ---

    fun saveLinkAlias(alias: String) {
        _state.value = _state.value.copy(actionBusy = true, actionMessage = null, actionError = false)
        viewModelScope.launch {
            try {
                graph.repository.updateLinkAlias(businessId, alias.trim())
                _state.value = _state.value.copy(actionBusy = false, actionMessage = "لینک اختصاصی ذخیره شد. ✅", actionError = false)
                loadBusiness()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val s = classify(e)
                _state.value = _state.value.copy(actionBusy = false, actionMessage = s.message, actionError = true)
            }
        }
    }

    // --- خدمات ---

    /** افزودن خدمت جدید به فهرست خدمات کسب‌وکار (از طریق اندپوینت update_services). */
    fun addService(name: String, price: Long, durationMinutes: Int, onDone: () -> Unit) {
        _state.value = _state.value.copy(actionBusy = true, actionMessage = null, actionError = false)
        viewModelScope.launch {
            try {
                val existing = graph.repository.businessDetails(businessId)?.services.orEmpty()
                graph.repository.updateServices(
                    businessId,
                    existing + Service(id = 0, name = name, price = price.toString(), durationMinutes = durationMinutes)
                )
                _state.value = _state.value.copy(actionBusy = false, actionMessage = "خدمت جدید ذخیره شد. ✅", actionError = false)
                refreshServices()
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val s = classify(e)
                _state.value = _state.value.copy(actionBusy = false, actionMessage = s.message, actionError = true)
            }
        }
    }

    /** حذف یک خدمت از فهرست خدمات (ارسال فهرست بدون خدمتِ حذف‌شده). */
    fun deleteService(service: Service) {
        _state.value = _state.value.copy(actionBusy = true, actionMessage = null, actionError = false)
        viewModelScope.launch {
            try {
                val remaining = graph.repository.businessDetails(businessId)?.services.orEmpty()
                    .filter { it.id != service.id }
                graph.repository.updateServices(businessId, remaining)
                _state.value = _state.value.copy(actionBusy = false, actionMessage = "خدمت حذف شد. ✅", actionError = false)
                refreshServices()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val s = classify(e)
                _state.value = _state.value.copy(actionBusy = false, actionMessage = s.message, actionError = true)
            }
        }
    }

    // --- صف و حضور/غیبت ---

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

    fun markAttendance(booking: BusinessBooking, status: String) {
        _state.value = _state.value.copy(actionBusy = true, actionMessage = null, actionError = false)
        viewModelScope.launch {
            try {
                val r = graph.repository.recordAttendance(businessId, booking.id, status)
                _state.value = _state.value.copy(
                    actionBusy = false,
                    actionMessage = if (r.status == "success") (r.message ?: "وضعیت حضور ثبت شد. ✅") else (r.message ?: "خطا در ثبت حضور"),
                    actionError = r.status != "success"
                )
                refreshBookings()
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
    val services by viewModel.services.collectAsState()
    val business by viewModel.business.collectAsState()
    val schedule by viewModel.schedule.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val bookingsAll by viewModel.bookingsAll.collectAsState()
    var showServiceDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

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
                    // مشخصات کسب‌وکار + ویرایش/حذف
                    GlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconTile(
                                icon = businessTypeIcon(business?.businessType),
                                tileSize = 44.dp,
                                palette = TilePalette.GREEN,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    business?.name ?: "کسب‌وکار من",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                business?.let { b ->
                                    val meta = listOfNotNull(b.phone, b.city, b.address)
                                    if (meta.isNotEmpty()) {
                                        Text(
                                            meta.joinToString(" • "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { showEditDialog = true }) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = "ویرایش کسب‌وکار",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "حذف کسب‌وکار",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                item {
                    ScheduleCard(
                        schedule = schedule,
                        busy = state.actionBusy,
                        onSave = { days, slot -> viewModel.saveSchedule(days, slot) }
                    )
                }

                item {
                    LinkCard(
                        currentAlias = business?.linkAlias,
                        busy = state.actionBusy,
                        onEdit = { showLinkDialog = true },
                        onCopy = { link ->
                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(link))
                        }
                    )
                }

                item {
                    GlassCard {
                        Text(
                            "خدمات کسب‌وکار",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "خدمت و مدت زمان قابل رزرو را مستقیماً از اپ اضافه یا حذف کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (services.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                services.forEach { svc ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                svc.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                "${Jalali.persianDigits(svc.durationMinutes ?: 30)} دقیقه" +
                                                    (svc.priceValue?.let { " • ${Jalali.formatPrice(it)}" } ?: ""),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteService(svc) },
                                            enabled = !state.actionBusy
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "حذف خدمت",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        GradientButton(
                            onClick = { showServiceDialog = true },
                            enabled = !state.actionBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("افزودن خدمت", fontWeight = FontWeight.Bold)
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
                            subtitle = "رزروهای امروز اینجا نمایش داده می‌شوند."
                        )
                    }
                    is UiState.Success -> items(q.data.queue) { item ->
                        QueueRow(item, busy = state.actionBusy, onComplete = { viewModel.complete(item.id) })
                    }
                }

                // --- فهرست رزروها ---
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "فهرست رزروها",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !bookingsAll,
                            onClick = { viewModel.setBookingsAll(false) },
                            label = { Text("امروز به بعد") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = bookingsAll,
                            onClick = { viewModel.setBookingsAll(true) },
                            label = { Text("همه") }
                        )
                    }
                }

                when (val b = bookings) {
                    is UiState.Loading -> item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    is UiState.Error -> item { ErrorState(b.message, b.kind, onRetry = viewModel::refreshBookings) }
                    UiState.Empty -> item {
                        EmptyState(
                            icon = "📋",
                            title = "رزروی ثبت نشده است",
                            subtitle = "به‌محض اینکه مشتری‌ها نوبت بگیرند، اینجا نمایش داده می‌شوند."
                        )
                    }
                    is UiState.Success -> items(b.data.bookings) { booking ->
                        BookingRow(
                            booking = booking,
                            busy = state.actionBusy,
                            onAttended = { viewModel.markAttendance(booking, "attended") },
                            onNoShow = { viewModel.markAttendance(booking, "no_show") }
                        )
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
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
        if (showEditDialog) {
            business?.let { b ->
                EditBusinessDialog(
                    initial = b,
                    busy = state.actionBusy,
                    onDismiss = { showEditDialog = false },
                    onSubmit = { name, type, phone, province, city, address ->
                        viewModel.editBusiness(name, type, phone, province, city, address)
                        showEditDialog = false
                    }
                )
            }
        }
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { if (!state.actionBusy) showDeleteDialog = false },
                title = { Text("حذف کسب‌وکار", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "با حذف «${business?.name ?: "این کسب‌وکار"}» همهٔ رزروها و تنظیمات آن برای همیشه پاک می‌شود. این عمل قابل بازگشت نیست."
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.deleteBusiness {
                            showDeleteDialog = false
                            navController.popBackStack()
                        }
                    }, enabled = !state.actionBusy) {
                        Text(if (state.actionBusy) "در حال حذف…" else "حذف کامل")
                    }
                },
                dismissButton = { TextButton(onClick = { showDeleteDialog = false }, enabled = !state.actionBusy) { Text("انصراف") } }
            )
        }
        if (showLinkDialog) {
            LinkAliasDialog(
                initialAlias = business?.linkAlias,
                busy = state.actionBusy,
                onDismiss = { showLinkDialog = false },
                onSubmit = { alias ->
                    viewModel.saveLinkAlias(alias)
                    showLinkDialog = false
                }
            )
        }
    }
}

// --- دیالوگ‌ها و کارت‌های کمکی در فایل جدا: OwnerExtras.kt ---

@Composable
private fun AddServiceDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, Long, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("30") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن خدمت", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام خدمت") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("قیمت (تومان)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("مدت (دقیقه)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
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
