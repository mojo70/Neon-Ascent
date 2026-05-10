package com.neon.ascent.core.domain

import com.neon.ascent.core.domain.goals.models.*
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getAllGoals(): Flow<List<Goal>>
    fun getActiveMissions(): Flow<List<Mission>>
    fun getHabits(): Flow<List<Habit>>
    fun getGoalById(id: String): Flow<Goal?>
    fun getHabitById(id: String): Flow<Habit?>
    fun getAspirationById(id: String): Flow<Aspiration?>
    fun getDueHabits(): Flow<List<Habit>>
    fun getAllAspirations(): Flow<List<Aspiration>>
    fun getMissionsForAspiration(aspirationId: String): Flow<List<Mission>>
    
    suspend fun saveGoal(goal: Goal)
    suspend fun saveHabit(habit: Habit)
    suspend fun saveMission(mission: Mission)
    suspend fun completeHabit(habitId: String, data: CompletionData)
    suspend fun createAspiration(aspiration: Aspiration)
    suspend fun updateGoalProgress(goalId: String, progress: GoalProgress)
    suspend fun linkHabitToMission(habitId: String, missionId: String)
    suspend fun deleteGoal(id: String)
}
