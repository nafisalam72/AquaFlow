package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.PreferencesManager

class ReminderBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "ReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val targetMillis = intent.getLongExtra(ReminderScheduler.EXTRA_TARGET_ALARM_MILLIS, System.currentTimeMillis())
        Log.d(TAG, "Alarm received: action=$action, targetMillis=$targetMillis")

        val prefs = PreferencesManager(context)

        when (action) {
            ReminderScheduler.ACTION_ADVANCE_REMINDER -> {
                // 5-Minute Advance Heads-Up Reminder
                ReminderScheduler.showAdvanceNotification(context, targetMillis)
            }
            ReminderScheduler.ACTION_WATER_REMINDER -> {
                // Primary Drinking Reminder Notification
                ReminderScheduler.showAlarmNotification(context, isTest = false)

                // Reschedule subsequent reminder
                ReminderScheduler.scheduleNextReminder(context)
            }
        }
    }
}
