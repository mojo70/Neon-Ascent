package com.neon.ascent.core.domain

import com.neon.ascent.core.domain.model.BenchmarkTest
import com.neon.ascent.core.domain.model.SpecialAttribute
import com.neon.ascent.core.domain.model.SpecialType
import kotlinx.coroutines.flow.Flow

interface SpecialRepository {
    suspend fun updateSpecialAttribute(attribute: SpecialAttribute)
    fun getSpecialAttribute(type: SpecialType): Flow<SpecialAttribute?>
    fun getAllSpecialAttributes(): Flow<List<SpecialAttribute>>
    suspend fun saveBenchmark(test: BenchmarkTest)
    fun getBenchmarkHistory(attribute: SpecialType): Flow<List<BenchmarkTest>>
}

/**
 * Health Connect Tie-in Plan:
 * 1. Implement HealthConnectManager to read data (steps, heart rate, sleep).
 * 2. Create SyncHealthDataUseCase to map raw metrics to S.P.E.C.I.A.L. attributes.
 *    - Steps/Activity -> STRENGTH/ENDURANCE
 *    - Sleep Quality -> PERCEPTION/INTELLIGENCE (recovery)
 * 3. Schedule periodic sync via WorkManager.
 * 4. Update SpecialRepository with new BenchmarkTests (Source: HEALTH_CONNECT).
 */
