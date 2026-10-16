package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesManager
import com.example.reminder.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AquaFlow", appName)
    }

    @Test
    fun `custom times starts empty by default without pre-populated values`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)
        prefs.clearCustomTimes()
        assertTrue(prefs.getCustomTimesList().isEmpty())
    }

    @Test
    fun `custom times can be added and cleared cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)
        prefs.clearCustomTimes()

        val sampleTimes = listOf("09:00", "12:30", "16:00")
        prefs.setCustomTimesList(sampleTimes)
        assertEquals(3, prefs.getCustomTimesList().size)
        assertEquals("09:00", prefs.getCustomTimesList()[0])

        prefs.clearCustomTimes()
        assertTrue(prefs.getCustomTimesList().isEmpty())
    }

    @Test
    fun `next schedule details formats correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)
        prefs.remindersEnabled = true
        prefs.scheduleMode = "INTERVAL"
        prefs.startTime = "08:00"
        prefs.endTime = "22:00"
        prefs.intervalMinutes = 60

        val info = ReminderScheduler.getNextScheduleDetails(context)
        assertTrue(info.hasUpcoming)
        assertTrue(info.fullFormattedDisplay.isNotEmpty())
    }
}
