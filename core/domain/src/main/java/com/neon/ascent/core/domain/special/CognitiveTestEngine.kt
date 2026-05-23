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
        loadHighQualityItemBank()
    }

    private fun loadHighQualityItemBank() {
        itemBank.addAll(listOf(
            // ====================== MATRIX REASONING - RAVEN'S STYLE (10 items) ======================
            CognitiveItem("rpm_m1", CognitiveSubTest.MATRIX_REASONING, 0.6, 1.2,
                "Each row increases the number of shapes by one. What completes the bottom row?",
                listOf("A", "B", "C", "D"), "C"),

            CognitiveItem("rpm_m2", CognitiveSubTest.MATRIX_REASONING, 1.1, 1.5,
                "The pattern rotates 90° clockwise in each column. Find the missing cell.",
                listOf("A", "B", "C", "D"), "B"),

            CognitiveItem("rpm_m3", CognitiveSubTest.MATRIX_REASONING, 1.3, 1.6,
                "Each element rotates by a progressive multiple of 45°. Select the missing element.",
                listOf("A", "B", "C", "D"), "C"),

            CognitiveItem("rpm_m4", CognitiveSubTest.MATRIX_REASONING, 1.6, 1.8,
                "The row follows an XOR logic operation on visual elements. Select the missing cell.",
                listOf("A", "B", "C", "D"), "A"),

            CognitiveItem("rpm_hr1", CognitiveSubTest.MATRIX_REASONING, 1.9, 2.0,
                "The grid follows a meta-rule where each quadrant applies a different transformation (rotation, inversion, addition). Identify the missing cell.",
                listOf("A", "B", "C", "D"), "A"),

            CognitiveItem("rpm_m5", CognitiveSubTest.MATRIX_REASONING, 2.0, 2.1,
                "Elements shift outward from the center in a logarithmic spiral. Identify the missing cell.",
                listOf("A", "B", "C", "D"), "D"),

            CognitiveItem("rpm_m6", CognitiveSubTest.MATRIX_REASONING, 2.2, 2.2,
                "The grid represents a 2D cellular automaton rule state. What completes the sequence?",
                listOf("A", "B", "C", "D"), "B"),

            CognitiveItem("rpm_hr2", CognitiveSubTest.MATRIX_REASONING, 2.4, 2.1,
                "Symmetry is broken systematically across both diagonals. The correct option restores balance without introducing new elements.",
                listOf("A", "B", "C", "D"), "C"),

            CognitiveItem("rpm_hr3", CognitiveSubTest.MATRIX_REASONING, 2.6, 2.2,
                "The pattern is defined by the progressive removal of symmetry. What completes the final position?",
                listOf("A", "B", "C", "D"), "B"),

            CognitiveItem("m_hr1", CognitiveSubTest.MATRIX_REASONING, 1.8, 1.9,
                "The grid follows a meta-rule of symmetry breaking across diagonals.",
                listOf("A", "B", "C", "D"), "A"),

            // ====================== NUMBER SERIES (8 items) ======================
            CognitiveItem("rpm_n1", CognitiveSubTest.NUMBER_SERIES, 0.7, 1.4,
                "2, 5, 10, 17, 26, ?", listOf("A: 35", "B: 37", "C: 42", "D: 39"), "B"),

            CognitiveItem("rpm_n2", CognitiveSubTest.NUMBER_SERIES, 1.1, 1.3,
                "3, 6, 11, 18, 27, ?", listOf("A: 36", "B: 38", "C: 39", "D: 41"), "B"),

            CognitiveItem("rpm_n3", CognitiveSubTest.NUMBER_SERIES, 1.5, 1.5,
                "2, 3, 5, 7, 11, 13, 17, ?", listOf("A: 19", "B: 21", "C: 23", "D: 20"), "A"),

            CognitiveItem("rpm_n4", CognitiveSubTest.NUMBER_SERIES, 1.8, 1.7,
                "1, 2, 4, 7, 12, 20, 33, ?", listOf("A: 45", "B: 54", "C: 51", "D: 49"), "B"),

            CognitiveItem("rpm_n_hr1", CognitiveSubTest.NUMBER_SERIES, 2.2, 2.0,
                "1, 2, 6, 42, 1806, ?", listOf("A: 3263442", "B: 3263443", "C: 108360", "D: 1807"), "A"),

            CognitiveItem("rpm_n5", CognitiveSubTest.NUMBER_SERIES, 2.3, 2.1,
                "3, 4, 12, 48, 576, ?", listOf("A: 27648", "B: 11520", "C: 24576", "D: 28800"), "A"),

            CognitiveItem("n1", CognitiveSubTest.NUMBER_SERIES, 0.6, 1.3,
                "2, 4, 8, 16, ?", listOf("A: 24", "B: 32", "C: 64", "D: 18"), "B"),

            CognitiveItem("n_hr2", CognitiveSubTest.NUMBER_SERIES, 2.4, 2.0,
                "7, 15, 31, 63, 127, ?", listOf("A: 255", "B: 254", "C: 256", "D: 128"), "A"),

            // ====================== VERBAL ANALOGIES (7 items) ======================
            CognitiveItem("rpm_v1", CognitiveSubTest.VERBAL_ANALOGIES, 1.2, 1.6,
                "Tree : Forest :: Neuron : ?", 
                listOf("A: Brain", "B: Synapse", "C: Mind", "D: Network"), "D"),

            CognitiveItem("rpm_v2", CognitiveSubTest.VERBAL_ANALOGIES, 1.1, 1.3,
                "Pencil : Paper :: Cursor : ?", 
                listOf("A: Mouse", "B: Screen", "C: Keyboard", "D: File"), "B"),

            CognitiveItem("rpm_v3", CognitiveSubTest.VERBAL_ANALOGIES, 1.4, 1.5,
                "Symphony : Composer :: Program : ?", 
                listOf("A: Computer", "B: Keyboard", "C: Developer", "D: Compiler"), "C"),

            CognitiveItem("rpm_v4", CognitiveSubTest.VERBAL_ANALOGIES, 1.7, 1.7,
                "Time : Clock :: Gravity : ?", 
                listOf("A: Mass", "B: Pendulum", "C: Falling", "D: Space"), "B"),

            CognitiveItem("rpm_v_hr1", CognitiveSubTest.VERBAL_ANALOGIES, 2.1, 1.9,
                "Chaos : Order :: Entropy : ?", 
                listOf("A: Energy", "B: Information", "C: Structure", "D: Heat"), "B"),

            CognitiveItem("rpm_v5", CognitiveSubTest.VERBAL_ANALOGIES, 2.2, 2.0,
                "Epistemology : Knowledge :: Ontology : ?", 
                listOf("A: Being", "B: Origin", "C: Truth", "D: Values"), "A"),

            CognitiveItem("v_hr1", CognitiveSubTest.VERBAL_ANALOGIES, 2.0, 1.8,
                "Infinity : Paradox :: Eternity : ?", 
                listOf("A: Time", "B: Recursion", "C: Mortality", "D: Singularity"), "B"),

            // ====================== SPATIAL ROTATION (6 items) ======================
            CognitiveItem("rpm_s1", CognitiveSubTest.SPATIAL_ROTATION, 1.3, 1.7,
                "Which option shows the L-shaped figure after 90° clockwise rotation?",
                listOf("A", "B", "C", "D"), "C"),

            CognitiveItem("rpm_s2", CognitiveSubTest.SPATIAL_ROTATION, 1.4, 1.5,
                "Identify the mirror image of the original shape after 180° rotation.",
                listOf("A", "B", "C", "D"), "A"),

            CognitiveItem("rpm_s3", CognitiveSubTest.SPATIAL_ROTATION, 1.8, 1.7,
                "Which option matches the pattern after a 3D rotation along the diagonal axis?",
                listOf("A", "B", "C", "D"), "C"),

            CognitiveItem("rpm_s4", CognitiveSubTest.SPATIAL_ROTATION, 2.1, 1.9,
                "Determine the correct projection of a folded tesseract net onto 3D space.",
                listOf("A", "B", "C", "D"), "D"),

            CognitiveItem("rpm_s_hr1", CognitiveSubTest.SPATIAL_ROTATION, 2.3, 2.0,
                "Visualize folding this net into a 3D polyhedron. Which face ends up opposite the marked one?",
                listOf("A", "B", "C", "D"), "B"),

            CognitiveItem("s_hr1", CognitiveSubTest.SPATIAL_ROTATION, 2.2, 1.9,
                "Visualize folding this net into a polyhedron. Which face is opposite the marked one?",
                listOf("A", "B", "C", "D"), "B")
        ))
    }

    private var currentTheta = 0.0
    private val responses = mutableListOf<Pair<CognitiveItem, Boolean>>()
    private val usedItemIds = mutableSetOf<String>()
    private val maxItems = 22

    val responseCount: Int get() = responses.size

    fun startNewSession(): CognitiveTestSession {
        currentTheta = 0.0
        responses.clear()
        usedItemIds.clear()
        return CognitiveTestSession(
            sessionId = "cog_${System.currentTimeMillis()}",
            totalScore = 0.0,
            estimatedPercentile = 50,
            subScores = emptyMap(),
            durationMinutes = 0,
            completedAt = Instant.now()
        )
    }

    fun selectNextItem(): CognitiveItem {
        if (responses.isEmpty()) {
            currentTheta = 0.0
            usedItemIds.clear()
            val firstItem = itemBank.filter { it.difficulty in 0.6..1.4 }.randomOrNull() ?: itemBank.random()
            usedItemIds.add(firstItem.id)
            return firstItem
        }

        // Filter out already used items
        val availableItems = itemBank.filter { item -> 
            item.id !in usedItemIds 
        }

        if (availableItems.isEmpty()) {
            // Fallback: reset if we somehow run out (very unlikely)
            usedItemIds.clear()
            val randomItem = itemBank.random()
            usedItemIds.add(randomItem.id)
            return randomItem
        }

        val bestItem = availableItems.maxByOrNull { item ->
            val info = fisherInformation(currentTheta, item)
            info * (1.0 + 0.18 * max(0.0, item.difficulty - 1.3))
        } ?: availableItems.random()

        usedItemIds.add(bestItem.id)
        return bestItem
    }

    private fun fisherInformation(theta: Double, item: CognitiveItem): Double {
        val p = probabilityCorrect(theta, item)
        return item.discrimination * item.discrimination * p * (1 - p)
    }

    private fun probabilityCorrect(theta: Double, item: CognitiveItem): Double {
        val z = item.discrimination * (theta - item.difficulty)
        return 1.0 / (1.0 + exp(-z))
    }

    fun recordResponse(item: CognitiveItem, correct: Boolean) {
        responses.add(item to correct)
        val p = probabilityCorrect(currentTheta, item)
        val direction = if (correct) 0.38 else -0.42
        currentTheta += direction * item.discrimination * (1.0 - p)

        if (currentTheta > 1.6 && correct) currentTheta += 0.09 // High-end boost
        currentTheta = currentTheta.coerceIn(-3.0, 3.5)
    }

    fun shouldStop(): Boolean {
        if (responses.size >= maxItems) return true
        val totalInfo = responses.sumOf { fisherInformation(currentTheta, it.first) }
        val se = 1.0 / sqrt(max(totalInfo, 0.2))
        return se < 0.33
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
            durationMinutes = 10,
            completedAt = Instant.now()
        )
    }

    private fun calculatePercentile(theta: Double): Int {
        val z = (theta / 1.4).coerceIn(-3.0, 3.5)
        return (50 + 42 * z).toInt().coerceIn(10, 98)
    }

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
