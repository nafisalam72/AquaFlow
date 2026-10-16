package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = OceanBlueLight,
    onPrimary = IcySurfaceLight,
    primaryContainer = IcySurfaceVariantLight,
    onPrimaryContainer = OceanBlueLightHover,
    secondary = AquaCyanLight,
    onSecondary = IcySurfaceLight,
    secondaryContainer = IcySurfaceVariantLight,
    onSecondaryContainer = OceanBlueLightHover,
    background = IcyBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = IcySurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = IcySurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = IcyOutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = OceanBlueDark,
    onPrimary = DeepNavyDark,
    primaryContainer = OceanBlueDarkContainer,
    onPrimaryContainer = IcySurfaceVariantLight,
    secondary = AquaCyanDark,
    onSecondary = DeepNavyDark,
    secondaryContainer = ElevatedSurfaceVariantDark,
    onSecondaryContainer = AquaCyanDark,
    background = DeepNavyDark,
    onBackground = TextPrimaryDark,
    surface = ElevatedSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = ElevatedSurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark
)

@Composable
fun AquaFlowTheme(
    themeSetting: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    content: @Composable () -> Unit
) {
    val isDark = when (themeSetting) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = colorScheme.background.toArgb()
                it.navigationBarColor = colorScheme.surface.toArgb()
                val insetsController = WindowCompat.getInsetsController(it, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
