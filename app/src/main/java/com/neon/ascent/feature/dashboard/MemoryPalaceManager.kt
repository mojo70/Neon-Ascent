package com.neon.ascent.feature.dashboard

import com.neon.ascent.core.data.local.dao.NeuralMemoryDao
import com.neon.ascent.core.data.local.entity.NeuralMemory
import com.neon.ascent.feature.health.domain.uplink.DeepBiometrics
import com.neon.ascent.feature.biohacking.AiProvider
import com.neon.ascent.core.ai.AiPersona
import com.neon.ascent.core.domain.repository.SkillRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryPalaceManager @Inject constructor(
    private val neuralMemoryDao: NeuralMemoryDao,
    private val aiProvider: AiProvider
) : SkillRepository {
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
     * Initializes the Palace with core Expert Skills if they don't exist.
     */
    override suspend fun installExpertSkills() {
        val skills = mapOf(
            "BIOHACKING" to """
                [SKILL: BIOHACKER_PREMIUM]
                Expertise: Optimization of the biological shell.
                Focus: HRV synchronization, circadian rhythm anchoring, nootropic stacks, and thermal regulation.
                Method: Data-driven biological intervention. When analyzing tasks, prioritize physical efficiency and neurochemical stability.
            """.trimIndent(),
            "MEDITATION" to """
                [SKILL: ZEN_ARCHITECT]
                Expertise: Neural stillness and ghost-stabilization.
                Focus: Mindfulness, box-breathing, Vipassana techniques, and alpha-wave induction.
                Method: Dialectic calm. Break tasks into moments of presence.
            """.trimIndent(),
            "REMOTE_VIEWING" to """
                [SKILL: COORDINATE_OBSERVER]
                Expertise: Non-local perception protocols.
                Focus: CRV Phase 1-3, ideogram decoding, signal line acquisition.
                Method: Sensory isolation and objective description. Use when tasks require intuition or "seeing" beyond the immediate grid.
            """.trimIndent(),
            "BUSINESS_BUILDING" to """
                [SKILL: VENTURE_SAMURAI]
                Expertise: Sprawl-scale operation construction.
                Focus: MVP iteration, unit economics, lean scalability, and disruption protocols.
                Method: Ruthless prioritization of value-capture. Break directives into "atomic revenue units."
            """.trimIndent(),
            "TRADING" to """
                [SKILL: QUANT_RUNNER]
                Expertise: Market-matrix manipulation.
                Focus: Risk/Reward ratios, technical analysis, Fibonacci retracements, and psychological stop-losses.
                Method: Probabilistic execution. Treat every habit or task as a trade with an entry, exit, and liquidation price.
            """.trimIndent()
        )

        skills.forEach { (name, prompt) ->
            // Check if skill already exists to avoid duplication
            val existing = neuralMemoryDao.getMemoriesByRoom("SKILLS", name).first()
            if (existing.isEmpty()) {
                storeMemory(
                    wing = "SKILLS",
                    room = name,
                    content = prompt,
                    importance = 1.0f
                )
            }
        }
    }

    /**
     * Retrieves a specific expert skill prompt from the Palace.
     */
    override suspend fun getSkillPrompt(skillName: String): String? {
        val memories = neuralMemoryDao.searchMemories(skillName, 1)
        return memories.firstOrNull { it.wing == "SKILLS" }?.content
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
        val results = neuralMemoryDao.searchMemories(query, limit)
            .filter { targetWing == null || it.wing == targetWing }
            .sortedByDescending { it.importance * (1.0 / (System.currentTimeMillis() - it.timestamp + 1)) }

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
