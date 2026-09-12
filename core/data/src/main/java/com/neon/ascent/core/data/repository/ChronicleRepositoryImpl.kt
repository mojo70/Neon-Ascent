package com.neon.ascent.core.data.repository

import com.google.gson.Gson
import com.neon.ascent.core.data.local.dao.NeuralMemoryDao
import com.neon.ascent.core.data.local.entity.NeuralMemory
import com.neon.ascent.core.domain.chronicle.ChronicleEntry
import com.neon.ascent.core.domain.chronicle.ChronicleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChronicleRepositoryImpl @Inject constructor(
    private val neuralMemoryDao: NeuralMemoryDao
) : ChronicleRepository {

    private val gson = Gson()

    private data class MetadataPayload(
        val source: String? = null,
        val sourceId: String? = null,
        val hearted: Boolean = false
    )

    override fun observeChronicle(): Flow<List<ChronicleEntry>> {
        return neuralMemoryDao.getMemoriesByWing("CHRONICLE").map { list ->
            list.map { toChronicleEntry(it) }
        }
    }

    override suspend fun saveEntry(entry: ChronicleEntry) {
        if (entry.content.isBlank()) return

        val existingMemories = neuralMemoryDao.getMemoriesByWing(entry.wing).first()
        val existing = existingMemories.firstOrNull { memoryMatches(it, entry.source, entry.sourceId) }

        val memoryToSave = toNeuralMemory(entry, existingId = existing?.id ?: 0L)
        neuralMemoryDao.insertMemory(memoryToSave)
    }

    override suspend fun setHearted(sourceId: String, hearted: Boolean) {
        val allMemories = neuralMemoryDao.getAllMemories().first()
        val existing = allMemories.firstOrNull { memory ->
            val payload = parseMetadata(memory.metadata)
            payload?.sourceId == sourceId || memory.id.toString() == sourceId
        } ?: return

        val currentPayload = parseMetadata(existing.metadata) ?: MetadataPayload(sourceId = sourceId)
        val updatedPayload = currentPayload.copy(hearted = hearted)
        val newImportance = if (hearted) maxOf(0.8f, existing.importance) else 0.5f

        val updatedMemory = existing.copy(
            importance = newImportance,
            metadata = gson.toJson(updatedPayload)
        )
        neuralMemoryDao.insertMemory(updatedMemory)
    }

    override suspend fun importIfAbsent(entry: ChronicleEntry): Boolean {
        if (entry.content.isBlank()) return false

        val existingMemories = neuralMemoryDao.getMemoriesByWing(entry.wing).first()
        val exists = existingMemories.any { memoryMatches(it, entry.source, entry.sourceId) }

        if (exists) return false

        val memoryToInsert = toNeuralMemory(entry)
        neuralMemoryDao.insertMemory(memoryToInsert)
        return true
    }

    private fun memoryMatches(memory: NeuralMemory, source: String, sourceId: String): Boolean {
        val payload = parseMetadata(memory.metadata)
        if (payload != null && payload.source == source && payload.sourceId == sourceId) {
            return true
        }
        return payload?.sourceId == sourceId
    }

    private fun parseMetadata(metadataStr: String?): MetadataPayload? {
        if (metadataStr.isNullOrBlank()) return null
        return try {
            gson.fromJson(metadataStr, MetadataPayload::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun toChronicleEntry(memory: NeuralMemory): ChronicleEntry {
        val payload = parseMetadata(memory.metadata)
        val source = payload?.source ?: when (memory.wing) {
            "CHRONICLE" -> if (memory.room == "SHARD") "shard" else if (memory.room == "FRAGMENT") "fragment" else "journal"
            "DIALOGUE" -> "chat"
            else -> "unknown"
        }
        val sourceId = payload?.sourceId ?: memory.id.toString()
        val isHearted = payload?.hearted ?: (memory.importance >= 0.8f)

        return ChronicleEntry(
            source = source,
            sourceId = sourceId,
            wing = memory.wing,
            room = memory.room,
            content = memory.content,
            timestamp = memory.timestamp,
            hearted = isHearted,
            metadata = memory.metadata
        )
    }

    private fun toNeuralMemory(entry: ChronicleEntry, existingId: Long = 0L): NeuralMemory {
        val importance = if (entry.hearted) 0.8f else 0.5f
        val metadataObj = MetadataPayload(
            source = entry.source,
            sourceId = entry.sourceId,
            hearted = entry.hearted
        )
        val metadataJson = gson.toJson(metadataObj)

        return NeuralMemory(
            id = existingId,
            wing = entry.wing,
            room = entry.room,
            content = entry.content,
            importance = importance,
            timestamp = entry.timestamp,
            metadata = metadataJson
        )
    }
}
