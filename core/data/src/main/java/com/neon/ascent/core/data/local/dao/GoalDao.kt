package com.neon.ascent.core.data.local.dao

import androidx.room.*
import com.neon.ascent.core.data.GoalEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals ORDER BY id")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE type = 'HABIT'")
    fun getAllHabits(): Flow<List<GoalEntity>>

    @Query("""
        SELECT * FROM goals 
        WHERE type = 'MISSION' 
        AND (expiresAtMillis IS NULL OR expiresAtMillis > :now)
        ORDER BY expiresAtMillis ASC
    """)
    fun getActiveMissions(now: Long = Instant.now().toEpochMilli()): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    fun getGoalById(id: String): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE type = 'ASPIRATION'")
    fun getAllAspirations(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE type = 'MISSION' AND parentAspirationId = :aspirationId")
    fun getMissionsForAspiration(aspirationId: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE type = 'HABIT' AND parentGoalId = :missionId")
    fun getHabitsForMission(missionId: String): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity)

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("UPDATE goals SET streak = streak + 1, lastCompletedMillis = :timestamp WHERE id = :habitId")
    suspend fun incrementHabitStreak(habitId: String, timestamp: Long)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: String)

    @Query("UPDATE goals SET progressCurrent = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Double)

    @Query("UPDATE goals SET status = 'COMPLETED' WHERE id = :id")
    suspend fun markCompleted(id: String)

    // Helper for bulk operations
    @Transaction
    suspend fun completeHabitTransaction(habitId: String, updatedHabit: GoalEntity) {
        updateGoal(updatedHabit)
        incrementHabitStreak(habitId, Instant.now().toEpochMilli())
    }
}
