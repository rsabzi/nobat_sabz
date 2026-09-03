package ir.sabztime.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.core.UiState
import ir.sabztime.app.core.classify
import ir.sabztime.app.data.BusinessSummary
import ir.sabztime.app.data.FiltersData
import ir.sabztime.app.ui.Routes
import ir.sabztime.app.ui.components.BottomBarSpacer
import ir.sabztime.app.ui.components.EmptyState
import ir.sabztime.app.ui.components.ErrorState
import ir.sabztime.app.ui.components.FilterRow
import ir.sabztime.app.ui.components.GhostIconButton
import ir.sabztime.app.ui.components.GoldDivider
import ir.sabztime.app.ui.components.LuxSearchBar
import ir.sabztime.app.ui.components.SectionTitle
import ir.sabztime.app.ui.components.VipBusinessCard
import ir.sabztime.app.ui.components.businessTypeIcon
import ir.sabztime.app.ui.theme.Cream
import ir.sabztime.app.ui.theme.Gold
import ir.sabztime.app.ui.theme.GoldDim
import ir.sabztime.app.ui.theme.GoldLight
import ir.sabztime.app.ui.theme.LuxCardShape
import ir.sabztime.app.ui.theme.MidnightSoft
import ir.sabztime.app.ui.theme.NavyCardDeep
import ir.sabztime.app.ui.theme.PureWhite
import ir.sabztime.app.ui.theme.TextMuted
import ir.sabztime.app.util.Jalali
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val query: String = "",
    val selectedCategory: String = "",
    val selectedProvince: String = "",
    val results: UiState<List<BusinessSummary>> = UiState.Loading,
    val filters: FiltersData = FiltersData(),
    val filtersLoaded: Boolean = false,
    /** نام کاربر برای خوش‌آمدگویی سرصفحه */
    val userName: String = ""
)

class HomeViewModel(private val graph: AppGraph) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch { loadFilters() }
        viewModelScope.launch { doSearch() }
        viewModelScope.launch { loadUser() }
    }

    /** جستجو با مهلت (debounce)؛ درخواست قبلی لغو می‌شود تا پاسخ‌ها با هم رقابت نکنند */
    fun updateQuery(q: String) {
        _state.value = _state.value.copy(query = q)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            doSearch()
        }
    }

    fun selectCategory(c: String) {
        _state.value = _state.value.copy(selectedCategory = if (c == _state.value.selectedCategory) "" else c)
        restartSearch()
    }

    fun selectProvince(p: String) {
        _state.value = _state.value.copy(selectedProvince = if (p == _state.value.selectedProvince) "" else p)
        restartSearch()
    }

    fun clearFilters() {
        _state.value = _state.value.copy(selectedCategory = "", selectedProvince = "")
        restartSearch()
    }

    fun refresh() {
        viewModelScope.launch {
            if (!_state.value.filtersLoaded) loadFilters()
            if (_state.value.userName.isBlank()) loadUser()
            doSearch()
        }
    }

    private fun restartSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch { doSearch() }
    }

    private suspend fun loadUser() {
        try {
            val me = graph.repository.me()
            _state.value = _state.value.copy(userName = me.user.name.trim())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // نام کاربر اختیاری است؛ سرصفحه بدون نام هم درست کار می‌کند
        }
    }

    private suspend fun loadFilters() {
        try {
            val f = graph.repository.filters()
            _state.value = _state.value.copy(filters = f, filtersLoaded = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // فیلترها اختیاری هستند
        }
    }

    private suspend fun doSearch() {
        val cur = _state.value
        _state.value = cur.copy(results = UiState.Loading)
        try {
            val result = graph.repository.search(
                query = cur.query.trim(),
                category = cur.selectedCategory,
                province = cur.selectedProvince
            )
            val list = result.data
            _state.value = _state.value.copy(
                results = if (list.isEmpty()) UiState.Empty else UiState.Success(list)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val s = classify(e)
            _state.value = _state.value.copy(results = UiState.Error(s.message, s.kind))
        }
    }
}

/**
 * صفحه‌ی خانه — سرصفحه‌ی خوش‌آمدگویی، سرچ‌بار لوکس، دو دکمه‌ی فیلتر
 * (استان در راست، دسته‌بندی در چپ) و فهرست کارت‌های کسب‌وکار.
 */
@Composable
fun HomeScreen(graph: AppGraph, navController: NavHostController) {
    val viewModel: HomeViewModel = viewModel { HomeViewModel(graph) }
    val state by viewModel.state.collectAsState()

    var categoryMenu by remember { mutableStateOf(false) }
    var provinceMenu by remember { mutableStateOf(false) }

    val today = LocalDate.now().toString()
    val dateLine = "${Jalali.weekdayName(today)}، ${Jalali.formatLong(today)}"
    val name = state.userName.ifBlank { "مهمان" }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(padding)
        ) {
            // ── سرصفحه: درود، {نام} 👋 + تاریخ جلالی ──
            ir.sabztime.app.ui.components.WelcomeHeader(
                name = name,
                dateLine = dateLine,
                actions = {
                    GhostIconButton(
                        icon = Icons.Filled.Notifications,
                        contentDescription = "اعلان‌ها",
                        onClick = { navController.navigate(Routes.NOTIFICATIONS) },
                        badge = true
                    )
                    Spacer(Modifier.width(8.dp))
                    GhostIconButton(
                        icon = Icons.Filled.Refresh,
                        contentDescription = "تازه‌سازی",
                        onClick = viewModel::refresh
                    )
                }
            )

            Spacer(Modifier.height(6.dp))

            // ── سرچ‌بار ──
            LuxSearchBar(
                value = state.query,
                onValueChange = viewModel::updateQuery,
                modifier = Modifier.padding(horizontal = 22.dp)
            )

            Spacer(Modifier.height(12.dp))

            // ── دکمه‌های فیلتر: راست = استان، چپ = دسته‌بندی ──
            FilterRow(
                province = state.selectedProvince,
                category = state.selectedCategory,
                onProvince = { provinceMenu = true },
                onCategory = { categoryMenu = true },
                modifier = Modifier.padding(horizontal = 22.dp)
            )

            if (state.selectedCategory.isNotBlank() || state.selectedProvince.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        "حذف فیلترها",
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldDim,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = viewModel::clearFilters)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(Modifier.padding(horizontal = 22.dp)) {
                SectionTitle(
                    text = "کسب‌وکارهای منتخب",
                    trailing = {
                        val count = (state.results as? UiState.Success)?.data?.size
                        if (count != null) {
                            Text(
                                "${Jalali.persianDigits(count)} مورد",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── نتایج ──
            when (val r = state.results) {
                is UiState.Loading -> Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GoldDim, strokeWidth = 2.5.dp)
                }
                is UiState.Error -> ErrorState(r.message, r.kind, onRetry = viewModel::refresh)
                UiState.Empty -> EmptyState(
                    icon = "🔍",
                    title = "کسب‌وکاری پیدا نشد",
                    subtitle = "عبارت دیگری را امتحان کنید یا فیلترها را حذف کنید."
                )
                is UiState.Success -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(r.data) { b ->
                        VipBusinessCard(
                            title = b.name,
                            location = listOfNotNull(b.city, b.province).joinToString("، ")
                                .ifBlank { b.address.orEmpty() },
                            services = b.services.take(3).joinToString("، "),
                            icon = businessTypeIcon(b.businessType),
                            badge = if (b.services.size >= 3) "VIP" else null,
                            onClick = { navController.navigate(Routes.business(b.id)) }
                        )
                    }
                    item { BottomBarSpacer() }
                }
            }
        }
    }

    if (categoryMenu) {
        SelectionDialog(
            title = "دسته‌بندی",
            options = state.filters.categories.map { it.businessType to it.count },
            onDismiss = { categoryMenu = false },
            onSelect = { viewModel.selectCategory(it); categoryMenu = false }
        )
    }
    if (provinceMenu) {
        SelectionDialog(
            title = "استان",
            options = state.filters.provinces.map { it.province to it.count },
            onDismiss = { provinceMenu = false },
            onSelect = { viewModel.selectProvince(it); provinceMenu = false }
        )
    }
}

/** دیالوگ انتخاب لوکس (استان / دسته‌بندی) */
@Composable
private fun SelectionDialog(
    title: String,
    options: List<Pair<String, Int>>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = LuxCardShape,
            color = Color.Transparent
        ) {
            Column(
                Modifier
                    .clip(LuxCardShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFF7FAFD), Color(0xFFE4EEF9))
                        )
                    )
                    .border(ir.sabztime.app.ui.theme.goldGradientBorder(), LuxCardShape)
                    .fillMaxWidth()
            ) {
                Text(
                    title,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ir.sabztime.app.ui.theme.Cocoa
                )
                GoldDivider()
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(options) { (label, count) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(label) }
                                .padding(horizontal = 22.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(7.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.verticalGradient(
                                            ir.sabztime.app.ui.theme.MetalGoldGradient
                                        )
                                    )
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ir.sabztime.app.ui.theme.Cocoa,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${Jalali.persianDigits(count)} کسب‌وکار",
                                style = MaterialTheme.typography.labelSmall,
                                color = ir.sabztime.app.ui.theme.CocoaSoft
                            )
                        }
                    }
                    item { Spacer(Modifier.height(10.dp)) }
                }
            }
        }
    }
}
