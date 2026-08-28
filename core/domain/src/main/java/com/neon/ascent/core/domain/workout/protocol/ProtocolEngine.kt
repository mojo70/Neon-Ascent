package com.neon.ascent.core.domain.workout.protocol

import com.neon.ascent.core.domain.workout.models.*

interface ProtocolEngine {
    val protocol: WorkoutProtocol
    val uiMode: ProtocolUiMode

    val recommendedDaysPerWeek: Int
    val defaultWeekdays: List<Int>
    val frequencyCopy: String

    fun dayTypes(): List<ProtocolDayType>

    /**
     * Prescribes a set for a given exercise based on the current cycle state and history.
     */
    fun prescribe(
        cycle: ProtocolCycle?,
        dayType: ProtocolDayType?,
        exercise: Exercise,
        setIndex: Int,
        afterWeightJump: Boolean,
        max: ExerciseMax?
    ): PrescribedSet

    /**
     * Determines if the session was successful based on the completed work sets.
     */
    fun sessionSucceeded(workSets: List<SetLog>): Boolean

    /**
     * Calculates the next load for an exercise.
     */
    fun nextLoad(current: Float, succeeded: Boolean, exercise: Exercise): Float

    /**
     * Action to take when progress stalls (e.g., reset, deload, change variant).
     */
    fun stallAction(exercise: Exercise, consecutiveMisses: Int): StallAction
}

sealed class StallAction {
    object Reset : StallAction()
    data class Deload(val percent: Float) : StallAction()
    object RotateVariant : StallAction()
    object None : StallAction()
}
