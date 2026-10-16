package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AquaFlowDatabase
import com.example.data.DailySummary
import com.example.data.PreferencesManager
import com.example.data.WaterEntry
import com.example.reminder.HydrationAlarmPlayer
import com.example.reminder.NextScheduleInfo
import com.example.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class AquaFlowUiState(
    val todayEntries: List<WaterEntry> = emptyList(),
    val todayTotalMl: Int = 0,
    val dailyGoalMl: Int = 2000,
    val remainingMl: Int = 2000,
    val progress: Float = 0f,
    val glassesCount: Int = 0,
    val remindersEnabled: Boolean = true,
    val scheduleMode: String = "INTERVAL", // "INTERVAL" or "CUSTOM_TIMES"
    val startTime: String = "08:00",
    val endTime: String = "22:00",
    val intervalMinutes: Int = 60,
    val customTimesList: List<String> = emptyList(),
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val themeMode: String = "SYSTEM",
    val preferredUnit: String = "ml",
    val preferredContainerMl: Int = 250,
    val nextScheduledReminderText: String = "",
    val nextScheduleInfo: NextScheduleInfo = NextScheduleInfo(
        hasUpcoming = false,
        targetMillis = 0L,
        timeLabel = "",
        dateLabel = "",
        countdownLabel = "",
        advanceAlertLabel = "",
        fullFormattedDisplay = ""
    ),
    val isAlarmActive: Boolean = false,
    val pendingOvergoalAmount: Int? = null,
    val pendingOvergoalNote: String = "",
    val canUndo: Boolean = false,
    val weeklySummaries: List<DailySummary> = emptyList(),
    val allEntries: List<WaterEntry> = emptyList(),
    val lastLoggedMessage: String? = null,
    val lastAddedAmount: Int? = null,
    val lastAddedTimestamp: Long = 0L,
    val showExtendGoalDialog: Boolean = false
)

class AquaFlowViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AquaFlowDatabase.getDatabase(application)
    private val dao = db.waterDao()
    private val prefs = PreferencesManager(application)

    private val _uiState = MutableStateFlow(AquaFlowUiState())
    val uiState: StateFlow<AquaFlowUiState> = _uiState.asStateFlow()

    private var lastLoggedEntryId: Long = -1L

    init {
        loadPreferences()
        observeTodayData()
        observeWeeklySummaries()
        observeAllEntries()
        refreshNextReminderText()
    }

    private fun observeAllEntries() {
        viewModelScope.launch {
            dao.getAllEntries().collect { entries ->
                _uiState.value = _uiState.value.copy(allEntries = entries)
            }
        }
    }

    private fun loadPreferences() {
        _uiState.value = _uiState.value.copy(
            dailyGoalMl = prefs.dailyGoalMl,
            remindersEnabled = prefs.remindersEnabled,
            scheduleMode = prefs.scheduleMode,
            startTime = prefs.startTime,
            endTime = prefs.endTime,
            intervalMinutes = prefs.intervalMinutes,
            customTimesList = prefs.getCustomTimesList(),
            soundEnabled = prefs.soundEnabled,
            vibrateEnabled = prefs.vibrateEnabled,
            themeMode = prefs.themeMode,
            preferredUnit = prefs.preferredUnit,
            preferredContainerMl = prefs.preferredContainerMl
        )
    }

    private fun observeTodayData() {
        val todayStr = PreferencesManager.getTodayDateString()
        viewModelScope.launch {
            combine(
                dao.getEntriesForDate(todayStr),
                dao.getTotalForDate(todayStr)
            ) { entries, total ->
                Pair(entries, total)
            }.collect { (entries, total) ->
                val goal = _uiState.value.dailyGoalMl
                val remaining = (goal - total).coerceAtLeast(0)
                val progress = if (goal > 0) (total.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
                _uiState.value = _uiState.value.copy(
                    todayEntries = entries,
                    todayTotalMl = total,
                    remainingMl = remaining,
                    progress = progress,
                    glassesCount = entries.size,
                    canUndo = lastLoggedEntryId > 0 && entries.any { it.id == lastLoggedEntryId }
                )
            }
        }
    }

    private fun observeWeeklySummaries() {
        viewModelScope.launch {
            dao.getDailySummaries(14).collect { summaries ->
                _uiState.value = _uiState.value.copy(weeklySummaries = summaries)
            }
        }
    }

    fun refreshNextReminderText() {
        val nextInfo = ReminderScheduler.getNextScheduleDetails(getApplication())
        _uiState.value = _uiState.value.copy(
            nextScheduledReminderText = nextInfo.fullFormattedDisplay,
            nextScheduleInfo = nextInfo
        )
    }

    /**
     * Checks if goal is full:
     * If goal is full (todayTotalMl >= dailyGoalMl), water is not added automatically.
     * Instead, prompts the Extend Goal Dialog offering +500ml, +1000ml, +1500ml, and Custom.
     */
    fun requestLogWater(amountMl: Int, note: String = "", customTimestampMillis: Long? = null) {
        if (amountMl <= 0) return
        val currentTotal = _uiState.value.todayTotalMl
        val goal = _uiState.value.dailyGoalMl

        if (currentTotal >= goal) {
            // Goal is full! Do not add automatically. Show Extend Goal Dialog!
            _uiState.value = _uiState.value.copy(
                showExtendGoalDialog = true,
                pendingOvergoalAmount = amountMl,
                pendingOvergoalNote = note
            )
        } else {
            // Goal not full yet: unblocked normal logging
            logWater(amountMl, note, customTimestampMillis)
        }
    }

    fun openExtendGoalDialog(pendingAmount: Int? = null) {
        _uiState.value = _uiState.value.copy(
            showExtendGoalDialog = true,
            pendingOvergoalAmount = pendingAmount
        )
    }

    fun dismissExtendGoalDialog() {
        _uiState.value = _uiState.value.copy(
            showExtendGoalDialog = false,
            pendingOvergoalAmount = null,
            pendingOvergoalNote = ""
        )
    }

    /**
     * Extends the daily goal by addedMl (e.g. 500, 1000, 1500, or custom) and optionally logs the pending drink.
     */
    fun extendDailyGoal(addedMl: Int, logPendingDrink: Boolean = true) {
        if (addedMl <= 0) return
        val currentGoal = _uiState.value.dailyGoalMl
        val newGoal = currentGoal + addedMl
        prefs.dailyGoalMl = newGoal

        val total = _uiState.value.todayTotalMl
        val remaining = (newGoal - total).coerceAtLeast(0)
        val progress = if (newGoal > 0) total.toFloat() / newGoal.toFloat() else 0f

        val pendingAmount = _uiState.value.pendingOvergoalAmount
        val pendingNote = _uiState.value.pendingOvergoalNote

        _uiState.value = _uiState.value.copy(
            dailyGoalMl = newGoal,
            remainingMl = remaining,
            progress = progress,
            showExtendGoalDialog = false,
            pendingOvergoalAmount = null,
            pendingOvergoalNote = ""
        )

        if (logPendingDrink && pendingAmount != null && pendingAmount > 0) {
            logWater(pendingAmount, if (pendingNote.isNotBlank()) pendingNote else "Logged drink")
        }
    }

    fun confirmOvergoalLog() {
        val amount = _uiState.value.pendingOvergoalAmount ?: return
        val note = _uiState.value.pendingOvergoalNote
        _uiState.value = _uiState.value.copy(
            showExtendGoalDialog = false,
            pendingOvergoalAmount = null,
            pendingOvergoalNote = ""
        )
        logWater(amount, if (note.isNotBlank()) note else "Intake", null)
    }

    fun cancelOvergoalLog() {
        _uiState.value = _uiState.value.copy(
            showExtendGoalDialog = false,
            pendingOvergoalAmount = null,
            pendingOvergoalNote = ""
        )
    }

    fun logWater(amountMl: Int, note: String = "", customTimestampMillis: Long? = null) {
        if (amountMl <= 0) return
        val targetMillis = customTimestampMillis ?: System.currentTimeMillis()
        viewModelScope.launch {
            val entry = WaterEntry.create(
                amountMl = amountMl,
                utcMillis = targetMillis,
                note = note.ifBlank { "Logged drink" }
            )
            val newId = dao.insert(entry)
            lastLoggedEntryId = newId
            prefs.lastLoggedEntryId = newId

            _uiState.value = _uiState.value.copy(
                canUndo = true,
                lastAddedAmount = amountMl,
                lastAddedTimestamp = System.currentTimeMillis(),
                lastLoggedMessage = null // No bottom popup! Animation runs inside water glass
            )

            // Reschedule subsequent reminder if reminders are active
            if (prefs.remindersEnabled) {
                ReminderScheduler.scheduleNextReminder(getApplication())
                refreshNextReminderText()
            }
        }
    }

    /**
     * Called when user completes the Drinking Alarm screen by tapping "Done — I Drank Water"
     */
    fun completeAlarmDrink(amountMl: Int) {
        HydrationAlarmPlayer.stopAlarm()
        _uiState.value = _uiState.value.copy(isAlarmActive = false)
        logWater(amountMl, "Hydration Alarm (Drank & Verified)")
    }

    /**
     * Triggers the full Hydration Alarm screen with sound & vibration
     */
    fun triggerAlarm() {
        _uiState.value = _uiState.value.copy(isAlarmActive = true)
    }

    fun dismissAlarm() {
        HydrationAlarmPlayer.stopAlarm()
        _uiState.value = _uiState.value.copy(isAlarmActive = false)
    }

    fun snoozeAlarm() {
        HydrationAlarmPlayer.stopAlarm()
        _uiState.value = _uiState.value.copy(isAlarmActive = false)
        ReminderScheduler.snoozeAlarm(getApplication())
        refreshNextReminderText()
    }

    fun undoLastLog() {
        if (lastLoggedEntryId <= 0) return
        viewModelScope.launch {
            dao.deleteById(lastLoggedEntryId)
            lastLoggedEntryId = -1L
            prefs.lastLoggedEntryId = -1L
            _uiState.value = _uiState.value.copy(
                canUndo = false
            )
        }
    }

    fun deleteEntry(entry: WaterEntry) {
        viewModelScope.launch {
            dao.delete(entry)
            if (entry.id == lastLoggedEntryId) {
                lastLoggedEntryId = -1L
                prefs.lastLoggedEntryId = -1L
            }
        }
    }

    fun setDailyGoal(goalMl: Int) {
        if (goalMl < 500) return
        prefs.dailyGoalMl = goalMl
        val total = _uiState.value.todayTotalMl
        val remaining = (goalMl - total).coerceAtLeast(0)
        val progress = if (goalMl > 0) (total.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f) else 0f
        _uiState.value = _uiState.value.copy(
            dailyGoalMl = goalMl,
            remainingMl = remaining,
            progress = progress
        )
    }

    fun setRemindersEnabled(enabled: Boolean) {
        prefs.remindersEnabled = enabled
        _uiState.value = _uiState.value.copy(remindersEnabled = enabled)
        if (enabled) {
            ReminderScheduler.scheduleNextReminder(getApplication())
        } else {
            ReminderScheduler.cancelReminders(getApplication())
        }
        refreshNextReminderText()
    }

    fun setScheduleMode(mode: String) {
        prefs.scheduleMode = mode
        _uiState.value = _uiState.value.copy(scheduleMode = mode)
        if (prefs.remindersEnabled) {
            ReminderScheduler.scheduleNextReminder(getApplication())
        }
        refreshNextReminderText()
    }

    fun setStartTime(time: String) {
        prefs.startTime = time
        _uiState.value = _uiState.value.copy(startTime = time)
        if (prefs.remindersEnabled) {
            ReminderScheduler.scheduleNextReminder(getApplication())
        }
        refreshNextReminderText()
    }

    fun setEndTime(time: String) {
        prefs.endTime = time
        _uiState.value = _uiState.value.copy(endTime = time)
        if (prefs.remindersEnabled) {
            ReminderScheduler.scheduleNextReminder(getApplication())
        }
        refreshNextReminderText()
    }

    fun setIntervalMinutes(minutes: Int) {
        prefs.intervalMinutes = minutes
        _uiState.value = _uiState.value.copy(intervalMinutes = minutes)
        if (prefs.remindersEnabled) {
            ReminderScheduler.scheduleNextReminder(getApplication())
        }
        refreshNextReminderText()
    }

    fun addCustomTime(time: String) {
        val current = prefs.getCustomTimesList().toMutableList()
        if (!current.contains(time)) {
            current.add(time)
            prefs.setCustomTimesList(current)
            _uiState.value = _uiState.value.copy(customTimesList = prefs.getCustomTimesList())
            if (prefs.remindersEnabled) {
                ReminderScheduler.scheduleNextReminder(getApplication())
            }
            refreshNextReminderText()
        }
    }

    fun removeCustomTime(time: String) {
        val current = prefs.getCustomTimesList().toMutableList()
        current.remove(time)
        prefs.setCustomTimesList(current)
        _uiState.value = _uiState.value.copy(customTimesList = prefs.getCustomTimesList())
        if (prefs.remindersEnabled) {
            ReminderScheduler.scheduleNextReminder(getApplication())
        }
        refreshNextReminderText()
    }

    fun clearAllCustomTimes() {
        prefs.clearCustomTimes()
        _uiState.value = _uiState.value.copy(customTimesList = emptyList())
        if (prefs.remindersEnabled) {
            ReminderScheduler.scheduleNextReminder(getApplication())
        }
        refreshNextReminderText()
    }

    fun loadStandardPreset() {
        val preset = listOf("08:00", "10:00", "12:00", "14:00", "16:00", "18:00", "20:00")
        prefs.setCustomTimesList(preset)
        _uiState.value = _uiState.value.copy(customTimesList = prefs.getCustomTimesList())
        if (prefs.remindersEnabled) {
            ReminderScheduler.scheduleNextReminder(getApplication())
        }
        refreshNextReminderText()
    }

    fun loadWorkdayPreset() {
        val preset = listOf("09:00", "11:00", "13:00", "15:00", "17:00")
        prefs.setCustomTimesList(preset)
        _uiState.value = _uiState.value.copy(customTimesList = prefs.getCustomTimesList())
        if (prefs.remindersEnabled) {
            ReminderScheduler.scheduleNextReminder(getApplication())
        }
        refreshNextReminderText()
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.soundEnabled = enabled
        _uiState.value = _uiState.value.copy(soundEnabled = enabled)
    }

    fun setVibrateEnabled(enabled: Boolean) {
        prefs.vibrateEnabled = enabled
        _uiState.value = _uiState.value.copy(vibrateEnabled = enabled)
    }

    fun setThemeMode(mode: String) {
        prefs.themeMode = mode
        _uiState.value = _uiState.value.copy(themeMode = mode)
    }

    fun setPreferredUnit(unit: String) {
        prefs.preferredUnit = unit
        _uiState.value = _uiState.value.copy(preferredUnit = unit)
    }

    fun setPreferredContainer(amountMl: Int) {
        prefs.preferredContainerMl = amountMl
        _uiState.value = _uiState.value.copy(preferredContainerMl = amountMl)
    }

    fun sendTestReminder() {
        ReminderScheduler.showAlarmNotification(getApplication(), isTest = true)
        refreshNextReminderText()
    }

    fun clearTodayData() {
        val todayStr = PreferencesManager.getTodayDateString()
        viewModelScope.launch {
            dao.clearDate(todayStr)
            lastLoggedEntryId = -1L
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            dao.clearAll()
            lastLoggedEntryId = -1L
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(lastLoggedMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        HydrationAlarmPlayer.stopAlarm()
    }
}
