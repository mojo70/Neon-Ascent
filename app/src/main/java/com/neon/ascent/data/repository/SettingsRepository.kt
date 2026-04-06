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

/**
 * --------------------------------------------------------------------------------
 * FIRST-TIME BEHAVIORS REGISTRY (Neural Profile Initializers)
 * --------------------------------------------------------------------------------
 * This registry documents all behaviors that trigger upon first-time usage or after 
 * a "Neural Profile Reset". 
 *
 * 1. AI_CORE_WELCOME_PROTOCOL
 *    - Logic: CoreDashboardViewModel.checkFirstEntry()
 *    - Trigger: SettingsRepository.isFirstAiCoreEntry is true.
 *    - Behavior: Prepend a "WELCOME_INITIALIZATION" log with a random 
 *      "Soul in the Machine" saying to the diagnostics history.
 *    - Reset: Handled in SettingsViewModel.resetProfile().
 *
 * 2. BIOHACKING_PRIVACY_ONBOARDING
 *    - Logic: BiohackingScreen.kt (PrivacyOnboarding overlay)
 *    - Trigger: BiohackingData.hasCompletedPrivacyOnboarding is false.
 *    - Behavior: Forces a full-screen data agreement and neural core toggle.
 *    - Reset: Handled via biohackingDao.deleteBiohackingData(0) in resetProfile().
 *
 * 3. NETRUNNER_DEFAULT_STATE
 *    - Logic: SettingsRepository.isNetrunnerMode
 *    - Behavior: Must default to OFF (false) to protect the user's kernel.
 *    - Reset: Set to false in SettingsViewModel.resetProfile().
 *
 * 4. CHARACTER_CREATION_FLOW
 *    - Logic: AppNavigation.kt (LoadingScreen check)
 *    - Trigger: UserCharacter.isCreationComplete is false.
 *    - Behavior: Redirects user to the multi-step character creation/personality scan.
 *    - Reset: Handled by userCharacterDao.resetCharacter() in resetProfile().
 *
 * 5. DATABASE_SEEDING
 *    - Logic: DashboardViewModel.seedSayingsIfEmpty()
 *    - Trigger: sayingsDao.count() == 0.
 *    - Behavior: Populates the world with initial lore, philosophy, and street wisdom.
 * --------------------------------------------------------------------------------
 */

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
