package com.neon.ascent.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neon.ascent.core.data.local.entity.RiteSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RiteSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: RiteSessionEntity)

    @Query("SELECT * FROM rite_sessions WHERE localDate = :date")
    fun getSessionsForDate(date: String): Flow<List<RiteSessionEntity>>

    @Query("SELECT * FROM rite_sessions WHERE localDate = :date AND kind = :kind")
    suspend fun getSessionsForDateAndKind(date: String, kind: String): List<RiteSessionEntity>

    @Query("SELECT * FROM rite_sessions WHERE kind = :kind ORDER BY startedAt DESC")
    fun getSessionsForKind(kind: String): Flow<List<RiteSessionEntity>>

    @Query("SELECT * FROM rite_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<RiteSessionEntity>>

    @Query("SELECT * FROM rite_sessions WHERE localDate >= :fromDate ORDER BY startedAt DESC")
    fun getSessionsFromDate(fromDate: String): Flow<List<RiteSessionEntity>>

    @Query("DELETE FROM rite_sessions WHERE id = :id")
    suspend fun deleteSession(id: String)
}
