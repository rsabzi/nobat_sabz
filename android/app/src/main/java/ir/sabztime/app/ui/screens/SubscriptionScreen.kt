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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.core.SabzException
import ir.sabztime.app.core.UiState
import ir.sabztime.app.core.classify
import ir.sabztime.app.data.Plan
import ir.sabztime.app.ui.components.EmptyState
import ir.sabztime.app.ui.components.ErrorState
import ir.sabztime.app.ui.theme.GlassBackground
import ir.sabztime.app.ui.components.GlassCard
import ir.sabztime.app.ui.components.GlassTopBar
import ir.sabztime.app.ui.components.IconTile
import ir.sabztime.app.ui.components.TilePalette
import ir.sabztime.app.util.Jalali
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SubscriptionViewModel(private val graph: AppGraph) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Plan>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Plan>>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                val plans = graph.repository.subscriptionPlans()
                _state.value = if (plans.isEmpty()) UiState.Empty else UiState.Success(plans)
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
fun SubscriptionScreen(graph: AppGraph, businessId: Int, navController: NavHostController) {
    val viewModel: SubscriptionViewModel = viewModel(key = "plans-$businessId") { SubscriptionViewModel(graph) }
    val state by viewModel.state.collectAsState()

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                GlassTopBar(
                    title = "پلن‌های اشتراک",
                    onBack = { navController.popBackStack() }
                )
            }
        ) { padding ->
            when (val s = state) {
                is UiState.Loading -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                is UiState.Error -> ErrorState(s.message, s.kind, onRetry = viewModel::load)
                UiState.Empty -> EmptyState(icon = "💳", title = "پلنی تعریف نشده")
                is UiState.Success -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        GlassCard {
                            Text(
                                "برای خرید و تمدید اشتراک، از ربات تلگرام نوبت‌سبز (منوی «مدیریت اشتراک») یا وب‌اپ پرداخت اقدام کنید. این صفحه فقط اطلاعات پلن‌ها را نمایش می‌دهد.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(s.data, key = { it.id }) { plan ->
                        GlassCard(shape = RoundedCornerShape(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconTile(
                                    icon = androidx.compose.material.icons.Icons.Filled.Star,
                                    tileSize = 44.dp,
                                    palette = TilePalette.AMBER,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    plan.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    Jalali.formatPrice(plan.priceValue),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (plan.reservationLimit >= 0) {
                                    Text(
                                        "حداکثر ${Jalali.persianDigits(plan.reservationLimit)} رزرو در ماه",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    Text(
                                        "رزرو نامحدود",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "اعتبار ${Jalali.persianDigits(plan.durationDays)} روزه",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}
