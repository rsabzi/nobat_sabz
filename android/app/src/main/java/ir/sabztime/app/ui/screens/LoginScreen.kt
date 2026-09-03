package ir.sabztime.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.core.SabzException
import ir.sabztime.app.core.UiState
import ir.sabztime.app.core.networkMessage
import ir.sabztime.app.data.AuthResponse
import ir.sabztime.app.ui.Routes
import ir.sabztime.app.ui.theme.GlassBackground
import ir.sabztime.app.ui.components.GlassCard
import ir.sabztime.app.ui.components.GradientButton
import ir.sabztime.app.ui.components.IconTile
import ir.sabztime.app.ui.components.SabzLeafIcon
import ir.sabztime.app.ui.components.SegmentedControl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val graph: AppGraph) : ViewModel() {
    // حالت اولیه «آیدیول» (Empty) است؛ Loading فقط در لحظه‌ی ارسال فرم تنظیم می‌شود.
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
                val sabz = e as? SabzException ?: SabzException.Network(networkMessage(e), cause = e)
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
                val sabz = e as? SabzException ?: SabzException.Network(networkMessage(e), cause = e)
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

private val PhoneRegex = Regex("^09\\d{9}$")

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
    var passwordVisible by remember { mutableStateOf(false) }

    // ورود موفق → صفحه اصلی
    LaunchedEffect(state) {
        if (state is UiState.Success<*>) {
            navController.navigate(Routes.MAIN) {
                popUpTo(Routes.LOGIN) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    fun validate(): Boolean {
        phoneError = null; passwordError = null; nameError = null
        if (!PhoneRegex.matches(phone)) phoneError = "شماره موبایل معتبر نیست (مثال: 09123456789)"
        if (password.length < 6) passwordError = "رمز عبور حداقل ۶ کاراکتر"
        if (tab == 1 && name.trim().length < 2) nameError = "نام و نام خانوادگی را وارد کنید"
        return phoneError == null && passwordError == null && nameError == null
    }

    GlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // نشان طلایی برند
            IconTile(
                icon = SabzLeafIcon,
                tileSize = 84.dp,
                contentDescription = null
            )
            Spacer(Modifier.height(16.dp))
            ir.sabztime.app.ui.components.GoldText(
                text = "نوبت‌سبز",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "جستجو، رزرو و مدیریت نوبت — با تجربه‌ای لوکس",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))

            GlassCard(contentPadding = 20.dp) {
                SegmentedControl(
                    options = listOf("ورود", "ثبت‌نام"),
                    selected = tab,
                    onSelect = { tab = it; viewModel.resetError() }
                )
                Spacer(Modifier.height(18.dp))

                if (tab == 1) {
                    GlassTextField(
                        value = name,
                        onValueChange = { name = it; nameError = null },
                        label = "نام و نام خانوادگی",
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        singleLine = true,
                        isError = nameError != null,
                        errorText = nameError
                    )
                    Spacer(Modifier.height(12.dp))
                }

                GlassTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { c -> c.isDigit() }; phoneError = null },
                    label = "شماره موبایل",
                    leadingIcon = { Icon(Icons.Filled.Call, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = phoneError != null,
                    errorText = phoneError
                )
                Spacer(Modifier.height(12.dp))
                GlassTextField(
                    value = password,
                    onValueChange = { password = it; passwordError = null },
                    label = "رمز عبور",
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "پنهان کردن رمز" else "نمایش رمز"
                            )
                        }
                    },
                    isError = passwordError != null,
                    errorText = passwordError
                )

                Spacer(Modifier.height(16.dp))

                when (val s = state) {
                    is UiState.Error -> ErrorBanner(s.message)
                    is UiState.Loading -> Row(
                        Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    }
                    else -> {}
                }

                GradientButton(
                    onClick = {
                        if (validate()) {
                            if (tab == 0) viewModel.login(phone, password)
                            else viewModel.register(phone, password, name.trim())
                        }
                    },
                    enabled = state !is UiState.Loading,
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Text(if (tab == 0) "ورود به نوبت‌سبز" else "ایجاد حساب کاربری", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(10.dp))

                ir.sabztime.app.ui.components.OutlineGoldButton(
                    onClick = { tab = if (tab == 0) 1 else 0; viewModel.resetError() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (tab == 0) "حساب ندارید؟ ثبت‌نام کنید" else "قبلاً ثبت‌نام کرده‌اید؟ وارد شوید",
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { navController.navigate(Routes.HELP) }) {
                Text("درباره سرویس نوبت‌سبز", color = ir.sabztime.app.ui.theme.GoldDim)
            }
            Text(
                "نکته: اطلاعات شما با حساب تلگرام و سیستم رزرو نوبت‌سبز یکپارچه است.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text("نسخه ۱٫۰٫۰", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** نشان‌خطای درون فرم */
@Composable
private fun ErrorBanner(message: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .background(
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                RoundedCornerShape(14.dp)
            )
    ) {
        Text(
            message,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

/** فیلد ورودی شیشه‌ای با برچسب، آیکون و خطا */
@Composable
private fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
    errorText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        isError = isError,
        supportingText = if (errorText != null) ({ Text(errorText) }) else null,
        shape = RoundedCornerShape(18.dp),
        // فقط پارامترهایی که در همه‌ی نسخه‌های M3 1.x وجود دارند (سازگار با 1.2.1)
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.55f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.32f),
            focusedTextColor = ir.sabztime.app.ui.theme.Cocoa,
            unfocusedTextColor = ir.sabztime.app.ui.theme.Cocoa,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = ir.sabztime.app.ui.theme.RingGold.copy(alpha = 0.45f),
            focusedLeadingIconColor = ir.sabztime.app.ui.theme.GoldDim,
            unfocusedLeadingIconColor = ir.sabztime.app.ui.theme.CocoaSoft,
            unfocusedLabelColor = ir.sabztime.app.ui.theme.CocoaSoft,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            errorLabelColor = MaterialTheme.colorScheme.error,
            errorBorderColor = MaterialTheme.colorScheme.error,
            errorSupportingTextColor = MaterialTheme.colorScheme.error
        )
    )
}
