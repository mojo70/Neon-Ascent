package com.neon.ascent.feature.goals.domain.usecases

import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.repository.AscensionRepository
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ExportNeuralLogUseCase @Inject constructor(
    private val repository: AscensionRepository
) {
    suspend operator fun invoke(): String {
        val directives = repository.getAllDirectives().first()
        val allTasks = repository.getAllRecurringTasks().first() // Includes all tasks currently in our simple repo
        
        val sb = StringBuilder()
        sb.append("# ⚡ NEURAL ASCENSION LOG // ANALOG_BACKUP\n\n")
        sb.append("Generated on: ${java.time.LocalDateTime.now()}\n\n")
        
        sb.append("## 🛰️ DIRECTIVE_OVERVIEW\n\n")
        directives.forEach { directive ->
            sb.append("### [${directive.status}] ${directive.title}\n")
            sb.append("- **Objective:** ${directive.description}\n")
            sb.append("- **Progress:** ${(directive.currentProgress * 100).toInt()}%\n")
            sb.append("- **XP Contributed:** ${directive.totalXPContributed}\n")
            if (!directive.notes.isNullOrBlank()) {
                sb.append("- **Operator Notes:** ${directive.notes}\n")
            }
            sb.append("\n")
            
            // Missions for this directive
            val missions = repository.getMissionsForDirective(directive.id).first()
            missions.forEach { mission ->
                sb.append("  #### MISSION: ${mission.title}\n")
                sb.append("  - ${mission.description}\n")
                sb.append("  - Status: ${mission.status}\n\n")
                
                // Tasks for this mission
                val tasks = repository.getTasksForParent(mission.id).first()
                tasks.forEach { task ->
                    appendTaskData(sb, task)
                }
            }
        }
        
        // Standalone tasks
        val standaloneTasks = allTasks.filter { it.parentId == null }
        if (standaloneTasks.isNotEmpty()) {
            sb.append("## 🛠️ STANDALONE_PROTOCOLS\n\n")
            standaloneTasks.forEach { task ->
                appendTaskData(sb, task)
            }
        }

        // AI Dialectic Archive
        val neuralLogs = repository.getAllNeuralLogs().first()
        if (neuralLogs.isNotEmpty()) {
            sb.append("## 🧠 CYBR-TES // DIALECTIC_ARCHIVE\n\n")
            neuralLogs.forEach { log ->
                sb.append("### ${log.title} [${log.type}]\n")
                sb.append("- **Timestamp:** ${log.timestamp}\n")
                sb.append("- **Synthesis:**\n\n${log.content}\n\n")
            }
        }

        return sb.toString()
    }

    private suspend fun appendTaskData(sb: StringBuilder, task: AscensionTask) {
        val completions = repository.getCompletionsForTask(task.id).first()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())
        
        sb.append("  - **TASK:** ${task.title} (Streak: ${task.currentStreak})\n")
        if (completions.isNotEmpty()) {
            sb.append("    - **Completion History:**\n")
            completions.forEach { completion ->
                sb.append("      - [${formatter.format(completion.timestamp)}] ")
                if (completion.mood != null) sb.append("Mood: ${completion.mood}/5 | ")
                sb.append(completion.notes ?: "No notes")
                sb.append("\n")
            }
        }
        sb.append("\n")
    }
}
