package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.dao.DopamineMenuDao
import com.neon.ascent.core.data.local.entity.toDomain
import com.neon.ascent.core.data.local.entity.toEntity
import com.neon.ascent.core.domain.model.DopamineMenuItem
import com.neon.ascent.core.domain.model.EnergyLevel
import com.neon.ascent.core.domain.repository.DopamineMenuRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DopamineMenuRepositoryImpl @Inject constructor(
    private val dopamineMenuDao: DopamineMenuDao
) : DopamineMenuRepository {

    override fun getAllItems(): Flow<List<DopamineMenuItem>> {
        return dopamineMenuDao.getAllItems().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getItemsByEnergyLevel(energyLevel: EnergyLevel): Flow<List<DopamineMenuItem>> {
        return dopamineMenuDao.getItemsByEnergyLevel(energyLevel).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getItemById(id: String): DopamineMenuItem? {
        return dopamineMenuDao.getItemById(id)?.toDomain()
    }

    override suspend fun upsertItem(item: DopamineMenuItem) {
        dopamineMenuDao.upsertItem(item.toEntity())
    }

    override suspend fun deleteItem(item: DopamineMenuItem) {
        dopamineMenuDao.deleteItem(item.toEntity())
    }

    override suspend fun logUsage(id: String, timestamp: Instant) {
        dopamineMenuDao.logUsage(id, timestamp)
    }
}
