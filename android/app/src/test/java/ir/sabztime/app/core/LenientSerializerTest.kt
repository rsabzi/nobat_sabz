package ir.sabztime.app.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

@Serializable
private data class LenientSample(
    @Serializable(with = LenientInt::class) val a: Int = 0,
    @Serializable(with = LenientLong::class) val b: Long = 0L,
    @Serializable(with = LenientDouble::class) val c: Double = 0.0,
    val phone: String = "",
    val date: String = ""
)

class LenientSerializerTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `accepts json numbers`() {
        val s = json.decodeFromString(LenientSample.serializer(), """{"a": 12, "b": 34, "c": 1.5}""")
        assertEquals(12, s.a)
        assertEquals(34L, s.b)
        assertEquals(1.5, s.c, 0.0001)
    }

    @Test
    fun `accepts numeric strings from PDO`() {
        val s = json.decodeFromString(LenientSample.serializer(), """{"a": "12", "b": "34", "c": "1.5"}""")
        assertEquals(12, s.a)
        assertEquals(34L, s.b)
        assertEquals(1.5, s.c, 0.0001)
    }

    @Test
    fun `falls back to zero for junk values`() {
        val s = json.decodeFromString(LenientSample.serializer(), """{"a": "abc", "b": null, "c": "x"}""")
        assertEquals(0, s.a)
        assertEquals(0L, s.b)
        assertEquals(0.0, s.c, 0.0001)
    }

    @Test
    fun `does not corrupt string fields that look numeric`() {
        // شماره تلفن و تاریخ باید بدون تغییر باقی بمانند
        val s = json.decodeFromString(
            LenientSample.serializer(),
            """{"a": 1, "phone": "09123456789", "date": "2026-09-02"}"""
        )
        assertEquals("09123456789", s.phone)
        assertEquals("2026-09-02", s.date)
    }
}
