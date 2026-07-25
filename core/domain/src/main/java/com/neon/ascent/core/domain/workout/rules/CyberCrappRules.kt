package com.neon.ascent.core.domain.workout.rules

import com.neon.ascent.core.domain.workout.models.MovementType

data class RepRange(val min: Int, val max: Int)

object CyberCrappRules {
    fun getRepRange(movementType: MovementType): RepRange {
        return when (movementType) {
            MovementType.COMPOUND_UPPER,
            MovementType.ISOLATION_UPPER,
            MovementType.BACK_WIDTH -> RepRange(11, 20)
            MovementType.BACK_THICKNESS -> RepRange(10, 15)
            MovementType.DEADLIFT -> RepRange(6, 9)
            MovementType.POSTERIOR_CHAIN -> RepRange(10, 15) // RDLs etc
            MovementType.QUAD_DOMINANT -> RepRange(6, 10)
            MovementType.HAMSTRING_ISOLATION -> RepRange(15, 25)
            MovementType.CALVES -> RepRange(10, 15)
            MovementType.ABS -> RepRange(15, 30)
            else -> RepRange(10, 20)
        }
    }

    fun getRepRangeString(movementType: MovementType): String {
        val range = getRepRange(movementType)
        return when (movementType) {
            MovementType.QUAD_DOMINANT -> "${range.min}-${range.max} + 20"
            else -> "${range.min}-${range.max}"
        }
    }

    const val WARMUP_REP_RANGE = "5-10"
}
