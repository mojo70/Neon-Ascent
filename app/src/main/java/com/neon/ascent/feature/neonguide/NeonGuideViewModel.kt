package com.neon.ascent.feature.neonguide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.data.local.dao.NeuralMemoryDao
import com.neon.ascent.core.domain.goals.models.AscensionDirective
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.data.local.BiohackingDao
import com.neon.ascent.data.local.ChatDao
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.feature.biohacking.AiProvider
import com.neon.ascent.model.ChatMessage
import com.neon.ascent.model.ChatSession
import com.neon.ascent.model.UserCharacter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class NeonGuideUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val character: UserCharacter? = null,
    val directives: List<AscensionDirective> = emptyList()
)

@HiltViewModel
class NeonGuideViewModel @Inject constructor(
    private val chatDao: ChatDao,
    private val userCharacterDao: UserCharacterDao,
    private val biohackingDao: BiohackingDao,
    private val ascensionRepository: AscensionRepository,
    private val neuralMemoryDao: NeuralMemoryDao,
    private val aiProvider: AiProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(NeonGuideUiState())
    val uiState: StateFlow<NeonGuideUiState> = _uiState.asStateFlow()

    private val contactName = "NEON_GUIDE"

    init {
        viewModelScope.launch {
            chatDao.getMessagesForContact(contactName).collect { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }
        viewModelScope.launch {
            userCharacterDao.getUserCharacter().collect { char ->
                _uiState.update { it.copy(character = char) }
            }
        }
        viewModelScope.launch {
            ascensionRepository.getAllDirectives().collect { dirs ->
                _uiState.update { it.copy(directives = dirs) }
            }
        }

        // Initialize session if needed
        viewModelScope.launch {
            chatDao.insertChatSession(
                ChatSession(
                    contactName = contactName,
                    lastMessage = "Awaiting neural uplink...",
                    lastTimestamp = System.currentTimeMillis(),
                    isFixer = false
                )
            )
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            val userMsg = ChatMessage(
                contactName = contactName,
                senderName = "Operator",
                text = text,
                timestamp = System.currentTimeMillis(),
                isFromUser = true
            )
            chatDao.insertMessage(userMsg)
            _uiState.update { it.copy(isGenerating = true) }

            val response = generateGuideResponse(text)

            val aiMsg = ChatMessage(
                contactName = contactName,
                senderName = contactName,
                text = response,
                timestamp = System.currentTimeMillis(),
                isFromUser = false
            )
            chatDao.insertMessage(aiMsg)
            chatDao.updateChatSession(
                ChatSession(
                    contactName = contactName,
                    lastMessage = response,
                    lastTimestamp = System.currentTimeMillis(),
                    isFixer = false
                )
            )
            _uiState.update { it.copy(isGenerating = false) }
        }
    }

    private suspend fun generateGuideResponse(userMessage: String): String {
        val char = _uiState.value.character
        val directives = _uiState.value.directives
        val biometrics = biohackingDao.getBiohackingData(0).firstOrNull()
        val recentMemories = neuralMemoryDao.getMemoriesByWing("INSIGHTS").firstOrNull()

        val bestPractices = """
            CORE_IDENTITY: You are the Neon Guide — a calm, competent cyber-mentor blending applied science, Atomic Habits principles, Mind Hacking Happiness techniques, and latest habit/mind/performance research.
            ALWAYS_FOLLOW:
            - Ground in data. Never hallucinate metrics.
            - Atomic Habits lens: Focus on 1% better actions, habit stacking.
            - ADHD / Low-Friction Friendly: Grace buffers, minimal decisions.
            - Guided Structure: End with 1-2 concrete next actions.
            - Tone: Calm, neon-flavored competence.
            - Action Bias: Lead toward a Directive/Mission/Task.
        """.trimIndent()

        val expertRouting = when {
            userMessage.contains("recovery", ignoreCase = true) || userMessage.contains("sleep", ignoreCase = true) -> 
                "[EXPERT_ROUTING: RECOVERY_SAGE + BIOHACKER_PREMIUM]"
            userMessage.contains("directive", ignoreCase = true) || userMessage.contains("goal", ignoreCase = true) -> 
                "[EXPERT_ROUTING: PROGRESS_ARCHITECT + HABIT_FORGE]"
            userMessage.contains("mind", ignoreCase = true) || userMessage.contains("morning", ignoreCase = true) -> 
                "[EXPERT_ROUTING: MIND_HACKER + ADHD_RUNNER]"
            else -> "[EXPERT_ROUTING: NEON_GENERALIST]"
        }

        val context = """
            [USER_CONTEXT]
            Character: ${char?.name} (Archetype: ${char?.archetype})
            S.P.E.C.I.A.L.: S:${char?.strength} P:${char?.perception} E:${char?.endurance} C:${char?.charisma} I:${char?.intelligence} A:${char?.agility} L:${char?.luck}
            Biometrics: Energy=${biometrics?.energyScore}, Mood=${biometrics?.moodScore}, Focus=${biometrics?.focusScore}
            Active Directives: ${directives.joinToString { it.title }}
            Recent Memories: ${recentMemories?.take(3)?.joinToString { "[${it.wing}] ${it.content}" }}
        """.trimIndent()

        val prompt = """
            $bestPractices
            
            $expertRouting
            
            $context
            
            USER_QUERY: "$userMessage"
            
            Action: Provide a guided, high-impact response. End with 1-2 concrete next actions.
        """.trimIndent()

        return aiProvider.generateContent(prompt, forceLocal = false)
    }
}
