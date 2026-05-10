package com.neon.ascent.core.data

import com.neon.ascent.core.data.local.dao.GoalDao
import com.neon.ascent.core.data.mapper.GoalMapper
import com.neon.ascent.core.domain.GoalRepository
import com.neon.ascent.core.domain.goals.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao,
    private val mapper: GoalMapper
) : GoalRepository {

    override fun getAllGoals(): Flow<List<Goal>> =
        goalDao.getAllGoals().map { entities ->
            entities.map { mapper.toDomain(it) }
        }

    override fun getHabits(): Flow<List<Habit>> =
        goalDao.getAllHabits().map { entities ->
            entities.map { mapper.toHabit(it) }
        }

    override fun getDueHabits(): Flow<List<Habit>> =
        getHabits().map { habits ->
            val startOfDay = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
            habits.filter { habit ->
                val lastCompleted = habit.lastCompleted
                lastCompleted == null || lastCompleted.isBefore(startOfDay)
            }
        }

    override fun getAllAspirations(): Flow<List<Aspiration>> =
        goalDao.getAllAspirations().map { entities ->
            entities.map { mapper.toAspiration(it) }
        }

    override fun getMissionsForAspiration(aspirationId: String): Flow<List<Mission>> =
        goalDao.getMissionsForAspiration(aspirationId).map { entities ->
            entities.map { mapper.toMission(it) }
        }

    override fun getActiveMissions(): Flow<List<Mission>> =
        goalDao.getActiveMissions().map { entities ->
            entities.map { mapper.toMission(it) }
        }

    override fun getGoalById(id: String): Flow<Goal?> =
        goalDao.getGoalById(id).map { it?.let { mapper.toDomain(it) } }

    override fun getHabitById(id: String): Flow<Habit?> =
        goalDao.getGoalById(id).map { it?.let { mapper.toHabit(it) } }

    override suspend fun saveGoal(goal: Goal) {
        goalDao.insertGoal(mapper.toEntity(goal))
    }

    override suspend fun saveHabit(habit: Habit) {
        goalDao.insertGoal(mapper.toEntity(habit))
    }

    override suspend fun saveMission(mission: Mission) {
        goalDao.insertGoal(mapper.toEntity(mission))
    }

    override suspend fun completeHabit(habitId: String, data: CompletionData) {
        val current = goalDao.getGoalById(habitId).first() ?: return
        val updatedEntity = mapper.updateHabitWithCompletion(current, data)
        goalDao.completeHabitTransaction(habitId, updatedEntity)
    }

    override suspend fun createAspiration(aspiration: Aspiration) {
        goalDao.insertGoal(mapper.toEntity(aspiration))
    }

    override suspend fun updateGoalProgress(goalId: String, progress: GoalProgress) {
        goalDao.updateProgress(goalId, progress.current.toDouble())
    }

    override suspend fun linkHabitToMission(habitId: String, missionId: String) {
        // TODO: Implementation for linking logic
    }

    override suspend fun deleteGoal(id: String) {
        goalDao.deleteGoal(id)
    }
}
