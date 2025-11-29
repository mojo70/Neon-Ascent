package com.neon.ascent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NeonScheme = darkColorScheme(
    primary = Color(0xFF00FFFF),
    secondary = Color(0xFFAA00FF),
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF1C1C1C),
    onPrimary = Color.Black,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
)

@Composable
fun NeonAscentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NeonScheme,
        typography = Typography,
        content = content
    )
}
