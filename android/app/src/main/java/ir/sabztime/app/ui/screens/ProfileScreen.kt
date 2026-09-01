package ir.sabztime.app.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.core.SabzException
import ir.sabztime.app.core.UiState
import ir.sabztime.app.data.ProfileData
import ir.sabztime.app.ui.Routes
import ir.sabztime.app.ui.components.ErrorState
import ir.sabztime.app.ui.components.InfoRow
import ir.sabztime.app.ui.components.SectionCard
import ir.sabztime.app.ui.components.collectAsStateSafe
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
                val sabz = e as? SabzException ?: SabzException.Network(e)
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

private data class MenuItem(val label: String, val icon: ImageVector, val route: String?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(graph: AppGraph, navController: NavHostController) {
    val viewModel: ProfileViewModel = viewModel { ProfileViewModel(graph) }
    val state by viewModel.state.collectAsStateSafe()
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { if (state.data !is UiState.Success) viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("پروفایل", fontWeight = FontWeight.Bold) },
                actions = { NotificationBadgeButton { navController.navigate(Routes.NOTIFICATIONS) } }
            )
        }
    ) { padding ->
        when (val d = state.data) {
            is UiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is UiState.Error -> ErrorState(d.message, d.kind, onRetry = viewModel::refresh)
            is UiState.Success -> {
                val profile = d.data
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // کارت کاربر
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(profile.user.name.take(1).ifBlank { "👤" }, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                            Spacer(Modifier.size(12.dp))
                            Column {
                                Text(profile.user.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(Jalali.persianDigits(profile.user.phone), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // امتیاز اعتباری
                    profile.credit?.let { credit ->
                        SectionCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⭐ اعتبار شما", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "${Jalali.persianDigits(credit.currentCredit)} امتیاز",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                StatItem("کل رزرو", profile.bookingStats.total)
                                StatItem("پیش رو", profile.bookingStats.upcoming)
                                StatItem("انجام‌شده", profile.bookingStats.completed)
                                StatItem("کسب امتیاز", credit.totalEarned)
                            }
                        }
                    }

                    // منو
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column {
                            MenuRow("کسب‌وکارهای من", Icons.Filled.Storefront, Routes.MY_BUSINESSES) { navController.navigate(Routes.MY_BUSINESSES) }
                            MenuRow("داشبورد بازاریاب", Icons.Filled.Campaign, Routes.MARKETER) { navController.navigate(Routes.MARKETER) }
                            MenuRow("اعلان‌ها", Icons.Filled.Notifications, Routes.NOTIFICATIONS) { navController.navigate(Routes.NOTIFICATIONS) }
                            MenuRow("راهنما", Icons.Filled.Help, Routes.HELP) { navController.navigate(Routes.HELP) }
                            MenuRow("خروج از حساب", Icons.Filled.Logout, null) { showLogoutDialog = true }
                        }
                    }

                    Text(
                        "نسخه ۱٫۰٫۰ — نوبت‌سبز",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
            else -> {}
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
private fun StatItem(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(Jalali.persianDigits(value), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MenuRow(label: String, icon: ImageVector, route: String?, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    )
}
