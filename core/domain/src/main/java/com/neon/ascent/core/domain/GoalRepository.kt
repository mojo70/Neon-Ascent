package com.neon.ascent.core.domain

import com.neon.ascent.core.domain.goals.models.*
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getAllGoals(): Flow<List<Goal>>
    fun getActiveMissions(): Flow<List<Mission>>
    fun getHabits(): Flow<List<Habit>>
    
    suspend fun completeHabit(habitId: String, data: CompletionData)
    suspend fun createAspiration(aspiration: Aspiration)
    suspend fun updateGoalProgress(goalId: String, progress: GoalProgress)
    suspend fun linkHabitToMission(habitId: String, missionId: String)
}
