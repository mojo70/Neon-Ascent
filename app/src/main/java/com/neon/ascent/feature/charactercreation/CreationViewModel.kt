package com.neon.ascent.feature.charactercreation

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.neon.ascent.BuildConfig
import com.neon.ascent.data.repository.CharacterRepository
import com.neon.ascent.data.repository.UserPreferencesRepository
import com.neon.ascent.domain.onboarding.OnboardingCompletionUseCase
import com.neon.ascent.core.domain.character.models.UserCharacter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class CreationViewModel @Inject constructor(
    private val characterRepository: CharacterRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val onboardingCompletionUseCase: OnboardingCompletionUseCase,
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<CreationUiState>(CreationUiState.Initial)
    val uiState = _uiState.asStateFlow()

    val userCharacter: StateFlow<UserCharacter?> = characterRepository.getUserCharacter()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var draftCharacter = UserCharacter(
        name = "",
        sex = "Unknown",
        dob = "Unknown",
        units = "metric",
        weight = "0",
        somatotype = 0.5f
    )

    init {
        viewModelScope.launch {
            characterRepository.getUserCharacter().collect { char ->
                char?.let { draftCharacter = it }
            }
        }
    }

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    fun abort(onComplete: () -> Unit) {
        viewModelScope.launch {
            characterRepository.saveCharacter(draftCharacter)
            onComplete()
        }
    }

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
            val existing = characterRepository.getUserCharacter().first()
            draftCharacter = (existing ?: draftCharacter).copy(
                name = name,
                sex = sex,
                dob = dob,
                units = units,
                weight = weight,
                somatotype = somatotype,
                heightFeet = heightFeet,
                heightInches = heightInches,
                heightCm = heightCm
            )
            characterRepository.saveCharacter(draftCharacter)
            userPreferencesRepository.updateMeasurementUnit(units)
        }
    }

    fun updatePersonality(mbti: String, alignment: String, archetype: String) {
        viewModelScope.launch {
            val existing = characterRepository.getUserCharacter().first()
            draftCharacter = (existing ?: draftCharacter).copy(
                mbti = mbti,
                alignment = alignment,
                archetype = archetype
            )
            characterRepository.saveCharacter(draftCharacter)
        }
    }

    fun completeCreation(avatarBitmap: Bitmap?) {
        viewModelScope.launch {
            _uiState.value = CreationUiState.Loading
            try {
                val existing = characterRepository.getUserCharacter().first()
                draftCharacter = existing ?: draftCharacter

                var savedPath: String? = null
                if (avatarBitmap != null) {
                    val file = File(getApplication<Application>().filesDir, "avatar_${System.currentTimeMillis()}.png")
                    FileOutputStream(file).use { out ->
                        avatarBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    savedPath = file.absolutePath
                }

                draftCharacter = draftCharacter.copy(
                    isCreationComplete = true,
                    avatarPath = savedPath ?: draftCharacter.avatarPath
                )
                characterRepository.saveCharacter(draftCharacter)

                // Seed starter habits and schedule Neural Pings
                draftCharacter.archetype?.let { archetype ->
                    onboardingCompletionUseCase(archetype)
                }

                _uiState.value = CreationUiState.Success
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
                characterRepository.saveCharacter(character)
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
