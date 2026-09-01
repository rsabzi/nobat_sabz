package ir.sabztime.app.util

object TextUtil {

    private val IRAN_MOBILE = Regex("^09\\d{9}$")

    fun isValidIranMobile(phone: String): Boolean = IRAN_MOBILE.matches(phone.trim())

    fun cleanMobileInput(input: String): String = input.filter { it.isDigit() || it == '+' }

    fun isValidPassword(password: String): Boolean = password.trim().length >= 6
}
