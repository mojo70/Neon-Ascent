package com.neon.ascent.data.local

import androidx.room.*
import com.neon.ascent.model.BioProtocolLog
import com.neon.ascent.model.BiohackingData
import kotlinx.coroutines.flow.Flow

@Dao
interface BiohackingDao {
    @Query("SELECT * FROM biohacking_data WHERE userId = :userId")
    fun getBiohackingData(userId: Int): Flow<BiohackingData?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(data: BiohackingData)

    @Insert
    suspend fun insertProtocolLog(log: BioProtocolLog)

    @Query("SELECT * FROM bio_protocol_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getProtocolLogs(userId: Int): Flow<List<BioProtocolLog>>

    @Query("SELECT * FROM bio_protocol_logs ORDER BY timestamp DESC")
    fun getAllProtocolLogs(): Flow<List<BioProtocolLog>>
}
