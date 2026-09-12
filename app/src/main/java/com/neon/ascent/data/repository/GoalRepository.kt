package com.neon.ascent.data.repository

import android.util.Log
import com.neon.ascent.core.data.mapper.V1_IMPORTED_DIRECTIVE_ID
import com.neon.ascent.core.domain.goals.models.AscensionMission
import com.neon.ascent.core.domain.goals.models.AscensionMissionStatus
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.data.local.GoalDao
import com.neon.ascent.domain.model.Goal
import com.neon.ascent.domain.model.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao,
    private val ascensionRepository: AscensionRepository
) {

    fun getActiveGoals(): Flow<List<Goal>> = 
        goalDao.getActiveGoals().map { list -> list.map { it.toDomain() } }

    fun getGoal(goalId: String): Flow<Goal?> = 
        goalDao.getGoal(goalId).map { it?.toDomain() }

    suspend fun createGoal(goal: Goal) {
        try {
            val mission = AscensionMission(
                id = goal.id,
                directiveId = V1_IMPORTED_DIRECTIVE_ID,
                title = goal.title,
                description = goal.description.ifBlank { goal.objective },
                objective = goal.objective.ifBlank { null },
                status = if (goal.isActive) AscensionMissionStatus.ACTIVE else AscensionMissionStatus.COMPLETED,
                progress = if (goal.targetValue > 0f && goal.currentValue > 0f) (goal.currentValue / goal.targetValue).coerceIn(0f, 1f) else 0f,
                linkedAttributes = goal.linkedSpecial?.let { special ->
                    runCatching { SpecialType.valueOf(special.name) }.getOrNull()?.let { listOf(it) }
                } ?: emptyList()
            )
            ascensionRepository.insertMission(mission)
            Log.i("GoalRepository", "Created V3 AscensionMission for V1 createGoal call: ${goal.id}")
        } catch (e: Exception) {
            Log.e("GoalRepository", "Failed to map createGoal to AscensionMission", e)
        }
    }

    suspend fun updateProgress(goalId: String, newValue: Float) {
        try {
            val activeMissions = ascensionRepository.getActiveMissions().first()
            val mission = activeMissions.find { it.id == goalId }
            if (mission != null) {
                val updated = mission.copy(progress = newValue.coerceIn(0f, 1f))
                ascensionRepository.updateMission(updated)
                Log.i("GoalRepository", "Updated V3 AscensionMission progress for V1 call: $goalId")
            } else {
                Log.w("GoalRepository", "No V3 AscensionMission found for updateProgress: $goalId")
            }
        } catch (e: Exception) {
            Log.e("GoalRepository", "Failed to update V3 AscensionMission progress", e)
        }
    }

    suspend fun archiveGoal(goalId: String) {
        try {
            val activeMissions = ascensionRepository.getActiveMissions().first()
            val mission = activeMissions.find { it.id == goalId }
            if (mission != null) {
                val updated = mission.copy(status = AscensionMissionStatus.ARCHIVED)
                ascensionRepository.updateMission(updated)
            }
        } catch (e: Exception) {
            Log.e("GoalRepository", "Failed to archive V3 AscensionMission", e)
        }
    }
}
