package com.neon.ascent.core.ai

import android.content.Context
import android.util.Log
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
    var lastInitError: String? = null
        private set

    val modelPath: String = File(context.getExternalFilesDir(null), "gemma.litertlm").absolutePath

    fun isReady(): Boolean = engine != null

    suspend fun warmup() {
        if (!isReady()) {
            initialize()
        }
    }

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (engine != null || isInitializing) return@withContext

        val modelFile = File(modelPath)
        if (!modelFile.exists()) {
            lastInitError = "MODEL_FILE_NOT_FOUND ($modelPath)"
            Log.w("GemmaClient", lastInitError!!)
            return@withContext
        }

        if (modelFile.length() == 0L) {
            lastInitError = "MODEL_FILE_EMPTY"
            Log.w("GemmaClient", lastInitError!!)
            return@withContext
        }

        isInitializing = true
        Log.i("GemmaClient", "Initializing LiteRT-LM engine with model size: ${modelFile.length()} bytes")
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
            lastInitError = null
            Log.i("GemmaClient", "LiteRT-LM GPU Engine initialized successfully.")
        } catch (e: Exception) {
            Log.w("GemmaClient", "LiteRT-LM GPU initialization failed. Falling back to CPU...", e)
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
                lastInitError = null
                Log.i("GemmaClient", "LiteRT-LM CPU Engine initialized successfully.")
            } catch (cpuEx: Exception) {
                lastInitError = "INIT_FAILED: ${cpuEx.localizedMessage}"
                Log.e("GemmaClient", "LiteRT-LM CPU Engine initialization failed.", cpuEx)
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

            val currentEngine = engine ?: return@withContext AiResult.Failure(
                lastInitError ?: "GEMMA_NOT_INITIALIZED"
            )

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
            Log.e("GemmaClient", "Error during Gemma inference", e)
            AiResult.Failure("GEMMA_GENERATE", e)
        }
    }

    @Deprecated("Use generate instead")
    suspend fun generateContent(prompt: String): String = when (val res = generate(prompt)) {
        is AiResult.Success -> res.text
        is AiResult.Failure -> "Signal lost in the sprawl. Retry later."
    }

    fun close() {
        try {
            engine?.close()
        } catch (e: Exception) {
            Log.w("GemmaClient", "Error closing LiteRT engine", e)
        } finally {
            engine = null
        }
    }

    fun isAvailable(): Boolean {
        val file = File(modelPath)
        return file.exists() && file.length() > 0
    }
}
