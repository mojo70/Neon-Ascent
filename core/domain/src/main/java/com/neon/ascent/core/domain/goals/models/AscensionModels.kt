package com.neon.ascent.core.domain.goals.models

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

data class AscensionDirective(
    val id: String,
    val title: String,
    val description: String,
    val archetypeTag: String? = null,
    val status: DirectiveStatus = DirectiveStatus.ACTIVE,
    val targetEndDate: LocalDate? = null,
    val currentProgress: Float = 0f,
    val totalXPContributed: Long = 0L,
    val notes: String? = null,
    val createdAt: Instant = Instant.now()
)

enum class DirectiveStatus { ACTIVE, PAUSED, COMPLETED }

data class AscensionMission(
    val id: String,
    val directiveId: String?,
    val title: String,
    val description: String,
    val deadline: Instant? = null,
    val progress: Float = 0f,
    val xpPool: Int = 100,
    val status: AscensionMissionStatus = AscensionMissionStatus.ACTIVE,
    val aiGenerated: Boolean = false,
    val mentorModeEnabled: Boolean = false,
    val isRecovery: Boolean = false
)

enum class AscensionMissionStatus { ACTIVE, COMPLETED, FAILED }

data class AscensionTask(
    val id: String,
    val parentId: String?, // Mission or Directive id
    val title: String,
    val description: String,
    val type: AscensionTaskType = AscensionTaskType.ONE_TIME,
    val recurrence: RecurrenceV3? = null,
    val timeWindows: List<String> = emptyList(), // e.g. ["within 60min of wake", "20:00"]
    val adaptiveWakeEnabled: Boolean = false,
    val reminderEnabled: Boolean = true,
    val xpValue: Int = 10,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val graceBufferDays: Int = 1,
    val lastCompleted: Instant? = null,
    val userNotesTemplate: String? = null
)

enum class AscensionTaskType { RECURRING, ONE_TIME }

data class RecurrenceV3(
    val type: RecurrenceTypeV3,
    val cron: String? = null,
    val intervalDays: Int? = null,
    val daysOfWeek: Set<DayOfWeek> = emptySet()
)

enum class RecurrenceTypeV3 { DAILY, WEEKDAYS, DAYS_OF_WEEK, CUSTOM, INTERVAL }

data class AscensionTaskCompletion(
    val taskId: String,
    val timestamp: Instant = Instant.now(),
    val notes: String? = null,
    val mood: Int? = null, // 1-5
    val linkedHealthSnapshot: String? = null // ID or JSON of health data
)

data class NeuralLog(
    val id: Long = 0,
    val timestamp: Instant,
    val type: String,
    val title: String,
    val content: String,
    val metadata: String? = null
)
