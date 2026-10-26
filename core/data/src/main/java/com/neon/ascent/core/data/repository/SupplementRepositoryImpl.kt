package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.datastore.SupplementPreferencesDataStore
import com.neon.ascent.core.data.local.dao.SupplementDao
import com.neon.ascent.core.data.local.entity.toDomain
import com.neon.ascent.core.data.local.entity.toEntity
import com.neon.ascent.core.domain.labs.models.SEED_SUPPLEMENTS
import com.neon.ascent.core.domain.labs.models.Supplement
import com.neon.ascent.core.domain.labs.models.SupplementOverride
import com.neon.ascent.core.domain.repository.SupplementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupplementRepositoryImpl @Inject constructor(
    private val supplementDao: SupplementDao,
    private val preferencesDataStore: SupplementPreferencesDataStore
) : SupplementRepository {

    override fun observeAll(): Flow<List<Supplement>> {
        return supplementDao.getAllSupplements().map { entities ->
            if (entities.isEmpty()) {
                SEED_SUPPLEMENTS
            } else {
                entities.map { it.toDomain() }
            }
        }
    }

    override fun observeById(id: String): Flow<Supplement?> {
        return supplementDao.getSupplementById(id).map { entity ->
            entity?.toDomain() ?: SEED_SUPPLEMENTS.firstOrNull { it.id == id }
        }
    }

    override suspend fun getById(id: String): Supplement? {
        val entity = supplementDao.getSupplementByIdSync(id)
        return entity?.toDomain() ?: SEED_SUPPLEMENTS.firstOrNull { it.id == id }
    }

    override suspend fun seedDefaultSupplements() {
        if (supplementDao.getSupplementCount() > 0) return
        supplementDao.insertSupplements(SEED_SUPPLEMENTS.map { it.toEntity() })
    }

    override fun observeActiveStack(): Flow<List<Supplement>> {
        return combine(observeAll(), preferencesDataStore.activeStackIds) { supplements, activeIds ->
            if (activeIds.isEmpty()) {
                emptyList()
            } else {
                supplements.filter { it.id in activeIds }
            }
        }
    }

    override suspend fun setActiveStack(ids: Set<String>) {
        preferencesDataStore.setActiveStackIds(ids)
    }

    override fun observeOverrides(): Flow<Map<String, SupplementOverride>> {
        return preferencesDataStore.overrides
    }

    override suspend fun updateOverride(id: String, override: SupplementOverride?) {
        preferencesDataStore.updateOverride(id, override)
    }

    override suspend fun updateAllOverrides(overrides: Map<String, SupplementOverride>) {
        preferencesDataStore.updateAllOverrides(overrides)
    }
}
