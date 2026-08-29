package com.neon.ascent.core.data.local.dao

import androidx.room.*
import com.neon.ascent.core.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AscensionDao {

    // Directives
    @Query("SELECT * FROM ascension_directives ORDER BY createdAt DESC")
    fun getAllDirectives(): Flow<List<AscensionDirectiveEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirective(directive: AscensionDirectiveEntity)

    @Update
    suspend fun updateDirective(directive: AscensionDirectiveEntity)

    @Query("DELETE FROM ascension_directives WHERE id = :id")
    suspend fun deleteDirective(id: String)

    @Query("SELECT * FROM ascension_directives WHERE id = :id")
    suspend fun getDirectiveById(id: String): AscensionDirectiveEntity?

    // Missions
    @Query("SELECT * FROM ascension_missions WHERE directiveId = :directiveId")
    fun getMissionsForDirective(directiveId: String): Flow<List<AscensionMissionEntity>>

    @Query("SELECT * FROM ascension_missions WHERE directiveId = :directiveId")
    suspend fun getMissionsForDirectiveSync(directiveId: String): List<AscensionMissionEntity>

    @Query("SELECT * FROM ascension_missions WHERE id = :id")
    suspend fun getMissionById(id: String): AscensionMissionEntity?

    @Query("SELECT * FROM ascension_missions WHERE status = 'ACTIVE'")
    fun getActiveMissions(): Flow<List<AscensionMissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMission(mission: AscensionMissionEntity)

    @Update
    suspend fun updateMission(mission: AscensionMissionEntity)

    // Tasks
    @Query("SELECT * FROM ascension_tasks WHERE parentId = :parentId")
    fun getTasksForParent(parentId: String): Flow<List<AscensionTaskEntity>>

    @Query("SELECT * FROM ascension_tasks WHERE parentId = :parentId")
    suspend fun getTasksForParentSync(parentId: String): List<AscensionTaskEntity>

    @Query("SELECT * FROM ascension_tasks WHERE type = 'RECURRING'")
    fun getAllRecurringTasks(): Flow<List<AscensionTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: AscensionTaskEntity)

    @Update
    suspend fun updateTask(task: AscensionTaskEntity)

    @Query("DELETE FROM ascension_tasks WHERE id = :id")
    suspend fun deleteTask(id: String)

    @Query("SELECT * FROM ascension_tasks WHERE id = :id")
    fun getTaskById(id: String): Flow<AscensionTaskEntity?>

    @Query("UPDATE ascension_directives SET notes = :notes WHERE id = :id")
    suspend fun updateDirectiveNotes(id: String, notes: String)

    // Completions
    @Insert
    suspend fun insertCompletion(completion: AscensionTaskCompletionEntity)

    @Query("SELECT * FROM ascension_task_completions WHERE taskId = :taskId ORDER BY timestamp DESC")
    fun getCompletionsForTask(taskId: String): Flow<List<AscensionTaskCompletionEntity>>

    @Query("SELECT * FROM ascension_task_completions WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getCompletionsInRange(startTime: java.time.Instant): Flow<List<AscensionTaskCompletionEntity>>

    @Transaction
    suspend fun completeTask(completion: AscensionTaskCompletionEntity, task: AscensionTaskEntity) {
        insertCompletion(completion)
        updateTask(task)
    }

    // Neural Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNeuralLog(log: NeuralLogEntity)

    @Query("SELECT * FROM neural_logs ORDER BY timestamp DESC")
    fun getAllNeuralLogs(): Flow<List<NeuralLogEntity>>
}
