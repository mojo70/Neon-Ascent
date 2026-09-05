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
    val sleepEndedAt: Instant?,
    val rhrToday: Double?,
    val rhr7d: List<Double>,
    val hrvToday: Double?,
    val hrv7d: List<Double>,
    val stepsToday: Long,
    val todaysSessions: List<SessionLoad> = emptyList(),
    val hrSamplesToday: List<Pair<Instant, Int>> = emptyList(),
    val exerciseWindowsToday: List<Pair<Instant, Instant>> = emptyList(),
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
     */
    fun calculateCharge(input: NeonChargeInput): NeonCharge {
        val drivers = mutableListOf<Pair<String, String>>()

        val isColdStart = (input.sleepMinutesLastNight == null || input.sleepMinutesLastNight <= 0) &&
                input.hrvToday == null &&
                input.rhrToday == null

        val wakeSeed: Int
        val confidence: ChargeConfidence

        if (isColdStart) {
            wakeSeed = 62
            confidence = ChargeConfidence.LOW
            drivers.add("COLD_START" to "Default baseline (62%) due to missing sleep & vitals")
        } else {
            val sleepMins = input.sleepMinutesLastNight ?: 0L
            val sleepScore = if (sleepMins > 0) {
                (sleepMins / 480.0 * 100.0).coerceIn(0.0, 100.0)
            } else {
                70.0
            }

            if (sleepMins > 0) {
                val hours = sleepMins / 60
                val mins = sleepMins % 60
                drivers.add("SLEEP" to "${hours}h ${mins}m logged (+${(sleepScore * 0.5).toInt()} pts)")
            }

            // z-scores computed only if 7d series size >= 5
            val hrvZ = if (input.hrvToday != null) calculateZScore(input.hrvToday, input.hrv7d) else null
            val rhrZ = if (input.rhrToday != null) calculateZScore(input.rhrToday, input.rhr7d) else null

            val hrvContrib = if (hrvZ != null) {
                val contrib = (hrvZ * 12.0).coerceIn(-25.0, 25.0)
                drivers.add("HRV_STRESS" to "HRV z-score ${formatDouble(hrvZ)} (${if (contrib >= 0) "+" else ""}${contrib.toInt()} pts)")
                contrib
            } else 0.0

            val rhrContrib = if (rhrZ != null) {
                val contrib = (-rhrZ * 12.0).coerceIn(-25.0, 25.0)
                drivers.add("RHR_STRESS" to "RHR z-score ${formatDouble(rhrZ)} (${if (contrib >= 0) "+" else ""}${contrib.toInt()} pts)")
                contrib
            } else 0.0

            val napContrib = (input.napsMinutesToday * 0.25).coerceIn(0.0, 15.0)
            if (input.napsMinutesToday > 0) {
                drivers.add("NAP_BONUS" to "+${napContrib.toInt()} pts from ${input.napsMinutesToday}m nap")
            }

            wakeSeed = (sleepScore * 0.5 + 25.0 + hrvContrib + rhrContrib + napContrib)
                .toInt()
                .coerceIn(10, 100)

            confidence = when {
                hrvZ != null && rhrZ != null && sleepMins > 0 -> ChargeConfidence.HIGH
                sleepMins > 0 || (hrvZ != null || rhrZ != null) -> ChargeConfidence.MED
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

        // Unmasked HR Drain
        val unmaskedSamples = input.hrSamplesToday.filter { sample ->
            input.exerciseWindowsToday.none { window ->
                !sample.first.isBefore(window.first) && !sample.first.isAfter(window.second)
            }
        }
        val elevatedCount = unmaskedSamples.count { it.second > 100 }
        val elevatedDrain = (elevatedCount * 0.15).coerceIn(0.0, 20.0)
        if (elevatedDrain > 1.0) {
            drivers.add("ELEVATED_HR" to "Non-workout elevated HR (-${elevatedDrain.toInt()} pts)")
        }

        // Steps Drain
        val stepsDrain = (input.stepsToday * 0.001).coerceIn(0.0, 15.0)
        if (stepsDrain > 1.0) {
            drivers.add("STEPS_DRAIN" to "${input.stepsToday} steps (-${stepsDrain.toInt()} pts)")
        }

        val totalDrain = passiveDrain + sessionDrain + elevatedDrain + stepsDrain
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
