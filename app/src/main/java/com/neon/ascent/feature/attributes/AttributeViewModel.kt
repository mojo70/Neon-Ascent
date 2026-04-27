package com.neon.ascent.feature.attributes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.repository.CharacterRepository
import com.neon.ascent.feature.biohacking.AiProvider
import com.neon.ascent.model.UserCharacter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttributeViewModel @Inject constructor(
    private val characterRepository: CharacterRepository,
    private val aiProvider: AiProvider
) : ViewModel() {

    private val _userCharacter = MutableStateFlow<UserCharacter?>(null)
    val userCharacter: StateFlow<UserCharacter?> = _userCharacter.asStateFlow()

    private val _aiResponse = MutableStateFlow<String>("")
    val aiResponse: StateFlow<String> = _aiResponse.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    init {
        viewModelScope.launch {
            characterRepository.getUserCharacter().collect {
                _userCharacter.value = it
            }
        }
    }

    fun askAi(attribute: AttributeDetail, message: String) {
        viewModelScope.launch {
            _isChatLoading.value = true
            val prompt = """
                You are ${attribute.aiExpertName}. 
                Your persona: ${attribute.aiExpertPersona}
                Your personality: ${attribute.aiPersonalityDescription}
                
                The user is asking about the ${attribute.name} attribute in the Neon Ascent system.
                
                User Message: $message
                
                Respond in character, keeping it brief and cyberpunk-themed.
            """.trimIndent()

            val response = aiProvider.generateContent(prompt)
            _aiResponse.value = response
            _isChatLoading.value = false
        }
    }
}
