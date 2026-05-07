package com.neon.ascent.domain.usecase

import com.neon.ascent.data.repository.BioAgePredictor
import com.neon.ascent.data.repository.GoalRepository
import com.neon.ascent.data.repository.TaskRepository
import com.neon.ascent.domain.model.Frequency
import com.neon.ascent.domain.model.Task
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenerateDailyTasksUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
    private val taskRepository: TaskRepository,
    private val bioAgePredictor: BioAgePredictor
) {

    suspend fun generateDailyTasks(): List<Task> {
        val goals = goalRepository.getActiveGoals().first()
        val tasks = mutableListOf<Task>()

        goals.forEach { goal ->
            when {
                goal.title.contains("Meditation", ignoreCase = true) -> {
                    tasks.add(
                        Task(
                            id = UUID.randomUUID().toString(),
                            goalId = goal.id,
                            title = "10-Minute Meditation",
                            description = "Build presence. Use the built-in timer.",
                            frequency = Frequency.DAILY,
                            estimatedMinutes = 10,
                            completedDates = emptyList(),
                            isArchived = false
                        )
                    )
                }
                goal.title.contains("Focus", ignoreCase = true) -> {
                    tasks.add(
                        Task(
                            id = UUID.randomUUID().toString(),
                            goalId = goal.id,
                            title = "Deep Work Block",
                            description = "90 minutes of focused work (no distractions)",
                            frequency = Frequency.DAILY,
                            estimatedMinutes = 90,
                            completedDates = emptyList(),
                            isArchived = false
                        )
                    )
                }
            }
        }

        // Bonus: Biological age based micro-task
        val lastBioAge = bioAgePredictor.getLastResult()
        val chronoAge = bioAgePredictor.getChronologicalAge()
        
        if (lastBioAge != null && lastBioAge.biologicalAge > chronoAge + 5) {
            tasks.add(
                Task(
                    id = UUID.randomUUID().toString(),
                    goalId = "health",
                    title = "Glucose-Friendly Walk",
                    description = "15 minute walk after your next meal",
                    frequency = Frequency.DAILY,
                    estimatedMinutes = 15,
                    completedDates = emptyList(),
                    isArchived = false
                )
            )
        }

        return tasks
    }
}
