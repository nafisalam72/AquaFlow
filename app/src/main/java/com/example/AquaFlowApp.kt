package com.example

import android.app.Application
import com.example.data.PreferencesManager
import com.example.reminder.ReminderScheduler

class AquaFlowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize notification channels
        ReminderScheduler.createNotificationChannels(this)

        // Ensure reminders are correctly scheduled if enabled
        val prefs = PreferencesManager(this)
        if (prefs.remindersEnabled) {
            ReminderScheduler.scheduleNextReminder(this)
        }
    }
}
