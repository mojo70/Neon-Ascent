package com.neon.ascent.core.domain.workout.protocol

import com.neon.ascent.core.domain.workout.models.*
import java.util.UUID

class FiveThreeOneEngine(
    private val weightIncrement: Float = 2.5f
) : ProtocolEngine {
    override val protocol: WorkoutProtocol = WorkoutProtocol.FIVE_THREE_ONE
    override val uiMode: ProtocolUiMode = ProtocolUiMode.PRESCRIBED

    override val recommendedDaysPerWeek: Int = 4
    override val defaultWeekdays: List<Int> = listOf(1, 2, 4, 5)
    override val frequencyCopy: String = "5/3/1 IS 4 MAINS. ONE LIFT PER DAY."

    override fun dayTypes(): List<ProtocolDayType> = listOf(
        ProtocolDayType.FTV_W1, ProtocolDayType.FTV_W2, ProtocolDayType.FTV_W3, ProtocolDayType.FTV_DELOAD
    )

    override fun prescribe(
        cycle: ProtocolCycle?,
        dayType: ProtocolDayType?,
        exercise: Exercise,
        setIndex: Int,
        afterWeightJump: Boolean,
        max: ExerciseMax?
    ): PrescribedSet {
        if (max == null || dayType == null) return PrescribedSet(0f, 0)
        
        val tm = max.trainingMax ?: (max.oneRepMax * 0.9f)
        
        // Warmups (simplified 5/3/1 style)
        if (setIndex < 3) {
            val warmupPercents = listOf(0.40f, 0.50f, 0.60f)
            return PrescribedSet(roundToIncrement(tm * warmupPercents[setIndex]), 5, SetType.WARMUP, percentOfMax = warmupPercents[setIndex])
        }

        // Work Sets
        val workIndex = setIndex - 3
        val (percents, reps) = when (dayType) {
            ProtocolDayType.FTV_W1 -> listOf(0.65f, 0.75f, 0.85f) to listOf(5, 5, 5)
            ProtocolDayType.FTV_W2 -> listOf(0.70f, 0.75f, 0.90f) to listOf(3, 3, 3)
            ProtocolDayType.FTV_W3 -> listOf(0.75f, 0.85f, 0.95f) to listOf(5, 3, 1)
            ProtocolDayType.FTV_DELOAD -> listOf(0.40f, 0.50f, 0.60f) to listOf(5, 5, 5)
            else -> listOf(0.65f, 0.75f, 0.85f) to listOf(5, 5, 5)
        }

        val pct = percents.getOrNull(workIndex) ?: 0.85f
        val targetReps = reps.getOrNull(workIndex) ?: 5
        val isAmrap = workIndex == 2 && dayType != ProtocolDayType.FTV_DELOAD

        return PrescribedSet(
            weight = roundToIncrement(tm * pct),
            reps = targetReps,
            setType = SetType.NORMAL,
            percentOfMax = pct,
            isAmrap = isAmrap
        )
    }

    override fun sessionSucceeded(workSets: List<SetLog>): Boolean {
        // In 5/3/1, hitting the minimum reps on the AMRAP is success
        val amrapSet = workSets.find { it.isAmrap } ?: workSets.lastOrNull()
        return amrapSet?.let { it.isCompleted && it.reps >= (it.prescribedReps ?: 1) } ?: false
    }

    override fun nextLoad(current: Float, succeeded: Boolean, exercise: Exercise): Float {
        // Progression is handled per-cycle, not per-session in 5/3/1
        return current
    }

    override fun stallAction(exercise: Exercise, consecutiveMisses: Int): StallAction {
        return StallAction.None
    }

    private fun roundToIncrement(weight: Float): Float {
        return (Math.round(weight / weightIncrement) * weightIncrement).toFloat()
    }

    fun getDayType(currentDayIndex: Int): ProtocolDayType {
        val weekIndex = currentDayIndex / 4
        return when (weekIndex % 4) {
            0 -> ProtocolDayType.FTV_W1
            1 -> ProtocolDayType.FTV_W2
            2 -> ProtocolDayType.FTV_W3
            else -> ProtocolDayType.FTV_DELOAD
        }
    }

    fun getMainExerciseId(currentDayIndex: Int): String {
        return when (currentDayIndex % 4) {
            0 -> "military_press"
            1 -> "deadlift"
            2 -> "bench_press"
            else -> "back_squat"
        }
    }

    fun getComplementaryExerciseId(mainId: String): String? {
        return when (mainId) {
            "bench_press" -> "bent_over_row"
            "back_squat" -> "romanian_deadlift"
            "military_press" -> "weighted_pullups"
            "deadlift" -> "back_squat" // BBB Front Squat or similar would be better, but sticking to existing
            else -> null
        }
    }
}
