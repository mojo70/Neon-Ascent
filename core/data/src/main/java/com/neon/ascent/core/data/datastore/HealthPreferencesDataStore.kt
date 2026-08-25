package com.neon.ascent.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.neon.ascent.core.domain.model.SpecialType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val Context.healthDataStore: DataStore<Preferences> by preferencesDataStore(name = "health_preferences")

@Singleton
class HealthPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val SYNC_INTERVAL_HOURS = intPreferencesKey("sync_interval_hours")
        val ENABLED_ATTRIBUTES = stringSetPreferencesKey("enabled_attributes") // e.g. ["AGILITY", "ENDURANCE"]
        val NOTIFICATION_ON_SYNC = booleanPreferencesKey("notification_on_sync")
        val LAST_SUCCESSFUL_SYNC = longPreferencesKey("last_successful_sync")
        val LIVE_MONITORING_ENABLED = booleanPreferencesKey("live_monitoring_enabled")
        val WORKOUT_ZOOM_LEVEL = floatPreferencesKey("workout_zoom_level")
        val DEFAULT_REST_TIME = intPreferencesKey("default_rest_time")
        val AUTO_START_REST_TIMER = booleanPreferencesKey("auto_start_rest_timer")
        val WORK_SET_REST_TIME = intPreferencesKey("work_set_rest_time")
        val WARMUP_SET_REST_TIME = intPreferencesKey("warmup_set_rest_time")
        val DROP_SET_REST_TIME = intPreferencesKey("drop_set_rest_time")
        val REST_TIMER_MODE = stringPreferencesKey("rest_timer_mode")
        val CODEX_PERIOD = stringPreferencesKey("codex_period")
        val CODEX_WING = stringPreferencesKey("codex_wing")
        val CODEX_LAST_EXERCISE_ID = stringPreferencesKey("codex_last_exercise_id")
    }

    private val dataStore = context.healthDataStore

    // === Last Sync Tracking ===
    val lastSyncTime: Flow<Instant?> = dataStore.data.map { prefs ->
        prefs[Keys.LAST_SYNC_TIMESTAMP]?.let { Instant.ofEpochMilli(it) }
    }

    val lastSuccessfulSync: Flow<Instant?> = dataStore.data.map { prefs ->
        prefs[Keys.LAST_SUCCESSFUL_SYNC]?.let { Instant.ofEpochMilli(it) }
    }

    suspend fun updateLastSyncTime(timestamp: Instant = Instant.now()) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_SYNC_TIMESTAMP] = timestamp.toEpochMilli()
            prefs[Keys.LAST_SUCCESSFUL_SYNC] = timestamp.toEpochMilli()
        }
    }

    // === User Preferences ===
    val autoSyncEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.AUTO_SYNC_ENABLED] ?: true
    }

    val syncIntervalHours: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.SYNC_INTERVAL_HOURS] ?: 8
    }

    val enabledAttributes: Flow<Set<SpecialType>> = dataStore.data.map { prefs ->
        prefs[Keys.ENABLED_ATTRIBUTES]?.mapNotNull { name ->
            runCatching { SpecialType.valueOf(name) }.getOrNull()
        }?.toSet() ?: SpecialType.entries.toSet()
    }

    val showSyncNotification: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATION_ON_SYNC] ?: false
    }

    val liveMonitoringEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.LIVE_MONITORING_ENABLED] ?: false
    }

    // === Update Functions ===
    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_SYNC_ENABLED] = enabled }
    }

    suspend fun setSyncIntervalHours(hours: Int) {
        dataStore.edit { it[Keys.SYNC_INTERVAL_HOURS] = hours.coerceIn(4, 24) }
    }

    suspend fun setEnabledAttributes(attributes: Set<SpecialType>) {
        dataStore.edit { prefs ->
            prefs[Keys.ENABLED_ATTRIBUTES] = attributes.map { it.name }.toSet()
        }
    }

    suspend fun setShowSyncNotification(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATION_ON_SYNC] = enabled }
    }

    suspend fun setLiveMonitoringEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.LIVE_MONITORING_ENABLED] = enabled }
    }

    val workoutZoomLevel: Flow<Float> = dataStore.data.map { prefs ->
        prefs[Keys.WORKOUT_ZOOM_LEVEL] ?: 1.0f
    }

    suspend fun setWorkoutZoomLevel(level: Float) {
        dataStore.edit { it[Keys.WORKOUT_ZOOM_LEVEL] = level }
    }

    val defaultRestTime: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_REST_TIME] ?: 60
    }

    suspend fun setDefaultRestTime(seconds: Int) {
        dataStore.edit { it[Keys.DEFAULT_REST_TIME] = seconds.coerceIn(15, 600) }
    }

    val autoStartRestTimer: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.AUTO_START_REST_TIMER] ?: true
    }

    suspend fun setAutoStartRestTimer(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_START_REST_TIMER] = enabled }
    }

    val workSetRestTime: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.WORK_SET_REST_TIME] ?: 120
    }

    suspend fun setWorkSetRestTime(seconds: Int) {
        dataStore.edit { it[Keys.WORK_SET_REST_TIME] = seconds }
    }

    val warmupSetRestTime: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.WARMUP_SET_REST_TIME] ?: 60
    }

    suspend fun setWarmupSetRestTime(seconds: Int) {
        dataStore.edit { it[Keys.WARMUP_SET_REST_TIME] = seconds }
    }

    val dropSetRestTime: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.DROP_SET_REST_TIME] ?: 30
    }

    suspend fun setDropSetRestTime(seconds: Int) {
        dataStore.edit { it[Keys.DROP_SET_REST_TIME] = seconds }
    }

    val restTimerMode: Flow<com.neon.ascent.core.domain.workout.models.RestTimerMode> = dataStore.data.map { prefs ->
        val name = prefs[Keys.REST_TIMER_MODE] ?: com.neon.ascent.core.domain.workout.models.RestTimerMode.BOTH.name
        runCatching { com.neon.ascent.core.domain.workout.models.RestTimerMode.valueOf(name) }.getOrDefault(com.neon.ascent.core.domain.workout.models.RestTimerMode.BOTH)
    }

    suspend fun setRestTimerMode(mode: com.neon.ascent.core.domain.workout.models.RestTimerMode) {
        dataStore.edit { it[Keys.REST_TIMER_MODE] = mode.name }
    }

    val codexPeriod: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.CODEX_PERIOD] ?: "THIRTY_DAYS"
    }

    suspend fun setCodexPeriod(period: String) {
        dataStore.edit { it[Keys.CODEX_PERIOD] = period }
    }

    val codexWing: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.CODEX_WING] ?: "OPS_LOG"
    }

    suspend fun setCodexWing(wing: String) {
        dataStore.edit { it[Keys.CODEX_WING] = wing }
    }

    val codexLastExerciseId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.CODEX_LAST_EXERCISE_ID]
    }

    suspend fun setCodexLastExerciseId(exerciseId: String?) {
        dataStore.edit { 
            if (exerciseId == null) {
                it.remove(Keys.CODEX_LAST_EXERCISE_ID)
            } else {
                it[Keys.CODEX_LAST_EXERCISE_ID] = exerciseId 
            }
        }
    }

    /** Reset all health preferences (useful for debugging or user "Reset Data" option) */
    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
