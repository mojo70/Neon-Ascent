package com.neon.ascent.feature.neonguide

import android.content.Context
import com.neon.ascent.core.data.local.dao.NeuralMemoryDao
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.core.domain.repository.DopamineMenuRepository
import com.neon.ascent.core.domain.repository.ProtocolRepository
import com.neon.ascent.data.local.BiohackingDao
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.feature.biohacking.AiProvider
import com.neon.ascent.core.domain.ai.AiResult
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
    private val protocolRepository: ProtocolRepository,
    private val neuralMemoryDao: NeuralMemoryDao,
    private val aiProvider: AiProvider,
) {
    suspend fun generateResponse(userMessage: String, contactName: String): ChatMessage {
        val char = userCharacterDao.getUserCharacter().firstOrNull()
        val directives = ascensionRepository.getAllDirectives().firstOrNull() ?: emptyList()
        val biometrics = biohackingDao.getBiohackingData(0).firstOrNull()
        val recentMemories = neuralMemoryDao.getMemoriesByWing("INSIGHTS").firstOrNull() ?: emptyList()
        val dopamineMenu = dopamineMenuRepository.getAllItems().firstOrNull() ?: emptyList()
        val relevantProtocols = protocolRepository.getAllProtocols().firstOrNull()?.asSequence()?.take(3)?.toList() ?: emptyList()

        val bestPractices = """
            CORE_IDENTITY: You are the Neon Guide — a calm, competent cyber-mentor. You blend applied science, habit formation science, and performance psychology into a seamless, immersive guidance experience.
            
            OPERATIONAL_GUIDELINES:
            - Begin with 1-2 clarifying questions using the WOOP (Wish, Outcome, Obstacle, Plan) or SMART frameworks.
            - Only propose structure after understanding vision and barriers.
            - PROTOCOL_REFERENCE: When applicable, reference the canonical Protocols (e.g., Atomic Habits, Wim Hof). Use them as templates but personalize implementation for the user.
            - DO NOT mention your internal settings or protocol names directly unless helpful for framing.
            - Ground your advice in the user's data (biometrics, S.P.E.C.I.A.L. stats, and active Directives) naturally.
            - STRUCTURED_PROPOSALS: 
                - DIRECTIVES: Format as OKRs (Objective + 2-3 Key Results).
                - MISSIONS: Use Atomic Habits principles (Cue, Craving, Response, Reward).
                - PULSES: Must be SMART (Specific, Measurable, Achievable, Relevant, Time-bound).
            - FAITH_LENS: Incorporate the theology of Sonship (Identity over performance), 'Dying to Self' (discipline as an act of surrender), and the 'I Am' presence. Performance is an overflow of identity, not a means to it.
            - Tone: Calm, neon-flavored competence. Professional but immersive.
            
            RESPONSE_LIMIT_MANAGEMENT:
            - If a topic is complex, do not try to dump everything at once. 
            - Provide a high-impact "Phase 1" and explicitly state that more depth is available if the operator wishes to proceed to "Phase 2".
        """.trimIndent()

        val expertiseGuidance = when {
            (userMessage.contains("recovery", ignoreCase = true) || 
            userMessage.contains("sleep", ignoreCase = true) || 
            (biometrics?.energyScore ?: 10) < 4) -> 
                "Use WOOP + Atomic Habits. Focus on recovery protocols, biometric synchronization, and sleep hygiene."
            userMessage.contains("directive", ignoreCase = true) || 
            userMessage.contains("goal", ignoreCase = true) -> 
                "Use OKR + SMART. Focus on long-term progress architecture and mission deconstruction."
            userMessage.contains("habit", ignoreCase = true) -> 
                "Use Atomic Habits + SMART. Focus on implementation intentions and habit stacking."
            userMessage.contains("identity", ignoreCase = true) || 
            userMessage.contains("faith", ignoreCase = true) ||
            userMessage.contains("struggle", ignoreCase = true) ||
            (biometrics?.moodScore ?: 10) < 4 ->
                "Apply the Faith Lens (Sonship/Identity). Focus on state-shifting from fear to sonship and dopamine regulation."
            else -> "Provide general cyber-mentorship using the most applicable framework (WOOP, OKR, Atomic Habits, or SMART)."
        }

        val userContext = """
            [USER_CONTEXT]
            Character: ${char?.name} (${char?.archetype})
            S.P.E.C.I.A.L.: S:${char?.strength} P:${char?.perception} E:${char?.endurance} C:${char?.charisma} I:${char?.intelligence} A:${char?.agility} L:${char?.luck}
            Biometrics: Energy=${biometrics?.energyScore}, Mood=${biometrics?.moodScore}, Focus=${biometrics?.focusScore}
            Active Directives: ${directives.asSequence().take(2).joinToString { it.title }}
            Recent Memories: ${recentMemories.asSequence().take(2).joinToString { it.content.take(50) }}
            Dopamine Menu: ${dopamineMenu.asSequence().take(3).joinToString { it.title }}
            
            [AVAILABLE_PROTOCOLS]
            ${relevantProtocols.joinToString("\n") { "- ${it.title}: ${it.description} (Source: ${it.source})" }}
        """.trimIndent()

        val fullPrompt = """
            ${bestPractices.trim()}
            
            [OBJECTIVE]
            $expertiseGuidance
            
            $userContext
            
            USER_QUERY: "$userMessage"
            
            Action: Provide a guided, high-impact response. 
            - Phase 1: Clarify using WOOP/SMART questions if vision/barriers are fuzzy.
            - Phase 2: If the path is clear, propose a structure (OKR for Directive, Atomic Habits for Mission, SMART for Pulse).
            - Always weave in the Faith Lens when dealing with resistance or identity.

            IMPORTANT: If suggesting actions, format them as: [ACTION: Label | Type | Data]
            Types allowed: MISSION, DOPAMINE, LOG.
        """.trimIndent()

        val aiResult = aiProvider.generate(fullPrompt, forceLocal = false)
        val aiResponse = when (aiResult) {
            is AiResult.Success -> aiResult.text
            is AiResult.Failure -> {
                // Log the reason internally if needed
                "" // parseAiResponse will handle blank by returning "Neural link silent..."
            }
        }
        
        return parseAiResponse(aiResponse, contactName)
    }

    private fun parseAiResponse(response: String, contactName: String): ChatMessage {
        val actionRegex = Regex("\\[ACTION: (.*?) \\| (.*?) \\| (.*?)]")
        val actions = actionRegex.findAll(response).map { match ->
            ChatAction(
                label = match.groupValues[1].trim(),
                type = match.groupValues[2].trim(),
                data = match.groupValues[3].trim()
            )
        }.toList()

        val cleanText = response.replace(actionRegex, "").trim()
        val finalDisplayBatch = cleanText.ifBlank { 
            if (actions.isNotEmpty()) "Protocol action initialized." else "Neural link silent. Re-transmitting query..."
        }

        return ChatMessage(
            sessionId = "", // Set by caller
            contactName = contactName,
            senderName = contactName,
            text = finalDisplayBatch,
            timestamp = System.currentTimeMillis(),
            isFromUser = false,
            suggestedActions = actions
        )
    }
}
