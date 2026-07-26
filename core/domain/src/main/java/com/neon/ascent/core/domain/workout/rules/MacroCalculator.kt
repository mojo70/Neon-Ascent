package com.neon.ascent.core.domain.workout.rules

import com.neon.ascent.core.domain.workout.models.Gender
import com.neon.ascent.core.domain.workout.models.Somatotype
import com.neon.ascent.core.domain.workout.models.UserWorkoutProfile

data class Macros(
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int
)

object MacroCalculator {

    fun calculateMacros(profile: UserWorkoutProfile): Macros {
        // Mifflin-St Jeor Equation
        val bmr = (10 * profile.weightKg) + (6.25 * profile.heightCm) - (5 * profile.age) + 
            if (profile.gender == Gender.MALE) 5 else -161
        
        val tdee = (bmr * profile.activityFactor).toInt()
        
        // Somatotype-adjusted protein targets
        val proteinPerKg = when (profile.somatotype) {
            Somatotype.ENDOMORPH -> 2.2f
            else -> 2.0f
        }
        
        val proteinGrams = (profile.weightKg * proteinPerKg).toInt()
        
        // Somatotype-adjusted fat percentages
        val fatPercentage = when (profile.somatotype) {
            Somatotype.ECTOMORPH -> 0.20f
            Somatotype.ENDOMORPH -> 0.30f
            else -> 0.25f
        }
        
        val fatGrams = ((tdee * fatPercentage) / 9).toInt()
        val carbGrams = ((tdee - (proteinGrams * 4) - (fatGrams * 9)) / 4).toInt()
        
        return Macros(
            calories = tdee,
            protein = proteinGrams,
            carbs = carbGrams,
            fat = fatGrams
        )
    }
}
