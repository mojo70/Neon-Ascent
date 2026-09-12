package com.neon.ascent.core.data

import com.neon.ascent.core.data.local.dao.GoalDao
import com.neon.ascent.core.data.mapper.GoalImportMappers
import com.neon.ascent.core.data.mapper.GoalMapper
import com.neon.ascent.core.domain.GoalRepository
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.repository.AscensionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao,
    private val mapper: GoalMapper,
    private val ascensionRepository: AscensionRepository
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
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
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

    override fun getAspirationById(id: String): Flow<Aspiration?> =
        goalDao.getGoalById(id).map { it?.let { mapper.toAspiration(it) } }

    override fun getMissionById(id: String): Flow<Mission?> =
        goalDao.getGoalById(id).map { it?.let { mapper.toMission(it) } }

    override fun getHabitsForMission(missionId: String): Flow<List<Habit>> =
        goalDao.getHabitsForMission(missionId).map { entities ->
            entities.map { mapper.toHabit(it) }
        }

    // ====================== V3 ONLY WRITES ======================

    override suspend fun saveGoal(goal: Goal) {
        when (goal) {
            is Aspiration -> createAspiration(goal)
            is Mission -> saveMission(goal)
            is Habit -> saveHabit(goal)
            is Task -> {
                val task = GoalImportMappers.mapV2TaskToAscensionTask(goal)
                ascensionRepository.insertTask(task)
            }
        }
    }

    override suspend fun saveHabit(habit: Habit) {
        val task = GoalImportMappers.mapV2HabitToAscensionTask(habit)
        ascensionRepository.insertTask(task)
    }

    override suspend fun saveMission(mission: Mission) {
        val ascensionMission = GoalImportMappers.mapV2MissionToAscensionMission(mission)
        ascensionRepository.insertMission(ascensionMission)
    }

    override suspend fun createAspiration(aspiration: Aspiration) {
        val directive = GoalImportMappers.mapV2AspirationToDirective(aspiration)
        ascensionRepository.insertDirective(directive)
    }

    override suspend fun completeHabit(habitId: String, data: CompletionData) {
        val task = ascensionRepository.getTaskById(habitId).first()
        if (task != null) {
            ascensionRepository.completeTask(task, null, null, null)
        }
    }

    override suspend fun updateGoalProgress(goalId: String, progress: GoalProgress) {
        val activeMissions = ascensionRepository.getActiveMissions().first()
        val mission = activeMissions.find { it.id == goalId }
        if (mission != null) {
            val newProgress = GoalImportMappers.calculateProgress(progress.current, progress.target)
            ascensionRepository.updateMission(mission.copy(progress = newProgress))
        }
    }

    override suspend fun linkHabitToMission(habitId: String, missionId: String) {
        val task = ascensionRepository.getTaskById(habitId).first()
        if (task != null) {
            ascensionRepository.updateTask(task.copy(parentId = missionId))
        }
    }

    override suspend fun deleteGoal(id: String) {
        ascensionRepository.deleteTask(id)
        ascensionRepository.deleteDirective(id)
    }
}
