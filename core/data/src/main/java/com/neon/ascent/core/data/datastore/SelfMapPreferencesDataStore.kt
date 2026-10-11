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

private val Context.selfMapDataStore: DataStore<Preferences> by preferencesDataStore(name = "self_map_preferences")

@Singleton
class SelfMapPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("self_map_onboarding_completed")
        val BASELINE_MASS = floatPreferencesKey("self_map_baseline_mass")
        val BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("self_map_biometric_lock_enabled")
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

    suspend fun clearAll() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
