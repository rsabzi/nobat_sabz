package ir.sabztime.app.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** حالت پوسته‌ی کاربر: خودکار (همراه سیستم) / روشن / تاریک */
enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun fromIndex(index: Int): ThemeMode = values().getOrElse(index) { SYSTEM }
    }
}

private val Context.themeDataStore by preferencesDataStore(name = "sabz_prefs")

/**
 * مدیریت و ذخیره‌ی انتخاب پوسته (روشن/تاریک/خودکار) در DataStore.
 * تغییر پوسته بلافاصله روی UI اعمال می‌شود و بین اجراها ماندگار است.
 */
class ThemeManager(context: Context, scope: CoroutineScope) {

    private val dataStore = context.applicationContext.themeDataStore

    val mode: StateFlow<ThemeMode> = dataStore.data
        .map { prefs -> ThemeMode.fromIndex(prefs[KEY_MODE] ?: 0) }
        .stateIn(scope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    suspend fun setMode(value: ThemeMode) {
        dataStore.edit { prefs -> prefs[KEY_MODE] = value.ordinal }
    }

    companion object {
        private val KEY_MODE = intPreferencesKey("theme_mode")
    }
}
