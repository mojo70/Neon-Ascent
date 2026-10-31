package com.neon.ascent.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.neon.ascent.core.domain.labs.models.LabsFocus
import com.neon.ascent.core.domain.labs.models.OutcomeDomain
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.labsFocusDataStore: DataStore<Preferences> by preferencesDataStore(name = "labs_focus_preferences")

@Singleton
open class LabsFocusPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val PRIMARY_DOMAIN = stringPreferencesKey("primary_domain")
        val SUPPORTING_DOMAINS_JSON = stringPreferencesKey("supporting_domains_json")
    }

    private val dataStore by lazy { context.labsFocusDataStore }
    private val gson = Gson()

    open val labsFocus: Flow<LabsFocus>
        get() = dataStore.data.map { prefs ->
            val primaryStr = prefs[Keys.PRIMARY_DOMAIN]
            val primary = primaryStr?.let {
                try {
                    OutcomeDomain.valueOf(it)
                } catch (_: Exception) {
                    null
                }
            }

            val supportingJson = prefs[Keys.SUPPORTING_DOMAINS_JSON]
            val supporting = if (!supportingJson.isNullOrBlank()) {
                try {
                    val list = gson.fromJson(supportingJson, Array<String>::class.java)
                    list.mapNotNull { name ->
                        try {
                            OutcomeDomain.valueOf(name)
                        } catch (_: Exception) {
                            null
                        }
                    }.toSet()
                } catch (_: Exception) {
                    emptySet()
                }
            } else {
                emptySet()
            }

            LabsFocus.createSanitized(primary = primary, supporting = supporting)
        }

    open suspend fun setLabsFocus(focus: LabsFocus) {
        val clean = focus.sanitized()
        dataStore.edit { prefs ->
            val primary = clean.primary
            if (primary != null) {
                prefs[Keys.PRIMARY_DOMAIN] = primary.name
            } else {
                prefs.remove(Keys.PRIMARY_DOMAIN)
            }

            if (clean.supporting.isNotEmpty()) {
                val array = clean.supporting.map { it.name }.toTypedArray()
                prefs[Keys.SUPPORTING_DOMAINS_JSON] = gson.toJson(array)
            } else {
                prefs.remove(Keys.SUPPORTING_DOMAINS_JSON)
            }
        }
    }
}
