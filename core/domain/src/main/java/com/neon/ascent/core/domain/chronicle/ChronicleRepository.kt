package com.neon.ascent.core.domain.chronicle

import kotlinx.coroutines.flow.Flow

interface ChronicleRepository {
    fun observeChronicle(): Flow<List<ChronicleEntry>>
    suspend fun saveEntry(entry: ChronicleEntry)
    suspend fun setHearted(sourceId: String, hearted: Boolean)
    suspend fun importIfAbsent(entry: ChronicleEntry): Boolean
}
