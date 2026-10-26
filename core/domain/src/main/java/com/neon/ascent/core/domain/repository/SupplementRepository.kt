package com.neon.ascent.core.domain.repository

import com.neon.ascent.core.domain.labs.models.Supplement
import com.neon.ascent.core.domain.labs.models.SupplementOverride
import kotlinx.coroutines.flow.Flow

interface SupplementRepository {
    fun observeAll(): Flow<List<Supplement>>
    fun observeById(id: String): Flow<Supplement?>
    suspend fun getById(id: String): Supplement?
    suspend fun seedDefaultSupplements()

    fun observeActiveStack(): Flow<List<Supplement>>
    suspend fun setActiveStack(ids: Set<String>)
    fun observeOverrides(): Flow<Map<String, SupplementOverride>>
    suspend fun updateOverride(id: String, override: SupplementOverride?)
    suspend fun updateAllOverrides(overrides: Map<String, SupplementOverride>)
}
