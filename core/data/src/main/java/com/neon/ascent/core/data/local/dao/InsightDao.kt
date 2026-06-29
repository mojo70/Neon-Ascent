package com.neon.ascent.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neon.ascent.core.data.local.entity.ActionEventEntity
import com.neon.ascent.core.data.local.entity.BiometricEventEntity
import com.neon.ascent.core.data.local.entity.SocraticInsightEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface InsightDao {

    // Biometric Events
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBiometricEvent(event: BiometricEventEntity)

    @Query("SELECT * FROM biometric_events WHERE type = :type ORDER BY timestamp DESC")
    fun getBiometricEventsByType(type: String): Flow<List<BiometricEventEntity>>

    @Query("SELECT * FROM biometric_events WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getBiometricEventsInRange(start: Instant, end: Instant): Flow<List<BiometricEventEntity>>

    // Action Events
    @Insert
    suspend fun insertActionEvent(event: ActionEventEntity)

    @Query("SELECT * FROM action_events ORDER BY timestamp DESC")
    fun getAllActionEvents(): Flow<List<ActionEventEntity>>

    // Socratic Insights
    @Insert
    suspend fun insertInsight(insight: SocraticInsightEntity)

    @Query("SELECT * FROM socratic_insights ORDER BY generatedAt DESC LIMIT 1")
    fun getLatestInsight(): Flow<SocraticInsightEntity?>

    @Query("SELECT * FROM socratic_insights WHERE version = :version ORDER BY generatedAt DESC")
    fun getInsightsByVersion(version: Int): Flow<List<SocraticInsightEntity>>
}
