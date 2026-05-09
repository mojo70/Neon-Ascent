package com.neon.ascent.core.domain

sealed class Goal {
    data class Aspiration(
        val id: String,
        val title: String,
        val description: String,
        val progress: GoalProgress
    ) : Goal()

    data class Mission(
        val id: String,
        val title: String,
        val archetype: String,
        val progress: GoalProgress
    ) : Goal()

    data class Task(
        val id: String,
        val title: String,
        val isCompleted: Boolean
    ) : Goal()

    data class Habit(
        val id: String,
        val title: String,
        val frequency: String, // e.g., "DAILY"
        val progress: GoalProgress
    ) : Goal()
}

data class GoalProgress(
    val current: Float,           // e.g. 0.72 for 72nd percentile
    val target: Float,
    val percentile: Int? = null,  // population norm comparison
    val contributionType: AttributeType
)

enum class AttributeType {
    STRENGTH,
    INTELLIGENCE,
    AGILITY,
    WILLPOWER,
    RECOVERY,
    SOCIAL
}
