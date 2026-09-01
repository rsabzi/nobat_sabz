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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import ir.sabztime.app.core.SabzException
import ir.sabztime.app.core.UiState
import ir.sabztime.app.core.toSabzException
import ir.sabztime.app.data.BusinessSummary
import ir.sabztime.app.data.FiltersData
import ir.sabztime.app.ui.Routes
import ir.sabztime.app.ui.components.AuroraBackdrop
import ir.sabztime.app.ui.components.EmojiBadge3D
import ir.sabztime.app.ui.components.EmptyState
import ir.sabztime.app.ui.components.ErrorState
import ir.sabztime.app.ui.components.GlassCard
import ir.sabztime.app.ui.components.StatusChip
import ir.sabztime.app.ui.components.glass
import ir.sabztime.app.util.Jalali
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

    init {
        viewModelScope.launch { loadFilters() }
        viewModelScope.launch { search() }
    }

    fun updateQuery(q: String) {
        _state.value = _state.value.copy(query = q)
        viewModelScope.launch { search(debounceMs = 350) }
    }

    fun selectCategory(c: String) {
        _state.value = _state.value.copy(selectedCategory = if (c == _state.value.selectedCategory) "" else c)
        viewModelScope.launch { search() }
    }

    fun selectProvince(p: String) {
        _state.value = _state.value.copy(selectedProvince = if (p == _state.value.selectedProvince) "" else p)
        viewModelScope.launch { search() }
    }

    fun clearFilters() {
        _state.value = _state.value.copy(selectedCategory = "", selectedProvince = "")
        viewModelScope.launch { search() }
    }

    fun retry() {
        viewModelScope.launch {
            if (!_state.value.filtersLoaded) loadFilters()
            search()
        }
    }

    private suspend fun loadFilters() {
        try {
            val f = graph.repository.filters()
            _state.value = _state.value.copy(filters = f, filtersLoaded = true)
        } catch (e: Exception) {
            // فیلترها اختیاری هستند
        }
    }

    suspend fun search(debounceMs: Long = 0) {
        if (debounceMs > 0) kotlinx.coroutines.delay(debounceMs)
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
        } catch (e: Exception) {
            val sabz = e.toSabzException()
            _state.value = _state.value.copy(results = UiState.Error(sabz.toFriendlyMessage(), sabz.kind))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(graph: AppGraph, navController: NavHostController) {
    val viewModel: HomeViewModel = viewModel { HomeViewModel(graph) }
    val state by viewModel.state.collectAsState()

    var categoryMenu by remember { mutableStateOf(false) }
    var provinceMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🌿 نوبت‌سبز", fontWeight = FontWeight.Bold)
                        Text(
                            "جستجوی کسب‌وکار و رزرو نوبت",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    NotificationBadgeButton { navController.navigate(Routes.NOTIFICATIONS) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            AuroraBackdrop(Modifier.matchParentSize())
            Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::updateQuery,
                placeholder = { Text("نام کسب‌وکار، خدمت یا شهر…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "جستجو") },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "پاک کردن")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.glass.cardTop,
                    unfocusedContainerColor = MaterialTheme.glass.cardBottom,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AssistChip(
                        onClick = { categoryMenu = true },
                        label = {
                            Text(if (state.selectedCategory.isBlank()) "دسته‌بندی" else state.selectedCategory)
                        },
                        leadingIcon = { Text(if (state.selectedCategory.isBlank()) "▾" else "✓") }
                    )
                }
                item {
                    AssistChip(
                        onClick = { provinceMenu = true },
                        label = {
                            Text(if (state.selectedProvince.isBlank()) "استان" else state.selectedProvince)
                        },
                        leadingIcon = { Text(if (state.selectedProvince.isBlank()) "▾" else "✓") }
                    )
                }
                if (state.selectedCategory.isNotBlank() || state.selectedProvince.isNotBlank()) {
                    item {
                        TextButton(onClick = viewModel::clearFilters) { Text("حذف فیلترها") }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))

            when (val r = state.results) {
                is UiState.Loading -> CircularProgressIndicator(
                    Modifier.padding(32.dp).align(Alignment.CenterHorizontally)
                )
                is UiState.Error -> ErrorState(r.message, r.kind, onRetry = viewModel::retry)
                UiState.Empty -> EmptyState(
                    icon = "🔍",
                    title = "کسب‌وکاری پیدا نشد",
                    subtitle = "عبارت دیگری را امتحان کنید یا فیلترها را حذف کنید.",
                    action = { Button(onClick = viewModel::retry) { Text("جستجوی دوباره") } }
                )
                is UiState.Success -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(r.data, key = { it.id }) { b ->
                        BusinessCard(b) { navController.navigate(Routes.business(b.id)) }
                    }
                }
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
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 4.dp) {
            LazyColumn(Modifier.heightIn(max = 420.dp).fillMaxWidth()) {
                item { ListItem(headlineContent = { Text(title, fontWeight = FontWeight.Bold) }) }
                items(options) { (label, count) ->
                    ListItem(
                        headlineContent = { Text(label) },
                        supportingContent = { Text("${Jalali.persianDigits(count)} کسب‌وکار") },
                        modifier = Modifier.clickable { onSelect(label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BusinessCard(b: BusinessSummary, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge3D(typeEmoji(b.businessType), size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    b.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    listOfNotNull(b.city, b.province).joinToString("، "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (b.services.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        b.services.take(3).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            b.businessType?.let {
                StatusChip(it, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (!b.address.isNullOrBlank()) {
            Text(
                "📍 ${b.address}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
        }
        Text(
            "مشاهده و رزرو ←",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** اموجی سه‌بعدی متناسب با نوع کسب‌وکار برای تشخیص سریع‌تر */
internal fun typeEmoji(type: String?): String {
    val t = type ?: return "🏪"
    return when {
        t.contains("آرایش") || t.contains("زیبایی") -> "💇"
        t.contains("دندان") -> "🦷"
        t.contains("پزشک") || t.contains("درمان") || t.contains("کلینیک") -> "🩺"
        t.contains("کافی") || t.contains("کافه") -> "☕"
        t.contains("رستوران") || t.contains("غذا") || t.contains("تیپ") -> "🍽"
        t.contains("ورزش") || t.contains("باشگاه") -> "🏋"
        t.contains("آموزش") || t.contains("آموزشگاه") -> "📚"
        t.contains("خودرو") || t.contains("تعمیر") || t.contains("فنی") -> "🛠"
        t.contains("پوشاک") || t.contains("فروشگاه") -> "🛍"
        t.contains("عکاسی") -> "📸"
        else -> "🏪"
    }
}
