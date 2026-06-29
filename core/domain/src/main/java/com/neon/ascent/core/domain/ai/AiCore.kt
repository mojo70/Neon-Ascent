package com.neon.ascent.core.domain.ai

/**
 * Domain-level interface for AI content generation, abstracting away 
 * specific implementations like Gemma or Gemini.
 */
interface AiCore {
    suspend fun generateContent(prompt: String, forceLocal: Boolean = false): String
}
