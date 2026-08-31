package com.neon.ascent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.neon.ascent.core.common.LocalNeonTheme
import com.neon.ascent.core.common.LocalVisualMode
import com.neon.ascent.core.common.LocalGlowEnabled
import com.neon.ascent.core.common.LocalIntensity
import com.neon.ascent.core.common.VisualMode
import com.neon.ascent.core.common.NeonThemeData
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

private val SteveScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = Color.Black,
    onSecondary = Color.White,
    background = Color.White,
    surface = Color(0xFFF5F5F5),
    onBackground = Color.Black,
    onSurface = Color.Black,
    error = Color.Black,
    outline = Color(0xFFDDDDDD)
)

@Composable
fun NeonAscentTheme(
    visualMode: VisualMode = VisualMode.CYBER,
    intensity: Float = 0.8f,
    content: @Composable () -> Unit
) {
    val colorScheme = when (visualMode) {
        VisualMode.CYBER -> NeonScheme
        VisualMode.STEVE -> SteveScheme
    }

    val neonThemeData = when (visualMode) {
        VisualMode.CYBER -> NeonThemeData.cyber(intensity)
        VisualMode.STEVE -> NeonThemeData.steve(intensity)
    }

    CompositionLocalProvider(
        LocalNeonTheme provides neonThemeData,
        LocalVisualMode provides visualMode,
        LocalGlowEnabled provides neonThemeData.glowEnabled,
        LocalIntensity provides intensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
