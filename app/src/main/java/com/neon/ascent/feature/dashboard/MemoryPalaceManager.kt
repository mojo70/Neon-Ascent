package com.neon.ascent.feature.dashboard

import com.neon.ascent.data.local.NeuralMemoryDao
import com.neon.ascent.data.local.entity.NeuralMemory
import com.neon.ascent.feature.health.domain.uplink.DeepBiometrics
import com.neon.ascent.feature.biohacking.AiProvider
import com.neon.ascent.core.ai.AiPersona
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryPalaceManager @Inject constructor(
    private val neuralMemoryDao: NeuralMemoryDao,
    private val aiProvider: AiProvider
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
            timestamp = System.currentTimeMillis(),
            metadata = metadata
        )
        neuralMemoryDao.insertMemory(memory)
    }

    /**
     * Mines current biometric state into the Palace and generates a Socratic Insight.
     */
    suspend fun mineBiometrics(metrics: DeepBiometrics) {
        val rawContent = """
            BODY_BATTERY: ${metrics.bodyBattery ?: "N/A"}
            SLEEP_SCORE: ${metrics.sleepScore ?: "N/A"}
            STRESS_LEVEL: ${metrics.stressLevel ?: "N/A"}
            VO2_MAX: ${metrics.vo2Max ?: "N/A"}
        """.trimIndent()

        // 1. Store Raw Metrics
        storeMemory(
            wing = "HEALTH",
            room = "BIOMETRIC_RAW",
            content = rawContent,
            importance = 0.4f
        )

        // 2. Generate Insight
        val context = "CURRENT_BIOMETRICS:\n$rawContent"
        val prompt = AiPersona.getSocratesPrompt(context) + 
            "\nTask: Provide a deep, piercing biohacking insight based on these numbers. " +
            "How does the physical state impact the operative's ghost? Max 40 words."
        
        val insight = aiProvider.generateContent(prompt)
        
        // 3. Store Insight
        storeMemory(
            wing = "INSIGHTS",
            room = "BIOMETRIC_ANALYSIS",
            content = insight,
            importance = 0.8f
        )
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
