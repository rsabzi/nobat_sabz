package ir.sabztime.app.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import ir.sabztime.app.data.BookingCreatedResponse
import ir.sabztime.app.data.BusinessDetails
import ir.sabztime.app.data.DateSlot
import ir.sabztime.app.data.Service
import ir.sabztime.app.data.TimeSlot
import ir.sabztime.app.ui.Routes
import ir.sabztime.app.ui.components.EmptyState
import ir.sabztime.app.ui.components.ErrorState
import ir.sabztime.app.ui.components.InfoRow
import ir.sabztime.app.ui.components.SectionCard
import ir.sabztime.app.ui.components.StatusChip
import ir.sabztime.app.util.Jalali
import kotlinx.coroutines.launch

data class BusinessDetailState(
    val business: UiState<BusinessDetails> = UiState.Loading,
    val dates: List<DateSlot> = emptyList(),
    val datesLoading: Boolean = false,
    val selectedDate: DateSlot? = null,
    val slots: List<TimeSlot> = emptyList(),
    val slotsLoading: Boolean = false,
    val selectedSlot: TimeSlot? = null,
    val selectedServiceId: Int? = null,
    val submit: UiState<BookingCreatedResponse>? = null,
    val showForm: Boolean = false
)

class BusinessDetailViewModel(private val graph: AppGraph, private val businessId: Int) : ViewModel() {
    var state by mutableStateOf(BusinessDetailState())
        private set

    var userId: Long = 0

    init {
        load()
        viewModelScope.launch {
            try {
                val me = graph.repository.me()
                userId = me.user.telegramId
            } catch (_: Exception) { /* Guest booking fallback */ }
        }
    }

    fun load() {
        state = state.copy(business = UiState.Loading)
        viewModelScope.launch {
            try {
                val b = graph.repository.businessDetails(businessId)
                    ?: throw SabzException.Server("کسب‌وکار پیدا نشد.", "NOT_FOUND", 404)
                state = state.copy(business = UiState.Success(b))
                loadDates()
            } catch (e: Exception) {
                val sabz = e as? SabzException ?: SabzException.Network(e)
                state = state.copy(business = UiState.Error(sabz.toFriendlyMessage(), sabz.kind))
            }
        }
    }

    fun loadDates() {
        state = state.copy(datesLoading = true)
        viewModelScope.launch {
            try {
                val d = graph.repository.availableDates(businessId)
                state = state.copy(dates = d, datesLoading = false, selectedDate = null)
            } catch (_: Exception) {
                state = state.copy(datesLoading = false)
            }
        }
    }

    fun selectDate(date: DateSlot) {
        state = state.copy(selectedDate = date, selectedSlot = null, slotsLoading = true)
        viewModelScope.launch {
            try {
                val s = graph.repository.availableSlots(businessId, date.date)
                state = state.copy(slots = s, slotsLoading = false)
            } catch (_: Exception) {
                state = state.copy(slots = emptyList(), slotsLoading = false)
            }
        }
    }

    fun selectSlot(slot: TimeSlot) {
        state = state.copy(selectedSlot = slot)
    }

    fun selectService(id: Int) {
        state = state.copy(selectedServiceId = id)
    }

    fun openForm() {
        state = state.copy(showForm = true, submit = null)
    }

    fun closeForm() {
        state = state.copy(showForm = false, submit = null)
    }

    fun submitBooking(name: String, phone: String, notes: String) {
        val d = state
        val date = d.selectedDate ?: return
        val slot = d.selectedSlot ?: return
        state = d.copy(submit = UiState.Loading)
        viewModelScope.launch {
            try {
                val resp = graph.repository.createBooking(
                    businessId = businessId,
                    date = date.date,
                    startTime = slot.startTime,
                    endTime = slot.endTime,
                    customerName = name,
                    customerPhone = phone,
                    customerNotes = notes,
                    userId = userId
                )
                state = state.copy(submit = UiState.Success(resp))
            } catch (e: Exception) {
                val sabz = e as? SabzException ?: SabzException.Network(e)
                state = state.copy(submit = UiState.Error(sabz.toFriendlyMessage(), sabz.kind))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessDetailScreen(graph: AppGraph, businessId: Int, navController: NavHostController) {
    val viewModel: BusinessDetailViewModel = viewModel(key = "biz-$businessId") { BusinessDetailViewModel(graph, businessId) }
    val state = viewModel.state
    val submitState = state.submit

    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var nameErr by remember { mutableStateOf<String?>(null) }
    var phoneErr by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val me = graph.repository.me()
            if (name.isBlank()) name = me.user.name
            if (phone.isBlank()) phone = me.user.phone
        } catch (_: Exception) {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text((state.business as? UiState.Success)?.data?.name ?: "کسب‌وکار", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        when (val bState = state.business) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> ErrorState(bState.message, bState.kind, onRetry = viewModel::load)
            UiState.Empty -> EmptyState(
                icon = "🏪",
                title = "کسب‌وکار پیدا نشد",
                subtitle = "ممکن است این کسب‌وکار حذف یا غیرفعال شده باشد.",
                modifier = Modifier.padding(padding)
            )
            is UiState.Success -> {
                val b = bState.data
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SectionCard {
                            Text(b.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            b.businessType?.let {
                                Spacer(Modifier.height(6.dp))
                                StatusChip(it, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                            }
                            if (!b.description.isNullOrBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Text(b.description, style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(Modifier.height(10.dp))
                            if (!b.address.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Place, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text(b.address, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (!b.phone.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Phone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text(Jalali.persianDigits(b.phone), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    if (b.services.isNotEmpty()) {
                        item {
                            Text("خدمات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        items(b.services, key = { "s${it.id}" }) { service ->
                            ServiceRow(
                                service,
                                selected = state.selectedServiceId == service.id,
                                onSelect = { viewModel.selectService(service.id) }
                            )
                        }
                    }

                    item {
                        Text("انتخاب تاریخ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    item {
                        when {
                            state.datesLoading -> CircularProgressIndicator(Modifier.size(22.dp))
                            state.dates.isEmpty() -> Text(
                                "برای این کسب‌وکار هنوز زمان رزرو ثبت نشده است.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(state.dates, key = { it.date }) { d ->
                                    FilterChip(
                                        selected = state.selectedDate?.date == d.date,
                                        onClick = { viewModel.selectDate(d) },
                                        label = {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(Jalali.format(d.date), fontWeight = FontWeight.Bold)
                                                Text(d.dayName ?: "", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (state.selectedDate != null) {
                        item {
                            Text("انتخاب ساعت", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        item {
                            when {
                                state.slotsLoading -> CircularProgressIndicator(Modifier.size(22.dp))
                                state.slots.isEmpty() -> Text(
                                    "زمان آزادی در این تاریخ وجود ندارد.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(state.slots, key = { it.startTime }) { slot ->
                                        FilterChip(
                                            selected = state.selectedSlot?.startTime == slot.startTime,
                                            onClick = { viewModel.selectSlot(slot) },
                                            label = { Text(Jalali.persianDigits(slot.startTime)) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(4.dp))
                        Button(
                            enabled = state.selectedDate != null && state.selectedSlot != null,
                            onClick = viewModel::openForm,
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text(
                                when {
                                    state.selectedSlot == null -> "زمان را انتخاب کنید"
                                    else -> "ادامه رزرو — ${Jalali.persianDigits(state.selectedSlot.startTime)}"
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

    if (state.showForm) {
        Dialog(onDismissRequest = { if (submitState !is UiState.Loading) viewModel.closeForm() }) {
            Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 4.dp) {
                val d = state.selectedDate
                val s = state.selectedSlot
                Column(
                    Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("تکمیل رزرو", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (d != null && s != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${Jalali.formatLong(d.date)} — ساعت ${Jalali.persianDigits(s.startTime)} تا ${Jalali.persianDigits(s.endTime)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    when (submitState) {
                        is UiState.Success -> {
                            Text("✅", style = MaterialTheme.typography.displayMedium)
                            Text("رزرو شما ثبت شد!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            InfoRow("کد رزرو", submitState.data.confirmationCode ?: "—")
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = {
                                viewModel.closeForm()
                                navController.navigate(Routes.BOOKINGS)
                            }, modifier = Modifier.fillMaxWidth()) { Text("مشاهده نوبت‌های من") }
                        }
                        is UiState.Error -> {
                            Text(submitState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = viewModel::closeForm) { Text("بستن") }
                        }
                        else -> {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it; nameErr = null },
                                label = { Text("نام و نام خانوادگی *") },
                                singleLine = true,
                                isError = nameErr != null,
                                supportingText = { nameErr?.let { Text(it) } },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it.filter { c -> c.isDigit() }; phoneErr = null },
                                label = { Text("شماره موبایل *") },
                                singleLine = true,
                                isError = phoneErr != null,
                                supportingText = { phoneErr?.let { Text(it) } },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("توضیحات (اختیاری)") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "با ثبت رزرو، پیام تأیید از طریق ربات تلگرام نیز برای شما ارسال می‌شود.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    phoneErr = if (!Regex("^09\\d{9}$").matches(phone)) "شماره موبایل معتبر نیست" else null
                                    nameErr = if (name.trim().length < 2) "نام را وارد کنید" else null
                                    if (nameErr == null && phoneErr == null) {
                                        viewModel.submitBooking(name.trim(), phone.trim(), notes.trim())
                                    }
                                },
                                enabled = submitState !is UiState.Loading,
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                if (submitState is UiState.Loading) {
                                    CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
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
}

@Composable
private fun ServiceRow(service: Service, selected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(service.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(
                    "مناسب برای انتخاب",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(Jalali.formatPrice(service.priceValue), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                service.durationMinutes?.let {
                    Text("⏱ ${Jalali.persianDigits(it)} دقیقه", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = onSelect, modifier = Modifier.fillMaxWidth()) {
                Text(if (selected) "✓ انتخاب شد" else "انتخاب این خدمت")
            }
        }
    }
}
