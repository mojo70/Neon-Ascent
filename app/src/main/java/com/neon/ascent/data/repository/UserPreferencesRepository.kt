package com.neon.ascent.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val MEASUREMENT_UNIT = stringPreferencesKey("measurement_unit")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LAST_BIO_AGE = floatPreferencesKey("last_bio_age")
        val LAST_BIO_AGE_TIMESTAMP = longPreferencesKey("last_bio_age_timestamp")
        val YEARLY_REVIEW_ENABLED = booleanPreferencesKey("yearly_review_enabled")
        
        // Neon Guide
        val GUIDE_VERBOSITY = stringPreferencesKey("guide_verbosity")
        val CLOUD_FALLBACK_ENABLED = booleanPreferencesKey("cloud_fallback_enabled")
        val EXPERT_WEIGHTING = stringPreferencesKey("expert_weighting")

        // Interface & Visibility
        val DOPAMINE_MENU_VISIBLE = booleanPreferencesKey("dopamine_menu_visible")
        val SELF_MAP_VISIBLE = booleanPreferencesKey("self_map_visible")
        val NEON_INTENSITY = floatPreferencesKey("neon_intensity")
        
        // Privacy
        val SHARD_VAULT_ENABLED = booleanPreferencesKey("shard_vault_enabled")
        
        // Workout Zoom
        val WORKOUT_ZOOM_LEVEL = floatPreferencesKey("workout_zoom_level")
    }

    val measurementUnit: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.MEASUREMENT_UNIT] ?: "Metric"
        }

    val guideVerbosity: Flow<String> = context.dataStore.data
        .map { it[PreferencesKeys.GUIDE_VERBOSITY] ?: "STANDARD" }

    val cloudFallbackEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.CLOUD_FALLBACK_ENABLED] ?: true }

    val expertWeighting: Flow<String> = context.dataStore.data
        .map { it[PreferencesKeys.EXPERT_WEIGHTING] ?: "BALANCED" }

    val isDopamineMenuVisible: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.DOPAMINE_MENU_VISIBLE] ?: true }

    val isSelfMapVisible: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.SELF_MAP_VISIBLE] ?: true }

    val neonIntensity: Flow<Float> = context.dataStore.data
        .map { it[PreferencesKeys.NEON_INTENSITY] ?: 0.8f }

    val isShardVaultEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.SHARD_VAULT_ENABLED] ?: false }

    val workoutZoomLevel: Flow<Float> = context.dataStore.data
        .map { it[PreferencesKeys.WORKOUT_ZOOM_LEVEL] ?: 1.0f }

    val lastBioAge: Flow<Float?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LAST_BIO_AGE]
        }

    val isYearlyReviewEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.YEARLY_REVIEW_ENABLED] ?: true
        }

    suspend fun updateMeasurementUnit(unit: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MEASUREMENT_UNIT] = unit
        }
    }

    suspend fun setGuideVerbosity(verbosity: String) {
        context.dataStore.edit { it[PreferencesKeys.GUIDE_VERBOSITY] = verbosity }
    }

    suspend fun setCloudFallbackEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.CLOUD_FALLBACK_ENABLED] = enabled }
    }

    suspend fun setExpertWeighting(weighting: String) {
        context.dataStore.edit { it[PreferencesKeys.EXPERT_WEIGHTING] = weighting }
    }

    suspend fun setDopamineMenuVisible(visible: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DOPAMINE_MENU_VISIBLE] = visible }
    }

    suspend fun setSelfMapVisible(visible: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SELF_MAP_VISIBLE] = visible }
    }

    suspend fun setNeonIntensity(intensity: Float) {
        context.dataStore.edit { it[PreferencesKeys.NEON_INTENSITY] = intensity }
    }

    suspend fun setShardVaultEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHARD_VAULT_ENABLED] = enabled }
    }

    suspend fun cacheBioAge(age: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BIO_AGE] = age
            preferences[PreferencesKeys.LAST_BIO_AGE_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun setYearlyReviewEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.YEARLY_REVIEW_ENABLED] = enabled
        }
    }

    suspend fun setWorkoutZoomLevel(level: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WORKOUT_ZOOM_LEVEL] = level
        }
    }
}
