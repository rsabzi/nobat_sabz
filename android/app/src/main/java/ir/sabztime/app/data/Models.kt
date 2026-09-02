package ir.sabztime.app.data

import ir.sabztime.app.core.LenientDouble
import ir.sabztime.app.core.LenientInt
import ir.sabztime.app.core.LenientLong
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

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
    @SerialName("current_page") @Serializable(with = LenientInt::class) val currentPage: Int = 1,
    @SerialName("per_page") @Serializable(with = LenientInt::class) val perPage: Int = 20,
    @Serializable(with = LenientInt::class) val total: Int = 0,
    @SerialName("total_pages") @Serializable(with = LenientInt::class) val totalPages: Int = 0,
    @SerialName("has_next") val hasNext: Boolean = false,
    @SerialName("has_prev") val hasPrev: Boolean = false
)

@Serializable
data class BusinessSummary(
    @Serializable(with = LenientInt::class) val id: Int,
    val name: String = "",
    val description: String? = null,
    @SerialName("business_type") val businessType: String? = null,
    val province: String? = null,
    val city: String? = null,
    val address: String? = null,
    val phone: String? = null,
    @SerialName("owner_name") val ownerName: String? = null,
    @SerialName("services_count") @Serializable(with = LenientInt::class) val servicesCount: Int = 0,
    val services: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class FiltersData(
    val provinces: List<ProvinceCount> = emptyList(),
    val categories: List<CategoryCount> = emptyList()
)

@Serializable
data class ProvinceCount(
    val province: String = "",
    @Serializable(with = LenientInt::class) val count: Int = 0
)

@Serializable
data class CategoryCount(
    @SerialName("business_type") val businessType: String = "",
    @Serializable(with = LenientInt::class) val count: Int = 0
)

@Serializable
data class CitiesResponse(val success: Boolean = true, val cities: List<String> = emptyList(), val error: String? = null)

// ---------------------------------------------------------------------------
// کسب‌وکار و خدمات (api.php / search_api.php)
// ---------------------------------------------------------------------------

@Serializable
data class BusinessDetails(
    @Serializable(with = LenientInt::class) val id: Int,
    val name: String = "",
    val description: String? = null,
    val province: String? = null,
    val city: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    @SerialName("business_type") val businessType: String? = null,
    @SerialName("slot_duration_minutes") @Serializable(with = LenientInt::class) val slotDurationMinutes: Int? = null,
    @SerialName("advance_booking_days") @Serializable(with = LenientInt::class) val advanceBookingDays: Int? = null,
    @SerialName("link_alias") val linkAlias: String? = null,
    @SerialName("is_active") @Serializable(with = LenientInt::class) val isActive: Int = 1,
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
    @Serializable(with = LenientInt::class) val id: Int,
    val name: String = "",
    val description: String? = null,
    // قیمت از دیتابیس به‌صورت رشته اعشاری (مثلاً "250000.00") می‌رسد
    val price: String? = null,
    @SerialName("duration_minutes") @Serializable(with = LenientInt::class) val durationMinutes: Int? = null,
    @SerialName("is_active") @Serializable(with = LenientInt::class) val isActive: Int = 1
) {
    /** قیمت به تومان (با پشتیبانی از اعداد اعشاری مثل "250000.00") */
    val priceValue: Long? get() = parsePrice(price)
}

@Serializable
data class DateSlot(
    val date: String,
    @SerialName("jalali_date") val jalaliDate: String? = null,
    @SerialName("day_name") val dayName: String? = null,
    @SerialName("slot_count") @Serializable(with = LenientInt::class) val slotCount: Int? = null
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

/** تبدیل رشته قیمت (از دیتابیس) به عدد؛ "250000.00" / "250000" / "250,000" همگی معتبرند */
fun parsePrice(raw: String?): Long? {
    val s = raw?.replace(",", "")?.trim() ?: return null
    return s.toDoubleOrNull()?.toLong()
}

// ---------------------------------------------------------------------------
// رزرو
// ---------------------------------------------------------------------------

@Serializable
data class BookingSummary(
    @Serializable(with = LenientInt::class) val id: Int,
    @SerialName("business_id") @Serializable(with = LenientInt::class) val businessId: Int = 0,
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
    @SerialName("time_until_booking") @Serializable(with = LenientDouble::class) val timeUntilBooking: Double = 0.0
)

@Serializable
data class BookingsResponse(
    val status: String = "",
    val message: String? = null,
    val bookings: List<BookingSummary> = emptyList(),
    @SerialName("total_count") @Serializable(with = LenientInt::class) val totalCount: Int = 0
)

@Serializable
data class BookingCreatedResponse(
    val status: String = "",
    val message: String? = null,
    @SerialName("booking_id") @Serializable(with = LenientInt::class) val bookingId: Int? = null,
    @SerialName("confirmation_code") val confirmationCode: String? = null,
    @SerialName("success") val success: Boolean? = null
)

@Serializable
data class CancelResponse(
    val status: String = "",
    val message: String? = null,
    @SerialName("booking_id") @Serializable(with = LenientInt::class) val bookingId: Int? = null,
    @SerialName("confirmation_code") val confirmationCode: String? = null,
    @SerialName("freed_time") val freedTime: String? = null
)

// ---------------------------------------------------------------------------
// صف
// ---------------------------------------------------------------------------

@Serializable
data class QueueItem(
    @Serializable(with = LenientInt::class) val id: Int,
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("customer_phone") val customerPhone: String? = null,
    @SerialName("booking_date") val bookingDate: String = "",
    @SerialName("start_time") val startTime: String = "",
    @SerialName("end_time") val endTime: String = "",
    val status: String = "",
    @SerialName("queue_status") val queueStatus: String = "waiting",
    @SerialName("user_id") @Serializable(with = LenientInt::class) val userId: Int? = null,
    @SerialName("user_telegram_id") @Serializable(with = LenientLong::class) val userTelegramId: Long? = null,
    @Serializable(with = LenientInt::class) val position: Int = 0,
    @SerialName("eta_minutes") @Serializable(with = LenientInt::class) val etaMinutes: Int = 0
)

@Serializable
data class QueueResponse(
    val status: String = "",
    val queue: List<QueueItem> = emptyList(),
    @SerialName("avg_duration_minutes") @Serializable(with = LenientDouble::class) val avgDurationMinutes: Double = 0.0
)

@Serializable
data class CustomerQueueInfoResponse(
    val status: String = "",
    @Serializable(with = LenientInt::class) val position: Int? = null,
    @SerialName("eta_minutes") @Serializable(with = LenientInt::class) val etaMinutes: Int? = null,
    @SerialName("booking_id") @Serializable(with = LenientInt::class) val bookingId: Int? = null,
    @SerialName("avg_duration_minutes") @Serializable(with = LenientDouble::class) val avgDurationMinutes: Double = 0.0,
    @SerialName("total_active") @Serializable(with = LenientInt::class) val totalActive: Int = 0
)

@Serializable
data class QueueActionResponse(
    val status: String = "",
    val message: String? = null,
    @SerialName("completed_booking_id") @Serializable(with = LenientInt::class) val completedBookingId: Int? = null,
    @SerialName("next_booking_id") @Serializable(with = LenientInt::class) val nextBookingId: Int? = null,
    @SerialName("booking_id") @Serializable(with = LenientInt::class) val bookingId: Int? = null
)

// ---------------------------------------------------------------------------
// اشتراک و کسب‌وکارهای مالک
// ---------------------------------------------------------------------------

@Serializable
data class SubscriptionStatusResponse(
    val status: String = "",
    @SerialName("free_tokens_remaining") @Serializable(with = LenientInt::class) val freeTokensRemaining: Int = 0,
    @SerialName("subscription_plan") val subscriptionPlan: String? = null,
    @SerialName("expiry_date") val expiryDate: String? = null,
    @SerialName("referral_code") val referralCode: String? = null
)

@Serializable
data class Plan(
    @Serializable(with = LenientInt::class) val id: Int,
    val name: String,
    val price: String? = null,
    @SerialName("reservation_limit") @Serializable(with = LenientInt::class) val reservationLimit: Int = 0,
    @SerialName("duration_days") @Serializable(with = LenientInt::class) val durationDays: Int = 30
) {
    val priceValue: Long? get() = parsePrice(price)
}

@Serializable
data class PlansResponse(val status: String = "", val plans: List<Plan> = emptyList())

@Serializable
data class MyBusinessItem(
    @Serializable(with = LenientInt::class) val id: Int,
    val name: String = "",
    @SerialName("business_type") val businessType: String? = null,
    val province: String? = null,
    val city: String? = null,
    val address: String? = null,
    val phone: String? = null,
    @SerialName("is_active") @Serializable(with = LenientInt::class) val isActive: Int = 1,
    @SerialName("slot_duration_minutes") @Serializable(with = LenientInt::class) val slotDurationMinutes: Int? = null,
    @SerialName("link_alias") val linkAlias: String? = null,
    @SerialName("subscription_plan_id") @Serializable(with = LenientInt::class) val subscriptionPlanId: Int? = null,
    @SerialName("subscription_expiry_date") val subscriptionExpiryDate: String? = null,
    @SerialName("free_reservation_tokens") @Serializable(with = LenientInt::class) val freeReservationTokens: Int? = null,
    @SerialName("services_count") @Serializable(with = LenientInt::class) val servicesCount: Int = 0,
    @SerialName("bookings_count") @Serializable(with = LenientInt::class) val bookingsCount: Int = 0,
    @SerialName("subscription_plan_name") val subscriptionPlanName: String? = null
)

@Serializable
data class MyBusinessesResponse(
    val status: String = "",
    val businesses: List<MyBusinessItem> = emptyList(),
    @SerialName("total_count") @Serializable(with = LenientInt::class) val totalCount: Int = 0
)

@Serializable
data class BusinessCreatedResponse(
    val status: String = "",
    val message: String? = null,
    @SerialName("business_id") @Serializable(with = LenientInt::class) val businessId: Int? = null
)

@Serializable
data class BusinessBooking(
    @Serializable(with = LenientInt::class) val id: Int,
    @SerialName("confirmation_code") val confirmationCode: String? = null,
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("customer_phone") val customerPhone: String? = null,
    @SerialName("booking_date") val bookingDate: String = "",
    @SerialName("jalali_date") val jalaliDate: String? = null,
    @SerialName("day_name") val dayName: String? = null,
    @SerialName("start_time") val startTime: String = "",
    @SerialName("end_time") val endTime: String = "",
    val status: String = "",
    @SerialName("queue_status") val queueStatus: String = "waiting",
    @SerialName("attendance_status") val attendanceStatus: String? = null,
    @SerialName("customer_credit") @Serializable(with = LenientInt::class) val customerCredit: Int = 0
)

@Serializable
data class BusinessBookingsResponse(
    val status: String = "",
    val message: String? = null,
    val bookings: List<BusinessBooking> = emptyList(),
    @SerialName("total_active") @Serializable(with = LenientInt::class) val totalActive: Int = 0
)

@Serializable
data class AttendanceResponse(
    val status: String = "",
    val message: String? = null,
    @SerialName("booking_id") @Serializable(with = LenientInt::class) val bookingId: Int? = null,
    @SerialName("attendance_status") val attendanceStatus: String? = null,
    @SerialName("new_credit") @Serializable(with = LenientInt::class) val newCredit: Int? = null
)

/** روز کاری هفتگی؛ dayOfWeek همان date('w') سرور است: 0=یکشنبه … 6=شنبه */
@Serializable
data class DaySchedule(
    @SerialName("day_of_week") @Serializable(with = LenientInt::class) val dayOfWeek: Int,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("is_active") @Serializable(with = LenientInt::class) val isActive: Int = 1
)

@Serializable
data class ScheduleData(
    @SerialName("slot_duration_minutes") @Serializable(with = LenientInt::class) val slotDurationMinutes: Int = 30,
    val schedules: List<DaySchedule> = emptyList()
)

@Serializable
data class LinkAliasResponse(
    val status: String = "",
    val message: String? = null,
    @SerialName("link_alias") val linkAlias: String? = null,
    @SerialName("full_link") val fullLink: String? = null
)

@Serializable
data class DeleteBusinessResponse(
    val status: String = "",
    val message: String? = null,
    @SerialName("business_name") val businessName: String? = null
)

@Serializable
data class ManagedServicesResponse(
    val status: String = "",
    val message: String? = null,
    val data: ManagedServicesData? = null
)

@Serializable
data class ManagedServicesData(val services: List<Service> = emptyList())

// ---------------------------------------------------------------------------
// پروفایل و احراز هویت (mobile_auth.php)
// ---------------------------------------------------------------------------

@Serializable
data class UserInfo(
    @SerialName("app_user_id") @Serializable(with = LenientLong::class) val appUserId: Long = 0,
    val name: String = "",
    val phone: String = "",
    @SerialName("telegram_id") @Serializable(with = LenientLong::class) val telegramId: Long = 0
)

@Serializable
data class CreditInfo(
    @SerialName("current_credit") @Serializable(with = LenientInt::class) val currentCredit: Int = 0,
    @SerialName("total_earned") @Serializable(with = LenientInt::class) val totalEarned: Int = 0,
    @SerialName("total_deducted") @Serializable(with = LenientInt::class) val totalDeducted: Int = 0,
    @SerialName("last_updated") val lastUpdated: String? = null
)

@Serializable
data class MarketerRegistrationResponse(
    val status: String = "",
    val message: String? = null,
    val marketer: MarketerInfo? = null,
    @SerialName("referral_link") val referralLink: String? = null
)

@Serializable
data class MarketerInfo(
    @Serializable(with = LenientInt::class) val id: Int = 0,
    @SerialName("marketer_code") val marketerCode: String = "",
    @SerialName("total_businesses") @Serializable(with = LenientInt::class) val totalBusinesses: Int = 0,
    @SerialName("total_bookings") @Serializable(with = LenientInt::class) val totalBookings: Int = 0,
    @SerialName("commission_rate") @Serializable(with = LenientDouble::class) val commissionRate: Double = 0.0,
    @SerialName("total_commission") @Serializable(with = LenientInt::class) val totalCommission: Int = 0
)

@Serializable
data class BookingStats(
    @Serializable(with = LenientInt::class) val total: Int = 0,
    @Serializable(with = LenientInt::class) val upcoming: Int = 0,
    @Serializable(with = LenientInt::class) val cancelled: Int = 0,
    @Serializable(with = LenientInt::class) val completed: Int = 0
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
    @SerialName("app_user_id") @Serializable(with = LenientLong::class) val appUserId: Long? = null,
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
    @SerialName("ref_id") @Serializable(with = LenientInt::class) val refId: Int? = null,
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
    @Serializable(with = LenientInt::class) val id: Int = 0,
    val name: String = "",
    @SerialName("business_type") val businessType: String? = null,
    val city: String? = null,
    val province: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("total_bookings") @Serializable(with = LenientInt::class) val totalBookings: Int = 0,
    @SerialName("commission_earned") @Serializable(with = LenientInt::class) val commissionEarned: Int = 0,
    @SerialName("free_reservation_tokens") @Serializable(with = LenientInt::class) val freeReservationTokens: Int? = null,
    @SerialName("is_active") @Serializable(with = LenientInt::class) val isActive: Int = 1
)

@Serializable
data class MarketerDashboardResponse(
    val status: String = "",
    @SerialName("total_businesses") @Serializable(with = LenientInt::class) val totalBusinesses: Int = 0,
    @SerialName("total_bookings") @Serializable(with = LenientInt::class) val totalBookings: Int = 0,
    @SerialName("total_commission") @Serializable(with = LenientInt::class) val totalCommission: Int = 0,
    @SerialName("commission_rate") @Serializable(with = LenientDouble::class) val commissionRate: Double = 0.0,
    @SerialName("monthly_businesses") @Serializable(with = LenientInt::class) val monthlyBusinesses: Int = 0,
    @SerialName("monthly_bookings") @Serializable(with = LenientInt::class) val monthlyBookings: Int = 0,
    @SerialName("monthly_commission") @Serializable(with = LenientInt::class) val monthlyCommission: Int = 0,
    @SerialName("monthly_revenue") val monthlyRevenue: List<Double> = emptyList(),
    @SerialName("business_types") val businessTypes: Map<String, Int> = emptyMap(),
    val businesses: List<MarketerBusiness> = emptyList()
)
