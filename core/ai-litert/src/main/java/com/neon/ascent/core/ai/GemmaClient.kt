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
            throw e
        } finally {
            isInitializing = false
        }
    }

    suspend fun generateContent(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            if (engine == null) {
                initialize()
            }

            val currentEngine = engine ?: return@withContext "ERROR: GEMMA_ENGINE_OFFLINE"

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

            if (!contents.isNullOrEmpty()) {
                when (val firstContent = contents[0]) {
                    is Content.Text -> firstContent.text
                    else -> firstContent.toString()
                }
            } else {
                ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "ERROR: GEMMA_MALFUNCTION: ${e.message}"
        }
    }

    fun close() {
        engine?.close()
        engine = null
    }

    fun isAvailable(): Boolean = File(modelPath).exists()
}
