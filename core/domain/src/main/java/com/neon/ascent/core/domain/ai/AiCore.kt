package com.neon.ascent.core.domain.ai

/**
 * Domain-level interface for AI content generation, abstracting away 
 * specific implementations like Gemma or Gemini.
 */
interface AiCore {
    @Deprecated("Use generate instead", ReplaceWith("generate(prompt, forceLocal)"))
    suspend fun generateContent(prompt: String, forceLocal: Boolean = false): String

    /**
     * Generates content using the active AI core.
     */
    suspend fun generate(prompt: String, forceLocal: Boolean = false): AiResult

    /**
     * Sends [prompt] unchanged for a surface that supplies its own instructions.
     * Implementations must not prepend a persona or replace the reply with one.
     * The default forwards to [generate] so test doubles keep working.
     * Production routing for this method skips persona synthesizers.
     */
    suspend fun generateIsolated(prompt: String): AiResult = generate(prompt, forceLocal = true)

    /**
     * Checks if the AI core is ready to process requests (warmed up and not circuit-broken).
     */
    suspend fun isReady(): Boolean

    /**
     * Attempts to initialize or warm up the AI core.
     */
    suspend fun warmup()
}

sealed class AiResult {
    data class Success(val text: String) : AiResult()
    data class Failure(val reason: String, val cause: Throwable? = null) : AiResult()
}
