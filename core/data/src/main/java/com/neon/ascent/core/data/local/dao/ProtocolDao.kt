package com.neon.ascent.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neon.ascent.core.data.local.entity.AdaptedProtocolEntity
import com.neon.ascent.core.data.local.entity.ProtocolEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProtocolDao {
    @Query("SELECT * FROM protocols")
    fun getAllProtocols(): Flow<List<ProtocolEntity>>

    @Query("SELECT * FROM protocols WHERE category = :category")
    fun getProtocolsByCategory(category: String): Flow<List<ProtocolEntity>>

    @Query("SELECT * FROM protocols WHERE specialTags LIKE '%' || :tag || '%'")
    fun getProtocolsByTag(tag: String): Flow<List<ProtocolEntity>>

    @Query("SELECT * FROM protocols WHERE id = :id")
    suspend fun getProtocolById(id: String): ProtocolEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProtocol(protocol: ProtocolEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdaptedProtocol(adaptedProtocol: AdaptedProtocolEntity)

    @Query("SELECT * FROM adapted_protocols WHERE directiveId = :directiveId")
    fun getAdaptedProtocolsForDirective(directiveId: String): Flow<List<AdaptedProtocolEntity>>

    @Query("SELECT COUNT(*) FROM protocols")
    suspend fun getProtocolCount(): Int

    @Query("DELETE FROM protocols WHERE id = :id AND isCanonical = 0")
    suspend fun deleteUserProtocol(id: String)
}
