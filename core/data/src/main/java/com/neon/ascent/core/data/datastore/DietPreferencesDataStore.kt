package com.neon.ascent.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.neon.ascent.core.domain.labs.models.DietOverrides
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dietDataStore: DataStore<Preferences> by preferencesDataStore(name = "diet_preferences")

@Singleton
open class DietPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ACTIVE_DIET_ID = stringPreferencesKey("active_diet_id")
        val PREFERRED_PROTEIN_G_KG = doublePreferencesKey("preferred_protein_g_kg")
        val PREFERRED_MODEST_CARB_TARGET = intPreferencesKey("preferred_modest_carb_target")
        val PREFERRED_HIGHER_CARB_MEAL_DAYS = stringPreferencesKey("preferred_higher_carb_meal_days")
        val PREFERRED_HIGHER_CARB_MEALS_PER_WEEK = intPreferencesKey("preferred_higher_carb_meals_per_week")
        val FREE_LIST_EXCEPTIONS = stringPreferencesKey("free_list_exceptions")
        val OVERRIDE_NOTES = stringPreferencesKey("override_notes")
    }

    private val dataStore by lazy { context.dietDataStore }
    private val gson = Gson()

    open val activeDietId: Flow<String?>
        get() = dataStore.data.map { prefs ->
            prefs[Keys.ACTIVE_DIET_ID]
        }

    open val dietOverrides: Flow<DietOverrides>
        get() = dataStore.data.map { prefs ->
            val daysJson = prefs[Keys.PREFERRED_HIGHER_CARB_MEAL_DAYS]
            val higherCarbDays: List<String> = if (!daysJson.isNullOrBlank()) {
                try {
                    gson.fromJson(daysJson, object : TypeToken<List<String>>() {}.type)
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            val freeExceptionsJson = prefs[Keys.FREE_LIST_EXCEPTIONS]
            val freeExceptions: List<String> = if (!freeExceptionsJson.isNullOrBlank()) {
                try {
                    gson.fromJson(freeExceptionsJson, object : TypeToken<List<String>>() {}.type)
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            DietOverrides(
                preferredProteinGramsPerKg = prefs[Keys.PREFERRED_PROTEIN_G_KG],
                preferredModestCarbTargetGrams = prefs[Keys.PREFERRED_MODEST_CARB_TARGET],
                preferredHigherCarbMealDays = higherCarbDays,
                preferredHigherCarbMealsPerWeek = prefs[Keys.PREFERRED_HIGHER_CARB_MEALS_PER_WEEK],
                freeListExceptions = freeExceptions,
                notes = prefs[Keys.OVERRIDE_NOTES]
            )
        }

    open suspend fun setActiveDietId(id: String?) {
        dataStore.edit { prefs ->
            if (id == null) {
                prefs.remove(Keys.ACTIVE_DIET_ID)
            } else {
                prefs[Keys.ACTIVE_DIET_ID] = id
            }
        }
    }

    open suspend fun updateDietOverrides(overrides: DietOverrides) {
        dataStore.edit { prefs ->
            val protein = overrides.preferredProteinGramsPerKg
            if (protein != null) {
                prefs[Keys.PREFERRED_PROTEIN_G_KG] = protein
            } else {
                prefs.remove(Keys.PREFERRED_PROTEIN_G_KG)
            }

            val carbTarget = overrides.preferredModestCarbTargetGrams
            if (carbTarget != null) {
                prefs[Keys.PREFERRED_MODEST_CARB_TARGET] = carbTarget
            } else {
                prefs.remove(Keys.PREFERRED_MODEST_CARB_TARGET)
            }

            if (overrides.preferredHigherCarbMealDays.isNotEmpty()) {
                prefs[Keys.PREFERRED_HIGHER_CARB_MEAL_DAYS] = gson.toJson(overrides.preferredHigherCarbMealDays)
            } else {
                prefs.remove(Keys.PREFERRED_HIGHER_CARB_MEAL_DAYS)
            }

            val mealsPerWeek = overrides.preferredHigherCarbMealsPerWeek
            if (mealsPerWeek != null) {
                prefs[Keys.PREFERRED_HIGHER_CARB_MEALS_PER_WEEK] = mealsPerWeek
            } else {
                prefs.remove(Keys.PREFERRED_HIGHER_CARB_MEALS_PER_WEEK)
            }

            if (overrides.freeListExceptions.isNotEmpty()) {
                prefs[Keys.FREE_LIST_EXCEPTIONS] = gson.toJson(overrides.freeListExceptions)
            } else {
                prefs.remove(Keys.FREE_LIST_EXCEPTIONS)
            }

            val notes = overrides.notes
            if (!notes.isNullOrBlank()) {
                prefs[Keys.OVERRIDE_NOTES] = notes
            } else {
                prefs.remove(Keys.OVERRIDE_NOTES)
            }
        }
    }
}
