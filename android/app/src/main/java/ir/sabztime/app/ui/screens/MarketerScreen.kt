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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.core.UiState
import ir.sabztime.app.core.classify
import ir.sabztime.app.data.MarketerDashboardResponse
import ir.sabztime.app.data.MarketerRegistrationResponse
import ir.sabztime.app.ui.components.EmptyState
import ir.sabztime.app.ui.components.ErrorState
import ir.sabztime.app.ui.theme.GlassBackground
import ir.sabztime.app.ui.components.GlassCard
import ir.sabztime.app.ui.components.GlassTopBar
import ir.sabztime.app.ui.components.GradientButton
import ir.sabztime.app.ui.components.IconTile
import ir.sabztime.app.ui.components.InfoRow
import ir.sabztime.app.ui.components.TilePalette
import ir.sabztime.app.util.Jalali
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MarketerViewModel(private val graph: AppGraph) : ViewModel() {
    private val _state = MutableStateFlow<UiState<MarketerDashboardResponse>>(UiState.Loading)
    val state: StateFlow<UiState<MarketerDashboardResponse>> = _state.asStateFlow()

    private val _registration = MutableStateFlow<MarketerRegistrationResponse?>(null)
    val registration: StateFlow<MarketerRegistrationResponse?> = _registration.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                val d = graph.repository.marketerDashboard()
                _state.value = UiState.Success(d)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val s = classify(e)
                _state.value = UiState.Error(s.message, s.kind)
            }
        }
    }

    /** فعال‌سازی حساب بازاریابی داخل اپ — هم‌منطق ربات (user_type_marketer). */
    fun activate() {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                val r = graph.repository.registerMarketer()
                _registration.value = r
                load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val s = classify(e)
                _state.value = UiState.Error(s.message, s.kind)
            } finally {
                _busy.value = false
            }
        }
    }
}

@Composable
fun MarketerScreen(graph: AppGraph, navController: NavHostController) {
    val viewModel: MarketerViewModel = viewModel { MarketerViewModel(graph) }
    val state by viewModel.state.collectAsState()
    val registration by viewModel.registration.collectAsState()
    val busy by viewModel.busy.collectAsState()

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                GlassTopBar(
                    title = "داشبورد بازاریاب",
                    onBack = { navController.popBackStack() },
                    actions = {
                        IconButton(onClick = viewModel::load) {
                            Icon(Icons.Filled.Refresh, contentDescription = "تازه‌سازی")
                        }
                    }
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
                is UiState.Error -> Column(
                    Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ErrorState(s.message, s.kind, onRetry = viewModel::load)
                    ActivationCard(registration, busy, onActivate = viewModel::activate)
                }
                is UiState.Success -> {
                    val d = s.data
                    if (d.totalBusinesses == 0 && d.totalBookings == 0 && registration == null) {
                        Column(
                            Modifier.fillMaxSize().padding(padding),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            EmptyState(
                                icon = "📣",
                                title = "هنوز بازاریاب نیستید",
                                subtitle = "با یک کلیک حساب بازاریابی خود را همین‌جا فعال کنید — کد اختصاصی و لینک معرفی برایتان ساخته می‌شود."
                            )
                            ActivationCard(registration, busy, onActivate = viewModel::activate)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(padding),
                            contentPadding = PaddingValues(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                ActivationCard(registration, busy, onActivate = viewModel::activate)
                            }
                            item {
                                GlassCard {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconTile(
                                            icon = Icons.Filled.Campaign,
                                            tileSize = 44.dp,
                                            palette = TilePalette.AMBER,
                                            contentDescription = null
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            "آمار کلی",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    InfoRow("کسب‌وکارهای معرفی‌شده", Jalali.persianDigits(d.totalBusinesses))
                                    InfoRow("کل رزروها", Jalali.persianDigits(d.totalBookings))
                                    InfoRow("کمیسیون دریافتی", Jalali.formatPrice(d.totalCommission.toLong()))
                                    InfoRow("نرخ کمیسیون", Jalali.persianDigits(d.commissionRate.toInt()) + "٪")
                                }
                            }
                            item {
                                GlassCard {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconTile(
                                            icon = Icons.Filled.TrendingUp,
                                            contentDescription = null
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            "این ماه",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    InfoRow("کسب‌وکار جدید", Jalali.persianDigits(d.monthlyBusinesses))
                                    InfoRow("رزروهای جدید", Jalali.persianDigits(d.monthlyBookings))
                                    InfoRow("کمیسیون ماه", Jalali.formatPrice(d.monthlyCommission.toLong()))
                                }
                            }
                            if (d.businesses.isNotEmpty()) {
                                item {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconTile(
                                            icon = Icons.Filled.WorkHistory,
                                            tileSize = 34.dp,
                                            contentDescription = null
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            "کسب‌وکارهای معرفی‌شده",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                items(d.businesses, key = { it.id }) { b ->
                                    GlassCard(shape = RoundedCornerShape(24.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconTile(
                                                icon = Icons.Filled.Storefront,
                                                tileSize = 40.dp,
                                                contentDescription = null
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Text(
                                                b.name,
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        InfoRow("رزروها", Jalali.persianDigits(b.totalBookings))
                                        InfoRow("کمیسیون", Jalali.formatPrice(b.commissionEarned.toLong()))
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(8.dp)) }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

/** کارت فعال‌سازی/کد بازاریابی: قبل از فعال‌سازی دکمه، بعدش کد + لینک اختصاصی + کپی. */
@Composable
private fun ActivationCard(
    registration: MarketerRegistrationResponse?,
    busy: Boolean,
    onActivate: () -> Unit
) {
    val code = registration?.marketer?.marketerCode
    val link = registration?.referralLink
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(copied) {
        if (copied) {
            delay(2500)
            copied = false
        }
    }

    GlassCard {
        Text(
            "پنل بازاریابی شما",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        if (code != null) {
            InfoRow("کد بازاریابی", code)
            Spacer(Modifier.height(6.dp))
            Text(
                "این لینک را برای صاحبان کسب‌وکار بفرستید؛ معرفی‌ها با کد شما ثبت می‌شوند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    link ?: "https://t.me/sabztime_bot?start=marketer_$code",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                IconButton(
                    onClick = {
                        val final = link ?: "https://t.me/sabztime_bot?start=marketer_$code"
                        clipboard.setText(AnnotatedString(final))
                        copied = true
                    }
                ) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "کپی لینک",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (copied) {
                Text(
                    "لینک کپی شد ✅",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Text(
                "با فعال‌سازی، کد اختصاصی بازاریابی و لینک معرفی برایتان ساخته می‌شود.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            GradientButton(
                onClick = onActivate,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(18.dp).height(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (busy) "در حال فعال‌سازی…" else "فعال‌سازی بازاریابی", fontWeight = FontWeight.Bold)
            }
        }
    }
}
