package com.neon.ascent.feature.neonguide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.goals.models.AscensionDirective
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.data.local.ChatDao
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.model.ChatMessage
import com.neon.ascent.model.ChatSession
import com.neon.ascent.model.UserCharacter
import com.neon.ascent.core.domain.model.DopamineCategory
import com.neon.ascent.core.domain.model.DopamineMenuItem
import com.neon.ascent.core.domain.model.EnergyLevel
import com.neon.ascent.core.domain.repository.DopamineMenuRepository
import com.neon.ascent.data.local.BiohackingDao
import com.neon.ascent.model.BioProtocolLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
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
    private val dopamineMenuRepository: DopamineMenuRepository,
    private val guideUseCase: NeonGuideUseCase,
    savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(NeonGuideUiState())
    val uiState: StateFlow<NeonGuideUiState> = _uiState.asStateFlow()

    private val contactName = "NEON_GUIDE"

    init {
        val initialMessage: String? = savedStateHandle["initialMessage"]
        if (initialMessage != null) {
            sendMessage(initialMessage)
        }
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

            try {
                val aiMsg = guideUseCase.generateResponse(text, contactName)
                chatDao.insertMessage(aiMsg)
                chatDao.updateChatSession(
                    ChatSession(
                        contactName = contactName,
                        lastMessage = aiMsg.text,
                        lastTimestamp = System.currentTimeMillis(),
                        isFixer = false
                    )
                )
            } catch (e: Exception) {
                val errorMsg = ChatMessage(
                    contactName = contactName,
                    senderName = "SYSTEM",
                    text = "ERROR: Neural link unstable. Signal dropped. (Details: ${e.message})",
                    timestamp = System.currentTimeMillis(),
                    isFromUser = false
                )
                chatDao.insertMessage(errorMsg)
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun handleAction(action: com.neon.ascent.model.ChatAction) {
        viewModelScope.launch {
            when (action.type) {
                "MISSION" -> {
                    val firstDir = _uiState.value.directives.firstOrNull()
                    if (firstDir != null) {
                        val mission = com.neon.ascent.core.domain.goals.models.AscensionMission(
                            id = UUID.randomUUID().toString(),
                            directiveId = firstDir.id,
                            title = action.data ?: action.label,
                            description = "Guided Mission: ${action.label}",
                            status = com.neon.ascent.core.domain.goals.models.AscensionMissionStatus.ACTIVE
                        )
                        ascensionRepository.insertMission(mission)
                    }
                }
                "DOPAMINE" -> {
                    val item = DopamineMenuItem(
                        id = UUID.randomUUID().toString(),
                        title = action.data ?: action.label,
                        description = "Suggested by Neon Guide",
                        durationMinutes = 10,
                        category = DopamineCategory.RESET,
                        specialTags = emptyList(),
                        energyLevel = EnergyLevel.MEDIUM
                    )
                    dopamineMenuRepository.upsertItem(item)
                }
                "LOG" -> {
                    val log = BioProtocolLog(
                        userId = 0,
                        timestamp = System.currentTimeMillis(),
                        energyScore = 5,
                        sleepQuality = 5,
                        moodScore = 5,
                        focusScore = 5,
                        sideEffects = "NONE",
                        notes = "ACTION_LOG: ${action.label} | ${action.data}",
                        protocolId = "GUIDE_ACTION_${UUID.randomUUID().toString().take(8)}"
                    )
                    biohackingDao.insertProtocolLog(log)
                }
            }
            
            val confirmation = ChatMessage(
                contactName = contactName,
                senderName = "SYSTEM",
                text = "ACTION_EXECUTED: ${action.label}. Uplink synchronized.",
                timestamp = System.currentTimeMillis(),
                isFromUser = false
            )
            chatDao.insertMessage(confirmation)
        }
    }
}
