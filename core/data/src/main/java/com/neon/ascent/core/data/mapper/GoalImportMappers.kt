package com.neon.ascent.core.data.mapper

import com.neon.ascent.core.data.GoalEntity
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.model.SpecialType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

const val V1_IMPORTED_DIRECTIVE_ID = "imported_v1"
const val V1_IMPORTED_DIRECTIVE_TITLE = "IMPORTED_OBJECTIVES"
const val V2_IMPORTED_DIRECTIVE_ID = "imported_v2"

data class V1GoalInput(
    val id: String,
    val title: String,
    val objective: String = "",
    val description: String = "",
    val aspirationLink: String = "",
    val targetValue: Float,
    val currentValue: Float = 0f,
    val unit: String = "hours",
    val deadline: Long? = null,
    val linkedSpecial: SpecialType? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

object GoalImportMappers {

    fun resolveImportId(id: String, prefix: String, exists: (String) -> Boolean = { false }): String {
        return if (exists(id)) "$prefix:$id" else id
    }

    fun calculateProgress(current: Float, target: Float): Float {
        return if (target > 0f && current > 0f) {
            (current / target).coerceIn(0f, 1f)
        } else 0f
    }

    fun calculateProgress(current: Double, target: Double): Float {
        return if (target > 0.0 && current > 0.0) {
            (current / target).toFloat().coerceIn(0f, 1f)
        } else 0f
    }

    // ====================== V1 MAPPING ======================

    fun createV1ImportDirective(): AscensionDirective {
        return AscensionDirective(
            id = V1_IMPORTED_DIRECTIVE_ID,
            title = V1_IMPORTED_DIRECTIVE_TITLE,
            description = "Imported objectives from V1 stack",
            status = DirectiveStatus.ACTIVE
        )
    }

    fun mapV1GoalToAscensionMission(
        v1: V1GoalInput,
        exists: (String) -> Boolean = { false }
    ): AscensionMission {
        val resolvedId = resolveImportId(v1.id, "v1", exists)
        val descriptionText = v1.description.ifBlank { v1.objective }
        val targetDate = v1.deadline?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }
        val calculatedProgress = calculateProgress(v1.currentValue, v1.targetValue)
        val status = if (v1.isActive) AscensionMissionStatus.ACTIVE else AscensionMissionStatus.COMPLETED

        return AscensionMission(
            id = resolvedId,
            directiveId = V1_IMPORTED_DIRECTIVE_ID,
            title = v1.title,
            description = descriptionText,
            objective = v1.objective.ifBlank { null },
            status = status,
            targetEndDate = targetDate,
            progress = calculatedProgress,
            linkedAttributes = if (v1.linkedSpecial != null) listOf(v1.linkedSpecial) else emptyList()
        )
    }

    // ====================== V2 DOMAIN MAPPING ======================

    fun mapV2AspirationToDirective(
        aspiration: Aspiration,
        exists: (String) -> Boolean = { false }
    ): AscensionDirective {
        val resolvedId = resolveImportId(aspiration.id, "v2", exists)
        return AscensionDirective(
            id = resolvedId,
            title = aspiration.title,
            description = aspiration.description,
            targetEndDate = aspiration.targetDate,
            linkedAttributes = aspiration.linkedAttributes,
            status = when (aspiration.status) {
                GoalStatus.ACTIVE -> DirectiveStatus.ACTIVE
                GoalStatus.COMPLETED -> DirectiveStatus.COMPLETED
                GoalStatus.FAILED -> DirectiveStatus.PAUSED
            },
            currentProgress = calculateProgress(aspiration.progress.current, aspiration.progress.target),
            totalXPContributed = aspiration.progress.xpContributed
        )
    }

    fun mapV2MissionToAscensionMission(
        mission: Mission,
        mappedDirectiveId: String? = null,
        exists: (String) -> Boolean = { false }
    ): AscensionMission {
        val resolvedId = resolveImportId(mission.id, "v2", exists)
        val parentId = mappedDirectiveId ?: mission.parentAspirationId ?: V2_IMPORTED_DIRECTIVE_ID
        val targetEndDate = mission.expiresAt.atZone(ZoneId.systemDefault()).toLocalDate()
        return AscensionMission(
            id = resolvedId,
            directiveId = parentId,
            title = mission.title,
            description = mission.description,
            status = AscensionMissionStatus.ACTIVE,
            targetEndDate = targetEndDate,
            progress = calculateProgress(mission.progress.current, mission.progress.target),
            totalXPContributed = mission.progress.xpContributed,
            linkedAttributes = mission.linkedAttributes
        )
    }

    fun mapV2HabitToAscensionTask(
        habit: Habit,
        parentId: String? = null,
        exists: (String) -> Boolean = { false }
    ): AscensionTask {
        val resolvedId = resolveImportId(habit.id, "v2", exists)
        val recurrenceV3 = RecurrenceV3(
            type = when (habit.recurrence.type) {
                RecurrenceType.DAILY -> RecurrenceTypeV3.DAILY
                RecurrenceType.WEEKLY -> RecurrenceTypeV3.WEEKDAYS
                RecurrenceType.CUSTOM -> RecurrenceTypeV3.DAYS_OF_WEEK
            },
            daysOfWeek = habit.recurrence.daysOfWeek
        )
        return AscensionTask(
            id = resolvedId,
            parentId = parentId,
            title = habit.title,
            description = habit.description,
            type = AscensionTaskType.RECURRING,
            recurrence = recurrenceV3,
            currentStreak = habit.streak,
            lastCompleted = habit.lastCompleted,
            linkedAttributes = habit.linkedAttributes
        )
    }

    fun mapV2TaskToAscensionTask(
        task: Task,
        mappedParentId: String? = null,
        exists: (String) -> Boolean = { false }
    ): AscensionTask {
        val resolvedId = resolveImportId(task.id, "v2", exists)
        val parent = mappedParentId ?: task.parentGoalId.ifBlank { null }
        return AscensionTask(
            id = resolvedId,
            parentId = parent,
            title = task.title,
            description = task.description,
            type = AscensionTaskType.ONE_TIME,
            linkedAttributes = task.linkedAttributes
        )
    }

    // ====================== V2 GOAL ENTITY MAPPING ======================

    fun mapV2GoalEntityAspirationToDirective(
        entity: GoalEntity,
        exists: (String) -> Boolean = { false }
    ): AscensionDirective {
        val resolvedId = resolveImportId(entity.id, "v2", exists)
        val statusEnum = when (entity.status?.uppercase()) {
            "COMPLETED" -> DirectiveStatus.COMPLETED
            "PAUSED" -> DirectiveStatus.PAUSED
            "ARCHIVED" -> DirectiveStatus.ARCHIVED
            else -> DirectiveStatus.ACTIVE
        }
        val targetDate = entity.targetDateMillis?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }
        return AscensionDirective(
            id = resolvedId,
            title = entity.title,
            description = entity.description,
            targetEndDate = targetDate,
            linkedAttributes = entity.linkedAttributes,
            status = statusEnum,
            currentProgress = calculateProgress(entity.progressCurrent, entity.progressTarget),
            totalXPContributed = entity.xpContributed.toLong()
        )
    }

    fun mapV2GoalEntityMissionToAscensionMission(
        entity: GoalEntity,
        mappedDirectiveId: String? = null,
        exists: (String) -> Boolean = { false }
    ): AscensionMission {
        val resolvedId = resolveImportId(entity.id, "v2", exists)
        val parentId = mappedDirectiveId ?: entity.parentAspirationId ?: V2_IMPORTED_DIRECTIVE_ID
        val targetEndDate = entity.expiresAtMillis?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }
        return AscensionMission(
            id = resolvedId,
            directiveId = parentId,
            title = entity.title,
            description = entity.description,
            status = AscensionMissionStatus.ACTIVE,
            targetEndDate = targetEndDate,
            progress = calculateProgress(entity.progressCurrent, entity.progressTarget),
            totalXPContributed = entity.xpContributed.toLong(),
            linkedAttributes = entity.linkedAttributes
        )
    }

    fun mapV2GoalEntityHabitToAscensionTask(
        entity: GoalEntity,
        parentId: String? = null,
        exists: (String) -> Boolean = { false }
    ): AscensionTask {
        val resolvedId = resolveImportId(entity.id, "v2", exists)
        val recTypeStr = entity.recurrenceType?.uppercase() ?: "DAILY"
        val recTypeV3 = when {
            recTypeStr.contains("DAILY") -> RecurrenceTypeV3.DAILY
            recTypeStr.contains("WEEK") -> RecurrenceTypeV3.WEEKDAYS
            else -> RecurrenceTypeV3.DAYS_OF_WEEK
        }
        val daysOfWeek = entity.recurrenceDays?.mapNotNull { dayStr ->
            runCatching { DayOfWeek.valueOf(dayStr) }.getOrNull()
        }?.toSet() ?: emptySet()

        val recurrenceV3 = RecurrenceV3(
            type = recTypeV3,
            daysOfWeek = daysOfWeek
        )
        return AscensionTask(
            id = resolvedId,
            parentId = parentId,
            title = entity.title,
            description = entity.description,
            type = AscensionTaskType.RECURRING,
            recurrence = recurrenceV3,
            currentStreak = entity.streak,
            lastCompleted = entity.lastCompletedMillis?.let { Instant.ofEpochMilli(it) },
            linkedAttributes = entity.linkedAttributes
        )
    }

    fun mapV2GoalEntityTaskToAscensionTask(
        entity: GoalEntity,
        mappedParentId: String? = null,
        exists: (String) -> Boolean = { false }
    ): AscensionTask {
        val resolvedId = resolveImportId(entity.id, "v2", exists)
        val parent = mappedParentId ?: entity.parentGoalId?.ifBlank { null }
        return AscensionTask(
            id = resolvedId,
            parentId = parent,
            title = entity.title,
            description = entity.description,
            type = AscensionTaskType.ONE_TIME,
            linkedAttributes = entity.linkedAttributes
        )
    }
}
