package com.neon.ascent.core.data

import com.neon.ascent.core.domain.GoalRepository
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.model.SpecialType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class GoalRepositoryImpl @Inject constructor(
    private val goalDao: NewGoalDao
) : GoalRepository {

    override fun getAllGoals(): Flow<List<Goal>> {
        return goalDao.getAllGoals().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getActiveMissions(): Flow<List<Mission>> {
        return goalDao.getGoalsByType("MISSION").map { entities ->
            entities.map { it.toMission() }
        }
    }

    override fun getHabits(): Flow<List<Habit>> {
        return goalDao.getGoalsByType("HABIT").map { entities ->
            entities.map { it.toHabit() }
        }
    }

    override suspend fun updateGoalProgress(goalId: String, progress: GoalProgress) {
        goalDao.updateProgress(goalId, progress.current)
    }

    override suspend fun completeHabit(habitId: String, data: CompletionData) {
        goalDao.markCompleted(habitId)
        // Additional logic to update parent goals could go here using 'data'
    }

    override suspend fun createAspiration(aspiration: Aspiration) {
        // Implementation for creating aspiration in DB
    }

    override suspend fun linkHabitToMission(habitId: String, missionId: String) {
        // Implementation for linking
    }
}

// Extension mappers
fun GoalEntity.toDomain(): Goal = when (type) {
    "ASPIRATION" -> toAspiration()
    "MISSION" -> toMission()
    "HABIT" -> toHabit()
    else -> toTask()
}

fun GoalEntity.toAspiration() = Aspiration(
    id = id,
    title = title,
    description = description,
    targetDate = null, // Needs DB field
    linkedAttributes = listOf(attributeType),
    progress = GoalProgress(currentProgress, targetProgress, percentile),
    status = if (isCompleted) GoalStatus.COMPLETED else GoalStatus.ACTIVE
)

fun GoalEntity.toMission() = Mission(
    id = id,
    title = title,
    description = description,
    expiresAt = Instant.now(), // Needs DB field
    linkedAttributes = listOf(attributeType),
    progress = GoalProgress(currentProgress, targetProgress, percentile),
    parentAspirationId = parentGoalId
)

fun GoalEntity.toHabit() = Habit(
    id = id,
    title = title,
    description = description,
    recurrence = Recurrence(if (frequency == "WEEKLY") RecurrenceType.WEEKLY else RecurrenceType.DAILY),
    linkedAttributes = listOf(attributeType),
    progress = GoalProgress(currentProgress, targetProgress, percentile),
    streak = 0, // Needs DB field
    lastCompleted = null // Needs DB field
)

fun GoalEntity.toTask() = Task(
    id = id,
    title = title,
    description = description,
    linkedAttributes = listOf(attributeType),
    progress = GoalProgress(currentProgress, targetProgress, percentile),
    parentGoalId = parentGoalId ?: ""
)
