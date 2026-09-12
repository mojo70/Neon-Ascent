package com.neon.ascent.core.domain.vault.models

enum class VaultAssetType {
    CASH, STOCK, CRYPTO, REAL_ESTATE, VEHICLE, CREDIT_CARD, LOAN, OTHER
}

data class VaultAccount(
    val id: String,
    val name: String,
    val institution: String,
    val type: VaultAssetType,
    val balance: Double,
    val currency: String = "USD",
    val isLinked: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class VaultSnapshot(
    val id: Int = 0,
    val timestamp: Long,
    val totalAssets: Double,
    val totalDebt: Double,
    val netWorth: Double
)

data class VaultWatchlistItem(
    val symbol: String,
    val name: String,
    val isCrypto: Boolean = false
)
