package com.neon.ascent.data.local

import androidx.room.*
import com.neon.ascent.model.CorpoTrust
import com.neon.ascent.model.DataShard
import com.neon.ascent.model.MemoryFragment
import kotlinx.coroutines.flow.Flow

@Dao
interface LoreDao {
    @Query("SELECT * FROM corpo_trust")
    fun getAllCorpoTrust(): Flow<List<CorpoTrust>>

    @Query("SELECT * FROM corpo_trust WHERE corpoId = :corpoId")
    fun getCorpoTrust(corpoId: String): Flow<CorpoTrust?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorpoTrust(trust: CorpoTrust)

    @Query("UPDATE corpo_trust SET trustLevel = :level WHERE corpoId = :corpoId")
    suspend fun updateTrustLevel(corpoId: String, level: Float)

    @Query("SELECT * FROM data_shards ORDER BY droppedAt DESC")
    fun getAllDataShards(): Flow<List<DataShard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDataShard(shard: DataShard)

    @Update
    suspend fun updateDataShard(shard: DataShard)

    @Query("UPDATE data_shards SET isDecrypted = :isDecrypted WHERE id = :shardId")
    suspend fun updateShardDecrypted(shardId: String, isDecrypted: Boolean)

    @Query("SELECT * FROM memory_fragments")
    fun getAllMemoryFragments(): Flow<List<MemoryFragment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoryFragment(fragment: MemoryFragment)

    @Update
    suspend fun updateMemoryFragment(fragment: MemoryFragment)
}
