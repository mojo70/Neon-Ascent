package com.neon.ascent.feature.charactercreation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    }

    fun completeCreation(avatar: Bitmap) {
        viewModelScope.launch {
            // In a real app, we'd save the bitmap to internal storage and store the path
            val character = _uiState.value.copy(
                isCreationComplete = true,
                avatarPath = "internal_storage_placeholder" 
            )
            userCharacterDao.insertUserCharacter(character)
        }
    }
}
