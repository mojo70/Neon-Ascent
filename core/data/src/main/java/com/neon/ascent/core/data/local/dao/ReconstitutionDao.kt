package com.neon.ascent.core.data.local.dao

import androidx.room.*
import com.neon.ascent.core.data.local.entity.SavedReconstitutionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReconstitutionDao {

    @Query("SELECT * FROM saved_reconstitutions ORDER BY createdAt DESC")
    fun getAllSavedMixes(): Flow<List<SavedReconstitutionEntity>>

    @Query("SELECT * FROM saved_reconstitutions WHERE id = :id")
    suspend fun getSavedMixById(id: String): SavedReconstitutionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedMix(mix: SavedReconstitutionEntity)

    @Query("DELETE FROM saved_reconstitutions WHERE id = :id")
    suspend fun deleteSavedMix(id: String)
}
