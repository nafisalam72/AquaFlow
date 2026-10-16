package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun AnimatedWaterGlass(
    consumedMl: Int,
    dailyGoalMl: Int,
    modifier: Modifier = Modifier,
    unit: String = "ml",
    lastAddedAmount: Int? = null,
    lastAddedTimestamp: Long = 0L
) {
    val rawProgress = if (dailyGoalMl > 0) consumedMl.toFloat() / dailyGoalMl.toFloat() else 0f
    val clampedProgress = rawProgress.coerceIn(0f, 1f)

    // Percentage readout capped at 100% max as requested by user
    val percentageInt = if (dailyGoalMl > 0) {
        ((consumedMl.toFloat() / dailyGoalMl.toFloat()) * 100).roundToInt().coerceIn(0, 100)
    } else 0

    // Smooth transition for water level height
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "waterLevelAnimation"
    )

    // Dynamic splash animation when water is added
    val splashAnim = remember { Animatable(0f) }
    var displayedAddedAmount by remember { mutableIntStateOf(0) }
    var isAnimatingAddition by remember { mutableStateOf(false) }

    LaunchedEffect(lastAddedTimestamp) {
        if (lastAddedTimestamp > 0L && lastAddedAmount != null && lastAddedAmount > 0) {
            displayedAddedAmount = lastAddedAmount
            isAnimatingAddition = true
            splashAnim.snapTo(0f)
            splashAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1800, easing = FastOutSlowInEasing)
            )
            isAnimatingAddition = false
        }
    }

    // Infinite wave transitions
    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase1"
    )

    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue = (2 * PI).toFloat(),
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase2"
    )

    // Gentle bubble rising offset
    val bubbleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubbleOffset"
    )

    val isDark = isSystemInDarkTheme()
    val glassStrokeColor = if (isDark) Color(0x66FFFFFF) else Color(0x4D0284C7)
    val glassFillColor = if (isDark) Color(0x1A0284C7) else Color(0x0D0284C7)

    Box(
        modifier = modifier
            .size(width = 240.dp, height = 280.dp)
            .testTag("animated_water_glass"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Glass tumbler geometry
            val topWidth = width * 0.86f
            val bottomWidth = width * 0.68f
            val topMargin = height * 0.08f
            val bottomMargin = height * 0.92f

            val topLeftX = (width - topWidth) / 2f
            val topRightX = topLeftX + topWidth
            val bottomLeftX = (width - bottomWidth) / 2f
            val bottomRightX = bottomLeftX + bottomWidth

            // Glass container interior path
            val glassInteriorPath = Path().apply {
                moveTo(topLeftX, topMargin)
                lineTo(bottomLeftX, bottomMargin - 16f)
                // Rounded bottom corners
                quadraticTo(bottomLeftX, bottomMargin, bottomLeftX + 16f, bottomMargin)
                lineTo(bottomRightX - 16f, bottomMargin)
                quadraticTo(bottomRightX, bottomMargin, bottomRightX, bottomMargin - 16f)
                lineTo(topRightX, topMargin)
                close()
            }

            // Draw glass background tint
            drawPath(
                path = glassInteriorPath,
                color = glassFillColor
            )

            // Draw water clipped inside the glass
            clipPath(glassInteriorPath) {
                if (animatedProgress > 0.005f) {
                    val glassUsableHeight = bottomMargin - topMargin
                    val waterSurfaceY = bottomMargin - (glassUsableHeight * animatedProgress)

                    // Splash surge enhances wave amplitude when water is poured in
                    val splashSurge = if (isAnimatingAddition) (1f - splashAnim.value) * 16f else 0f
                    val waveAmplitude = (10f + splashSurge) * (1f - (clampedProgress - 0.5f) * (clampedProgress - 0.5f) * 2f).coerceAtLeast(0.4f)

                    // 1. Back wave (lighter cyan)
                    val backWavePath = Path().apply {
                        moveTo(0f, height)
                        lineTo(0f, waterSurfaceY)
                        val step = 12f
                        var x = 0f
                        while (x <= width) {
                            val y = waterSurfaceY + waveAmplitude * 0.8f * sin((x / width * 2 * PI + wavePhase2).toFloat())
                            lineTo(x, y)
                            x += step
                        }
                        lineTo(width, height)
                        close()
                    }

                    drawPath(
                        path = backWavePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xCC38BDF8),
                                Color(0xEE0284C7)
                            ),
                            startY = waterSurfaceY - 15f,
                            endY = bottomMargin
                        )
                    )

                    // 2. Front wave (rich vibrant ocean blue)
                    val frontWavePath = Path().apply {
                        moveTo(0f, height)
                        lineTo(0f, waterSurfaceY)
                        val step = 10f
                        var x = 0f
                        while (x <= width) {
                            val y = waterSurfaceY + waveAmplitude * sin((x / width * 2 * PI + wavePhase1).toFloat())
                            lineTo(x, y)
                            x += step
                        }
                        lineTo(width, height)
                        close()
                    }

                    drawPath(
                        path = frontWavePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF00B4D8),
                                Color(0xFF0077B6),
                                Color(0xFF03045E)
                            ),
                            startY = waterSurfaceY - 10f,
                            endY = bottomMargin
                        )
                    )

                    // Floating sparkling bubbles
                    val bubbleX1 = bottomLeftX + (bottomRightX - bottomLeftX) * 0.35f
                    val bubbleX2 = bottomLeftX + (bottomRightX - bottomLeftX) * 0.65f
                    val bubbleRange = (bottomMargin - waterSurfaceY).coerceAtLeast(10f)

                    val bY1 = bottomMargin - (bubbleOffset * bubbleRange)
                    val bY2 = bottomMargin - (((bubbleOffset + 0.5f) % 1f) * bubbleRange)

                    if (bY1 > waterSurfaceY + 10f) {
                        drawCircle(
                            color = Color(0x80FFFFFF),
                            radius = 4f,
                            center = Offset(bubbleX1 + sin(bubbleOffset * 10f) * 6f, bY1)
                        )
                    }
                    if (bY2 > waterSurfaceY + 10f) {
                        drawCircle(
                            color = Color(0x60FFFFFF),
                            radius = 3f,
                            center = Offset(bubbleX2 - sin(bubbleOffset * 8f) * 5f, bY2)
                        )
                    }
                }
            }

            // Glass specular highlight reflection on the left wall
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x00FFFFFF),
                        Color(0x55FFFFFF),
                        Color(0x22FFFFFF),
                        Color(0x00FFFFFF)
                    ),
                    startY = topMargin + 10f,
                    endY = bottomMargin - 20f
                ),
                start = Offset(topLeftX + 8f, topMargin + 14f),
                end = Offset(bottomLeftX + 6f, bottomMargin - 16f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )

            // Outer glass outline and rim
            drawPath(
                path = glassInteriorPath,
                color = glassStrokeColor,
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )

            // Glass top rim oval highlight
            drawOval(
                color = glassStrokeColor,
                topLeft = Offset(topLeftX, topMargin - 4f),
                size = androidx.compose.ui.geometry.Size(topWidth, 8f),
                style = Stroke(width = 2.5f)
            )

            // Glass bottom base weight
            drawLine(
                color = glassStrokeColor,
                start = Offset(bottomLeftX + 8f, bottomMargin + 2f),
                end = Offset(bottomRightX - 8f, bottomMargin + 2f),
                strokeWidth = 3.5f,
                cap = StrokeCap.Round
            )
        }

        // Center readout overlay on the glass
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$percentageInt%",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (animatedProgress > 0.45f) Color.White else MaterialTheme.colorScheme.onBackground
            )

            val displayConsumed = if (unit == "fl_oz") {
                "%.1f oz".format(consumedMl * 0.033814f)
            } else {
                "%,d ml".format(consumedMl)
            }

            Text(
                text = displayConsumed,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (animatedProgress > 0.55f) Color(0xFFE0F7FA) else MaterialTheme.colorScheme.primary
            )

            val displayGoal = if (unit == "fl_oz") {
                "Goal: %.1f oz".format(dailyGoalMl * 0.033814f)
            } else {
                "Goal: %,d ml".format(dailyGoalMl)
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = displayGoal,
                fontSize = 12.sp,
                color = if (animatedProgress > 0.65f) Color(0xB3FFFFFF) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Floating +X ml water splash animation badge inside the water bucket
        if (isAnimatingAddition && displayedAddedAmount > 0) {
            val progress = splashAnim.value
            val floatOffsetY = ((1f - progress) * 35f - progress * 95f).roundToInt()
            val scaleVal = if (progress < 0.25f) 0.65f + (progress / 0.25f) * 0.5f else 1.15f - (progress - 0.25f) * 0.2f
            val alphaVal = if (progress < 0.65f) 1f else ((1f - progress) / 0.35f).coerceIn(0f, 1f)

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0284C7).copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF7DD3FC)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .offset { IntOffset(0, floatOffsetY) }
                    .scale(scaleVal)
                    .alpha(alphaVal)
                    .testTag("water_bucket_addition_badge")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = Color(0xFFE0F2FE),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "+$displayedAddedAmount ml",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
