package ir.sabztime.app.data

import ir.sabztime.app.core.ApiClient
import ir.sabztime.app.core.jsonBody
import ir.sabztime.app.core.obj
import kotlinx.serialization.json.put

/**
 * لایه داده: همه تماس‌ها به API موجود سرور (بدون تغییر منطق سمت سرور).
 */
class SabzRepository(private val api: ApiClient) {

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
        return api.serializer.decodeFromJsonElement(SearchResponse.serializer(), json)
    }

    suspend fun filters(): FiltersData {
        val json = api.get("search_api.php", mapOf("action" to "filters"))
        val t = api.serializer.decodeFromJsonElement(SearchResponse.serializer(), json)
        return t.filters ?: FiltersData()
    }

    suspend fun cities(province: String): List<String> {
        val json = api.get("search_api.php", mapOf("action" to "cities", "province" to province))
        return api.serializer.decodeFromJsonElement(CitiesResponse.serializer(), json).cities
    }

    suspend fun businessDetails(id: Int): BusinessDetails? {
        val json = api.get("search_api.php", mapOf("action" to "business_details", "id" to id.toString()))
        val root = json
        val business = root.obj("business")
        return business?.let { api.serializer.decodeFromJsonElement(BusinessDetails.serializer(), it) }
    }

    // --- api.php: عمومی ---
    suspend fun business(id: Int): BusinessDetails? {
        val json = api.get("api.php", mapOf("action" to "get_business", "id" to id.toString()))
        val data = json.obj("data")
        return data?.let { api.serializer.decodeFromJsonElement(BusinessDetails.serializer(), it) }
    }

    suspend fun availableDates(businessId: Int): List<DateSlot> {
        val json = api.get("api.php", mapOf(
            "action" to "get_available_dates",
            "id" to businessId.toString()
        ))
        return api.serializer.decodeFromJsonElement(DatesResponse.serializer(), json).data
    }

    suspend fun availableSlots(businessId: Int, date: String): List<TimeSlot> {
        val json = api.get("api.php", mapOf(
            "action" to "get_available_slots",
            "id" to businessId.toString(),
            "date" to date
        ))
        return api.serializer.decodeFromJsonElement(SlotsResponse.serializer(), json).data
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
        return api.serializer.decodeFromJsonElement(BookingCreatedResponse.serializer(), json)
    }

    suspend fun myBookings(includeHistory: Boolean = false): List<BookingSummary> {
        val json = api.post("api.php", body = jsonBody {
            put("action", "get_user_bookings")
            if (includeHistory) put("include_history", true)
        })
        return api.serializer.decodeFromJsonElement(BookingsResponse.serializer(), json).bookings
    }

    suspend fun cancelBooking(bookingId: Int): CancelResponse {
        val json = api.post("api.php", body = jsonBody {
            put("action", "cancel_booking")
            put("booking_id", bookingId)
        })
        return api.serializer.decodeFromJsonElement(CancelResponse.serializer(), json)
    }

    // --- صف ---
    suspend fun activeQueue(businessId: Int): QueueResponse {
        val json = api.get("api.php", mapOf(
            "action" to "get_active_queue",
            "business_id" to businessId.toString()
        ))
        return api.serializer.decodeFromJsonElement(QueueResponse.serializer(), json)
    }

    suspend fun myQueueInfo(businessId: Int): CustomerQueueInfoResponse {
        val json = api.get("api.php", mapOf(
            "action" to "get_customer_queue_info",
            "business_id" to businessId.toString()
        ))
        return api.serializer.decodeFromJsonElement(CustomerQueueInfoResponse.serializer(), json)
    }

    suspend fun advanceQueue(businessId: Int): QueueActionResponse {
        val json = api.post("api.php", body = jsonBody {
            put("action", "advance_queue")
            put("business_id", businessId)
        })
        return api.serializer.decodeFromJsonElement(QueueActionResponse.serializer(), json)
    }

    suspend fun completeBooking(businessId: Int, bookingId: Int): QueueActionResponse {
        val json = api.post("api.php", body = jsonBody {
            put("action", "complete_booking")
            put("business_id", businessId)
            put("booking_id", bookingId)
        })
        return api.serializer.decodeFromJsonElement(QueueActionResponse.serializer(), json)
    }

    // --- مالک ---
    suspend fun myBusinesses(): List<MyBusinessItem> {
        val json = api.get("api.php", mapOf("action" to "get_my_businesses"))
        return api.serializer.decodeFromJsonElement(MyBusinessesResponse.serializer(), json).businesses
    }

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
        return api.serializer.decodeFromJsonElement(BusinessCreatedResponse.serializer(), json)
    }

    suspend fun updateServices(businessId: Int, services: List<Service>): List<Service> {
        val json = api.post("api.php", body = jsonBody {
            put("action", "update_services")
            put("business_id", businessId)
            putArray("services", kotlinx.serialization.json.JsonArray(services.map { service ->
                kotlinx.serialization.json.buildJsonObject {
                    put("id", service.id)
                    put("name", service.name)
                    put("description", service.description ?: "")
                    put("price", service.priceValue ?: 0)
                    put("duration_minutes", service.durationMinutes ?: 30)
                    put("is_active", service.isActive)
                }
            }))
        })
        return api.serializer.decodeFromJsonElement(ManagedServicesResponse.serializer(), json).data?.services.orEmpty()
    }

    suspend fun subscriptionStatus(businessId: Int): SubscriptionStatusResponse {
        val json = api.post("api.php", body = jsonBody {
            put("action", "get_subscription_status")
            put("business_id", businessId)
        })
        return api.serializer.decodeFromJsonElement(SubscriptionStatusResponse.serializer(), json)
    }

    suspend fun subscriptionPlans(): List<Plan> {
        val json = api.post("api.php", body = jsonBody { put("action", "get_subscription_plans") })
        return api.serializer.decodeFromJsonElement(PlansResponse.serializer(), json).plans
    }

    // --- بازاریاب ---
    suspend fun marketerDashboard(): MarketerDashboardResponse {
        val json = api.post("api.php", body = jsonBody { put("action", "get_marketer_dashboard") })
        return api.serializer.decodeFromJsonElement(MarketerDashboardResponse.serializer(), json)
    }

    suspend fun registerMarketer(): MarketerRegistrationResponse {
        val json = api.post("api.php", body = jsonBody { put("action", "register_marketer") })
        return api.serializer.decodeFromJsonElement(MarketerRegistrationResponse.serializer(), json)
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
        return api.serializer.decodeFromJsonElement(AuthResponse.serializer(), json)
    }

    suspend fun login(phone: String, password: String, device: String): AuthResponse {
        val json = api.post("mobile_auth.php", body = jsonBody {
            put("action", "login")
            put("phone", phone)
            put("password", password)
            put("device", device)
        })
        return api.serializer.decodeFromJsonElement(AuthResponse.serializer(), json)
    }

    suspend fun logout() {
        api.post("mobile_auth.php", body = jsonBody { put("action", "logout") })
    }

    suspend fun me(): ProfileData {
        val json = api.post("mobile_auth.php", body = jsonBody { put("action", "me") })
        return api.serializer.decodeFromJsonElement(ProfileResponse.serializer(), json).let {
            ProfileData(
                user = it.user ?: UserInfo(),
                credit = it.credit,
                businesses = it.businesses ?: emptyList(),
                marketer = it.marketer,
                bookingStats = it.bookingStats ?: BookingStats()
            )
        }
    }

    suspend fun updateProfile(name: String, phone: String): ProfileData {
        val json = api.post("mobile_auth.php", body = jsonBody {
            put("action", "update_profile")
            put("name", name)
            put("phone", phone)
        })
        return api.serializer.decodeFromJsonElement(ProfileResponse.serializer(), json).let {
            ProfileData(user = it.user ?: UserInfo())
        }
    }

    suspend fun notifications(): List<NotificationItem> {
        val json = api.post("mobile_auth.php", body = jsonBody { put("action", "get_notifications") })
        return api.serializer.decodeFromJsonElement(NotificationsResponse.serializer(), json).notifications
    }
}
