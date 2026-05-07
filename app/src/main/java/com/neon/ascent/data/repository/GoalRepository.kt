package com.neon.ascent.data.repository

import com.neon.ascent.data.local.GoalDao
import com.neon.ascent.domain.model.Goal
import com.neon.ascent.domain.model.toDomain
import com.neon.ascent.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao
) {

    fun getActiveGoals(): Flow<List<Goal>> = 
        goalDao.getActiveGoals().map { list -> list.map { it.toDomain() } }

    fun getGoal(goalId: String): Flow<Goal?> = 
        goalDao.getGoal(goalId).map { it?.toDomain() }

    suspend fun createGoal(goal: Goal) {
        goalDao.upsertGoal(goal.toEntity())
    }

    suspend fun updateProgress(goalId: String, newValue: Float) {
        goalDao.updateProgress(goalId, newValue)
    }

    suspend fun archiveGoal(goalId: String) {
        // Soft delete or isActive = false logic could go here if added to DAO
    }
}
