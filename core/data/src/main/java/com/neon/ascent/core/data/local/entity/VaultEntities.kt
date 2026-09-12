package com.neon.ascent.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.neon.ascent.core.domain.vault.models.VaultAssetType

@Entity(tableName = "vault_accounts")
data class VaultAccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val institution: String,
    val type: VaultAssetType,
    val balance: Double,
    val currency: String = "USD",
    val isLinked: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_snapshots")
data class VaultSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val totalAssets: Double,
    val totalDebt: Double,
    val netWorth: Double
)

@Entity(tableName = "vault_watchlist")
data class VaultWatchlistItemEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val isCrypto: Boolean = false
)
