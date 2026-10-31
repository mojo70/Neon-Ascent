package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.dao.SanctuaryDao
import com.neon.ascent.core.data.local.entity.toDomain
import com.neon.ascent.core.data.local.seed.SanctuarySeeds
import com.neon.ascent.core.domain.sanctuary.models.SanctuaryPassage
import com.neon.ascent.core.domain.sanctuary.repository.SanctuaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SanctuaryRepositoryImpl @Inject constructor(
    private val sanctuaryDao: SanctuaryDao
) : SanctuaryRepository {

    override fun observeAllPassages(): Flow<List<SanctuaryPassage>> {
        return sanctuaryDao.getAllPassages().map { entities ->
            if (entities.isEmpty()) {
                SanctuarySeeds.SANCTUARY_SEED.map { it.toDomain() }
            } else {
                entities.map { it.toDomain() }
            }
        }
    }

    override fun observePassagesByTheme(theme: String): Flow<List<SanctuaryPassage>> {
        return sanctuaryDao.getPassagesByTheme(theme).map { entities ->
            if (entities.isEmpty()) {
                SanctuarySeeds.SANCTUARY_SEED
                    .filter { passage -> passage.themes.any { it.equals(theme, ignoreCase = true) } }
                    .map { it.toDomain() }
            } else {
                entities.map { it.toDomain() }
            }
        }
    }

    override fun observePassagesByTag(tag: String): Flow<List<SanctuaryPassage>> {
        return sanctuaryDao.getPassagesByTag(tag).map { entities ->
            if (entities.isEmpty()) {
                SanctuarySeeds.SANCTUARY_SEED
                    .filter { passage -> passage.tags.any { it.equals(tag, ignoreCase = true) } }
                    .map { it.toDomain() }
            } else {
                entities.map { it.toDomain() }
            }
        }
    }

    override suspend fun getPassageById(id: String): SanctuaryPassage? {
        val entity = sanctuaryDao.getPassageById(id)
        return entity?.toDomain() ?: SanctuarySeeds.SANCTUARY_SEED.firstOrNull { it.id == id }?.toDomain()
    }

    override suspend fun seedDefaultPassagesIfAbsent() {
        sanctuaryDao.insertPassagesIfAbsent(SanctuarySeeds.SANCTUARY_SEED)
    }
}
