package com.neon.ascent.core.domain.workout.protocol

import com.neon.ascent.core.domain.workout.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
import java.time.temporal.ChronoUnit

class HSTEngine(
    private val unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    private val weightIncrement: Float = 2.5f
) : ProtocolEngine {
    override val protocol: WorkoutProtocol = WorkoutProtocol.HST
    override val uiMode: ProtocolUiMode = ProtocolUiMode.PRESCRIBED

    override val recommendedDaysPerWeek: Int = 3
    override val defaultWeekdays: List<Int> = listOf(1, 3, 5)
    override val frequencyCopy: String = "HST IS FULL BODY 3×. LADDER ASSUMES THIS."

    private val gson = Gson()

    override fun dayTypes(): List<ProtocolDayType> = listOf(
        ProtocolDayType.HST_15, ProtocolDayType.HST_10, ProtocolDayType.HST_5,
        ProtocolDayType.HST_NEG, ProtocolDayType.HST_SD
    )

    override fun prescribe(
        cycle: ProtocolCycle?,
        dayType: ProtocolDayType?,
        exercise: Exercise,
        setIndex: Int,
        afterWeightJump: Boolean,
        max: ExerciseMax?
    ): PrescribedSet {
        if (cycle == null || max == null || dayType == null) return PrescribedSet(0f, 0)

        val blockReps = when (dayType) {
            ProtocolDayType.HST_15 -> 15
            ProtocolDayType.HST_10 -> 10
            ProtocolDayType.HST_5 -> 5
            ProtocolDayType.HST_NEG -> 5
            else -> 0
        }

        val rmValue = when (dayType) {
            ProtocolDayType.HST_15 -> max.rm15 ?: (max.oneRepMax * 0.65f)
            ProtocolDayType.HST_10 -> max.rm10 ?: (max.oneRepMax * 0.75f)
            ProtocolDayType.HST_5, ProtocolDayType.HST_NEG -> max.rm5 ?: (max.oneRepMax * 0.86f)
            else -> 0f
        }

        // Session index in current 6-session block
        val sessionsPerBlock = 6
        val sessionIndexInBlock = cycle.currentDayIndex % sessionsPerBlock
        
        // Weight calculation: start at 80% of RM, end at 100% of RM over 6 sessions
        val startWeight = rmValue * 0.80f
        val rawWeight = startWeight + (rmValue - startWeight) * sessionIndexInBlock / (sessionsPerBlock - 1)
        
        // Check for "hold" flag in config if previous session was a miss
        val config = parseConfig(cycle.configJson)
        val isHeld = config.holds[exercise.familyId] == true
        val finalWeight = if (isHeld) {
            // Ideally we'd look up the weight from the last session, but for now we just use the same logic
            // but effectively don't advance the index for this specific lift. 
            // Simplified: if held, use index - 1 (or current if index is 0)
            val heldIndex = if (sessionIndexInBlock > 0) sessionIndexInBlock - 1 else 0
            startWeight + (rmValue - startWeight) * heldIndex / (sessionsPerBlock - 1)
        } else {
            rawWeight
        }

        val roundedWeight = roundToIncrement(finalWeight)

        // Warmups: 1-2 sets
        return when (setIndex) {
            0 -> PrescribedSet(roundToIncrement(roundedWeight * 0.6f), blockReps, SetType.WARMUP)
            1 -> PrescribedSet(roundToIncrement(roundedWeight * 0.8f), blockReps, SetType.WARMUP)
            else -> PrescribedSet(roundedWeight, blockReps, SetType.NORMAL)
        }
    }

    override fun sessionSucceeded(workSets: List<SetLog>): Boolean {
        if (workSets.isEmpty()) return false
        // HST succeeds if any work set hits the prescribed reps (less strict than SS to keep moving)
        // or all work sets hit reps. Requirements said "do not bump ladder if miss".
        // Let's say all work sets must hit.
        return workSets.all { it.isCompleted && it.reps >= (it.prescribedReps ?: 0) }
    }

    override fun nextLoad(current: Float, succeeded: Boolean, exercise: Exercise): Float {
        // Ladder is pre-calculated based on session index, but we use this to handle RM updates after block
        return current
    }

    override fun stallAction(exercise: Exercise, consecutiveMisses: Int): StallAction {
        return StallAction.None
    }

    private fun roundToIncrement(weight: Float): Float {
        return (Math.round(weight / weightIncrement) * weightIncrement).toFloat()
    }

    fun getDayType(currentDayIndex: Int): ProtocolDayType {
        return when (currentDayIndex) {
            in 0..5 -> ProtocolDayType.HST_15
            in 6..11 -> ProtocolDayType.HST_10
            in 12..17 -> ProtocolDayType.HST_5
            in 18..23 -> ProtocolDayType.HST_NEG // Optional
            else -> ProtocolDayType.HST_SD
        }
    }

    fun isSdActive(cycle: ProtocolCycle): Boolean {
        val config = parseConfig(cycle.configJson)
        val sdUntil = config.sdUntil ?: return false
        return Instant.now().isBefore(Instant.ofEpochMilli(sdUntil))
    }

    /**
     * Estimates HST RMs from a 1RM value.
     */
    fun estimateRms(oneRepMax: Float): Triple<Float, Float, Float> {
        return Triple(
            roundToIncrement(oneRepMax * 0.65f),
            roundToIncrement(oneRepMax * 0.75f),
            roundToIncrement(oneRepMax * 0.86f)
        )
    }

    fun generateLadder(rm: Float): List<Float> {
        val sessionsPerBlock = 6
        val startWeight = rm * 0.80f
        return (0 until sessionsPerBlock).map { i ->
            roundToIncrement(startWeight + (rm - startWeight) * i / (sessionsPerBlock - 1))
        }
    }

    data class HSTConfig(
        val holds: Map<String, Boolean> = emptyMap(),
        val sdUntil: Long? = null
    )

    fun parseConfig(json: String?): HSTConfig {
        if (json == null) return HSTConfig()
        return try {
            gson.fromJson(json, HSTConfig::class.java)
        } catch (e: Exception) {
            HSTConfig()
        }
    }

    fun updateConfigWithMiss(json: String?, familyId: String, missed: Boolean): String {
        val config = parseConfig(json)
        val newHolds = config.holds.toMutableMap()
        newHolds[familyId] = missed
        return gson.toJson(config.copy(holds = newHolds))
    }

    fun startSd(json: String?, days: Int = 12): String {
        val config = parseConfig(json)
        val sdUntil = Instant.now().plus(days.toLong(), ChronoUnit.DAYS).toEpochMilli()
        return gson.toJson(config.copy(sdUntil = sdUntil))
    }

    /**
     * Sample ladder for squat 10RM=200 across 6 sessions:
     * Start = 200 * 0.8 = 160
     * S0: 160 + (200-160)*0/5 = 160
     * S1: 160 + 40*1/5 = 168 -> 167.5 (if 2.5 inc)
     * S2: 160 + 40*2/5 = 176 -> 175
     * S3: 160 + 40*3/5 = 184 -> 185
     * S4: 160 + 40*4/5 = 192 -> 192.5
     * S5: 160 + 40*5/5 = 200
     */
}
