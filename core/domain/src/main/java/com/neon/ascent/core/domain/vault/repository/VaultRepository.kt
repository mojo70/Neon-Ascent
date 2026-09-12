package com.neon.ascent.core.domain.vault.repository

import com.neon.ascent.core.domain.vault.models.VaultAccount
import com.neon.ascent.core.domain.vault.models.VaultSnapshot
import com.neon.ascent.core.domain.vault.models.VaultWatchlistItem
import kotlinx.coroutines.flow.Flow

interface VaultRepository {
    fun getAllAccounts(): Flow<List<VaultAccount>>
    suspend fun upsertAccount(account: VaultAccount)
    suspend fun deleteAccount(account: VaultAccount)

    fun getRecentSnapshots(): Flow<List<VaultSnapshot>>
    suspend fun insertSnapshot(snapshot: VaultSnapshot)

    fun getWatchlist(): Flow<List<VaultWatchlistItem>>
    suspend fun addToWatchlist(item: VaultWatchlistItem)
    suspend fun removeFromWatchlist(item: VaultWatchlistItem)
    suspend fun isFollowing(symbol: String): Boolean
}
