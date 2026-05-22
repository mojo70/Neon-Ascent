package com.neon.ascent.feature.dashboard

import com.neon.ascent.data.local.NeuralMemoryDao
import com.neon.ascent.data.local.entity.NeuralMemory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryPalaceManager @Inject constructor(
    private val neuralMemoryDao: NeuralMemoryDao
) {
    /**
     * Store a verbatim piece of data into a specific wing and room.
     */
    suspend fun storeMemory(
        wing: String,
        room: String,
        content: String,
        importance: Float = 0.5f,
        metadata: String? = null
    ) {
        val memory = NeuralMemory(
            wing = wing,
            room = room,
            content = content,
            importance = importance,
            metadata = metadata
        )
        neuralMemoryDao.insertMemory(memory)
    }

    /**
     * Retrieve relevant "drawers" for a given context.
     * This simulates the MemPalace retrieval logic by combining wing/room scoping with keyword search.
     */
    suspend fun fetchContext(
        query: String,
        targetWing: String? = null,
        limit: Int = 5
    ): String {
        // In a real MemPalace, this would be semantic search. 
        // Here we use FTS/Keyword search on SQLite as a robust on-device alternative.
        val results = neuralMemoryDao.searchMemories(query, limit)
            .filter { targetWing == null || it.wing == targetWing }
            .sortedByDescending { it.importance * (1.0 / (System.currentTimeMillis() - it.timestamp + 1)) } // Simple temporal/importance boost

        if (results.isEmpty()) return "No specific memories found for context."

        return results.joinToString("\n\n") { memory ->
            "[WING: ${memory.wing}][ROOM: ${memory.room}] ${memory.content}"
        }
    }

    /**
     * Utility to log a Socratic dialogue session.
     */
    suspend fun logDialogue(runnerMessage: String, aiResponse: String) {
        storeMemory(
            wing = "DIALOGUE",
            room = "SOCRATIC_SESSION",
            content = "Runner: $runnerMessage\nCYBR-TES: $aiResponse",
            importance = 0.7f
        )
    }
}
