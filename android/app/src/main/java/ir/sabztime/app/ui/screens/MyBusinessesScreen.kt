package ir.sabztime.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import ir.sabztime.app.data.MyBusinessItem
import ir.sabztime.app.ui.Routes
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

class MyBusinessesViewModel(private val graph: AppGraph) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<MyBusinessItem>>>(UiState.Loading)
    val state: StateFlow<UiState<List<MyBusinessItem>>> = _state.asStateFlow()

    init { load() }

    fun addBusiness(name: String, type: String, phone: String, province: String, city: String, address: String, onCreated: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val result = graph.repository.addBusiness(name, type, phone, province, city, address)
                result.businessId?.let(onCreated) ?: load()
            } catch (e: Exception) {
                val sabz = e as? SabzException ?: SabzException.Network(e)
                _state.value = UiState.Error(sabz.toFriendlyMessage(), sabz.kind)
            }
        }
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                val list = graph.repository.myBusinesses()
                _state.value = if (list.isEmpty()) UiState.Empty else UiState.Success(list)
            } catch (e: Exception) {
                val sabz = e as? SabzException ?: SabzException.Network(e)
                _state.value = UiState.Error(sabz.toFriendlyMessage(), sabz.kind)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBusinessesScreen(graph: AppGraph, navController: NavHostController) {
    val viewModel: MyBusinessesViewModel = viewModel { MyBusinessesViewModel(graph) }
    val state by viewModel.state.collectAsState()
    var showAddDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Text("+") },
                text = { Text("افزودن کسب‌وکار") }
            )
        },
        topBar = {
            TopAppBar(
                title = { Text("کسب‌وکارهای من", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            is UiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            is UiState.Error -> ErrorState(s.message, s.kind, onRetry = viewModel::load)
            UiState.Empty -> EmptyState(
                icon = "🏪",
                title = "هنوز کسب‌وکاری ثبت نکرده‌اید",
                subtitle = "برای شروع، اطلاعات کسب‌وکار خود را وارد کنید.",
                action = { Button(onClick = { showAddDialog = true }) { Text("ثبت کسب‌وکار") } }
            )
            is UiState.Success -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(s.data, key = { it.id }) { b ->
                    Card(
                        onClick = { navController.navigate(Routes.owner(b.id)) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(b.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                StatusChip(
                                    if (b.isActive == 1) "فعال" else "غیرفعال",
                                    if (b.isActive == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    if (b.isActive == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            InfoRow("خدمات", Jalali.persianDigits(b.servicesCount))
                            InfoRow("کل رزروها", Jalali.persianDigits(b.bookingsCount))
                            InfoRow(
                                "اشتراک",
                                b.subscriptionPlanName ?: "پایه (رایگان)"
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("مدیریت نوبت‌ها و صف ←", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.End))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddBusinessDialog(
            onDismiss = { showAddDialog = false },
            onSubmit = { name, type, phone, province, city, address ->
                viewModel.addBusiness(name, type, phone, province, city, address) { id ->
                    showAddDialog = false
                    navController.navigate(Routes.owner(id))
                }
            }
        )
    }
}

@Composable
private fun AddBusinessDialog(onDismiss: () -> Unit, onSubmit: (String, String, String, String, String, String) -> Unit) {
    var name by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var type by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var phone by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var province by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var city by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var address by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت کسب‌وکار جدید") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("نام کسب‌وکار") }, singleLine = true)
                OutlinedTextField(type, { type = it }, label = { Text("نوع کسب‌وکار") }, singleLine = true)
                OutlinedTextField(phone, { phone = it }, label = { Text("شماره تماس (۰۹…) ") }, singleLine = true)
                OutlinedTextField(province, { province = it }, label = { Text("استان") }, singleLine = true)
                OutlinedTextField(city, { city = it }, label = { Text("شهر") }, singleLine = true)
                OutlinedTextField(address, { address = it }, label = { Text("نشانی (اختیاری)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(name, type, phone, province, city, address) }, enabled = listOf(name, type, phone, province, city).all { it.isNotBlank() }) { Text("ثبت و مدیریت خدمات") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
fun SubscriptionSummaryCard(item: MyBusinessItem) {
    SectionCard {
        InfoRow("تعداد نوبت‌های رایگان", Jalali.persianDigits(item.freeReservationTokens ?: 0))
        InfoRow("پلن فعال", item.subscriptionPlanName ?: "—")
        InfoRow("انقضای پلن", Jalali.formatLong(item.subscriptionExpiryDate))
    }
}
