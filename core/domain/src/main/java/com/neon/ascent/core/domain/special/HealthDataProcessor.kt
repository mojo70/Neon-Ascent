package com.neon.ascent.core.domain.special

import com.neon.ascent.core.domain.model.BenchmarkTest
import com.neon.ascent.core.domain.model.DataSource
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.core.domain.model.TestType
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class HealthDataProcessor @Inject constructor() {

    /** 
     * Process pre-aggregated steps today.
     * Never sum raw records for this value to avoid double-counting.
     */
    fun processSteps(totalSteps: Long): BenchmarkTest? {
        if (totalSteps <= 0) return null

        // CDC / ACS guidelines + population norms
        val dailyAvg = totalSteps.toDouble()
        val percentile = when {
            dailyAvg >= 12000 -> 92
            dailyAvg >= 10000 -> 82
            dailyAvg >= 7500  -> 65
            dailyAvg >= 5000  -> 48
            else -> 35
        }

        return BenchmarkTest(
            id = "health_steps_${Instant.now().toEpochMilli()}",
            attribute = SpecialType.AGILITY,
            testType = TestType.WEARABLE_DERIVED,
            rawScore = dailyAvg,
            normalizedScore = dailyAvg / 15000.0,
            percentile = percentile,
            metadata = mapOf("total_steps" to totalSteps.toString()),
            source = DataSource.HEALTH_CONNECT
        )
    }

    /**
     * Process sleep sessions and HRV. 
     * Caller provides the longest overnight session duration.
     * Do not produce a fake 0-100 sleep score for SPECIAL.
     */
    fun processSleepAndHRV(
        sleepMinutes: Long,
        avgHrvRmssd: Double
    ): BenchmarkTest? {
        if (sleepMinutes <= 0 && avgHrvRmssd <= 0) return null

        // Combine sleep duration (WHO/ACS) + HRV (age-adjusted norms)
        // This calculates an attribute percentile, not a "Sleep Score" card.
        val sleepScore = (sleepMinutes / 480.0).coerceAtMost(1.0)   // 8h ideal
        val hrvScore = (avgHrvRmssd / 65.0).coerceAtMost(1.0)      // rough adult median

        val combinedPercentile = ((sleepScore * 0.6 + hrvScore * 0.4) * 100).roundToInt()

        return BenchmarkTest(
            id = "health_endurance_${Instant.now().toEpochMilli()}",
            attribute = SpecialType.ENDURANCE,
            testType = TestType.WEARABLE_DERIVED,
            rawScore = sleepMinutes.toDouble() + avgHrvRmssd,
            normalizedScore = (sleepScore + hrvScore) / 2,
            percentile = combinedPercentile.coerceIn(10, 95),
            metadata = mapOf(
                "sleep_minutes" to sleepMinutes.toString(),
                "avg_hrv_ms" to avgHrvRmssd.toString()
            ),
            source = DataSource.HEALTH_CONNECT
        )
    }

    fun processStrength(totalCalories: Double): BenchmarkTest? {
        if (totalCalories <= 0) return null

        val percentile = when {
            totalCalories >= 2500 -> 90 // Adjusted for Total Calories (Active + Basal)
            totalCalories >= 2000 -> 80
            totalCalories >= 1500 -> 65
            totalCalories >= 1000 -> 45
            else -> 30
        }

        return BenchmarkTest(
            id = "health_strength_${Instant.now().toEpochMilli()}",
            attribute = SpecialType.STRENGTH,
            testType = TestType.WEARABLE_DERIVED,
            rawScore = totalCalories,
            normalizedScore = (totalCalories / 3000.0).coerceAtMost(1.0),
            percentile = percentile,
            metadata = mapOf("total_calories" to totalCalories.toString()),
            source = DataSource.HEALTH_CONNECT
        )
    }
}
