package com.neon.ascent.core.domain

import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getAspirations(): Flow<List<Goal.Aspiration>>
    fun getMissions(): Flow<List<Goal.Mission>>
    fun getHabits(): Flow<List<Goal.Habit>>
    
    suspend fun updateGoalProgress(goalId: String, progress: GoalProgress)
    suspend fun completeHabit(habitId: String)
    suspend fun linkHabitToMission(habitId: String, missionId: String)
}
