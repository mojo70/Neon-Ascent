package com.neon.ascent.core.domain.goals.models

import com.neon.ascent.core.domain.model.SpecialType
import java.time.Instant
import java.time.LocalDate

sealed class Goal {
    abstract val id: String
    abstract val title: String
    abstract val description: String
    abstract val linkedAttributes: List<SpecialType>
    abstract val progress: GoalProgress
}

data class Aspiration(
    override val id: String,
    override val title: String,
    override val description: String,
    val targetDate: LocalDate?,
    override val linkedAttributes: List<SpecialType>,
    override val progress: GoalProgress,
    val status: GoalStatus = GoalStatus.ACTIVE
) : Goal()

data class Mission(
    override val id: String,
    override val title: String,
    override val description: String,
    val expiresAt: Instant,
    override val linkedAttributes: List<SpecialType>,
    override val progress: GoalProgress,
    val parentAspirationId: String?
) : Goal()

data class Habit(
    override val id: String,
    override val title: String,
    override val description: String = "",
    val recurrence: Recurrence,
    override val linkedAttributes: List<SpecialType>,
    override val progress: GoalProgress,
    val streak: Int = 0,
    val lastCompleted: Instant? = null
) : Goal()

data class Task( // atomic one-off inside missions/habits
    override val id: String,
    override val title: String,
    override val description: String = "",
    override val linkedAttributes: List<SpecialType> = emptyList(),
    override val progress: GoalProgress,
    val parentGoalId: String
) : Goal()

data class GoalProgress(
    val current: Float,           // 0.0 - 1.0
    val target: Float,
    val percentile: Int? = null,
    val xpContributed: Long = 0L
)

enum class GoalStatus { ACTIVE, COMPLETED, FAILED }

data class Recurrence(
    val type: RecurrenceType,
    val daysOfWeek: Set<java.time.DayOfWeek> = emptySet() // for custom
)

enum class RecurrenceType { DAILY, WEEKLY, CUSTOM }

data class CompletionData(
    val timestamp: Instant = Instant.now(),
    val progressDelta: Float = 1f,
    val attributeContributions: Map<SpecialType, Long> = emptyMap(),
    val metadata: Map<String, String> = emptyMap()
)
