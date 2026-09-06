package com.neon.ascent.core.ai

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
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
    private var activeConversation: Conversation? = null
    private var isInitializing = false
    var lastInitError: String? = null
        private set

    val modelPath: String
        get() = findModelFile()?.absolutePath ?: File(context.getExternalFilesDir(null), "gemma.litertlm").absolutePath

    fun findModelFile(): File? {
        // 1. App's private external files dir
        val appFile = File(context.getExternalFilesDir(null), "gemma.litertlm")
        if (appFile.exists() && appFile.length() > 0) return appFile

        val appE2B = File(context.getExternalFilesDir(null), "gemma-4-E2B-it.litertlm")
        if (appE2B.exists() && appE2B.length() > 0) return appE2B

        // 2. Google AI Edge Gallery model storage directory
        try {
            val galleryDir = File("/storage/emulated/0/Android/data/com.google.ai.edge.gallery/files/")
            if (galleryDir.exists()) {
                val litertFiles = galleryDir.walkTopDown()
                    .filter { it.isFile && it.name.endsWith(".litertlm") && it.length() > 0 }
                    .toList()
                if (litertFiles.isNotEmpty()) {
                    val found = litertFiles.maxByOrNull { it.length() }
                    if (found != null) {
                        Log.i("GemmaClient", "Found AI Edge Gallery LiteRT model at: ${found.absolutePath}")
                        return found
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("GemmaClient", "Error searching gallery directory", e)
        }

        // 3. Downloads directory
        try {
            val downloadsDir = File("/storage/emulated/0/Download/")
            if (downloadsDir.exists()) {
                val downloadFiles = downloadsDir.listFiles { _, name -> name.endsWith(".litertlm") }
                if (!downloadFiles.isNullOrEmpty()) {
                    val found = downloadFiles.firstOrNull { it.length() > 0 }
                    if (found != null) {
                        Log.i("GemmaClient", "Found Download LiteRT model at: ${found.absolutePath}")
                        return found
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("GemmaClient", "Error searching downloads directory", e)
        }

        return null
    }

    fun isReady(): Boolean = engine != null

    suspend fun warmup() {
        if (!isReady()) {
            initialize()
        }
    }

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (engine != null || isInitializing) return@withContext

        val modelFile = findModelFile()
        if (modelFile == null || !modelFile.exists()) {
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
        val targetPath = modelFile.absolutePath
        Log.i("GemmaClient", "Initializing LiteRT-LM engine with model: $targetPath (Size: ${modelFile.length()} bytes)")
        try {
            val gpuBackend = Backend.GPU()
            val engineConfig = EngineConfig(
                targetPath,
                gpuBackend,
                null, // vision backend
                null, // audio backend
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
                    targetPath,
                    cpuBackend,
                    null, // vision backend
                    null, // audio backend
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

            var conversation = activeConversation
            if (conversation == null) {
                val samplerConfig = SamplerConfig(40, 0.9, 0.7, 42)
                val conversationConfig = ConversationConfig(
                    null, // systemInstruction
                    emptyList(), // initialMessages
                    emptyList(), // tools
                    samplerConfig,
                    false, // automaticToolCalling
                    emptyList() // channels
                )
                conversation = currentEngine.createConversation(conversationConfig)
                activeConversation = conversation
            }

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
            try {
                activeConversation?.close()
            } catch (_: Exception) {}
            activeConversation = null
            AiResult.Failure("GEMMA_GENERATE: ${e.localizedMessage}", e)
        }
    }

    @Deprecated("Use generate instead")
    suspend fun generateContent(prompt: String): String = when (val res = generate(prompt)) {
        is AiResult.Success -> res.text
        is AiResult.Failure -> "Signal lost in the sprawl. Retry later."
    }

    fun close() {
        try {
            activeConversation?.close()
            engine?.close()
        } catch (e: Exception) {
            Log.w("GemmaClient", "Error closing LiteRT engine", e)
        } finally {
            activeConversation = null
            engine = null
        }
    }

    fun isAvailable(): Boolean {
        return findModelFile() != null
    }
}
