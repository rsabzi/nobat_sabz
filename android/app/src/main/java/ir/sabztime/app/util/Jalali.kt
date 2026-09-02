package ir.sabztime.app.util

import java.time.LocalDate

/**
 * تبدیل تاریخ میلادی ↔ جلالی (هم‌ارز الگوریتم سمت سرور که در api.php استفاده می‌شود)
 * + قالب‌بندی اعداد فارسی.
 */
object Jalali {

    private val jMonths = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private val weekDays = arrayOf(
        "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه"
    )

    fun toJalali(year: Int, month: Int, day: Int): Triple<Int, Int, Int> {
        val gDays = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDays = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        var gy = year - 1600
        val gm = month - 1
        val gd = day - 1
        var gDayNo = 365L * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
        for (i in 0 until gm) gDayNo += gDays[i]
        if (gm > 1 && ((gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0)) gDayNo++
        gDayNo += gd

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053
        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461
        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }
        var i = 0
        while (i < 11 && jDayNo >= jDays[i]) {
            jDayNo -= jDays[i]
            i++
        }
        val jm = i + 1
        val jd = (jDayNo + 1).toInt()
        return Triple(jy.toInt(), jm, jd)
    }

    /** "2025-02-10" → "۱۴۰۳/۱۱/۲۲" */
    fun format(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return "—"
        val parts = isoDate.trim().split("-").mapNotNull { it.take(4).toIntOrNull() }
        if (parts.size < 3) return isoDate
        val (jy, jm, jd) = toJalali(parts[0], parts[1], parts[2])
        return "${persianDigits(jy)}/${persianDigits(jm.toString().padStart(2, '0'))}/${persianDigits(jd.toString().padStart(2, '0'))}"
    }

    /** "2025-02-10" → "۲۲ بهمن ۱۴۰۳" */
    fun formatLong(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return "—"
        val parts = isoDate.trim().split("-").mapNotNull { it.take(4).toIntOrNull() }
        if (parts.size < 3) return isoDate
        val (jy, jm, jd) = toJalali(parts[0], parts[1], parts[2])
        return "${persianDigits(jd)} ${jMonths[jm - 1]} ${persianDigits(jy)}"
    }

    /** "2025-02-10" → Triple(1403, 11, 22) یا null در صورت ورودی نامعتبر */
    fun parts(isoDate: String?): Triple<Int, Int, Int>? {
        if (isoDate.isNullOrBlank()) return null
        val p = isoDate.trim().split("-").mapNotNull { it.take(4).toIntOrNull() }
        if (p.size < 3) return null
        return try {
            toJalali(p[0], p[1], p[2])
        } catch (e: Exception) {
            null
        }
    }

    /** نام ماه جلالی (شماره‌ی ۱ تا ۱۲) */
    fun monthName(monthIndex: Int): String =
        jMonths.getOrElse(monthIndex - 1) { "—" }

    fun weekdayName(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return ""
        return try {
            val d = LocalDate.parse(isoDate.take(10))
            weekDays[(d.dayOfWeek.value % 7)]
        } catch (e: Exception) {
            ""
        }
    }

    private val faDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun persianDigits(input: Any?): String =
        input.toString().map { c -> if (c in '0'..'9') faDigits[c - '0'] else c }.joinToString("")

    fun persianDigits(input: String?): String =
        input?.let { persianDigits(it as Any) } ?: ""

    /** 250000 → "۲۵۰٬۰۰۰ تومان" */
    fun formatPrice(price: Long?, currency: String = "تومان"): String {
        if (price == null || price <= 0) return "رایگان"
        val s = price.toString()
        val sb = StringBuilder()
        for ((i, c) in s.withIndex()) {
            sb.append(c)
            val remaining = s.length - i - 1
            if (remaining > 0 && remaining % 3 == 0) sb.append('٬')
        }
        return "${persianDigits(sb)} $currency"
    }

    fun minutesToPersian(minutes: Int): String {
        if (minutes <= 0) return "همین حالا"
        return "${persianDigits(minutes)} دقیقه"
    }
}
