package com.neon.ascent.feature.goals.domain.usecases

import com.neon.ascent.core.ai.GemmaClient
import com.neon.ascent.core.ai.AiPersona
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.core.domain.repository.SkillRepository
import com.neon.ascent.core.data.local.dao.NeuralMemoryDao
import java.util.UUID
import javax.inject.Inject

/**
 * CYBR-TES (Cybernetic Total Evaluative System) - A digital variation on Socrates.
 * Focuses on dialectic analysis, structured breakdown, and relentless questioning to achieve mastery.
 */
class NeonMentorUseCase @Inject constructor(
    private val gemmaClient: GemmaClient,
    private val repository: AscensionRepository,
    private val skillRepository: SkillRepository,
    private val neuralMemoryDao: NeuralMemoryDao
) {
    suspend fun generateMissionsForDirective(directive: AscensionDirective, manualSkillPrompt: String? = null) {
        val skillPrompt = manualSkillPrompt ?: autoSelectSkills(directive)
        
        val prompt = """
            ${AiPersona.CYBER_SOCRATES_PROMPT}
            ${skillPrompt ?: ""}
            [PROTOCOL: ARCHITECT_GENESIS]
            [OBJECTIVE: DECONSTRUCT_DIRECTIVE]
            
            Directive Title: ${directive.title}
            Directive Description: ${directive.description}
            
            Task: Generate 2 specific, actionable MISSIONS to fulfill this DIRECTIVE.
            For each MISSION, generate 3-4 granular PULSES (daily or weekly tasks).
            Ensure these PULSES are "atomic" and "sticky".
            Map each PULSE to relevant S.P.E.C.I.A.L. stats (STRENGTH, PERCEPTION, ENDURANCE, CHARISMA, INTELLIGENCE, AGILITY, LUCK).
            
            Format your response as a JSON-like structured list:
            MISSION: [Title] | [Description]
              PULSE: [Title] | [Description] | [Type: ONE_TIME/RECURRING] | [Frequency: DAILY/WEEKDAYS] | [STATS: COMMA_SEP_LIST]
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
            val trimmed = line.trim()
            when {
                trimmed.startsWith("MISSION:") -> {
                    val parts = trimmed.substringAfter("MISSION:").split("|")
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
                trimmed.startsWith("PULSE:") || trimmed.startsWith("TASK:") -> {
                    val label = if (trimmed.startsWith("PULSE:")) "PULSE:" else "TASK:"
                    val parts = trimmed.substringAfter(label).split("|")
                    if (parts.size >= 4) {
                        val stats = if (parts.size >= 5) {
                            parts[4].substringAfter("STATS:").split(",")
                                .map { it.trim().uppercase() }
                                .mapNotNull { try { SpecialType.valueOf(it) } catch(e: Exception) { null } }
                        } else emptyList()

                        val task = AscensionTask(
                            id = UUID.randomUUID().toString(),
                            parentId = currentMissionId ?: directiveId,
                            title = parts[0].trim(),
                            description = parts[1].trim(),
                            type = if (parts[2].trim() == "RECURRING") AscensionTaskType.RECURRING else AscensionTaskType.ONE_TIME,
                            recurrence = if (parts[2].trim() == "RECURRING") {
                                RecurrenceV3(type = if (parts[3].trim() == "WEEKDAYS") RecurrenceTypeV3.WEEKDAYS else RecurrenceTypeV3.DAILY)
                            } else null,
                            linkedAttributes = stats
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

    suspend fun askMentor(task: AscensionTask, parentName: String?, parentType: String?, question: String): String {
        val parentContext = if (parentName != null) " (Parent $parentType: $parentName)" else ""
        val prompt = """
            ${AiPersona.CYBER_SOCRATES_PROMPT}
            The operator is asking a question about the task: '${task.title}'$parentContext.
            Description of the task: '${task.description}'.
            
            Operator's question: "$question"
            
            Provide a piercing, concise, dialectic guidance in response. Keep it under 120 words. Focus on deconstructing their resistance or helping them start immediately.
        """.trimIndent()
        return gemmaClient.generateContent(prompt)
    }

    suspend fun getMentorDialogue(directive: AscensionDirective, missions: List<AscensionMission>, tasks: List<AscensionTask>, mode: MentorMode, message: String): MentorUiMessage {
        // 1. Memory Palace Loading
        val recentMemories = try {
            neuralMemoryDao.searchMemories(directive.title, 5)
                .joinToString("\n") { "[Memory (${it.wing}/${it.room})]: ${it.content}" }
        } catch (e: Exception) {
            ""
        }

        // 2. Expert Persona Routing Matrix
        val expertPrompts = when {
            directive.title.contains("Hustle", ignoreCase = true) || directive.description.contains("Business", ignoreCase = true) || directive.title.contains("Biz", ignoreCase = true) -> {
                """
                [ACTIVE_EXPERTS: VENTURE_SAMURAI + PROGRESS_ARCHITECT + HABIT_FORGE]
                Approach: Highly tactical lean startup execution. Focus on MVP deployment, identifying unit economics quickly, and ruthlessly prioritizing atomic revenue units.
                """
            }
            directive.title.contains("Frame", ignoreCase = true) || directive.description.contains("Health", ignoreCase = true) || directive.title.contains("Bio", ignoreCase = true) -> {
                """
                [ACTIVE_EXPERTS: BIOHACKER_PREMIUM + RECOVERY_SAGE + ADHD_RUNNER]
                Approach: Circadian and biomonitor synchronization. Focus on physical optimization, HRV, sleeping protocols, and minimizing dopamine exhaustion.
                """
            }
            directive.title.contains("Code", ignoreCase = true) || directive.title.contains("Dev", ignoreCase = true) -> {
                """
                [ACTIVE_EXPERTS: CODE_ARCHITECT + FOCUS_MONK]
                Approach: Deep work protocols, modular construction, and rigorous testing cycles. Focus on flow state entry and technical debt elimination.
                """
            }
            else -> {
                """
                [ACTIVE_EXPERTS: HABIT_FORGE + ADHD_RUNNER + PROGRESS_ARCHITECT]
                Approach: Friction reduction and habit stacking. Keep it extremely tiny, build momentum first, and preserve streaks using grace buffers.
                """
            }
        }

        val context = """
            Current Directive: '${directive.title}' - ${directive.description}. 
            Vision: ${directive.visionStatement ?: "None"}. 
            Mode: ${mode.name}. 
            Missions: ${missions.joinToString { it.title }}. 
            Direct Pulses: ${tasks.joinToString { it.title }}.
            
            Memory Palace Context:
            $recentMemories
            
            Active Experts:
            $expertPrompts
        """.trimIndent()

        val modePrompt = when (mode) {
            MentorMode.REVIEW -> "Provide a concise status report analyzing their patterns, consistency, and progress. List 1-2 pattern detections."
            MentorMode.SOUNDING_BOARD -> "Act as a thoughtful, deconstructive mirror. Pose 1-2 open-ended reflective questions about their blockers."
            MentorMode.GUIDE -> """
                Provide active coaching. Give step-by-step checklists, estimated difficulties, habit stacking suggestions.
                Proactively suggest a structured plan (Missions + Pulses) if appropriate.
            """.trimIndent()
        }

        val prompt = """
            ${AiPersona.CYBER_SOCRATES_PROMPT}
            Context: $context
            
            Action Required: Respond to the operator's query using Socratic questioning and the active experts.
            Guideline: $modePrompt
            
            If providing a plan, format your response as:
            [MESSAGE]
            (Your conversational response)
            
            [PROPOSAL]
            MISSION: [Title] | [Description]
              PULSE: [Title] | [Description] | [Type: ONE_TIME/RECURRING] | [Frequency: DAILY/WEEKDAYS] | [TimeWindow: e.g. morning] | [STATS: COMMA_SEP_LIST]
            
            User/Operator Query: "$message"
            
            OUTPUT:
        """.trimIndent()

        val response = gemmaClient.generateContent(prompt)
        return parseDialogueResponse(response)
    }

    private fun parseDialogueResponse(response: String): MentorUiMessage {
        val messagePart = response.substringAfter("[MESSAGE]").substringBefore("[PROPOSAL]").trim()
        val proposalPart = response.substringAfter("[PROPOSAL]", "").trim()
        
        val proposedMissions = mutableListOf<ProposedMission>()
        var currentMission: ProposedMission? = null
        val currentTasks = mutableListOf<ProposedTask>()

        proposalPart.lines().forEach { line ->
            val trimmedLine = line.trim()
            when {
                trimmedLine.startsWith("MISSION:") -> {
                    currentMission?.let {
                        proposedMissions.add(it.copy(tasks = currentTasks.toList()))
                        currentTasks.clear()
                    }
                    val parts = trimmedLine.substringAfter("MISSION:").split("|")
                    if (parts.size >= 2) {
                        currentMission = ProposedMission(parts[0].trim(), parts[1].trim())
                    }
                }
                trimmedLine.startsWith("PULSE:") || trimmedLine.startsWith("TASK:") -> {
                    val label = if (trimmedLine.startsWith("PULSE:")) "PULSE:" else "TASK:"
                    val parts = trimmedLine.substringAfter(label).split("|")
                    if (parts.size >= 4) {
                        val stats = if (parts.size >= 6) {
                            parts[5].substringAfter("STATS:").split(",")
                                .map { it.trim().uppercase() }
                                .mapNotNull { try { SpecialType.valueOf(it) } catch(e: Exception) { null } }
                        } else emptyList()

                        val task = ProposedTask(
                            title = parts[0].trim(),
                            description = parts[1].trim(),
                            type = if (parts[2].trim() == "RECURRING") AscensionTaskType.RECURRING else AscensionTaskType.ONE_TIME,
                            recurrence = if (parts[2].trim() == "RECURRING") {
                                val freq = parts.getOrNull(3)?.trim() ?: "DAILY"
                                RecurrenceV3(type = if (freq == "WEEKDAYS") RecurrenceTypeV3.WEEKDAYS else RecurrenceTypeV3.DAILY)
                            } else null,
                            timeWindows = parts.getOrNull(4)?.split(",")?.map { it.trim() } ?: emptyList(),
                            linkedAttributes = stats
                        )
                        currentTasks.add(task)
                    }
                }
            }
        }
        
        if (currentMission != null) {
            proposedMissions.add(currentMission!!.copy(tasks = currentTasks.toList()))
        }

        val text = if (messagePart.isBlank() && proposalPart.isBlank()) response else messagePart
        
        return MentorUiMessage(
            text = text,
            isFromUser = false,
            proposedMissions = proposedMissions
        )
    }

    suspend fun generateTasksForMission(mission: AscensionMission) {
        val prompt = """
            ${AiPersona.CYBER_SOCRATES_PROMPT}
            [PROTOCOL: TASK_EXPANSION]
            [MISSION: ${mission.title}]
            Mission Description: ${mission.description}
            Mission Objective: ${mission.objective ?: "None"}
            
            Task: Generate 3-4 granular, actionable, recurring or one-time Tasks to fulfill this mission.
            Ensure these tasks are atomic and highly achievable (ADHD-friendly).
            
            Format your response as a structured list:
            TASK: [Title] | [Description] | [Type: ONE_TIME/RECURRING] | [Frequency: DAILY/WEEKDAYS]
        """.trimIndent()

        val response = gemmaClient.generateContent(prompt)
        response.lines().forEach { line ->
            if (line.trim().startsWith("TASK:")) {
                val parts = line.substringAfter("TASK:").split("|")
                if (parts.size >= 4) {
                    val task = AscensionTask(
                        id = UUID.randomUUID().toString(),
                        parentId = mission.id,
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
