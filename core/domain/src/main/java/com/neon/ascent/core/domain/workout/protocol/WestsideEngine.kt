package com.neon.ascent.core.domain.workout.protocol

import com.neon.ascent.core.domain.workout.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class WestsideEngine(
    private val weightIncrement: Float = 2.5f
) : ProtocolEngine {
    override val protocol: WorkoutProtocol = WorkoutProtocol.WESTSIDE
    override val uiMode: ProtocolUiMode = ProtocolUiMode.DYNAMIC // Overridden in prescribe for ME

    override val recommendedDaysPerWeek: Int = 4
    override val defaultWeekdays: List<Int> = listOf(1, 3, 5, 6)
    override val frequencyCopy: String = "CONJUGATE IS 4 DAYS. ME / DE / ME / DE."

    private val gson = Gson()

    override fun dayTypes(): List<ProtocolDayType> = listOf(
        ProtocolDayType.WS_ME_LOWER, ProtocolDayType.WS_DE_UPPER,
        ProtocolDayType.WS_ME_UPPER, ProtocolDayType.WS_DE_LOWER,
        ProtocolDayType.WS_RE
    )

    override fun prescribe(
        cycle: ProtocolCycle?,
        dayType: ProtocolDayType?,
        exercise: Exercise,
        setIndex: Int,
        afterWeightJump: Boolean,
        max: ExerciseMax?
    ): PrescribedSet {
        if (dayType == null || max == null) return PrescribedSet(0f, 0)

        return when (dayType) {
            ProtocolDayType.WS_ME_LOWER, ProtocolDayType.WS_ME_UPPER -> {
                prescribeME(max, setIndex)
            }
            ProtocolDayType.WS_DE_LOWER, ProtocolDayType.WS_DE_UPPER -> {
                val weekIndex = (cycle?.currentDayIndex ?: 0) / 4 % 3
                val pct = 0.50f + (weekIndex * 0.05f)
                val reps = if (dayType == ProtocolDayType.WS_DE_LOWER) 2 else 3
                
                if (setIndex < 3) {
                    PrescribedSet(roundToIncrement(max.oneRepMax * 0.3f), reps, SetType.WARMUP)
                } else {
                    PrescribedSet(roundToIncrement(max.oneRepMax * pct), reps, SetType.POWER, percentOfMax = pct)
                }
            }
            ProtocolDayType.WS_RE -> {
                PrescribedSet(0f, 12, SetType.NORMAL)
            }
            else -> PrescribedSet(0f, 0)
        }
    }

    private fun prescribeME(max: ExerciseMax, setIndex: Int): PrescribedSet {
        val oneRM = max.oneRepMax
        return when {
            setIndex == 0 -> PrescribedSet(roundToIncrement(oneRM * 0.4f), 5, SetType.WARMUP)
            setIndex == 1 -> PrescribedSet(roundToIncrement(oneRM * 0.5f), 3, SetType.WARMUP)
            setIndex == 2 -> PrescribedSet(roundToIncrement(oneRM * 0.6f), 2, SetType.WARMUP)
            setIndex == 3 -> PrescribedSet(roundToIncrement(oneRM * 0.7f), 1, SetType.WARMUP)
            setIndex == 4 -> PrescribedSet(roundToIncrement(oneRM * 0.8f), 1, SetType.WARMUP)
            setIndex == 5 -> PrescribedSet(roundToIncrement(oneRM * 0.9f), 1, SetType.MAX_EFFORT, percentOfMax = 0.9f)
            else -> PrescribedSet(roundToIncrement(oneRM * 0.9f), 1, SetType.MAX_EFFORT)
        }
    }

    override fun sessionSucceeded(workSets: List<SetLog>): Boolean {
        // Success is subjective in Westside (hitting a heavy single), but we'll say completing any work set.
        return workSets.any { it.isCompleted && it.reps >= (it.prescribedReps ?: 1) }
    }

    override fun nextLoad(current: Float, succeeded: Boolean, exercise: Exercise): Float {
        return current // Handled by 1RM updates
    }

    override fun stallAction(exercise: Exercise, consecutiveMisses: Int): StallAction {
        return StallAction.None
    }

    private fun roundToIncrement(weight: Float): Float {
        return (Math.round(weight / weightIncrement) * weightIncrement).toFloat()
    }

    fun getVariantIds(dayType: ProtocolDayType): List<String> {
        return when (dayType) {
            ProtocolDayType.WS_ME_LOWER -> listOf("back_squat", "squat_box", "squat_safety_bar", "deadlift", "deadlift_sumo", "trap_bar_deadlift")
            ProtocolDayType.WS_ME_UPPER -> listOf("bench_press", "floor_press_dumbbell", "military_press")
            ProtocolDayType.WS_DE_LOWER -> listOf("squat_box", "back_squat")
            ProtocolDayType.WS_DE_UPPER -> listOf("bench_press")
            else -> emptyList()
        }
    }

    fun rotateVariant(configJson: String?, dayType: ProtocolDayType): String {
        val config = parseConfig(configJson)
        val variants = getVariantIds(dayType)
        if (variants.isEmpty()) return configJson ?: "{}"

        val key = when (dayType) {
            ProtocolDayType.WS_ME_LOWER -> "lastVariantIndexME_L"
            ProtocolDayType.WS_ME_UPPER -> "lastVariantIndexME_U"
            else -> return configJson ?: "{}"
        }

        val lastIndex = config.indices[key] ?: -1
        val nextIndex = (lastIndex + 1) % variants.size
        
        val newIndices = config.indices.toMutableMap()
        newIndices[key] = nextIndex
        
        return gson.toJson(config.copy(indices = newIndices))
    }

    fun getCurrentVariant(configJson: String?, dayType: ProtocolDayType): String? {
        val config = parseConfig(configJson)
        val variants = getVariantIds(dayType)
        if (variants.isEmpty()) return null

        val key = when (dayType) {
            ProtocolDayType.WS_ME_LOWER -> "lastVariantIndexME_L"
            ProtocolDayType.WS_ME_UPPER -> "lastVariantIndexME_U"
            else -> return variants.first()
        }

        val index = (config.indices[key] ?: 0) % variants.size
        return variants[index]
    }

    data class WestsideConfig(
        val indices: Map<String, Int> = emptyMap()
    )

    private fun parseConfig(json: String?): WestsideConfig {
        if (json == null) return WestsideConfig()
        return try {
            gson.fromJson(json, WestsideConfig::class.java)
        } catch (e: Exception) {
            WestsideConfig()
        }
    }
}
