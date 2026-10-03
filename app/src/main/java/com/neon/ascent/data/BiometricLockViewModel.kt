package com.neon.ascent.data

import androidx.lifecycle.ViewModel
import com.neon.ascent.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class BiometricLockViewModel @Inject constructor(
    private val biometricLockManager: BiometricLockManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val isBiometricLockEnabled: StateFlow<Boolean> = settingsRepository.isBiometricLockEnabled
    val isAppUnlocked: StateFlow<Boolean> = biometricLockManager.isAppUnlocked

    fun unlock() {
        biometricLockManager.unlock()
    }

    fun onAppBackgrounded() {
        biometricLockManager.onAppBackgrounded()
    }

    fun onAppForegrounded() {
        biometricLockManager.onAppForegrounded()
    }
}
