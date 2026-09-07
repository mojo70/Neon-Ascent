package com.neon.ascent.feature.biohacking

import android.content.Context
import android.os.Build
import android.util.Log
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
    private var isChecked = false
    private var isHardwareSupported = false
    private var isModelReady = false

    /**
     * Checks if Gemini Nano / AICore Play Services are supported on this hardware.
     */
    suspend fun isSupported(): Boolean = withContext(Dispatchers.IO) {
        if (isChecked) return@withContext isHardwareSupported

        try {
            // Check Android 14+ / API 34+ requirement for Play Services AICore / ML Kit GenAI
            if (Build.VERSION.SDK_INT >= 34) {
                isHardwareSupported = true
            } else {
                isHardwareSupported = false
            }
        } catch (e: Throwable) {
            Log.w("GeminiNanoClient", "Hardware check for Gemini Nano failed", e)
            isHardwareSupported = false
        } finally {
            isChecked = true
        }
        isHardwareSupported
    }

    /**
     * Returns true if hardware is supported and model/service is bound and ready.
     */
    fun isReady(): Boolean = isHardwareSupported && isModelReady

    suspend fun warmup() = withContext(Dispatchers.IO) {
        if (isSupported()) {
            try {
                // Attempt Play Services AICore bind/warmup check
                isModelReady = false // Model pending download/bind check
            } catch (e: Throwable) {
                Log.w("GeminiNanoClient", "Nano warmup failed", e)
                isModelReady = false
            }
        } else {
            isModelReady = false
        }
    }

    suspend fun generate(prompt: String): AiResult = withContext(Dispatchers.IO) {
        if (!isReady()) {
            return@withContext AiResult.Failure("GEMINI_NANO_NOT_READY (Hardware unsupported or model uninitialized)")
        }

        try {
            // ML Kit GenAI Prompt API / Play Services execution
            AiResult.Failure("GEMINI_NANO_MODEL_PENDING_DOWNLOAD")
        } catch (e: Exception) {
            Log.e("GeminiNanoClient", "Error during Gemini Nano execution", e)
            AiResult.Failure("NANO_GENERATE: ${e.localizedMessage}", e)
        }
    }

    @Deprecated("Use generate instead")
    suspend fun generateContent(prompt: String): String = when (val res = generate(prompt)) {
        is AiResult.Success -> res.text
        is AiResult.Failure -> "ERROR: GEMINI_NANO_FAILED [${res.reason}]"
    }
}
