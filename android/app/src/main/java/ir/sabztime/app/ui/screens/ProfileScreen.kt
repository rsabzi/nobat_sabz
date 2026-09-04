package ir.sabztime.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import ir.sabztime.app.ui.components.BottomBarSpacer
import ir.sabztime.app.ui.components.ErrorState
import ir.sabztime.app.ui.components.GhostIconButton
import ir.sabztime.app.ui.components.GlassCard
import ir.sabztime.app.ui.components.GoldDivider
import ir.sabztime.app.ui.components.GoldText
import ir.sabztime.app.ui.components.LoadingState
import ir.sabztime.app.ui.components.LuxMenuRow
import ir.sabztime.app.ui.components.ProfileHeroCard
import ir.sabztime.app.ui.components.SectionTitle
import ir.sabztime.app.ui.components.SegmentedControl
import ir.sabztime.app.ui.components.SoftBanner
import ir.sabztime.app.ui.components.TilePalette
import ir.sabztime.app.ui.theme.CocoaSoft
import ir.sabztime.app.ui.theme.Cream
import ir.sabztime.app.ui.theme.CreamDim
import ir.sabztime.app.ui.theme.Gold
import ir.sabztime.app.ui.theme.GoldDim
import ir.sabztime.app.ui.theme.GoldLight
import ir.sabztime.app.ui.theme.LuxCardShape
import ir.sabztime.app.ui.theme.MidnightSoft
import ir.sabztime.app.ui.theme.NavyCardDeep
import ir.sabztime.app.ui.theme.PureWhite
import ir.sabztime.app.ui.theme.TextMuted
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
    val logoutBusy: Boolean = false,
    val deleteBusy: Boolean = false
)

class ProfileViewModel(private val graph: AppGraph) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
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
                // توکن محلی در هر صورت پاک می‌شود
            }
            graph.clearSession()
            _state.value = _state.value.copy(logoutBusy = false)
            onDone()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { graph.themeManager.setMode(mode) }
    }

    /** حذف کامل حساب از سرور + پاک‌کردن نشست محلی. در خطا حساب محلی پاک نمی‌شود. */
    fun deleteAccount(onDone: () -> Unit) {
        if (_state.value.deleteBusy) return
        _state.value = _state.value.copy(deleteBusy = true)
        viewModelScope.launch {
            try {
                graph.repository.deleteAccount()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val s = classify(e)
                _state.value = _state.value.copy(
                    deleteBusy = false,
                    error = s.message,
                    errorKind = s.kind
                )
                return@launch
            }
            graph.clearSession()
            _state.value = _state.value.copy(deleteBusy = false)
            onDone()
        }
    }
}

/**
 * صفحه‌ی پروفایل — کارت داشبورد زرشکی عمیق:
 *  ردیف هویت (ستاره‌ی طلایی، نام، امتیاز/سطح، آواتار دایره‌ای)
 *  + لیست منو (کسب‌وکارهای من، داشبورد بازاریاب، اعلان‌ها، تنظیمات)
 */
@Composable
fun ProfileScreen(graph: AppGraph, navController: NavHostController) {
    val viewModel: ProfileViewModel = viewModel { ProfileViewModel(graph) }
    val state by viewModel.state.collectAsState()
    val themeMode by graph.themeManager.mode.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val error = state.error
    val profile = state.profile

    Scaffold(containerColor = Color.Transparent) { padding ->
        when {
            state.loading -> LoadingState(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            )

            error != null && profile == null -> ErrorState(
                error,
                state.errorKind,
                onRetry = viewModel::refresh
            )

            else -> profile?.let { p ->
                val credit = p.credit
                // امتیاز نمایشی از اعتبار کاربر (۰ تا ۵) — سطح ممتاز از تعداد نوبت‌های انجام‌شده
                val score = ((credit?.currentCredit ?: 0).coerceAtMost(100) / 20.0)
                    .coerceAtLeast(0.0)
                val scoreText = "${Jalali.persianDigits(String.format(java.util.Locale.US, "%.1f", score))}/۵"
                val level = when {
                    p.bookingStats.completed >= 20 -> "سطح ممتاز"
                    p.bookingStats.completed >= 8 -> "سطح طلایی"
                    p.bookingStats.completed >= 3 -> "سطح نقره‌ای"
                    else -> "کاربر تازه‌وارد"
                }

                Column(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Spacer(Modifier.height(4.dp))

                    // ── سرصفحه ──
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "پروفایل",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Cream
                            )
                            Text(
                                Jalali.persianDigits(p.user.phone),
                                style = MaterialTheme.typography.labelMedium,
                                color = CocoaSoft
                            )
                        }
                        GhostIconButton(
                            icon = Icons.Filled.Refresh,
                            contentDescription = "تازه‌سازی",
                            onClick = viewModel::refresh
                        )
                    }

                    if (state.offline) {
                        SoftBanner("اتصال برقرار نیست — آخرین اطلاعات ذخیره‌شده نمایش داده می‌شود.")
                    }

                    // ── کارت داشبورد زرشکی ──
                    ProfileHeroCard(
                        name = p.user.name.ifBlank { "کاربر نوبت‌سبز" },
                        rating = "$scoreText ($level)",
                        level = "امتیاز"
                    ) {
                        Column {
                            LuxMenuRow(
                                icon = Icons.Filled.Storefront,
                                label = "کسب‌وکارهای من",
                                trailing = if (p.businesses.isNotEmpty())
                                    Jalali.persianDigits(p.businesses.size) else null,
                                onClick = { navController.navigate(Routes.MY_BUSINESSES) }
                            )
                            GoldDivider()
                            LuxMenuRow(
                                icon = Icons.Filled.Campaign,
                                label = "داشبورد بازاریاب",
                                onClick = { navController.navigate(Routes.MARKETER) }
                            )
                            GoldDivider()
                            LuxMenuRow(
                                icon = Icons.Filled.Notifications,
                                label = "اعلان‌ها",
                                onClick = { navController.navigate(Routes.NOTIFICATIONS) }
                            )
                            GoldDivider()
                            LuxMenuRow(
                                icon = Icons.Filled.Settings,
                                label = "تنظیمات",
                                onClick = { navController.navigate(Routes.HELP) }
                            )
                        }
                    }

                    // ── پوسته برنامه ──
                    GlassCard(contentPadding = 16.dp) {
                        Column {
                            Text(
                                "پوسته برنامه",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Cream
                            )
                            Spacer(Modifier.height(12.dp))
                            SegmentedControl(
                                options = listOf("خودکار", "روشن", "تاریک"),
                                selected = themeMode.ordinal,
                                onSelect = { viewModel.setThemeMode(ThemeMode.values()[it]) },
                                icons = listOf(
                                    Icons.Filled.BrightnessAuto,
                                    Icons.Filled.WbSunny,
                                    Icons.Filled.DarkMode
                                )
                            )
                        }
                    }

                    // ── اعتبار و آمار ──
                    if (credit != null) {
                        GlassCard(pattern = true, contentPadding = 18.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "اعتبار شما",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = CreamDim
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    GoldText(
                                        text = "${Jalali.persianDigits(credit.currentCredit)} امتیاز",
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                }
                                ir.sabztime.app.ui.components.IconTile(
                                    icon = Icons.Filled.Campaign,
                                    tileSize = 48.dp,
                                    palette = TilePalette.GOLD,
                                    contentDescription = null
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            GoldDivider()
                            Spacer(Modifier.height(14.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                LuxStat("کل رزرو", Jalali.persianDigits(p.bookingStats.total))
                                LuxStat("پیش رو", Jalali.persianDigits(p.bookingStats.upcoming))
                                LuxStat("انجام‌شده", Jalali.persianDigits(p.bookingStats.completed))
                                LuxStat("کسب امتیاز", Jalali.persianDigits(credit.totalEarned))
                            }
                        }
                    }

                    // ── پشتیبانی ──
                    SectionTitle("پشتیبانی و حساب")
                    GlassCard(contentPadding = 12.dp) {
                        LuxMenuRow(
                            icon = Icons.Filled.HelpOutline,
                            label = "راهنما و پرسش‌های متداول",
                            onClick = { navController.navigate(Routes.HELP) }
                        )
                        GoldDivider()
                        LuxMenuRow(
                            icon = Icons.Filled.PrivacyTip,
                            label = "حریم خصوصی",
                            onClick = {
                                // صفحه‌ی privacy.php کنار api.php روی سرور قرار دارد
                                val url = BuildConfig.API_BASE_URL.trimEnd('/') + "/privacy.php"
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            }
                        )
                        GoldDivider()
                        LuxMenuRow(
                            icon = Icons.Filled.Logout,
                            label = "خروج از حساب",
                            palette = TilePalette.ERROR,
                            onClick = { showLogoutDialog = true }
                        )
                        GoldDivider()
                        LuxMenuRow(
                            icon = Icons.Filled.DeleteForever,
                            label = "حذف حساب کاربری",
                            palette = TilePalette.ERROR,
                            onClick = { showDeleteDialog = true }
                        )
                    }

                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "نوبت‌سبز — نسخه ${Jalali.persianDigits(BuildConfig.VERSION_NAME)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted.copy(alpha = 0.7f)
                        )
                    }

                    BottomBarSpacer()
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            shape = LuxCardShape,
            containerColor = MidnightSoft,
            titleContentColor = Cream,
            textContentColor = CreamDim,
            title = { Text("خروج از حساب", fontWeight = FontWeight.Bold) },
            text = { Text("آیا از حساب کاربری خود خارج می‌شوید؟") },
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
                    Text("خروج", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("منصرف شدم", color = GoldDim)
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!state.deleteBusy) showDeleteDialog = false },
            shape = LuxCardShape,
            containerColor = MidnightSoft,
            titleContentColor = Cream,
            textContentColor = CreamDim,
            title = { Text("حذف حساب کاربری", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "حساب شما و همه‌ی داده‌هایتان (نوبت‌ها، کسب‌وکارها و امتیازها) برای همیشه از سرور حذف می‌شود.\n\nاین عمل قابل بازگشت نیست. مطمئن هستید؟"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount {
                            showDeleteDialog = false
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    enabled = !state.deleteBusy
                ) {
                    Text(
                        if (state.deleteBusy) "در حال حذف…" else "بله، برای همیشه حذف کن",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !state.deleteBusy
                ) {
                    Text("منصرف شدم", color = GoldDim)
                }
            }
        )
    }
}

/** آمار کوچک لوکس: عدد طلایی برس‌خورده + برچسب قهوه‌ایِ ملایم */
@Composable
private fun LuxStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xE6FFFFFF), Color(0xAAEDF1F6))
                    )
                )
                .border(
                    ir.sabztime.app.ui.theme.goldBorder(0.4f),
                    RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            GoldText(text = value, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(5.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}
