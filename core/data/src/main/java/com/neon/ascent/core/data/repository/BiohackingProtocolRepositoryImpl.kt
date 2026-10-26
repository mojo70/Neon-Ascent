package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.datastore.BiohackingProtocolPreferencesDataStore
import com.neon.ascent.core.data.local.dao.BiohackingProtocolDao
import com.neon.ascent.core.data.local.entity.toDomain
import com.neon.ascent.core.data.local.entity.toEntity
import com.neon.ascent.core.domain.labs.models.BiohackingProtocol
import com.neon.ascent.core.domain.labs.models.BiohackingProtocolOverride
import com.neon.ascent.core.domain.labs.models.SEED_BIOHACKING_PROTOCOLS
import com.neon.ascent.core.domain.repository.BiohackingProtocolRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiohackingProtocolRepositoryImpl @Inject constructor(
    private val dao: BiohackingProtocolDao,
    private val preferencesDataStore: BiohackingProtocolPreferencesDataStore
) : BiohackingProtocolRepository {

    override fun observeAll(): Flow<List<BiohackingProtocol>> {
        return dao.getAllProtocols().map { entities ->
            if (entities.isEmpty()) {
                SEED_BIOHACKING_PROTOCOLS
            } else {
                entities.map { it.toDomain() }
            }
        }
    }

    override fun observeById(id: String): Flow<BiohackingProtocol?> {
        return dao.getProtocolById(id).map { entity ->
            entity?.toDomain() ?: SEED_BIOHACKING_PROTOCOLS.firstOrNull { it.id == id }
        }
    }

    override suspend fun getById(id: String): BiohackingProtocol? {
        val entity = dao.getProtocolByIdSync(id)
        return entity?.toDomain() ?: SEED_BIOHACKING_PROTOCOLS.firstOrNull { it.id == id }
    }

    override suspend fun seedDefaultProtocols() {
        if (dao.getProtocolCount() > 0) return
        dao.insertProtocols(SEED_BIOHACKING_PROTOCOLS.map { it.toEntity() })
    }

    override fun observeActiveProtocols(): Flow<List<BiohackingProtocol>> {
        return combine(observeAll(), preferencesDataStore.activeProtocolIds) { protocols, activeIds ->
            if (activeIds.isEmpty()) {
                emptyList()
            } else {
                protocols.filter { it.id in activeIds }
            }
        }
    }

    override fun observeActiveIds(): Flow<Set<String>> {
        return preferencesDataStore.activeProtocolIds
    }

    override suspend fun setActiveProtocols(ids: Set<String>) {
        preferencesDataStore.setActiveProtocolIds(ids)
    }

    override fun observeOverrides(): Flow<Map<String, BiohackingProtocolOverride>> {
        return preferencesDataStore.overrides
    }

    override suspend fun updateOverride(id: String, override: BiohackingProtocolOverride?) {
        preferencesDataStore.updateOverride(id, override)
    }

    override suspend fun updateAllOverrides(overrides: Map<String, BiohackingProtocolOverride>) {
        preferencesDataStore.updateAllOverrides(overrides)
    }
}
