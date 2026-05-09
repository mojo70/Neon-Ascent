package com.neon.ascent.feature.cyberdeck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.local.ChatDao
import com.neon.ascent.feature.biohacking.AiProvider
import com.neon.ascent.core.lore.data.LoreRepository
import com.neon.ascent.core.lore.data.Megacorp
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
    private val loreRepository: LoreRepository
) : ViewModel() {

    private val _megacorps = MutableStateFlow<List<Megacorp>>(emptyList())
    val megacorps: StateFlow<List<Megacorp>> = _megacorps.asStateFlow()

    private val _executiveTrust = MutableStateFlow<Map<String, Float>>(emptyMap())
    val executiveTrust: StateFlow<Map<String, Float>> = _executiveTrust.asStateFlow()

    val chatSessions: StateFlow<List<ChatSession>> = chatDao.getChatSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentContact = MutableStateFlow<String?>(null)
    val messages: StateFlow<List<ChatMessage>> = _currentContact.flatMapLatest { contact ->
        if (contact == null) flowOf(emptyList())
        else chatDao.getMessagesForContact(contact)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectContact(name: String) {
        _currentContact.value = name
        viewModelScope.launch {
            chatDao.markAsRead(name)
        }
    }

    fun sendMessage(text: String) {
        val contactName = _currentContact.value ?: return
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            val userMsg = ChatMessage(
                contactName = contactName,
                senderName = "USER",
                text = text,
                timestamp = timestamp,
                isFromUser = true
            )
            chatDao.insertMessage(userMsg)
            
            // Update session
            val session = chatSessions.value.find { it.contactName == contactName }
            if (session != null) {
                chatDao.insertChatSession(session.copy(lastMessage = text, lastTimestamp = timestamp, isUnread = false))
                
                // Special case for Thrust's first message
                if (contactName == "Thrust" && messages.value.size == 1) { // Size 1 because we just inserted the user message
                    val thrusterGreeting = "Grid connection established. This is Vance ‘Thrust’ Calder, CEO of AetherX. Who the hell is this and why are you pinging my private line? Make it good — I’ve got a Heavy Starship leaving for Uranus in 47 minutes and I’m not in the mood for bullshit."
                    val greetingTimestamp = System.currentTimeMillis() + 100
                    val aiMsg = ChatMessage(
                        contactName = contactName,
                        senderName = contactName,
                        text = thrusterGreeting,
                        timestamp = greetingTimestamp,
                        isFromUser = false
                    )
                    chatDao.insertMessage(aiMsg)
                    chatDao.insertChatSession(session.copy(lastMessage = thrusterGreeting, lastTimestamp = greetingTimestamp, isUnread = true))
                } else if (session.isFixer) {
                    generateAiResponse(session, text)
                }
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
                contactName = session.contactName,
                senderName = session.contactName,
                text = response,
                timestamp = timestamp,
                isFromUser = false
            )
            chatDao.insertMessage(aiMsg)
            chatDao.insertChatSession(session.copy(lastMessage = response, lastTimestamp = timestamp, isUnread = true))
        }
    }

    fun addContact(name: String, isFixer: Boolean = false) {
        viewModelScope.launch {
            val megacorp = _megacorps.value.find { it.ceo.netHandle == name }
            val personality = if (megacorp != null) {
                loreRepository.loadCeoPrompt(megacorp.ceo.gemmaPromptPath)
            } else if (isFixer) {
                PREDEFINED_FIXERS.find { it.name == name }?.personality
            } else {
                "Netrunner colleague."
            }
            
            val lastMsg = "CONNECTION_ESTABLISHED"

            val session = ChatSession(
                contactName = name,
                lastMessage = lastMsg,
                lastTimestamp = System.currentTimeMillis(),
                isUnread = true,
                isFixer = isFixer || (megacorp != null) || (name == "Thrust"),
                personalityPrompt = personality
            )
            chatDao.insertChatSession(session)
        }
    }

    init {
        // Pre-populate with fixers if empty (except Thrust)
        viewModelScope.launch {
            _megacorps.value = loreRepository.getAllMegacorps()
            
            // Mock trust levels for demo
            _executiveTrust.value = mapOf(
                "aetherx" to 0.45f,
                "panopticon" to 0.12f,
                "microhard" to 0.05f,
                "obsidianveil" to 0.28f
            )

            if (chatSessions.value.isEmpty()) {
                PREDEFINED_FIXERS.forEach { fixer ->
                    if (fixer.name != "Thrust") {
                        addContact(fixer.name, isFixer = true)
                    }
                }
            }
        }
    }
}
