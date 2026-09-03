package ir.sabztime.app.ui.screens

import androidx.compose.foundation.clickable
import ir.sabztime.app.ui.theme.LocalSabzDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import ir.sabztime.app.ui.components.EmptyState
import ir.sabztime.app.ui.components.ErrorState
import ir.sabztime.app.ui.theme.GlassBackground
import ir.sabztime.app.ui.components.GlassCard
import ir.sabztime.app.ui.components.GlassChip
import ir.sabztime.app.ui.components.IconTile
import ir.sabztime.app.ui.components.NotificationIconButton
import ir.sabztime.app.ui.components.businessTypeIcon
import ir.sabztime.app.util.Jalali
import java.time.LocalDate
import java.time.LocalTime
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
    val filtersLoaded: Boolean = false
)

class HomeViewModel(private val graph: AppGraph) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch { loadFilters() }
        viewModelScope.launch { doSearch() }
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
            doSearch()
        }
    }

    private fun restartSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch { doSearch() }
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
            _state.value = _state.value.copy(
                results = UiState.Error(s.message, s.kind)
            )
        }
    }
}

@Composable
fun HomeScreen(graph: AppGraph, navController: NavHostController) {
    val viewModel: HomeViewModel = viewModel { HomeViewModel(graph) }
    val state by viewModel.state.collectAsState()
    val dark = LocalSabzDarkTheme.current

    var categoryMenu by remember { mutableStateOf(false) }
    var provinceMenu by remember { mutableStateOf(false) }

    val today = LocalDate.now().toString()
    val hour = LocalTime.now().hour
    val greeting = when {
        hour in 5..11 -> "صبح بخیر"
        hour in 12..16 -> "روز بخیر"
        else -> "شب بخیر"
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(padding)) {
            // --- سرصفحه: سلام + تاریخ جلالی ---
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(greeting + " 👋", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${Jalali.weekdayName(today)}، ${Jalali.formatLong(today)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                NotificationIconButton { navController.navigate(Routes.NOTIFICATIONS) }
                IconButton(onClick = viewModel::refresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "تازه‌سازی")
                }
            }

            // --- جستجو ---
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::updateQuery,
                placeholder = { Text("جستجوی کسب‌وکار، خدمت یا شهر…") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "پاک کردن")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                // فقط پارامترهایی که در همه‌ی نسخه‌های M3 1.x وجود دارند (سازگار با 1.2.1)
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (dark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.7f),
                    unfocusedContainerColor = if (dark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.7f),
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (dark) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )

            // --- فیلترها ---
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    GlassChip(
                        selected = state.selectedCategory.isNotBlank(),
                        onClick = { categoryMenu = true }
                    ) {
                        Icon(
                            Icons.Filled.Category,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (state.selectedCategory.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (state.selectedCategory.isBlank()) "دسته‌بندی" else state.selectedCategory,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (state.selectedCategory.isNotBlank()) FontWeight.Bold else FontWeight.Medium,
                            color = if (state.selectedCategory.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                item {
                    GlassChip(
                        selected = state.selectedProvince.isNotBlank(),
                        onClick = { provinceMenu = true }
                    ) {
                        Icon(
                            Icons.Filled.Place,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (state.selectedProvince.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (state.selectedProvince.isBlank()) "استان" else state.selectedProvince,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (state.selectedProvince.isNotBlank()) FontWeight.Bold else FontWeight.Medium,
                            color = if (state.selectedProvince.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (state.selectedCategory.isNotBlank() || state.selectedProvince.isNotBlank()) {
                    item {
                        TextButton(onClick = viewModel::clearFilters) {
                            Text("حذف فیلترها", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))

            // --- نتایج ---
            when (val r = state.results) {
                is UiState.Loading -> Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                is UiState.Error -> ErrorState(r.message, r.kind, onRetry = viewModel::refresh)
                UiState.Empty -> EmptyState(
                    icon = "🔍",
                    title = "کسب‌وکاری پیدا نشد",
                    subtitle = "عبارت دیگری را امتحان کنید یا فیلترها را حذف کنید."
                )
                is UiState.Success -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(r.data, key = { it.id }) { b ->
                        BusinessCard(b) { navController.navigate(Routes.business(b.id)) }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
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

@Composable
private fun SelectionDialog(
    title: String,
    options: List<Pair<String, Int>>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val dark = LocalSabzDarkTheme.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            LazyColumn(
                Modifier
                    .heightIn(max = 420.dp)
                    .fillMaxWidth()
            ) {
                item {
                    Text(
                        title,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(options) { (label, count) ->
                    Row(
                        Modifier
                            .clickable { onSelect(label) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(
                            "${Jalali.persianDigits(count)} کسب‌وکار",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun BusinessCard(b: BusinessSummary, onClick: () -> Unit) {
    GlassCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon = businessTypeIcon(b.businessType), tileSize = 52.dp, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    b.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    listOfNotNull(b.city, b.province).joinToString("، "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (b.services.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                b.services.take(4).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!b.address.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                b.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "مشاهده و رزرو",
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
