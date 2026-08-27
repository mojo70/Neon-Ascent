package com.neon.ascent.core.domain.workout.rules

import com.neon.ascent.core.domain.workout.models.*

data class RepRange(val min: Int, val max: Int, val label: String)

object CyberCrappRules {
    fun resolve(
        protocol: WorkoutProtocol,
        exercise: Exercise,
        setType: SetType,
        afterWeightJump: Boolean = false,
        targets: List<ProtocolRepTarget> = emptyList()
    ): RepRange {
        // 1. Exercise override
        if (exercise.rangeOverrideMin != null && exercise.rangeOverrideMax != null) {
            return formatRange(exercise.rangeOverrideMin, exercise.rangeOverrideMax, setType, afterWeightJump)
        }

        // 2. Family-specific row
        targets.find { 
            it.protocol == protocol && 
            it.familyId == exercise.familyId && 
            it.setType == setType 
        }?.let { return formatRange(it.minReps, it.maxReps, setType, afterWeightJump, it.unit) }

        // 3. MovementType + SetType row
        targets.find { 
            it.protocol == protocol && 
            it.movementType == exercise.movementType && 
            it.setType == setType 
        }?.let { return formatRange(it.minReps, it.maxReps, setType, afterWeightJump, it.unit) }

        // 4. Protocol + SetType wildcard (MovementType.UNDEFINED)
        targets.find { 
            it.protocol == protocol && 
            it.movementType == MovementType.UNDEFINED && 
            it.setType == setType 
        }?.let { return formatRange(it.minReps, it.maxReps, setType, afterWeightJump, it.unit) }

        // 5. Hardcoded fallbacks if no table matches
        return when (setType) {
            SetType.WARMUP -> formatRange(5, 10, setType, false)
            SetType.WIDOWMAKER -> formatRange(20, 20, setType, false)
            else -> {
                val base = getRepRange(exercise.movementType)
                formatRange(base.min, base.max, setType, afterWeightJump)
            }
        }
    }

    private fun formatRange(min: Int, max: Int, setType: SetType, afterWeightJump: Boolean, unit: String = "REPS"): RepRange {
        val isWorkingSet = setType == SetType.REST_PAUSE || setType == SetType.NORMAL || setType == SetType.FAILURE
        val label = when {
            unit == "SECONDS" -> "${min}-${max}s"
            afterWeightJump && isWorkingSet -> "$min"
            min == max -> "$min"
            else -> "$min-$max"
        }
        return RepRange(min, max, label)
    }

    // Facade for backward compatibility
    fun getRepRange(movementType: MovementType): RepRange {
        val range = when (movementType) {
            MovementType.COMPOUND_UPPER,
            MovementType.ISOLATION_UPPER,
            MovementType.BACK_WIDTH -> 11 to 20
            MovementType.BACK_THICKNESS -> 10 to 15
            MovementType.DEADLIFT -> 6 to 9
            MovementType.POSTERIOR_CHAIN -> 10 to 15
            MovementType.QUAD_DOMINANT -> 6 to 10
            MovementType.HAMSTRING_ISOLATION -> 15 to 25
            MovementType.CALVES -> 10 to 15
            MovementType.ABS -> 15 to 30
            else -> 10 to 20
        }
        return RepRange(range.first, range.second, "${range.first}-${range.second}")
    }

    @Deprecated("Use resolve instead")
    fun getRepRangeString(movementType: MovementType, familyId: String? = null): String {
        val range = getRepRange(movementType)
        return "${range.min}-${range.max}"
    }

    const val WARMUP_REP_RANGE = "5-10"
}
