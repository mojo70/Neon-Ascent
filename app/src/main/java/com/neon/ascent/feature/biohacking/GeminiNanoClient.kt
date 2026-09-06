package com.neon.ascent.feature.biohacking

import android.content.Context
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
import com.neon.ascent.core.domain.ai.AiResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiNanoClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var model: GenerativeModel? = null

    /**
     * Checks if AICore (Gemini Nano) is supported on this hardware.
     */
    suspend fun isSupported(): Boolean = withContext(Dispatchers.Main) {
        try {
            getModel()
            true
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Checks if the model is initialized and ready.
     */
    fun isReady(): Boolean = model != null

    /**
     * Warms up the model.
     */
    suspend fun warmup() {
        if (!isReady()) {
            try {
                getModel()
            } catch (e: Exception) {
                // Log but don't crash
            }
        }
    }

    private suspend fun getModel(): GenerativeModel = withContext(Dispatchers.Main) {
        model ?: run {
            val config = generationConfig {
                this.context = this@GeminiNanoClient.context
                temperature = 0.4f
                topK = 16
                maxOutputTokens = 1024
            }
            
            GenerativeModel(generationConfig = config).also { model = it }
        }
    }

    suspend fun generate(prompt: String): AiResult = withContext(Dispatchers.IO) {
        try {
            val generativeModel = getModel()
            val response = generativeModel.generateContent(prompt)
            val result = response.text
            if (result.isNullOrBlank()) {
                AiResult.Failure("NANO_EMPTY_SIGNAL")
            } else {
                AiResult.Success(result)
            }
        } catch (e: Exception) {
            AiResult.Failure("NANO_GENERATE", e)
        }
    }

    @Deprecated("Use generate instead")
    suspend fun generateContent(prompt: String): String = when (val res = generate(prompt)) {
        is AiResult.Success -> res.text
        is AiResult.Failure -> "CYBR-TES: UPLINK_INTERRUPTED // Tap send to re-establish connection."
    }
}
