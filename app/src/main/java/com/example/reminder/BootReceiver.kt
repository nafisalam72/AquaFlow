package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.PreferencesManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "Received action: ${intent.action}")
        val prefs = PreferencesManager(context)
        if (prefs.remindersEnabled) {
            val scheduledTime = ReminderScheduler.scheduleNextReminder(context)
            Log.d("BootReceiver", "Rescheduled next reminder at: $scheduledTime")
        }
    }
}
