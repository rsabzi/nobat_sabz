package ir.sabztime.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JalaliTest {

    @Test
    fun `new year 1404 starts at 2025-03-21`() {
        val (y, m, d) = Jalali.toJalali(2025, 3, 21)
        assertEquals(1404, y)
        assertEquals(1, m)
        assertEquals(1, d)
    }

    @Test
    fun `new year 1403 starts at 2024-03-20`() {
        val (y, m, d) = Jalali.toJalali(2024, 3, 20)
        assertEquals(1403, y)
        assertEquals(1, m)
        assertEquals(1, d)
    }

    @Test
    fun `2025-02-10 is 1403-11-22 (bahman 22, 1403)`() {
        val (y, m, d) = Jalali.toJalali(2025, 2, 10)
        assertEquals(1403, y)
        assertEquals(11, m)
        assertEquals(22, d)
    }

    @Test
    fun `roundtrip last day of leap year`() {
        // 1403 کبیسه است؛ 1403/12/30 = 2025-03-20
        val (y, m, d) = Jalali.toJalali(2025, 3, 20)
        assertEquals(1403, y)
        assertEquals(12, m)
        assertEquals(30, d)
    }

    @Test
    fun `persian digits conversion`() {
        assertEquals("۱۴۰۴", Jalali.persianDigits("1404"))
        assertEquals("۰۹۱۲۳۴۵۶۷۸۹", Jalali.persianDigits("09123456789"))
    }

    @Test
    fun `price formatting`() {
        assertEquals("۲۵۰٬۰۰۰ تومان", Jalali.formatPrice(250_000))
        assertEquals("رایگان", Jalali.formatPrice(0))
        assertEquals("رایگان", Jalali.formatPrice(null))
    }

    @Test
    fun `month name lookup`() {
        assertEquals("۲۲ بهمن ۱۴۰۳", Jalali.formatLong("2025-02-10"))
    }

    @Test
    fun `phone validation`() {
        assertTrue(TextUtil.isValidIranMobile("09123456789"))
        assertFalse(TextUtil.isValidIranMobile("0912345678"))
        assertFalse(TextUtil.isValidIranMobile("02123456789"))
        assertFalse(TextUtil.isValidIranMobile(""))
    }

    @Test
    fun `password validation`() {
        assertTrue(TextUtil.isValidPassword("123456"))
        assertFalse(TextUtil.isValidPassword("12345"))
    }
}
