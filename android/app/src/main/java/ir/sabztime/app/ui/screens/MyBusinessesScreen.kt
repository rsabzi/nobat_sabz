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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import ir.sabztime.app.ui.components.LoadingState
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

    Scaffold(
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
            is UiState.Loading -> LoadingState(Modifier.padding(padding))
            is UiState.Error -> ErrorState(s.message, s.kind, onRetry = viewModel::load)
            UiState.Empty -> EmptyState(
                icon = "🏪",
                title = "هنوز کسب‌وکاری ثبت نکرده‌اید",
                subtitle = "از ربات تلگرام نوبت‌سبز (/start) می‌توانید کسب‌وکار خود را ثبت کنید.",
                action = { Button(onClick = { navController.popBackStack() }) { Text("بازگشت") } }
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
}

@Composable
fun SubscriptionSummaryCard(item: MyBusinessItem) {
    SectionCard {
        InfoRow("تعداد نوبت‌های رایگان", Jalali.persianDigits(item.freeReservationTokens ?: 0))
        InfoRow("پلن فعال", item.subscriptionPlanName ?: "—")
        InfoRow("انقضای پلن", Jalali.formatLong(item.subscriptionExpiryDate))
    }
}
