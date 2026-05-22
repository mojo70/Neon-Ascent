package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.dao.AscensionDao
import com.neon.ascent.core.data.local.entity.AscensionTaskCompletionEntity
import com.neon.ascent.core.data.mapper.*
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.repository.AscensionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class AscensionRepositoryImpl @Inject constructor(
    private val dao: AscensionDao
) : AscensionRepository {

    override fun getAllDirectives(): Flow<List<AscensionDirective>> =
        dao.getAllDirectives().map { list -> list.map { it.toDomain() } }

    override suspend fun insertDirective(directive: AscensionDirective) =
        dao.insertDirective(directive.toEntity())

    override suspend fun updateDirective(directive: AscensionDirective) =
        dao.updateDirective(directive.toEntity())

    override suspend fun deleteDirective(id: String) =
        dao.deleteDirective(id)

    override fun getMissionsForDirective(directiveId: String): Flow<List<AscensionMission>> =
        dao.getMissionsForDirective(directiveId).map { list -> list.map { it.toDomain() } }

    override fun getActiveMissions(): Flow<List<AscensionMission>> =
        dao.getActiveMissions().map { list -> list.map { it.toDomain() } }

    override suspend fun insertMission(mission: AscensionMission) =
        dao.insertMission(mission.toEntity())

    override suspend fun updateMission(mission: AscensionMission) =
        dao.updateMission(mission.toEntity())

    override fun getTasksForParent(parentId: String): Flow<List<AscensionTask>> =
        dao.getTasksForParent(parentId).map { list -> list.map { it.toDomain() } }

    override fun getAllRecurringTasks(): Flow<List<AscensionTask>> =
        dao.getAllRecurringTasks().map { list -> list.map { it.toDomain() } }

    override suspend fun insertTask(task: AscensionTask) =
        dao.insertTask(task.toEntity())

    override suspend fun updateTask(task: AscensionTask) =
        dao.updateTask(task.toEntity())

    override suspend fun deleteTask(id: String) =
        dao.deleteTask(id)

    override suspend fun completeTask(
        task: AscensionTask,
        notes: String?,
        mood: Int?,
        linkedHealthSnapshot: String?
    ) {
        val completion = AscensionTaskCompletionEntity(
            taskId = task.id,
            timestamp = Instant.now(),
            notes = notes,
            mood = mood,
            linkedHealthSnapshot = linkedHealthSnapshot
        )
        val updatedTask = task.copy(
            lastCompleted = completion.timestamp,
            currentStreak = task.currentStreak + 1 // Simple increment for now, ADHD-friendly logic can be added here
        )
        dao.completeTask(completion, updatedTask.toEntity())
    }

    override fun getCompletionsForTask(taskId: String): Flow<List<AscensionTaskCompletion>> =
        dao.getCompletionsForTask(taskId).map { list -> list.map { it.toDomain() } }

    override fun getCompletionsInRange(startTime: Instant): Flow<List<AscensionTaskCompletion>> =
        dao.getCompletionsInRange(startTime).map { list -> list.map { it.toDomain() } }
}
