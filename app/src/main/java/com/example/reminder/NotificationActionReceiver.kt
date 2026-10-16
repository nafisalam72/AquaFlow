package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.data.AquaFlowDatabase
import com.example.data.PreferencesManager
import com.example.data.WaterEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        private var lastLogClickTimestamp: Long = 0L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("NotificationActionReceiver", "Received action: $action")

        // Stop any ringing alarm immediately when user interacts
        HydrationAlarmPlayer.stopAlarm()

        if (action == ReminderScheduler.ACTION_SNOOZE_ALARM) {
            ReminderScheduler.snoozeAlarm(context)
            return
        }

        val nowElapsed = SystemClock.elapsedRealtime()
        // Prevent duplicate trigger if double tapped within 1.5 seconds
        if (nowElapsed - lastLogClickTimestamp < 1500) {
            Log.d("NotificationActionReceiver", "Ignoring duplicate tap within 1.5s")
            return
        }
        lastLogClickTimestamp = nowElapsed

        val amount = intent.getIntExtra(ReminderScheduler.EXTRA_LOG_AMOUNT, 250)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AquaFlowDatabase.getDatabase(context)
                val prefs = PreferencesManager(context)
                val todayStr = PreferencesManager.getTodayDateString()

                val entry = WaterEntry.create(
                    amountMl = amount,
                    utcMillis = System.currentTimeMillis(),
                    note = "Logged from notification/alarm"
                )
                val newId = db.waterDao().insert(entry)
                prefs.lastLoggedEntryId = newId

                val newTotal = db.waterDao().getTotalForDateSync(todayStr)
                val goal = prefs.dailyGoalMl

                // Dismiss alarm notification
                NotificationManagerCompat.from(context).cancel(ReminderScheduler.NOTIFICATION_ID_ALARM)
                NotificationManagerCompat.from(context).cancel(ReminderScheduler.NOTIFICATION_ID_REMINDER)

                // Show confirmation notification
                ReminderScheduler.showLoggedConfirmationNotification(
                    context,
                    amountMl = amount,
                    newTotalMl = newTotal,
                    goalMl = goal
                )

                // Reschedule next reminder since user just drank!
                ReminderScheduler.scheduleNextReminder(context)
            } catch (e: Exception) {
                Log.e("NotificationActionReceiver", "Error logging water from notification", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
