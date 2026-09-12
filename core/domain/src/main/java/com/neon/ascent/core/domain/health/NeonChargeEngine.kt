package com.neon.ascent.core.domain.health

import com.neon.ascent.core.domain.workout.models.WorkoutProtocol
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
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
    val now: Instant = Instant.now()
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
     * Computes the Neon Charge (0-100) and drivers per NeonCharge.md specification.
     *
     * Sleep factor calculation:
     * - 450 min (7.5h) + z0 -> wakeSeed 72
     * - 360 min (6h) + z0 -> wakeSeed not 72 (e.g. 57)
     * - missing sleep & missing sanctum -> cold start wakeSeed 62 (LOW confidence), do not write SEED.
     */
    fun calculateCharge(input: NeonChargeInput): NeonCharge {
        val drivers = mutableListOf<Pair<String, String>>()

        val hasSleepData = input.sanctumScore != null ||
                (input.sleepMinutesLastNight != null && input.sleepMinutesLastNight > 0)

        val isColdStart = !hasSleepData

        val wakeSeed: Int
        val confidence: ChargeConfidence

        if (isColdStart) {
            wakeSeed = 62
            confidence = ChargeConfidence.LOW
            drivers.add("COLD_START" to "Default baseline (62%) due to missing sleep & vitals")
        } else {
            val sleepFactor: Double

            if (input.sanctumScore != null) {
                sleepFactor = (input.sanctumScore / 80.0).coerceIn(0.55, 1.15)
                drivers.add("SLEEP" to "SANCTUM ${input.sanctumScore}")
            } else {
                val sleepMin = input.sleepMinutesLastNight ?: 450L
                sleepFactor = (sleepMin / 450.0).coerceIn(0.55, 1.15)
                val hours = sleepMin / 60
                val mins = sleepMin % 60
                val timeStr = if (mins > 0) "${hours}h${mins}m" else "${hours}h"
                drivers.add("SLEEP" to "SLEEP $timeStr")
            }

            // z-scores computed only if 7d series size >= 5
            val hrvZ = if (input.hrvToday != null) calculateZScore(input.hrvToday, input.hrv7d) else null
            val rhrZ = if (input.rhrToday != null) calculateZScore(input.rhrToday, input.rhr7d) else null

            val hrvFactor = if (hrvZ != null) {
                val f = 1.0 + 0.08 * hrvZ.coerceIn(-1.5, 1.5)
                val contrib = ((f - 1.0) * 100.0).toInt()
                drivers.add("HRV_STRESS" to "HRV z-score ${formatDouble(hrvZ)} (${if (contrib >= 0) "+" else ""}$contrib pts)")
                f
            } else 1.0

            val rhrFactor = if (rhrZ != null) {
                val f = 1.0 - 0.08 * rhrZ.coerceIn(-1.5, 1.5)
                val contrib = ((f - 1.0) * 100.0).toInt()
                drivers.add("RHR_STRESS" to "RHR z-score ${formatDouble(rhrZ)} (${if (contrib >= 0) "+" else ""}$contrib pts)")
                f
            } else 1.0

            val napContrib = (input.napsMinutesToday * 0.25).coerceIn(0.0, 15.0)
            if (input.napsMinutesToday > 0) {
                drivers.add("NAP_BONUS" to "+${napContrib.toInt()} pts from ${input.napsMinutesToday}m nap")
            }

            val wakeChargeRaw = 72.0 * sleepFactor * hrvFactor * rhrFactor + napContrib
            wakeSeed = wakeChargeRaw.toInt().coerceIn(35, 95)

            confidence = when {
                hrvZ != null && rhrZ != null -> ChargeConfidence.HIGH
                hrvZ != null || rhrZ != null -> ChargeConfidence.MED
                else -> ChargeConfidence.LOW
            }
        }

        // Drain calculations
        val wakeTime = input.sleepEndedAt ?: input.now.atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()

        val hoursAwake = Duration.between(wakeTime, input.now).toMinutes() / 60.0
        val passiveDrain = (hoursAwake.coerceAtLeast(0.0) * 2.8).coerceIn(0.0, 50.0)

        // Session Drain
        var sessionDrain = 0.0
        input.todaysSessions.forEach { session ->
            val durationHours = session.durationSeconds / 3600.0
            val rpeFactor = (session.sessionRpe ?: 6) / 10.0
            val protoMultiplier = if (session.protocol == WorkoutProtocol.CYBER_CRAPP) 1.25 else 1.0
            sessionDrain += durationHours * 15.0 * rpeFactor * protoMultiplier
        }
        if (sessionDrain > 0.5) {
            drivers.add("WORKOUT_DRAIN" to "Workout load (-${sessionDrain.toInt()} pts)")
        }

        // HR_LOAD Drain (non-exercise, non-sit elevated HR)
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
                hrLoadDrain = (elevatedHrMinutes / 12.0).coerceIn(0.0, 15.0)
                drivers.add("HR_LOAD" to "${elevatedHrMinutes}m")
            }
        }

        // Steps Drain
        val stepsDrain = (input.stepsToday * 0.001).coerceIn(0.0, 15.0)
        if (stepsDrain > 1.0) {
            drivers.add("STEPS_DRAIN" to "${input.stepsToday} steps (-${stepsDrain.toInt()} pts)")
        }

        val totalDrain = passiveDrain + sessionDrain + hrLoadDrain + stepsDrain
        val finalValue = (wakeSeed - totalDrain).toInt().coerceIn(0, 100)

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
