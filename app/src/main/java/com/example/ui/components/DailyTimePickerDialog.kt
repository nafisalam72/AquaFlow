package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Daily Time Picker Dialog (No Date Picker).
 * Daily recurring alarms only.
 * Step 1: Clock Dial to select Hour (1..12) with explicit AM/PM control.
 * Step 2: Clock Dial to select Minute (0..59) with quick chips and stepper.
 * Includes instant live preview of Today vs Tomorrow with a 45s grace window
 * to prevent 12:30 -> 12:31 from flipping unexpectedly to tomorrow.
 */
@Composable
fun DailyTimePickerDialog(
    title: String = "Set Daily Alarm Time",
    initialHour24: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    initialMinute: Int = Calendar.getInstance().get(Calendar.MINUTE),
    onDismiss: () -> Unit,
    onConfirm: (formattedTime24: String, hour24: Int, minute: Int) -> Unit
) {
    // Current step: 1 = Hour, 2 = Minute
    var currentStep by remember { mutableIntStateOf(1) }

    // Convert 24-hr initial hour into 12-hr format and AM/PM
    val initIsPm = initialHour24 >= 12
    val initHour12 = when {
        initialHour24 == 0 -> 12
        initialHour24 > 12 -> initialHour24 - 12
        else -> initialHour24
    }

    var selectedHour12 by remember { mutableIntStateOf(initHour12) }
    var isPm by remember { mutableStateOf(initIsPm) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }

    // Convert to 24-hour hour
    val calculatedHour24 by remember {
        derivedStateOf {
            when {
                selectedHour12 == 12 -> if (isPm) 12 else 0
                isPm -> selectedHour12 + 12
                else -> selectedHour12
            }
        }
    }

    // Live preview calculation: Does this time fall into Today or Tomorrow?
    val liveSchedulePreview by remember {
        derivedStateOf {
            val now = System.currentTimeMillis()
            val calToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, calculatedHour24)
                set(Calendar.MINUTE, selectedMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Include a 45-second grace window so setting the next upcoming minute
            // (e.g. 12:31 when it's 12:30:50) never gets pushed to tomorrow
            val isToday = (calToday.timeInMillis + 45_000L >= now)

            val targetMillis = if (isToday) {
                maxOf(now, calToday.timeInMillis)
            } else {
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, calculatedHour24)
                    set(Calendar.MINUTE, selectedMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }

            val diffMinutes = ((targetMillis - now) / 60000).toInt()

            val countdown = when {
                diffMinutes <= 0 -> "Due in under a minute"
                diffMinutes < 60 -> "in $diffMinutes min"
                else -> "in ${diffMinutes / 60}h ${diffMinutes % 60}m"
            }

            val formatted12 = "%02d:%02d %s".format(
                selectedHour12,
                selectedMinute,
                if (isPm) "PM" else "AM"
            )

            Triple(isToday, formatted12, countdown)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    // Step Indicator Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = if (currentStep == 1) "1. Hour Dial" else "2. Minute Dial",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Time Display Header: [ HH ] : [ MM ] [ AM / PM ]
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hour clickable box
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (currentStep == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .clickable { currentStep = 1 }
                                .testTag("time_picker_hour_header")
                        ) {
                            Text(
                                text = "%02d".format(selectedHour12),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (currentStep == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = ":",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        // Minute clickable box
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (currentStep == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .clickable { currentStep = 2 }
                                .testTag("time_picker_minute_header")
                        ) {
                            Text(
                                text = "%02d".format(selectedMinute),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (currentStep == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // AM / PM Badges
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (!isPm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = if (!isPm) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .clickable { isPm = false }
                                    .testTag("time_picker_toggle_am")
                            ) {
                                Text(
                                    text = "AM",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isPm) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isPm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = if (isPm) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .clickable { isPm = true }
                                    .testTag("time_picker_toggle_pm")
                            ) {
                                Text(
                                    text = "PM",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPm) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "time_picker_step_animation"
                ) { step ->
                    if (step == 1) {
                        // ================= STEP 1: CLOCK DIAL FOR HOUR =================
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tap Clock Face or Number:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = if (isPm) "🌙 PM (Afternoon/Night)" else "☀️ AM (Morning)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Circular Clock Dial for Hours
                            ClockDialFace(
                                items = (1..12).toList(),
                                selectedValue = selectedHour12,
                                displayTransform = { "$it" },
                                onSelect = { hour ->
                                    selectedHour12 = hour
                                },
                                isHourDial = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Quick AM/PM explicit toggle bar with noon/midnight note
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (!isPm) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = if (!isPm) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { isPm = false }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "☀️ AM (Morning)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (!isPm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (selectedHour12 == 12) "12 AM is Midnight" else "Before noon",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isPm) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = if (isPm) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { isPm = true }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "🌙 PM (Afternoon)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (selectedHour12 == 12) "12 PM is Noon" else "After noon",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // ================= STEP 2: CLOCK DIAL FOR MINUTE =================
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Select Minute (00 to 59):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                TextButton(
                                    onClick = { currentStep = 1 },
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Hour", fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Circular Clock Dial for Minutes
                            val minuteDialNumbers = listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)
                            ClockDialFace(
                                items = minuteDialNumbers,
                                selectedValue = (selectedMinute / 5) * 5,
                                displayTransform = { "%02d".format(it) },
                                onSelect = { minute ->
                                    selectedMinute = minute
                                },
                                isHourDial = false
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Fine Precision Stepper (-5, -1, +1, +5)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { selectedMinute = (selectedMinute - 5 + 60) % 60 },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("-5m", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { selectedMinute = (selectedMinute - 1 + 60) % 60 },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("-1m", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { selectedMinute = (selectedMinute + 1) % 60 },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("+1m", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { selectedMinute = (selectedMinute + 5) % 60 },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("+5m", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Live Preview Card (Addresses the user's specific concern about Today vs Tomorrow!)
                val (isToday, formattedTime, countdown) = liveSchedulePreview
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isToday) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isToday) Color(0xFF81C784) else Color(0xFFFFB74D)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (isToday) Color(0xFF2E7D32) else Color(0xFFE65100),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isToday) "Will Ring TODAY at $formattedTime" else "Will Ring TOMORROW at $formattedTime",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isToday) Color(0xFF1B5E20) else Color(0xFFBF360C)
                            )
                            Text(
                                text = "⏰ Scheduled for $countdown • Repeats daily",
                                fontSize = 11.sp,
                                color = if (isToday) Color(0xFF2E7D32) else Color(0xFFE65100)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (currentStep == 1) {
                Button(
                    onClick = { currentStep = 2 },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("time_picker_next_button")
                ) {
                    Text("Next: Select Minutes")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Button(
                    onClick = {
                        val formattedTime24 = "%02d:%02d".format(calculatedHour24, selectedMinute)
                        onConfirm(formattedTime24, calculatedHour24, selectedMinute)
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("time_picker_save_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Daily Alarm")
                }
            }
        },
        dismissButton = {
            if (currentStep == 2) {
                OutlinedButton(
                    onClick = { currentStep = 1 },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back to Hour")
                }
            } else {
                TextButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

/**
 * Clock Face Dial Component
 * Renders an authentic circular clock face (12 positions) with numbers and center hand
 */
@Composable
private fun ClockDialFace(
    items: List<Int>,
    selectedValue: Int,
    displayTransform: (Int) -> String,
    onSelect: (Int) -> Unit,
    isHourDial: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)

    Box(
        modifier = modifier
            .size(200.dp)
            .clip(CircleShape)
            .background(surfaceColor)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Calculate the angle of selected item for the hand
        val selectedIndex = items.indexOf(selectedValue).takeIf { it >= 0 } ?: 0
        val angleDeg = (selectedIndex * (360f / items.size)) - 90f
        val angleRad = angleDeg * (PI / 180.0)

        // Draw Clock Hand & Center Pin
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val handRadius = (size.width / 2f) - 24.dp.toPx()
            val endX = center.x + (handRadius * cos(angleRad)).toFloat()
            val endY = center.y + (handRadius * sin(angleRad)).toFloat()

            // Hand Line
            drawLine(
                color = primaryColor,
                start = center,
                end = Offset(endX, endY),
                strokeWidth = 2.5.dp.toPx()
            )

            // Center Pin
            drawCircle(
                color = primaryColor,
                radius = 4.dp.toPx(),
                center = center
            )
        }

        // Place Numbers in a Circle
        items.forEachIndexed { index, value ->
            val itemAngleDeg = (index * (360f / items.size)) - 90f
            val itemAngleRad = itemAngleDeg * (PI / 180.0)
            val dialRadius = 74.0 // dp radius from center

            val offsetX = (dialRadius * cos(itemAngleRad)).roundToInt()
            val offsetY = (dialRadius * sin(itemAngleRad)).roundToInt()

            val isSelected = (value == selectedValue)

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.dp.roundToPx(), offsetY.dp.roundToPx()) }
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) primaryColor else Color.Transparent)
                    .clickable { onSelect(value) }
                    .testTag(if (isHourDial) "hour_chip_$value" else "minute_chip_$value"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayTransform(value),
                    fontSize = if (isHourDial) 13.sp else 11.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
