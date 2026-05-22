package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.dao.AscensionDao
import com.neon.ascent.core.data.local.entity.AscensionTaskCompletionEntity
import com.neon.ascent.core.data.local.entity.NeuralLogEntity
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

    override suspend fun updateDirectiveNotes(id: String, notes: String) =
        dao.updateDirectiveNotes(id, notes)

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
        val now = Instant.now()
        val completion = AscensionTaskCompletionEntity(
            taskId = task.id,
            timestamp = now,
            notes = notes,
            mood = mood,
            linkedHealthSnapshot = linkedHealthSnapshot
        )
        
        // ADHD-friendly streak logic
        val lastDate = task.lastCompleted?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate()
        val today = java.time.LocalDate.now()
        
        val newStreak = when {
            lastDate == null -> 1
            lastDate == today -> task.currentStreak // Already completed today
            lastDate == today.minusDays(1) -> task.currentStreak + 1
            lastDate.isAfter(today.minusDays(task.graceBufferDays.toLong() + 1)) -> {
                // Within grace buffer - flicker/maintain momentum
                task.currentStreak + 1
            }
            else -> 1 // Streak broken
        }

        val updatedTask = task.copy(
            lastCompleted = now,
            currentStreak = newStreak,
            longestStreak = if (newStreak > task.longestStreak) newStreak else task.longestStreak
        )
        dao.completeTask(completion, updatedTask.toEntity())
    }

    override fun getCompletionsForTask(taskId: String): Flow<List<AscensionTaskCompletion>> =
        dao.getCompletionsForTask(taskId).map { list -> list.map { it.toDomain() } }

    override fun getCompletionsInRange(startTime: Instant): Flow<List<AscensionTaskCompletion>> =
        dao.getCompletionsInRange(startTime).map { list -> list.map { it.toDomain() } }

    override suspend fun insertNeuralLog(title: String, content: String, type: String) {
        dao.insertNeuralLog(NeuralLogEntity(
            timestamp = Instant.now(),
            type = type,
            title = title,
            content = content
        ))
    }

    override fun getAllNeuralLogs(): Flow<List<NeuralLog>> =
        dao.getAllNeuralLogs().map { list -> list.map { it.toDomain() } }
}
