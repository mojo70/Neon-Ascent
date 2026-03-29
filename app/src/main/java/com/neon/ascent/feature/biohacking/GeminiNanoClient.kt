package com.neon.ascent.feature.biohacking

import android.content.Context
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
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
     * Checks if AICore (Gemini Nano) is supported and ready on this device.
     */
    suspend fun isSupported(): Boolean = withContext(Dispatchers.Main) {
        try {
            // Using a simple check to see if we can create a model.
            // The previous checkFeatureStatus API was from an incompatible version/library.
            getModel()
            true
        } catch (e: Exception) {
            // If the service fails to bind or isn't present, we'll hit this.
            false
        }
    }

    private suspend fun getModel(): GenerativeModel = withContext(Dispatchers.Main) {
        model ?: run {
            val config = generationConfig {
                this.context = this@GeminiNanoClient.context
                temperature = 0.4f
                topK = 16
                maxOutputTokens = 128
            }
            
            GenerativeModel(generationConfig = config).also { model = it }
        }
    }

    suspend fun generateContent(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            if (!isSupported()) return@withContext "ERROR: AI_CORE_UNAVAILABLE"

            val generativeModel = getModel()
            val response = generativeModel.generateContent(prompt)
            response.text ?: "ERROR: NEURAL_LINK_DISCONNECT"
        } catch (e: Exception) {
            "ERROR: CORE_MALFUNCTION: ${e.message}"
        }
    }
}
