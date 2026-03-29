package com.neon.ascent.feature.charactercreation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.neon.ascent.BuildConfig
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.model.UserCharacter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreationViewModel @Inject constructor(
    private val userCharacterDao: UserCharacterDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreationUiState>(CreationUiState.Initial)
    val uiState = _uiState.asStateFlow()

    private var draftCharacter = UserCharacter(
        name = "",
        sex = "Unknown",
        dob = "Unknown",
        units = "metric",
        weight = "0",
        somatotype = 0.5f
    )

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    fun updateBasicInfo(
        name: String,
        sex: String,
        dob: String,
        units: String,
        weight: String,
        somatotype: Float,
        heightFeet: String?,
        heightInches: String?,
        heightCm: String?
    ) {
        viewModelScope.launch {
            val finalCharacter = draftCharacter.copy(
                name = name,
                sex = sex,
                dob = dob,
                units = units,
                weight = weight,
                somatotype = somatotype,
                heightFeet = heightFeet,
                heightInches = heightInches,
                heightCm = heightCm,
                isCreationComplete = true // Mark as complete when basic info is submitted
            )
            userCharacterDao.insertUserCharacter(finalCharacter)
            _uiState.value = CreationUiState.Success
        }
    }

    fun updatePersonality(mbti: String, alignment: String, archetype: String) {
        viewModelScope.launch {
            userCharacterDao.getUserCharacter().collect { char ->
                char?.let {
                    val updated = it.copy(
                        mbti = mbti,
                        alignment = alignment,
                        archetype = archetype
                    )
                    userCharacterDao.insertUserCharacter(updated)
                }
            }
        }
    }

    fun completeCreation(avatarBitmap: Bitmap?) {
        viewModelScope.launch {
            _uiState.value = CreationUiState.Loading
            try {
                userCharacterDao.getUserCharacter().collect { char ->
                    char?.let {
                        val finalCharacter = it.copy(
                            isCreationComplete = true,
                            avatarPath = null 
                        )
                        userCharacterDao.insertUserCharacter(finalCharacter)
                        _uiState.value = CreationUiState.Success
                    }
                }
            } catch (e: Exception) {
                _uiState.value = CreationUiState.Error(e.message ?: "Failed to save character")
            }
        }
    }

    fun createCharacter(name: String, bio: String) {
        viewModelScope.launch {
            _uiState.value = CreationUiState.Loading
            try {
                val response = generativeModel.generateContent(
                    content {
                        text("Create a cyberpunk character profile for a person named $name with the following bio: $bio. Return it as JSON.")
                    }
                )
                val character = UserCharacter(
                    name = name,
                    sex = "Unknown",
                    dob = "Unknown",
                    units = "metric",
                    weight = "0",
                    somatotype = 0.5f,
                    level = 1,
                    neuralLoad = 0.2f,
                    isCreationComplete = true
                )
                userCharacterDao.insertUserCharacter(character)
                _uiState.value = CreationUiState.Success
            } catch (e: Exception) {
                _uiState.value = CreationUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class CreationUiState {
    object Initial : CreationUiState()
    object Loading : CreationUiState()
    object Success : CreationUiState()
    data class Error(val message: String) : CreationUiState()
}
