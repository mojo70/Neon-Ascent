package com.neon.ascent.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.local.BiohackingDao
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
    private val biohackingDao: BiohackingDao,
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

    /**
     * RESET_PROTOCOL: FULL_WIPE
     * First-time behaviors to reset:
     * 1. UserCharacter (Level, Eddies, etc.)
     * 2. Netrunner Mode (Settings) -> OFF
     * 3. AI Core Welcome Protocol (Settings) -> READY
     * 4. Biohacking Privacy Onboarding (Database) -> READY
     */
    fun resetProfile(onComplete: () -> Unit) {
        viewModelScope.launch {
            // 1. Reset character data
            userCharacterDao.resetCharacter()
            
            // 2. Reset Settings Repository Flags
            settingsRepository.setReligionShortcutEnabled(false)
            settingsRepository.setLocalAiOnly(false)
            settingsRepository.setNetrunnerMode(false) // Netrunner selection back to OFF
            settingsRepository.setFirstAiCoreEntry(true) // Re-enable AI Core welcome protocol
            
            // 3. Reset Biohacking State
            // This triggers the Privacy Onboarding prompt again by deleting the local data record
            biohackingDao.deleteBiohackingData(0)
            biohackingDao.deleteBioProtocolLogs(0)

            onComplete()
        }
    }

    fun acceptHolyGhost() {
        viewModelScope.launch {
            userCharacterDao.updateHolyGhost(1)
        }
    }
}
