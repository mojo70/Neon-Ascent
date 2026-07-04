package com.neon.ascent.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "ascension_directives",
    indices = [Index("status")]
)
data class AscensionDirectiveEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val visionStatement: String?,
    val status: String,
    val successMetrics: List<com.neon.ascent.core.domain.goals.models.SuccessMetric>,
    val targetEndDate: LocalDate?,
    val isQuarterly: Boolean,
    val createdAt: Instant,
    val archivedAt: Instant?,
    val currentProgress: Float,
    val totalXPContributed: Long,
    val xpTarget: Long?,
    val archetypeTag: String?,
    val tags: List<String>,
    val linkedAttributes: List<String>,
    val aiMentorMode: String,
    val aiGenerated: Boolean,
    val notes: String?,
    val completionHistorySummary: String?,
    val lastReviewDate: LocalDate?
)

@Entity(
    tableName = "ascension_missions",
    foreignKeys = [
        ForeignKey(
            entity = AscensionDirectiveEntity::class,
            parentColumns = ["id"],
            childColumns = ["directiveId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("directiveId"), Index("status")]
)
data class AscensionMissionEntity(
    @PrimaryKey val id: String,
    val directiveId: String?,
    val title: String,
    val description: String,
    val objective: String?,
    val status: String,
    val startDate: LocalDate,
    val targetEndDate: LocalDate?,
    val createdAt: Instant,
    val completedAt: Instant?,
    val archivedAt: Instant?,
    val progress: Float,
    val contributionWeight: Float,
    val totalXPContributed: Long,
    val xpTarget: Long?,
    val aiMentorMode: String,
    val aiGenerated: Boolean,
    val notes: String?,
    val successCriteria: String?,
    val completionHistorySummary: String?,
    val tags: List<String>,
    val linkedAttributes: List<String>,
    val linkedArchetype: String?
)

@Entity(
    tableName = "ascension_tasks",
    indices = [Index("parentId"), Index("type")]
)
data class AscensionTaskEntity(
    @PrimaryKey val id: String,
    val parentId: String?, // Mission or Directive id
    val title: String,
    val description: String,
    val type: String,
    val isPulse: Boolean,
    val recurrenceType: String?,
    val recurrenceCron: String?,
    val recurrenceIntervalDays: Int?,
    val recurrenceDaysOfWeek: String?, // Comma separated days
    val timeWindows: String?, // Comma separated windows
    val adaptiveWakeEnabled: Boolean,
    val reminderEnabled: Boolean,
    val xpValue: Int,
    val impactWeight: Float,
    val currentStreak: Int,
    val longestStreak: Int,
    val graceBufferDays: Int,
    val lastCompleted: Instant?,
    val linkedAttributes: List<String> = emptyList(),
    val userNotesTemplate: String?
)

@Entity(
    tableName = "ascension_task_completions",
    foreignKeys = [
        ForeignKey(
            entity = AscensionTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId")]
)
data class AscensionTaskCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: String,
    val timestamp: Instant,
    val notes: String?,
    val mood: Int?,
    val linkedHealthSnapshot: String?
)

@Entity(
    tableName = "neural_logs",
    indices = [Index("type")]
)
data class NeuralLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Instant,
    val type: String, // RITUAL_SYNTHESIS, SYSTEM_ALERT, etc.
    val title: String,
    val content: String,
    val metadata: String? = null
)
