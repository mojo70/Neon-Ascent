package com.neon.ascent.core.common

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class NeonThemeData(
    val canvas: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val accent: Color,
    val accentDanger: Color,
    val hairline: Color,
    val grid: Color,
    val overlay: Color,
    val glowEnabled: Boolean,
    val intensity: Float,
    val mode: VisualMode
) {
    val primary: Color get() = accent
    val secondary: Color get() = accentDanger
    val textPrimary: Color get() = ink
    val textSecondary: Color get() = inkMuted
    val background: Color get() = canvas

    fun accentFor(megacorpId: String): Color {
        if (mode == VisualMode.STEVE) return ink
        
        return when (megacorpId) {
            "aetherx" -> Color(0xFF00FFFF)
            "panopticon" -> Color(0xFFFF006E)
            "microhard" -> Color(0xFF0088FF)
            "obsidianveil" -> Color(0xFFA020F0)
            "omnisight" -> Color(0xFFB0B0B0)
            "helixspace" -> Color(0xFF00FF9C)
            "kagami" -> Color(0xFFFF3131)
            "neobank" -> Color(0xFFFFD700)
            "vitasynth" -> Color(0xFF00FF9F)
            "securacorp" -> Color(0xFF555555)
            "aegis" -> Color(0xFFFF8C00)
            "mojotyger", "mojotygerdynamics" -> Color(0xFFFF5F1F)
            "netwatch" -> Color(0xFFFF0055)
            else -> primary
        }
    }

    companion object {
        fun cyber(intensity: Float = 0.8f) = NeonThemeData(
            canvas = Color(0xFF050505),
            surface = Color(0xFF0F0F0F),
            surfaceRaised = Color(0xFF1A1A1A),
            ink = Color(0xFF00FF9C),
            inkMuted = Color(0xFF5A6E78),
            inkFaint = Color(0xFF1A262E),
            accent = Color(0xFF00FF9C),
            accentDanger = Color(0xFFFF006E),
            hairline = Color(0xFF00FF9C).copy(alpha = 0.2f),
            grid = Color(0xFF1A1A1A),
            overlay = Color.Black.copy(alpha = 0.85f),
            glowEnabled = true,
            intensity = intensity,
            mode = VisualMode.CYBER
        )

        fun steve(intensity: Float = 0.8f) = NeonThemeData(
            canvas = Color(0xFFFFFFFF),
            surface = Color(0xFFF5F5F5),
            surfaceRaised = Color(0xFFEEEEEE),
            ink = Color(0xFF000000),
            inkMuted = lerp(Color(0xFF666666), Color(0xFF1C1C1C), intensity),
            inkFaint = Color(0xFFE0E0E0),
            accent = Color(0xFF000000),
            accentDanger = Color(0xFF000000),
            hairline = Color(0xFFDDDDDD),
            grid = Color(0xFFF0F0F0),
            overlay = Color.White.copy(alpha = 0.9f),
            glowEnabled = false,
            intensity = intensity,
            mode = VisualMode.STEVE
        )

        private fun lerp(start: Color, stop: Color, fraction: Float): Color {
            return Color(
                red = start.red + (stop.red - start.red) * fraction,
                green = start.green + (stop.green - start.green) * fraction,
                blue = start.blue + (stop.blue - start.blue) * fraction,
                alpha = start.alpha + (stop.alpha - start.alpha) * fraction
            )
        }
    }
}

val LocalNeonTheme = staticCompositionLocalOf { NeonThemeData.cyber() }
