package com.neon.ascent.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neon.ascent.core.data.local.entity.BiohackingProtocolEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BiohackingProtocolDao {
    @Query("SELECT * FROM biohacking_protocols")
    fun getAllProtocols(): Flow<List<BiohackingProtocolEntity>>

    @Query("SELECT * FROM biohacking_protocols WHERE id = :id")
    fun getProtocolById(id: String): Flow<BiohackingProtocolEntity?>

    @Query("SELECT * FROM biohacking_protocols WHERE id = :id")
    suspend fun getProtocolByIdSync(id: String): BiohackingProtocolEntity?

    @Query("SELECT COUNT(*) FROM biohacking_protocols")
    suspend fun getProtocolCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProtocols(protocols: List<BiohackingProtocolEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProtocol(protocol: BiohackingProtocolEntity)
}
