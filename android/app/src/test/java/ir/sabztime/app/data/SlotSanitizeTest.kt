package ir.sabztime.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون «زمان‌ها لود نمی‌شدند»:
 * فهرست تاریخ/زمان باید در برابر پاسخ‌های ناقص یا متفاوت سرور PHP دوام بیاورد.
 */
class SlotSanitizeTest {

    @Test
    fun `dates are deduped sorted and filled from slot_date`() {
        val dates = listOf(
            DateSlot(date = "", slotDate = "2026-09-06", dayName = "جمعه", slotCount = 4),
            DateSlot(date = "2026-09-05", dayName = "پنج‌شنبه", slotCount = 2),
            DateSlot(date = "2026-09-05", dayName = "پنج‌شنبه", slotCount = 2),
            DateSlot(date = "  ")
        )
        val clean = sanitizeDates(dates)
        assertEquals(listOf("2026-09-05", "2026-09-06"), clean.map { it.date })
        assertEquals(4, clean[1].slotCount)
        assertEquals("جمعه", clean[1].dayName)
    }

    @Test
    fun `time date keeps only the date part of a datetime string`() {
        val clean = sanitizeDates(listOf(DateSlot(date = "2026-09-05 00:00:00")))
        assertEquals("2026-09-05", clean.single().date)
    }

    @Test
    fun `missing end time is computed from the business slot duration`() {
        val slots = listOf(TimeSlot(startTime = "09:00"), TimeSlot(startTime = "09:30", endTime = ""))
        val clean = sanitizeTimeSlots(slots, slotDurationMinutes = 45)
        assertEquals(listOf("09:45", "10:15"), clean.map { it.endTime })
    }

    @Test
    fun `times are normalized to HH_MM sorted and unique`() {
        val slots = listOf(
            TimeSlot(startTime = "18:00:00", endTime = "18:30:00"),
            TimeSlot(startTime = "9:30", endTime = "10:00"),
            TimeSlot(startTime = "09:30", endTime = "10:00"),
            TimeSlot(startTime = "نامعتبر", endTime = "11:00"),
            TimeSlot(startTime = "", endTime = "")
        )
        val clean = sanitizeTimeSlots(slots, 30)
        assertEquals(listOf("09:30" to "10:00", "18:00" to "18:30"), clean.map { it.startTime to it.endTime })
    }

    @Test
    fun `effective end time falls back to start plus duration`() {
        assertEquals("10:00", TimeSlot(startTime = "09:30").effectiveEndTime(30))
        assertEquals("23:59", TimeSlot(startTime = "23:50").effectiveEndTime(90))
        assertEquals("", TimeSlot(startTime = "??").effectiveEndTime(30))
    }

    @Test
    fun `slots never lose the whole list because of one bad row`() {
        val slots = listOf(
            TimeSlot(startTime = "10:00", endTime = "10:30"),
            TimeSlot(startTime = "bad-input", endTime = "11:30")
        )
        val clean = sanitizeTimeSlots(slots, 30)
        assertEquals(1, clean.size)
        assertEquals("10:00", clean[0].startTime)
        assertTrue(clean.all { it.endTime.isNotBlank() })
    }
}
