package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = YomuPrimaryDark,
    onPrimary = YomuOnPrimaryDark,
    primaryContainer = YomuPrimaryContainerDark,
    onPrimaryContainer = YomuOnPrimaryContainerDark,
    secondary = YomuSecondaryDark,
    onSecondary = YomuOnSecondaryDark,
    secondaryContainer = YomuSecondaryContainerDark,
    onSecondaryContainer = YomuOnSecondaryContainerDark,
    tertiary = YomuTertiaryDark,
    background = YomuBackgroundDark,
    surface = YomuSurfaceDark,
    surfaceVariant = YomuSurfaceVariantDark,
    onSurface = YomuOnSurfaceDark,
    onSurfaceVariant = YomuOnSurfaceVariantDark,
    outline = YomuOutlineDark,
    outlineVariant = YomuOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = YomuPrimaryLight,
    onPrimary = YomuOnPrimaryLight,
    primaryContainer = YomuPrimaryContainerLight,
    onPrimaryContainer = YomuOnPrimaryContainerLight,
    secondary = YomuSecondaryLight,
    onSecondary = YomuOnSecondaryLight,
    secondaryContainer = YomuSecondaryContainerLight,
    onSecondaryContainer = YomuOnSecondaryContainerLight,
    tertiary = YomuTertiaryLight,
    background = YomuBackgroundLight,
    surface = YomuSurfaceLight,
    surfaceVariant = YomuSurfaceVariantLight,
    onSurface = YomuOnSurfaceLight,
    onSurfaceVariant = YomuOnSurfaceVariantLight,
    outline = YomuOutlineLight,
    outlineVariant = YomuOutlineLight
)

@Composable
fun YomuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve crafted editorial identity
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    YomuTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
