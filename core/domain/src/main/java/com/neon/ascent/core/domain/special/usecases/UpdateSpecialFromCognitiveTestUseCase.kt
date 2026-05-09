package com.neon.ascent.core.domain.special.usecases

import com.neon.ascent.core.domain.SpecialRepository
import com.neon.ascent.core.domain.model.CognitiveTestSession
import com.neon.ascent.core.domain.model.SpecialAttribute
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.core.domain.special.CognitiveTestEngine
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

/**
 * Single source of truth for updating Intelligence from a cognitive test session.
 * Also logs the raw BenchmarkTest for history & Diagnostics.
 */
class UpdateSpecialFromCognitiveTestUseCase @Inject constructor(
    private val specialRepository: SpecialRepository,
    private val cognitiveTestEngine: CognitiveTestEngine
) {

    /**
     * Call this from the Diagnostics screen after user completes a session.
     */
    suspend operator fun invoke(sessionResult: CognitiveTestSession): SpecialAttribute {
        // 1. Create persistent benchmark record
        val benchmark = cognitiveTestEngine.createBenchmarkFromSession(sessionResult)

        // 2. Persist raw test result (history, re-test comparison, charts)
        specialRepository.saveBenchmark(benchmark)

        // 3. Get current Intelligence attribute
        val currentSpecial = specialRepository.getSpecialAttribute(SpecialType.INTELLIGENCE).first()
            ?: SpecialAttribute(
                type = SpecialType.INTELLIGENCE,
                currentValue = 5,
                percentile = 50
            )

        // 4. Calculate new values (grounded progression)
        val xpGained = calculateXpGain(sessionResult.estimatedPercentile, currentSpecial.percentile)
        val newTotalXp = currentSpecial.totalXp + xpGained

        val newValue = calculateNewAttributeValue(currentSpecial.currentValue, sessionResult.estimatedPercentile)
        val newPercentile = sessionResult.estimatedPercentile

        val updatedSpecial = currentSpecial.copy(
            currentValue = newValue,
            percentile = newPercentile,
            totalXp = newTotalXp,
            lastUpdated = Instant.now()
        )

        // 5. Persist and return
        specialRepository.updateSpecialAttribute(updatedSpecial)

        // Optional: Trigger any linked Missions / Aspirations / Biohacking nodes
        // specialRepository.notifyGoalProgress(SpecialType.INTELLIGENCE, xpGained)

        return updatedSpecial
    }

    /** Grounded XP formula – tunable based on real progression data later */
    private fun calculateXpGain(newPercentile: Int, oldPercentile: Int?): Long {
        val delta = (newPercentile - (oldPercentile ?: 50)).coerceAtLeast(0)
        return (delta * 12L) + 25L   // Base reward + percentile bonus
    }

    /** Clamp 1-10 scale with diminishing returns at high end */
    private fun calculateNewAttributeValue(current: Int, newPercentile: Int): Int {
        val target = when {
            newPercentile >= 90 -> 10
            newPercentile >= 75 -> 9
            newPercentile >= 60 -> 8
            newPercentile >= 45 -> 7
            newPercentile >= 30 -> 6
            else -> 5
        }
        // Slow ramp so users feel steady progress
        return (current + (target - current) / 2).coerceIn(1, 10)
    }
}
