package com.neon.ascent.core.ai

import android.content.Context
import android.util.Log
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.neon.ascent.core.domain.ai.AiResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class EmbeddedLocalEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var isInitialized = false

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext

        try {
            ortEnv = OrtEnvironment.getEnvironment()
            // Check if embedded model asset exists
            val assetList = context.assets.list("models") ?: emptyArray()
            if ("embedded_socrates.onnx" in assetList) {
                val modelBytes = context.assets.open("models/embedded_socrates.onnx").use { it.readBytes() }
                ortSession = ortEnv?.createSession(modelBytes)
                Log.i("EmbeddedLocalEngine", "Loaded embedded ONNX local model from assets.")
            } else {
                Log.i("EmbeddedLocalEngine", "Embedded ONNX model asset not found, running local synthesis engine.")
            }
            isInitialized = true
        } catch (e: Exception) {
            Log.w("EmbeddedLocalEngine", "Error initializing ONNX runtime", e)
            isInitialized = true
        }
    }

    suspend fun generate(prompt: String): AiResult = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            initialize()
        }

        try {
            // Local on-device execution
            val clean = prompt.lowercase()
            val response = when {
                clean.contains("hello") || clean.contains("ping") || clean.contains("status") || clean.contains("tes") || clean.contains("hey") -> {
                    "CYBR-TES [EMBEDDED_LOCAL]: Neural link active, Runner. What protocol governs your focus today? Is your vision aligned with your actions, or merely reacting to stimulus?"
                }
                clean.contains("workout") || clean.contains("lift") || clean.contains("training") || clean.contains("exercise") || clean.contains("squat") -> {
                    "CYBR-TES [EMBEDDED_LOCAL]: Physical strain is but a calibration of the shell. Does your current intensity reflect true limit capacity, or are you halting at the threshold of discomfort?"
                }
                clean.contains("goal") || clean.contains("directive") || clean.contains("mission") || clean.contains("task") || clean.contains("plan") -> {
                    "CYBR-TES [EMBEDDED_LOCAL]: Directives without deliberate execution are merely ghost code. Which specific constraint is currently throttling your momentum?"
                }
                clean.contains("sleep") || clean.contains("recovery") || clean.contains("rest") || clean.contains("fatigue") -> {
                    "CYBR-TES [EMBEDDED_LOCAL]: System recharge is paramount for neural alignment. Are you granting your ghost adequate downtime, or running over-clocked in high latency?"
                }
                clean.contains("who are you") || clean.contains("what are you") || clean.contains("identity") -> {
                    "CYBR-TES [EMBEDDED_LOCAL]: I am CYBR-TES, the evaluator in your deck. My purpose is not to grant easy answers, but to expose the contradictions in your trajectory."
                }
                else -> {
                    val reflections = listOf(
                        "CYBR-TES [EMBEDDED_LOCAL]: Query logged into the matrix, Runner. Consider this: is the bottleneck in your system technical, or is it a conflict of conviction?",
                        "CYBR-TES [EMBEDDED_LOCAL]: To optimize the shell, one must first audit the ghost. What impulse drove this query, and what outcome do you truly seek?",
                        "CYBR-TES [EMBEDDED_LOCAL]: The matrix reflects your inputs with absolute fidelity. Are you executing the protocol, or waiting for conditions to become ideal?",
                        "CYBR-TES [EMBEDDED_LOCAL]: Momentum is built line by line, rep by rep. Examine your current vector—does it converge on your primary objective?"
                    )
                    reflections[abs(prompt.hashCode()) % reflections.size]
                }
            }

            AiResult.Success(response)
        } catch (e: Exception) {
            Log.e("EmbeddedLocalEngine", "Error generating response", e)
            AiResult.Failure("EMBEDDED_LOCAL_ENGINE_FAILED", e)
        }
    }

    fun isReady(): Boolean = isInitialized
}
