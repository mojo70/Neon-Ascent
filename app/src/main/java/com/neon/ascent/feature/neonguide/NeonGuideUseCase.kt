package com.neon.ascent.feature.neonguide

import android.content.Context
import com.neon.ascent.core.ai.AiMode
import com.neon.ascent.core.ai.AiPersona
import com.neon.ascent.core.data.local.dao.NeuralMemoryDao
import com.neon.ascent.core.domain.ai.AiResult
import com.neon.ascent.core.domain.character.repository.CharacterRepository
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.core.domain.repository.DopamineMenuRepository
import com.neon.ascent.core.domain.repository.ProtocolRepository
import com.neon.ascent.data.local.BiohackingDao
import com.neon.ascent.feature.biohacking.AiProvider
import com.neon.ascent.model.ChatAction
import com.neon.ascent.model.ChatMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class NeonGuideUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val characterRepository: CharacterRepository,
    private val biohackingDao: BiohackingDao,
    private val ascensionRepository: AscensionRepository,
    private val dopamineMenuRepository: DopamineMenuRepository,
    private val protocolRepository: ProtocolRepository,
    private val neuralMemoryDao: NeuralMemoryDao,
    private val aiProvider: AiProvider,
) {
    suspend fun generateResponse(userMessage: String, contactName: String): ChatMessage {
        val char = characterRepository.getUserCharacter().firstOrNull()
        val directives = ascensionRepository.getAllDirectives().firstOrNull() ?: emptyList()
        val biometrics = biohackingDao.getBiohackingData(0).firstOrNull()
        val recentMemories = neuralMemoryDao.getMemoriesByWing("INSIGHTS").firstOrNull() ?: emptyList()
        val dopamineMenu = dopamineMenuRepository.getAllItems().firstOrNull() ?: emptyList()
        val relevantProtocols = protocolRepository.getAllProtocols().firstOrNull()?.asSequence()?.take(3)?.toList() ?: emptyList()

        val faithKeywords = listOf("faith", "god", "scripture", "prayer", "sonship", "theology", "jesus", "christ")
        val hasExplicitFaithLanguage = faithKeywords.any { userMessage.contains(it, ignoreCase = true) }

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

        val basePersona = AiPersona.promptFor(
            mode = AiMode.GUIDE,
            context = userContext,
            faithLens = hasExplicitFaithLanguage
        )

        val fullPrompt = """
            $basePersona
            
            USER_QUERY: "$userMessage"
            
            Action: Provide a guided, calm, high-impact response grounded in the user's context.
            - If vision or barriers are fuzzy, ask 1-2 clarifying questions.
            - Lead toward concrete micro-wins or actionable next steps.
            
            IMPORTANT: If suggesting actions, format them as: [ACTION: Label | Type | Data]
            Types allowed: MISSION, DOPAMINE, LOG.
        """.trimIndent()

        val aiResult = aiProvider.generate(fullPrompt, forceLocal = false)
        val aiResponse = when (aiResult) {
            is AiResult.Success -> aiResult.text
            is AiResult.Failure -> ""
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
