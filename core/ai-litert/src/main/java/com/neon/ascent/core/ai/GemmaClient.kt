package com.neon.ascent.core.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.neon.ascent.core.domain.ai.AiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class GemmaClient(private val context: Context) {
    private var engine: Engine? = null
    private var isInitializing = false
    private val modelPath: String = File(context.getExternalFilesDir(null), "gemma.litertlm").absolutePath

    fun isReady(): Boolean = engine != null

    suspend fun warmup() {
        if (!isReady()) {
            initialize()
        }
    }

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
                // Do not throw here, allow generate() to return AiResult.Failure
            }
        } finally {
            isInitializing = false
        }
    }

    suspend fun generate(prompt: String): AiResult = withContext(Dispatchers.IO) {
        try {
            if (engine == null) {
                initialize()
            }

            val currentEngine = engine ?: return@withContext AiResult.Failure("GEMMA_NOT_INITIALIZED")

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
                null
            }

            if (result.isNullOrBlank()) {
                AiResult.Failure("GEMMA_EMPTY_SIGNAL")
            } else {
                AiResult.Success(result)
            }
        } catch (e: Exception) {
            AiResult.Failure("GEMMA_GENERATE", e)
        }
    }

    @Deprecated("Use generate instead")
    suspend fun generateContent(prompt: String): String = when (val res = generate(prompt)) {
        is AiResult.Success -> res.text
        is AiResult.Failure -> "Signal lost in the sprawl. Retry later."
    }

    fun close() {
        engine?.close()
        engine = null
    }

    fun isAvailable(): Boolean = File(modelPath).exists()
}
