package ir.sabztime.app.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.core.SabzException
import ir.sabztime.app.core.UiState
import ir.sabztime.app.core.classify
import ir.sabztime.app.data.BookingCreatedResponse
import ir.sabztime.app.data.BusinessDetails
import ir.sabztime.app.data.DateSlot
import ir.sabztime.app.data.Service
import ir.sabztime.app.data.TimeSlot
import ir.sabztime.app.data.sanitizeDates
import ir.sabztime.app.data.sanitizeTimeSlots
import ir.sabztime.app.ui.Routes
import ir.sabztime.app.ui.components.EmptyState
import ir.sabztime.app.ui.components.ErrorState
import ir.sabztime.app.ui.theme.GlassBackground
import ir.sabztime.app.ui.components.GlassCard
import ir.sabztime.app.ui.components.GlassChip
import ir.sabztime.app.ui.components.GlassTopBar
import ir.sabztime.app.ui.components.GradientButton
import ir.sabztime.app.ui.components.IconTile
import ir.sabztime.app.ui.components.InfoRow
import ir.sabztime.app.ui.components.LoadingState
import ir.sabztime.app.ui.components.StatusChip
import ir.sabztime.app.ui.components.TilePalette
import ir.sabztime.app.ui.components.businessTypeIcon
import ir.sabztime.app.util.Jalali
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BusinessDetailState(
    val business: UiState<BusinessDetails> = UiState.Loading,
    val dates: List<DateSlot> = emptyList(),
    val datesLoading: Boolean = false,
    val datesError: String? = null,
    val selectedDate: DateSlot? = null,
    val slots: List<TimeSlot> = emptyList(),
    val slotsLoading: Boolean = false,
    val slotsError: String? = null,
    val selectedSlot: TimeSlot? = null,
    val selectedServiceId: Int? = null,
    val submit: UiState<BookingCreatedResponse>? = null,
    val showForm: Boolean = false,
    val prefillName: String = "",
    val prefillPhone: String = ""
)

class BusinessDetailViewModel(private val graph: AppGraph, private val businessId: Int) : ViewModel() {
    private val _state = MutableStateFlow(BusinessDetailState())
    val state: StateFlow<BusinessDetailState> = _state.asStateFlow()

    var userId: Long = 0
        private set

    /**
     * فقط آخرین درخواست اسلات معتبر است؛ با عوض‌کردن سریع تاریخ، پاسخ قبلی
     * باید دور ریخته شود تا زمان‌های تاریخ نادرست نمایش داده نشود.
     */
    private var slotsJob: Job? = null

    init {
        load()
        viewModelScope.launch {
            try {
                val me = graph.repository.me()
                userId = me.user.telegramId
                _state.update { it.copy(prefillName = me.user.name, prefillPhone = me.user.phone) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // مهمان — فرم را دستی پر می‌کند
            }
        }
    }

    fun load() {
        _state.update { it.copy(business = UiState.Loading) }
        viewModelScope.launch {
            try {
                val b = graph.repository.businessDetails(businessId)
                    ?: throw SabzException.Server("کسب‌وکار پیدا نشد.", "NOT_FOUND", 404)
                _state.update { it.copy(business = UiState.Success(b)) }
                loadDates()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val s = classify(e)
                _state.update { it.copy(business = UiState.Error(s.message, s.kind)) }
            }
        }
    }

    fun loadDates() {
        _state.update { it.copy(datesLoading = true, datesError = null) }
        viewModelScope.launch {
            try {
                val d = graph.repository.availableDates(businessId)
                val duration = (_state.value.business as? UiState.Success)?.data?.slotDurationMinutes
                val normalized = sanitizeDates(d)
                // اولین تاریخ به‌طور خودکار انتخاب می‌شود تا زمان‌های همان روز بی‌درنگ بارگذاری شوند
                val keepSelected = normalized.firstOrNull { it.date == _state.value.selectedDate?.date }
                    ?: normalized.firstOrNull()
                _state.update {
                    it.copy(
                        dates = normalized,
                        datesLoading = false,
                        datesError = if (normalized.isEmpty() && d.isNotEmpty())
                            "ساختار پاسخ سرور برای تاریخ‌ها قابل خواندن نبود." else null,
                        selectedDate = keepSelected,
                        slots = emptyList(),
                        selectedSlot = null,
                        slotsError = null,
                        slotsLoading = keepSelected != null
                    )
                }
                keepSelected?.let { fetchSlots(it, duration) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val s = classify(e)
                _state.update { it.copy(datesLoading = false, datesError = s.message) }
            }
        }
    }

    fun selectDate(date: DateSlot) {
        if (_state.value.selectedDate?.date == date.date) return
        _state.update { it.copy(selectedDate = date, selectedSlot = null, slots = emptyList(), slotsError = null, slotsLoading = true) }
        val duration = (_state.value.business as? UiState.Success)?.data?.slotDurationMinutes
        fetchSlots(date, duration)
    }

    fun retryDates() = loadDates()

    fun retrySlots() {
        val date = _state.value.selectedDate ?: return
        _state.update { it.copy(slotsLoading = true, slotsError = null) }
        val duration = (_state.value.business as? UiState.Success)?.data?.slotDurationMinutes
        fetchSlots(date, duration)
    }

    /** بارگذاری زمان‌های یک تاریخ (با لغو درخواست قبلی و یکدست‌سازی پاسخ). */
    private fun fetchSlots(date: DateSlot, slotDurationMinutes: Int?) {
        slotsJob?.cancel()
        val iso = date.isoDate
        slotsJob = viewModelScope.launch {
            try {
                val raw = graph.repository.availableSlots(businessId, iso)
                val clean = sanitizeTimeSlots(raw, slotDurationMinutes)
                // اگر کاربر تا حالا تاریخ دیگری را انتخاب نکرده باشد، نتیجه اعمال می‌شود
                if (_state.value.selectedDate?.isoDate == iso) {
                    val keepSlot = _state.value.selectedSlot?.startTime
                    _state.update {
                        it.copy(
                            slots = clean,
                            slotsLoading = false,
                            slotsError = null,
                            selectedSlot = clean.firstOrNull { s -> s.startTime == keepSlot }
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val s = classify(e)
                if (_state.value.selectedDate?.isoDate == iso) {
                    _state.update {
                        it.copy(
                            slots = emptyList(),
                            slotsLoading = false,
                            // خطای واقعی سرور پنهان نمی‌شود؛ کاربر دکمهٔ «تلاش دوباره» می‌بیند
                            slotsError = s.message
                        )
                    }
                }
            }
        }
    }

    fun selectSlot(slot: TimeSlot) {
        _state.update { it.copy(selectedSlot = slot) }
    }

    fun selectService(id: Int) {
        _state.update { it.copy(selectedServiceId = if (it.selectedServiceId == id) null else id) }
    }

    fun openForm() {
        _state.update { it.copy(showForm = true, submit = null) }
    }

    fun closeForm() {
        _state.update { it.copy(showForm = false, submit = null) }
    }

    fun submitBooking(name: String, phone: String, notes: String) {
        val d = _state.value
        val date = d.selectedDate ?: return
        val slot = d.selectedSlot ?: return
        _state.update { it.copy(submit = UiState.Loading) }
        viewModelScope.launch {
            try {
                val resp = graph.repository.createBooking(
                    businessId = businessId,
                    date = date.isoDate.take(10), // همیشه YYYY-MM-DD؛ حتی اگر سرور datetime فرستاده باشد
                    startTime = slot.startTime,
                    endTime = slot.effectiveEndTime(
                        (_state.value.business as? UiState.Success)?.data?.slotDurationMinutes ?: 30
                    ),
                    customerName = name,
                    customerPhone = phone,
                    customerNotes = notes,
                    userId = userId
                )
                _state.update { it.copy(submit = UiState.Success(resp)) }
                // زمان رزروشده باید از فهرست خارج شود و تاریخ‌ها به‌روز شوند
                loadDates()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val s = classify(e)
                _state.update { it.copy(submit = UiState.Error(s.message, s.kind)) }
            }
        }
    }

    companion object {
        /**
         * تاریخ‌های تکراری/بی‌اعتبار حذف و مرتب می‌شوند.
         * (سرور ممکن است یک تاریخ را هم از `specific_time_slots` و هم از
         * `schedule_overrides` برگرداند؛ تکرار کلید، لیست Compose را می‌شکست.)
         */
        internal fun normalizeDates(dates: List<DateSlot>): List<DateSlot> = sanitizeDates(dates)

        /** زمان‌ها: یکدست، بدون تکرار و مرتب (با ساخت `end_time` غایب). */
        internal fun normalizeSlots(slots: List<TimeSlot>, slotDurationMinutes: Int?): List<TimeSlot> =
            sanitizeTimeSlots(slots, slotDurationMinutes)
    }
}

@Composable
fun BusinessDetailScreen(graph: AppGraph, businessId: Int, navController: NavHostController) {
    val viewModel: BusinessDetailViewModel = viewModel(key = "biz-$businessId") {
        BusinessDetailViewModel(graph, businessId)
    }
    val state by viewModel.state.collectAsState()
    val submitState = state.submit

    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var nameErr by remember { mutableStateOf<String?>(null) }
    var phoneErr by remember { mutableStateOf<String?>(null) }

    // پیش‌پر کردن اطلاعات تماس از پروفایل
    LaunchedEffect(state.prefillName, state.prefillPhone) {
        if (name.isBlank()) name = state.prefillName
        if (phone.isBlank()) phone = state.prefillPhone
    }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                GlassTopBar(
                    title = (state.business as? UiState.Success)?.data?.name ?: "کسب‌وکار",
                    onBack = { navController.popBackStack() }
                )
            }
        ) { padding ->
            when (val bState = state.business) {
                is UiState.Loading -> LoadingState(Modifier.fillMaxSize().padding(padding))
                is UiState.Error -> ErrorState(
                    bState.message,
                    bState.kind,
                    onRetry = viewModel::load,
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
                UiState.Empty -> EmptyState(
                    icon = "🏪",
                    title = "کسب‌وکار پیدا نشد",
                    subtitle = "ممکن است این کسب‌وکار حذف یا غیرفعال شده باشد.",
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
                is UiState.Success -> {
                    val b = bState.data
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // --- کارت کسب‌وکار ---
                        item {
                            GlassCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconTile(
                                        icon = businessTypeIcon(b.businessType),
                                        tileSize = 56.dp,
                                        contentDescription = null
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(b.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        b.businessType?.let {
                                            Spacer(Modifier.height(6.dp))
                                            StatusChip(
                                                it,
                                                MaterialTheme.colorScheme.onPrimaryContainer,
                                                MaterialTheme.colorScheme.primaryContainer
                                            )
                                        }
                                    }
                                }
                                if (!b.description.isNullOrBlank()) {
                                    Spacer(Modifier.height(10.dp))
                                    Text(b.description, style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(Modifier.height(10.dp))
                                if (!b.address.isNullOrBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.Place, null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(b.address, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                if (!b.phone.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.Phone, null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(Jalali.persianDigits(b.phone), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        // --- خدمات ---
                        if (b.services.isNotEmpty()) {
                            item { SectionHeader(Icons.Filled.Work, "خدمات") }
                            items(b.services, key = { "s${it.id}" }) { service ->
                                ServiceRow(
                                    service,
                                    selected = state.selectedServiceId == service.id,
                                    onSelect = { viewModel.selectService(service.id) }
                                )
                            }
                        }

                        // --- انتخاب تاریخ ---
                        item { SectionHeader(Icons.Filled.DateRange, "انتخاب تاریخ") }
                        item {
                            val datesErr = state.datesError
                            when {
                                state.datesLoading -> InlineLoading("در حال دریافت تاریخ‌های رزرو…")
                                datesErr != null && state.dates.isEmpty() -> InlineError(
                                    message = "تاریخ‌های رزرو بارگذاری نشد.\n$datesErr",
                                    onRetry = viewModel::retryDates
                                )
                                state.dates.isEmpty() -> Text(
                                    "برای این کسب‌وکار هنوز زمان رزرو ثبت نشده است.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (datesErr != null) {
                                        InlineError(message = datesErr, onRetry = viewModel::retryDates)
                                    }
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(state.dates, key = { it.date }) { d ->
                                            val sel = state.selectedDate?.date == d.date
                                            GlassChip(
                                                selected = sel,
                                                onClick = { viewModel.selectDate(d) }
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        Jalali.format(d.date),
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        listOfNotNull(
                                                            d.dayName?.takeIf { it.isNotBlank() }
                                                                ?: Jalali.weekdayName(d.date).takeIf { it.isNotBlank() },
                                                            d.slotCount?.takeIf { it > 0 }
                                                                ?.let { "${Jalali.persianDigits(it)} زمان" }
                                                        ).joinToString(" • "),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (sel) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // --- انتخاب ساعت ---
                        if (state.selectedDate != null) {
                            item { SectionHeader(Icons.Filled.AccessTime, "انتخاب ساعت") }
                            item {
                                val slotsErr = state.slotsError
                                when {
                                    state.slotsLoading -> InlineLoading("در حال دریافت زمان‌های آزاد…")
                                    slotsErr != null -> InlineError(
                                        message = "زمان‌های این تاریخ بارگذاری نشد.\n$slotsErr",
                                        onRetry = viewModel::retrySlots
                                    )
                                    state.slots.isEmpty() -> Text(
                                        "زمان آزادی در این تاریخ وجود ندارد.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    else -> TimeSlotGrid(
                                        slots = state.slots,
                                        selected = state.selectedSlot?.startTime,
                                        onSelect = { viewModel.selectSlot(it) }
                                    )
                                }
                            }
                        }

                        item { Spacer(Modifier.height(4.dp)) }
                        item {
                            val selDate = state.selectedDate
                            val selSlot = state.selectedSlot
                            val slotsErrCta = state.slotsError
                            val canContinue = selDate != null && selSlot != null &&
                                !state.slotsLoading && slotsErrCta == null
                            GradientButton(
                                enabled = canContinue,
                                onClick = viewModel::openForm,
                                modifier = Modifier.fillMaxWidth().height(54.dp)
                            ) {
                                Text(
                                    when {
                                        selDate == null && state.dates.isEmpty() -> "زمان رزرو موجود نیست"
                                        selDate == null -> "ابتدا تاریخ را انتخاب کنید"
                                        state.slotsLoading -> "در حال دریافت زمان‌ها…"
                                        slotsErrCta != null -> "بارگذاری زمان‌ها ناموفق بود"
                                        selSlot == null -> "ساعت را انتخاب کنید"
                                        else -> "ادامه رزرو — ساعت ${Jalali.persianDigits(selSlot.startTime)}"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }

    if (state.showForm) {
        BookingDialog(
            state = state,
            submitState = submitState,
            name = name,
            phone = phone,
            notes = notes,
            nameErr = nameErr,
            phoneErr = phoneErr,
            onNameChange = { name = it; nameErr = null },
            onPhoneChange = { phone = it.filter { c -> c.isDigit() }; phoneErr = null },
            onNotesChange = { notes = it },
            onNameErr = { nameErr = it },
            onPhoneErr = { phoneErr = it },
            onSubmit = { viewModel.submitBooking(name.trim(), phone.trim(), notes.trim()) },
            onDismiss = { if (submitState !is UiState.Loading) viewModel.closeForm() },
            onViewBookings = {
                viewModel.closeForm()
                navController.navigate(Routes.BOOKINGS)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeSlotGrid(slots: List<TimeSlot>, selected: String?, onSelect: (TimeSlot) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        slots.forEach { slot ->
            val sel = selected == slot.startTime
            GlassChip(selected = sel, onClick = { onSelect(slot) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        Jalali.persianDigits(slot.startTime),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                        color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    if (slot.endTime.isNotBlank() && slot.endTime != slot.startTime) {
                        Text(
                            "تا ${Jalali.persianDigits(slot.endTime)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (sel) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineLoading(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        CircularProgressIndicator(Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InlineError(message: String, onRetry: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    GlassCard(shape = RoundedCornerShape(18.dp)) {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = cs.error
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRetry) {
            Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("تلاش دوباره", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        IconTile(icon = icon, tileSize = 34.dp, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ServiceRow(service: Service, selected: Boolean, onSelect: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    GlassCard(
        onClick = onSelect,
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(
                icon = Icons.Filled.CheckCircle,
                tileSize = 40.dp,
                selected = selected,
                palette = if (selected) TilePalette.GREEN else TilePalette.MUTED,
                contentDescription = if (selected) "انتخاب شد" else "انتخاب"
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(service.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        Jalali.formatPrice(service.priceValue),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    service.durationMinutes?.let {
                        Text(
                            "⏱ ${Jalali.persianDigits(it)} دقیقه",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingDialog(
    state: BusinessDetailState,
    submitState: UiState<BookingCreatedResponse>?,
    name: String,
    phone: String,
    notes: String,
    nameErr: String?,
    phoneErr: String?,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onNameErr: (String?) -> Unit,
    onPhoneErr: (String?) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    onViewBookings: () -> Unit
) {
    val dark = isSystemInDarkTheme()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = if (dark) Color(0xFF1E2521) else Color(0xFFFBFDFC)
        ) {
            Column(
                Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("تکمیل رزرو", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                val d = state.selectedDate
                val s = state.selectedSlot
                if (d != null && s != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${Jalali.formatLong(d.date)} — ساعت ${Jalali.persianDigits(s.startTime)} تا ${Jalali.persianDigits(s.endTime)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                Spacer(Modifier.height(16.dp))

                when (submitState) {
                    is UiState.Success -> {
                        IconTile(Icons.Filled.CheckCircle, tileSize = 64.dp, contentDescription = null)
                        Spacer(Modifier.height(10.dp))
                        Text("رزرو شما ثبت شد!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        InfoRow("کد رزرو", submitState.data.confirmationCode ?: "—")
                        Spacer(Modifier.height(14.dp))
                        GradientButton(onClick = onViewBookings, modifier = Modifier.fillMaxWidth()) {
                            Text("مشاهده نوبت‌های من", fontWeight = FontWeight.Bold)
                        }
                    }
                    is UiState.Error -> {
                        Text(
                            submitState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("بستن") }
                    }
                    else -> {
                        OutlinedTextField(
                            value = name,
                            onValueChange = onNameChange,
                            label = { Text("نام و نام خانوادگی *") },
                            singleLine = true,
                            isError = nameErr != null,
                            supportingText = { nameErr?.let { Text(it) } },
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = phone,
                            onValueChange = onPhoneChange,
                            label = { Text("شماره موبایل *") },
                            singleLine = true,
                            isError = phoneErr != null,
                            supportingText = { phoneErr?.let { Text(it) } },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = onNotesChange,
                            label = { Text("توضیحات (اختیاری)") },
                            minLines = 2,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "با ثبت رزرو، پیام تأیید از طریق ربات تلگرام نیز برای شما ارسال می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(14.dp))
                        GradientButton(
                            onClick = {
                                val pErr = if (!Regex("^09\\d{9}$").matches(phone)) "شماره موبایل معتبر نیست" else null
                                val nErr = if (name.trim().length < 2) "نام را وارد کنید" else null
                                onPhoneErr(pErr)
                                onNameErr(nErr)
                                if (pErr == null && nErr == null) onSubmit()
                            },
                            enabled = submitState !is UiState.Loading,
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            if (submitState is UiState.Loading) {
                                CircularProgressIndicator(Modifier.size(20.dp))
                            } else {
                                Text("ثبت نهایی رزرو", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
