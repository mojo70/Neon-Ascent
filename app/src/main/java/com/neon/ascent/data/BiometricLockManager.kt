package com.neon.ascent.data

import com.neon.ascent.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages in-memory app unlock state and evaluates background timeout for Biometric Node Lock.
 */
@Singleton
class BiometricLockManager @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private var lastBackgroundTimestamp: Long = 0L

    fun unlock() {
        _isAppUnlocked.value = true
    }

    fun lock() {
        _isAppUnlocked.value = false
    }

    fun onAppBackgrounded() {
        if (settingsRepository.isBiometricLockEnabled.value) {
            lastBackgroundTimestamp = System.currentTimeMillis()
        }
    }

    fun onAppForegrounded() {
        val isEnabled = settingsRepository.isBiometricLockEnabled.value
        if (!isEnabled) {
            _isAppUnlocked.value = true
            return
        }

        // If never unlocked yet (e.g. cold launch), stay locked
        if (!_isAppUnlocked.value) {
            return
        }

        // If unlocked previously, evaluate background timeout
        if (lastBackgroundTimestamp > 0L) {
            val elapsed = System.currentTimeMillis() - lastBackgroundTimestamp
            val timeoutMillis = getTimeoutMillis(settingsRepository.biometricLockTimeout.value)
            if (elapsed >= timeoutMillis) {
                _isAppUnlocked.value = false
            }
        }
    }

    fun getTimeoutMillis(timeoutSetting: String): Long {
        return when (timeoutSetting) {
            "1_MIN" -> 60_000L
            "5_MIN" -> 300_000L
            "15_MIN" -> 900_000L
            "IMMEDIATE" -> 0L
            else -> 0L
        }
    }
}
