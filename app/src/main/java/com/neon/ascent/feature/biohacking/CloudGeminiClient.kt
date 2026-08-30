package com.neon.ascent.feature.biohacking

import com.google.ai.client.generativeai.GenerativeModel
import com.neon.ascent.BuildConfig
import com.neon.ascent.core.domain.ai.AiResult
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

    suspend fun generate(prompt: String): AiResult = withContext(Dispatchers.IO) {
        try {
            val response = model.generateContent(prompt)
            val result = response.text
            if (result.isNullOrBlank()) {
                AiResult.Failure("CLOUD_EMPTY_SIGNAL")
            } else {
                AiResult.Success(result)
            }
        } catch (e: Exception) {
            AiResult.Failure("CLOUD_GENERATE", e)
        }
    }

    @Deprecated("Use generate instead")
    suspend fun generateContent(prompt: String): String = when (val res = generate(prompt)) {
        is AiResult.Success -> res.text
        is AiResult.Failure -> "Cloud uplink unstable. Retrying via secondary link..."
    }
}
