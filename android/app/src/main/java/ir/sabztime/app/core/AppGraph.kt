package ir.sabztime.app.core

import android.content.Context
import android.os.Build
import ir.sabztime.app.BuildConfig
import ir.sabztime.app.data.SabzRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking

/** وضعیت نشست کاربر */
data class SessionState(val loggedIn: Boolean = false, val hasError: Boolean = false)

/** وابستگی‌های سراسری برنامه (DI ساده و بدون کتابخانه) */
class AppGraph(context: Context) {

    val appContext: Context = context.applicationContext
    private val secureStore = SecureStore(context)

    private val _session = MutableStateFlow(SessionState())
    val session: StateFlow<SessionState> = _session.asStateFlow()

    val tokenProvider: () -> String? = {
        runBlocking { secureStore.token() }
    }

    val api = ApiClient(
        baseUrl = BuildConfig.API_BASE_URL,
        tokenProvider = tokenProvider
    )

    val repository = SabzRepository(api)

    val deviceName: String
        get() = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

    /** بعد از ورود موفق / خروج، وضعیت نشست را تازه‌سازی می‌کند */
    fun markSessionState(loggedIn: Boolean) {
        _session.value = SessionState(loggedIn = loggedIn)
    }

    /** در شروع برنامه بررسی می‌کند که توکن معتبری در کش وجود دارد یا نه */
    suspend fun restoreSession(): Boolean {
        val token = secureStore.token()
        _session.value = SessionState(loggedIn = token != null)
        return token != null
    }

    suspend fun saveToken(token: String) = secureStore.saveToken(token)

    suspend fun clearSession() {
        secureStore.clear()
        _session.value = SessionState(loggedIn = false)
    }
}
