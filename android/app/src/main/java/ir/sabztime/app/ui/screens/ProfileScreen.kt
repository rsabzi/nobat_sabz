package ir.sabztime.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import ir.sabztime.app.ui.theme.LocalSabzDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import ir.sabztime.app.BuildConfig
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.core.SabzException
import ir.sabztime.app.core.ThemeMode
import ir.sabztime.app.core.classify
import ir.sabztime.app.data.ProfileData
import ir.sabztime.app.ui.Routes
import ir.sabztime.app.ui.components.ErrorState
import ir.sabztime.app.ui.theme.GlassBackground
import ir.sabztime.app.ui.components.GlassCard
import ir.sabztime.app.ui.components.GlassTopBar
import ir.sabztime.app.ui.components.IconTile
import ir.sabztime.app.ui.components.LoadingState
import ir.sabztime.app.ui.components.NotificationIconButton
import ir.sabztime.app.ui.components.SegmentedControl
import ir.sabztime.app.ui.components.StatItem
import ir.sabztime.app.ui.components.TilePalette
import ir.sabztime.app.util.Jalali
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: ProfileData? = null,
    val loading: Boolean = true,
    val refreshBusy: Boolean = false,
    val error: String? = null,
    val errorKind: SabzException.Kind = SabzException.Kind.UNKNOWN,
    /** خطا رخ داده ولی آخرین پروفایل بارگذاری‌شده هنوز نمایش داده می‌شود */
    val offline: Boolean = false,
    val logoutBusy: Boolean = false
)

class ProfileViewModel(private val graph: AppGraph) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        // بارگذاری فقط یک‌بار؛ تغییر تب دیگر درخواست تکراری ایجاد نمی‌کند
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val had = _state.value.profile != null
            _state.value = _state.value.copy(
                loading = !had,
                refreshBusy = had,
                error = null,
                offline = false
            )
            try {
                val d = graph.repository.me()
                _state.value = _state.value.copy(profile = d, loading = false, refreshBusy = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val s = classify(e)
                _state.value = _state.value.copy(
                    loading = false,
                    refreshBusy = false,
                    error = s.message,
                    errorKind = s.kind,
                    offline = _state.value.profile != null
                )
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        _state.value = _state.value.copy(logoutBusy = true)
        viewModelScope.launch {
            try {
                graph.repository.logout()
            } catch (e: Exception) {
                // توکن محلی هم پاک می‌شود
            }
            graph.clearSession()
            _state.value = _state.value.copy(logoutBusy = false)
            onDone()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { graph.themeManager.setMode(mode) }
    }
}

@Composable
fun ProfileScreen(graph: AppGraph, navController: NavHostController) {
    val viewModel: ProfileViewModel = viewModel { ProfileViewModel(graph) }
    val state by viewModel.state.collectAsState()
    val themeMode by graph.themeManager.mode.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    // مقادیر در val محلی: smart cast روی property دلیگیت‌شده ممکن نیست
    val error = state.error
    val profile = state.profile

    GlassBackground {
        androidx.compose.material3.Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                GlassTopBar(
                    title = "پروفایل",
                    actions = {
                        NotificationIconButton { navController.navigate(Routes.NOTIFICATIONS) }
                        if (state.profile != null) {
                            IconButton(onClick = viewModel::refresh) {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = "تازه‌سازی",
                                    modifier = if (state.refreshBusy) Modifier.size(18.dp) else Modifier
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            when {
                state.loading -> LoadingState(Modifier.fillMaxSize().padding(padding))
                error != null && profile == null -> ErrorState(
                    error,
                    state.errorKind,
                    onRetry = viewModel::refresh
                )
                else -> {
                    profile?.let { profile ->
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // نوار آفلاین
                            if (state.offline) {
                                OfflineBanner()
                            }

                            // --- کارت کاربر ---
                            GlassCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Avatar(profile.user.name)
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            profile.user.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            Jalali.persianDigits(profile.user.phone),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // --- اعتبار ---
                            profile.credit?.let { credit ->
                                GlassCard {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconTile(
                                            icon = Icons.Filled.Star,
                                            tileSize = 48.dp,
                                            palette = TilePalette.AMBER,
                                            contentDescription = null
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text("اعتبار شما", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(
                                                Jalali.persianDigits(credit.currentCredit) + " امتیاز",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        StatItem("کل رزرو", Jalali.persianDigits(profile.bookingStats.total))
                                        StatItem("پیش رو", Jalali.persianDigits(profile.bookingStats.upcoming))
                                        StatItem("انجام‌شده", Jalali.persianDigits(profile.bookingStats.completed))
                                        StatItem("کسب امتیاز", Jalali.persianDigits(credit.totalEarned))
                                    }
                                }
                            }

                            // --- منو ---
                            GlassCard {
                                MenuRow(
                                    icon = Icons.Filled.Storefront,
                                    label = "کسب‌وکارهای من",
                                    onClick = { navController.navigate(Routes.MY_BUSINESSES) }
                                )
                                RowDivider()
                                MenuRow(
                                    icon = Icons.Filled.Campaign,
                                    label = "داشبورد بازاریاب",
                                    palette = TilePalette.AMBER,
                                    onClick = { navController.navigate(Routes.MARKETER) }
                                )
                                RowDivider()
                                MenuRow(
                                    icon = Icons.Filled.Notifications,
                                    label = "اعلان‌ها",
                                    onClick = { navController.navigate(Routes.NOTIFICATIONS) }
                                )
                                RowDivider()
                                MenuRow(
                                    icon = Icons.Filled.Help,
                                    label = "راهنما",
                                    onClick = { navController.navigate(Routes.HELP) }
                                )
                            }

                            // --- پوسته ---
                            GlassCard {
                                Text("پوسته برنامه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(10.dp))
                                SegmentedControl(
                                    options = listOf("خودکار", "روشن", "تاریک"),
                                    selected = themeMode.ordinal,
                                    onSelect = { viewModel.setThemeMode(ThemeMode.values()[it]) },
                                    icons = listOf(Icons.Filled.BrightnessAuto, Icons.Filled.WbSunny, Icons.Filled.DarkMode)
                                )
                            }

                            // --- خروج ---
                            GlassCard {
                                MenuRow(
                                    icon = Icons.Filled.Logout,
                                    label = "خروج از حساب",
                                    palette = TilePalette.ERROR,
                                    onClick = { showLogoutDialog = true }
                                )
                            }

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "نوبت‌سبز — نسخه ${Jalali.persianDigits(BuildConfig.VERSION_NAME)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("خروج", fontWeight = FontWeight.Bold) },
            text = { Text("آیا از حساب خود خارج می‌شوید؟") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }) {
                    Text("خروج", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("منصرف شدم") } }
        )
    }
}

@Composable
private fun Avatar(name: String) {
    val initial = name.trim().take(1).ifBlank { "؟" }
    Box(
        Modifier
            .size(64.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = if (LocalSabzDarkTheme.current) {
                        listOf(Color(0xFF5C9BFF), Color(0xFF1D53A8))
                    } else {
                        listOf(Color(0xFF1E6FE0), Color(0xFF0B4F9E))
                    }
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initial,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun OfflineBanner() {
    Text(
        "اتصال برقرار نیست — آخرین اطلاعات ذخیره‌شده نمایش داده می‌شود.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

@Composable
private fun RowDivider() {
    Spacer(Modifier.height(2.dp))
    androidx.compose.material3.HorizontalDivider(
        color = if (LocalSabzDarkTheme.current) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
    )
    Spacer(Modifier.height(2.dp))
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    palette: TilePalette = TilePalette.GREEN,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon = icon, tileSize = 40.dp, palette = palette, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Filled.ChevronLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
