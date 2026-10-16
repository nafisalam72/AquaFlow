package com.example.reminder

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

object HydrationAlarmPlayer {
    private const val TAG = "HydrationAlarmPlayer"

    private var currentRingtone: Ringtone? = null
    private var isPlaying = false
    private var vibrator: Vibrator? = null

    @Synchronized
    fun startAlarm(context: Context, playSound: Boolean = true, vibrate: Boolean = true) {
        if (isPlaying) return
        isPlaying = true
        Log.d(TAG, "Starting hydration alarm: sound=$playSound, vibrate=$vibrate")

        val appContext = context.applicationContext

        // 1. Play Alarm Sound
        if (playSound) {
            try {
                var alarmUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                if (alarmUri == null) {
                    alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                }
                if (alarmUri == null) {
                    alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }

                alarmUri?.let { uri ->
                    currentRingtone = RingtoneManager.getRingtone(appContext, uri)?.apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            audioAttributes = AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        }
                        play()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing alarm sound", e)
            }
        }

        // 2. Continuous Vibration
        if (vibrate) {
            try {
                vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    manager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

                val pattern = longArrayOf(0, 500, 300, 500, 300, 800)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting vibration", e)
            }
        }
    }

    @Synchronized
    fun stopAlarm() {
        Log.d(TAG, "Stopping hydration alarm")
        isPlaying = false

        try {
            currentRingtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
            currentRingtone = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping ringtone", e)
        }

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling vibrator", e)
        }
    }

    fun isAlarmActive(): Boolean = isPlaying
}
