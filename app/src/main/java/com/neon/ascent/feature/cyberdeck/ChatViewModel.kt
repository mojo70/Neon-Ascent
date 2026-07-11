package com.neon.ascent.feature.cyberdeck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.local.ChatDao
import com.neon.ascent.data.local.LoreDao
import com.neon.ascent.feature.biohacking.AiProvider
import com.neon.ascent.core.lore.data.LoreRepository
import com.neon.ascent.core.lore.data.Megacorp
import com.neon.ascent.data.repository.CharacterRepository
import com.neon.ascent.core.domain.character.models.UserCharacter
import com.neon.ascent.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatDao: ChatDao,
    private val aiProvider: AiProvider,
    private val loreRepository: LoreRepository,
    private val loreDao: LoreDao,
    private val characterRepository: CharacterRepository
) : ViewModel() {

    val userCharacter: StateFlow<UserCharacter?> = characterRepository.getUserCharacter()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _megacorps = MutableStateFlow<List<Megacorp>>(emptyList())
    val megacorps: StateFlow<List<Megacorp>> = _megacorps.asStateFlow()

    val executiveTrust: StateFlow<Map<String, Float>> = loreDao.getAllCorpoTrust()
        .map { list -> list.associate { it.corpoId to it.trustLevel } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val chatSessions: StateFlow<List<ChatSession>> = chatDao.getChatSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val messages: StateFlow<List<ChatMessage>> = _currentSessionId.flatMapLatest { sessionId ->
        if (sessionId == null) flowOf(emptyList())
        else chatDao.getMessagesForSession(sessionId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectContact(name: String) {
        viewModelScope.launch {
            val latestSession = chatDao.getLatestSessionForContact(name)
            if (latestSession != null) {
                _currentSessionId.value = latestSession.sessionId
                chatDao.markAsRead(latestSession.sessionId)
            } else {
                // Should not happen for predefined fixers
                addContact(name, isFixer = true)
            }
        }
    }

    fun sendMessage(text: String) {
        val sessionId = _currentSessionId.value ?: return
        viewModelScope.launch {
            val session = chatDao.getSessionById(sessionId) ?: return@launch
            val contactName = session.contactName
            val timestamp = System.currentTimeMillis()
            val userMsg = ChatMessage(
                sessionId = sessionId,
                contactName = contactName,
                senderName = "USER",
                text = text,
                timestamp = timestamp,
                isFromUser = true
            )
            chatDao.insertMessage(userMsg)
            
            chatDao.updateChatSession(session.copy(lastMessage = text, lastTimestamp = timestamp, isUnread = false))
            
            // Special case for Thrust's first message
            if (contactName == "Thrust" && messages.value.size == 1) { 
                val thrusterGreeting = "Grid connection established. This is Vance ‘Thrust’ Calder, CEO of AetherX. Who the hell is this and why are you pinging my private line? Make it good — I’ve got a Heavy Starship leaving for Uranus in 47 minutes and I’m not in the mood for bullshit."
                val greetingTimestamp = System.currentTimeMillis() + 100
                val aiMsg = ChatMessage(
                    sessionId = sessionId,
                    contactName = contactName,
                    senderName = contactName,
                    text = thrusterGreeting,
                    timestamp = greetingTimestamp,
                    isFromUser = false
                )
                chatDao.insertMessage(aiMsg)
                chatDao.updateChatSession(session.copy(lastMessage = thrusterGreeting, lastTimestamp = greetingTimestamp, isUnread = true))
            } else if (contactName == "Mojo" && messages.value.size == 1) {
                val mojoGreeting = "Grid connection established. Yo, Mojo here. CEO of MojoTyger and primary victim of my own codebases. I'm probably looking at a stack trace or shipping a hotfix right now. If you're a runner, talk tech, or hit me with a top-tier shitpost. Just don't ask me to deploy on a Friday."
                val greetingTimestamp = System.currentTimeMillis() + 100
                val aiMsg = ChatMessage(
                    sessionId = sessionId,
                    contactName = contactName,
                    senderName = contactName,
                    text = mojoGreeting,
                    timestamp = greetingTimestamp,
                    isFromUser = false
                )
                chatDao.insertMessage(aiMsg)
                chatDao.updateChatSession(session.copy(lastMessage = mojoGreeting, lastTimestamp = greetingTimestamp, isUnread = true))
            } else if (session.isFixer) {
                generateAiResponse(session, text)
            }
        }
    }

    private fun generateAiResponse(session: ChatSession, userText: String) {
        viewModelScope.launch {
            val personality = session.personalityPrompt ?: "You are a mysterious Netrunner."
            val prompt = "$personality\nUser says: \"$userText\"\nRespond in character, keep it short."
            
            val response = aiProvider.generateContent(prompt)
            val timestamp = System.currentTimeMillis()
            
            val aiMsg = ChatMessage(
                sessionId = session.sessionId,
                contactName = session.contactName,
                senderName = session.contactName,
                text = response,
                timestamp = timestamp,
                isFromUser = false
            )
            chatDao.insertMessage(aiMsg)
            chatDao.updateChatSession(session.copy(lastMessage = response, lastTimestamp = timestamp, isUnread = true))
        }
    }

    fun addContact(name: String, isFixer: Boolean = false) {
        viewModelScope.launch {
            val existing = chatDao.getLatestSessionForContact(name)
            if (existing != null) {
                _currentSessionId.value = existing.sessionId
                return@launch
            }

            val megacorp = _megacorps.value.find { it.ceo.netHandle == name }
            val personality = if (megacorp != null) {
                megacorp.ceo.gemmaPromptPath?.let { loreRepository.loadCeoPrompt(it) } ?: megacorp.ceo.personality
            } else if (isFixer) {
                PREDEFINED_FIXERS.find { it.name == name }?.personality
            } else {
                "Netrunner colleague."
            }
            
            val lastMsg = "CONNECTION_ESTABLISHED"
            val sessionId = java.util.UUID.randomUUID().toString()

            val session = ChatSession(
                sessionId = sessionId,
                contactName = name,
                lastMessage = lastMsg,
                lastTimestamp = System.currentTimeMillis(),
                isUnread = true,
                isFixer = isFixer || (megacorp != null) || (name == "Thrust"),
                personalityPrompt = personality
            )
            chatDao.insertChatSession(session)
            _currentSessionId.value = sessionId
        }
    }

    init {
        // Pre-populate with fixers if empty (except Thrust)
        viewModelScope.launch {
            _megacorps.value = loreRepository.getAllMegacorps()
            
            // Seed trust levels if empty
            val existingTrust = loreDao.getAllCorpoTrust().first()
            if (existingTrust.isEmpty()) {
                val initialTrust = _megacorps.value.map { 
                    com.neon.ascent.model.CorpoTrust(it.id, 0.0f)
                }
                initialTrust.forEach { loreDao.insertCorpoTrust(it) }
            }

            if (chatSessions.value.isEmpty()) {
                PREDEFINED_FIXERS.forEach { fixer ->
                    // Only add street fixers, not corporate CEOs
                    if (fixer.name != "Thrust") {
                        addContact(fixer.name, isFixer = true)
                    }
                }
            }
        }
    }
}
