package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.example.ui.components.ExtendGoalDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WaterEntry
import com.example.ui.AquaFlowUiState
import com.example.ui.components.AnimatedWaterGlass
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun HomeScreen(
    uiState: AquaFlowUiState,
    onLogWater: (Int, String, Long?) -> Unit,
    onUndo: () -> Unit,
    onDeleteEntry: (WaterEntry) -> Unit,
    onNavigateToReminders: () -> Unit,
    onDismissMessage: () -> Unit,
    onConfirmOvergoal: () -> Unit = {},
    onCancelOvergoal: () -> Unit = {},
    onTestAlarm: () -> Unit = {},
    onExtendGoal: (Int, Boolean) -> Unit = { _, _ -> },
    onDismissExtendGoal: () -> Unit = {},
    onOpenExtendGoal: (Int?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCustomLogDialog by remember { mutableStateOf(false) }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Stay hydrated"
        }
    }

    val formattedDate = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Header with greeting and date
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = "AquaFlow Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AquaFlow",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$greeting 💧",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Notification status indicator badge
                    Surface(
                        shape = CircleShape,
                        color = if (uiState.remindersEnabled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onNavigateToReminders() }
                            .testTag("notification_status_badge")
                    ) {
                        Box(modifier = Modifier.padding(10.dp)) {
                            Icon(
                                imageVector = if (uiState.remindersEnabled) {
                                    Icons.Default.Notifications
                                } else {
                                    Icons.Default.NotificationsOff
                                },
                                contentDescription = "Reminders Status",
                                tint = if (uiState.remindersEnabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // 2. Animated Water Glass (Central Highlight)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("water_glass_container_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedWaterGlass(
                            consumedMl = uiState.todayTotalMl,
                            dailyGoalMl = uiState.dailyGoalMl,
                            unit = uiState.preferredUnit,
                            lastAddedAmount = uiState.lastAddedAmount,
                            lastAddedTimestamp = uiState.lastAddedTimestamp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Summary Row
                        val isGoalMet = uiState.todayTotalMl >= uiState.dailyGoalMl
                        val remainingText = if (isGoalMet) {
                            if (uiState.todayTotalMl > uiState.dailyGoalMl) {
                                "+${uiState.todayTotalMl - uiState.dailyGoalMl} ml"
                            } else {
                                "Goal Met! 💧"
                            }
                        } else {
                            formatVolume(uiState.remainingMl, uiState.preferredUnit)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatColumn(
                                label = "Consumed",
                                value = formatVolume(uiState.todayTotalMl, uiState.preferredUnit),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(32.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            )
                            StatColumn(
                                label = if (isGoalMet) "Completed" else "Remaining",
                                value = remainingText,
                                color = if (isGoalMet) Color(0xFF10B981) else MaterialTheme.colorScheme.secondary
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(32.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            )
                            StatColumn(
                                label = "Daily Goal",
                                value = formatVolume(uiState.dailyGoalMl, uiState.preferredUnit),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 3. Sleek & Modern Next Schedule & Alarm Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_dashboard_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.remindersEnabled) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Header row with Icon, Title, and Countdown Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (uiState.remindersEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Next Hydration Alarm",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (uiState.scheduleMode == "CUSTOM_TIMES") "Daily Schedule" else "Interval (${uiState.intervalMinutes}m)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Dynamic pill showing Today / Tomorrow and Countdown
                            if (uiState.remindersEnabled && uiState.nextScheduleInfo.hasUpcoming) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "${uiState.nextScheduleInfo.dateLabel} • ${uiState.nextScheduleInfo.countdownLabel}",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (uiState.remindersEnabled) "No Times Set" else "Paused",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Large clean time display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (uiState.remindersEnabled && uiState.nextScheduleInfo.hasUpcoming) {
                                        uiState.nextScheduleInfo.timeLabel
                                    } else if (uiState.remindersEnabled) {
                                        "No Alarms Set"
                                    } else {
                                        "Alarms Paused"
                                    },
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (uiState.remindersEnabled && uiState.nextScheduleInfo.hasUpcoming) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )

                                if (uiState.remindersEnabled && uiState.nextScheduleInfo.hasUpcoming) {
                                    Text(
                                        text = "🔔 5-min advance heads-up scheduled before alarm",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Quick actions
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = onTestAlarm,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("test_alarm_swipe_button")
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = "Test alarm",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = onNavigateToReminders,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("edit_reminders_schedule_button"),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Alarms", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Manual Water Logging (Drank Outside Schedule)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Quick Log Water",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Text(
                                text = "Tap a glass below to log your water and see the splash animation!",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (uiState.canUndo) {
                            TextButton(
                                onClick = onUndo,
                                modifier = Modifier.testTag("undo_quick_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Undo,
                                    contentDescription = "Undo",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Undo", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val isGoalFull = uiState.todayTotalMl >= uiState.dailyGoalMl
                    if (isGoalFull) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOpenExtendGoal(null) }
                                .testTag("goal_completed_extend_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Goal Completed! 🎉",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                        Text(
                                            text = "Tap to extend (+500ml, +1000ml, +1500ml)",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF10B981)
                                ) {
                                    Text(
                                        text = "+ Extend Goal",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // 4 Quick Buttons Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickLogChip(
                            label = "+150 ml",
                            subtitle = "Small",
                            amountMl = 150,
                            modifier = Modifier.weight(1f),
                            onClick = { onLogWater(150, "Small glass (150 ml)", null) }
                        )
                        QuickLogChip(
                            label = "+250 ml",
                            subtitle = "Glass",
                            amountMl = 250,
                            modifier = Modifier.weight(1f),
                            onClick = { onLogWater(250, "Glass (250 ml)", null) }
                        )
                        QuickLogChip(
                            label = "+350 ml",
                            subtitle = "Mug",
                            amountMl = 350,
                            modifier = Modifier.weight(1f),
                            onClick = { onLogWater(350, "Mug (350 ml)", null) }
                        )
                        QuickLogChip(
                            label = "+500 ml",
                            subtitle = "Bottle",
                            amountMl = 500,
                            modifier = Modifier.weight(1f),
                            onClick = { onLogWater(500, "Bottle (500 ml)", null) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Custom Amount Button
                    OutlinedButton(
                        onClick = { showCustomLogDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("custom_amount_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Custom Water Amount", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 5. Today's Drinks Log
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Intake (${uiState.glassesCount} drinks)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (uiState.todayEntries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalDrink,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No water logged yet today",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tap a button above to record your first glass!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            } else {
                items(uiState.todayEntries, key = { it.id }) { entry ->
                    DrinkEntryCard(
                        entry = entry,
                        unit = uiState.preferredUnit,
                        onDelete = { onDeleteEntry(entry) }
                    )
                }
            }
        }
    }

    // Custom Log Dialog
    if (showCustomLogDialog) {
        CustomAmountDialog(
            unit = uiState.preferredUnit,
            onDismiss = { showCustomLogDialog = false },
            onConfirm = { amount, note, timestampMillis ->
                onLogWater(amount, note, timestampMillis)
                showCustomLogDialog = false
            }
        )
    }

    // Extend Goal Dialog (when goal is full and user taps to add water or extend goal)
    if (uiState.showExtendGoalDialog) {
        ExtendGoalDialog(
            currentGoalMl = uiState.dailyGoalMl,
            consumedMl = uiState.todayTotalMl,
            pendingDrinkAmountMl = uiState.pendingOvergoalAmount,
            unit = uiState.preferredUnit,
            onDismiss = onDismissExtendGoal,
            onConfirmExtend = onExtendGoal
        )
    }
}

@Composable
private fun QuickLogChip(
    label: String,
    subtitle: String,
    amountMl: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("quick_log_${amountMl}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LocalDrink,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DrinkEntryCard(
    entry: WaterEntry,
    unit: String,
    onDelete: () -> Unit
) {
    val timeFormatted = remember(entry.timestamp) {
        SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()).format(Date(entry.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("drink_entry_${entry.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = formatVolume(entry.amountMl, unit),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (entry.note.isNotEmpty()) "${entry.note} • $timeFormatted" else timeFormatted,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onDelete() }
                    .testTag("delete_entry_${entry.id}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Delete",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CustomAmountDialog(
    unit: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, Long) -> Unit
) {
    var amountText by remember { mutableStateOf("250") }
    var noteText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var chosenTimestampMillis by remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
    var showDateTimePicker by remember { mutableStateOf(false) }

    val formattedDateTime = remember(chosenTimestampMillis) {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy • h:mm a", Locale.getDefault())
        sdf.format(Date(chosenTimestampMillis))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Custom Water", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                // Interactive Date & Time Card (Tap to pick date then time)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showDateTimePicker = true }
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Date & Time (Tap to Change):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = formattedDateTime,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Date/Time",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "Enter amount in $unit:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { char -> char.isDigit() }
                        error = false
                    },
                    label = { Text("Amount ($unit)") },
                    isError = error,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_amount_input")
                )
                if (error) {
                    Text(
                        text = "Please enter an amount between 10 and 3000 $unit",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note (optional, e.g. Tea, Lemon Water)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_amount_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toIntOrNull()
                    if (amount != null && amount in 10..3000) {
                        val finalMl = if (unit == "fl_oz") (amount / 0.033814f).toInt() else amount
                        onConfirm(finalMl, noteText.trim(), chosenTimestampMillis)
                    } else {
                        error = true
                    }
                },
                modifier = Modifier.testTag("custom_amount_submit")
            ) {
                Text("Log Drink")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDateTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = chosenTimestampMillis }
        com.example.ui.components.DateTimePickerDialog(
            title = "Select Date & Time",
            initialDateMillis = chosenTimestampMillis,
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            onDismiss = { showDateTimePicker = false },
            onConfirm = { dateMillis, hour, minute, _ ->
                val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    timeInMillis = dateMillis
                }
                val localCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                    set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                chosenTimestampMillis = localCal.timeInMillis
                showDateTimePicker = false
            }
        )
    }
}

fun formatVolume(amountMl: Int, unit: String): String {
    return if (unit == "fl_oz") {
        "%.1f oz".format(amountMl * 0.033814f)
    } else {
        "%,d ml".format(amountMl)
    }
}
