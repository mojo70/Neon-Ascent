package com.neon.ascent.core.domain.goals.usecases

import com.neon.ascent.core.domain.repository.AscensionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdateAscensionProgressUseCase @Inject constructor(
    private val repository: AscensionRepository
) {
    suspend operator fun invoke(taskId: String) {
        val allTasks = repository.getAllRecurringTasks().first()
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
            } else {
                // Task might belong directly to a Directive
                updateDirectiveProgress(parentId)
            }
        }
    }

    private suspend fun updateDirectiveProgress(directiveId: String) {
        val missions = repository.getMissionsForDirective(directiveId).first()
        val directTasks = repository.getTasksForParent(directiveId).first()
        
        if (missions.isNotEmpty() || directTasks.isNotEmpty()) {
            val missionTotalWeight = missions.sumOf { it.contributionWeight.toDouble() }.toFloat()
            val missionWeightedProgress = missions.sumOf { (it.progress * it.contributionWeight).toDouble() }.toFloat()
            
            val taskTotalWeight = directTasks.sumOf { it.impactWeight.toDouble() }.toFloat()
            val taskCompletedWeight = directTasks.filter { it.lastCompleted != null }
                .sumOf { it.impactWeight.toDouble() }.toFloat()
            
            val totalWeight = missionTotalWeight + taskTotalWeight
            val totalCompleted = missionWeightedProgress + taskCompletedWeight
            
            val directiveProgress = if (totalWeight > 0) totalCompleted / totalWeight else 0f
            
            val allDirectives = repository.getAllDirectives().first()
            val directive = allDirectives.find { it.id == directiveId }
            
            if (directive != null) {
                repository.updateDirective(directive.copy(currentProgress = directiveProgress))
            }
        }
    }
}
