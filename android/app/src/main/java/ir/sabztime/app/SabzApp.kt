package ir.sabztime.app

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.worker.ReminderWorker
import java.util.Locale

class SabzApp : Application() {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()

        // Force Persian (fa-IR) locale for UI and RTL support
        val persianLocale = Locale.Builder()
            .setLanguage("fa")
            .setRegion("IR")
            .build()

        Locale.setDefault(persianLocale)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("fa-IR"))

        // Initialize dependencies and background workers
        graph = AppGraph(this)
        ReminderWorker.schedulePeriodic(this)
    }

    companion object {
        fun from(context: Context): SabzApp =
            context.applicationContext as SabzApp
    }
}
