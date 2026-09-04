package ir.sabztime.app.util

object TextUtil {

    private val IRAN_MOBILE = Regex("^09\\d{9}$")

    fun isValidIranMobile(phone: String): Boolean = IRAN_MOBILE.matches(phone.trim())

    fun cleanMobileInput(input: String): String = input.filter { it.isDigit() || it == '+' }

    fun isValidPassword(password: String): Boolean = password.trim().length >= 6

    /**
     * تبدیل ارقام فارسی (۰-۹) و عربی (٠-٩) به انگلیسی.
     * کیبورد فارسی معمولاً «۰۹۱۲…» می‌فرستد ولی اعتبارسنجی و سرور ارقام انگلیسی می‌فهمند.
     */
    fun normalizeDigits(input: String): String = buildString(input.length) {
        for (c in input) {
            when (c) {
                in '۰'..'۹' -> append('0' + (c - '۰'))
                in '٠'..'٩' -> append('0' + (c - '٠'))
                else -> append(c)
            }
        }
    }

    /** شماره موبایل را نرمال می‌کند (ارقام فارسی→انگلیسی) و اعتبار آن را برمی‌گرداند. */
    fun isValidIranMobileNormalized(phone: String): Boolean =
        isValidIranMobile(normalizeDigits(cleanMobileInput(phone)))
}
