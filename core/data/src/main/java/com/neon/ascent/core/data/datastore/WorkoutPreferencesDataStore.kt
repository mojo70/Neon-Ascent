package com.neon.ascent.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.neon.ascent.core.domain.workout.models.WorkoutLayoutMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.workoutDataStore: DataStore<Preferences> by preferencesDataStore(name = "workout_preferences")

@Singleton
class WorkoutPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val WORKOUT_LAYOUT_MODE = stringPreferencesKey("workoutLayoutMode")
    }

    private val dataStore = context.workoutDataStore

    val workoutLayoutMode: Flow<WorkoutLayoutMode> = dataStore.data.map { prefs ->
        val modeStr = prefs[Keys.WORKOUT_LAYOUT_MODE]
        if (modeStr != null) {
            try {
                WorkoutLayoutMode.valueOf(modeStr)
            } catch (e: Exception) {
                WorkoutLayoutMode.CLASSIC
            }
        } else {
            WorkoutLayoutMode.CLASSIC
        }
    }

    suspend fun setWorkoutLayoutMode(mode: WorkoutLayoutMode) {
        dataStore.edit { prefs ->
            prefs[Keys.WORKOUT_LAYOUT_MODE] = mode.name
        }
    }
}
