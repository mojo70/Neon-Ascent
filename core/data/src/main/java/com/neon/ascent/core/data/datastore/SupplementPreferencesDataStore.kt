package com.neon.ascent.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.neon.ascent.core.domain.labs.models.SupplementOverride
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.supplementDataStore: DataStore<Preferences> by preferencesDataStore(name = "supplement_preferences")

@Singleton
open class SupplementPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ACTIVE_STACK_IDS = stringPreferencesKey("active_stack_ids")
        val OVERRIDES_JSON = stringPreferencesKey("overrides_json")
    }

    private val dataStore by lazy { context.supplementDataStore }
    private val gson = Gson()

    open val activeStackIds: Flow<Set<String>>
        get() = dataStore.data.map { prefs ->
            val json = prefs[Keys.ACTIVE_STACK_IDS]
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

    open val overrides: Flow<Map<String, SupplementOverride>>
        get() = dataStore.data.map { prefs ->
            val json = prefs[Keys.OVERRIDES_JSON]
            if (!json.isNullOrBlank()) {
                try {
                    val type = object : TypeToken<Map<String, SupplementOverride>>() {}.type
                    gson.fromJson<Map<String, SupplementOverride>>(json, type) ?: emptyMap()
                } catch (_: Exception) {
                    emptyMap()
                }
            } else {
                emptyMap()
            }
        }

    open suspend fun setActiveStackIds(ids: Set<String>) {
        dataStore.edit { prefs ->
            if (ids.isEmpty()) {
                prefs.remove(Keys.ACTIVE_STACK_IDS)
            } else {
                prefs[Keys.ACTIVE_STACK_IDS] = gson.toJson(ids)
            }
        }
    }

    open suspend fun updateOverride(id: String, override: SupplementOverride?) {
        dataStore.edit { prefs ->
            val currentJson = prefs[Keys.OVERRIDES_JSON]
            val currentMap: MutableMap<String, SupplementOverride> = if (!currentJson.isNullOrBlank()) {
                try {
                    val type = object : TypeToken<Map<String, SupplementOverride>>() {}.type
                    gson.fromJson<Map<String, SupplementOverride>>(currentJson, type)?.toMutableMap() ?: mutableMapOf()
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

    open suspend fun updateAllOverrides(map: Map<String, SupplementOverride>) {
        dataStore.edit { prefs ->
            if (map.isEmpty()) {
                prefs.remove(Keys.OVERRIDES_JSON)
            } else {
                prefs[Keys.OVERRIDES_JSON] = gson.toJson(map)
            }
        }
    }
}
