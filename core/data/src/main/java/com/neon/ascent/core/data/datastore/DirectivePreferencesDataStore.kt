package com.neon.ascent.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.neon.ascent.core.domain.goals.models.DirectiveIntakeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.directiveDataStore: DataStore<Preferences> by preferencesDataStore(name = "directive_preferences")

@Singleton
class DirectivePreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DIRECTIVE_INTAKE_MODE = stringPreferencesKey("directiveIntakeMode")
    }

    private val dataStore = context.directiveDataStore

    val directiveIntakeMode: Flow<DirectiveIntakeMode> = dataStore.data.map { prefs ->
        val modeStr = prefs[Keys.DIRECTIVE_INTAKE_MODE]
        if (modeStr != null) {
            try {
                DirectiveIntakeMode.valueOf(modeStr)
            } catch (e: Exception) {
                DirectiveIntakeMode.MENTOR
            }
        } else {
            DirectiveIntakeMode.MENTOR
        }
    }

    suspend fun setDirectiveIntakeMode(mode: DirectiveIntakeMode) {
        dataStore.edit { prefs ->
            prefs[Keys.DIRECTIVE_INTAKE_MODE] = mode.name
        }
    }
}
