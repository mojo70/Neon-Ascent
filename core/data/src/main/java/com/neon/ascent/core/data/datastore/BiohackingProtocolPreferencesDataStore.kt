package com.neon.ascent.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.neon.ascent.core.domain.labs.models.BiohackingProtocolOverride
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.biohackingProtocolDataStore: DataStore<Preferences> by preferencesDataStore(name = "biohacking_protocol_preferences")

@Singleton
open class BiohackingProtocolPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ACTIVE_PROTOCOL_IDS = stringPreferencesKey("active_protocol_ids")
        val OVERRIDES_JSON = stringPreferencesKey("overrides_json")
    }

    private val dataStore by lazy { context.biohackingProtocolDataStore }
    private val gson = Gson()

    open val activeProtocolIds: Flow<Set<String>>
        get() = dataStore.data.map { prefs ->
            val json = prefs[Keys.ACTIVE_PROTOCOL_IDS]
            if (!json.isNullOrBlank()) {
                try {
                    val type = object : TypeToken<Set<String>>() {}.type
                    gson.fromJson<Set<String>>(json, type) ?: emptySet()
                } catch (_: Exception) {
                    emptySet()
                }
            } else {
                emptySet()
            }
        }

    open val overrides: Flow<Map<String, BiohackingProtocolOverride>>
        get() = dataStore.data.map { prefs ->
            val json = prefs[Keys.OVERRIDES_JSON]
            if (!json.isNullOrBlank()) {
                try {
                    val type = object : TypeToken<Map<String, BiohackingProtocolOverride>>() {}.type
                    gson.fromJson<Map<String, BiohackingProtocolOverride>>(json, type) ?: emptyMap()
                } catch (_: Exception) {
                    emptyMap()
                }
            } else {
                emptyMap()
            }
        }

    open suspend fun setActiveProtocolIds(ids: Set<String>) {
        dataStore.edit { prefs ->
            if (ids.isEmpty()) {
                prefs.remove(Keys.ACTIVE_PROTOCOL_IDS)
            } else {
                prefs[Keys.ACTIVE_PROTOCOL_IDS] = gson.toJson(ids)
            }
        }
    }

    open suspend fun updateOverride(id: String, override: BiohackingProtocolOverride?) {
        dataStore.edit { prefs ->
            val currentJson = prefs[Keys.OVERRIDES_JSON]
            val currentMap: MutableMap<String, BiohackingProtocolOverride> = if (!currentJson.isNullOrBlank()) {
                try {
                    val type = object : TypeToken<Map<String, BiohackingProtocolOverride>>() {}.type
                    gson.fromJson<Map<String, BiohackingProtocolOverride>>(currentJson, type)?.toMutableMap() ?: mutableMapOf()
                } catch (_: Exception) {
                    mutableMapOf()
                }
            } else {
                mutableMapOf()
            }

            if (override == null) {
                currentMap.remove(id)
            } else {
                currentMap[id] = override
            }

            if (currentMap.isEmpty()) {
                prefs.remove(Keys.OVERRIDES_JSON)
            } else {
                prefs[Keys.OVERRIDES_JSON] = gson.toJson(currentMap)
            }
        }
    }

    open suspend fun updateAllOverrides(map: Map<String, BiohackingProtocolOverride>) {
        dataStore.edit { prefs ->
            if (map.isEmpty()) {
                prefs.remove(Keys.OVERRIDES_JSON)
            } else {
                prefs[Keys.OVERRIDES_JSON] = gson.toJson(map)
            }
        }
    }
}
