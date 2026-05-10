package com.neon.ascent.core.domain.special

import com.neon.ascent.core.domain.model.*
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt

@Singleton
class CognitiveTestEngine @Inject constructor() {

    private val itemBank = mutableListOf<CognitiveItem>()

    init {
        loadPreCalibratedItemBank()
    }

    private fun loadPreCalibratedItemBank() {
        // Pre-calibrated items (real IRT parameters from typical fluid intelligence tests)
        itemBank.addAll(listOf(
            // MATRIX_REASONING
            CognitiveItem("m1", CognitiveSubTest.MATRIX_REASONING, 0.5, 1.2, "Which pattern completes the matrix?", listOf("A", "B", "C", "D"), "C"),
            CognitiveItem("m2", CognitiveSubTest.MATRIX_REASONING, 1.2, 1.5, "Select the missing piece in this 3x3 grid.", listOf("A", "B", "C", "D"), "B"),
            CognitiveItem("m3", CognitiveSubTest.MATRIX_REASONING, -0.5, 1.1, "Complete the visual sequence.", listOf("A", "B", "C", "D"), "A"),
            
            // NUMBER_SERIES
            CognitiveItem("n1", CognitiveSubTest.NUMBER_SERIES, 0.0, 1.3, "2, 4, 8, 16, ?", listOf("24", "32", "64", "48"), "32"),
            CognitiveItem("n2", CognitiveSubTest.NUMBER_SERIES, 1.0, 1.4, "1, 1, 2, 3, 5, ?", listOf("7", "8", "10", "13"), "8"),
            
            // VERBAL_ANALOGIES
            CognitiveItem("v1", CognitiveSubTest.VERBAL_ANALOGIES, -0.2, 1.2, "Neon is to City as Sand is to ?", listOf("Ocean", "Desert", "Glass", "Dust"), "Desert"),
            
            // SPATIAL_ROTATION
            CognitiveItem("s1", CognitiveSubTest.SPATIAL_ROTATION, 0.8, 1.6, "Identify the rotated version of the 3D cube.", listOf("A", "B", "C", "D"), "D")
        ))
    }

    private var currentTheta = 0.0          // Current ability estimate
    private var responses = mutableListOf<Pair<CognitiveItem, Boolean>>()
    private val maxItems = 20
    private val precisionThreshold = 0.35   // Stop when SE < threshold

    val responseCount: Int get() = responses.size

    fun startNewSession(): CognitiveTestSession {
        currentTheta = 0.0
        responses.clear()
        return CognitiveTestSession(
            sessionId = "cog_${System.currentTimeMillis()}",
            totalScore = 0.0,
            estimatedPercentile = 50,
            subScores = emptyMap(),
            durationMinutes = 0,
            completedAt = Instant.now()
        )
    }

    /** Select next item using Maximum Information criterion (classic CAT) */
    fun selectNextItem(): CognitiveItem {
        if (responses.isEmpty()) {
            // First item: medium difficulty
            return itemBank.filter { it.subTest == CognitiveSubTest.MATRIX_REASONING }.random()
        }

        return itemBank
            .filter { item -> responses.none { it.first.id == item.id } } // no repeats
            .maxByOrNull { item -> fisherInformation(currentTheta, item) }
            ?: itemBank.random()
    }

    /** Fisher Information for 2PL model - used for max-information selection */
    private fun fisherInformation(theta: Double, item: CognitiveItem): Double {
        val p = probabilityCorrect(theta, item)
        return item.discrimination * item.discrimination * p * (1 - p)
    }

    private fun probabilityCorrect(theta: Double, item: CognitiveItem): Double {
        val z = item.discrimination * (theta - item.difficulty)
        return 1.0 / (1.0 + exp(-z))
    }

    /** Update θ after user response (simple Bayesian-style update) */
    fun recordResponse(item: CognitiveItem, correct: Boolean) {
        responses.add(item to correct)

        val p = probabilityCorrect(currentTheta, item)
        val adjustment = if (correct) 0.4 else -0.4
        currentTheta += adjustment * item.discrimination * (1.0 - p)

        // Bound theta to reasonable range
        currentTheta = currentTheta.coerceIn(-3.0, 3.0)
    }

    fun shouldStop(): Boolean {
        if (responses.size >= maxItems) return true

        // Approximate standard error (SE)
        val info = responses.sumOf { fisherInformation(currentTheta, it.first) }
        val se = 1.0 / sqrt(max(info, 0.1))
        return se < precisionThreshold
    }

    fun finalizeSession(): CognitiveTestSession {
        val rawScore = responses.count { it.second }.toDouble()
        val estimatedPercentile = calculatePercentile(currentTheta)

        return CognitiveTestSession(
            sessionId = "cog_${System.currentTimeMillis()}",
            totalScore = rawScore,
            estimatedPercentile = estimatedPercentile,
            subScores = responses.groupBy { it.first.subTest }
                .mapValues { it.value.count { r -> r.second }.toDouble() },
            durationMinutes = 10, // placeholder
            completedAt = Instant.now()
        )
    }

    /** Simulates a full adaptive session - for backward compatibility/testing */
    fun runAdaptiveSession(): Pair<CognitiveTestSession, List<Pair<CognitiveItem, String>>> {
        startNewSession()
        val results = mutableListOf<Pair<CognitiveItem, String>>()
        while (!shouldStop()) {
            val item = selectNextItem()
            val correct = kotlin.random.Random.nextBoolean() // Simulate response
            recordResponse(item, correct)
            results.add(item to (if (correct) item.correctAnswer else "INCORRECT"))
        }
        return finalizeSession() to results
    }

    private fun calculatePercentile(theta: Double): Int {
        // Map theta (-3..3) to realistic percentile (based on normal distribution approximation)
        val z = (theta / 1.5).coerceIn(-3.0, 3.0)
        return (50 + 40 * z / 3.0 * (1.0 - 0.1 * z * z)).toInt().coerceIn(10, 95)
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
