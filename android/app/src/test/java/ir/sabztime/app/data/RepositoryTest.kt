package ir.sabztime.app.data

import ir.sabztime.app.core.ApiClient
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: SabzRepository

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        repo = SabzRepository(ApiClient(server.url("/").toString(), tokenProvider = { "tok" }))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `search decodes businesses and pagination`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "success": true,
                  "data": [
                    {"id": 1, "name": "آرایشگاه تست", "business_type": "آرایشی و زیبایی",
                     "city": "تهران", "province": "تهران", "services_count": 2,
                     "services": ["کوتاهی مو", "رنگ مو"]}
                  ],
                  "pagination": {"current_page": 1, "per_page": 20, "total": 1,
                                 "total_pages": 1, "has_next": false, "has_prev": false}
                }
                """.trimIndent()
            )
        )
        val res = repo.search(query = "آرایشگاه")
        assertEquals(1, res.data.size)
        assertEquals(2, res.data[0].servicesCount)
        assertEquals(2, res.data[0].services.size)
        assertEquals("آرایشگاه تست", res.data[0].name)
    }

    @Test
    fun `business details decode with services`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                {"success": true, "business": {
                  "id": 2, "name": "کلینیک دندانپزشکی", "business_type": "پزشکی",
                  "slot_duration_minutes": 30, "is_active": 1,
                  "services": [{"id": 3, "name": "معاینه دندان", "price": "100000",
                                "duration_minutes": 30, "is_active": 1}]
                }}
                """.trimIndent()
            )
        )
        val b = repo.businessDetails(2)
        assertNotNull(b)
        assertEquals(30, b!!.slotDurationMinutes)
        assertEquals(100_000L, b.services[0].priceValue)
    }

    @Test
    fun `user bookings decode jalali fields and cancel flag`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                {"status":"success","message":"ok",
                 "bookings":[
                   {"id": 7, "confirmation_code": "ABC123", "booking_date": "2026-09-10",
                    "start_time": "10:00", "end_time": "10:30", "customer_name": "علی",
                    "customer_phone": "09123456789", "status": "confirmed",
                    "business_name": "آرایشگاه", "can_cancel": true,
                    "time_until_booking": 4.5}
                 ],"total_count":1}
                """.trimIndent()
            )
        )
        val list = repo.myBookings()
        assertEquals(1, list.size)
        assertTrue(list[0].canCancel)
        assertEquals("ABC123", list[0].confirmationCode)
    }

    @Test
    fun `profile decodes aggregate`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                {"status":"success",
                 "user": {"app_user_id": 1, "name": "علی", "phone": "09123456789", "telegram_id": -9000000000000000000},
                 "credit": {"current_credit": 10, "total_earned": 10, "total_deducted": 0},
                 "businesses": [{"id": 1, "name": "آرایشگاه", "services_count": 2, "bookings_count": 5}],
                 "marketer": {"id": 1, "marketer_code": "SAMPLE8181", "total_businesses": 2,
                              "total_bookings": 5, "commission_rate": 15, "total_commission": 250},
                 "booking_stats": {"total": 3, "upcoming": 2, "cancelled": 1, "completed": 0}}
                """.trimIndent()
            )
        )
        val p = repo.me()
        assertEquals("علی", p.user.name)
        assertEquals(10, p.credit?.currentCredit)
        assertEquals(1, p.businesses.size)
        assertEquals("SAMPLE8181", p.marketer?.marketerCode)
    }

    @Test
    fun `create booking posts proper payload`() = runTest {
        server.enqueue(
            MockResponse().setBody("""{"status":"success","message":"ok","booking_id":9,"confirmation_code":"XYZ99"}""")
        )
        val resp = repo.createBooking(
            businessId = 1, date = "2026-09-10", startTime = "10:00", endTime = "10:30",
            customerName = "علی", customerPhone = "09123456789", customerNotes = "", userId = -123
        )
        assertEquals(9, resp.bookingId)
        assertEquals("XYZ99", resp.confirmationCode)
        val req = server.takeRequest()
        val body = req.body.readUtf8()
        assertTrue(body.contains("\"action\":\"create_booking\""))
        assertTrue(body.contains("\"business_id\":1"))
    }
}
