package com.neon.ascent.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

import com.neon.ascent.core.domain.selfmap.SelfMapIntakeMode

private val Context.selfMapDataStore: DataStore<Preferences> by preferencesDataStore(name = "self_map_preferences")

@Singleton
class SelfMapPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("self_map_onboarding_completed")
        val BASELINE_MASS = floatPreferencesKey("self_map_baseline_mass")
        val BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("self_map_biometric_lock_enabled")
        val CONVERSATIONAL_INTAKE_SEEN = booleanPreferencesKey("self_map_conversational_intake_seen")
        val REVIEW_REMINDER_ENABLED = booleanPreferencesKey("self_map_review_reminder_enabled")
        val REVIEW_REMINDER_CADENCE_DAYS = intPreferencesKey("self_map_review_reminder_cadence_days")
        val SELF_MAP_INTAKE_MODE = stringPreferencesKey("selfMapIntakeMode")
        val INTAKE_STEP_INDEX = intPreferencesKey("self_map_intake_step_index")
    }

    private val dataStore = context.selfMapDataStore

    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETED] ?: false
    }

    val baselineMass: Flow<Float?> = dataStore.data.map { prefs ->
        prefs[Keys.BASELINE_MASS]
    }

    val biometricLockEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.BIOMETRIC_LOCK_ENABLED] ?: false
    }

    val conversationalIntakeSeen: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.CONVERSATIONAL_INTAKE_SEEN] ?: false
    }

    val reviewReminderEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.REVIEW_REMINDER_ENABLED] ?: false
    }

    val reviewReminderCadenceDays: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.REVIEW_REMINDER_CADENCE_DAYS] ?: 14
    }

    val selfMapIntakeMode: Flow<SelfMapIntakeMode> = dataStore.data.map { prefs ->
        val modeStr = prefs[Keys.SELF_MAP_INTAKE_MODE]
        if (modeStr != null) {
            try {
                SelfMapIntakeMode.valueOf(modeStr)
            } catch (_: IllegalArgumentException) {
                SelfMapIntakeMode.CHAT
            }
        } else {
            SelfMapIntakeMode.CHAT
        }
    }

    val intakeStepIndex: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.INTAKE_STEP_INDEX] ?: 0
    }

    suspend fun setSelfMapIntakeMode(mode: SelfMapIntakeMode) {
        dataStore.edit { prefs ->
            prefs[Keys.SELF_MAP_INTAKE_MODE] = mode.name
        }
    }

    suspend fun setIntakeStepIndex(step: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.INTAKE_STEP_INDEX] = step
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setBaselineMass(mass: Float) {
        dataStore.edit { prefs ->
            prefs[Keys.BASELINE_MASS] = mass
        }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setConversationalIntakeSeen(seen: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.CONVERSATIONAL_INTAKE_SEEN] = seen
        }
    }

    suspend fun setReviewReminderEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.REVIEW_REMINDER_ENABLED] = enabled
        }
    }

    suspend fun setReviewReminderCadenceDays(days: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.REVIEW_REMINDER_CADENCE_DAYS] = days
        }
    }

    suspend fun clearAll() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
