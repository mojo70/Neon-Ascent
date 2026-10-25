package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.datastore.DietPreferencesDataStore
import com.neon.ascent.core.data.local.dao.DietDao
import com.neon.ascent.core.data.local.entity.toDomain
import com.neon.ascent.core.data.local.entity.toEntity
import com.neon.ascent.core.domain.labs.models.Diet
import com.neon.ascent.core.domain.labs.models.DietOverrides
import com.neon.ascent.core.domain.labs.models.SEED_DIETS
import com.neon.ascent.core.domain.repository.DietRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DietRepositoryImpl @Inject constructor(
    private val dietDao: DietDao,
    private val preferencesDataStore: DietPreferencesDataStore
) : DietRepository {

    override fun observeAll(): Flow<List<Diet>> {
        return dietDao.getAllDiets().map { entities ->
            if (entities.isEmpty()) {
                SEED_DIETS
            } else {
                entities.map { it.toDomain() }
            }
        }
    }

    override fun observeById(id: String): Flow<Diet?> {
        return dietDao.getDietById(id).map { entity ->
            entity?.toDomain() ?: SEED_DIETS.firstOrNull { it.id == id }
        }
    }

    override suspend fun getById(id: String): Diet? {
        val entity = dietDao.getDietByIdSync(id)
        return entity?.toDomain() ?: SEED_DIETS.firstOrNull { it.id == id }
    }

    override suspend fun seedDefaultDiets() {
        if (dietDao.getDietCount() > 0) return
        dietDao.insertDiets(SEED_DIETS.map { it.toEntity() })
    }

    override fun observeActiveDiet(): Flow<Diet?> {
        return combine(observeAll(), preferencesDataStore.activeDietId) { diets, activeId ->
            if (activeId.isNullOrBlank()) null
            else diets.firstOrNull { it.id == activeId }
        }
    }

    override suspend fun setActiveDiet(id: String?) {
        preferencesDataStore.setActiveDietId(id)
    }

    override fun observeOverrides(): Flow<DietOverrides> {
        return preferencesDataStore.dietOverrides
    }

    override suspend fun updateOverrides(overrides: DietOverrides) {
        preferencesDataStore.updateDietOverrides(overrides)
    }
}
