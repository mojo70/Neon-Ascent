package com.neon.ascent.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _isBiometricLockEnabled = MutableStateFlow(sharedPreferences.getBoolean("biometric_lock", false))
    val isBiometricLockEnabled: StateFlow<Boolean> = _isBiometricLockEnabled.asStateFlow()

    private val _isReligionShortcutEnabled = MutableStateFlow(sharedPreferences.getBoolean("religion_shortcut", false))
    val isReligionShortcutEnabled: StateFlow<Boolean> = _isReligionShortcutEnabled.asStateFlow()

    private val _isLocalAiOnly = MutableStateFlow(sharedPreferences.getBoolean("local_ai_only", false))
    val isLocalAiOnly: StateFlow<Boolean> = _isLocalAiOnly.asStateFlow()

    // AI Parameters
    private val _nanoTemperature = MutableStateFlow(sharedPreferences.getFloat("nano_temp", 0.7f))
    val nanoTemperature = _nanoTemperature.asStateFlow()

    private val _cloudFallbackThreshold = MutableStateFlow(sharedPreferences.getFloat("cloud_fallback", 20f))
    val cloudFallbackThreshold = _cloudFallbackThreshold.asStateFlow()

    private val _philosophySeed = MutableStateFlow(sharedPreferences.getString("philosophy_seed", "PLATO") ?: "PLATO")
    val philosophySeed = _philosophySeed.asStateFlow()

    private val _isNetrunnerMode = MutableStateFlow(sharedPreferences.getBoolean("netrunner_mode", false))
    val isNetrunnerMode: StateFlow<Boolean> = _isNetrunnerMode.asStateFlow()

    private val _isFirstAiCoreEntry = MutableStateFlow(sharedPreferences.getBoolean("first_ai_core_entry", true))
    val isFirstAiCoreEntry: StateFlow<Boolean> = _isFirstAiCoreEntry.asStateFlow()

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

    fun setNanoTemperature(temp: Float) {
        sharedPreferences.edit().putFloat("nano_temp", temp).apply()
        _nanoTemperature.value = temp
    }

    fun setCloudFallbackThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("cloud_fallback", threshold).apply()
        _cloudFallbackThreshold.value = threshold
    }

    fun setPhilosophySeed(seed: String) {
        sharedPreferences.edit().putString("philosophy_seed", seed).apply()
        _philosophySeed.value = seed
    }

    fun setNetrunnerMode(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("netrunner_mode", enabled).apply()
        _isNetrunnerMode.value = enabled
    }

    fun setFirstAiCoreEntry(isFirst: Boolean) {
        sharedPreferences.edit().putBoolean("first_ai_core_entry", isFirst).apply()
        _isFirstAiCoreEntry.value = isFirst
    }
}
