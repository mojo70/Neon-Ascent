package com.neon.ascent.data.local

import androidx.room.*
import com.neon.ascent.model.StrengthBenchmark
import kotlinx.coroutines.flow.Flow

@Dao
interface BenchmarkDao {
    @Query("SELECT * FROM strength_benchmarks")
    fun getAllBenchmarks(): Flow<List<StrengthBenchmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBenchmarks(benchmarks: List<StrengthBenchmark>)

    @Query("DELETE FROM strength_benchmarks")
    suspend fun deleteAllBenchmarks()

    @Query("SELECT * FROM strength_benchmarks WHERE sex = :sex AND ageGroup = :ageGroup AND weightClass = :weightClass LIMIT 1")
    suspend fun getBenchmark(sex: String, ageGroup: String, weightClass: String): StrengthBenchmark?
}
