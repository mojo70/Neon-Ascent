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

    /** Reset all health preferences (useful for debugging or user "Reset Data" option) */
    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
