package ir.sabztime.app.core

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlinx.serialization.SerializationException

/**
 * مدل خطاهای لایه شبکه/داده.
 * انواع خطا به‌صورت دقیق دسته‌بندی می‌شوند تا کاربر پیام گمراه‌کننده (مثل «اینترنت ندارید»
 * برای یک خطای دیکد) نبیند.
 */
sealed class SabzException(
    val kind: Kind,
    override val message: String,
    val code: String? = null,
    cause: Throwable? = null
) : Exception(message, cause) {

    enum class Kind { NETWORK, SERVER, AUTH, SESSION, UNKNOWN }

    class Network(message: String = MSG_NETWORK, cause: Throwable?) : SabzException(
        Kind.NETWORK, message, cause = cause
    )

    class Server(msg: String, code: String?, httpCode: Int) : SabzException(
        Kind.SERVER, msg.ifBlank { MSG_SERVER }, code
    )

    class Auth(msg: String, code: String? = "AUTH") : SabzException(Kind.AUTH, msg, code)

    class SessionExpired : SabzException(Kind.SESSION, "نشست شما منقضی شده است. لطفاً دوباره وارد شوید.", "AUTH_REQUIRED")

    class Unknown(msg: String, cause: Throwable? = null) : SabzException(Kind.UNKNOWN, msg, cause = cause)

    fun toFriendlyMessage(): String = message

    companion object {
        const val MSG_NETWORK = "اتصال برقرار نشد. اینترنت خود را بررسی کنید و دوباره تلاش کنید."
        const val MSG_TIMEOUT = "سرور پاسخی نداد. اینترنت خود را بررسی کنید و دوباره تلاش کنید."
        const val MSG_NO_HOST = "به سرور نوبت‌سبز دسترسی ممکن نیست. اینترنت یا آدرس سرور را بررسی کنید."
        const val MSG_SSL = "اتصال امن برقرار نشد. دوباره تلاش کنید."
        const val MSG_SERVER = "خطای سرور رخ داد. لطفاً بعداً تلاش کنید."
        const val MSG_DECODE = "ساختار پاسخ سرور قابل خواندن نبود؛ لطفاً دوباره تلاش کنید."
        const val MSG_BAD_RESPONSE = "پاسخ نامعتبر از سرور دریافت شد."
        const val MSG_UNKNOWN = "خطای غیرمنتظره‌ای رخ داد. لطفاً دوباره تلاش کنید."
    }
}

/** پیام مناسب‌برای هر نوع خطای شبکه */
fun networkMessage(cause: Throwable): String = when (cause) {
    is SocketTimeoutException -> SabzException.MSG_TIMEOUT
    is UnknownHostException -> SabzException.MSG_NO_HOST
    is SSLException -> SabzException.MSG_SSL
    else -> SabzException.MSG_NETWORK
}

/**
 * نگاشت هر خطای ممکن به SabzException با دسته‌بندی صحیح:
 *  - خطاهای دیکد/سریالی‌سازی  → SERVER (نه شبکه!)
 *  - خطاهای I/O              → NETWORK با پیام دقیق
 *  - سایر                    → UNKNOWN
 */
fun classify(e: Throwable): SabzException = when (e) {
    is SabzException -> e
    is SerializationException -> SabzException.Server(SabzException.MSG_DECODE, "DECODE_ERROR", 0)
    is IOException -> SabzException.Network(networkMessage(e), cause = e)
    else -> SabzException.Unknown(e.message ?: SabzException.MSG_UNKNOWN, cause = e)
}

/** نتیجه ساده‌شده برای مصرف ViewModel */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String, val kind: SabzException.Kind = SabzException.Kind.UNKNOWN) : UiState<Nothing>

    /** بدون داده یا حالت اولیه/خنثی (مثلاً صفحه ورود قبل از فشردن دکمه). */
    data object Empty : UiState<Nothing>
}

fun <T> Result<T>.toUiState(): UiState<T> = fold(
    onSuccess = { UiState.Success(it) },
    onFailure = { e ->
        val s = classify(e)
        UiState.Error(s.message, s.kind)
    }
)

inline fun <T> runApi(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: Exception) {
    Result.failure(classify(e))
}
