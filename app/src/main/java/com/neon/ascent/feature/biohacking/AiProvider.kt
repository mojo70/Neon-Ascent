package com.neon.ascent.feature.biohacking

import android.util.Log
import com.neon.ascent.core.ai.GemmaClient
import com.neon.ascent.core.domain.ai.AiCore
import com.neon.ascent.core.domain.ai.AiResult
import com.neon.ascent.data.repository.SettingsRepository
import kotlinx.coroutines.delay
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

enum class EngineStatus {
    LOCAL_GEMMA_READY,
    LOCAL_NANO_READY,
    CLOUD_READY,
    MODEL_MISSING,
    DOWNLOADING_MODEL,
    ERROR
}

data class AiEngineTelemetry(
    val status: EngineStatus = EngineStatus.MODEL_MISSING,
    val badgeLabel: String = "[MODEL_MISSING]",
    val activeType: AiType = AiType.NONE
)

@Singleton
class AiProvider @Inject constructor(
    private val geminiNanoClient: GeminiNanoClient,
    private val gemmaClient: GemmaClient,
    private val cloudGeminiClient: CloudGeminiClient,
    private val settingsRepository: SettingsRepository
) : AiCore {
    private val _activeAiType = MutableStateFlow(AiType.NONE)
    val activeAiType: StateFlow<AiType> = _activeAiType.asStateFlow()

    private val _engineTelemetry = MutableStateFlow(AiEngineTelemetry())
    val engineTelemetry: StateFlow<AiEngineTelemetry> = _engineTelemetry.asStateFlow()

    suspend fun initialize() {
        try {
            when {
                gemmaClient.isAvailable() -> {
                    gemmaClient.initialize()
                    if (gemmaClient.isReady()) {
                        _activeAiType.value = AiType.LOCAL
                        _engineTelemetry.value = AiEngineTelemetry(
                            status = EngineStatus.LOCAL_GEMMA_READY,
                            badgeLabel = "[GEMMA_2B_LOCAL]",
                            activeType = AiType.LOCAL
                        )
                        return
                    }
                }
                geminiNanoClient.isSupported() -> {
                    geminiNanoClient.warmup()
                    if (geminiNanoClient.isReady()) {
                        _activeAiType.value = AiType.LOCAL
                        _engineTelemetry.value = AiEngineTelemetry(
                            status = EngineStatus.LOCAL_NANO_READY,
                            badgeLabel = "[GEMINI_NANO]",
                            activeType = AiType.LOCAL
                        )
                        return
                    }
                }
            }
            _activeAiType.value = AiType.CLOUD
            _engineTelemetry.value = AiEngineTelemetry(
                status = EngineStatus.MODEL_MISSING,
                badgeLabel = "[CLOUD_GEMINI]",
                activeType = AiType.CLOUD
            )
        } catch (e: Exception) {
            Log.e("AiProvider", "Error initializing AiProvider", e)
            _activeAiType.value = AiType.CLOUD
            _engineTelemetry.value = AiEngineTelemetry(
                status = EngineStatus.ERROR,
                badgeLabel = "[INIT_ERROR]",
                activeType = AiType.NONE
            )
        }
    }

    override suspend fun isReady(): Boolean = true

    override suspend fun warmup() {
        if (gemmaClient.isAvailable()) gemmaClient.warmup()
        if (geminiNanoClient.isSupported()) geminiNanoClient.warmup()
    }

    override suspend fun generate(prompt: String, forceLocal: Boolean): AiResult {
        val isGlobalLocalOnly = settingsRepository.isLocalAiOnly.first()
        val shouldForceLocal = forceLocal || isGlobalLocalOnly

        val failureReasons = mutableListOf<String>()

        // 1. Try Local Gemma if ready
        if (gemmaClient.isAvailable() && gemmaClient.isReady()) {
            when (val localResult = gemmaClient.generate(prompt)) {
                is AiResult.Success -> {
                    _activeAiType.value = AiType.LOCAL
                    _engineTelemetry.value = AiEngineTelemetry(
                        status = EngineStatus.LOCAL_GEMMA_READY,
                        badgeLabel = "[GEMMA_2B_LOCAL]",
                        activeType = AiType.LOCAL
                    )
                    return localResult
                }
                is AiResult.Failure -> failureReasons.add("GEMMA: ${localResult.reason}")
            }
        } else {
            if (!gemmaClient.isAvailable()) {
                failureReasons.add("GEMMA: Model file missing (${gemmaClient.modelPath})")
            } else if (!gemmaClient.isReady()) {
                failureReasons.add("GEMMA: Uninitialized (${gemmaClient.lastInitError ?: "unknown"})")
            }
        }

        // 2. Try Gemini Nano if ready
        if (geminiNanoClient.isReady()) {
            when (val localResult = geminiNanoClient.generate(prompt)) {
                is AiResult.Success -> {
                    _activeAiType.value = AiType.LOCAL
                    _engineTelemetry.value = AiEngineTelemetry(
                        status = EngineStatus.LOCAL_NANO_READY,
                        badgeLabel = "[GEMINI_NANO]",
                        activeType = AiType.LOCAL
                    )
                    return localResult
                }
                is AiResult.Failure -> failureReasons.add("NANO: ${localResult.reason}")
            }
        } else {
            failureReasons.add("NANO: AICore unsupported or uninitialized")
        }

        if (shouldForceLocal) {
            val failureMsg = failureReasons.joinToString(" | ")
            _engineTelemetry.value = AiEngineTelemetry(
                status = EngineStatus.ERROR,
                badgeLabel = "[LOCAL_AI_FAILED]",
                activeType = AiType.LOCAL
            )
            return AiResult.Failure("LOCAL_AI_FAILED ($failureMsg)")
        }

        // 3. Try Cloud Gemini if configured
        val cloudResult = cloudGeminiClient.generate(prompt)
        if (cloudResult is AiResult.Success) {
            _activeAiType.value = AiType.CLOUD
            _engineTelemetry.value = AiEngineTelemetry(
                status = EngineStatus.CLOUD_READY,
                badgeLabel = "[CLOUD_GEMINI]",
                activeType = AiType.CLOUD
            )
            return cloudResult
        } else if (cloudResult is AiResult.Failure) {
            failureReasons.add("CLOUD: ${cloudResult.reason}")
            if (cloudResult.reason != "NO_API_KEY") {
                // Retry only on genuine network failure
                val maxRetries = 2
                for (attempt in 1..maxRetries) {
                    delay(attempt * 200L)
                    val retryResult = cloudGeminiClient.generate(prompt)
                    if (retryResult is AiResult.Success) {
                        _activeAiType.value = AiType.CLOUD
                        _engineTelemetry.value = AiEngineTelemetry(
                            status = EngineStatus.CLOUD_READY,
                            badgeLabel = "[CLOUD_GEMINI]",
                            activeType = AiType.CLOUD
                        )
                        return retryResult
                    }
                }
            }
        }

        // 4. Return explicit failure with detailed reasons
        val combinedError = failureReasons.joinToString(" | ")
        _engineTelemetry.value = AiEngineTelemetry(
            status = EngineStatus.ERROR,
            badgeLabel = "[AI_FAILED]",
            activeType = AiType.NONE
        )
        return AiResult.Failure("ALL_ENGINES_FAILED: $combinedError")
    }

    @Deprecated("Use generate instead")
    override suspend fun generateContent(prompt: String, forceLocal: Boolean): String {
        return when (val res = generate(prompt, forceLocal)) {
            is AiResult.Success -> res.text
            is AiResult.Failure -> {
                val causeMsg = res.cause?.message
                val detail = if (causeMsg != null) "${res.reason} ($causeMsg)" else res.reason
                "ERROR: AI_GENERATION_FAILED [$detail]"
            }
        }
    }

    fun onModelDownloaded() {
        _activeAiType.value = AiType.LOCAL
        _engineTelemetry.value = AiEngineTelemetry(
            status = EngineStatus.LOCAL_GEMMA_READY,
            badgeLabel = "[GEMMA_2B_LOCAL]",
            activeType = AiType.LOCAL
        )
    }
}
