package com.neon.ascent.domain.usecase

import android.content.Context
import com.google.gson.Gson
import com.neon.ascent.data.repository.BioAgePredictor
import com.neon.ascent.data.repository.GoalRepository
import com.neon.ascent.data.repository.TaskRepository
import com.neon.ascent.domain.model.Frequency
import com.neon.ascent.domain.model.Task
import com.neon.ascent.domain.model.TaskBank
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenerateDailyTasksUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
    private val taskRepository: TaskRepository,
    private val bioAgePredictor: BioAgePredictor,
    @ApplicationContext private val context: Context
) {

    suspend fun generateTodaysTasks(): List<Task> {
        val activeGoals = goalRepository.getActiveGoals().first()
        val taskBank = loadTaskBank()
        val tasks = mutableListOf<Task>()

        // Hydration micro-habit (always start here)
        taskBank.tasks.find { it.category == "hydration" }?.let { template ->
            tasks.add(Task(
                id = UUID.randomUUID().toString(),
                goalId = "hydration",
                title = "${template.title} ${template.defaultValue ?: ""} ${template.unit ?: ""}".trim(),
                description = template.description,
                frequency = Frequency.valueOf(template.frequency),
                estimatedMinutes = template.baseMinutes,
                completedDates = emptyList(),
                isArchived = false
            ))
        }

        // Add 1–2 more tasks based on goals (keep it light)
        activeGoals.forEach { goal ->
            val match = when {
                goal.title.contains("Meditation", ignoreCase = true) -> taskBank.tasks.find { it.category == "meditation" }
                goal.title.contains("Focus", ignoreCase = true) -> taskBank.tasks.find { it.category == "focus" }
                goal.title.contains("Movement", ignoreCase = true) || goal.title.contains("Walk", ignoreCase = true) -> taskBank.tasks.find { it.category == "movement" }
                else -> null
            }
            
            match?.let { template ->
                if (tasks.none { it.title.contains(template.title) }) {
                    tasks.add(Task(
                        id = UUID.randomUUID().toString(),
                        goalId = goal.id,
                        title = template.title,
                        description = template.description,
                        frequency = Frequency.valueOf(template.frequency),
                        estimatedMinutes = template.baseMinutes,
                        completedDates = emptyList(),
                        isArchived = false
                    ))
                }
            }
        }

        // Bio-age based micro-task (if needed and not already added)
        val lastBioAge = bioAgePredictor.getLastResult()
        val chronoAge = bioAgePredictor.getChronologicalAge()
        if (lastBioAge != null && lastBioAge.biologicalAge > chronoAge + 5) {
            if (tasks.none { it.goalId == "health" || it.title.contains("Walk", ignoreCase = true) }) {
                taskBank.tasks.find { it.category == "movement" }?.let { template ->
                    tasks.add(Task(
                        id = UUID.randomUUID().toString(),
                        goalId = "health",
                        title = template.title,
                        description = template.description,
                        frequency = Frequency.valueOf(template.frequency),
                        estimatedMinutes = template.baseMinutes,
                        completedDates = emptyList(),
                        isArchived = false
                    ))
                }
            }
        }

        return tasks.take(3)   // Never overwhelm — max 3 daily tasks
    }

    private fun loadTaskBank(): TaskBank {
        val json = context.assets.open("databank/task_bank.json").bufferedReader().use { it.readText() }
        return Gson().fromJson(json, TaskBank::class.java)
    }
}
