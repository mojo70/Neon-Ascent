package com.neon.ascent.feature.charactercreation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.neon.ascent.BuildConfig
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.model.UserCharacter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreationViewModel @Inject constructor(
    private val userCharacterDao: UserCharacterDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserCharacter(name = "", sex = "", dob = "", units = "", weight = "", somatotype = 5f))
    val uiState: StateFlow<UserCharacter> = _uiState

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    fun updateBasicInfo(name: String, sex: String, dob: String, units: String, weight: String, somatotype: Float, ft: String?, inches: String?, cm: String?) {
        _uiState.value = _uiState.value.copy(
            name = name,
            sex = sex,
            dob = dob,
            units = units,
            weight = weight,
            somatotype = somatotype,
            heightFeet = ft,
            heightInches = inches,
            heightCm = cm
        )
    }

    fun updatePersonality(mbti: String, alignment: String, archetype: String) {
        _uiState.value = _uiState.value.copy(
            mbti = mbti,
            alignment = alignment,
            archetype = archetype
        )
        generateNetrunnerName()
    }

    private fun generateNetrunnerName() {
        viewModelScope.launch {
            val character = _uiState.value
            val prompt = "Generate a cool, one-word cyberpunk netrunner alias for a character with these traits: " +
                    "Archetype: ${character.archetype}, MBTI: ${character.mbti}, Alignment: ${character.alignment}. " +
                    "Examples: Zero, Glitch, Hex, Vector, Cipher. Return only the name."
            
            try {
                val response = generativeModel.generateContent(prompt)
                val generatedName = response.text?.trim()?.filter { it.isLetterOrDigit() } ?: "RUNNER_${(1000..9999).random()}"
                _uiState.value = _uiState.value.copy(netrunnerName = generatedName)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(netrunnerName = "RUNNER_${(1000..9999).random()}")
            }
        }
    }

    fun completeCreation(avatar: Bitmap) {
        viewModelScope.launch {
            val character = _uiState.value.copy(
                isCreationComplete = true,
                avatarPath = "internal_storage_placeholder" 
            )
            userCharacterDao.insertUserCharacter(character)
        }
    }
}
