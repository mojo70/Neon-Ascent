package com.neon.ascent.core.domain.repository

import com.neon.ascent.core.domain.goals.models.*
import kotlinx.coroutines.flow.Flow

interface AscensionRepository {
    fun getAllDirectives(): Flow<List<AscensionDirective>>
    suspend fun insertDirective(directive: AscensionDirective)
    suspend fun updateDirective(directive: AscensionDirective)
    suspend fun deleteDirective(id: String)

    fun getMissionsForDirective(directiveId: String): Flow<List<AscensionMission>>
    fun getActiveMissions(): Flow<List<AscensionMission>>
    suspend fun insertMission(mission: AscensionMission)
    suspend fun updateMission(mission: AscensionMission)

    fun getTasksForParent(parentId: String): Flow<List<AscensionTask>>
    fun getAllRecurringTasks(): Flow<List<AscensionTask>>
    suspend fun insertTask(task: AscensionTask)
    suspend fun updateTask(task: AscensionTask)
    suspend fun deleteTask(id: String)

    suspend fun completeTask(task: AscensionTask, notes: String?, mood: Int?, linkedHealthSnapshot: String?)
    fun getCompletionsForTask(taskId: String): Flow<List<AscensionTaskCompletion>>
}
