package com.neon.ascent.feature.biohacking

import com.neon.ascent.core.ai.GemmaClient
import com.neon.ascent.core.domain.ai.AiCore
import com.neon.ascent.core.domain.ai.AiResult
import com.neon.ascent.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.time.Instant
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
) : AiCore {
    private val _activeAiType = MutableStateFlow(AiType.NONE)
    val activeAiType: StateFlow<AiType> = _activeAiType.asStateFlow()

    private var failureCount = 0
    private var lastFailureTime: Instant? = null
    private val FAILURE_THRESHOLD = 3
    private val CIRCUIT_BREAKER_WINDOW_HOURS = 6L

    suspend fun initialize() {
        try {
            when {
                gemmaClient.isAvailable() -> {
                    gemmaClient.initialize()
                    _activeAiType.value = AiType.LOCAL
                }
                geminiNanoClient.isSupported() -> {
                    geminiNanoClient.warmup()
                    _activeAiType.value = AiType.LOCAL
                }
                else -> {
                    _activeAiType.value = AiType.CLOUD
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _activeAiType.value = AiType.CLOUD
        }
    }

    override suspend fun isReady(): Boolean {
        // Circuit breaker check
        val now = Instant.now()
        lastFailureTime?.let {
            if (failureCount >= FAILURE_THRESHOLD && it.plusSeconds(CIRCUIT_BREAKER_WINDOW_HOURS * 3600).isAfter(now)) {
                return false
            }
        }

        return when (_activeAiType.value) {
            AiType.LOCAL -> gemmaClient.isReady() || geminiNanoClient.isReady()
            AiType.CLOUD -> true
            AiType.NONE -> false
        }
    }

    override suspend fun warmup() {
        failureCount = 0
        lastFailureTime = null
        if (gemmaClient.isAvailable()) gemmaClient.warmup()
        if (geminiNanoClient.isSupported()) geminiNanoClient.warmup()
    }

    override suspend fun generate(prompt: String, forceLocal: Boolean): AiResult {
        if (!isReady()) {
            return AiResult.Failure("CIRCUIT_BREAKER_ACTIVE")
        }

        val isGlobalLocalOnly = settingsRepository.isLocalAiOnly.first()
        val shouldForceLocal = forceLocal || isGlobalLocalOnly

        val result = when {
            gemmaClient.isAvailable() -> gemmaClient.generate(prompt)
            geminiNanoClient.isReady() -> geminiNanoClient.generate(prompt)
            shouldForceLocal -> AiResult.Failure("LOCAL_AI_REQUIRED_BUT_UNAVAILABLE")
            _activeAiType.value == AiType.CLOUD -> cloudGeminiClient.generate(prompt)
            else -> AiResult.Failure("NO_AI_CORE_READY")
        }

        if (result is AiResult.Failure) {
            recordFailure()
        } else {
            resetFailures()
        }

        return result
    }

    private fun recordFailure() {
        failureCount++
        lastFailureTime = Instant.now()
    }

    private fun resetFailures() {
        failureCount = 0
        lastFailureTime = null
    }

    @Deprecated("Use generate instead")
    override suspend fun generateContent(prompt: String, forceLocal: Boolean): String {
        return when (val res = generate(prompt, forceLocal)) {
            is AiResult.Success -> res.text
            is AiResult.Failure -> "Neural link unstable. Re-transmitting protocol..."
        }
    }

    fun onModelDownloaded() {
        _activeAiType.value = AiType.LOCAL
    }
}
