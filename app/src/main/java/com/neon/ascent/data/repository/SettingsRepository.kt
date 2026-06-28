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

    // Notification Settings
    private val _isNeuralBriefEnabled = MutableStateFlow(sharedPreferences.getBoolean("neural_brief_enabled", true))
    val isNeuralBriefEnabled = _isNeuralBriefEnabled.asStateFlow()

    private val _quietHoursStart = MutableStateFlow(sharedPreferences.getString("quiet_hours_start", "22:00") ?: "22:00")
    val quietHoursStart = _quietHoursStart.asStateFlow()

    private val _quietHoursEnd = MutableStateFlow(sharedPreferences.getString("quiet_hours_end", "07:00") ?: "07:00")
    val quietHoursEnd = _quietHoursEnd.asStateFlow()

    private val _briefFrequency = MutableStateFlow(sharedPreferences.getString("brief_frequency", "DAILY") ?: "DAILY")
    val briefFrequency = _briefFrequency.asStateFlow()

    private val _insightDepth = MutableStateFlow(sharedPreferences.getString("insight_depth", "DETAILED") ?: "DETAILED")
    val insightDepth = _insightDepth.asStateFlow()

    private val _hasCompletedSinnersPrayer = MutableStateFlow(sharedPreferences.getBoolean("has_completed_sinners_prayer", false))
    val hasCompletedSinnersPrayer: StateFlow<Boolean> = _hasCompletedSinnersPrayer.asStateFlow()

    private val _lastAltarVisit = MutableStateFlow(sharedPreferences.getLong("last_altar_visit", 0L))
    val lastAltarVisit: StateFlow<Long> = _lastAltarVisit.asStateFlow()

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

    fun setNeuralBriefEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("neural_brief_enabled", enabled).apply()
        _isNeuralBriefEnabled.value = enabled
    }

    fun setQuietHoursStart(time: String) {
        sharedPreferences.edit().putString("quiet_hours_start", time).apply()
        _quietHoursStart.value = time
    }

    fun setQuietHoursEnd(time: String) {
        sharedPreferences.edit().putString("quiet_hours_end", time).apply()
        _quietHoursEnd.value = time
    }

    fun setBriefFrequency(frequency: String) {
        sharedPreferences.edit().putString("brief_frequency", frequency).apply()
        _briefFrequency.value = frequency
    }

    fun setInsightDepth(depth: String) {
        sharedPreferences.edit().putString("insight_depth", depth).apply()
        _insightDepth.value = depth
    }

    fun setCompletedSinnersPrayer(completed: Boolean) {
        sharedPreferences.edit().putBoolean("has_completed_sinners_prayer", completed).apply()
        _hasCompletedSinnersPrayer.value = completed
    }

    fun setLastAltarVisit(timestamp: Long) {
        sharedPreferences.edit().putLong("last_altar_visit", timestamp).apply()
        _lastAltarVisit.value = timestamp
    }

    fun getBookProgress(bookId: String): Int {
        return sharedPreferences.getInt("book_progress_$bookId", 0)
    }

    fun saveBookProgress(bookId: String, chapterIndex: Int) {
        sharedPreferences.edit().putInt("book_progress_$bookId", chapterIndex).apply()
    }
}
