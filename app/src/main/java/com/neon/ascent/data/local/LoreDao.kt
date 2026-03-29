package com.neon.ascent.data.local

import androidx.room.*
import com.neon.ascent.model.DataShard
import com.neon.ascent.model.MemoryFragment
import kotlinx.coroutines.flow.Flow

@Dao
interface LoreDao {
    @Query("SELECT * FROM data_shards ORDER BY droppedAt DESC")
    fun getAllDataShards(): Flow<List<DataShard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDataShard(shard: DataShard)

    @Update
    suspend fun updateDataShard(shard: DataShard)

    @Query("SELECT * FROM memory_fragments")
    fun getAllMemoryFragments(): Flow<List<MemoryFragment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoryFragment(fragment: MemoryFragment)

    @Update
    suspend fun updateMemoryFragment(fragment: MemoryFragment)
}
