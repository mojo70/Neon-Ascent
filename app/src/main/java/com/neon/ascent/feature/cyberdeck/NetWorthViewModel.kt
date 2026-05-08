package com.neon.ascent.feature.cyberdeck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.local.NetWorthDao
import com.neon.ascent.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NetWorthViewModel @Inject constructor(
    private val netWorthDao: NetWorthDao
) : ViewModel() {

    val accounts = netWorthDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val snapshots = netWorthDao.getRecentSnapshots()
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

    // TODO: Integrate financial data provider APIs (e.g., Plaid, MX, or exchange-specific APIs)
    // to allow users to securely link their actual accounts and automate data synchronization.
    // Manual entry serves as the MVP for offline/private asset tracking.
    fun addManualAccount(name: String, institution: String, type: AssetType, balance: Double) {
        viewModelScope.launch {
            val account = AssetAccount(
                id = UUID.randomUUID().toString(),
                name = name,
                institution = institution,
                type = type,
                balance = balance,
                isLinked = false
            )
            netWorthDao.insertAccount(account)
            saveSnapshot()
        }
    }

    private fun saveSnapshot() {
        viewModelScope.launch {
            val s = summary.value
            netWorthDao.insertSnapshot(
                AssetSnapshot(
                    timestamp = System.currentTimeMillis(),
                    totalAssets = s.totalValue + s.totalDebt,
                    totalDebt = s.totalDebt,
                    netWorth = s.totalValue
                )
            )
        }
    }

    fun deleteAccount(account: AssetAccount) {
        viewModelScope.launch {
            netWorthDao.deleteAccount(account)
            saveSnapshot()
        }
    }
}
