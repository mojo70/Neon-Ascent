package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.dao.VaultDao
import com.neon.ascent.core.data.mapper.toDomain
import com.neon.ascent.core.data.mapper.toEntity
import com.neon.ascent.core.domain.vault.models.VaultAccount
import com.neon.ascent.core.domain.vault.models.VaultSnapshot
import com.neon.ascent.core.domain.vault.models.VaultWatchlistItem
import com.neon.ascent.core.domain.vault.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepositoryImpl @Inject constructor(
    private val vaultDao: VaultDao
) : VaultRepository {

    override fun getAllAccounts(): Flow<List<VaultAccount>> {
        return vaultDao.getAllAccounts().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun upsertAccount(account: VaultAccount) {
        vaultDao.insertAccount(account.toEntity())
    }

    override suspend fun deleteAccount(account: VaultAccount) {
        vaultDao.deleteAccount(account.toEntity())
    }

    override fun getRecentSnapshots(): Flow<List<VaultSnapshot>> {
        return vaultDao.getRecentSnapshots().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertSnapshot(snapshot: VaultSnapshot) {
        vaultDao.insertSnapshot(snapshot.toEntity())
    }

    override fun getWatchlist(): Flow<List<VaultWatchlistItem>> {
        return vaultDao.getWatchlist().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addToWatchlist(item: VaultWatchlistItem) {
        vaultDao.addToWatchlist(item.toEntity())
    }

    override suspend fun removeFromWatchlist(item: VaultWatchlistItem) {
        vaultDao.removeFromWatchlist(item.toEntity())
    }

    override suspend fun isFollowing(symbol: String): Boolean {
        return vaultDao.isFollowing(symbol)
    }
}
