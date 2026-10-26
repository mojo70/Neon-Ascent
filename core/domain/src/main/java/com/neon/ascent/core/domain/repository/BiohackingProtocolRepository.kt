package com.neon.ascent.core.domain.repository

import com.neon.ascent.core.domain.labs.models.BiohackingProtocol
import com.neon.ascent.core.domain.labs.models.BiohackingProtocolOverride
import kotlinx.coroutines.flow.Flow

interface BiohackingProtocolRepository {
    fun observeAll(): Flow<List<BiohackingProtocol>>
    fun observeById(id: String): Flow<BiohackingProtocol?>
    suspend fun getById(id: String): BiohackingProtocol?
    suspend fun seedDefaultProtocols()

    fun observeActiveProtocols(): Flow<List<BiohackingProtocol>>
    fun observeActiveIds(): Flow<Set<String>>
    suspend fun setActiveProtocols(ids: Set<String>)
    fun observeOverrides(): Flow<Map<String, BiohackingProtocolOverride>>
    suspend fun updateOverride(id: String, override: BiohackingProtocolOverride?)
    suspend fun updateAllOverrides(overrides: Map<String, BiohackingProtocolOverride>)
}
