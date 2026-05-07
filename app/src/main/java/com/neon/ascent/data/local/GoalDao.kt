package com.neon.ascent.data.local

import androidx.room.*
import com.neon.ascent.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE isActive = 1 ORDER BY updatedAt DESC")
    fun getActiveGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :goalId")
    fun getGoal(goalId: String): Flow<GoalEntity?>

    @Upsert
    suspend fun upsertGoal(goal: GoalEntity)

    @Query("UPDATE goals SET currentValue = :newValue, updatedAt = :timestamp WHERE id = :goalId")
    suspend fun updateProgress(goalId: String, newValue: Float, timestamp: Long = System.currentTimeMillis())
}
