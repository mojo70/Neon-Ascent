package com.neon.ascent.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neon.ascent.core.data.local.entity.VaultAccountEntity
import com.neon.ascent.core.data.local.entity.VaultSnapshotEntity
import com.neon.ascent.core.data.local.entity.VaultWatchlistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    // Accounts
    @Query("SELECT * FROM vault_accounts")
    fun getAllAccounts(): Flow<List<VaultAccountEntity>>

    @Query("SELECT * FROM vault_accounts")
    suspend fun getAllAccountsOnce(): List<VaultAccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: VaultAccountEntity)

    @Delete
    suspend fun deleteAccount(account: VaultAccountEntity)

    // Snapshots
    @Query("SELECT * FROM vault_snapshots ORDER BY timestamp DESC LIMIT 30")
    fun getRecentSnapshots(): Flow<List<VaultSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: VaultSnapshotEntity)

    // Watchlist
    @Query("SELECT * FROM vault_watchlist")
    fun getWatchlist(): Flow<List<VaultWatchlistItemEntity>>

    @Query("SELECT * FROM vault_watchlist")
    suspend fun getWatchlistOnce(): List<VaultWatchlistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlist(item: VaultWatchlistItemEntity)

    @Delete
    suspend fun removeFromWatchlist(item: VaultWatchlistItemEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM vault_watchlist WHERE symbol = :symbol)")
    suspend fun isFollowing(symbol: String): Boolean
}
