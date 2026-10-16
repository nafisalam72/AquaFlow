package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailySummary
import com.example.data.WaterEntry
import com.example.ui.AquaFlowUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class DayGroupedHydration(
    val dateKey: String, // "yyyy-MM-dd"
    val dateDisplay: String, // localized label
    val totalMl: Int,
    val count: Int,
    val entries: List<WaterEntry>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    uiState: AquaFlowUiState,
    onDeleteEntry: (WaterEntry) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Group all entries dynamically by device local date, respecting timezone offsets
    val groupedDays = remember(uiState.allEntries, uiState.weeklySummaries) {
        val allEntries = uiState.allEntries
        if (allEntries.isNotEmpty()) {
            val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val yesterdayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

            allEntries.groupBy { it.getEffectiveLocalDateString() }
                .map { (dateKey, entriesList) ->
                    val total = entriesList.sumOf { it.amountMl }
                    val sample = entriesList.firstOrNull()
                    val dateDisplay = when (dateKey) {
                        todayKey -> "Today • " + SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
                        yesterdayKey -> "Yesterday • " + (sample?.formatCurrentLocalDate() ?: dateKey)
                        else -> sample?.formatCurrentLocalDate() ?: dateKey
                    }
                    DayGroupedHydration(
                        dateKey = dateKey,
                        dateDisplay = dateDisplay,
                        totalMl = total,
                        count = entriesList.size,
                        entries = entriesList.sortedByDescending { it.utcTimestampMillis }
                    )
                }.sortedByDescending { it.dateKey }
        } else {
            // Fallback to summaries if allEntries not yet loaded
            uiState.weeklySummaries.map { summary ->
                DayGroupedHydration(
                    dateKey = summary.dateString,
                    dateDisplay = summary.dateString,
                    totalMl = summary.totalMl,
                    count = summary.count,
                    entries = emptyList()
                )
            }
        }
    }

    // 7-day intake metrics
    val totalWeeklyIntake = remember(groupedDays) {
        groupedDays.take(7).sumOf { it.totalMl }
    }
    val avgDailyIntake = remember(groupedDays) {
        if (groupedDays.isNotEmpty()) groupedDays.take(7).map { it.totalMl }.average().toInt() else 0
    }
    val daysGoalMet = remember(groupedDays, uiState.dailyGoalMl) {
        groupedDays.take(7).count { it.totalMl >= uiState.dailyGoalMl }
    }
    val bestDayMl = remember(groupedDays) {
        groupedDays.take(7).maxOfOrNull { it.totalMl } ?: 0
    }

    val currentTzName = remember {
        val tz = TimeZone.getDefault()
        val now = System.currentTimeMillis()
        val offsetHours = tz.getOffset(now) / (60 * 60 * 1000)
        val sign = if (offsetHours >= 0) "+" else ""
        "${tz.id} (UTC$sign$offsetHours)"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header
        item {
            Column {
                Text(
                    text = "Weekly Hydration Summary",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Intake trends with UTC timestamps & local timezone synchronization",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Device Timezone: $currentTzName",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 2. Recharts-style Interactive Chart Card
        item {
            RechartsWeeklyHydrationCard(
                allEntries = uiState.allEntries,
                summaries = uiState.weeklySummaries,
                dailyGoalMl = uiState.dailyGoalMl,
                unit = uiState.preferredUnit
            )
        }

        // 3. Weekly Summary Highlights
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weekly_summary_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "7-Day Total Volume",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatVolume(totalWeeklyIntake, uiState.preferredUnit),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp)
                    ) {
                        Text(
                            text = "Daily Average",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatVolume(avgDailyIntake, uiState.preferredUnit),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // 4. Metric Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Best Day Intake",
                    value = formatVolume(bestDayMl, uiState.preferredUnit),
                    subtitle = "personal weekly high",
                    icon = Icons.Default.Star,
                    iconTint = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Goal Success",
                    value = "$daysGoalMet / 7 Days",
                    subtitle = if (daysGoalMet >= 5) "Great consistency! 🌟" else "Keep hydrating! 💧",
                    icon = Icons.Default.CheckCircle,
                    iconTint = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 5. Past Days Breakdown List with UTC / Timezone records
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Intake Records",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${groupedDays.size} days logged",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (groupedDays.isEmpty()) {
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
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No history recorded yet",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Your daily summaries will be archived here automatically",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            items(groupedDays, key = { it.dateKey }) { dayGroup ->
                DailyGroupedHydrationCard(
                    dayGroup = dayGroup,
                    dailyGoalMl = uiState.dailyGoalMl,
                    unit = uiState.preferredUnit,
                    onDeleteEntry = onDeleteEntry
                )
            }
        }
    }
}

data class ChartDayItem(
    val dateKey: String,
    val dayAbbr: String,
    val fullDateLabel: String,
    val isToday: Boolean,
    val amountMl: Int,
    val drinkCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RechartsWeeklyHydrationCard(
    allEntries: List<WaterEntry>,
    summaries: List<DailySummary>,
    dailyGoalMl: Int,
    unit: String
) {
    var chartMode by remember { mutableStateOf("BAR") } // "BAR" or "AREA"
    var selectedDayIndex by remember { mutableIntStateOf(6) } // Defaults to Today (index 6)

    // Build the 7-day chronological dataset respecting current device timezone
    val past7DaysData = remember(allEntries, summaries) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayAbbrFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val fullDateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

        val dayGroupMap = allEntries.groupBy { it.getEffectiveLocalDateString() }

        (6 downTo 0).map { daysAgo ->
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            val dateKey = sdf.format(cal.time)
            val matchedEntries = dayGroupMap[dateKey] ?: emptyList()
            val totalMl = if (matchedEntries.isNotEmpty()) {
                matchedEntries.sumOf { it.amountMl }
            } else {
                summaries.firstOrNull { it.dateString == dateKey }?.totalMl ?: 0
            }
            val count = if (matchedEntries.isNotEmpty()) matchedEntries.size else {
                summaries.firstOrNull { it.dateString == dateKey }?.count ?: 0
            }

            ChartDayItem(
                dateKey = dateKey,
                dayAbbr = if (daysAgo == 0) "Today" else dayAbbrFormat.format(cal.time),
                fullDateLabel = if (daysAgo == 0) "Today (${fullDateFormat.format(cal.time)})" else fullDateFormat.format(cal.time),
                isToday = (daysAgo == 0),
                amountMl = totalMl,
                drinkCount = count
            )
        }
    }

    val selectedItem = past7DaysData.getOrElse(selectedDayIndex) { past7DaysData.last() }
    val maxVal = remember(past7DaysData, dailyGoalMl) {
        val maxRecorded = past7DaysData.maxOfOrNull { it.amountMl } ?: 0
        (maxOf(maxRecorded, dailyGoalMl) * 1.15f).toInt().coerceAtLeast(1000)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recharts_weekly_chart_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header with Chart Type Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hydration Trend",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Tap any day to inspect details",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Bar vs Area Segmented Button
                SingleChoiceSegmentedButtonRow(modifier = Modifier.width(150.dp)) {
                    SegmentedButton(
                        selected = chartMode == "BAR",
                        onClick = { chartMode = "BAR" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Bars",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    SegmentedButton(
                        selected = chartMode == "AREA",
                        onClick = { chartMode = "AREA" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Trend Area",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Recharts-Style Interactive Tooltip Box
            RechartsTooltipCard(
                item = selectedItem,
                dailyGoalMl = dailyGoalMl,
                unit = unit
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Chart Canvas Section
            if (chartMode == "BAR") {
                RechartsBarCanvas(
                    data = past7DaysData,
                    dailyGoalMl = dailyGoalMl,
                    maxVal = maxVal,
                    selectedIndex = selectedDayIndex,
                    onSelectIndex = { selectedDayIndex = it }
                )
            } else {
                RechartsAreaCanvas(
                    data = past7DaysData,
                    dailyGoalMl = dailyGoalMl,
                    maxVal = maxVal,
                    selectedIndex = selectedDayIndex,
                    onSelectIndex = { selectedDayIndex = it }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Goal Reference Line Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Target Goal Line: ${formatVolume(dailyGoalMl, unit)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RechartsTooltipCard(
    item: ChartDayItem,
    dailyGoalMl: Int,
    unit: String
) {
    val percent = ((item.amountMl.toFloat() / dailyGoalMl.coerceAtLeast(1)) * 100).toInt().coerceIn(0, 100)
    val isMet = item.amountMl >= dailyGoalMl
    val diffMl = item.amountMl - dailyGoalMl

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isMet) Color(0xFF10B981) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = item.fullDateLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (item.drinkCount > 0) "${item.drinkCount} drinks recorded" else "No intake logged",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = if (isMet) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatVolume(item.amountMl, unit),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = if (isMet) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = if (isMet) {
                        "$percent% of goal (+$diffMl ml)"
                    } else {
                        "$percent% of goal (${-diffMl} ml left)"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isMet) Color(0xFF10B981) else MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun RechartsBarCanvas(
    data: List<ChartDayItem>,
    dailyGoalMl: Int,
    maxVal: Int,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Draw Cartesian Grid Lines (50%, 100% max)
                val gridY50 = height * 0.5f
                val gridY100 = height * 0.1f
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                drawLine(
                    color = gridColor,
                    start = Offset(0f, gridY50),
                    end = Offset(width, gridY50),
                    strokeWidth = 1.5f,
                    pathEffect = dashEffect
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, gridY100),
                    end = Offset(width, gridY100),
                    strokeWidth = 1.5f,
                    pathEffect = dashEffect
                )

                // Draw Goal Reference Line (dashed)
                val goalRatio = (dailyGoalMl.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                val goalY = height * (1f - goalRatio)
                drawLine(
                    color = primaryColor,
                    start = Offset(0f, goalY),
                    end = Offset(width, goalY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )

                // Draw Bars
                val count = data.size
                val stepX = width / count
                val barWidth = stepX * 0.52f

                data.forEachIndexed { index, item ->
                    val centerX = stepX * index + stepX / 2f
                    val left = centerX - barWidth / 2f
                    val ratio = (item.amountMl.toFloat() / maxVal.toFloat()).coerceIn(0.03f, 1f)
                    val barHeight = height * ratio
                    val top = height - barHeight
                    val isGoalMet = item.amountMl >= dailyGoalMl
                    val isSelected = (index == selectedIndex)

                    // Draw Background Pill Track
                    drawRoundRect(
                        color = Color(0x120284C7),
                        topLeft = Offset(left, 0f),
                        size = Size(barWidth, height),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )

                    // Draw Filled Bar with Gradient
                    val barBrush = if (isGoalMet) {
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF34D399), Color(0xFF059669)),
                            startY = top,
                            endY = height
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(secondaryColor, primaryColor),
                            startY = top,
                            endY = height
                        )
                    }

                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )

                    // Active bar highlight outline
                    if (isSelected) {
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(left - 3f, top - 3f),
                            size = Size(barWidth + 6f, barHeight + 3f),
                            cornerRadius = CornerRadius((barWidth + 6f) / 2f, (barWidth + 6f) / 2f),
                            style = Stroke(width = 3f)
                        )
                    }

                    // Goal met star/check mark dot
                    if (isGoalMet) {
                        drawCircle(
                            color = Color(0xFF10B981),
                            radius = 4.5f,
                            center = Offset(centerX, (top - 8f).coerceAtLeast(6f))
                        )
                    }
                }
            }

            // Invisible gesture layer to handle taps per bar
            Row(modifier = Modifier.fillMaxSize()) {
                data.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onSelectIndex(index) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // X-Axis Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEachIndexed { index, item ->
                val isSelected = (index == selectedIndex)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectIndex(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.dayAbbr,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected || item.isToday) FontWeight.ExtraBold else FontWeight.Normal,
                        color = if (isSelected) primaryColor else if (item.isToday) secondaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RechartsAreaCanvas(
    data: List<ChartDayItem>,
    dailyGoalMl: Int,
    maxVal: Int,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Draw Goal Reference Line
                val goalRatio = (dailyGoalMl.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                val goalY = height * (1f - goalRatio)
                drawLine(
                    color = primaryColor,
                    start = Offset(0f, goalY),
                    end = Offset(width, goalY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )

                // Build Area and Line Paths
                val count = data.size
                val stepX = width / (count - 1).coerceAtLeast(1)

                val points = data.mapIndexed { index, item ->
                    val x = stepX * index
                    val ratio = (item.amountMl.toFloat() / maxVal.toFloat()).coerceIn(0.04f, 1f)
                    val y = height * (1f - ratio)
                    Offset(x, y)
                }

                // Area Fill Path with smooth cubic spline
                val areaPath = Path().apply {
                    moveTo(0f, height)
                    lineTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                    lineTo(width, height)
                    close()
                }

                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.45f),
                            secondaryColor.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = height
                    )
                )

                // Trend Spline Stroke
                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                }

                drawPath(
                    path = linePath,
                    color = primaryColor,
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )

                // Dots at each data point
                points.forEachIndexed { index, pt ->
                    val isSelected = (index == selectedIndex)
                    val isGoalMet = data[index].amountMl >= dailyGoalMl

                    drawCircle(
                        color = Color.White,
                        radius = if (isSelected) 7f else 4.5f,
                        center = pt
                    )
                    drawCircle(
                        color = if (isGoalMet) Color(0xFF10B981) else primaryColor,
                        radius = if (isSelected) 5f else 3f,
                        center = pt
                    )
                }
            }

            // Interactive touch zones
            Row(modifier = Modifier.fillMaxSize()) {
                data.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onSelectIndex(index) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // X-Axis Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEachIndexed { index, item ->
                val isSelected = (index == selectedIndex)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectIndex(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.dayAbbr,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected || item.isToday) FontWeight.ExtraBold else FontWeight.Normal,
                        color = if (isSelected) primaryColor else if (item.isToday) secondaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun DailyGroupedHydrationCard(
    dayGroup: DayGroupedHydration,
    dailyGoalMl: Int,
    unit: String,
    onDeleteEntry: (WaterEntry) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val progress = (dayGroup.totalMl.toFloat() / dailyGoalMl.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val isGoalMet = dayGroup.totalMl >= dailyGoalMl

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isGoalMet) Icons.Default.CheckCircle else Icons.Default.LocalDrink,
                        contentDescription = null,
                        tint = if (isGoalMet) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = dayGroup.dateDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${dayGroup.count} drinks logged • tap to ${if (expanded) "hide" else "view"} drinks",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatVolume(dayGroup.totalMl, unit),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = if (isGoalMet) Color(0xFF10B981) else MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${(progress * 100).toInt().coerceIn(0, 100)}% of goal",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isGoalMet) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Expandable itemized list of drinks with local time and timezone offset
            AnimatedVisibility(visible = expanded && dayGroup.entries.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    )

                    dayGroup.entries.forEach { entry ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = entry.formatCurrentLocalTime(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = entry.getFormattedOffset(),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        if (entry.note.isNotEmpty()) {
                                            Text(
                                                text = entry.note,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "+${formatVolume(entry.amountMl, unit)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onDeleteEntry(entry) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete entry",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier
                                                .padding(4.dp)
                                                .size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
