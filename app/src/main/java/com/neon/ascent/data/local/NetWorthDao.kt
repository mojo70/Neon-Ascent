package com.neon.ascent.data.local

import androidx.room.*
import com.neon.ascent.model.AssetAccount
import com.neon.ascent.model.AssetSnapshot
import kotlinx.coroutines.flow.Flow

@Dao
interface NetWorthDao {
    @Query("SELECT * FROM asset_accounts")
    fun getAllAccounts(): Flow<List<AssetAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AssetAccount)

    @Delete
    suspend fun deleteAccount(account: AssetAccount)

    @Query("SELECT * FROM asset_snapshots ORDER BY timestamp DESC LIMIT 30")
    fun getRecentSnapshots(): Flow<List<AssetSnapshot>>

    @Insert
    suspend fun insertSnapshot(snapshot: AssetSnapshot)
}
