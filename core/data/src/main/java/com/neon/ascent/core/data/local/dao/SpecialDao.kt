package com.neon.ascent.core.data.local.dao

import androidx.room.*
import com.neon.ascent.core.data.local.entity.BenchmarkTestEntity
import com.neon.ascent.core.data.local.entity.SpecialAttributeEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface SpecialDao {

    @Query("SELECT * FROM special_attributes")
    fun getAllSpecialAttributes(): Flow<List<SpecialAttributeEntity>>

    @Query("SELECT * FROM special_attributes WHERE type = :type")
    fun getSpecialAttribute(type: String): Flow<SpecialAttributeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSpecialAttribute(entity: SpecialAttributeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBenchmark(test: BenchmarkTestEntity)

    @Query("SELECT * FROM benchmark_tests WHERE attribute = :attribute ORDER BY timestamp DESC")
    fun getBenchmarkHistory(attribute: String): Flow<List<BenchmarkTestEntity>>

    @Query("DELETE FROM benchmark_tests WHERE attribute = :attribute")
    suspend fun deleteBenchmarkHistory(attribute: String)

    @Query("DELETE FROM benchmark_tests WHERE timestamp < :olderThan")
    suspend fun deleteOldBenchmarks(olderThan: Instant)
}
