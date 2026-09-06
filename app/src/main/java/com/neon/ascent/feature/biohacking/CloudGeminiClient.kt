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
    private val isApiKeyValid = BuildConfig.GEMINI_API_KEY.isNotBlank() &&
            !BuildConfig.GEMINI_API_KEY.contains("YOUR_GEMINI_API_KEY", ignoreCase = true)

    private val model = if (isApiKeyValid) {
        try {
            GenerativeModel(
                modelName = "gemini-2.0-flash",
                apiKey = BuildConfig.GEMINI_API_KEY
            )
        } catch (_: Exception) {
            null
        }
    } else null

    suspend fun generate(prompt: String): AiResult = withContext(Dispatchers.IO) {
        if (!isApiKeyValid || model == null) {
            return@withContext AiResult.Failure("NO_API_KEY")
        }

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
        is AiResult.Failure -> "ERROR: CLOUD_AI_FAILED [${res.reason}]"
    }
}
