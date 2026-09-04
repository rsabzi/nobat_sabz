package ir.sabztime.app.data

import android.util.Log
import ir.sabztime.app.core.ApiClient
import ir.sabztime.app.core.arr
import ir.sabztime.app.core.classify
import ir.sabztime.app.core.jsonBody
import ir.sabztime.app.core.obj
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * لایه داده: همه تماس‌ها به API موجود سرور (بدون تغییر منطق سمت سرور).
 *
 * دیکد defensive است: اگر ساختار واقعی پاسخ سرور با مدل اندروید تفاوت داشته باشد
 * (مثلاً یک فیلد عدد به‌جای آبجکت)، به‌جای کرش/پیام «ساختار پاسخ قابل خواندن نبود»،
 * مقدار پیش‌فرض سالم برمی‌گردد و جزئیات در لاگ ثبت می‌شود.
 */
class SabzRepository(private val api: ApiClient) {

    private companion object { const val TAG = "SabzRepository" }

    // --- دیکد محافظت‌شده ---

    /** دیکد با فال‌بک: خطای ساختار پاسخ، کرش یا پیام خطای گمراه‌کننده نمی‌سازد. */
    private fun <T : Any> decode(ser: KSerializer<T>, json: JsonObject, fallback: T): T = try {
        api.serializer.decodeFromJsonElement(ser, json)
    } catch (e: Exception) {
        Log.w(TAG, "decode ${ser.descriptor.serialName} failed, using fallback: ${e.message}")
        fallback
    }

    /** دیکد که در شکست null برمی‌گرداند (برای فیلدهای اختیاری). */
    private fun <T : Any> decodeOrNull(ser: KSerializer<T>, json: JsonObject): T? = try {
        api.serializer.decodeFromJsonElement(ser, json)
    } catch (e: Exception) {
        Log.w(TAG, "decode ${ser.descriptor.serialName} failed: ${e.message}")
        null
    }

    // --- جستجو (search_api.php) ---
    suspend fun search(
        query: String = "",
        category: String = "",
        province: String = "",
        sort: String = "relevance",
        page: Int = 1,
        limit: Int = 20
    ): SearchResponse {
        val json = api.get("search_api.php", mapOf(
            "action" to "search",
            "query" to query,
            "category" to category,
            "province" to province,
            "sort" to sort,
            "page" to page.toString(),
            "limit" to limit.toString()
        ))
        return decode(SearchResponse.serializer(), json, SearchResponse())
    }

    suspend fun filters(): FiltersData {
        val json = api.get("search_api.php", mapOf("action" to "filters"))
        val t = decode(SearchResponse.serializer(), json, SearchResponse())
        return t.filters ?: FiltersData()
    }

    suspend fun cities(province: String): List<String> {
        val json = api.get("search_api.php", mapOf("action" to "cities", "province" to province))
        return decode(CitiesResponse.serializer(), json, CitiesResponse()).cities
    }

    suspend fun businessDetails(id: Int): BusinessDetails? {
        val json = api.get("search_api.php", mapOf("action" to "business_details", "id" to id.toString()))
        val business = json.obj("business")
        return business?.let { decodeOrNull(BusinessDetails.serializer(), it) }
    }

    // --- api.php: عمومی ---
    suspend fun business(id: Int): BusinessDetails? {
        val json = api.get("api.php", mapOf("action" to "get_business", "id" to id.toString()))
        val data = json.obj("data")
        return data?.let { decodeOrNull(BusinessDetails.serializer(), it) }
    }

    suspend fun availableDates(businessId: Int): List<DateSlot> {
        val json = api.get("api.php", mapOf(
            "action" to "get_available_dates",
            "id" to businessId.toString()
        ))
        return decode(DatesResponse.serializer(), json, DatesResponse()).data
    }

    suspend fun availableSlots(businessId: Int, date: String): List<TimeSlot> {
        val json = api.get("api.php", mapOf(
            "action" to "get_available_slots",
            "id" to businessId.toString(),
            "date" to date
        ))
        return decode(SlotsResponse.serializer(), json, SlotsResponse()).data
    }

    // --- رزرو ---
    suspend fun createBooking(
        businessId: Int,
        date: String,
        startTime: String,
        endTime: String,
        customerName: String,
        customerPhone: String,
        customerNotes: String,
        userId: Long
    ): BookingCreatedResponse {
        val json = api.post("api.php", body = jsonBody {
            put("action", "create_booking")
            put("business_id", businessId)
            put("date", date)
            put("start_time", startTime)
            put("end_time", endTime)
            put("customer_name", customerName)
            put("customer_phone", customerPhone)
            put("customer_notes", customerNotes)
            put("user_id", userId)
        })
        // عملیات نوشتاری: در شکست دیکد خطای صادقانه نشان بده (نه موفقیت ساختگی)
        return try {
            api.serializer.decodeFromJsonElement(BookingCreatedResponse.serializer(), json)
        } catch (e: Exception) {
            Log.w(TAG, "createBooking decode failed: ${e.message}")
            throw classify(e)
        }
    }

    suspend fun myBookings(includeHistory: Boolean = false): List<BookingSummary> {
        val json = api.post("api.php", body = jsonBody {
            put("action", "get_user_bookings")
            if (includeHistory) put("include_history", true)
        })
        val t = decode(BookingsResponse.serializer(), json, BookingsResponse())
        // هر آیتم جداگانه دیکد می‌شود تا یک ردیف ناسازگار کل لیست را از بین نبرد
        return t.bookings
    }

    suspend fun cancelBooking(bookingId: Int): CancelResponse {
        val json = api.post("api.php", body = jsonBody {
            put("action", "cancel_booking")
            put("booking_id", bookingId)
        })
        return try {
            api.serializer.decodeFromJsonElement(CancelResponse.serializer(), json)
        } catch (e: Exception) {
            Log.w(TAG, "cancelBooking decode failed: ${e.message}")
            throw classify(e)
        }
    }

    // --- صف ---
    suspend fun activeQueue(businessId: Int): QueueResponse {
        val json = api.get("api.php", mapOf(
            "action" to "get_active_queue",
            "business_id" to businessId.toString()
        ))
        return decode(QueueResponse.serializer(), json, QueueResponse())
    }

    suspend fun myQueueInfo(businessId: Int): CustomerQueueInfoResponse {
        val json = api.get("api.php", mapOf(
            "action" to "get_customer_queue_info",
            "business_id" to businessId.toString()
        ))
        return decode(CustomerQueueInfoResponse.serializer(), json, CustomerQueueInfoResponse())
    }

    suspend fun advanceQueue(businessId: Int): QueueActionResponse {
        val json = api.post("api.php", body = jsonBody {
            put("action", "advance_queue")
            put("business_id", businessId)
        })
        return decode(QueueActionResponse.serializer(), json, QueueActionResponse())
    }

    suspend fun completeBooking(businessId: Int, bookingId: Int): QueueActionResponse {
        val json = api.post("api.php", body = jsonBody {
            put("action", "complete_booking")
            put("business_id", businessId)
            put("booking_id", bookingId)
        })
        return decode(QueueActionResponse.serializer(), json, QueueActionResponse())
    }

    // --- مالک ---
    suspend fun myBusinesses(): List<MyBusinessItem> {
        val json = api.get("api.php", mapOf("action" to "get_my_businesses"))
        return decode(MyBusinessesResponse.serializer(), json, MyBusinessesResponse()).businesses
    }

    /** ثبت کسب‌وکار جدید توسط مالک (اندپوینت add_business بک‌اند). */
    suspend fun addBusiness(
        name: String,
        type: String,
        phone: String,
        province: String,
        city: String,
        address: String,
        marketerCode: String = ""
    ): BusinessCreatedResponse {
        val json = api.post("api.php", body = jsonBody {
            put("action", "add_business")
            put("name", name)
            put("type", type)
            put("phone", phone)
            put("province", province)
            put("city", city)
            put("address", address)
            put("marketer_code", marketerCode)
        })
        return try {
            api.serializer.decodeFromJsonElement(BusinessCreatedResponse.serializer(), json)
        } catch (e: Exception) {
            Log.w(TAG, "addBusiness decode failed: ${e.message}")
            throw classify(e)
        }
    }

    /** جایگزینی کامل فهرست خدمات یک کسب‌وکار (اندپوینت update_services بک‌اند). */
    suspend fun updateServices(businessId: Int, services: List<Service>): List<Service> {
        val json = api.post("api.php", body = jsonBody {
            put("action", "update_services")
            put("business_id", businessId)
            putArray(
                "services",
                JsonArray(services.map { s ->
                    buildJsonObject {
                        put("id", s.id)
                        put("name", s.name)
                        put("description", s.description ?: "")
                        put("price", s.priceValue ?: 0)
                        put("duration_minutes", s.durationMinutes ?: 30)
                        put("is_active", s.isActive)
                    }
                })
            )
        })
        return try {
            api.serializer.decodeFromJsonElement(ManagedServicesResponse.serializer(), json).data?.services.orEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "updateServices decode failed: ${e.message}")
            throw classify(e)
        }
    }

    /** ویرایش اطلاعات پایهٔ کسب‌وکار (اندپوینت update_business بک‌اند). */
    suspend fun updateBusiness(
        businessId: Int,
        name: String,
        type: String,
        phone: String,
        province: String,
        city: String,
        address: String
    ) {
        api.post("api.php", body = jsonBody {
            put("action", "update_business")
            put("business_id", businessId)
            put("name", name)
            put("type", type)
            put("phone", phone)
            put("province", province)
            put("city", city)
            put("address", address)
        })
    }

    /** حذف کامل کسب‌وکار (اندپوینت delete_business بک‌اند). */
    suspend fun deleteBusiness(businessId: Int): DeleteBusinessResponse {
        val json = api.post("api.php", body = jsonBody {
            put("action", "delete_business")
            put("business_id", businessId)
        })
        return decode(DeleteBusinessResponse.serializer(), json, DeleteBusinessResponse())
    }

    /** ذخیرهٔ برنامهٔ کاری هفتگی (اندپوینت update_schedule بک‌اند). */
    suspend fun saveSchedule(businessId: Int, slotDurationMinutes: Int, days: List<DaySchedule>) {
        api.post("api.php", body = jsonBody {
            put("action", "update_schedule")
            put("business_id", businessId)
            put("slot_duration_minutes", slotDurationMinutes)
            putArray(
                "schedules",
                JsonArray(days.map { d ->
                    buildJsonObject {
                        put("day_of_week", d.dayOfWeek)
                        put("start_time", d.startTime ?: "")
                        put("end_time", d.endTime ?: "")
                        put("is_active", d.isActive == 1)
                    }
                })
            )
        })
    }

    /** برنامهٔ کاری هفتگی فعلی (اندپوینت get_schedule بک‌اند). */
    suspend fun getSchedule(businessId: Int): ScheduleData {
        val json = api.get("api.php", mapOf(
            "action" to "get_schedule",
            "business_id" to businessId.toString()
        ))
        // پاسخ سرور داده‌ها را زیر کلید «data» می‌فرستد
        val data = json.obj("data") ?: json
        return decode(ScheduleData.serializer(), data, ScheduleData())
    }

    /** لیست رزروهای کسب‌وکار برای مالک (اندپوینت get_business_bookings بک‌اند). */
    suspend fun businessBookings(businessId: Int, showAll: Boolean): BusinessBookingsResponse {
        val json = api.get("api.php", mapOf(
            "action" to "get_business_bookings",
            "business_id" to businessId.toString(),
            "show_all" to (if (showAll) "1" else "0")
        ))
        return decode(BusinessBookingsResponse.serializer(), json, BusinessBookingsResponse())
    }

    /** ثبت حضور/عدم‌حضور یک رزرو (اندپوینت record_attendance بک‌اند). */
    suspend fun recordAttendance(businessId: Int, bookingId: Int, attendanceStatus: String): AttendanceResponse {
        val json = api.post("api.php", body = jsonBody {
            put("action", "record_attendance")
            put("business_id", businessId)
            put("booking_id", bookingId)
            put("attendance_status", attendanceStatus)
        })
        return decode(AttendanceResponse.serializer(), json, AttendanceResponse())
    }

    /** ذخیرهٔ نامک لینک اختصاصی (اندپوینت update_link_alias بک‌اند). */
    suspend fun updateLinkAlias(businessId: Int, alias: String): LinkAliasResponse {
        val json = api.post("api.php", body = jsonBody {
            put("action", "update_link_alias")
            put("business_id", businessId)
            put("link_alias", alias)
        })
        return decode(LinkAliasResponse.serializer(), json, LinkAliasResponse())
    }

    /** اطلاعات کسب‌وکار برای فرم ویرایش (اندپوینت get_business_for_edit بک‌اند). */
    suspend fun businessForEdit(businessId: Int): BusinessDetails? {
        val json = api.get("api.php", mapOf(
            "action" to "get_business_for_edit",
            "business_id" to businessId.toString()
        ))
        return json.obj("data")?.let { decodeOrNull(BusinessDetails.serializer(), it) }
    }

    suspend fun subscriptionStatus(businessId: Int): SubscriptionStatusResponse {
        val json = api.post("api.php", body = jsonBody {
            put("action", "get_subscription_status")
            put("business_id", businessId)
        })
        return decode(SubscriptionStatusResponse.serializer(), json, SubscriptionStatusResponse())
    }

    suspend fun subscriptionPlans(): List<Plan> {
        val json = api.post("api.php", body = jsonBody { put("action", "get_subscription_plans") })
        return decode(PlansResponse.serializer(), json, PlansResponse()).plans
    }

    // --- بازاریاب ---
    suspend fun marketerDashboard(): MarketerDashboardResponse {
        val json = api.post("api.php", body = jsonBody { put("action", "get_marketer_dashboard") })
        return decode(MarketerDashboardResponse.serializer(), json, MarketerDashboardResponse())
    }

    /**
     * فعال‌سازی حساب بازاریابی — هم‌منطق ربات (user_type_marketer):
     * اگر کاربر بازاریاب نباشد، رکورد جدید با کد یکتا ساخته می‌شود.
     */
    suspend fun registerMarketer(): MarketerRegistrationResponse {
        val json = api.post("api.php", body = jsonBody { put("action", "register_marketer") })
        return try {
            api.serializer.decodeFromJsonElement(MarketerRegistrationResponse.serializer(), json)
        } catch (e: Exception) {
            Log.w(TAG, "registerMarketer decode failed: ${e.message}")
            throw classify(e)
        }
    }

    // --- احراز هویت و پروفایل (mobile_auth.php) ---
    suspend fun register(phone: String, password: String, name: String, device: String): AuthResponse {
        val json = api.post("mobile_auth.php", body = jsonBody {
            put("action", "register")
            put("phone", phone)
            put("password", password)
            put("name", name)
            put("device", device)
        })
        return decode(AuthResponse.serializer(), json, AuthResponse())
    }

    suspend fun login(phone: String, password: String, device: String): AuthResponse {
        val json = api.post("mobile_auth.php", body = jsonBody {
            put("action", "login")
            put("phone", phone)
            put("password", password)
            put("device", device)
        })
        return decode(AuthResponse.serializer(), json, AuthResponse())
    }

    suspend fun logout() {
        api.post("mobile_auth.php", body = jsonBody { put("action", "logout") })
    }

    /**
     * حذف کامل حساب و داده‌ها (الزام گوگل‌پلی/کافه‌بازار).
     * سرور همه‌ی داده‌های وابسته را در یک transaction پاک می‌کند؛
     * اگر خطا برگردد، ApiClient استثنا پرتاب می‌کند و فراخواننده به کاربر پیام می‌دهد.
     */
    suspend fun deleteAccount() {
        api.post("mobile_auth.php", body = jsonBody { put("action", "delete_account") })
    }

    /**
     * پروفایل کاربر. هر بخش جداگانه و با محافظت دیکد می‌شود تا شکست در
     * ساختار هر فیلد (حتی اگر نسخه‌ی سرور متفاوت باشد) کل صفحه را نبرَد.
     */
    suspend fun me(): ProfileData {
        val raw = api.post("mobile_auth.php", body = jsonBody { put("action", "me") })
        // سازگاری با نسخه‌های سرور: بعضی نسخه‌ها داده‌ها را صاف می‌فرستند و بعضی زیر کلید «data»
        val json = if (raw.obj("user") == null && raw.obj("data") != null) raw.obj("data")!! else raw
        return ProfileData(
            user = json.obj("user")?.let { decode(UserInfo.serializer(), it, UserInfo()) } ?: UserInfo(),
            credit = json.obj("credit")?.let { decodeOrNull(CreditInfo.serializer(), it) },
            businesses = json.arr("businesses")
                ?.mapNotNull { el -> (el as? JsonObject)?.let { decodeOrNull(MyBusinessItem.serializer(), it) } }
                ?: emptyList(),
            marketer = json.obj("marketer")?.let { decodeOrNull(MarketerInfo.serializer(), it) },
            bookingStats = json.obj("booking_stats")?.let { decode(BookingStats.serializer(), it, BookingStats()) } ?: BookingStats()
        )
    }

    suspend fun updateProfile(name: String, phone: String): ProfileData {
        val json = api.post("mobile_auth.php", body = jsonBody {
            put("action", "update_profile")
            put("name", name)
            put("phone", phone)
        })
        val user = json.obj("user")?.let { decodeOrNull(UserInfo.serializer(), it) }
        return ProfileData(user = user ?: UserInfo(name = name, phone = phone))
    }

    suspend fun notifications(): List<NotificationItem> {
        val json = api.post("mobile_auth.php", body = jsonBody { put("action", "get_notifications") })
        return decode(NotificationsResponse.serializer(), json, NotificationsResponse()).notifications
    }
}
