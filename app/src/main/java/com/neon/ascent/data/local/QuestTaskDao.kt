package com.neon.ascent.data.local

import androidx.room.*
import com.neon.ascent.model.Quest
import com.neon.ascent.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {
    @Query("SELECT * FROM quests ORDER BY createdAt DESC")
    fun getAllQuests(): Flow<List<Quest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: Quest)

    @Update
    suspend fun updateQuest(quest: Quest)

    @Delete
    suspend fun deleteQuest(quest: Quest)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE questId = :questId")
    fun getTasksForQuest(questId: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isDaily = 1")
    fun getDailyTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: String, isCompleted: Boolean)

    @Delete
    suspend fun deleteTask(task: Task)
}
