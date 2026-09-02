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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import ir.sabztime.app.ui.components.IconTile
import ir.sabztime.app.ui.components.InfoRow
import ir.sabztime.app.ui.components.StatusChip
import ir.sabztime.app.ui.components.businessTypeIcon
import ir.sabztime.app.util.Jalali
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyBusinessesViewModel(private val graph: AppGraph) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<MyBusinessItem>>>(UiState.Loading)
    val state: StateFlow<UiState<List<MyBusinessItem>>> = _state.asStateFlow()

    init {
        load()
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
            when (val s = state) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Error -> ErrorState(s.message, s.kind, onRetry = viewModel::load)
                UiState.Empty -> EmptyState(
                    icon = "🏪",
                    title = "هنوز کسب‌وکاری ثبت نکرده‌اید",
                    subtitle = "از ربات تلگرام نوبت‌سبز (دستور /start) می‌توانید کسب‌وکار خود را ثبت کنید."
                )
                is UiState.Success -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
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
    }
}
