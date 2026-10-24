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

private val MacricColorScheme = darkColorScheme(
    primary = MacricViolet,
    onPrimary = MacricTextPrimary,
    primaryContainer = MacricVioletDark,
    onPrimaryContainer = MacricTextPrimary,
    secondary = MacricMagenta,
    onSecondary = MacricTextPrimary,
    tertiary = MacricCyan,
    background = MacricBlack,
    onBackground = MacricTextPrimary,
    surface = MacricDarkSurface,
    onSurface = MacricTextPrimary,
    surfaceVariant = MacricDarkCard,
    onSurfaceVariant = MacricTextMuted,
    outline = MacricBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = MacricColorScheme,
        typography = Typography,
        content = content
    )
}
