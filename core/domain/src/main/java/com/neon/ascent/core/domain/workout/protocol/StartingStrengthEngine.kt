package com.neon.ascent.core.domain.workout.protocol

import com.neon.ascent.core.domain.workout.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

class StartingStrengthEngine(
    private val unitSystem: UnitSystem = UnitSystem.IMPERIAL
) : ProtocolEngine {
    override val protocol: WorkoutProtocol = WorkoutProtocol.STARTING_STRENGTH
    override val uiMode: ProtocolUiMode = ProtocolUiMode.LINEAR

    override val recommendedDaysPerWeek: Int = 3
    override val defaultWeekdays: List<Int> = listOf(1, 3, 5)
    override val frequencyCopy: String = "LINEAR NOVICE IS 3 DAYS. A/B ALTERNATE."

    private val gson = Gson()

    override fun dayTypes(): List<ProtocolDayType> = listOf(ProtocolDayType.SS_A, ProtocolDayType.SS_B)

    override fun prescribe(
        cycle: ProtocolCycle?,
        dayType: ProtocolDayType?,
        exercise: Exercise,
        setIndex: Int,
        afterWeightJump: Boolean,
        max: ExerciseMax?
    ): PrescribedSet {
        val workingWeight = max?.trainingMax ?: 0f
        
        // Warmup logic
        val barWeight = if (unitSystem == UnitSystem.IMPERIAL) 45f else 20f
        
        return when (setIndex) {
            0 -> PrescribedSet(barWeight, 5, SetType.WARMUP)
            1 -> PrescribedSet(roundToIncrement(workingWeight * 0.4f, exercise), 5, SetType.WARMUP)
            2 -> PrescribedSet(roundToIncrement(workingWeight * 0.6f, exercise), 3, SetType.WARMUP)
            3 -> PrescribedSet(roundToIncrement(workingWeight * 0.8f, exercise), 2, SetType.WARMUP)
            else -> PrescribedSet(workingWeight, 5, SetType.NORMAL)
        }
    }

    override fun sessionSucceeded(workSets: List<SetLog>): Boolean {
        if (workSets.isEmpty()) return false
        return workSets.all { it.reps >= (it.prescribedReps ?: 5) && it.isCompleted }
    }

    override fun nextLoad(current: Float, succeeded: Boolean, exercise: Exercise): Float {
        if (!succeeded) return current
        
        val increment = when (exercise.movementType) {
            MovementType.QUAD_DOMINANT, MovementType.DEADLIFT -> 5f
            else -> 2.5f
        }
        return current + increment
    }

    override fun stallAction(exercise: Exercise, consecutiveMisses: Int): StallAction {
        return if (consecutiveMisses >= 3) {
            StallAction.Deload(0.9f)
        } else {
            StallAction.None
        }
    }

    private fun roundToIncrement(weight: Float, exercise: Exercise): Float {
        val inc = 2.5f // Default micro-loading increment
        return (Math.round(weight / inc) * inc).toFloat()
    }

    /**
     * Builds the list of exercises for the given day type.
     */
    fun getExercisesForDay(dayType: ProtocolDayType, lastDlFailed: Boolean): List<String> {
        return when (dayType) {
            ProtocolDayType.SS_A -> listOf("back_squat", "bench_press", "deadlift")
            ProtocolDayType.SS_B -> {
                if (lastDlFailed) {
                    listOf("back_squat", "military_press", "weighted_pullups")
                } else {
                    listOf("back_squat", "military_press", "deadlift")
                }
            }
            else -> emptyList()
        }
    }

    fun getNextDayType(current: ProtocolDayType?): ProtocolDayType {
        return when (current) {
            ProtocolDayType.SS_A -> ProtocolDayType.SS_B
            else -> ProtocolDayType.SS_A
        }
    }

    fun parseStrikes(configJson: String?): Map<String, Int> {
        if (configJson == null) return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val rawMap: Map<String, Any> = gson.fromJson(configJson, type)
            val strikesRaw = rawMap["strikes"] as? Map<String, Double>
            strikesRaw?.mapValues { it.value.toInt() } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun updateStrikes(configJson: String?, familyId: String, failed: Boolean): String {
        val strikes = parseStrikes(configJson).toMutableMap()
        if (failed) {
            strikes[familyId] = (strikes[familyId] ?: 0) + 1
        } else {
            strikes[familyId] = 0
        }
        
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val rawMap: MutableMap<String, Any> = try {
            gson.fromJson(configJson ?: "{}", type)
        } catch (e: Exception) {
            mutableMapOf()
        }
        rawMap["strikes"] = strikes
        return gson.toJson(rawMap)
    }
}
