package com.neon.ascent.core.data.local.dao

import androidx.room.*
import com.neon.ascent.core.data.local.entity.DailyVitalRollupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyVitalRollupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rollup: DailyVitalRollupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rollups: List<DailyVitalRollupEntity>)

    @Query("SELECT * FROM daily_vital_rollups WHERE metric = :metric AND localDate BETWEEN :fromDate AND :toDate ORDER BY localDate ASC")
    fun getRange(metric: String, fromDate: String, toDate: String): Flow<List<DailyVitalRollupEntity>>

    @Query("SELECT * FROM daily_vital_rollups WHERE localDate = :localDate")
    fun getDay(localDate: String): Flow<List<DailyVitalRollupEntity>>
}
