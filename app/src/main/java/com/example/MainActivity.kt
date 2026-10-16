package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reminder.ReminderScheduler
import com.example.ui.AquaFlowViewModel
import com.example.ui.components.HydrationAlarmOverlay
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AquaFlowTheme

enum class AquaScreen(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.WaterDrop, Icons.Outlined.WaterDrop, "nav_tab_home"),
    REMINDERS("Reminders", Icons.Filled.Notifications, Icons.Outlined.Notifications, "nav_tab_reminders"),
    HISTORY("History", Icons.Filled.BarChart, Icons.Outlined.BarChart, "nav_tab_history"),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_tab_settings")
}

class MainActivity : ComponentActivity() {
    private val viewModel: AquaFlowViewModel by viewModels()
    private var pendingNavigationScreen: AquaScreen? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            var currentScreen by remember { mutableStateOf(pendingNavigationScreen ?: AquaScreen.HOME) }

            // Listen for changes from notification taps
            LaunchedEffect(pendingNavigationScreen) {
                pendingNavigationScreen?.let {
                    currentScreen = it
                    pendingNavigationScreen = null
                }
            }

            AquaFlowTheme(themeSetting = uiState.themeMode) {
                // BackHandler returns to Home screen if on secondary tab
                BackHandler(enabled = currentScreen != AquaScreen.HOME) {
                    currentScreen = AquaScreen.HOME
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.navigationBars,
                    bottomBar = {
                        AquaFlowBottomNavBar(
                            currentScreen = currentScreen,
                            onScreenSelected = { screen ->
                                currentScreen = screen
                                if (screen == AquaScreen.REMINDERS || screen == AquaScreen.HOME) {
                                    viewModel.refreshNextReminderText()
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Crossfade(
                        targetState = currentScreen,
                        label = "screenTransition",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) { screen ->
                        when (screen) {
                            AquaScreen.HOME -> HomeScreen(
                                uiState = uiState,
                                onLogWater = { amount, note, timestamp -> viewModel.requestLogWater(amount, note, timestamp) },
                                onUndo = { viewModel.undoLastLog() },
                                onDeleteEntry = { entry -> viewModel.deleteEntry(entry) },
                                onNavigateToReminders = {
                                    currentScreen = AquaScreen.REMINDERS
                                    viewModel.refreshNextReminderText()
                                },
                                onDismissMessage = { viewModel.dismissMessage() },
                                onConfirmOvergoal = { viewModel.confirmOvergoalLog() },
                                onCancelOvergoal = { viewModel.cancelOvergoalLog() },
                                onTestAlarm = { viewModel.triggerAlarm() },
                                onExtendGoal = { addedMl, logDrink -> viewModel.extendDailyGoal(addedMl, logDrink) },
                                onDismissExtendGoal = { viewModel.dismissExtendGoalDialog() },
                                onOpenExtendGoal = { pending -> viewModel.openExtendGoalDialog(pending) }
                            )

                            AquaScreen.REMINDERS -> RemindersScreen(
                                uiState = uiState,
                                onToggleReminders = { enabled -> viewModel.setRemindersEnabled(enabled) },
                                onSetScheduleMode = { mode -> viewModel.setScheduleMode(mode) },
                                onSetStartTime = { time -> viewModel.setStartTime(time) },
                                onSetEndTime = { time -> viewModel.setEndTime(time) },
                                onSetIntervalMinutes = { mins -> viewModel.setIntervalMinutes(mins) },
                                onAddCustomTime = { time -> viewModel.addCustomTime(time) },
                                onRemoveCustomTime = { time -> viewModel.removeCustomTime(time) },
                                onClearAllCustomTimes = { viewModel.clearAllCustomTimes() },
                                onLoadStandardPreset = { viewModel.loadStandardPreset() },
                                onLoadWorkdayPreset = { viewModel.loadWorkdayPreset() },
                                onSetSoundEnabled = { sound -> viewModel.setSoundEnabled(sound) },
                                onSetVibrateEnabled = { vib -> viewModel.setVibrateEnabled(vib) },
                                onSendTestReminder = { viewModel.sendTestReminder() },
                                onTestAlarm = { viewModel.triggerAlarm() }
                            )

                            AquaScreen.HISTORY -> HistoryScreen(
                                uiState = uiState,
                                onDeleteEntry = { entry -> viewModel.deleteEntry(entry) }
                            )

                            AquaScreen.SETTINGS -> SettingsScreen(
                                uiState = uiState,
                                onSetDailyGoal = { goal -> viewModel.setDailyGoal(goal) },
                                onSetPreferredServing = { s -> viewModel.setPreferredContainer(s) },
                                onSetUnit = { unit -> viewModel.setPreferredUnit(unit) },
                                onSetThemeMode = { mode -> viewModel.setThemeMode(mode) },
                                onClearToday = { viewModel.clearTodayData() },
                                onClearAllHistory = { viewModel.clearAllHistory() }
                            )
                        }
                    }
                }

                // Interactive Full-Screen Hydration Alarm Overlay ("Swipe up to drink -> Done -> Automatic Goal Update")
                if (uiState.isAlarmActive) {
                    HydrationAlarmOverlay(
                        amountMl = uiState.preferredContainerMl,
                        currentTotalMl = uiState.todayTotalMl,
                        goalMl = uiState.dailyGoalMl,
                        unit = uiState.preferredUnit,
                        onDoneDrinking = { amount -> viewModel.completeAlarmDrink(amount) },
                        onSnooze = { viewModel.snoozeAlarm() },
                        onDismiss = { viewModel.dismissAlarm() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        if (intent.getBooleanExtra(ReminderScheduler.EXTRA_TRIGGER_ALARM, false)) {
            viewModel.triggerAlarm()
        }

        val navigateTo = intent.getStringExtra(ReminderScheduler.EXTRA_NAVIGATE_TO)
        if (navigateTo == "reminders") {
            pendingNavigationScreen = AquaScreen.REMINDERS
        } else if (navigateTo == "home") {
            pendingNavigationScreen = AquaScreen.HOME
        }
    }
}

@Composable
fun AquaFlowBottomNavBar(
    currentScreen: AquaScreen,
    onScreenSelected: (AquaScreen) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier.testTag("bottom_nav_bar")
    ) {
        AquaScreen.values().forEach { screen ->
            val isSelected = currentScreen == screen
            NavigationBarItem(
                selected = isSelected,
                onClick = { onScreenSelected(screen) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = screen.title
                    )
                },
                label = { Text(screen.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag(screen.testTag)
            )
        }
    }
}
