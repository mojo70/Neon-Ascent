package com.neon.ascent.feature.goals.domain.usecases

import com.neon.ascent.core.domain.goals.models.AscensionMission
import com.neon.ascent.core.domain.goals.models.AscensionTask
import com.neon.ascent.core.domain.repository.AscensionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdateAscensionProgressUseCase @Inject constructor(
    private val repository: AscensionRepository
) {
    suspend operator fun invoke(taskId: String) {
        val allTasks = repository.getAllRecurringTasks().first() // Simplification: we might need a better way to get parent hierarchy
        val task = allTasks.find { it.id == taskId } ?: return
        val parentId = task.parentId ?: return

        // 1. Update Mission Progress
        val missionTasks = repository.getTasksForParent(parentId).first()
        if (missionTasks.isNotEmpty()) {
            val totalWeight = missionTasks.sumOf { it.impactWeight.toDouble() }.toFloat()
            val completedWeight = missionTasks.filter { it.lastCompleted != null }
                .sumOf { it.impactWeight.toDouble() }.toFloat()
            
            val progress = if (totalWeight > 0) completedWeight / totalWeight else 0f
            
            val activeMissions = repository.getActiveMissions().first()
            val mission = activeMissions.find { it.id == parentId }
            
            if (mission != null) {
                val updatedMission = mission.copy(progress = progress)
                repository.updateMission(updatedMission)
                
                // 2. Update Directive Progress if Mission has a parent Directive
                mission.directiveId?.let { directiveId ->
                    updateDirectiveProgress(directiveId)
                }
            }
        }
    }

    private suspend fun updateDirectiveProgress(directiveId: String) {
        val missions = repository.getMissionsForDirective(directiveId).first()
        if (missions.isNotEmpty()) {
            val totalWeight = missions.sumOf { it.contributionWeight.toDouble() }.toFloat()
            val weightedProgress = missions.sumOf { (it.progress * it.contributionWeight).toDouble() }.toFloat()
            
            val directiveProgress = if (totalWeight > 0) weightedProgress / totalWeight else 0f
            
            val allDirectives = repository.getAllDirectives().first()
            val directive = allDirectives.find { it.id == directiveId }
            
            if (directive != null) {
                repository.updateDirective(directive.copy(currentProgress = directiveProgress))
            }
        }
    }
}
