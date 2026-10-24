package com.myaccounting.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.myaccounting.app.domain.CurrencyUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("system", "سیستم"), LIGHT("light", "روشن"), DARK("dark", "تیره");

    companion object { fun fromKey(k: String?) = values().firstOrNull { it.key == k } ?: SYSTEM }
}

private val Context.dataStore by preferencesDataStore(name = "settings")

/** App/UI preferences (same ones the original keeps outside the database: theme, currency, font scale, collapsed groups). */
class AppPrefs(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")
    private val unitKey = stringPreferencesKey("currency")
    private val fontScaleKey = intPreferencesKey("font_scale")
    private val collapsedKey = stringSetPreferencesKey("collapsed_account_groups")
    private val notifDateKey = stringPreferencesKey("notif_dismissed_date")
    private val notifIdsKey = stringSetPreferencesKey("notif_dismissed_ids")
    private val exportsFolderKey = stringPreferencesKey("exports_folder_uri")
    private val reminderEnabledKey = booleanPreferencesKey("check_reminders_enabled")
    private val reminderHourKey = intPreferencesKey("check_reminders_hour")

    val theme: Flow<ThemeMode> = context.dataStore.data.map { ThemeMode.fromKey(it[themeKey]) }
    val unit: Flow<CurrencyUnit> = context.dataStore.data.map { p -> CurrencyUnit.values().firstOrNull { it.json == p[unitKey] } ?: CurrencyUnit.TOMAN }
    /** Percent, html font-size = 16px × fontScale / 100. */
    val fontScale: Flow<Int> = context.dataStore.data.map { it[fontScaleKey] ?: DEFAULT_FONT_SCALE }
    val collapsedGroups: Flow<Set<String>> = context.dataStore.data.map { it[collapsedKey] ?: emptySet() }
    /** Reminder ids dismissed today (localStorage `notif_dismissed` in the web app). */
    val notifDismissed: Flow<Set<String>> = context.dataStore.data.map { p ->
        if (p[notifDateKey] != today()) emptySet() else p[notifIdsKey] ?: emptySet()
    }
    /** The persisted SAF tree uri of the exports folder (exportDelivery.ts EXPORTS_PLUGIN_KEY). */
    val exportsFolder: Flow<String> = context.dataStore.data.map { it[exportsFolderKey] ?: "" }
    /** Check reminder prefs (checkNotifications.ts PREFS_KEY; defaults enabled=true, hour=9). */
    val checkReminderEnabled: Flow<Boolean> = context.dataStore.data.map { it[reminderEnabledKey] ?: true }
    val checkReminderHour: Flow<Int> = context.dataStore.data.map {
        (it[reminderHourKey] ?: com.myaccounting.app.domain.Notifications.CheckReminderPrefs.DEFAULT_HOUR).coerceIn(0, 23)
    }

    suspend fun setTheme(mode: ThemeMode) { context.dataStore.edit { it[themeKey] = mode.key } }
    suspend fun setUnit(unit: CurrencyUnit) { context.dataStore.edit { it[unitKey] = unit.json } }
    suspend fun setFontScale(percent: Int) { context.dataStore.edit { it[fontScaleKey] = percent.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE) } }
    suspend fun setCollapsedGroups(uids: Set<String>) { context.dataStore.edit { it[collapsedKey] = uids } }
    suspend fun setNotifDismissed(ids: Set<String>) {
        context.dataStore.edit { it[notifDateKey] = today(); it[notifIdsKey] = ids }
    }
    suspend fun setExportsFolder(uri: String) { context.dataStore.edit { it[exportsFolderKey] = uri } }
    suspend fun setCheckReminderPrefs(enabled: Boolean, hour: Int) {
        context.dataStore.edit { it[reminderEnabledKey] = enabled; it[reminderHourKey] = hour.coerceIn(0, 23) }
    }

    private fun today(): String = java.time.LocalDate.now().toString()

    companion object {
        /**
         * The web app's default (html font-size = 16px, i.e. 1rem = 44 device px at density 2.75).
         * Measured: the reference title «حساب‌ها و کارت‌های بانکی» is 569 px wide at 100 %, and the
         * native render at 105 % was 5 % wider (598 px), so the reference screenshots are at 100 %.
         */
        const val DEFAULT_FONT_SCALE = 100
        const val MIN_FONT_SCALE = 80
        const val MAX_FONT_SCALE = 150
    }
}
