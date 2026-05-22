package com.neon.ascent.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neon.ascent.core.data.local.entity.NeuralMemory
import kotlinx.coroutines.flow.Flow

@Dao
interface NeuralMemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: NeuralMemory)

    @Query("SELECT * FROM neural_memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<NeuralMemory>>

    @Query("SELECT * FROM neural_memories WHERE wing = :wing ORDER BY timestamp DESC")
    fun getMemoriesByWing(wing: String): Flow<List<NeuralMemory>>

    @Query("SELECT * FROM neural_memories WHERE wing = :wing AND room = :room ORDER BY timestamp DESC")
    fun getMemoriesByRoom(wing: String, room: String): Flow<List<NeuralMemory>>

    @Query("SELECT * FROM neural_memories WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun searchMemories(query: String, limit: Int = 10): List<NeuralMemory>

    @Query("DELETE FROM neural_memories WHERE timestamp < :threshold")
    suspend fun pruneOldMemories(threshold: Long)
}
