package com.neon.ascent.feature.goals.domain.usecases

import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.repository.AscensionRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class CheckRecoveryMissionsUseCase @Inject constructor(
    private val repository: AscensionRepository,
    private val mentorUseCase: NeonMentorUseCase
) {
    suspend operator fun invoke(): List<String> {
        val tasks = repository.getAllRecurringTasks().first()
        val today = LocalDate.now()
        val recoveryCreated = mutableListOf<String>()
        
        tasks.forEach { task ->
            if (isTaskMissed(task, today)) {
                val activeMissions = repository.getActiveMissions().first()
                val hasRecovery = activeMissions.any { it.isRecovery && it.title.contains(task.title, ignoreCase = true) }
                
                if (!hasRecovery) {
                    val recoveryMission = mentorUseCase.generateRecoveryMission(task).copy(isRecovery = true)
                    repository.insertMission(recoveryMission)
                    
                    val recoveryTask = AscensionTask(
                        id = java.util.UUID.randomUUID().toString(),
                        parentId = recoveryMission.id,
                        title = "STABILIZE: ${task.title}",
                        description = "Recovery protocol initiated to maintain streak.",
                        type = AscensionTaskType.ONE_TIME,
                        xpValue = 5
                    )
                    repository.insertTask(recoveryTask)
                    recoveryCreated.add(task.title)
                }
            }
        }
        return recoveryCreated
    }

    private fun isTaskMissed(task: AscensionTask, today: LocalDate): Boolean {
        val lastCompletedDate = task.lastCompleted?.atZone(ZoneId.systemDefault())?.toLocalDate()
        
        // If never completed, it's not a "missed streak" yet
        if (lastCompletedDate == null) return false
        
        // If completed today, not missed
        if (lastCompletedDate == today) return false
        
        // Check if yesterday was a scheduled day and it was missed
        val yesterday = today.minusDays(1)
        val wasScheduledYesterday = isScheduledFor(task, yesterday)
        
        return wasScheduledYesterday && lastCompletedDate.isBefore(yesterday)
    }

    private fun isScheduledFor(task: AscensionTask, date: LocalDate): Boolean {
        if (task.type == AscensionTaskType.ONE_TIME) return false
        val recurrence = task.recurrence ?: return false
        
        return when (recurrence.type) {
            RecurrenceTypeV3.DAILY -> true
            RecurrenceTypeV3.WEEKDAYS -> date.dayOfWeek.value in 1..5
            RecurrenceTypeV3.DAYS_OF_WEEK -> recurrence.daysOfWeek.contains(date.dayOfWeek)
            else -> false
        }
    }
}
