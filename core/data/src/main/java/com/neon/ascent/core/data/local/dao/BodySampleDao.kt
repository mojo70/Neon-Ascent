package com.neon.ascent.core.data.local.dao

import androidx.room.*
import com.neon.ascent.core.data.local.entity.BodySampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BodySampleDao {
    @Upsert
    suspend fun upsertSample(sample: BodySampleEntity)

    @Upsert
    suspend fun upsertSamples(samples: List<BodySampleEntity>)

    @Query("DELETE FROM body_samples WHERE id = :id")
    suspend fun deleteSample(id: String)

    @Query("SELECT * FROM body_samples WHERE metric = :metric AND loggedAt BETWEEN :fromMillis AND :toMillis ORDER BY loggedAt DESC")
    fun getSamplesForMetric(metric: String, fromMillis: Long, toMillis: Long): Flow<List<BodySampleEntity>>

    @Query("SELECT * FROM body_samples WHERE metric = :metric AND localDate BETWEEN :fromDate AND :toDate ORDER BY loggedAt DESC")
    suspend fun getSamplesForMetricRange(metric: String, fromDate: String, toDate: String): List<BodySampleEntity>

    @Query("SELECT * FROM body_samples WHERE metric = :metric AND localDate BETWEEN :fromDate AND :toDate ORDER BY loggedAt DESC")
    fun getSamplesForMetricRangeFlow(metric: String, fromDate: String, toDate: String): Flow<List<BodySampleEntity>>

    @Query("SELECT * FROM body_samples WHERE localDate = :localDate ORDER BY loggedAt DESC")
    fun getSamplesForDay(localDate: String): Flow<List<BodySampleEntity>>

    @Query("SELECT * FROM body_samples WHERE localDate = :localDate ORDER BY loggedAt DESC")
    suspend fun getSamplesForDayList(localDate: String): List<BodySampleEntity>

    @Query("SELECT * FROM body_samples WHERE loggedAt BETWEEN :fromMillis AND :toMillis ORDER BY loggedAt DESC")
    suspend fun getSamplesBetween(fromMillis: Long, toMillis: Long): List<BodySampleEntity>

    @Query("""
        SELECT * FROM body_samples 
        WHERE (:metric IS NULL OR metric = :metric)
          AND (:site IS NULL OR site = :site)
          AND (:method IS NULL OR method = :method)
          AND (:position IS NULL OR position = :position)
          AND (loggedAt BETWEEN :fromMillis AND :toMillis)
        ORDER BY loggedAt DESC
    """)
    fun getFilteredSamples(
        metric: String?,
        site: String?,
        method: String?,
        position: String?,
        fromMillis: Long,
        toMillis: Long
    ): Flow<List<BodySampleEntity>>
}
