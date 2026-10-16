package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Entity(tableName = "water_entries")
data class WaterEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountMl: Int,
    val utcTimestampMillis: Long, // Full absolute UTC timestamp in epoch milliseconds
    val zoneOffsetMinutes: Int, // Local timezone offset in minutes from UTC (e.g. -420 for UTC-7)
    val localDateString: String, // "yyyy-MM-dd" formatted in recorded local offset
    val localTimeString: String, // "HH:mm" formatted in recorded local offset
    val utcIsoString: String, // ISO-8601 UTC string: "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    val note: String = ""
) {
    // Backward-compatible accessors
    val timestamp: Long get() = utcTimestampMillis
    val dateString: String get() = localDateString

    /**
     * Formats timestamp into user-friendly time string (e.g. "2:30 PM") using current device locale/timezone
     */
    fun formatCurrentLocalTime(): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date(utcTimestampMillis))
    }

    /**
     * Formats timestamp into user-friendly date string (e.g. "Fri, Oct 16, 2026")
     */
    fun formatCurrentLocalDate(): String {
        val sdf = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(utcTimestampMillis))
    }

    /**
     * Returns "yyyy-MM-dd" date key calculated from UTC millis in the device's current timezone.
     * Ensures consistent day grouping even after timezone shifts.
     */
    fun getEffectiveLocalDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(utcTimestampMillis))
    }

    /**
     * Formats timezone offset string (e.g. "UTC-07:00" or "UTC+06:00")
     */
    fun getFormattedOffset(): String {
        val sign = if (zoneOffsetMinutes >= 0) "+" else "-"
        val absMinutes = Math.abs(zoneOffsetMinutes)
        val hours = absMinutes / 60
        val mins = absMinutes % 60
        return "UTC$sign%02d:%02d".format(hours, mins)
    }

    companion object {
        fun create(
            amountMl: Int,
            utcMillis: Long = System.currentTimeMillis(),
            note: String = ""
        ): WaterEntry {
            val tz = TimeZone.getDefault()
            val offsetMillis = tz.getOffset(utcMillis)
            val offsetMinutes = offsetMillis / (60 * 1000)

            val localDateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = tz
            }
            val localTimeSdf = SimpleDateFormat("HH:mm", Locale.US).apply {
                timeZone = tz
            }
            val utcSdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            return WaterEntry(
                amountMl = amountMl,
                utcTimestampMillis = utcMillis,
                zoneOffsetMinutes = offsetMinutes,
                localDateString = localDateSdf.format(Date(utcMillis)),
                localTimeString = localTimeSdf.format(Date(utcMillis)),
                utcIsoString = utcSdf.format(Date(utcMillis)),
                note = note
            )
        }
    }
}

data class DailySummary(
    val dateString: String,
    val totalMl: Int,
    val count: Int
)
