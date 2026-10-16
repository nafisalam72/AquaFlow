package com.example.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.PreferencesManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class NextScheduleInfo(
    val hasUpcoming: Boolean,
    val targetMillis: Long,
    val timeLabel: String,
    val dateLabel: String,
    val countdownLabel: String,
    val advanceAlertLabel: String,
    val fullFormattedDisplay: String
)

object ReminderScheduler {
    const val CHANNEL_ID = "aquaflow_hydration_channel"
    const val CHANNEL_ALARM_ID = "aquaflow_alarm_channel"
    const val CHANNEL_ADVANCE_ID = "aquaflow_advance_channel"

    const val NOTIFICATION_ID_REMINDER = 1001
    const val NOTIFICATION_ID_LOG_CONFIRM = 1002
    const val NOTIFICATION_ID_ADVANCE = 1003
    const val NOTIFICATION_ID_ALARM = 1004

    const val REQUEST_CODE_ALARM = 2001
    const val REQUEST_CODE_ADVANCE_ALARM = 2002
    const val REQUEST_CODE_ACTION_LOG = 3001
    const val REQUEST_CODE_ACTION_SNOOZE = 3002
    const val REQUEST_CODE_CONTENT_INTENT = 4001
    const val REQUEST_CODE_FULL_SCREEN = 4002

    const val ACTION_WATER_REMINDER = "com.example.ACTION_WATER_REMINDER"
    const val ACTION_ADVANCE_REMINDER = "com.example.ACTION_ADVANCE_REMINDER"
    const val ACTION_QUICK_LOG_WATER = "com.example.ACTION_QUICK_LOG_WATER"
    const val ACTION_SNOOZE_ALARM = "com.example.ACTION_SNOOZE_ALARM"

    const val EXTRA_LOG_AMOUNT = "extra_log_amount"
    const val EXTRA_NAVIGATE_TO = "extra_navigate_to"
    const val EXTRA_TRIGGER_ALARM = "extra_trigger_alarm"
    const val EXTRA_TARGET_ALARM_MILLIS = "extra_target_alarm_millis"

    private const val FIVE_MINUTES_MILLIS = 5 * 60 * 1000L

    fun createNotificationChannel(context: Context) = createNotificationChannels(context)

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. High Priority Hydration Alarm Channel
            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM_ID,
                "Hydration Alarms & Ringing",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority alarms when it's time to drink water"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 300, 500, 300, 800)
                setShowBadge(true)
                setBypassDnd(true)
                val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                setSound(alarmSound, audioAttributes)
            }
            notificationManager.createNotificationChannel(alarmChannel)

            // 2. 5-Minute Advance Reminder Channel
            val advanceChannel = NotificationChannel(
                CHANNEL_ADVANCE_ID,
                "5-Minute Advance Hydration Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Heads up notifications 5 minutes before scheduled drinking alarms"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(advanceChannel)

            // 3. General Hydration Channel
            val generalChannel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(generalChannel)
        }
    }

    /**
     * Schedules the next drinking alarm and the 5-minute advance reminder heads-up.
     */
    fun scheduleNextReminder(context: Context): Long? {
        val prefs = PreferencesManager(context)
        if (!prefs.remindersEnabled) {
            cancelReminders(context)
            return null
        }

        val targetMillis = calculateNextReminderMillis(prefs) ?: run {
            cancelReminders(context)
            return null
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return null

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        // 1. Schedule Primary Drinking Alarm
        val alarmIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_WATER_REMINDER
            putExtra(EXTRA_TARGET_ALARM_MILLIS, targetMillis)
        }
        val alarmPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            alarmIntent,
            flags
        )

        setAlarmExact(alarmManager, targetMillis, alarmPendingIntent)

        // 2. Schedule 5-Minute Advance Reminder if there's enough time (at least 30 seconds ahead)
        val advanceMillis = targetMillis - FIVE_MINUTES_MILLIS
        val now = System.currentTimeMillis()
        if (advanceMillis > now + 30_000L) {
            val advanceIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                action = ACTION_ADVANCE_REMINDER
                putExtra(EXTRA_TARGET_ALARM_MILLIS, targetMillis)
            }
            val advancePendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_ADVANCE_ALARM,
                advanceIntent,
                flags
            )
            setAlarmExact(alarmManager, advanceMillis, advancePendingIntent)

            context.getSharedPreferences("aquaflow_prefs", Context.MODE_PRIVATE)
                .edit()
                .putLong("next_advance_reminder_millis", advanceMillis)
                .apply()
        } else {
            // Cancel any stale advance pending intent
            val advanceIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                action = ACTION_ADVANCE_REMINDER
            }
            val advancePending = PendingIntent.getBroadcast(context, REQUEST_CODE_ADVANCE_ALARM, advanceIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            if (advancePending != null) {
                alarmManager.cancel(advancePending)
                advancePending.cancel()
            }
            context.getSharedPreferences("aquaflow_prefs", Context.MODE_PRIVATE)
                .edit()
                .remove("next_advance_reminder_millis")
                .apply()
        }

        // Save scheduled time for UI display
        context.getSharedPreferences("aquaflow_prefs", Context.MODE_PRIVATE)
            .edit()
            .putLong("next_scheduled_reminder_millis", targetMillis)
            .apply()

        return targetMillis
    }

    private fun setAlarmExact(alarmManager: AlarmManager, triggerMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } catch (ex: Exception) {
                // Ignore
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun cancelReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

        // Cancel Main Alarm
        val mainIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_WATER_REMINDER
        }
        val mainPending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            mainIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (mainPending != null && alarmManager != null) {
            alarmManager.cancel(mainPending)
            mainPending.cancel()
        }

        // Cancel Advance Alarm
        val advanceIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_ADVANCE_REMINDER
        }
        val advancePending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ADVANCE_ALARM,
            advanceIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (advancePending != null && alarmManager != null) {
            alarmManager.cancel(advancePending)
            advancePending.cancel()
        }

        context.getSharedPreferences("aquaflow_prefs", Context.MODE_PRIVATE)
            .edit()
            .remove("next_scheduled_reminder_millis")
            .remove("next_advance_reminder_millis")
            .apply()
    }

    /**
     * Calculates the exact future trigger timestamp according to interval or custom schedule.
     */
    fun calculateNextReminderMillis(prefs: PreferencesManager): Long? {
        val now = System.currentTimeMillis()

        if (prefs.scheduleMode == "CUSTOM_TIMES") {
            val times = prefs.getCustomTimesList()
            if (times.isEmpty()) return null

            // Sort custom times chronologically by minute of day (0..1439)
            val sortedTimes = times.sortedBy { timeStr ->
                val (h, m) = parseHourMinute(timeStr)
                h * 60 + m
            }

            // Find first slot today that is in the future (> now) or within the current minute
            for (timeStr in sortedTimes) {
                val slotMillis = parseTimeToday(timeStr)
                // 45s grace window handles user confirming right as the minute tick occurs
                if (slotMillis + 45_000L >= now) {
                    return maxOf(now + 1_000L, slotMillis)
                }
            }
            // All slots today have passed, schedule the earliest slot tomorrow
            return parseTimeTomorrow(sortedTimes.first())
        } else {
            // INTERVAL MODE
            val startParts = parseHourMinute(prefs.startTime)
            val endParts = parseHourMinute(prefs.endTime)
            val intervalMinutes = prefs.intervalMinutes.coerceAtLeast(15)

            val startToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, startParts.first)
                set(Calendar.MINUTE, startParts.second)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val endToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, endParts.first)
                set(Calendar.MINUTE, endParts.second)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            var currentSlot = startToday
            while (currentSlot <= endToday) {
                if (currentSlot > now) {
                    return currentSlot
                }
                currentSlot += intervalMinutes * 60 * 1000L
            }

            // Outside today's window or all passed, schedule for start time tomorrow
            return Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, startParts.first)
                set(Calendar.MINUTE, startParts.second)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }

    private fun parseHourMinute(timeStr: String): Pair<Int, Int> {
        val parts = timeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return Pair(hour, min)
    }

    private fun parseTimeToday(timeStr: String): Long {
        val (h, m) = parseHourMinute(timeStr)
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun parseTimeTomorrow(timeStr: String): Long {
        val (h, m) = parseHourMinute(timeStr)
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Shows the 5-Minute Advance Notification ("Hydration in 5 minutes! Prepare your glass").
     */
    fun showAdvanceNotification(context: Context, targetAlarmMillis: Long) {
        if (!hasPostNotificationPermission(context)) return

        createNotificationChannels(context)
        val prefs = PreferencesManager(context)

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val formattedTarget = timeFormat.format(Date(targetAlarmMillis))

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO, "home")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_CONTENT_INTENT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val logActionIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_QUICK_LOG_WATER
            putExtra(EXTRA_LOG_AMOUNT, prefs.preferredContainerMl)
        }
        val logPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ACTION_LOG,
            logActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ADVANCE_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("💧 Hydration Alert in 5 Minutes")
            .setContentText("Prepare your glass! Drinking alarm rings at $formattedTarget.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Prepare your glass! Drinking alarm rings at $formattedTarget. Get ready to drink your ${prefs.preferredContainerMl} ml of water."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Drink Now (${prefs.preferredContainerMl} ml)",
                logPendingIntent
            )

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ADVANCE, builder.build())
        } catch (e: Exception) {
            // Handled
        }
    }

    /**
     * Shows high priority alarm notification with Full-Screen Intent to launch interactive Swipe-Up Alarm Screen.
     */
    fun showAlarmNotification(context: Context, isTest: Boolean = false) {
        if (!hasPostNotificationPermission(context)) return

        createNotificationChannels(context)
        val prefs = PreferencesManager(context)

        // Dismiss any advance reminder
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_ADVANCE)

        // Full Screen Intent to launch MainActivity with Alarm Mode
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TRIGGER_ALARM, true)
            putExtra(EXTRA_NAVIGATE_TO, "home")
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_FULL_SCREEN,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Log Action Button
        val logActionIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_QUICK_LOG_WATER
            putExtra(EXTRA_LOG_AMOUNT, prefs.preferredContainerMl)
        }
        val logPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ACTION_LOG,
            logActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze Action Button
        val snoozeActionIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE_ALARM
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ACTION_SNOOZE,
            snoozeActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isTest) "💧 AquaFlow Hydration Reminder (Test)" else "💧 Time to Drink Water!"
        val message = "Time for your hydration break! Drink ${prefs.preferredContainerMl} ml to stay refreshed and reach your goal."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "✓ Done (${prefs.preferredContainerMl} ml)",
                logPendingIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "⏰ Snooze 5m",
                snoozePendingIntent
            )

        if (prefs.soundEnabled) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }
        if (prefs.vibrateEnabled) {
            builder.setVibrate(longArrayOf(0, 300, 200, 300))
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ALARM, builder.build())
        } catch (e: Exception) {
            // Handled
        }
    }

    /**
     * Shows a brief confirmation notification when water is logged
     */
    fun showLoggedConfirmationNotification(context: Context, amountMl: Int, newTotalMl: Int, goalMl: Int) {
        if (!hasPostNotificationPermission(context)) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO, "home")
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_CONTENT_INTENT,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val percent = ((newTotalMl.toFloat() / goalMl.coerceAtLeast(1)) * 100).toInt().coerceIn(0, 100)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("💧 +$amountMl ml Logged!")
            .setContentText("Today: $newTotalMl / $goalMl ml ($percent%). Great job staying hydrated!")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.cancel(NOTIFICATION_ID_ALARM)
            manager.cancel(NOTIFICATION_ID_REMINDER)
            manager.notify(NOTIFICATION_ID_LOG_CONFIRM, builder.build())
        } catch (e: Exception) {
            // Ignored
        }
    }

    fun snoozeAlarm(context: Context) {
        HydrationAlarmPlayer.stopAlarm()
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_ALARM)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val targetMillis = System.currentTimeMillis() + FIVE_MINUTES_MILLIS

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_WATER_REMINDER
            putExtra(EXTRA_TARGET_ALARM_MILLIS, targetMillis)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarmExact(alarmManager, targetMillis, pendingIntent)

        context.getSharedPreferences("aquaflow_prefs", Context.MODE_PRIVATE)
            .edit()
            .putLong("next_scheduled_reminder_millis", targetMillis)
            .apply()
    }

    fun getNextScheduleDetails(context: Context): NextScheduleInfo {
        val prefs = PreferencesManager(context)
        if (!prefs.remindersEnabled) {
            return NextScheduleInfo(
                hasUpcoming = false,
                targetMillis = 0L,
                timeLabel = "Reminders Paused",
                dateLabel = "Disabled",
                countdownLabel = "Paused",
                advanceAlertLabel = "",
                fullFormattedDisplay = "Reminders are paused"
            )
        }

        // Always calculate the accurate next reminder
        val target = calculateNextReminderMillis(prefs) ?: return NextScheduleInfo(
            hasUpcoming = false,
            targetMillis = 0L,
            timeLabel = "No Times Configured",
            dateLabel = "Add a time below",
            countdownLabel = "None",
            advanceAlertLabel = "",
            fullFormattedDisplay = "No alarms configured"
        )

        // Save current target for background receivers
        context.getSharedPreferences("aquaflow_prefs", Context.MODE_PRIVATE)
            .edit()
            .putLong("next_scheduled_reminder_millis", target)
            .apply()

        val now = System.currentTimeMillis()
        val diffMinutes = ((target - now) / 60000).toInt()

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val targetCal = Calendar.getInstance().apply { timeInMillis = target }
        val nowCal = Calendar.getInstance()

        val isToday = (targetCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
                && targetCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR))

        val dateLabel = if (isToday) "Today" else "Tomorrow"
        val timeLabel = timeFormat.format(Date(target))

        val countdownLabel = when {
            diffMinutes <= 0 -> "Due now"
            diffMinutes < 60 -> "in ${diffMinutes}m"
            else -> "in ${diffMinutes / 60}h ${diffMinutes % 60}m"
        }

        val advanceMillis = target - FIVE_MINUTES_MILLIS
        val advanceAlertLabel = if (advanceMillis > now) {
            "🔔 5-min alert at ${timeFormat.format(Date(advanceMillis))}"
        } else {
            ""
        }

        val fullFormatted = "$dateLabel at $timeLabel ($countdownLabel)"

        return NextScheduleInfo(
            hasUpcoming = true,
            targetMillis = target,
            timeLabel = timeLabel,
            dateLabel = dateLabel,
            countdownLabel = countdownLabel,
            advanceAlertLabel = advanceAlertLabel,
            fullFormattedDisplay = fullFormatted
        )
    }

    fun getNextScheduledTimeFormatted(context: Context): String {
        return getNextScheduleDetails(context).fullFormattedDisplay
    }

    fun getNextTriggerDateForTime(timeStr: String): Pair<String, String> {
        val now = System.currentTimeMillis()
        val parts = timeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val calToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val isTodaySlot = (calToday.timeInMillis + 45_000L >= now)
        val targetCal = if (isTodaySlot) {
            calToday
        } else {
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val isToday = (targetCal.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                && targetCal.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR))

        val diffMinutes = ((targetCal.timeInMillis - now) / 60000).toInt()
        val relative = when {
            diffMinutes <= 0 -> "Due now"
            diffMinutes < 60 -> "in ${diffMinutes}m"
            else -> "in ${diffMinutes / 60}h ${diffMinutes % 60}m"
        }

        val dateDisplay = if (isToday) "Today • $relative" else "Tomorrow • $relative"
        val timeDisplay = timeFormat.format(targetCal.time)

        return Pair(dateDisplay, timeDisplay)
    }

    private fun hasPostNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }
}
