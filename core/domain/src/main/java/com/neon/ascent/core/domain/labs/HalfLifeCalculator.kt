package com.neon.ascent.core.domain.labs

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.exp
import kotlin.math.ln

/**
 * Deterministic math engine for single and multi-dose exponential decay curves,
 * peak/trough approximations, and time to steady state.
 */
object HalfLifeCalculator {

    data class TimePoint(
        val timeHours: Double,
        val amountMg: Double
    )

    data class HalfLifeResult(
        val peakSteadyStateMg: BigDecimal,
        val troughSteadyStateMg: BigDecimal,
        val timeToSteadyStateHours: BigDecimal,
        val peakTroughRatio: BigDecimal,
        val curvePoints: List<TimePoint>
    )

    /**
     * Calculates exponential decay approximation.
     * @param halfLifeHours Half-life in hours
     * @param doseIntervalHours Interval between doses in hours
     * @param doseAmountMg Amount per dose in mg
     * @param numDoses Number of doses to simulate (default 10)
     */
    fun calculateDecay(
        halfLifeHours: Double,
        doseIntervalHours: Double,
        doseAmountMg: Double,
        numDoses: Int = 10
    ): HalfLifeResult? {
        if (halfLifeHours <= 0.0 || doseIntervalHours <= 0.0 || doseAmountMg <= 0.0 || numDoses <= 0) {
            return null
        }

        // Decay constant k = ln(2) / t_1/2
        val k = ln(2.0) / halfLifeHours

        // Accumulation factor R = 1 / (1 - exp(-k * tau))
        val expKInterval = exp(-k * doseIntervalHours)
        val r = 1.0 / (1.0 - expKInterval)

        val peakSteadyState = doseAmountMg * r
        val troughSteadyState = peakSteadyState * expKInterval
        val timeToSteadyState = 4.5 * halfLifeHours
        val ratio = if (troughSteadyState > 0) peakSteadyState / troughSteadyState else 1.0

        // Generate curve time points
        val totalTimeHours = (numDoses * doseIntervalHours).coerceAtLeast(halfLifeHours * 5.0)
        val steps = 100
        val dt = totalTimeHours / steps
        val points = mutableListOf<TimePoint>()

        for (i in 0..steps) {
            val t = i * dt
            var totalAmount = 0.0
            for (d in 0 until numDoses) {
                val doseTime = d * doseIntervalHours
                if (t >= doseTime) {
                    val elapsed = t - doseTime
                    totalAmount += doseAmountMg * exp(-k * elapsed)
                }
            }
            points.add(TimePoint(t, totalAmount))
        }

        return HalfLifeResult(
            peakSteadyStateMg = BigDecimal(peakSteadyState).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros(),
            troughSteadyStateMg = BigDecimal(troughSteadyState).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros(),
            timeToSteadyStateHours = BigDecimal(timeToSteadyState).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros(),
            peakTroughRatio = BigDecimal(ratio).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros(),
            curvePoints = points
        )
    }
}
