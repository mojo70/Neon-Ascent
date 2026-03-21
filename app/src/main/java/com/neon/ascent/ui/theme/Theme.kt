package com.neon.ascent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NeonScheme = darkColorScheme(
    primary = Color(0xFF00FF9C), // Matrix green
    secondary = Color(0xFFFF006E), // Cyberpink
    background = Color(0xFF050505), // Near black
    surface = Color(0xFF0F0F0F), // Dark gray
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFF00FF9C), // Matrix green text for labels
    onSurface = Color(0xFFE0E0E0), // High visibility light gray for input text
    error = Color(0xFFFF3131),
    outline = Color(0xFF39FF14) // Neon outline
)

@Composable
fun NeonAscentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NeonScheme,
        typography = Typography,
        content = content
    )
}
