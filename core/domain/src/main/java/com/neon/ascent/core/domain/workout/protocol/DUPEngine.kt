package com.neon.ascent.core.domain.workout.protocol

import com.neon.ascent.core.domain.workout.models.*

class DUPEngine(
    private val weightIncrement: Float = 2.5f
) : ProtocolEngine {
    override val protocol: WorkoutProtocol = WorkoutProtocol.DUP
    override val uiMode: ProtocolUiMode = ProtocolUiMode.LINEAR // Re-evaluated dynamically in prescribe

    override val recommendedDaysPerWeek: Int = 3
    override val defaultWeekdays: List<Int> = listOf(1, 3, 5)
    override val frequencyCopy: String = "UNDULATION IS 3 DAYS. HYP / STR / PWR."

    override fun dayTypes(): List<ProtocolDayType> = listOf(
        ProtocolDayType.DUP_HYPERTROPHY,
        ProtocolDayType.DUP_STRENGTH,
        ProtocolDayType.DUP_POWER
    )

    override fun prescribe(
        cycle: ProtocolCycle?,
        dayType: ProtocolDayType?,
        exercise: Exercise,
        setIndex: Int,
        afterWeightJump: Boolean,
        max: ExerciseMax?
    ): PrescribedSet {
        val workingWeight = max?.trainingMax ?: 0f
        
        return when (dayType) {
            ProtocolDayType.DUP_HYPERTROPHY -> {
                // Hypertrophy: ~75-80% of Strength max, 8-12 reps
                PrescribedSet(roundToIncrement(workingWeight * 0.8f), 10, SetType.NORMAL)
            }
            ProtocolDayType.DUP_STRENGTH -> {
                // Strength: 100% of workingWeight, 3-5 reps
                PrescribedSet(workingWeight, 5, SetType.NORMAL)
            }
            ProtocolDayType.DUP_POWER -> {
                // Power: ~70% of Strength weight, 2-3 reps, explosive
                PrescribedSet(roundToIncrement(workingWeight * 0.7f), 3, SetType.POWER)
            }
            else -> PrescribedSet(workingWeight, 8, SetType.NORMAL)
        }
    }

    override fun sessionSucceeded(workSets: List<SetLog>): Boolean {
        if (workSets.isEmpty()) return false
        return workSets.all { it.isCompleted && it.reps >= (it.prescribedReps ?: 0) }
    }

    override fun nextLoad(current: Float, succeeded: Boolean, exercise: Exercise): Float {
        // Increment only on Strength day success
        return if (succeeded) current + weightIncrement else current
    }

    override fun stallAction(exercise: Exercise, consecutiveMisses: Int): StallAction {
        return if (consecutiveMisses >= 3) StallAction.Deload(0.9f) else StallAction.None
    }

    private fun roundToIncrement(weight: Float): Float {
        return (Math.round(weight / weightIncrement) * weightIncrement).toFloat()
    }

    fun getDayType(currentDayIndex: Int): ProtocolDayType {
        return when (currentDayIndex % 3) {
            0 -> ProtocolDayType.DUP_HYPERTROPHY
            1 -> ProtocolDayType.DUP_STRENGTH
            else -> ProtocolDayType.DUP_POWER
        }
    }
}
