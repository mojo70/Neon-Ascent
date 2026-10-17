package com.neon.ascent.core.domain.workout.rules

import com.neon.ascent.core.domain.workout.models.MovementType
import com.neon.ascent.core.domain.workout.models.ProgressionMode
import com.neon.ascent.core.domain.workout.models.UserWorkoutProfile
import kotlin.math.round

object ProgressionCalculator {

    fun isLowerBodyCompound(movementType: MovementType): Boolean {
        return movementType == MovementType.QUAD_DOMINANT ||
                movementType == MovementType.POSTERIOR_CHAIN ||
                movementType == MovementType.DEADLIFT ||
                movementType.name.contains("DEADLIFT") ||
                movementType.name.contains("QUAD")
    }

    fun isUpperBodyCompound(movementType: MovementType): Boolean {
        return movementType == MovementType.COMPOUND_UPPER ||
                movementType == MovementType.BACK_WIDTH ||
                movementType == MovementType.BACK_THICKNESS ||
                (movementType.name.contains("COMPOUND") && !isLowerBodyCompound(movementType))
    }

    fun calculateNextWeight(
        currentWeight: Float,
        movementType: MovementType,
        profile: UserWorkoutProfile
    ): Float {
        val roundingStep = profile.roundingIncrement.coerceAtLeast(0.5f)

        val targetWeight = when (profile.progressionMode) {
            ProgressionMode.FIXED_INCREMENT -> {
                val increment = when {
                    isLowerBodyCompound(movementType) -> profile.weightIncrementLowerCompound
                    isUpperBodyCompound(movementType) -> profile.weightIncrementUpperCompound
                    else -> profile.weightIncrementIsolation
                }
                currentWeight + increment
            }
            ProgressionMode.PERCENTAGE -> {
                val percent = profile.progressionPercentage.coerceAtLeast(0.1f)
                currentWeight * (1f + (percent / 100f))
            }
        }

        val rounded = round(targetWeight / roundingStep) * roundingStep

        // Ensure the recommended weight advances past currentWeight
        return if (rounded <= currentWeight) {
            round((currentWeight + roundingStep) / roundingStep) * roundingStep
        } else {
            rounded
        }
    }
}
