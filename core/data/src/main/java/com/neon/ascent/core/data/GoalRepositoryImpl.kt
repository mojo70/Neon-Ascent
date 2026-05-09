package com.neon.ascent.core.data

import com.neon.ascent.core.domain.Goal
import com.neon.ascent.core.domain.GoalProgress
import com.neon.ascent.core.domain.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GoalRepositoryImpl @Inject constructor(
    private val goalDao: NewGoalDao
) : GoalRepository {

    override fun getAspirations(): Flow<List<Goal.Aspiration>> {
        return goalDao.getGoalsByType("ASPIRATION").map { entities ->
            entities.map { it.toAspiration() }
        }
    }

    override fun getMissions(): Flow<List<Goal.Mission>> {
        return goalDao.getGoalsByType("MISSION").map { entities ->
            entities.map { it.toMission() }
        }
    }

    override fun getHabits(): Flow<List<Goal.Habit>> {
        return goalDao.getGoalsByType("HABIT").map { entities ->
            entities.map { it.toHabit() }
        }
    }

    override suspend fun updateGoalProgress(goalId: String, progress: GoalProgress) {
        goalDao.updateProgress(goalId, progress.current)
    }

    override suspend fun completeHabit(habitId: String) {
        goalDao.markCompleted(habitId)
        // Additional logic to update parent goals could go here
    }

    override suspend fun linkHabitToMission(habitId: String, missionId: String) {
        // Implementation for linking
    }
}

// Extension mappers
fun GoalEntity.toAspiration() = Goal.Aspiration(
    id = id,
    title = title,
    description = description,
    progress = GoalProgress(currentProgress, targetProgress, percentile, attributeType)
)

fun GoalEntity.toMission() = Goal.Mission(
    id = id,
    title = title,
    archetype = archetype ?: "DEFAULT",
    progress = GoalProgress(currentProgress, targetProgress, percentile, attributeType)
)

fun GoalEntity.toHabit() = Goal.Habit(
    id = id,
    title = title,
    frequency = frequency ?: "DAILY",
    progress = GoalProgress(currentProgress, targetProgress, percentile, attributeType)
)
