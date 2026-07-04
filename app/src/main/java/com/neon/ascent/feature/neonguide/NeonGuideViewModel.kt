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
    val directives: List<AscensionDirective> = emptyList(),
    val sessions: List<ChatSession> = emptyList(),
    val currentSessionId: String? = null
)

@HiltViewModel
class NeonGuideViewModel @Inject constructor(
    private val chatDao: ChatDao,
    private val userCharacterDao: UserCharacterDao,
    private val biohackingDao: BiohackingDao,
    private val ascensionRepository: AscensionRepository,
    private val dopamineMenuRepository: DopamineMenuRepository,
    private val guideUseCase: NeonGuideUseCase,
    private val savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(NeonGuideUiState())
    val uiState: StateFlow<NeonGuideUiState> = _uiState.asStateFlow()

    private val contactName = "NEON_GUIDE"

    init {
        viewModelScope.launch {
            chatDao.getSessionsForContact(contactName).collect { sessions ->
                _uiState.update { it.copy(sessions = sessions) }
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

        initializeSession()
    }

    private fun initializeSession() {
        viewModelScope.launch {
            val latestSession = chatDao.getLatestSessionForContact(contactName)
            val now = System.currentTimeMillis()
            val dayInMillis = 24 * 60 * 60 * 1000L

            val sessionId = if (latestSession == null || (now - latestSession.lastTimestamp) > dayInMillis) {
                startNewConversationSync()
            } else {
                latestSession.sessionId
            }
            
            loadSession(sessionId)
            
            // Handle initial message from deep link
            savedStateHandle.get<String>("initialMessage")?.let { 
                sendMessage(it)
                savedStateHandle.remove<String>("initialMessage")
            }
        }
    }

    private suspend fun startNewConversationSync(): String {
        val newSessionId = UUID.randomUUID().toString()
        val session = ChatSession(
            sessionId = newSessionId,
            contactName = contactName,
            lastMessage = "CONNECTION_ESTABLISHED",
            lastTimestamp = System.currentTimeMillis(),
            isFixer = false
        )
        chatDao.insertChatSession(session)
        return newSessionId
    }

    fun startNewConversation() {
        viewModelScope.launch {
            val id = startNewConversationSync()
            loadSession(id)
        }
    }

    fun loadSession(sessionId: String) {
        _uiState.update { it.copy(currentSessionId = sessionId) }
        viewModelScope.launch {
            chatDao.getMessagesForSession(sessionId).collect { msgs ->
                if (_uiState.value.currentSessionId == sessionId) {
                    _uiState.update { it.copy(messages = msgs) }
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val sessionId = _uiState.value.currentSessionId ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            val userMsg = ChatMessage(
                sessionId = sessionId,
                contactName = contactName,
                senderName = "Operator",
                text = text,
                timestamp = System.currentTimeMillis(),
                isFromUser = true
            )
            chatDao.insertMessage(userMsg)
            _uiState.update { it.copy(isGenerating = true) }

            try {
                val aiMsg = guideUseCase.generateResponse(text, contactName).copy(sessionId = sessionId)
                chatDao.insertMessage(aiMsg)
                
                chatDao.getSessionById(sessionId)?.let { session ->
                    chatDao.updateChatSession(
                        session.copy(
                            lastMessage = aiMsg.text.take(50) + "...",
                            lastTimestamp = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                val errorMsg = ChatMessage(
                    sessionId = sessionId,
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
        val sessionId = _uiState.value.currentSessionId ?: return
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
                sessionId = sessionId,
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
