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
    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val sharedPreferences: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                "secure_settings",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "EncryptedSharedPreferences failed", e)
            context.getSharedPreferences("secure_settings_fallback", Context.MODE_PRIVATE)
        }
    }

    private val _isBiometricLockEnabled = MutableStateFlow(false)
    val isBiometricLockEnabled: StateFlow<Boolean> = _isBiometricLockEnabled.asStateFlow()

    private val _isReligionShortcutEnabled = MutableStateFlow(false)
    val isReligionShortcutEnabled: StateFlow<Boolean> = _isReligionShortcutEnabled.asStateFlow()

    private val _isLocalAiOnly = MutableStateFlow(false)
    val isLocalAiOnly: StateFlow<Boolean> = _isLocalAiOnly.asStateFlow()

    // AI Parameters
    private val _nanoTemperature = MutableStateFlow(0.7f)
    val nanoTemperature = _nanoTemperature.asStateFlow()

    private val _cloudFallbackThreshold = MutableStateFlow(20f)
    val cloudFallbackThreshold = _cloudFallbackThreshold.asStateFlow()

    private val _philosophySeed = MutableStateFlow("PLATO")
    val philosophySeed = _philosophySeed.asStateFlow()

    private val _isNetrunnerMode = MutableStateFlow(false)
    val isNetrunnerMode: StateFlow<Boolean> = _isNetrunnerMode.asStateFlow()

    private val _isFirstAiCoreEntry = MutableStateFlow(true)
    val isFirstAiCoreEntry: StateFlow<Boolean> = _isFirstAiCoreEntry.asStateFlow()

    // Notification Settings
    private val _isNeuralBriefEnabled = MutableStateFlow(true)
    val isNeuralBriefEnabled = _isNeuralBriefEnabled.asStateFlow()

    private val _quietHoursStart = MutableStateFlow("22:00")
    val quietHoursStart = _quietHoursStart.asStateFlow()

    private val _quietHoursEnd = MutableStateFlow("07:00")
    val quietHoursEnd = _quietHoursEnd.asStateFlow()

    private val _briefFrequency = MutableStateFlow("DAILY")
    val briefFrequency = _briefFrequency.asStateFlow()

    private val _insightDepth = MutableStateFlow("DETAILED")
    val insightDepth = _insightDepth.asStateFlow()

    private val _hasCompletedSinnersPrayer = MutableStateFlow(false)
    val hasCompletedSinnersPrayer: StateFlow<Boolean> = _hasCompletedSinnersPrayer.asStateFlow()

    private val _lastAltarVisit = MutableStateFlow(0L)
    val lastAltarVisit: StateFlow<Long> = _lastAltarVisit.asStateFlow()

    init {
        // Initialize values from sharedPreferences with safety
        try {
            _isBiometricLockEnabled.value = sharedPreferences.getBoolean("biometric_lock", false)
            _isReligionShortcutEnabled.value = sharedPreferences.getBoolean("religion_shortcut", false)
            _isLocalAiOnly.value = sharedPreferences.getBoolean("local_ai_only", false)
            _nanoTemperature.value = sharedPreferences.getFloat("nano_temp", 0.7f)
            _cloudFallbackThreshold.value = sharedPreferences.getFloat("cloud_fallback", 20f)
            _philosophySeed.value = sharedPreferences.getString("philosophy_seed", "PLATO") ?: "PLATO"
            _isNetrunnerMode.value = sharedPreferences.getBoolean("netrunner_mode", false)
            _isFirstAiCoreEntry.value = sharedPreferences.getBoolean("first_ai_core_entry", true)
            _isNeuralBriefEnabled.value = sharedPreferences.getBoolean("neural_brief_enabled", true)
            _quietHoursStart.value = sharedPreferences.getString("quiet_hours_start", "22:00") ?: "22:00"
            _quietHoursEnd.value = sharedPreferences.getString("quiet_hours_end", "07:00") ?: "07:00"
            _briefFrequency.value = sharedPreferences.getString("brief_frequency", "DAILY") ?: "DAILY"
            _insightDepth.value = sharedPreferences.getString("insight_depth", "DETAILED") ?: "DETAILED"
            _hasCompletedSinnersPrayer.value = sharedPreferences.getBoolean("has_completed_sinners_prayer", false)
            _lastAltarVisit.value = sharedPreferences.getLong("last_altar_visit", 0L)
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Failed to load initial settings", e)
        }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        try {
            sharedPreferences.edit().putBoolean("biometric_lock", enabled).apply()
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Failed to save biometric_lock", e)
        }
        _isBiometricLockEnabled.value = enabled
    }

    fun setReligionShortcutEnabled(enabled: Boolean) {
        try {
            sharedPreferences.edit().putBoolean("religion_shortcut", enabled).apply()
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error saving setting", e)
        }
        _isReligionShortcutEnabled.value = enabled
    }

    fun setLocalAiOnly(enabled: Boolean) {
        try {
            sharedPreferences.edit().putBoolean("local_ai_only", enabled).apply()
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error saving setting", e)
        }
        _isLocalAiOnly.value = enabled
    }

    fun setNanoTemperature(temp: Float) {
        try {
            sharedPreferences.edit().putFloat("nano_temp", temp).apply()
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error saving setting", e)
        }
        _nanoTemperature.value = temp
    }

    fun setCloudFallbackThreshold(threshold: Float) {
        try {
            sharedPreferences.edit().putFloat("cloud_fallback", threshold).apply()
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error saving setting", e)
        }
        _cloudFallbackThreshold.value = threshold
    }

    fun setPhilosophySeed(seed: String) {
        try {
            sharedPreferences.edit().putString("philosophy_seed", seed).apply()
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error saving setting", e)
        }
        _philosophySeed.value = seed
    }

    fun setNetrunnerMode(enabled: Boolean) {
        try {
            sharedPreferences.edit().putBoolean("netrunner_mode", enabled).apply()
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error saving setting", e)
        }
        _isNetrunnerMode.value = enabled
    }

    fun setFirstAiCoreEntry(isFirst: Boolean) {
        try {
            sharedPreferences.edit().putBoolean("first_ai_core_entry", isFirst).apply()
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error saving setting", e)
        }
        _isFirstAiCoreEntry.value = isFirst
    }

    fun setNeuralBriefEnabled(enabled: Boolean) {
        try {
            sharedPreferences.edit().putBoolean("neural_brief_enabled", enabled).apply()
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error saving setting", e)
        }
        _isNeuralBriefEnabled.value = enabled
    }

    fun setQuietHoursStart(time: String) {
        try {
            sharedPreferences.edit().putString("quiet_hours_start", time).apply()
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error saving setting", e)
        }
        _quietHoursStart.value = time
    }

    fun setQuietHoursEnd(time: String) {
        try {
            sharedPreferences.edit().putString("quiet_hours_end", time).apply()
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error saving setting", e)
        }
        _quietHoursEnd.value = time
    }

    fun setBriefFrequency(frequency: String) {
        try {
            sharedPreferences.edit().putString("brief_frequency", frequency).apply()
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error saving setting", e)
        }
        _briefFrequency.value = frequency
    }

    fun setInsightDepth(depth: String) {
        try {
            sharedPreferences.edit().putString("insight_depth", depth).apply()
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error saving setting", e)
        }
        _insightDepth.value = depth
    }

    fun setCompletedSinnersPrayer(completed: Boolean) {
        try {
            sharedPreferences.edit().putBoolean("has_completed_sinners_prayer", completed).apply()
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error saving setting", e)
        }
        _hasCompletedSinnersPrayer.value = completed
    }

    fun setLastAltarVisit(timestamp: Long) {
        try {
            sharedPreferences.edit().putLong("last_altar_visit", timestamp).apply()
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error saving setting", e)
        }
        _lastAltarVisit.value = timestamp
    }

    fun getBookProgress(bookId: String): Int {
        return sharedPreferences.getInt("book_progress_$bookId", 0)
    }

    fun saveBookProgress(bookId: String, chapterIndex: Int) {
        sharedPreferences.edit().putInt("book_progress_$bookId", chapterIndex).apply()
    }
}
