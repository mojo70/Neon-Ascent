package com.neon.ascent.data.local

import androidx.room.*
import com.neon.ascent.model.DailyPrayer
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyPrayerDao {
    @Query("SELECT * FROM daily_prayers WHERE day = :day")
    suspend fun getPrayerForDay(day: Int): DailyPrayer?

    @Query("SELECT * FROM daily_prayers")
    fun getAllPrayers(): Flow<List<DailyPrayer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayers(prayers: List<DailyPrayer>)
}
