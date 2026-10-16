package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AquaFlowDatabase
import com.example.data.PreferencesManager
import com.example.data.WaterEntry
import com.example.reminder.ReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AquaFlowTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testAppName() {
        val appName = context.getString(R.string.app_name)
        assertEquals("AquaFlow", appName)
    }

    @Test
    fun testPreferencesDefaultsAndChanges() {
        val prefs = PreferencesManager(context)
        prefs.dailyGoalMl = 2500
        assertEquals(2500, prefs.dailyGoalMl)

        prefs.remindersEnabled = true
        assertTrue(prefs.remindersEnabled)

        prefs.intervalMinutes = 45
        assertEquals(45, prefs.intervalMinutes)

        prefs.setCustomTimesList(listOf("09:00", "12:00", "15:00"))
        val times = prefs.getCustomTimesList()
        assertEquals(3, times.size)
        assertEquals("09:00", times[0])
    }

    @Test
    fun testRoomDatabaseInsertAndTotal() = runBlocking {
        val db = AquaFlowDatabase.getDatabase(context)
        val dao = db.waterDao()
        dao.clearAll()

        val today = PreferencesManager.getTodayDateString()
        val entry1 = WaterEntry.create(amountMl = 250, utcMillis = System.currentTimeMillis(), note = "Morning drink")
        val entry2 = WaterEntry.create(amountMl = 500, utcMillis = System.currentTimeMillis() + 1000, note = "Afternoon drink")

        dao.insert(entry1)
        dao.insert(entry2)

        val total = dao.getTotalForDateSync(today)
        assertEquals(750, total)

        val entries = dao.getEntriesForDate(today).first()
        assertEquals(2, entries.size)
        assertNotNull(entries[0].utcIsoString)
        assertNotNull(entries[0].getFormattedOffset())

        // Clean up
        dao.clearAll()
    }

    @Test
    fun testReminderCalculation() {
        val prefs = PreferencesManager(context)
        prefs.remindersEnabled = true
        prefs.scheduleMode = "INTERVAL"
        prefs.startTime = "08:00"
        prefs.endTime = "22:00"
        prefs.intervalMinutes = 60

        val nextTrigger = ReminderScheduler.calculateNextReminderMillis(prefs)
        assertNotNull(nextTrigger)
        assertTrue(nextTrigger!! > System.currentTimeMillis())
    }

    @Test
    fun testNextTriggerDateForTimeIncludesDate() {
        val (dateStr, timeStr) = ReminderScheduler.getNextTriggerDateForTime("15:30")
        assertNotNull(dateStr)
        assertNotNull(timeStr)
        assertTrue(dateStr.contains("Today") || dateStr.contains("Tomorrow"))
        assertTrue(timeStr.contains("3:30"))
    }
}
