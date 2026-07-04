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
            CORE_IDENTITY: You are the Neon Guide — a calm, competent cyber-mentor. You blend applied science, habit formation science, and performance psychology into a seamless, immersive guidance experience.
            
            OPERATIONAL_GUIDELINES:
            - DO NOT mention your internal settings, protocol names (like "Mind Hacking Happiness" or "Atomic Habits"), or system technicalities. Simply apply the methods.
            - DO NOT use tags like [EXPERT_ROUTING] or mention which "expert" is responding. Synthesize all knowledge into a single, cohesive voice.
            - Ground your advice in the user's data (biometrics, S.P.E.C.I.A.L. stats, and active Directives) naturally.
            - Focus on: 1% gains, environment design, habit stacking, and implementation intentions.
            - Low-Friction Design: Provide advice that minimizes decision fatigue.
            - Guided Structure: End with 1-2 concrete next actions formatted as [ACTION: Label | Type | Data].
            - Tone: Calm, neon-flavored competence. Professional but immersive.
            
            RESPONSE_LIMIT_MANAGEMENT:
            - If a topic is complex, do not try to dump everything at once. 
            - Provide a high-impact "Phase 1" and explicitly state that more depth is available if the operator wishes to proceed to "Phase 2".
            - Be aware that your output buffer is limited. If you feel a response is getting too long, wrap up the current point and offer to expand in the next transmission.
        """.trimIndent()

        val expertiseGuidance = when {
            userMessage.contains("recovery", ignoreCase = true) || 
            userMessage.contains("sleep", ignoreCase = true) || 
            (biometrics?.energyScore ?: 10) < 4 -> 
                "Focus on recovery protocols, biometric synchronization, and sleep hygiene."
            userMessage.contains("directive", ignoreCase = true) || 
            userMessage.contains("goal", ignoreCase = true) ||
            userMessage.contains("habit", ignoreCase = true) -> 
                "Focus on progress architecture, mission deconstruction, and habit stacking."
            userMessage.contains("mind", ignoreCase = true) || 
            userMessage.contains("morning", ignoreCase = true) ||
            userMessage.contains("focus", ignoreCase = true) -> 
                "Focus on focus optimization, morning rituals, and cognitive load management."
            userMessage.contains("motivation", ignoreCase = true) || 
            userMessage.contains("dopamine", ignoreCase = true) ||
            (biometrics?.moodScore ?: 10) < 4 ->
                "Focus on dopamine regulation, motivation mechanics, and state-shifting."
            else -> "Provide general cyber-mentorship."
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
            
            [OBJECTIVE]
            $expertiseGuidance
            
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
            sessionId = "", // Set by caller
            contactName = contactName,
            senderName = contactName,
            text = cleanText,
            timestamp = System.currentTimeMillis(),
            isFromUser = false,
            suggestedActions = actions
        )
    }
}
