package com.neon.ascent.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userCharacterDao: UserCharacterDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val isBiometricLockEnabled: StateFlow<Boolean> = settingsRepository.isBiometricLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isReligionShortcutEnabled: StateFlow<Boolean> = settingsRepository.isReligionShortcutEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isLocalAiOnly: StateFlow<Boolean> = settingsRepository.isLocalAiOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometricLockEnabled(enabled)
        }
    }

    fun setReligionShortcutEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReligionShortcutEnabled(enabled)
        }
    }

    fun setLocalAiOnly(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLocalAiOnly(enabled)
        }
    }

    fun resetProfile(onComplete: () -> Unit) {
        viewModelScope.launch {
            // Reset character data
            userCharacterDao.resetCharacter()
            // Reset settings
            settingsRepository.setReligionShortcutEnabled(false)
            settingsRepository.setLocalAiOnly(false)
            onComplete()
        }
    }

    fun acceptHolyGhost() {
        viewModelScope.launch {
            userCharacterDao.updateHolyGhost(1)
        }
    }
}
