package ir.sabztime.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.core.SabzException
import ir.sabztime.app.core.UiState
import ir.sabztime.app.data.AuthResponse
import ir.sabztime.app.ui.Routes
import ir.sabztime.app.util.Jalali
import ir.sabztime.app.util.TextUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val graph: AppGraph) : ViewModel() {
    // حالت اولیه «ایdle» (Empty) است؛ Loading فقط در لحظه‌ی ارسال فرم تنظیم می‌شود.
    // اگر اینجا Loading بود، دکمه تا همیشه غیرفعال و چرخنده تا همیشه فعال می‌ماند.
    private val _state = MutableStateFlow<UiState<AuthResponse>>(UiState.Empty)
    val state: StateFlow<UiState<AuthResponse>> = _state.asStateFlow()

    var busy = false
        private set

    fun login(phone: String, password: String) {
        if (busy) return
        busy = true
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                val resp = graph.repository.login(phone, password, graph.deviceName)
                val token = resp.token
                if (token.isNullOrBlank()) {
                    _state.value = UiState.Error("پاسخ نامعتبر از سرور")
                } else {
                    graph.saveToken(token)
                    graph.markSessionState(true)
                    _state.value = UiState.Success(resp)
                }
            } catch (e: Exception) {
                val sabz = e as? SabzException ?: SabzException.Network(e)
                _state.value = UiState.Error(sabz.toFriendlyMessage(), sabz.kind)
            } finally {
                busy = false
            }
        }
    }

    fun register(phone: String, password: String, name: String) {
        if (busy) return
        busy = true
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                val resp = graph.repository.register(phone, password, name, graph.deviceName)
                val token = resp.token
                if (token.isNullOrBlank()) {
                    _state.value = UiState.Error("پاسخ نامعتبر از سرور")
                } else {
                    graph.saveToken(token)
                    graph.markSessionState(true)
                    _state.value = UiState.Success(resp)
                }
            } catch (e: Exception) {
                val sabz = e as? SabzException ?: SabzException.Network(e)
                _state.value = UiState.Error(sabz.toFriendlyMessage(), sabz.kind)
            } finally {
                busy = false
            }
        }
    }

    fun resetError() {
        // به حالت خنثی برمی‌گردیم (نه Loading) تا دکمه دوباره فعال بماند
        if (_state.value is UiState.Error) _state.value = UiState.Empty
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(graph: AppGraph, navController: NavHostController) {
    val viewModel: LoginViewModel = viewModel { LoginViewModel(graph) }
    val state by viewModel.state.collectAsState()

    var tab by rememberSaveable { mutableStateOf(0) }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }

    // ورود موفق → صفحه اصلی
    androidx.compose.runtime.LaunchedEffect(state) {
        if (state is UiState.Success<*>) {
            navController.navigate(Routes.MAIN) {
                popUpTo(Routes.LOGIN) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    fun validate(): Boolean {
        phoneError = null; passwordError = null; nameError = null
        val normalizedPhone = TextUtil.normalizeMobile(phone)
        if (normalizedPhone == null) {
            phoneError = "شماره موبایل معتبر نیست (مثال: 09123456789)"
        } else {
            phone = normalizedPhone
        }
        if (password.length < 6) passwordError = "رمز عبور حداقل ۶ کاراکتر"
        if (tab == 1 && name.trim().length < 2) nameError = "نام و نام خانوادگی را وارد کنید"
        return phoneError == null && passwordError == null && nameError == null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🌿", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "نوبت‌سبز",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "جستجو، رزرو و مدیریت نوبت در یک اپلیکیشن",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        TabRow(selectedTabIndex = tab, modifier = Modifier.fillMaxWidth()) {
            Tab(selected = tab == 0, onClick = { tab = 0; viewModel.resetError() }, text = { Text("ورود") })
            Tab(selected = tab == 1, onClick = { tab = 1; viewModel.resetError() }, text = { Text("ثبت‌نام") })
        }
        Spacer(Modifier.height(20.dp))

        if (tab == 1) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = null },
                label = { Text("نام و نام خانوادگی") },
                singleLine = true,
                isError = nameError != null,
                supportingText = { nameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = phone,
            onValueChange = {
                phone = TextUtil.latinDigits(it).filter { c -> c.isDigit() || c == '+' }
                phoneError = null
            },
            label = { Text("شماره موبایل") },
            singleLine = true,
            isError = phoneError != null,
            supportingText = { phoneError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; passwordError = null },
            label = { Text("رمز عبور") },
            singleLine = true,
            isError = passwordError != null,
            supportingText = { passwordError?.let { Text(it) } },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        when (val s = state) {
            is UiState.Error -> {
                androidx.compose.material3.Text(
                    s.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }
            is UiState.Loading -> androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.height(24.dp)
            )
            else -> {}
        }

        Button(
            onClick = {
                if (validate()) {
                    if (tab == 0) viewModel.login(phone, password)
                    else viewModel.register(phone, password, name)
                }
            },
            enabled = state !is UiState.Loading,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(if (tab == 0) "ورود به نوبت‌سبز" else "ایجاد حساب کاربری", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(onClick = { tab = if (tab == 0) 1 else 0; viewModel.resetError() }, modifier = Modifier.fillMaxWidth()) {
            Text(if (tab == 0) "حساب ندارید؟ ثبت‌نام کنید" else "قبلاً ثبت‌نام کرده‌اید؟ وارد شوید")
        }

        TextButton(onClick = { pinScreen(navController) }) {
            Text("درباره سرویس نوبت‌سبز")
        }
        Text(
            "نکته: اطلاعات شما با حساب تلگرام و سیستم رزرو نوبت‌سبز یکپارچه است.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text("\n" + Jalali.persianDigits("نسخه ۱٫۰٫۰"), style = MaterialTheme.typography.labelSmall)
    }
}

private fun pinScreen(navController: NavHostController) {
    navController.navigate(Routes.HELP)
}
