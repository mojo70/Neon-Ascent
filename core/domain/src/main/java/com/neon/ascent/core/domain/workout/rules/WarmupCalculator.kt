package com.neon.ascent.core.domain.workout.rules

import com.neon.ascent.core.domain.workout.models.UnitSystem
import kotlin.math.round

object WarmupCalculator {

    /**
     * Calculates two progressive warmup weights based on a target working weight.
     * Weights are rounded to the nearest 5 (Imperial) or 2.5 (Metric).
     */
    fun calculateWarmupWeights(targetWeight: Float, unitSystem: UnitSystem): List<Float> {
        if (targetWeight <= 0) return emptyList()

        val baseRounding = if (unitSystem == UnitSystem.IMPERIAL) 5.0f else 2.5f
        
        val warmup1 = roundWeight(targetWeight * 0.5f, baseRounding)
        val warmup2 = roundWeight(targetWeight * 0.75f, baseRounding)

        // Ensure warmup2 is actually greater than warmup1 if target is high enough
        val finalWarmup2 = if (warmup2 <= warmup1 && targetWeight > baseRounding * 2) {
            warmup1 + baseRounding
        } else {
            warmup2
        }

        return listOf(warmup1, finalWarmup2).filter { it > 0 && it < targetWeight }
    }

    private fun roundWeight(weight: Float, base: Float): Float {
        return round(weight / base) * base
    }
}
