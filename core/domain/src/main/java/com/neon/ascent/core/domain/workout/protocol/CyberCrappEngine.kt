package com.neon.ascent.core.domain.workout.protocol

import com.neon.ascent.core.domain.workout.models.*

class CyberCrappEngine : ProtocolEngine {
    override val protocol: WorkoutProtocol = WorkoutProtocol.CYBER_CRAPP
    override val uiMode: ProtocolUiMode = ProtocolUiMode.CLUSTER

    override val recommendedDaysPerWeek: Int = 3
    override val defaultWeekdays: List<Int> = listOf(1, 3, 5)
    override val frequencyCopy: String = "CYBERCRAPP RUNS 3 DAYS. A/B/C ROTATION."

    override fun dayTypes(): List<ProtocolDayType> = emptyList()

    override fun prescribe(
        cycle: ProtocolCycle?,
        dayType: ProtocolDayType?,
        exercise: Exercise,
        setIndex: Int,
        afterWeightJump: Boolean,
        max: ExerciseMax?
    ): PrescribedSet = PrescribedSet(0f, 0)

    override fun sessionSucceeded(workSets: List<SetLog>): Boolean = false

    override fun nextLoad(current: Float, succeeded: Boolean, exercise: Exercise): Float = current

    override fun stallAction(exercise: Exercise, consecutiveMisses: Int): StallAction = StallAction.None
}
