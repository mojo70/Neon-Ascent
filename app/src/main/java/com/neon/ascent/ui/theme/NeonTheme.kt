package com.neon.ascent.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class NeonThemeData(
    val primary: Color = Color(0xFF00FF9C),
    val secondary: Color = Color(0xFFFF006E),
    val accent: Color = Color(0xFF00FFFF),
    val textPrimary: Color = Color.White,
    val textSecondary: Color = Color.Gray,
    val background: Color = Color(0xFF050505)
) {
    fun accentFor(megacorpId: String): Color {
        return when (megacorpId) {
            "aetherx" -> Color(0xFF00FFFF)
            "panopticon" -> Color(0xFFFF006E)
            "microhard" -> Color(0xFF0088FF)
            "obsidianveil" -> Color(0xFFA020F0) // Deep Purple
            "omnisight" -> Color(0xFFB0B0B0) // Cold Steel
            "helixspace" -> Color(0xFF00FF9C) // Emerald Green
            "kagami" -> Color(0xFFFF3131) // Crimson Red
            "neobank" -> Color(0xFFFFD700) // Gold
            "vitasynth" -> Color(0xFF00FF9F) // Neon Green / Vital
            "securacorp" -> Color(0xFF555555) // Iron Gray
            "aegis" -> Color(0xFFFF8C00) // Dark Orange / Forge
            else -> primary
        }
    }
}

val LocalNeonTheme = staticCompositionLocalOf { NeonThemeData() }
