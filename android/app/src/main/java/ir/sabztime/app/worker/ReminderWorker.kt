package ir.sabztime.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ir.sabztime.app.SabzApp
import ir.sabztime.app.notif.Notifier
import ir.sabztime.app.util.Jalali
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * همگام‌سازی دوره‌ای: رزروهای آینده را از سرور می‌گیرد و برای نوبت‌هایی که
 * حدود ۳۰ دقیقه مانده به شروع، اعلان محلی ثبت می‌کند.
 */
class ReminderWorker(
    context: Context,
    params: androidx.work.WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SabzApp ?: return Result.failure()
        val loggedIn = app.graph.session.first().loggedIn
        if (!loggedIn) return Result.success()

        return try {
            val myBookings = app.graph.repository.myBookings(includeHistory = false)
            val now = java.time.ZonedDateTime.now()
            for (b in myBookings) {
                val start = try {
                    java.time.ZonedDateTime.of(
                        LocalDate.parse(b.bookingDate),
                        LocalTime.parse(b.startTime),
                        java.time.ZoneId.of("Asia/Tehran")
                    )
                } catch (e: Exception) { continue }
                val minutes = ChronoUnit.MINUTES.between(now, start)
                if (minutes in 0..45) {
                    Notifier.notifyBooking(
                        applicationContext,
                        b.id,
                        b.businessName,
                        Jalali.persianDigits(b.startTime),
                        Jalali.formatLong(b.bookingDate)
                    )
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedulePeriodic(context: Context) {
            Notifier.ensureChannel(context)
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(30, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "sabztime_reminders",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
