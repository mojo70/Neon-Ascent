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
        val LAST_BRIEF_SLOT = stringPreferencesKey("last_brief_slot")
        val LAST_BRIEF_FACTS_HASH = stringPreferencesKey("last_brief_facts_hash")
        val LAST_LEAD_SESSION_ID = stringPreferencesKey("last_lead_session_id")
        val LAST_BRIEF_TITLE = stringPreferencesKey("last_brief_title")
        val LAST_BRIEF_BODY = stringPreferencesKey("last_brief_body")
        val LAST_BRIEF_CARD_BODY = stringPreferencesKey("last_brief_card_body")
        val ADAPTIVE_WAKE_ENABLED = booleanPreferencesKey("adaptive_wake_enabled")
        val QUIET_HOURS_END = stringPreferencesKey("quiet_hours_end")

        // Slots / Target Wake / PM Preferences
        val TARGET_WAKE_WD = stringPreferencesKey("target_wake_wd")
        val TARGET_WAKE_WE = stringPreferencesKey("target_wake_we")
        val PULSE_PM_MODE = stringPreferencesKey("pulse_pm_mode") // NEED_ONLY, WEEKDAY_CLOCK, CUSTOM
        val PULSE_PM_CUSTOM_DAYS = stringPreferencesKey("pulse_pm_custom_days")
        val PULSE_PM_CUSTOM_TIME = stringPreferencesKey("pulse_pm_custom_time")
    }

    private val dataStore = context.briefDataStore

    val lastBriefDate: Flow<String?> = dataStore.data.map { it[Keys.LAST_BRIEF_DATE] }
    val lastBriefSlot: Flow<String?> = dataStore.data.map { it[Keys.LAST_BRIEF_SLOT] }
    val lastBriefFactsHash: Flow<String?> = dataStore.data.map { it[Keys.LAST_BRIEF_FACTS_HASH] }
    val lastLeadSessionId: Flow<String?> = dataStore.data.map { it[Keys.LAST_LEAD_SESSION_ID] }
    val lastBriefTitle: Flow<String?> = dataStore.data.map { it[Keys.LAST_BRIEF_TITLE] }
    val lastBriefBody: Flow<String?> = dataStore.data.map { it[Keys.LAST_BRIEF_BODY] }
    val lastBriefCardBody: Flow<String?> = dataStore.data.map { it[Keys.LAST_BRIEF_CARD_BODY] }
    
    val adaptiveWakeEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.ADAPTIVE_WAKE_ENABLED] ?: true }
    val quietHoursEnd: Flow<String> = dataStore.data.map { it[Keys.QUIET_HOURS_END] ?: "07:00" }

    val targetWakeWd: Flow<String?> = dataStore.data.map { it[Keys.TARGET_WAKE_WD] }
    val targetWakeWe: Flow<String?> = dataStore.data.map { it[Keys.TARGET_WAKE_WE] }
    val pulsePmMode: Flow<String> = dataStore.data.map { it[Keys.PULSE_PM_MODE] ?: "NEED_ONLY" }
    val pulsePmCustomDays: Flow<String> = dataStore.data.map { it[Keys.PULSE_PM_CUSTOM_DAYS] ?: "MON,TUE,WED,THU,FRI" }
    val pulsePmCustomTime: Flow<String> = dataStore.data.map { it[Keys.PULSE_PM_CUSTOM_TIME] ?: "20:30" }

    suspend fun updateLastBrief(
        date: String,
        slot: String,
        factsHash: String,
        title: String,
        body: String,
        cardBody: String? = null,
        leadSessionId: String? = null
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_BRIEF_DATE] = date
            prefs[Keys.LAST_BRIEF_SLOT] = slot
            prefs[Keys.LAST_BRIEF_FACTS_HASH] = factsHash
            prefs[Keys.LAST_BRIEF_TITLE] = title
            prefs[Keys.LAST_BRIEF_BODY] = body
            if (cardBody != null) prefs[Keys.LAST_BRIEF_CARD_BODY] = cardBody
            if (leadSessionId != null) prefs[Keys.LAST_LEAD_SESSION_ID] = leadSessionId
        }
    }

    suspend fun setTargetWakeWd(time: String?) {
        dataStore.edit {
            if (time != null) it[Keys.TARGET_WAKE_WD] = time else it.remove(Keys.TARGET_WAKE_WD)
        }
    }

    suspend fun setTargetWakeWe(time: String?) {
        dataStore.edit {
            if (time != null) it[Keys.TARGET_WAKE_WE] = time else it.remove(Keys.TARGET_WAKE_WE)
        }
    }

    suspend fun setPulsePmMode(mode: String) {
        dataStore.edit { it[Keys.PULSE_PM_MODE] = mode }
    }

    suspend fun setPulsePmCustomSchedule(days: String, time: String) {
        dataStore.edit {
            it[Keys.PULSE_PM_CUSTOM_DAYS] = days
            it[Keys.PULSE_PM_CUSTOM_TIME] = time
        }
    }

    suspend fun setAdaptiveWakeEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.ADAPTIVE_WAKE_ENABLED] = enabled }
    }

    suspend fun setQuietHoursEnd(time: String) {
        dataStore.edit { it[Keys.QUIET_HOURS_END] = time }
    }
}
