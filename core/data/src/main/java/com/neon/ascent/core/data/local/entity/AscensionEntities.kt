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
    val archetypeTag: String?,
    val status: String,
    val targetEndDate: LocalDate?,
    val currentProgress: Float,
    val totalXPContributed: Long,
    val notes: String?,
    val createdAt: Instant
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
    val deadline: Instant?,
    val progress: Float,
    val xpPool: Int,
    val status: String,
    val aiGenerated: Boolean,
    val mentorModeEnabled: Boolean,
    val isRecovery: Boolean
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
    val recurrenceType: String?,
    val recurrenceCron: String?,
    val recurrenceIntervalDays: Int?,
    val recurrenceDaysOfWeek: String?, // Comma separated days
    val timeWindows: String?, // Comma separated windows
    val adaptiveWakeEnabled: Boolean,
    val reminderEnabled: Boolean,
    val xpValue: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val graceBufferDays: Int,
    val lastCompleted: Instant?,
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
