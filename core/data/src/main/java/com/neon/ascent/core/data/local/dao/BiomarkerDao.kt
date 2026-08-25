package com.neon.ascent.core.data.local.dao

import androidx.room.*
import com.neon.ascent.core.data.local.entity.BiomarkerSampleEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface BiomarkerDao {
    @Upsert
    suspend fun upsertSample(sample: BiomarkerSampleEntity)

    @Query("DELETE FROM biomarker_samples WHERE id = :id")
    suspend fun deleteSample(id: String)

    @Query("SELECT * FROM biomarker_samples WHERE markerKey = :markerKey ORDER BY drawnAt DESC")
    fun getSamplesForMarker(markerKey: String): Flow<List<BiomarkerSampleEntity>>

    @Query("SELECT * FROM biomarker_samples WHERE drawnAt BETWEEN :from AND :to ORDER BY drawnAt DESC")
    fun getAllSamplesBetween(from: Instant, to: Instant): Flow<List<BiomarkerSampleEntity>>

    @Query("""
        SELECT * FROM biomarker_samples t1
        WHERE drawnAt = (SELECT MAX(drawnAt) FROM biomarker_samples t2 WHERE t2.markerKey = t1.markerKey)
        ORDER BY displayName ASC
    """)
    fun getLatestPerMarker(): Flow<List<BiomarkerSampleEntity>>
}
