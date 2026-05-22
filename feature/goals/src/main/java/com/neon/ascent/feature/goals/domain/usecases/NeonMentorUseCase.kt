package com.neon.ascent.feature.goals.domain.usecases

import com.neon.ascent.core.ai.GemmaClient
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.repository.AscensionRepository
import java.util.UUID
import javax.inject.Inject

/**
 * CYBR-TES (Cybernetic Total Evaluative System) - A digital variation on Socrates.
 * Focuses on dialectic analysis, structured breakdown, and relentless questioning to achieve mastery.
 */
class NeonMentorUseCase @Inject constructor(
    private val gemmaClient: GemmaClient,
    private val repository: AscensionRepository
) {
    suspend fun generateMissionsForDirective(directive: AscensionDirective) {
        val prompt = """
            [IDENTITY: CYBR-TES]
            [PROTOCOL: ASCENSION_GENESIS]
            [OBJECTIVE: DECONSTRUCT_DIRECTIVE]
            
            Directive Title: ${directive.title}
            Directive Description: ${directive.description}
            
            Task: Generate 2 specific, actionable Missions to fulfill this directive.
            For each Mission, generate 3-4 granular, recurring or one-time Tasks.
            
            Format your response as a JSON-like structured list:
            MISSION: [Title] | [Description]
              TASK: [Title] | [Description] | [Type: ONE_TIME/RECURRING] | [Frequency: DAILY/WEEKDAYS]
        """.trimIndent()

        val response = gemmaClient.generateContent(prompt)
        parseAndSaveResponse(directive.id, response)
    }

    private suspend fun parseAndSaveResponse(directiveId: String, response: String) {
        // Simple line-based parser for the structured prompt
        var currentMissionId: String? = null
        
        response.lines().forEach { line ->
            when {
                line.trim().startsWith("MISSION:") -> {
                    val parts = line.substringAfter("MISSION:").split("|")
                    if (parts.size >= 2) {
                        val mission = AscensionMission(
                            id = UUID.randomUUID().toString(),
                            directiveId = directiveId,
                            title = parts[0].trim(),
                            description = parts[1].trim(),
                            aiGenerated = true
                        )
                        repository.insertMission(mission)
                        currentMissionId = mission.id
                    }
                }
                line.trim().startsWith("TASK:") -> {
                    val parts = line.substringAfter("TASK:").split("|")
                    if (parts.size >= 4) {
                        val task = AscensionTask(
                            id = UUID.randomUUID().toString(),
                            parentId = currentMissionId ?: directiveId,
                            title = parts[0].trim(),
                            description = parts[1].trim(),
                            type = if (parts[2].trim() == "RECURRING") AscensionTaskType.RECURRING else AscensionTaskType.ONE_TIME,
                            recurrence = if (parts[2].trim() == "RECURRING") {
                                RecurrenceV3(type = if (parts[3].trim() == "WEEKDAYS") RecurrenceTypeV3.WEEKDAYS else RecurrenceTypeV3.DAILY)
                            } else null
                        )
                        repository.insertTask(task)
                    }
                }
            }
        }
    }

    suspend fun getReview(directive: AscensionDirective, missions: List<AscensionMission>, tasks: List<AscensionTask>): String {
        val prompt = "[CYBR-TES // DIALECTIC_REVIEW] Analyze progress for '${directive.title}'. " +
                "Missions: ${missions.joinToString { it.title }}. " +
                "Tasks: ${tasks.joinToString { it.title }}. " +
                "Question the operator's current path and suggest optimizations."
        return gemmaClient.generateContent(prompt)
    }

    suspend fun getGuide(task: AscensionTask): String {
        val prompt = "[CYBR-TES // ELENCHUS_MODE] Deconstruct task: '${task.title}'. " +
                "Provide a step-by-step dialectic breakdown. What is the essential nature of this task? " +
                "Provide actionable steps for the operator."
        return gemmaClient.generateContent(prompt)
    }

    suspend fun getReflection(task: AscensionTask, notes: String?): String {
        val prompt = "[CYBR-TES // DIALECTIC_REFLECTION] The operator completed '${task.title}'. " +
                "User Notes: ${notes ?: "None"}. " +
                "Pose 2-3 short, piercing questions to help them uncover the 'why' behind their performance or resistance."
        return gemmaClient.generateContent(prompt)
    }

    suspend fun getTerminalRitualAnalysis(history: List<AscensionTaskCompletion>): String {
        val prompt = "[CYBR-TES // TERMINAL_RITUAL] Quarterly analysis protocol initiated. " +
                "Reviewing ${history.size} total completions. " +
                "Analyze patterns in mood, consistency, and notes. " +
                "Provide a deep, philosophical synthesis of this period. " +
                "Recommend 3 new high-level Directives for the next cycle."
        return gemmaClient.generateContent(prompt)
    }

    suspend fun generateRecoveryMission(task: AscensionTask): AscensionMission {
        val prompt = "[CYBR-TES // RECOVERY_PROTOCOL] The operator missed execution of '${task.title}'. " +
                "Status: Streak at risk. Buffer active. " +
                "Generate a short, high-energy 'Plot Twist' recovery mission to restore momentum. " +
                "Format: Title | Description"
        val response = gemmaClient.generateContent(prompt)
        val parts = response.split("|")
        return AscensionMission(
            id = UUID.randomUUID().toString(),
            directiveId = task.parentId,
            title = parts.getOrNull(0)?.trim() ?: "MOMENTUM_RESTORE",
            description = parts.getOrNull(1)?.trim() ?: "Complete a reduced version of ${task.title} to stabilize the neural link.",
            aiGenerated = true
        )
    }
}
