package com.neon.ascent.core.domain.workout.rules

import com.neon.ascent.core.domain.workout.models.Exercise
import com.neon.ascent.core.domain.workout.models.MovementType

object ExerciseRanking {

    /**
     * Ranks alternative exercises for substitution or rotation relative to a target exercise.
     *
     * Ranking Order (Product Lock):
     * 1. Same familyId (different implement/stance)
     * 2. Same CyberCrapp slot / movementType (other families in the same rotation pool)
     * 3. Primary variants first, then alphabetical tie-breaker.
     */
    fun rankAlternatives(
        target: Exercise,
        candidates: List<Exercise>
    ): List<Exercise> {
        val candidatesWithoutTarget = candidates.filter { it.id != target.id }

        return candidatesWithoutTarget.sortedWith(
            compareByDescending<Exercise> { it.familyId == target.familyId }
                .thenByDescending { it.movementType == target.movementType && it.movementType != MovementType.UNDEFINED }
                .thenByDescending { it.isPrimaryVariant }
                .thenBy { it.name }
        )
    }

    /**
     * Overload for ranking alternatives by target familyId and movementType.
     */
    fun rankAlternatives(
        targetFamilyId: String,
        targetMovementType: MovementType,
        targetExerciseId: String?,
        candidates: List<Exercise>
    ): List<Exercise> {
        val candidatesWithoutTarget = candidates.filter { it.id != targetExerciseId }

        return candidatesWithoutTarget.sortedWith(
            compareByDescending<Exercise> { it.familyId == targetFamilyId }
                .thenByDescending { it.movementType == targetMovementType && it.movementType != MovementType.UNDEFINED }
                .thenByDescending { it.isPrimaryVariant }
                .thenBy { it.name }
        )
    }
}
