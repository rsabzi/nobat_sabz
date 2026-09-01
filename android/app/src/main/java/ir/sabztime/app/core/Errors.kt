package ir.sabztime.app.core

import java.io.IOException

/**
 * مدل خطاهای لایه شبکه/داده.
 * انواع خطا به فارسی و قابل‌نمایش در UI نگاشت می‌شوند.
 */
sealed class SabzException(
    val kind: Kind,
    override val message: String,
    val code: String? = null,
    cause: Throwable? = null
) : Exception(message, cause) {

    enum class Kind { NETWORK, SERVER, AUTH, SESSION, UNKNOWN }

    class Network(cause: Throwable) : SabzException(
        Kind.NETWORK,
        "اتصال برقرار نشد. اینترنت خود را بررسی کنید و دوباره تلاش کنید.",
        cause = cause
    )

    class Server(msg: String, code: String?, httpCode: Int) : SabzException(
        Kind.SERVER,
        msg.ifBlank { "خطای سرور رخ داد. لطفاً بعداً تلاش کنید." },
        code
    )

    class Auth(msg: String, code: String? = "AUTH") : SabzException(Kind.AUTH, msg, code)

    class SessionExpired : SabzException(Kind.SESSION, "نشست شما منقضی شده است. لطفاً دوباره وارد شوید.", "AUTH_REQUIRED")

    fun toFriendlyMessage(): String = message
}

/** نتیجه ساده‌شده برای مصرف ViewModel */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String, val kind: SabzException.Kind = SabzException.Kind.UNKNOWN) : UiState<Nothing>
    data object Empty : UiState<Nothing>

    /** حالت اولیه/خنثی: هنوز درخواستی ارسال نشده (مثلاً صفحه ورود قبل از فشردن دکمه). */
    data object Idle : UiState<Nothing>
}

fun <T> Result<T>.toUiState(): UiState<T> = fold(
    onSuccess = { UiState.Success(it) },
    onFailure = { e ->
        when (e) {
            is SabzException -> UiState.Error(e.toFriendlyMessage(), e.kind)
            is IOException -> UiState.Error(SabzException.Network(e).message, SabzException.Kind.NETWORK)
            else -> UiState.Error(e.message ?: "خطای ناشناخته", SabzException.Kind.UNKNOWN)
        }
    }
)

inline fun <T> runApi(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: Exception) {
    if (e is SabzException) Result.failure(e)
    else Result.failure(SabzException.Network(e))
}
