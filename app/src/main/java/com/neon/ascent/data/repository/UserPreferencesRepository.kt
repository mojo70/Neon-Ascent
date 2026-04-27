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

    suspend fun updateMeasurementUnit(unit: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MEASUREMENT_UNIT] = unit
        }
    }
}
