package com.neon.ascent.data.local

import androidx.room.*
import com.neon.ascent.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalTaskDao {
    @Query("SELECT * FROM task_entities WHERE goalId = :goalId AND isArchived = 0 ORDER BY sortOrder")
    fun getTasksForGoal(goalId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task_entities WHERE frequency = 'DAILY' AND isArchived = 0")
    fun getDailyTasks(): Flow<List<TaskEntity>>

    @Upsert
    suspend fun upsertTask(task: TaskEntity)

    @Query("SELECT * FROM task_entities WHERE id = :taskId")
    fun getTaskById(taskId: String): Flow<TaskEntity?>

    @Query("UPDATE task_entities SET completedDates = :newDates WHERE id = :taskId")
    suspend fun markCompleted(taskId: String, newDates: List<String>)
}
