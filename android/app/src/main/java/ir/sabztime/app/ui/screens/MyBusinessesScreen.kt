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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import ir.sabztime.app.core.classify
import ir.sabztime.app.data.MyBusinessItem
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
import ir.sabztime.app.ui.components.businessTypeIcon
import ir.sabztime.app.util.Jalali
import ir.sabztime.app.util.TextUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyBusinessesViewModel(private val graph: AppGraph) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<MyBusinessItem>>>(UiState.Loading)
    val state: StateFlow<UiState<List<MyBusinessItem>>> = _state.asStateFlow()

    private val _deleteBusy = MutableStateFlow(false)
    val deleteBusy: StateFlow<Boolean> = _deleteBusy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _messageError = MutableStateFlow(false)
    val messageError: StateFlow<Boolean> = _messageError.asStateFlow()

    /** وضعیت فرم افزودن: تا موفقیت پنجره بسته نمی‌شود و خطای سرور داخل خود پنجره دیده می‌شود. */
    private val _addBusy = MutableStateFlow(false)
    val addBusy: StateFlow<Boolean> = _addBusy.asStateFlow()

    private val _addError = MutableStateFlow<String?>(null)
    val addError: StateFlow<String?> = _addError.asStateFlow()

    init {
        load()
    }

    /** ثبت کسب‌وکار جدید؛ در موفقیت به صفحه مدیریت همان کسب‌وکار می‌رود. */
    fun addBusiness(
        name: String,
        type: String,
        phone: String,
        province: String,
        city: String,
        address: String,
        onCreated: (Int) -> Unit
    ) {
        if (_addBusy.value) return
        viewModelScope.launch {
            _addBusy.value = true
            _addError.value = null
            try {
                val result = graph.repository.addBusiness(name, type, phone, province, city, address)
                _addBusy.value = false
                result.businessId?.let(onCreated) ?: load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // خطا در خود پنجره نمایش داده می‌شود؛ فرم پاک نمی‌شود
                val s = classify(e)
                _addError.value = s.message
                _addBusy.value = false
            }
        }
    }

    fun clearAddError() { _addError.value = null }

    /** حذف کامل یک کسب‌وکار (هم‌منطق ربات)؛ فهرست پس از موفقیت تازه می‌شود. */
    fun deleteBusiness(b: MyBusinessItem, onDone: () -> Unit) {
        _deleteBusy.value = true
        viewModelScope.launch {
            try {
                graph.repository.deleteBusiness(b.id)
                _message.value = "«${b.name}» حذف شد. ✅"
                _messageError.value = false
                onDone()
                load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val s = classify(e)
                _message.value = s.message
                _messageError.value = true
            } finally {
                _deleteBusy.value = false
            }
        }
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                val list = graph.repository.myBusinesses()
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

@Composable
fun MyBusinessesScreen(graph: AppGraph, navController: NavHostController) {
    val viewModel: MyBusinessesViewModel = viewModel { MyBusinessesViewModel(graph) }
    val state by viewModel.state.collectAsState()
    val deleteBusy by viewModel.deleteBusy.collectAsState()
    val message by viewModel.message.collectAsState()
    val messageError by viewModel.messageError.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<MyBusinessItem?>(null) }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                GlassTopBar(
                    title = "کسب‌وکارهای من",
                    onBack = { navController.popBackStack() }
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                message?.let { msg ->
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (messageError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
                if (state != UiState.Loading && state !is UiState.Error) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
                        GradientButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("افزودن کسب‌وکار", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                when (val s = state) {
                    is UiState.Loading -> {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    is UiState.Error -> ErrorState(s.message, s.kind, onRetry = viewModel::load)
                    UiState.Empty -> EmptyState(
                        icon = "🏪",
                        title = "هنوز کسب‌وکاری ثبت نکرده‌اید",
                        subtitle = "برای شروع، اطلاعات کسب‌وکار خود را در بالا وارد کنید."
                    )
                    is UiState.Success -> LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                    items(s.data, key = { it.id }) { b ->
                        GlassCard(onClick = { navController.navigate(Routes.owner(b.id)) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconTile(
                                    icon = businessTypeIcon(b.businessType),
                                    tileSize = 52.dp,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        b.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    StatusChip(
                                        if (b.isActive == 1) "فعال" else "غیرفعال",
                                        if (b.isActive == 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                        if (b.isActive == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                    )
                                }
                                IconButton(onClick = { deleteTarget = b }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "حذف کسب‌وکار",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                InfoRow("خدمات", Jalali.persianDigits(b.servicesCount))
                                InfoRow("کل رزروها", Jalali.persianDigits(b.bookingsCount))
                            }
                            InfoRow("اشتراک", b.subscriptionPlanName ?: "پایه (رایگان)")
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "مدیریت نوبت‌ها و صف",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.Filled.ChevronLeft,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
        if (showAddDialog) {
            val addBusy by viewModel.addBusy.collectAsState()
            val addError by viewModel.addError.collectAsState()
            AddBusinessDialog(
                busy = addBusy,
                serverError = addError,
                onDismiss = {
                    if (!addBusy) {
                        viewModel.clearAddError()
                        showAddDialog = false
                    }
                },
                onSubmit = { name, type, phone, province, city, address ->
                    viewModel.addBusiness(name, type, phone, province, city, address) { id ->
                        // پنجره فقط در موفقیت بسته می‌شود؛ در خطا مقادیر کاربر محفوظ می‌مانند
                        showAddDialog = false
                        navController.navigate(Routes.owner(id))
                    }
                }
            )
        }
        deleteTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { if (!deleteBusy) deleteTarget = null },
                title = { Text("حذف کسب‌وکار", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "با حذف «${target.name}» همهٔ رزروها و تنظیمات آن برای همیشه پاک می‌شود. این عمل قابل بازگشت نیست."
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.deleteBusiness(target) { deleteTarget = null }
                    }, enabled = !deleteBusy) {
                        Text(if (deleteBusy) "در حال حذف…" else "حذف کامل")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null }, enabled = !deleteBusy) { Text("انصراف") }
                }
            )
        }
        }
    }
}

@Composable
private fun AddBusinessDialog(
    busy: Boolean,
    serverError: String?,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    // «لمس‌شده» یعنی کاربر در این فیلد تایپ کرده؛ خطا فقط بعد از لمس نمایش داده می‌شود
    var nameTouched by remember { mutableStateOf(false) }
    var typeTouched by remember { mutableStateOf(false) }
    var phoneTouched by remember { mutableStateOf(false) }
    var provinceTouched by remember { mutableStateOf(false) }
    var cityTouched by remember { mutableStateOf(false) }

    // اعتبارسنجی زنده: با هر تایپ دوباره محاسبه می‌شود
    val nameError = if (nameTouched && name.trim().length < 2) "نام کسب‌وکار را کامل وارد کنید (حداقل ۲ حرف)." else null
    val typeError = if (typeTouched && type.isBlank()) "نوع کسب‌وکار را وارد کنید (مثلاً آرایشگاه)." else null
    val phoneNorm = TextUtil.normalizeDigits(TextUtil.cleanMobileInput(phone))
    val phoneError = if (phoneTouched && !TextUtil.isValidIranMobile(phoneNorm))
        "شماره موبایل معتبر وارد کنید؛ ۱۱ رقم و با ۰۹ شروع شود." else null
    val provinceError = if (provinceTouched && province.isBlank()) "استان را وارد کنید." else null
    val cityError = if (cityTouched && city.isBlank()) "شهر را وارد کنید." else null

    val formValid = name.trim().length >= 2 && type.isNotBlank() &&
        TextUtil.isValidIranMobile(phoneNorm) && province.isNotBlank() && city.isNotBlank()

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("ثبت کسب‌وکار جدید", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameTouched = true },
                    label = { Text("نام کسب‌وکار") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = { nameError?.let { Text(it) } },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it; typeTouched = true },
                    label = { Text("نوع کسب‌وکار (مثلاً آرایشگاه)") },
                    singleLine = true,
                    isError = typeError != null,
                    supportingText = { typeError?.let { Text(it) } },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; phoneTouched = true },
                    label = { Text("شماره تماس (۰۹…)") },
                    singleLine = true,
                    isError = phoneError != null,
                    supportingText = { phoneError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = province,
                    onValueChange = { province = it; provinceTouched = true },
                    label = { Text("استان") },
                    singleLine = true,
                    isError = provinceError != null,
                    supportingText = { provinceError?.let { Text(it) } },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it; cityTouched = true },
                    label = { Text("شهر") },
                    singleLine = true,
                    isError = cityError != null,
                    supportingText = { cityError?.let { Text(it) } },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("نشانی (اختیاری)") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                // خطای سرور (مثلاً قطع اینترنت) — داخل پنجره، بدون پاک شدن فرم
                serverError?.let { err ->
                    Text(
                        err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(name.trim(), type.trim(), phoneNorm, province.trim(), city.trim(), address.trim()) },
                enabled = formValid && !busy
            ) {
                if (busy) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("در حال ثبت…")
                } else {
                    Text("ثبت و مدیریت خدمات")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("انصراف") } }
    )
}
