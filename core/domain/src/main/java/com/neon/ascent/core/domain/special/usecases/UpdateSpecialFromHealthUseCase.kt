package com.neon.ascent.core.domain.special.usecases

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.neon.ascent.core.domain.SpecialRepository
import com.neon.ascent.core.domain.model.BenchmarkTest
import com.neon.ascent.core.domain.model.SpecialAttribute
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.core.domain.special.HealthDataProcessor
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

/**
 * Processes Health Connect data (Garmin-synced) → grounded S.P.E.C.I.A.L. updates.
 * Privacy-first: All reads are on-device, user-controlled permissions, read-only for MVP.
 */
class UpdateSpecialFromHealthUseCase @Inject constructor(
    private val healthConnectClient: HealthConnectClient,
    private val specialRepository: SpecialRepository,
    private val processor: HealthDataProcessor
) {

    /**
     * Call this from a WorkManager worker (daily/periodic) or after user triggers "Sync Diagnostics".
     * Time window: last 24h or 7 days depending on attribute.
     */
    suspend operator fun invoke(startTime: Instant = Instant.now().minusSeconds(86400)): List<SpecialAttribute> {
        val updatedAttributes = mutableListOf<SpecialAttribute>()

        // 1. Read relevant records
        val stepsData = readSteps(startTime)
        val sleepData = readSleepSessions(startTime)
        val hrvData = readHRV(startTime)
        
        // Calories: Check Total first, fallback to Active
        var totalCalories = try {
            val request = ReadRecordsRequest(
                recordType = TotalCaloriesBurnedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, Instant.now())
            )
            healthConnectClient.readRecords(request).records.sumOf { it.energy.inKilocalories }
        } catch (e: Throwable) { 0.0 }

        if (totalCalories <= 0.0) {
            totalCalories = try {
                val request = ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, Instant.now())
                )
                healthConnectClient.readRecords(request).records.sumOf { it.energy.inKilocalories }
            } catch (e: Throwable) { 0.0 }
        }

        // 2. Process into grounded benchmarks
        val agilityBenchmark = processor.processSteps(stepsData)
        val enduranceBenchmark = processor.processSleepAndHRV(sleepData, hrvData)
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

    private suspend fun readSteps(startTime: Instant): List<StepsRecord> {
        return try {
            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, Instant.now())
            )
            // Use withContext to ensure we're off-thread and catch the SDK-internal validation crash
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                healthConnectClient.readRecords(request).records
                    .filter { it.count > 0 }
            }
        } catch (e: IllegalArgumentException) {
            // Specifically catch the "count must not be less than 1" crash 
            // from the Health Connect SDK when it encountered bad data.
            android.util.Log.e("UpdateSpecial", "SDK validation error reading StepsRecord: ${e.message}")
            emptyList()
        } catch (e: Throwable) {
            android.util.Log.e("UpdateSpecial", "Error reading steps", e)
            emptyList()
        }
    }

    private suspend fun readSleepSessions(startTime: Instant): List<SleepSessionRecord> {
        return try {
            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, Instant.now())
            )
            healthConnectClient.readRecords(request).records
        } catch (e: Throwable) {
            emptyList()
        }
    }

    private suspend fun readHRV(startTime: Instant): List<HeartRateVariabilityRmssdRecord> {
        return try {
            val request = ReadRecordsRequest(
                recordType = HeartRateVariabilityRmssdRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, Instant.now())
            )
            healthConnectClient.readRecords(request).records
        } catch (e: Throwable) {
            emptyList()
        }
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

    // Same grounded formulas as cognitive UseCase for consistency
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
        return (current + (target - current) / 3).coerceIn(1, 10)   // slower ramp for daily data
    }
}
