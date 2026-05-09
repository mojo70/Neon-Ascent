package com.neon.ascent.core.domain.model

import java.time.Instant

/** S.P.E.C.I.A.L. Core Model - All stats are grounded in real-world norms */
data class SpecialAttribute(
    val type: SpecialType,
    val baseValue: Int = 5,           // 1-10 starting scale from intake
    val currentValue: Int,
    val percentile: Int?,             // e.g. 72 = 72nd percentile for age/sex group
    val lastUpdated: Instant = Instant.now(),
    val totalXp: Long = 0L
)

enum class SpecialType {
    STRENGTH, PERCEPTION, ENDURANCE, CHARISMA, INTELLIGENCE, AGILITY, LUCK;

    fun getIcon(): String = when (this) {
        STRENGTH -> "💪"
        PERCEPTION -> "👁"
        ENDURANCE -> "🛡️"
        CHARISMA -> "🗣"
        INTELLIGENCE -> "🧠"
        AGILITY -> "⚡"
        LUCK -> "🍀"
    }
}

/** Raw benchmark / test result - stored for history & re-testing */
data class BenchmarkTest(
    val id: String,
    val attribute: SpecialType,
    val testType: TestType,
    val rawScore: Double,
    val normalizedScore: Double,      // 0.0 - 1.0 or percentile
    val percentile: Int?,
    val metadata: Map<String, String> = emptyMap(), // e.g. "age": "35", "sex": "M"
    val timestamp: Instant = Instant.now(),
    val source: DataSource
)

enum class TestType {
    PHYSICAL_SELF_REPORT,
    QUIZ_MBTI,
    WEARABLE_DERIVED,
    COGNITIVE_ADAPTIVE     // ← New for Intelligence
}

enum class DataSource {
    INTAKE, HEALTH_CONNECT, MANUAL_LOG, COGNITIVE_TEST
}

// ====================== INTELLIGENCE-SPECIFIC ======================

/** Cognitive test item (fluid intelligence focused) */
data class CognitiveItem(
    val id: String,
    val type: CognitiveSubTest,
    val difficulty: Int,           // 1-10
    val question: String,          // Text or describe visual (we'll render accordingly)
    val options: List<String>? = null,
    val correctAnswer: String,
    val explanation: String? = null // for post-test feedback
)

enum class CognitiveSubTest {
    MATRIX_REASONING,      // Visual pattern completion
    NUMBER_SERIES,         // Quantitative
    VERBAL_ANALOGIES,      // Verbal
    SPATIAL_ROTATION       // 3D mental rotation
}

/** Session result used by CognitiveTestEngine */
data class CognitiveTestSession(
    val sessionId: String,
    val totalScore: Double,        // 0-100 raw
    val estimatedPercentile: Int,
    val subScores: Map<CognitiveSubTest, Double>,
    val durationMinutes: Int,
    val completedAt: Instant
)
