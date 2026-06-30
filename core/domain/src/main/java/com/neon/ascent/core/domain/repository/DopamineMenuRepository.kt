package com.neon.ascent.core.domain.repository

import com.neon.ascent.core.domain.model.DopamineMenuItem
import com.neon.ascent.core.domain.model.EnergyLevel
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface DopamineMenuRepository {
    fun getAllItems(): Flow<List<DopamineMenuItem>>
    fun getItemsByEnergyLevel(energyLevel: EnergyLevel): Flow<List<DopamineMenuItem>>
    suspend fun getItemById(id: String): DopamineMenuItem?
    suspend fun upsertItem(item: DopamineMenuItem)
    suspend fun deleteItem(item: DopamineMenuItem)
    suspend fun logUsage(id: String, timestamp: Instant)
}
