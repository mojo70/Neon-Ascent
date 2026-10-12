package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.dao.ReconstitutionDao
import com.neon.ascent.core.data.local.entity.SavedReconstitutionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReconstitutionRepository @Inject constructor(
    private val reconstitutionDao: ReconstitutionDao
) {
    val savedMixes: Flow<List<SavedReconstitutionEntity>> = reconstitutionDao.getAllSavedMixes()

    suspend fun saveMix(mix: SavedReconstitutionEntity) {
        reconstitutionDao.insertSavedMix(mix)
    }

    suspend fun deleteMix(id: String) {
        reconstitutionDao.deleteSavedMix(id)
    }
}
