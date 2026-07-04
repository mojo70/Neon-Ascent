package com.neon.ascent.feature.biohacking

import com.google.ai.client.generativeai.GenerativeModel
import com.neon.ascent.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudGeminiClient @Inject constructor() {
    private val model = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun generateContent(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val response = model.generateContent(prompt)
            val result = response.text
            if (result.isNullOrBlank()) "ERROR: NEURAL_LINK_EMPTY_SIGNAL" else result
        } catch (e: Exception) {
            // Mapping serialization fragilities and other common errors to user-friendly messages
            if (e.message?.contains("MissingFieldException") == true || e.message?.contains("details") == true) {
                "ERROR: CORE_MALFUNCTION: Protocol parse error. Neural core requires update."
            } else {
                "ERROR: CORE_MALFUNCTION: ${e.message}"
            }
        }
    }
}
