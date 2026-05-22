package com.neon.ascent.feature.goals.domain.usecases

import com.neon.ascent.core.ai.GemmaClient
import com.neon.ascent.core.ai.AiPersona
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.core.domain.repository.SkillRepository
import java.util.UUID
import javax.inject.Inject

/**
 * CYBR-TES (Cybernetic Total Evaluative System) - A digital variation on Socrates.
 * Focuses on dialectic analysis, structured breakdown, and relentless questioning to achieve mastery.
 */
class NeonMentorUseCase @Inject constructor(
    private val gemmaClient: GemmaClient,
    private val repository: AscensionRepository,
    private val skillRepository: SkillRepository
) {
    suspend fun generateMissionsForDirective(directive: AscensionDirective, manualSkillPrompt: String? = null) {
        val skillPrompt = manualSkillPrompt ?: autoSelectSkills(directive)
        
        val prompt = """
            ${AiPersona.CYBER_SOCRATES_PROMPT}
            ${skillPrompt ?: ""}
            [PROTOCOL: ASCENSION_GENESIS]
            [OBJECTIVE: DECONSTRUCT_DIRECTIVE]
            
            Directive Title: ${directive.title}
            Directive Description: ${directive.description}
            
            Task: Generate 2 specific, actionable Missions to fulfill this directive.
            For each Mission, generate 3-4 granular, recurring or one-time Tasks.
            Ensure these tasks are "atomic" and "sticky" (easy to start, hard to ignore).
            If multiple skills were provided, synthesize their methods (e.g., combine Trading logic with Biohacking stability).
            
            Format your response as a JSON-like structured list:
            MISSION: [Title] | [Description]
              TASK: [Title] | [Description] | [Type: ONE_TIME/RECURRING] | [Frequency: DAILY/WEEKDAYS]
        """.trimIndent()

        val response = gemmaClient.generateContent(prompt)
        parseAndSaveResponse(directive.id, response)
    }

    private suspend fun autoSelectSkills(directive: AscensionDirective): String? {
        val routingPrompt = """
            [PROTOCOL: SKILL_ROUTING]
            Available Expert Skills: BIOHACKING, MEDITATION, REMOTE_VIEWING, BUSINESS_BUILDING, TRADING
            
            Directive: ${directive.title} - ${directive.description}
            
            Task: Identify which 1-2 expert skills from the list are most relevant to this directive. 
            Respond ONLY with the skill names separated by commas. If none apply, respond 'NONE'.
            OUTPUT:
        """.trimIndent()

        val selection = gemmaClient.generateContent(routingPrompt)
            .substringAfter("OUTPUT:")
            .trim()
            .uppercase()

        if (selection == "NONE") return null

        val selectedSkills = selection.split(",").map { it.trim() }
        val prompts = selectedSkills.mapNotNull { skillRepository.getSkillPrompt(it) }
        
        return if (prompts.isNotEmpty()) {
            "[ACTIVE_SKILL_SYNTHESIS]\n" + prompts.joinToString("\n\n")
        } else null
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
        val context = "Analyze progress for '${directive.title}'. Missions: ${missions.joinToString { it.title }}. Tasks: ${tasks.joinToString { it.title }}."
        val prompt = AiPersona.getSocratesPrompt(context) + "\nQuestion the operator's current path and suggest optimizations."
        return gemmaClient.generateContent(prompt)
    }

    suspend fun getGuide(task: AscensionTask): String {
        val context = "Deconstruct task: '${task.title}'."
        val prompt = AiPersona.getSocratesPrompt(context) + "\nProvide a step-by-step dialectic breakdown. What is the essential nature of this task? Provide actionable steps for the operator."
        return gemmaClient.generateContent(prompt)
    }

    suspend fun getReflection(task: AscensionTask, notes: String?): String {
        val context = "The operator completed '${task.title}'. User Notes: ${notes ?: "None"}."
        val prompt = AiPersona.getSocratesPrompt(context) + "\nPose 2-3 short, piercing questions to help them uncover the 'why' behind their performance or resistance."
        return gemmaClient.generateContent(prompt)
    }

    suspend fun getTerminalRitualAnalysis(history: List<AscensionTaskCompletion>): String {
        val context = "Quarterly analysis protocol initiated. Reviewing ${history.size} total completions. Analyze patterns in mood, consistency, and notes."
        val prompt = AiPersona.getSocratesPrompt(context) + "\nProvide a deep, philosophical synthesis of this period. Recommend 3 new high-level Directives for the next cycle."
        return gemmaClient.generateContent(prompt)
    }

    suspend fun generateRecoveryMission(task: AscensionTask): AscensionMission {
        val context = "The operator missed execution of '${task.title}'. Status: Streak at risk. Buffer active."
        val prompt = AiPersona.getSocratesPrompt(context) + "\nGenerate a short, high-energy 'Plot Twist' recovery mission to restore momentum. Format: Title | Description"
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
