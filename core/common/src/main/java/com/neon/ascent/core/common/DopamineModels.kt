package com.neon.ascent.core.common

enum class CelebrationLevel { 
    SUBTLE, 
    SYNC, 
    ASCENSION,
    STREAK_RECOVERY,
    MISSION_COMPLETE,
    DIRECTIVE_MILESTONE
}

data class DopamineEvent(
    val level: CelebrationLevel,
    val message: String? = null,
    val xpGained: Int = 0,
    val streakCount: Int = 0,
    val isGraceRecovery: Boolean = false,
    val directiveProgress: Float = 0f,
    val actionLabel: String? = null
)
