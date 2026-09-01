package ir.sabztime.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.core.SabzException
import ir.sabztime.app.core.UiState
import ir.sabztime.app.core.toSabzException
import ir.sabztime.app.data.ProfileData
import ir.sabztime.app.ui.Routes
import ir.sabztime.app.ui.components.AuroraBackdrop
import ir.sabztime.app.ui.components.ErrorState
import ir.sabztime.app.ui.components.GlassCard
import ir.sabztime.app.ui.components.GlassChip
import ir.sabztime.app.ui.components.IconBadge3D
import ir.sabztime.app.ui.components.LoadingState
import ir.sabztime.app.ui.components.glass
import ir.sabztime.app.ui.theme.ThemePrefs
import ir.sabztime.app.util.Jalali
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val data: UiState<ProfileData> = UiState.Loading,
    val logoutBusy: Boolean = false
)

class ProfileViewModel(private val graph: AppGraph) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = _state.value.copy(data = UiState.Loading)
        viewModelScope.launch {
            try {
                val d = graph.repository.me()
                _state.value = _state.value.copy(data = UiState.Success(d))
            } catch (e: Exception) {
                val sabz = e.toSabzException()
                _state.value = _state.value.copy(data = UiState.Error(sabz.toFriendlyMessage(), sabz.kind))
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        _state.value = _state.value.copy(logoutBusy = true)
        viewModelScope.launch {
            try { graph.repository.logout() } catch (e: Exception) { /* توکن محلی هم پاک می‌شود */ }
            graph.clearSession()
            _state.value = _state.value.copy(logoutBusy = false)
            onDone()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(graph: AppGraph, navController: NavHostController) {
    val viewModel: ProfileViewModel = viewModel { ProfileViewModel(graph) }
    val state by viewModel.state.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { if (state.data !is UiState.Success) viewModel.refresh() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("پروفایل", fontWeight = FontWeight.Bold) },
                actions = { NotificationBadgeButton { navController.navigate(Routes.NOTIFICATIONS) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AuroraBackdrop(Modifier.matchParentSize())

            when (val d = state.data) {
                is UiState.Loading -> LoadingState(Modifier.fillMaxSize())
                is UiState.Error -> ErrorState(d.message, d.kind, onRetry = viewModel::refresh, modifier = Modifier.fillMaxSize())
                UiState.Empty -> LoadingState(Modifier.fillMaxSize())
                is UiState.Success -> ProfileContent(
                    profile = d.data,
                    onOpen = { route -> navController.navigate(route) },
                    onLogoutRequest = { showLogoutDialog = true }
                )
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("خروج") },
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
                }) { Text("خروج") }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("منصرف شدم") } }
        )
    }
}

@Composable
private fun ProfileContent(
    profile: ProfileData,
    onOpen: (String) -> Unit,
    onLogoutRequest: () -> Unit
) {
    val context = LocalContext.current
    val name = profile.user.name.trim().ifBlank { "کاربر نوبت‌سبز" }
    val initial = profile.user.name.trim().take(1).ifBlank { "ن" }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- هدر کاربر ---
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(MaterialTheme.glass.badgeTop, MaterialTheme.glass.badgeBottom)
                            )
                        )
                        .border(
                            1.dp,
                            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        Jalali.persianDigits(profile.user.phone.ifBlank { "—" }),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            profile.credit?.let { credit ->
                Spacer(Modifier.height(12.dp))
                GlassChip(text = "⭐ ${Jalali.persianDigits(credit.currentCredit)} امتیاز اعتباری")
            }
        }

        // --- آمار ---
        GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatTile("کل رزرو", profile.bookingStats.total)
                StatTile("پیش رو", profile.bookingStats.upcoming)
                StatTile("انجام‌شده", profile.bookingStats.completed)
                StatTile("کسب امتیاز", profile.credit?.totalEarned ?: 0)
            }
        }

        // --- ظاهر برنامه (تم روشن/تاریک/سیستم) ---
        GlassCard {
            Text("ظاهر برنامه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeOption(
                    "سیستم", Icons.Filled.BrightnessAuto,
                    selected = ThemePrefs.darkMode == null,
                    modifier = Modifier.weight(1f)
                ) { ThemePrefs.set(context, null) }
                ThemeOption(
                    "روشن", Icons.Filled.LightMode,
                    selected = ThemePrefs.darkMode == false,
                    modifier = Modifier.weight(1f)
                ) { ThemePrefs.set(context, false) }
                ThemeOption(
                    "تاریک", Icons.Filled.DarkMode,
                    selected = ThemePrefs.darkMode == true,
                    modifier = Modifier.weight(1f)
                ) { ThemePrefs.set(context, true) }
            }
        }

        // --- منو ---
        GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp)) {
            MenuRow("کسب‌وکارهای من", Icons.Filled.Storefront, badgeTop = Color(0xFF34B36A), badgeBottom = Color(0xFF128A4C)) { onOpen(Routes.MY_BUSINESSES) }
            MenuDivider()
            MenuRow("داشبورد بازاریاب", Icons.Filled.Campaign, badgeTop = Color(0xFFFBBF24), badgeBottom = Color(0xFFD97706)) { onOpen(Routes.MARKETER) }
            MenuDivider()
            MenuRow("اعلان‌ها", Icons.Filled.Notifications, badgeTop = Color(0xFF60A5FA), badgeBottom = Color(0xFF2563EB)) { onOpen(Routes.NOTIFICATIONS) }
            MenuDivider()
            MenuRow("راهنما", Icons.Filled.Help, badgeTop = Color(0xFFA78BFA), badgeBottom = Color(0xFF7C3AED)) { onOpen(Routes.HELP) }
            MenuDivider()
            MenuRow("خروج از حساب", Icons.Filled.Logout, badgeTop = Color(0xFFF87171), badgeBottom = Color(0xFFB91C1C), tintLabel = MaterialTheme.colorScheme.error) { onLogoutRequest() }
        }

        Text(
            "نسخه ۱٫۰٫۰ — نوبت‌سبز 🌿",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StatTile(label: String, value: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        Text(
            Jalali.persianDigits(value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ThemeOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val g = MaterialTheme.glass
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .shadow(elevation = if (selected) 5.dp else 0.dp, shape = shape)
            .clip(shape)
            .background(
                if (selected) Brush.verticalGradient(listOf(g.badgeTop, g.badgeBottom))
                else Brush.verticalGradient(listOf(g.cardTop, g.cardBottom))
            )
            .border(
                1.dp,
                if (selected) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.Transparent))
                else Brush.verticalGradient(listOf(g.strokeTop, g.strokeBottom)),
                shape
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        modifier = Modifier.padding(start = 62.dp)
    )
}

@Composable
private fun MenuRow(
    label: String,
    icon: ImageVector,
    badgeTop: Color,
    badgeBottom: Color,
    tintLabel: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge3D(icon, size = 42.dp, containerTop = badgeTop, containerBottom = badgeBottom, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = tintLabel,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
