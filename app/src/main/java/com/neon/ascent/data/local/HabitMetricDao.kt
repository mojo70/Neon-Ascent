package com.neon.ascent.data.local

import androidx.room.*
import com.neon.ascent.data.local.entity.HabitMetricEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitMetricDao {
    @Upsert
    suspend fun upsert(metric: HabitMetricEntity)

    @Query("""
        SELECT COUNT(DISTINCT date) FROM habit_metrics 
        WHERE completed = 1 AND date >= :startDate
    """)
    fun getCurrentStreakPotential(startDate: String): Flow<Int>

    @Query("SELECT COUNT(DISTINCT date) FROM habit_metrics WHERE completed = 1")
    fun getTotalCompletedDays(): Flow<Int>

    // Completion heatmap data (for nice calendar view)
    @Query("SELECT date, COUNT(*) as completed FROM habit_metrics WHERE completed = 1 GROUP BY date ORDER BY date DESC LIMIT 90")
    fun get90DayHeatmap(): Flow<List<DailyCompletion>>
}

data class DailyCompletion(
    val date: String,
    val completed: Int
)
