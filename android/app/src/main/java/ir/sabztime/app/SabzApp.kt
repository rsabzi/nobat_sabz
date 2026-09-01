package ir.sabztime.app

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.worker.BookingReminderScheduler
import java.util.Locale

class SabzApp : Application() {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        // رابط کاربری فارسی و راست‌به‌چپ
        Locale.setDefault(Locale("fa", "IR"))
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("fa"))
        graph = AppGraph(this)
        BookingReminderScheduler.schedulePeriodic(this)
    }

    companion object {
        fun from(context: Context): SabzApp =
            context.applicationContext as SabzApp
    }
}
