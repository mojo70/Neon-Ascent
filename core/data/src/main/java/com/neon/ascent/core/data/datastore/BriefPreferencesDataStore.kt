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

private val Context.briefDataStore: DataStore<Preferences> by preferencesDataStore(name = "brief_preferences")

@Singleton
class BriefPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val LAST_BRIEF_DATE = stringPreferencesKey("last_brief_date")
        val LAST_BRIEF_FACTS_HASH = stringPreferencesKey("last_brief_facts_hash")
        val LAST_BRIEF_TITLE = stringPreferencesKey("last_brief_title")
        val LAST_BRIEF_BODY = stringPreferencesKey("last_brief_body")
        val ADAPTIVE_WAKE_ENABLED = booleanPreferencesKey("adaptive_wake_enabled")
        val QUIET_HOURS_END = stringPreferencesKey("quiet_hours_end")
    }

    private val dataStore = context.briefDataStore

    val lastBriefDate: Flow<String?> = dataStore.data.map { it[Keys.LAST_BRIEF_DATE] }
    val lastBriefFactsHash: Flow<String?> = dataStore.data.map { it[Keys.LAST_BRIEF_FACTS_HASH] }
    val lastBriefTitle: Flow<String?> = dataStore.data.map { it[Keys.LAST_BRIEF_TITLE] }
    val lastBriefBody: Flow<String?> = dataStore.data.map { it[Keys.LAST_BRIEF_BODY] }
    
    val adaptiveWakeEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.ADAPTIVE_WAKE_ENABLED] ?: true }
    val quietHoursEnd: Flow<String> = dataStore.data.map { it[Keys.QUIET_HOURS_END] ?: "07:00" }

    suspend fun updateLastBrief(date: String, factsHash: String, title: String, body: String) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_BRIEF_DATE] = date
            prefs[Keys.LAST_BRIEF_FACTS_HASH] = factsHash
            prefs[Keys.LAST_BRIEF_TITLE] = title
            prefs[Keys.LAST_BRIEF_BODY] = body
        }
    }

    suspend fun setAdaptiveWakeEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.ADAPTIVE_WAKE_ENABLED] = enabled }
    }

    suspend fun setQuietHoursEnd(time: String) {
        dataStore.edit { it[Keys.QUIET_HOURS_END] = time }
    }
}
