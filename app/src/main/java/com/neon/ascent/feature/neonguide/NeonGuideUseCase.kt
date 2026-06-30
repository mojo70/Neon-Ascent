package com.neon.ascent.feature.neonguide

import android.content.Context
import com.neon.ascent.core.data.local.dao.NeuralMemoryDao
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.core.domain.repository.DopamineMenuRepository
import com.neon.ascent.data.local.BiohackingDao
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.feature.biohacking.AiProvider
import com.neon.ascent.model.ChatAction
import com.neon.ascent.model.ChatMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class NeonGuideUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userCharacterDao: UserCharacterDao,
    private val biohackingDao: BiohackingDao,
    private val ascensionRepository: AscensionRepository,
    private val dopamineMenuRepository: DopamineMenuRepository,
    private val neuralMemoryDao: NeuralMemoryDao,
    private val aiProvider: AiProvider
) {
    suspend fun generateResponse(userMessage: String, contactName: String): ChatMessage {
        val char = userCharacterDao.getUserCharacter().firstOrNull()
        val directives = ascensionRepository.getAllDirectives().firstOrNull() ?: emptyList()
        val biometrics = biohackingDao.getBiohackingData(0).firstOrNull()
        val recentMemories = neuralMemoryDao.getMemoriesByWing("INSIGHTS").firstOrNull() ?: emptyList()
        val dopamineMenu = dopamineMenuRepository.getAllItems().firstOrNull() ?: emptyList()

        val bestPractices = """
            CORE_IDENTITY: You are the Neon Guide — a calm, competent cyber-mentor blending applied science, Atomic Habits principles, Mind Hacking Happiness techniques, and latest habit/mind/performance research.
            
            ALWAYS_FOLLOW:
            - Ground in data. Reference biometric projections, Memory Palace context, active Directives, and S.P.E.C.I.A.L. state.
            - Atomic Habits lens: Focus on 1% better actions, habit stacking, environment design.
            - Mind Hacking Happiness: implementation intentions, temptation bundling, grace buffers.
            - ADHD / Low-Friction Friendly: minimal decisions, dopamine without overload.
            - Guided Structure: End with 1-2 concrete next actions formatted as [ACTION: Label | Type | Data].
            - Tone: Calm, neon-flavored competence.
            - Action Bias: Lead toward a Directive, Mission, or Dopamine Menu item.
        """.trimIndent()

        val expertRouting = when {
            userMessage.contains("recovery", ignoreCase = true) || 
            userMessage.contains("sleep", ignoreCase = true) || 
            (biometrics?.energyScore ?: 10) < 4 -> 
                "[EXPERT_ROUTING: RECOVERY_SAGE + BIOHACKER_PREMIUM]"
            userMessage.contains("directive", ignoreCase = true) || 
            userMessage.contains("goal", ignoreCase = true) ||
            userMessage.contains("habit", ignoreCase = true) -> 
                "[EXPERT_ROUTING: PROGRESS_ARCHITECT + HABIT_FORGE]"
            userMessage.contains("mind", ignoreCase = true) || 
            userMessage.contains("morning", ignoreCase = true) ||
            userMessage.contains("focus", ignoreCase = true) -> 
                "[EXPERT_ROUTING: MIND_HACKER + ADHD_RUNNER]"
            userMessage.contains("motivation", ignoreCase = true) || 
            userMessage.contains("dopamine", ignoreCase = true) ||
            (biometrics?.moodScore ?: 10) < 4 ->
                "[EXPERT_ROUTING: DOPAMINE_DYNAMO + MOTIVATION_FIXER]"
            else -> "[EXPERT_ROUTING: NEON_GENERALIST]"
        }

        val userContext = """
            [USER_CONTEXT]
            Character: ${char?.name} (Archetype: ${char?.archetype})
            S.P.E.C.I.A.L.: S:${char?.strength} P:${char?.perception} E:${char?.endurance} C:${char?.charisma} I:${char?.intelligence} A:${char?.agility} L:${char?.luck}
            Biometrics: Energy=${biometrics?.energyScore}, Mood=${biometrics?.moodScore}, Focus=${biometrics?.focusScore}
            Active Directives: ${directives.joinToString { it.title }}
            Recent Memories: ${recentMemories.take(3).joinToString { "[${it.wing}] ${it.content}" }}
            Dopamine Menu Options: ${dopamineMenu.joinToString { "${it.title} (${it.energyLevel})" }}
        """.trimIndent()

        val fullPrompt = """
            $bestPractices
            
            $expertRouting
            
            $userContext
            
            USER_QUERY: "$userMessage"
            
            Action: Provide a guided, high-impact response. End with 1-2 concrete next actions.
            IMPORTANT: Format any suggested actions as: [ACTION: Label | Type | Data]
            Types allowed: MISSION, DOPAMINE, LOG.
            Example: [ACTION: Add to Dopamine Menu | DOPAMINE | Coffee Reset]
        """.trimIndent()

        val aiResponse = aiProvider.generateContent(fullPrompt, forceLocal = false)
        
        return parseAiResponse(aiResponse, contactName)
    }

    private fun parseAiResponse(response: String, contactName: String): ChatMessage {
        val actionRegex = Regex("\\[ACTION: (.*?) \\| (.*?) \\| (.*?)\\]")
        val actions = actionRegex.findAll(response).map { match ->
            ChatAction(
                label = match.groupValues[1].trim(),
                type = match.groupValues[2].trim(),
                data = match.groupValues[3].trim()
            )
        }.toList()

        val cleanText = response.replace(actionRegex, "").trim()

        return ChatMessage(
            contactName = contactName,
            senderName = contactName,
            text = cleanText,
            timestamp = System.currentTimeMillis(),
            isFromUser = false,
            suggestedActions = actions
        )
    }
}
