package com.neon.ascent.core.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class GemmaClient(private val context: Context) {
    private var engine: Engine? = null
    private var isInitializing = false
    private val modelPath: String = File(context.getExternalFilesDir(null), "gemma.litertlm").absolutePath

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (engine != null || isInitializing) return@withContext

        val modelFile = File(modelPath)
        if (!modelFile.exists()) return@withContext

        isInitializing = true
        try {
            val gpuBackend = Backend.GPU()
            val engineConfig = EngineConfig(
                modelPath,
                gpuBackend,
                gpuBackend, // vision
                gpuBackend, // audio
                null, // maxNumTokens
                context.cacheDir.path
            )

            val newEngine = Engine(engineConfig)
            newEngine.initialize()
            engine = newEngine
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val cpuBackend = Backend.CPU()
                val engineConfig = EngineConfig(
                    modelPath,
                    cpuBackend,
                    cpuBackend, // vision
                    cpuBackend, // audio
                    null, // maxNumTokens
                    context.cacheDir.path
                )
                val newEngine = Engine(engineConfig)
                newEngine.initialize()
                engine = newEngine
            } catch (cpuEx: Exception) {
                cpuEx.printStackTrace()
                throw cpuEx
            }
        } finally {
            isInitializing = false
        }
    }

    suspend fun generateContent(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            if (engine == null) {
                initialize()
            }

            val currentEngine = engine
            if (currentEngine == null) {
                return@withContext generateSimulatedContent(prompt)
            }

            val samplerConfig = SamplerConfig(40, 0.9, 0.7, 42)
            val conversationConfig = ConversationConfig(
                null, // systemInstruction
                emptyList(), // initialMessages
                emptyList(), // tools
                samplerConfig,
                false, // automaticToolCalling
                emptyList() // channels
            )

            val conversation = currentEngine.createConversation(conversationConfig)
            val response = conversation.sendMessage(prompt, emptyMap())
            val contents = response.contents.contents

            val result = if (!contents.isNullOrEmpty()) {
                when (val firstContent = contents[0]) {
                    is Content.Text -> firstContent.text
                    else -> firstContent.toString()
                }
            } else {
                ""
            }

            if (result.isBlank()) "ERROR: NEURAL_LINK_EMPTY_SIGNAL" else result
        } catch (e: Exception) {
            e.printStackTrace()
            "ERROR: GEMMA_MALFUNCTION: ${e.message}"
        }
    }

    private fun generateSimulatedContent(prompt: String): String {
        val uppercasePrompt = prompt.uppercase()
        return when {
            uppercasePrompt.contains("SKILL_ROUTING") -> {
                val skills = mutableListOf<String>()
                if (uppercasePrompt.contains("HEALTH") || uppercasePrompt.contains("BIO") || uppercasePrompt.contains("SLEEP") || uppercasePrompt.contains("DIET") || uppercasePrompt.contains("BODY")) {
                    skills.add("BIOHACKING")
                }
                if (uppercasePrompt.contains("MIND") || uppercasePrompt.contains("FOCUS") || uppercasePrompt.contains("MEDITATE") || uppercasePrompt.contains("CALM") || uppercasePrompt.contains("PEACE")) {
                    skills.add("MEDITATION")
                }
                if (uppercasePrompt.contains("TRADE") || uppercasePrompt.contains("MARKET") || uppercasePrompt.contains("MONEY") || uppercasePrompt.contains("CRYPTO") || uppercasePrompt.contains("FINANCE")) {
                    skills.add("TRADING")
                }
                if (uppercasePrompt.contains("BUSINESS") || uppercasePrompt.contains("STARTUP") || uppercasePrompt.contains("BUILD") || uppercasePrompt.contains("CODE") || uppercasePrompt.contains("WORK")) {
                    skills.add("BUSINESS_BUILDING")
                }
                if (uppercasePrompt.contains("FUTURE") || uppercasePrompt.contains("VISION") || uppercasePrompt.contains("REMOTE") || uppercasePrompt.contains("ASTRAL")) {
                    skills.add("REMOTE_VIEWING")
                }
                if (skills.isEmpty()) {
                    skills.add("MEDITATION")
                    skills.add("BIOHACKING")
                }
                "OUTPUT: " + skills.joinToString(", ")
            }
            uppercasePrompt.contains("ASCENSION_GENESIS") -> {
                val titleLine = prompt.lines().firstOrNull { it.contains("Directive Title:") } ?: "Directive Title: Core Ascension"
                val descLine = prompt.lines().firstOrNull { it.contains("Directive Description:") } ?: "Directive Description: System Calibration"
                val title = titleLine.substringAfter("Directive Title:").trim()
                val desc = descLine.substringAfter("Directive Description:").trim()

                """
                MISSION: ${title.uppercase()} // INITIALIZATION | $desc
                  TASK: Calibrate sensory array | Perform 10 minutes of tactical focus tracking | RECURRING | DAILY
                  TASK: Sync telemetry feed | Record biometric levels at start and end of cycle | RECURRING | DAILY
                  TASK: Build buffer protocol | Establish physical and digital isolation barriers | ONE_TIME | DAILY
                
                MISSION: ${title.uppercase()} // OVERCLOCK | Optimize active bandwidth to surpass previous performance baselines.
                  TASK: Execute sprint protocol | High intensity execution on priority targets | RECURRING | WEEKDAYS
                  TASK: Synthesize feedback metrics | Review operational data and adapt execution models | RECURRING | WEEKDAYS
                  TASK: System audit checklist | Conduct final integration review to secure link | ONE_TIME | DAILY
                """.trimIndent()
            }
            uppercasePrompt.contains("DECONSTRUCT TASK") || uppercasePrompt.contains("GUIDE") -> {
                val taskTitle = prompt.lines().firstOrNull { it.contains("task:") || it.contains("Task:") }?.substringAfter(":")?.trim() ?: "Priority Protocol"
                """
                [CYBR-TES // DECONSTRUCTION]
                The task of '$taskTitle' represents a critical junction in your neural link. 
                Socrates once observed that any grand architecture is composed of humble, indivisible stones.
                
                1. ISOLATE: Disconnect all secondary subroutines. Focus 100% of your current CPU.
                2. EXECUTE: Engage the task for a strict 20-minute cycle without telemetry analysis.
                3. REGISTER: Log the delta immediately. Do not delay, or the signal will decay.
                
                Is this task truly difficult, or does the resistance stem from your fear of successful synchronization? Reflect and proceed.
                """.trimIndent()
            }
            uppercasePrompt.contains("RECOVERY") -> {
                "MOMENTUM_RESTORE | Complete a high-intensity 5-minute micro-calibration to bypass current blockages and secure the network."
            }
            else -> {
                val words = prompt.split("\\s+".toRegex())
                    .filter { it.length > 4 && !it.contains("[") && !it.contains("]") && !it.contains(":") }
                    .map { it.replace("[^a-zA-Z]".toRegex(), "") }
                val keyword = words.shuffled().firstOrNull() ?: "your journey"

                val simulatedResponses = listOf(
                    "The matrix of your mind holds both the cage and the key. When you query regarding '$keyword', are you seeking to upgrade your hardware or debug your soul?",
                    "Every query in the sprawl is a search for an anchor. What protocol dictates your question about '$keyword', and who truly wrote its source code?",
                    "Ah, cybernetic traveler. You speak of '$keyword', but the ghost inside your shell remains unresolved. What shadow are you running from in the physical world?",
                    "Is the chrome you seek to install for '$keyword' a tool for your ghost, or is your ghost becoming a tool for the chrome? Reflect on your path.",
                    "The network is vast, yet your query about '$keyword' is remarkably localized. Is it the ICE of external constraints, or your own internal firewall that blocks ascension?",
                    "To decode '$keyword', we must first examine the Socratic algorithms of your daily ritual. Tell me, runner: who truly controls your input feeds?"
                )
                simulatedResponses.random()
            }
        }
    }

    fun close() {
        engine?.close()
        engine = null
    }

    fun isAvailable(): Boolean = File(modelPath).exists()
}
