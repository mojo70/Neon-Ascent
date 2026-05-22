package com.neon.ascent.core.common

enum class CelebrationLevel { SUBTLE, SYNC, ASCENSION }

data class DopamineEvent(
    val level: CelebrationLevel,
    val message: String? = null,
    val xpGained: Int = 0
)
