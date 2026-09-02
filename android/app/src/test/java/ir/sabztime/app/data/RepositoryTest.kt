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
                 "businesses": [{"id": 1, "name": "آرایشگاه", "services_count": 2, "bookings_count": 5},
                                {"id": 2, "name": "کلینیک دندانپزشکی", "services_count": 1, "bookings_count": 3}],
                 "marketer": {"id": 1, "marketer_code": "SAMPLE8181", "total_businesses": 2,
                              "total_bookings": 5, "commission_rate": 15, "total_commission": 250},
                 "booking_stats": {"total": 3, "upcoming": 2, "cancelled": 1, "completed": 0}}
                """.trimIndent()
            )
        )
        val p = repo.me()
        assertEquals("علی", p.user.name)
        assertEquals(10, p.credit?.currentCredit)
        assertEquals(2, p.businesses.size)
        assertEquals("SAMPLE8181", p.marketer?.marketerCode)
    }

    @Test
    fun `profile decodes when PDO returns numbers as strings`() = runTest {
        // PDO با emulated prepares همه ستون‌های عددی را به‌صورت رشته می‌فرستد؛
        // این حالت قبلاً باعث شکست دیکد صفحه پروفایل (و پیام گمراه‌کننده «اینترنت») می‌شد.
        server.enqueue(
            MockResponse().setBody(
                """
                {"status":"success",
                 "user": {"app_user_id": "1", "name": "علی", "phone": "09123456789", "telegram_id": "-9000000000000000001"},
                 "credit": {"current_credit": "10", "total_earned": "10", "total_deducted": "0",
                            "last_updated": "2026-09-01 12:00:00"},
                 "businesses": [{"id": "1", "name": "آرایشگاه", "is_active": "1", "services_count": "2", "bookings_count": "5"}],
                 "marketer": null,
                 "booking_stats": {"total": "3", "upcoming": "2", "cancelled": "1", "completed": "0"}}
                """.trimIndent()
            )
        )
        val p = repo.me()
        assertEquals("علی", p.user.name)
        assertEquals(-9000000000000000001L, p.user.telegramId)
        assertEquals(10, p.credit?.currentCredit)
        assertEquals(10, p.credit?.totalEarned)
        assertEquals(0, p.credit?.totalDeducted)
        assertEquals(5, p.businesses[0].bookingsCount)
        assertEquals(3, p.bookingStats.total)
    }

    @Test
    fun `profile tolerates unexpected production shapes`() = runTest {
        // اگر پاسخ واقعی سرور با مدل اندروید تفاوت ساختاری داشته باشد (مثلاً credit عدد باشد،
        // businesses آبجکت به‌جای آرای، marketer رشته)، دیکدِ کل‌جا قبلاً صفحه پروفایل را با
        // «ساختار پاسخ سرور قابل خواندن نبود» کرش می‌داد. حالا هر بخش جداگانه و امن دیکد می‌شود.
        server.enqueue(
            MockResponse().setBody(
                """
                {"status":"success",
                 "user": {"app_user_id": 5, "name": "سارا", "phone": "09120000000"},
                 "credit": 10,
                 "businesses": {"0": {"id": "1", "name": "آرایشگاه"}},
                 "marketer": "inactive",
                 "booking_stats": {"total": "3"}}
                """
            )
        )
        val p = repo.me()
        assertEquals("سارا", p.user.name)
        org.junit.Assert.assertNull(p.credit)
        assertEquals(0, p.businesses.size)
        org.junit.Assert.assertNull(p.marketer)
        assertEquals(3, p.bookingStats.total)
        assertEquals(0, p.bookingStats.completed)
    }

    @Test
    fun `profile tolerates data-wrapped response`() = runTest {
        // بعضی نسخه‌های API داده‌ها را زیر کلید «data» می‌فرستند؛ پروفایل باید از هر دو شکل بخواند.
        server.enqueue(
            MockResponse().setBody(
                """
                {"status":"success","data":
                 {"user":{"app_user_id":7,"name":"مهدی","phone":"09121111111"},
                  "credit":{"current_credit":"20"},
                  "businesses":[],
                  "booking_stats":{"total":"1","upcoming":"1","cancelled":"0","completed":"0"}}}
                """
            )
        )
        val p = repo.me()
        assertEquals("مهدی", p.user.name)
        assertEquals(20, p.credit?.currentCredit)
        assertEquals(1, p.bookingStats.total)
    }

    @Test
    fun `bookings decode when ids are strings from PDO`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                {"status":"success","message":"ok",
                 "bookings":[
                   {"id": "7", "business_id": "2", "confirmation_code": "ABC123",
                    "booking_date": "2026-09-10", "start_time": "10:00", "end_time": "10:30",
                    "customer_name": "علی", "customer_phone": "09123456789", "status": "confirmed",
                    "business_name": "آرایشگاه", "can_cancel": true, "time_until_booking": "4.5"}
                 ],"total_count":"1"}
                """.trimIndent()
            )
        )
        val list = repo.myBookings()
        assertEquals(1, list.size)
        assertEquals(7, list[0].id)
        assertEquals(2, list[0].businessId)
        assertEquals(4.5, list[0].timeUntilBooking, 0.001)
    }

    @Test
    fun `decimal price string parses to long`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                {"success": true, "business": {
                  "id": 2, "name": "آرایشگاه", "slot_duration_minutes": "30", "is_active": "1",
                  "services": [
                    {"id": "3", "name": "کوتاهی", "price": "250000.00", "duration_minutes": "30", "is_active": "1"},
                    {"id": "4", "name": "رنگ مو", "price": "1200000.50", "duration_minutes": "120", "is_active": "1"}
                  ]
                }}
                """.trimIndent()
            )
        )
        val b = repo.businessDetails(2)
        assertNotNull(b)
        assertEquals(30, b!!.slotDurationMinutes)
        assertEquals(250_000L, b.services[0].priceValue)
        assertEquals(1_200_000L, b.services[1].priceValue)
    }

    @Test
    fun `add business posts owner setup fields and returns generated id`() = runTest {
        server.enqueue(MockResponse().setBody("""{"status":"success","business_id":42}"""))
        val result = repo.addBusiness("آرایشگاه نمونه", "آرایشی", "09123456789", "تهران", "تهران", "خیابان نمونه")
        assertEquals(42, result.businessId)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"action\":\"add_business\""))
        assertTrue(body.contains("\"name\":\"آرایشگاه نمونه\""))
        assertTrue(body.contains("\"phone\":\"09123456789\""))
    }

    @Test
    fun `update services posts full services array and decodes result`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"status":"success","data":{"services":[{"id":1,"name":"برش","price":"250000.00","duration_minutes":30,"is_active":1}]}}"""
            )
        )
        val list = repo.updateServices(
            7,
            listOf(Service(id = 0, name = "برش", price = "250000", durationMinutes = 30))
        )
        assertEquals(1, list.size)
        assertEquals("برش", list[0].name)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"action\":\"update_services\""))
        assertTrue(body.contains("\"business_id\":7"))
        assertTrue(body.contains("\"name\":\"برش\""))
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
