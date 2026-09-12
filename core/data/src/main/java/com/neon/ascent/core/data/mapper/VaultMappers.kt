package com.neon.ascent.core.data.mapper

import com.neon.ascent.core.data.local.entity.VaultAccountEntity
import com.neon.ascent.core.data.local.entity.VaultSnapshotEntity
import com.neon.ascent.core.data.local.entity.VaultWatchlistItemEntity
import com.neon.ascent.core.domain.vault.models.VaultAccount
import com.neon.ascent.core.domain.vault.models.VaultSnapshot
import com.neon.ascent.core.domain.vault.models.VaultWatchlistItem

fun VaultAccountEntity.toDomain() = VaultAccount(
    id = id,
    name = name,
    institution = institution,
    type = type,
    balance = balance,
    currency = currency,
    isLinked = isLinked,
    lastUpdated = lastUpdated
)

fun VaultAccount.toEntity() = VaultAccountEntity(
    id = id,
    name = name,
    institution = institution,
    type = type,
    balance = balance,
    currency = currency,
    isLinked = isLinked,
    lastUpdated = lastUpdated
)

fun VaultSnapshotEntity.toDomain() = VaultSnapshot(
    id = id,
    timestamp = timestamp,
    totalAssets = totalAssets,
    totalDebt = totalDebt,
    netWorth = netWorth
)

fun VaultSnapshot.toEntity() = VaultSnapshotEntity(
    id = id,
    timestamp = timestamp,
    totalAssets = totalAssets,
    totalDebt = totalDebt,
    netWorth = netWorth
)

fun VaultWatchlistItemEntity.toDomain() = VaultWatchlistItem(
    symbol = symbol,
    name = name,
    isCrypto = isCrypto
)

fun VaultWatchlistItem.toEntity() = VaultWatchlistItemEntity(
    symbol = symbol,
    name = name,
    isCrypto = isCrypto
)
