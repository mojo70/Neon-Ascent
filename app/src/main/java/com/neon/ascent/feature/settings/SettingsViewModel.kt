package com.neon.ascent.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
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

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometricLockEnabled(enabled)
        }
    }

    fun resetProfile(onComplete: () -> Unit) {
        viewModelScope.launch {
            userCharacterDao.resetCharacter()
            onComplete()
        }
    }
}
