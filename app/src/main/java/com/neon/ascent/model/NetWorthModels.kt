package com.neon.ascent.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class AssetType {
    CASH, STOCK, CRYPTO, REAL_ESTATE, VEHICLE, CREDIT_CARD, LOAN, OTHER
}

@Serializable
@Entity(tableName = "asset_accounts")
data class AssetAccount(
    @PrimaryKey val id: String,
    val name: String,
    val institution: String, // e.g., "Robinhood", "Chase", "Coinbase"
    val type: AssetType,
    val balance: Double,
    val currency: String = "USD",
    val isLinked: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "asset_snapshots")
data class AssetSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val totalAssets: Double,
    val totalDebt: Double,
    val netWorth: Double
)

@Serializable
data class NetWorthSummary(
    val totalValue: Double,
    val changePercentage: Double,
    val isUp: Boolean,
    val liquidNetWorth: Double,
    val totalDebt: Double,
    val totalExpenses: Double
)
