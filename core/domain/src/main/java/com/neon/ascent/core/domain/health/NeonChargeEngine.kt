package com.neon.ascent.core.domain.health

import com.neon.ascent.core.domain.workout.models.WorkoutProtocol
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlin.math.sqrt

enum class ChargeConfidence {
    LOW, MED, HIGH
}

data class SessionLoad(
    val sessionRpe: Int? = null,
    val protocol: WorkoutProtocol? = null,
    val startedAt: Instant? = null,
    val durationSeconds: Long = 0L
)

data class NeonChargeInput(
    val sleepMinutesLastNight: Long?,
    val sanctumScore: Int? = null,
    val clearanceToday: Int? = null,
    val sleepEndedAt: Instant?,
    val rhrToday: Double?,
    val rhr7d: List<Double>,
    val hrvToday: Double?,
    val hrv7d: List<Double>,
    val stepsToday: Long,
    val todaysSessions: List<SessionLoad> = emptyList(),
    val hrSamplesToday: List<Pair<Instant, Int>> = emptyList(),
    val exerciseWindowsToday: List<Pair<Instant, Instant>> = emptyList(),
    val sitWindowsToday: List<Pair<Instant, Instant>> = emptyList(),
    val napsMinutesToday: Int = 0,
    val now: Instant = Instant.now(),
    val calibration: CalibrationResult? = null
)

data class NeonCharge(
    val value: Int,
    val confidence: ChargeConfidence,
    val drivers: List<Pair<String, String>>,
    val wakeSeed: Int,
    val computedAt: Instant = Instant.now()
)

object NeonChargeEngine {

    /**
     * Computes the Neon Charge (0-100) and drivers per NeonCharge.md / NeonChargeV2.md specification.
     *
     * Fixtures:
     * 1) wakeSeed 75, 10 h awake, 45 min CC RPE 7, 8 k steps, 0 HR_LOAD → evening in 15–35.
     * 2) wakeSeed 75, 8 h awake, no session, 3 k steps → still above 40.
     *
     * Sleep factor calculation:
     * - clearanceToday != null -> wakeSeed = clearanceToday (FROZEN)
     * - 450 min (7.5h) + z0 -> wakeSeed 72
     * - 360 min (6h) + z0 -> wakeSeed not 72 (e.g. 57)
     * - missing sleep & missing sanctum -> cold start wakeSeed 62 (LOW confidence), do not write SEED.
     */
    fun calculateCharge(input: NeonChargeInput): NeonCharge {
        val drivers = mutableListOf<Pair<String, String>>()

        val hasSleepData = input.sanctumScore != null ||
                (input.sleepMinutesLastNight != null && input.sleepMinutesLastNight > 0)

        val isColdStart = !hasSleepData && input.clearanceToday == null

        val wakeSeed: Int
        val confidence: ChargeConfidence

        if (input.clearanceToday != null) {
            wakeSeed = input.clearanceToday.coerceIn(0, 100)
            confidence = ChargeConfidence.HIGH
            drivers.add("SEED" to "SEED ${input.clearanceToday} FROZEN")
        } else if (isColdStart) {
            wakeSeed = 62
            confidence = ChargeConfidence.LOW
            drivers.add("COLD_START" to "Default baseline (62%) due to missing sleep & vitals")
        } else {
            val baseSleepScore: Double

            if (input.sanctumScore != null) {
                baseSleepScore = input.sanctumScore.toDouble()
                drivers.add("SLEEP" to "SANCTUM ${input.sanctumScore}")
            } else {
                val sleepMin = input.sleepMinutesLastNight ?: 450L
                // Non-linear curve: 8h (480m) = 100%, 6h = ~81%, 4h = ~61%
                val optimalSleep = 480.0
                val sleepRatio = (sleepMin / optimalSleep).coerceIn(0.0, 1.25)
                baseSleepScore = (100.0 * Math.pow(sleepRatio, 0.7)).coerceIn(10.0, 100.0)

                val hours = sleepMin / 60
                val mins = sleepMin % 60
                val timeStr = if (mins > 0) "${hours}h${mins}m" else "${hours}h"
                drivers.add("SLEEP" to "SLEEP $timeStr (Base: ${baseSleepScore.toInt()})")
            }

            // z-scores computed only if 7d series size >= 5
            val hrvZ = if (input.hrvToday != null) calculateZScore(input.hrvToday, input.hrv7d) else null
            val rhrZ = if (input.rhrToday != null) calculateZScore(input.rhrToday, input.rhr7d) else null

            var ansModifiers = 0.0

            if (hrvZ != null) {
                val hrvPts = (hrvZ * 5.0).coerceIn(-10.0, 10.0)
                ansModifiers += hrvPts
                drivers.add("HRV_STRESS" to "HRV z-score ${formatDouble(hrvZ)} (${if (hrvPts >= 0) "+" else ""}${hrvPts.toInt()} pts)")
            }

            if (rhrZ != null) {
                val rhrPts = (-rhrZ * 5.0).coerceIn(-10.0, 10.0) // Lower RHR is better
                ansModifiers += rhrPts
                drivers.add("RHR_STRESS" to "RHR z-score ${formatDouble(rhrZ)} (${if (rhrPts >= 0) "+" else ""}${rhrPts.toInt()} pts)")
            }

            val wakeChargeRaw = baseSleepScore + ansModifiers
            wakeSeed = wakeChargeRaw.toInt().coerceIn(10, 100)

            confidence = when {
                hrvZ != null && rhrZ != null -> ChargeConfidence.HIGH
                hrvZ != null || rhrZ != null -> ChargeConfidence.MED
                else -> ChargeConfidence.LOW
            }
        }

        // V2 Drain calculations: hoursAwake starts at sleepEnd, not local midnight (00:00).
        val activeCalibration = input.calibration?.takeIf { it.isPromoted }
        val kPassive = activeCalibration?.kPassive ?: 0.085
        val sessionMult = activeCalibration?.sessionMult ?: 1.0
        val stepMult = activeCalibration?.stepMult ?: 1.0
        val hrMult = activeCalibration?.hrMult ?: 1.0

        if (activeCalibration != null) {
            val kStr = String.format(Locale.US, "%.3f", activeCalibration.kPassive)
            drivers.add("CALIBRATED" to "CALIBRATED n=${activeCalibration.effectiveN} · k=$kStr")
        }

        val wakeTime = input.sleepEndedAt ?: input.now
        val hoursAwake = Duration.between(wakeTime, input.now).toMinutes() / 60.0
        
        // Passive Asymptotic Drain
        val passiveFraction = 1.0 - Math.exp(-kPassive * hoursAwake.coerceAtLeast(0.0))
        val passiveDrain = (wakeSeed * passiveFraction).coerceAtMost(wakeSeed * 0.85)
        if (hoursAwake > 0.1 && passiveDrain > 0.5) {
            val hoursStr = String.format(Locale.US, "%.1f", hoursAwake)
            drivers.add("PASSIVE" to "PASSIVE ${hoursStr}h (-${passiveDrain.toInt()})")
        }

        // Session Drain (V2)
        var sessionDrain = 0.0
        input.todaysSessions.forEach { session ->
            val durationHours = session.durationSeconds / 3600.0
            val rpeFactor = (session.sessionRpe ?: 6) / 10.0
            val isHardProtocol = session.protocol == WorkoutProtocol.CYBER_CRAPP || session.protocol == WorkoutProtocol.WESTSIDE
            val protoMultiplier = if (isHardProtocol) 1.4 else 1.0
            sessionDrain += durationHours * 48.0 * rpeFactor * protoMultiplier * sessionMult
        }
        if (sessionDrain > 0.5) {
            drivers.add("WORKOUT_DRAIN" to "Workout load (-${sessionDrain.toInt()} pts)")
        }

        // HR_LOAD Drain (V2)
        val hrSpanMinutes = if (input.hrSamplesToday.size >= 2) {
            Duration.between(input.hrSamplesToday.first().first, input.hrSamplesToday.last().first).toMinutes()
        } else 0L

        val hasHrCoverage = input.hrSamplesToday.size >= 20 && hrSpanMinutes >= 120
        val rhrBaseline = input.rhrToday ?: if (input.rhr7d.size >= 5) input.rhr7d.average() else null

        var hrLoadDrain = 0.0
        if (hasHrCoverage && rhrBaseline != null && rhrBaseline > 0.0) {
            val hrThreshold = maxOf(rhrBaseline + 25.0, 100.0)

            val validSitWindows = input.sitWindowsToday.filter {
                Duration.between(it.first, it.second).toMinutes() >= 10
            }
            val allMaskWindows = input.exerciseWindowsToday + validSitWindows

            val unmaskedSamples = input.hrSamplesToday.filter { sample ->
                allMaskWindows.none { window ->
                    !sample.first.isBefore(window.first) && !sample.first.isAfter(window.second)
                }
            }

            val elevatedBins = unmaskedSamples
                .filter { it.second.toDouble() >= hrThreshold }
                .map { it.first.epochSecond / 60 }
                .toSet()

            val elevatedHrMinutes = elevatedBins.size
            if (elevatedHrMinutes > 0) {
                val isThin = input.hrSamplesToday.size < 40
                if (isThin) {
                    hrLoadDrain = (elevatedHrMinutes / 12.0).coerceIn(0.0, 15.0) * hrMult
                    drivers.add("HR_LOAD_THIN" to "${elevatedHrMinutes}m (thin)")
                } else {
                    hrLoadDrain = (elevatedHrMinutes / 8.0).coerceIn(0.0, 22.0) * hrMult
                    drivers.add("HR_LOAD" to "${elevatedHrMinutes}m")
                }
            }
        }

        // Steps Drain (V2)
        val stepsAbove4000 = maxOf(0L, input.stepsToday - 4000L)
        val stepsDrain = (stepsAbove4000 / 280.0) * stepMult
        if (stepsDrain > 1.0) {
            drivers.add("STEPS_DRAIN" to "${input.stepsToday} steps (-${stepsDrain.toInt()} pts)")
        }

        // Nap Recharge (Non-linear piecewise based on sleep architecture)
        val napRecharge = when {
            input.napsMinutesToday <= 0 -> 0.0
            input.napsMinutesToday <= 30 -> input.napsMinutesToday * 0.5 // Power nap zone
            input.napsMinutesToday <= 60 -> 15.0 - ((input.napsMinutesToday - 30) * 0.2) // Sleep inertia penalty
            else -> 9.0 + ((input.napsMinutesToday - 60) * 0.3).coerceAtMost(16.0) // Full cycle recovery
        }
        
        if (napRecharge > 0) {
            drivers.add("NAP_RECHARGE" to "${input.napsMinutesToday}m nap (+${napRecharge.toInt()} pts)")
        }

        val totalLoadDrain = sessionDrain + hrLoadDrain + stepsDrain
        val finalValue = (wakeSeed - passiveDrain - totalLoadDrain + napRecharge).toInt().coerceIn(5, 98)

        return NeonCharge(
            value = finalValue,
            confidence = confidence,
            drivers = drivers,
            wakeSeed = wakeSeed,
            computedAt = input.now
        )
    }

    private fun calculateZScore(value: Double, series: List<Double>): Double? {
        if (series.size < 5) return null
        val mean = series.average()
        val variance = series.sumOf { (it - mean) * (it - mean) } / series.size
        val stdDev = sqrt(variance)
        if (stdDev < 0.001) return 0.0
        return ((value - mean) / stdDev).coerceIn(-2.5, 2.5)
    }

    private fun formatDouble(valDouble: Double): String {
        return String.format(Locale.US, "%.2f", valDouble)
    }
}
