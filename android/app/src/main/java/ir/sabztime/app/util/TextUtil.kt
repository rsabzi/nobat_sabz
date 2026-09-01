package ir.sabztime.app.util

object TextUtil {

    /** Converts Persian and Arabic-Indic numerals to ASCII digits. */
    fun latinDigits(input: String): String = buildString(input.length) {
        input.forEach { char ->
            append(
                when (char) {
                    in '۰'..'۹' -> '0' + (char - '۰')
                    in '٠'..'٩' -> '0' + (char - '٠')
                    else -> char
                }
            )
        }
    }

    /** Normalizes the mobile formats accepted by the server to 09xxxxxxxxx. */
    fun normalizeMobile(input: String): String? {
        val digits = latinDigits(input).filter { it in '0'..'9' }
        val normalized = when {
            digits.length == 11 && digits.startsWith("09") -> digits
            digits.length == 10 && digits.startsWith("9") -> "0$digits"
            digits.length == 12 && digits.startsWith("98") -> "0${digits.substring(2)}"
            else -> return null
        }
        return normalized.takeIf { it.length == 11 && it.startsWith("09") }
    }

    fun isValidIranMobile(phone: String): Boolean = normalizeMobile(phone) != null

    fun cleanMobileInput(input: String): String =
        latinDigits(input).filter { it.isDigit() || it == '+' }

    fun isValidPassword(password: String): Boolean = password.trim().length >= 6
}
