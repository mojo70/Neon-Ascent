package com.neon.ascent.core.domain.model

import java.time.Instant

data class DopamineMenuItem(
    val id: String,
    val title: String,
    val description: String,
    val durationMinutes: Int,
    val category: DopamineCategory,
    val specialTags: List<SpecialType>,
    val energyLevel: EnergyLevel,
    val lastUsed: Instant? = null,
    val usageCount: Int = 0
)

enum class DopamineCategory {
    RESET,      // 1-5 min: Deep breathing, cold splash
    MOVEMENT,   // Walk, stretch, dance
    CREATIVE,   // Journal, doodle, music
    SENSORY,    // Tea, weighted blanket
    PRODUCTIVE, // Micro-win
    SOCIAL      // Text partner, pet cuddle
}

enum class EnergyLevel {
    LOW,
    MEDIUM,
    HIGH
}
