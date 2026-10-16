package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("aquaflow_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_DAILY_GOAL = "daily_goal_ml"
        const val KEY_REMINDERS_ENABLED = "reminders_enabled"
        const val KEY_SCHEDULE_MODE = "schedule_mode" // "INTERVAL" or "CUSTOM_TIMES"
        const val KEY_START_TIME = "start_time" // "HH:mm"
        const val KEY_END_TIME = "end_time" // "HH:mm"
        const val KEY_INTERVAL_MINUTES = "interval_minutes" // e.g. 60
        const val KEY_CUSTOM_TIMES = "custom_times" // comma separated "08:00,10:00,12:00,14:00,16:00,18:00,20:00"
        const val KEY_SOUND_ENABLED = "sound_enabled"
        const val KEY_VIBRATE_ENABLED = "vibrate_enabled"
        const val KEY_THEME_MODE = "theme_mode" // "SYSTEM", "LIGHT", "DARK"
        const val KEY_PREFERRED_UNIT = "preferred_unit" // "ml" or "fl_oz"
        const val KEY_LAST_LOGGED_ID = "last_logged_id"
        const val KEY_PREFERRED_CONTAINER = "preferred_container_ml"

        fun getTodayDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
    }

    var dailyGoalMl: Int
        get() = prefs.getInt(KEY_DAILY_GOAL, 2000)
        set(value) = prefs.edit().putInt(KEY_DAILY_GOAL, value).apply()

    var remindersEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMINDERS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, value).apply()

    var scheduleMode: String
        get() = prefs.getString(KEY_SCHEDULE_MODE, "INTERVAL") ?: "INTERVAL"
        set(value) = prefs.edit().putString(KEY_SCHEDULE_MODE, value).apply()

    var startTime: String
        get() = prefs.getString(KEY_START_TIME, "08:00") ?: "08:00"
        set(value) = prefs.edit().putString(KEY_START_TIME, value).apply()

    var endTime: String
        get() = prefs.getString(KEY_END_TIME, "22:00") ?: "22:00"
        set(value) = prefs.edit().putString(KEY_END_TIME, value).apply()

    var intervalMinutes: Int
        get() = prefs.getInt(KEY_INTERVAL_MINUTES, 60)
        set(value) = prefs.edit().putInt(KEY_INTERVAL_MINUTES, value).apply()

    var customTimesString: String
        get() = prefs.getString(KEY_CUSTOM_TIMES, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_TIMES, value).apply()

    fun getCustomTimesList(): List<String> {
        val str = customTimesString
        if (str.isBlank()) return emptyList()
        return str.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .sorted()
    }

    fun setCustomTimesList(list: List<String>) {
        val sortedStr = list.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted().joinToString(",")
        customTimesString = sortedStr
    }

    fun clearCustomTimes() {
        customTimesString = ""
    }

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var vibrateEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATE_ENABLED, value).apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    var preferredUnit: String
        get() = prefs.getString(KEY_PREFERRED_UNIT, "ml") ?: "ml"
        set(value) = prefs.edit().putString(KEY_PREFERRED_UNIT, value).apply()

    var preferredContainerMl: Int
        get() = prefs.getInt(KEY_PREFERRED_CONTAINER, 250)
        set(value) = prefs.edit().putInt(KEY_PREFERRED_CONTAINER, value).apply()

    var lastLoggedEntryId: Long
        get() = prefs.getLong(KEY_LAST_LOGGED_ID, -1L)
        set(value) = prefs.edit().putLong(KEY_LAST_LOGGED_ID, value).apply()

    // Flow for real-time reactivity in ViewModels
    val preferencesChangedFlow: Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            key?.let { trySend(it) }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
