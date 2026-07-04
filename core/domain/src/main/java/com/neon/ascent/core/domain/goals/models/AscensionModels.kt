package com.neon.ascent.core.domain.goals.models

import com.neon.ascent.core.domain.model.SpecialType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

data class AscensionDirective(
    val id: String,
    val title: String,
    val description: String,
    val visionStatement: String? = null,
    val status: DirectiveStatus = DirectiveStatus.ACTIVE,
    val targetEndDate: LocalDate? = null,
    val isQuarterly: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val archivedAt: Instant? = null,
    val currentProgress: Float = 0f,
    val totalXPContributed: Long = 0L,
    val xpTarget: Long? = null,
    val archetypeTag: String? = null,
    val tags: List<String> = emptyList(),
    val linkedAttributes: List<SpecialType> = emptyList(),
    val aiMentorMode: MentorMode = MentorMode.REVIEW,
    val aiGenerated: Boolean = false,
    val notes: String? = null,
    val completionHistorySummary: String? = null,
    val lastReviewDate: LocalDate? = null
)

enum class DirectiveStatus { ACTIVE, PAUSED, COMPLETED, ARCHIVED }

enum class MentorMode { REVIEW, SOUNDING_BOARD, GUIDE }

data class AscensionMission(
    val id: String,
    val directiveId: String?,
    val title: String,
    val description: String,
    val objective: String? = null,
    val status: AscensionMissionStatus = AscensionMissionStatus.ACTIVE,
    val startDate: LocalDate = LocalDate.now(),
    val targetEndDate: LocalDate? = null,
    val createdAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
    val archivedAt: Instant? = null,
    val progress: Float = 0f,
    val totalXPContributed: Long = 0L,
    val xpTarget: Long? = null,
    val aiMentorMode: MentorMode = MentorMode.REVIEW,
    val aiGenerated: Boolean = false,
    val notes: String? = null,
    val successCriteria: String? = null,
    val completionHistorySummary: String? = null,
    val tags: List<String> = emptyList(),
    val linkedAttributes: List<SpecialType> = emptyList(),
    val linkedArchetype: String? = null
)

enum class AscensionMissionStatus { ACTIVE, PAUSED, COMPLETED, ARCHIVED, RECOVERY }

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
    val linkedAttributes: List<SpecialType> = emptyList(),
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

data class MentorUiMessage(
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val proposedMissions: List<ProposedMission> = emptyList()
)

data class ProposedMission(
    val title: String,
    val description: String,
    val tasks: List<ProposedTask> = emptyList()
)

data class ProposedTask(
    val title: String,
    val description: String,
    val type: AscensionTaskType,
    val recurrence: RecurrenceV3? = null,
    val timeWindows: List<String> = emptyList(),
    val linkedAttributes: List<SpecialType> = emptyList()
)
