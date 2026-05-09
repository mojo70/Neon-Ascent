package com.neon.ascent.core.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NewGoalDao {
    @Query("SELECT * FROM new_goals")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM new_goals WHERE type = :type")
    fun getGoalsByType(type: String): Flow<List<GoalEntity>>

    @Upsert
    suspend fun upsertGoal(goal: GoalEntity)

    @Query("UPDATE new_goals SET currentProgress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Float)

    @Query("UPDATE new_goals SET isCompleted = 1 WHERE id = :id")
    suspend fun markCompleted(id: String)
}
