package com.neon.ascent.core.domain.special.usecases

import com.neon.ascent.core.domain.SpecialRepository
import com.neon.ascent.core.domain.health.HealthManager
import com.neon.ascent.core.domain.model.BenchmarkTest
import com.neon.ascent.core.domain.model.SpecialAttribute
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.core.domain.special.HealthDataProcessor
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

/**
 * Processes Health Connect data → grounded S.P.E.C.I.A.L. updates.
 * Privacy-first: All reads are on-device, user-controlled permissions.
 * Prevents double-counting by using pre-aggregated totals.
 */
class UpdateSpecialFromHealthUseCase @Inject constructor(
    private val healthManager: HealthManager,
    private val specialRepository: SpecialRepository,
    private val processor: HealthDataProcessor
) {

    /**
     * Call this from a WorkManager worker (daily/periodic) or after user triggers "Sync Diagnostics".
     * Time window: last 24h or 48h.
     */
    suspend operator fun invoke(startTime: Instant = Instant.now().minusSeconds(86400)): List<SpecialAttribute> {
        if (!healthManager.isAvailableAndHasPermissions()) return emptyList()
        
        val updatedAttributes = mutableListOf<SpecialAttribute>()
        val now = Instant.now()

        // 1. Fetch Aggregates (Truth)
        val totalSteps = healthManager.aggregateSteps(startTime, now)
        
        // Calories: Check Total first, fallback to Active. Aggregated to prevent double-counting.
        var totalCalories = healthManager.aggregateTotalCaloriesKcal(startTime, now)
        if (totalCalories <= 0.0) {
            totalCalories = healthManager.aggregateActiveCaloriesKcal(startTime, now)
        }

        // Sleep: Caller picks longest overnight, does not sum overlapping.
        val sleepSessions = healthManager.sleepSessions(startTime, now)
        val longestSleepMinutes = sleepSessions.maxOfOrNull { 
            java.time.Duration.between(it.startTime, it.endTime).toMinutes() 
        } ?: 0L
        
        // HRV: Latest RMSSD in window.
        val avgHrv = healthManager.latestHrvRmssd(startTime, now) ?: 0.0

        // 2. Process into grounded benchmarks
        val agilityBenchmark = processor.processSteps(totalSteps)
        val enduranceBenchmark = processor.processSleepAndHRV(longestSleepMinutes, avgHrv)
        val strengthBenchmark = processor.processStrength(totalCalories)

        // 3. Save raw benchmarks for Diagnostics history
        agilityBenchmark?.let { specialRepository.saveBenchmark(it) }
        enduranceBenchmark?.let { specialRepository.saveBenchmark(it) }
        strengthBenchmark?.let { specialRepository.saveBenchmark(it) }

        // 4. Update S.P.E.C.I.A.L. attributes
        agilityBenchmark?.let {
            val updated = updateAttribute(SpecialType.AGILITY, it)
            updatedAttributes.add(updated)
        }

        enduranceBenchmark?.let {
            val updated = updateAttribute(SpecialType.ENDURANCE, it)
            updatedAttributes.add(updated)
        }

        strengthBenchmark?.let {
            val updated = updateAttribute(SpecialType.STRENGTH, it)
            updatedAttributes.add(updated)
        }

        return updatedAttributes
    }

    private suspend fun updateAttribute(
        type: SpecialType,
        benchmark: BenchmarkTest
    ): SpecialAttribute {
        val current = specialRepository.getSpecialAttribute(type).first()
            ?: SpecialAttribute(type = type, currentValue = 5, percentile = 50)

        val xpGained = calculateXpGain(benchmark.percentile ?: 50, current.percentile)
        val newValue = calculateNewAttributeValue(current.currentValue, benchmark.percentile ?: 50)

        val updated = current.copy(
            currentValue = newValue,
            percentile = benchmark.percentile,
            totalXp = current.totalXp + xpGained,
            lastUpdated = Instant.now()
        )

        specialRepository.updateSpecialAttribute(updated)
        return updated
    }

    private fun calculateXpGain(newPercentile: Int, oldPercentile: Int?): Long {
        val delta = (newPercentile - (oldPercentile ?: 50)).coerceAtLeast(0)
        return (delta * 10L) + 20L
    }

    private fun calculateNewAttributeValue(current: Int, newPercentile: Int): Int {
        val target = when {
            newPercentile >= 90 -> 10
            newPercentile >= 75 -> 9
            newPercentile >= 60 -> 8
            newPercentile >= 45 -> 7
            newPercentile >= 30 -> 6
            else -> 5
        }
        return (current + (target - current) / 3).coerceIn(1, 10)
    }
}
