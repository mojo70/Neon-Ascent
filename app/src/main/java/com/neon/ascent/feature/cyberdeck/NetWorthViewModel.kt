package com.neon.ascent.feature.cyberdeck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.vault.models.VaultAccount
import com.neon.ascent.core.domain.vault.models.VaultAssetType
import com.neon.ascent.core.domain.vault.models.VaultSnapshot
import com.neon.ascent.core.domain.vault.repository.VaultRepository
import com.neon.ascent.data.local.NetWorthDao
import com.neon.ascent.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NetWorthViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val netWorthDao: NetWorthDao
) : ViewModel() {

    init {
        // Idempotent copy-if-absent from NetWorthDao to VaultRepository
        viewModelScope.launch {
            val legacyAccounts = netWorthDao.getAllAccounts().first()
            val currentVaultAccounts = vaultRepository.getAllAccounts().first()
            val existingIds = currentVaultAccounts.map { it.id }.toSet()
            legacyAccounts.forEach { legacy ->
                if (!existingIds.contains(legacy.id)) {
                    vaultRepository.upsertAccount(legacy.toVaultAccount())
                }
            }

            val legacySnapshots = netWorthDao.getRecentSnapshots().first()
            val currentVaultSnapshots = vaultRepository.getRecentSnapshots().first()
            if (currentVaultSnapshots.isEmpty() && legacySnapshots.isNotEmpty()) {
                legacySnapshots.forEach { s ->
                    vaultRepository.insertSnapshot(
                        VaultSnapshot(
                            id = s.id,
                            timestamp = s.timestamp,
                            totalAssets = s.totalAssets,
                            totalDebt = s.totalDebt,
                            netWorth = s.netWorth
                        )
                    )
                }
            }
        }
    }

    // Read path reads directly from VaultRepository
    val accounts: StateFlow<List<AssetAccount>> = vaultRepository.getAllAccounts()
        .map { list -> list.map { it.toAppAssetAccount() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val snapshots: StateFlow<List<AssetSnapshot>> = vaultRepository.getRecentSnapshots()
        .map { list ->
            list.map {
                AssetSnapshot(
                    id = it.id,
                    timestamp = it.timestamp,
                    totalAssets = it.totalAssets,
                    totalDebt = it.totalDebt,
                    netWorth = it.netWorth
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary = accounts.map { list ->
        val totalAssets = list.filter { it.type != AssetType.CREDIT_CARD && it.type != AssetType.LOAN }.sumOf { it.balance }
        val totalDebt = list.filter { it.type == AssetType.CREDIT_CARD || it.type == AssetType.LOAN }.sumOf { it.balance }
        val liquid = list.filter { it.type == AssetType.CASH || it.type == AssetType.STOCK || it.type == AssetType.CRYPTO }.sumOf { it.balance }
        
        NetWorthSummary(
            totalValue = totalAssets - totalDebt,
            changePercentage = 2.4, // Placeholder
            isUp = true,
            liquidNetWorth = liquid,
            totalDebt = totalDebt,
            totalExpenses = 0.0 // Placeholder
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetWorthSummary(0.0, 0.0, true, 0.0, 0.0, 0.0))

    fun addManualAccount(name: String, institution: String, type: AssetType, balance: Double) {
        viewModelScope.launch {
            val accountId = UUID.randomUUID().toString()
            val account = AssetAccount(
                id = accountId,
                name = name,
                institution = institution,
                type = type,
                balance = balance,
                isLinked = false
            )
            // Write to VaultRepository only
            vaultRepository.upsertAccount(account.toVaultAccount())
            saveSnapshot()
        }
    }

    private fun saveSnapshot() {
        viewModelScope.launch {
            val s = summary.value
            val snapshot = AssetSnapshot(
                timestamp = System.currentTimeMillis(),
                totalAssets = s.totalValue + s.totalDebt,
                totalDebt = s.totalDebt,
                netWorth = s.totalValue
            )
            vaultRepository.insertSnapshot(
                VaultSnapshot(
                    timestamp = snapshot.timestamp,
                    totalAssets = snapshot.totalAssets,
                    totalDebt = snapshot.totalDebt,
                    netWorth = snapshot.netWorth
                )
            )
        }
    }

    fun deleteAccount(account: AssetAccount) {
        viewModelScope.launch {
            vaultRepository.deleteAccount(account.toVaultAccount())
            saveSnapshot()
        }
    }
}

private fun AssetAccount.toVaultAccount() = VaultAccount(
    id = id,
    name = name,
    institution = institution,
    type = when (type) {
        AssetType.CASH -> VaultAssetType.CASH
        AssetType.STOCK -> VaultAssetType.STOCK
        AssetType.CRYPTO -> VaultAssetType.CRYPTO
        AssetType.REAL_ESTATE -> VaultAssetType.REAL_ESTATE
        AssetType.VEHICLE -> VaultAssetType.VEHICLE
        AssetType.CREDIT_CARD -> VaultAssetType.CREDIT_CARD
        AssetType.LOAN -> VaultAssetType.LOAN
        AssetType.OTHER -> VaultAssetType.OTHER
    },
    balance = balance,
    currency = currency,
    isLinked = isLinked,
    lastUpdated = lastUpdated
)

private fun VaultAccount.toAppAssetAccount() = AssetAccount(
    id = id,
    name = name,
    institution = institution,
    type = when (type) {
        VaultAssetType.CASH -> AssetType.CASH
        VaultAssetType.STOCK -> AssetType.STOCK
        VaultAssetType.CRYPTO -> AssetType.CRYPTO
        VaultAssetType.REAL_ESTATE -> AssetType.REAL_ESTATE
        VaultAssetType.VEHICLE -> AssetType.VEHICLE
        VaultAssetType.CREDIT_CARD -> AssetType.CREDIT_CARD
        VaultAssetType.LOAN -> AssetType.LOAN
        VaultAssetType.OTHER -> AssetType.OTHER
    },
    balance = balance,
    currency = currency,
    isLinked = isLinked,
    lastUpdated = lastUpdated
)
