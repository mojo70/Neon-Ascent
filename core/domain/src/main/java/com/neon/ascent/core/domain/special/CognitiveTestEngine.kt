package com.neon.ascent.core.domain.special

import com.neon.ascent.core.domain.model.BenchmarkTest
import com.neon.ascent.core.domain.model.CognitiveItem
import com.neon.ascent.core.domain.model.CognitiveSubTest
import com.neon.ascent.core.domain.model.CognitiveTestSession
import com.neon.ascent.core.domain.model.DataSource
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.core.domain.model.TestType
import java.time.Instant
import javax.inject.Inject

/**
 * Adaptive Cognitive Test Engine - Grounded Intelligence measurement
 *
 * Inspired by high-range item styles (matrix, series, analogies) but kept short,
 * mobile-friendly, and built with real psychometric principles (adaptive difficulty,
 * multiple sub-tests targeting fluid g).
 *
 * Scoring uses simple IRT-lite + published norm approximations (WAIS-IV / Raven's style).
 * All data stays on-device.
 */
class CognitiveTestEngine @Inject constructor() {

    private val itemBank = mutableListOf<CognitiveItem>() // populated in init or from assets/DB

    init {
        loadSampleItemBank() // Replace with real item loading (JSON assets or Room later)
    }

    private fun loadSampleItemBank() {
        // TODO: Load 60-80 high-quality items from encrypted assets or pre-seeded DB
        // For MVP we seed a few. Expand this aggressively.
        itemBank.addAll(listOf(
            // MATRIX_REASONING examples
            CognitiveItem("m1", CognitiveSubTest.MATRIX_REASONING, 3,
                "What pattern completes the 3x3 grid?", null, "C", null),
            // Add 15-20 real items per category in final version
        ))
    }

    /**
     * Run a full adaptive session (~8-12 minutes)
     * Returns session result + raw answers for logging
     */
    suspend fun runAdaptiveSession(
        targetDurationMin: Int = 10
    ): Pair<CognitiveTestSession, List<Pair<CognitiveItem, String>>> {
        val answers = mutableListOf<Pair<CognitiveItem, String>>()
        var currentDifficulty = 5
        var score = 0.0
        val subScores = mutableMapOf<CognitiveSubTest, Double>()

        val startTime = System.currentTimeMillis()

        // 20-25 items total, balanced across sub-tests
        val testPlan = generateTestPlan()

        for (item in testPlan) {
            val presentedItem = getNextItem(item, currentDifficulty)
            // In real UI: show item, get user answer
            val userAnswer = "simulate_user_answer" // ← replaced by actual UI response

            answers.add(presentedItem to userAnswer)

            val isCorrect = userAnswer == presentedItem.correctAnswer
            if (isCorrect) {
                score += (1.0 + (presentedItem.difficulty * 0.15))
                currentDifficulty = minOf(10, currentDifficulty + 1)
            } else {
                currentDifficulty = maxOf(1, currentDifficulty - 1)
            }

            subScores[item] = (subScores[item] ?: 0.0) + if (isCorrect) 1.0 else 0.0
        }

        val durationMin = ((System.currentTimeMillis() - startTime) / 60000).toInt()
        val estimatedPercentile = calculatePercentile(score, testPlan.size)

        val session = CognitiveTestSession(
            sessionId = "cog_${System.currentTimeMillis()}",
            totalScore = score,
            estimatedPercentile = estimatedPercentile,
            subScores = subScores,
            durationMinutes = durationMin,
            completedAt = Instant.now()
        )

        return session to answers
    }

    private fun generateTestPlan(): List<CognitiveSubTest> {
        // Balanced mix - can be made more sophisticated
        return listOf(
            CognitiveSubTest.MATRIX_REASONING,
            CognitiveSubTest.NUMBER_SERIES,
            CognitiveSubTest.VERBAL_ANALOGIES,
            CognitiveSubTest.SPATIAL_ROTATION
        ).flatMap { type -> List(5) { type } }.shuffled()
    }

    private fun getNextItem(subTest: CognitiveSubTest, difficulty: Int): CognitiveItem {
        val candidates = itemBank.filter {
            it.type == subTest && it.difficulty in (difficulty-1)..(difficulty+1)
        }
        return candidates.randomOrNull() ?: itemBank.first { it.type == subTest }
    }

    /** Simple norm-based percentile conversion (replace with real lookup table later) */
    private fun calculatePercentile(rawScore: Double, itemCount: Int): Int {
        val accuracy = (rawScore / itemCount) * 100
        // Real version: use age/sex/education stratified tables from WAIS/Raven norms
        return when {
            accuracy >= 90 -> 95
            accuracy >= 75 -> 82
            accuracy >= 60 -> 68
            accuracy >= 45 -> 52
            else -> 35
        }
    }

    /** Use this after test to update S.P.E.C.I.A.L. */
    fun createBenchmarkFromSession(
        session: CognitiveTestSession
    ): BenchmarkTest {
        return BenchmarkTest(
            id = session.sessionId,
            attribute = SpecialType.INTELLIGENCE,
            testType = TestType.COGNITIVE_ADAPTIVE,
            rawScore = session.totalScore,
            normalizedScore = session.totalScore / 100.0,
            percentile = session.estimatedPercentile,
            metadata = mapOf("duration" to session.durationMinutes.toString()),
            source = DataSource.COGNITIVE_TEST
        )
    }
}
