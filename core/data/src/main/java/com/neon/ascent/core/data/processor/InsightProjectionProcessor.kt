package com.neon.ascent.core.data.processor

import android.util.Log
import com.neon.ascent.core.data.local.dao.InsightDao
import com.neon.ascent.core.data.local.entity.ActionEventEntity
import com.neon.ascent.core.data.local.entity.BiometricEventEntity
import com.neon.ascent.core.data.local.entity.SocraticInsightEntity
import com.neon.ascent.core.domain.ai.AiCore
import com.neon.ascent.core.domain.ai.AiResult
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Materializes projections from raw Biometric and Action events using a combination 
 * of rule-based logic and LLM synthesis.
 */
@Singleton
class InsightProjectionProcessor @Inject constructor(
    private val insightDao: InsightDao,
    private val aiCore: AiCore
) {
    private val version = 1 // Current schema/prompt version
    
    // In a real implementation, this would be persisted in DataStore/Database
    private var lastProcessedEventId: Long = 0

    suspend fun processProjections() {
        val now = Instant.now()
        val windowStart = now.minus(24, ChronoUnit.HOURS)

        // 1. Fetch relevant events for the 24h window
        val biometricEvents = insightDao.getBiometricEventsInRange(windowStart, now).first()
        val actionEvents = insightDao.getAllActionEvents().first()
            .filter { it.timestamp.isAfter(windowStart) }

        if (biometricEvents.isEmpty() && actionEvents.isEmpty()) return

        // 2. Extract Watermark (Last Event ID)
        val maxBiometricId = biometricEvents.maxOfOrNull { it.id } ?: 0L
        val maxActionId = actionEvents.maxOfOrNull { it.id } ?: 0L
        val currentMaxId = maxOf(maxBiometricId, maxActionId)

        if (currentMaxId <= lastProcessedEventId) {
            Log.d("InsightProcessor", "No new events since $lastProcessedEventId. Skipping.")
            return
        }

        // 3. Lightweight Heuristic Rules (Fast path)
        val ruleFindings = runHeuristicRules(biometricEvents, actionEvents)

        // 4. Gemma Synthesis (Deep path)
        val aiSynthesis = synthesizeWithAi(biometricEvents, actionEvents, ruleFindings)

        // 5. Materialize and Persist
        val newInsight = SocraticInsightEntity(
            generatedAt = now,
            timeWindowStart = windowStart,
            timeWindowEnd = now,
            content = aiSynthesis,
            basedOnEventIds = biometricEvents.map { it.id } + actionEvents.map { it.id },
            version = version
        )

        insightDao.insertInsight(newInsight)
        lastProcessedEventId = currentMaxId
        
        Log.i("InsightProcessor", "Materialized new Socratic Insight version $version")
    }

    private fun runHeuristicRules(
        biometrics: List<BiometricEventEntity>,
        actions: List<ActionEventEntity>
    ): List<String> {
        val findings = mutableListOf<String>()

        // HRV Recovery Check
        val recentHrv = biometrics.filter { it.type == "HRV" }
        if (recentHrv.isNotEmpty()) {
            val avgHrv = recentHrv.map { it.value }.average()
            if (avgHrv < 40) {
                findings.add("LOW_RECOVERY_SIGNAL: HRV average $avgHrv is below baseline.")
            }
        }

        // Habit Consistency Check
        val completions = actions.filter { it.actionType == "HABIT_COMPLETION" }
        if (completions.size >= 3) {
            findings.add("HIGH_MOMENTUM: $completions.size protocols executed in last 24h.")
        }

        return findings
    }

    private suspend fun synthesizeWithAi(
        biometrics: List<BiometricEventEntity>,
        actions: List<ActionEventEntity>,
        ruleFindings: List<String>
    ): String {
        val fallback = "DATA_LINK_STABLE: System processing ongoing. " + (ruleFindings.firstOrNull() ?: "Maintain protocol.")
        
        if (!aiCore.isReady()) {
            Log.d("InsightProcessor", "AI Core not ready, using heuristic fallback.")
            return fallback
        }

        val prompt = StringBuilder()
        prompt.append("Analyze the following biometric and action data for a cyberpunk high-performer.\n")
        prompt.append("Return a concise, Socratic insight (1-2 sentences) about their current state.\n\n")
        
        prompt.append("### RECENT DATA\n")
        biometrics.take(10).forEach { 
            prompt.append("- ${it.type}: ${it.value} at ${it.timestamp}\n")
        }
        actions.take(5).forEach {
            prompt.append("- Action: ${it.actionType} (${it.content}) at ${it.timestamp}\n")
        }
        
        prompt.append("\n### SYSTEM HEURISTICS\n")
        ruleFindings.forEach { prompt.append("- $it\n") }

        prompt.append("\nInsight Protocol:")

        return when (val result = aiCore.generate(prompt.toString(), forceLocal = true)) {
            is AiResult.Success -> result.text
            is AiResult.Failure -> {
                Log.w("InsightProcessor", "AI Synthesis failed: ${result.reason}")
                fallback
            }
        }
    }

    fun getLatestInsight() = insightDao.getLatestInsight()
}
