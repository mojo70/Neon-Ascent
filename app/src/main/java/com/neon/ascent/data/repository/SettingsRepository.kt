package com.neon.ascent.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _isBiometricLockEnabled = MutableStateFlow(getBiometricLockEnabled())
    val isBiometricLockEnabled: StateFlow<Boolean> = _isBiometricLockEnabled

    private val _isReligionShortcutEnabled = MutableStateFlow(getReligionShortcutEnabled())
    val isReligionShortcutEnabled: StateFlow<Boolean> = _isReligionShortcutEnabled

    private val _isLocalAiOnly = MutableStateFlow(getLocalAiOnly())
    val isLocalAiOnly: StateFlow<Boolean> = _isLocalAiOnly

    fun setBiometricLockEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("biometric_lock", enabled).apply()
        _isBiometricLockEnabled.value = enabled
    }

    fun setReligionShortcutEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("religion_shortcut", enabled).apply()
        _isReligionShortcutEnabled.value = enabled
    }

    fun setLocalAiOnly(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("local_ai_only", enabled).apply()
        _isLocalAiOnly.value = enabled
    }

    private fun getBiometricLockEnabled(): Boolean {
        return sharedPreferences.getBoolean("biometric_lock", false)
    }

    private fun getReligionShortcutEnabled(): Boolean {
        return sharedPreferences.getBoolean("religion_shortcut", false)
    }

    private fun getLocalAiOnly(): Boolean {
        return sharedPreferences.getBoolean("local_ai_only", false)
    }
}
