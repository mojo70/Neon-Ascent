package com.neon.ascent.feature.biohacking

import com.neon.ascent.core.ai.GemmaClient
import com.neon.ascent.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

enum class AiType {
    LOCAL,
    CLOUD,
    NONE
}

@Singleton
class AiProvider @Inject constructor(
    private val geminiNanoClient: GeminiNanoClient,
    private val gemmaClient: GemmaClient,
    private val cloudGeminiClient: CloudGeminiClient,
    private val settingsRepository: SettingsRepository
) {
    private val _activeAiType = MutableStateFlow(AiType.NONE)
    val activeAiType: StateFlow<AiType> = _activeAiType.asStateFlow()

    suspend fun initialize() {
        try {
            when {
                gemmaClient.isAvailable() -> {
                    gemmaClient.initialize()
                    _activeAiType.value = AiType.LOCAL
                }
                geminiNanoClient.isSupported() -> {
                    _activeAiType.value = AiType.LOCAL
                }
                else -> {
                    _activeAiType.value = AiType.CLOUD
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to Cloud if local initialization fails
            _activeAiType.value = AiType.CLOUD
        }
    }

    fun onModelDownloaded() {
        _activeAiType.value = AiType.LOCAL
    }

    /**
     * Generates content using the active AI core.
     * @param forceLocal If true, the request will fail if the local AI core is not available,
     * skipping any fallback to Cloud.
     */
    suspend fun generateContent(
        prompt: String, 
        forceLocal: Boolean = false
    ): String {
        val currentType = _activeAiType.value
        val isGlobalLocalOnly = settingsRepository.isLocalAiOnly.first()
        
        // Final decision on whether we are allowed to use Cloud
        val shouldForceLocal = forceLocal || isGlobalLocalOnly
        
        return when {
            gemmaClient.isAvailable() -> gemmaClient.generateContent(prompt)
            currentType == AiType.LOCAL -> geminiNanoClient.generateContent(prompt)
            shouldForceLocal -> "ERROR: LOCAL_AI_CORE_REQUIRED_BUT_UNAVAILABLE"
            currentType == AiType.CLOUD -> cloudGeminiClient.generateContent(prompt)
            else -> "ERROR: NO_AI_CORE_DETECTED"
        }
    }
}
