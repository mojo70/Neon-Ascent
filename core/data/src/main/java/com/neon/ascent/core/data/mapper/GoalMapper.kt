package com.neon.ascent.core.data.mapper

import com.neon.ascent.core.data.GoalEntity
import com.neon.ascent.core.domain.goals.models.*
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalMapper @Inject constructor() {

    // ====================== DOMAIN → ENTITY ======================

    fun toEntity(goal: Goal): GoalEntity = when (goal) {
        is Habit -> goal.toEntity()
        is Mission -> goal.toEntity()
        is Aspiration -> goal.toEntity()
        is Task -> goal.toEntity()
    }

    private fun Habit.toEntity() = GoalEntity(
        id = id,
        type = "HABIT",
        title = title,
        description = description,
        linkedAttributes = linkedAttributes,
        progressCurrent = progress.current.toDouble(),
        progressTarget = progress.target.toDouble(),
        xpContributed = progress.xpContributed.toInt(),
        recurrenceType = recurrence.type.name,
        recurrenceDays = recurrence.daysOfWeek.map { it.name },
        streak = streak,
        lastCompletedMillis = lastCompleted?.toEpochMilli()
    )

    private fun Mission.toEntity() = GoalEntity(
        id = id,
        type = "MISSION",
        title = title,
        description = description,
        linkedAttributes = linkedAttributes,
        progressCurrent = progress.current.toDouble(),
        progressTarget = progress.target.toDouble(),
        xpContributed = progress.xpContributed.toInt(),
        expiresAtMillis = expiresAt.toEpochMilli(),
        parentAspirationId = parentAspirationId
    )

    private fun Aspiration.toEntity() = GoalEntity(
        id = id,
        type = "ASPIRATION",
        title = title,
        description = description,
        linkedAttributes = linkedAttributes,
        progressCurrent = progress.current.toDouble(),
        progressTarget = progress.target.toDouble(),
        xpContributed = progress.xpContributed.toInt(),
        targetDateMillis = targetDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        status = status.name
    )

    private fun Task.toEntity() = GoalEntity(
        id = id,
        type = "TASK",
        title = title,
        description = description,
        linkedAttributes = linkedAttributes,
        progressCurrent = progress.current.toDouble(),
        progressTarget = progress.target.toDouble(),
        xpContributed = progress.xpContributed.toInt(),
        parentGoalId = parentGoalId
    )

    // ====================== ENTITY → DOMAIN ======================

    fun toDomain(entity: GoalEntity): Goal = when (entity.type) {
        "ASPIRATION" -> toAspiration(entity)
        "MISSION" -> toMission(entity)
        "HABIT" -> toHabit(entity)
        else -> toTask(entity)
    }

    fun toHabit(entity: GoalEntity): Habit = with(entity) {
        Habit(
            id = id,
            title = title,
            description = description,
            recurrence = Recurrence(
                type = RecurrenceType.valueOf(recurrenceType ?: "DAILY"),
                daysOfWeek = recurrenceDays?.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }?.toSet()
                    ?: emptySet()
            ),
            linkedAttributes = linkedAttributes,
            progress = GoalProgress(
                current = progressCurrent.toFloat(),
                target = progressTarget.toFloat(),
                xpContributed = xpContributed.toLong()
            ),
            streak = streak,
            lastCompleted = lastCompletedMillis?.let { Instant.ofEpochMilli(it) }
        )
    }

    fun toMission(entity: GoalEntity): Mission = with(entity) {
        Mission(
            id = id,
            title = title,
            description = description,
            expiresAt = expiresAtMillis?.let { Instant.ofEpochMilli(it) } ?: Instant.now(),
            linkedAttributes = linkedAttributes,
            progress = GoalProgress(
                current = progressCurrent.toFloat(),
                target = progressTarget.toFloat(),
                xpContributed = xpContributed.toLong()
            ),
            parentAspirationId = parentAspirationId
        )
    }

    fun toAspiration(entity: GoalEntity): Aspiration = with(entity) {
        Aspiration(
            id = id,
            title = title,
            description = description,
            targetDate = targetDateMillis?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            },
            linkedAttributes = linkedAttributes,
            progress = GoalProgress(
                current = progressCurrent.toFloat(),
                target = progressTarget.toFloat(),
                xpContributed = xpContributed.toLong()
            ),
            status = GoalStatus.valueOf(status ?: "ACTIVE")
        )
    }

    fun toTask(entity: GoalEntity): Task = with(entity) {
        Task(
            id = id,
            title = title,
            description = description,
            linkedAttributes = linkedAttributes,
            progress = GoalProgress(
                current = progressCurrent.toFloat(),
                target = progressTarget.toFloat(),
                xpContributed = xpContributed.toLong()
            ),
            parentGoalId = parentGoalId ?: ""
        )
    }

    // ====================== HELPER FOR UPDATES ======================

    fun updateHabitWithCompletion(entity: GoalEntity, data: CompletionData): GoalEntity {
        val newProgress = (entity.progressCurrent + data.progressDelta).coerceAtMost(entity.progressTarget)
        return entity.copy(
            progressCurrent = newProgress,
            lastCompletedMillis = Instant.now().toEpochMilli()
        )
    }
}
