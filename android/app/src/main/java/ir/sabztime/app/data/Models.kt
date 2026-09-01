package ir.sabztime.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ---------------------------------------------------------------------------
// جستجو (search_api.php)
// ---------------------------------------------------------------------------

@Serializable
data class SearchResponse(
    val success: Boolean = true,
    val data: List<BusinessSummary> = emptyList(),
    val pagination: Pagination? = null,
    val filters: FiltersData? = null,
    val error: String? = null
)

@Serializable
data class Pagination(
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("per_page") val perPage: Int = 20,
    val total: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 0,
    @SerialName("has_next") val hasNext: Boolean = false,
    @SerialName("has_prev") val hasPrev: Boolean = false
)

@Serializable
data class BusinessSummary(
    val id: Int,
    val name: String = "",
    val description: String? = null,
    @SerialName("business_type") val businessType: String? = null,
    val province: String? = null,
    val city: String? = null,
    val address: String? = null,
    val phone: String? = null,
    @SerialName("owner_name") val ownerName: String? = null,
    @SerialName("services_count") val servicesCount: Int = 0,
    val services: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class FiltersData(
    val provinces: List<ProvinceCount> = emptyList(),
    val categories: List<CategoryCount> = emptyList()
)

@Serializable
data class ProvinceCount(val province: String = "", val count: Int = 0)

@Serializable
data class CategoryCount(@SerialName("business_type") val businessType: String = "", val count: Int = 0)

@Serializable
data class CitiesResponse(val success: Boolean = true, val cities: List<String> = emptyList(), val error: String? = null)

// ---------------------------------------------------------------------------
// کسب‌وکار و خدمات (api.php / search_api.php)
// ---------------------------------------------------------------------------

@Serializable
data class BusinessDetails(
    val id: Int,
    val name: String = "",
    val description: String? = null,
    val province: String? = null,
    val city: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    @SerialName("business_type") val businessType: String? = null,
    @SerialName("slot_duration_minutes") val slotDurationMinutes: Int? = null,
    @SerialName("advance_booking_days") val advanceBookingDays: Int? = null,
    @SerialName("link_alias") val linkAlias: String? = null,
    @SerialName("is_active") val isActive: Int = 1,
    @SerialName("owner_name") val ownerName: String? = null,
    val services: List<Service> = emptyList()
)

@Serializable
data class BusinessDetailsResponse(
    val status: String = "",
    val message: String? = null,
    val data: BusinessDetails? = null
)

@Serializable
data class Service(
    val id: Int,
    val name: String = "",
    val description: String? = null,
    val price: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    @SerialName("is_active") val isActive: Int = 1
) {
    val priceValue: Long? get() = price?.toLongOrNull()
}

@Serializable
data class DateSlot(
    val date: String,
    @SerialName("jalali_date") val jalaliDate: String? = null,
    @SerialName("day_name") val dayName: String? = null,
    @SerialName("slot_count") val slotCount: Int? = null
)

@Serializable
data class DatesResponse(val status: String = "", val data: List<DateSlot> = emptyList())

@Serializable
data class TimeSlot(
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String
)

@Serializable
data class SlotsResponse(val status: String = "", val data: List<TimeSlot> = emptyList())

// ---------------------------------------------------------------------------
// رزرو
// ---------------------------------------------------------------------------

@Serializable
data class BookingSummary(
    val id: Int,
    @SerialName("business_id") val businessId: Int = 0,
    @SerialName("confirmation_code") val confirmationCode: String = "",
    @SerialName("booking_date") val bookingDate: String = "",
    @SerialName("start_time") val startTime: String = "",
    @SerialName("end_time") val endTime: String = "",
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("customer_phone") val customerPhone: String = "",
    @SerialName("customer_notes") val customerNotes: String? = null,
    val status: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("business_name") val businessName: String = "",
    @SerialName("business_address") val businessAddress: String? = null,
    @SerialName("business_phone") val businessPhone: String? = null,
    @SerialName("jalali_date") val jalaliDate: String? = null,
    @SerialName("day_name") val dayName: String? = null,
    @SerialName("can_cancel") val canCancel: Boolean = false,
    @SerialName("time_until_booking") val timeUntilBooking: Double = 0.0
)

@Serializable
data class BookingsResponse(
    val status: String = "",
    val message: String? = null,
    val bookings: List<BookingSummary> = emptyList(),
    @SerialName("total_count") val totalCount: Int = 0
)

@Serializable
data class BookingCreatedResponse(
    val status: String = "",
    val message: String? = null,
    @SerialName("booking_id") val bookingId: Int? = null,
    @SerialName("confirmation_code") val confirmationCode: String? = null,
    @SerialName("success") val success: Boolean? = null
)

@Serializable
data class CancelResponse(
    val status: String = "",
    val message: String? = null,
    @SerialName("booking_id") val bookingId: Int? = null,
    @SerialName("confirmation_code") val confirmationCode: String? = null,
    @SerialName("freed_time") val freedTime: String? = null
)

// ---------------------------------------------------------------------------
// صف
// ---------------------------------------------------------------------------

@Serializable
data class QueueItem(
    val id: Int,
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("customer_phone") val customerPhone: String? = null,
    @SerialName("booking_date") val bookingDate: String = "",
    @SerialName("start_time") val startTime: String = "",
    @SerialName("end_time") val endTime: String = "",
    val status: String = "",
    @SerialName("queue_status") val queueStatus: String = "waiting",
    @SerialName("user_id") val userId: Int? = null,
    @SerialName("user_telegram_id") val userTelegramId: Long? = null,
    val position: Int = 0,
    @SerialName("eta_minutes") val etaMinutes: Int = 0
)

@Serializable
data class QueueResponse(
    val status: String = "",
    val queue: List<QueueItem> = emptyList(),
    @SerialName("avg_duration_minutes") val avgDurationMinutes: Double = 0.0
)

@Serializable
data class CustomerQueueInfoResponse(
    val status: String = "",
    val position: Int? = null,
    @SerialName("eta_minutes") val etaMinutes: Int? = null,
    @SerialName("booking_id") val bookingId: Int? = null,
    @SerialName("avg_duration_minutes") val avgDurationMinutes: Double = 0.0,
    @SerialName("total_active") val totalActive: Int = 0
)

@Serializable
data class QueueActionResponse(
    val status: String = "",
    val message: String? = null,
    @SerialName("completed_booking_id") val completedBookingId: Int? = null,
    @SerialName("next_booking_id") val nextBookingId: Int? = null,
    @SerialName("booking_id") val bookingId: Int? = null
)

// ---------------------------------------------------------------------------
// اشتراک و کسب‌وکارهای مالک
// ---------------------------------------------------------------------------

@Serializable
data class SubscriptionStatusResponse(
    val status: String = "",
    @SerialName("free_tokens_remaining") val freeTokensRemaining: Int = 0,
    @SerialName("subscription_plan") val subscriptionPlan: String? = null,
    @SerialName("expiry_date") val expiryDate: String? = null,
    @SerialName("referral_code") val referralCode: String? = null
)

@Serializable
data class Plan(
    val id: Int,
    val name: String,
    val price: String? = null,
    @SerialName("reservation_limit") val reservationLimit: Int = 0,
    @SerialName("duration_days") val durationDays: Int = 30
)

@Serializable
data class PlansResponse(val status: String = "", val plans: List<Plan> = emptyList())

@Serializable
data class MyBusinessItem(
    val id: Int,
    val name: String = "",
    @SerialName("business_type") val businessType: String? = null,
    val province: String? = null,
    val city: String? = null,
    val address: String? = null,
    val phone: String? = null,
    @SerialName("is_active") val isActive: Int = 1,
    @SerialName("slot_duration_minutes") val slotDurationMinutes: Int? = null,
    @SerialName("link_alias") val linkAlias: String? = null,
    @SerialName("subscription_plan_id") val subscriptionPlanId: Int? = null,
    @SerialName("subscription_expiry_date") val subscriptionExpiryDate: String? = null,
    @SerialName("free_reservation_tokens") val freeReservationTokens: Int? = null,
    @SerialName("services_count") val servicesCount: Int = 0,
    @SerialName("bookings_count") val bookingsCount: Int = 0,
    @SerialName("subscription_plan_name") val subscriptionPlanName: String? = null
)

@Serializable
data class MyBusinessesResponse(
    val status: String = "",
    val businesses: List<MyBusinessItem> = emptyList(),
    @SerialName("total_count") val totalCount: Int = 0
)

// ---------------------------------------------------------------------------
// پروفایل و احراز هویت (mobile_auth.php)
// ---------------------------------------------------------------------------

@Serializable
data class UserInfo(
    @SerialName("app_user_id") val appUserId: Long = 0,
    val name: String = "",
    val phone: String = "",
    @SerialName("telegram_id") val telegramId: Long = 0
)

@Serializable
data class CreditInfo(
    @SerialName("current_credit") val currentCredit: Int = 0,
    @SerialName("total_earned") val totalEarned: Int = 0,
    @SerialName("total_deducted") val totalDeducted: Int = 0,
    @SerialName("last_updated") val lastUpdated: String? = null
)

@Serializable
data class MarketerInfo(
    val id: Int = 0,
    @SerialName("marketer_code") val marketerCode: String = "",
    @SerialName("total_businesses") val totalBusinesses: Int = 0,
    @SerialName("total_bookings") val totalBookings: Int = 0,
    @SerialName("commission_rate") val commissionRate: Int = 0,
    @SerialName("total_commission") val totalCommission: Int = 0
)

@Serializable
data class BookingStats(
    val total: Int = 0,
    val upcoming: Int = 0,
    val cancelled: Int = 0,
    val completed: Int = 0
)

@Serializable
data class ProfileData(
    val user: UserInfo = UserInfo(),
    val credit: CreditInfo? = null,
    val businesses: List<MyBusinessItem> = emptyList(),
    val marketer: MarketerInfo? = null,
    @SerialName("booking_stats") val bookingStats: BookingStats = BookingStats()
)

@Serializable
data class ProfileResponse(
    val status: String = "",
    val message: String? = null,
    @SerialName("app_user_id") val appUserId: Long? = null,
    val user: UserInfo? = null,
    val credit: CreditInfo? = null,
    val businesses: List<MyBusinessItem>? = null,
    val marketer: MarketerInfo? = null,
    @SerialName("booking_stats") val bookingStats: BookingStats? = null
)

@Serializable
data class AuthResponse(
    val status: String = "",
    val message: String? = null,
    val token: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    val user: AuthUser? = null
)

@Serializable
data class AuthUser(val name: String = "", val phone: String = "")

@Serializable
data class NotificationItem(
    val type: String = "",
    val icon: String = "🔔",
    val title: String = "",
    val body: String = "",
    @SerialName("ref_id") val refId: Int? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class NotificationsResponse(
    val status: String = "",
    val notifications: List<NotificationItem> = emptyList()
)

// ---------------------------------------------------------------------------
// داشبورد بازاریاب
// ---------------------------------------------------------------------------

@Serializable
data class MarketerBusiness(
    val id: Int = 0,
    val name: String = "",
    @SerialName("business_type") val businessType: String? = null,
    val city: String? = null,
    val province: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("total_bookings") val totalBookings: Int = 0,
    @SerialName("commission_earned") val commissionEarned: Int = 0,
    @SerialName("free_reservation_tokens") val freeReservationTokens: Int? = null,
    @SerialName("is_active") val isActive: Int = 1
)

@Serializable
data class MarketerDashboardResponse(
    val status: String = "",
    @SerialName("total_businesses") val totalBusinesses: Int = 0,
    @SerialName("total_bookings") val totalBookings: Int = 0,
    @SerialName("total_commission") val totalCommission: Int = 0,
    @SerialName("commission_rate") val commissionRate: Int = 0,
    @SerialName("monthly_businesses") val monthlyBusinesses: Int = 0,
    @SerialName("monthly_bookings") val monthlyBookings: Int = 0,
    @SerialName("monthly_commission") val monthlyCommission: Int = 0,
    @SerialName("monthly_revenue") val monthlyRevenue: List<Double> = emptyList(),
    @SerialName("business_types") val businessTypes: Map<String, Int> = emptyMap(),
    val businesses: List<MarketerBusiness> = emptyList()
)
