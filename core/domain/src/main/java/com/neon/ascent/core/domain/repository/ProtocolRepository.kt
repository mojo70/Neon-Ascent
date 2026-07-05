package com.neon.ascent.core.domain.repository

import com.neon.ascent.core.domain.goals.models.AdaptedProtocol
import com.neon.ascent.core.domain.goals.models.Protocol
import kotlinx.coroutines.flow.Flow

interface ProtocolRepository {
    fun getAllProtocols(): Flow<List<Protocol>>
    fun getProtocolsByCategory(category: String): Flow<List<Protocol>>
    fun getProtocolsByTag(tag: String): Flow<List<Protocol>>
    suspend fun getProtocolById(id: String): Protocol?
    suspend fun insertProtocol(protocol: Protocol)
    suspend fun insertAdaptedProtocol(adaptedProtocol: AdaptedProtocol)
    fun getAdaptedProtocolsForDirective(directiveId: String): Flow<List<AdaptedProtocol>>
    suspend fun deleteUserProtocol(id: String)
    suspend fun seedDefaultProtocols()
}
