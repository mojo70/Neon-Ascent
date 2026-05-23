package com.neon.ascent.feature.terminal.ui

import androidx.compose.ui.graphics.Color
import com.neon.ascent.core.common.NeonBlue
import com.neon.ascent.core.common.NeonCyan
import com.neon.ascent.core.common.NeonPink
import kotlin.random.Random

data class MatrixPattern(
    val cells: List<CellPattern>,
    val options: List<CellPattern>,
    val correctOption: Int
)

data class CellPattern(
    val shape: ShapeType,
    val color: Color,
    val rotation: Int, // 0, 90, 180, 270
    val size: Float = 1f
) {
    fun getVisualSignature(): String {
        val normalizedRotation = when (shape) {
            ShapeType.CIRCLE -> 0
            ShapeType.SQUARE -> rotation % 90
            ShapeType.HEXAGON -> rotation % 60
            else -> rotation % 360
        }
        return "${shape.name}_${color.hashCode()}_${normalizedRotation}_${String.format(java.util.Locale.US, "%.2f", size)}"
    }
}

enum class ShapeType { CIRCLE, SQUARE, TRIANGLE, STAR, HEXAGON }

object MatrixPatternGenerator {

    fun generatePattern(difficulty: Double, targetCorrectIndex: Int): MatrixPattern {
        val seed = Random.nextInt()
        val patternType = when (difficulty) {
            in 0.0..1.0 -> PatternType.LINEAR
            in 1.0..1.8 -> PatternType.ROTATION
            else -> PatternType.META
        }

        val cells = when (patternType) {
            PatternType.LINEAR -> generateLinearPattern(seed)
            PatternType.ROTATION -> generateRotationPattern(seed)
            PatternType.META -> generateMetaPattern(seed)
        }

        return populateOptions(cells, seed, targetCorrectIndex)
    }

    private fun populateOptions(cells: List<CellPattern>, seed: Int, targetCorrectIndex: Int): MatrixPattern {
        val correctCell = cells[8]
        val rand = Random(seed)

        val uniqueOptions = mutableListOf<CellPattern>()
        uniqueOptions.add(correctCell)

        val correctSig = correctCell.getVisualSignature()
        val seenSignatures = mutableSetOf(correctSig)

        val allShapes = ShapeType.values()
        val allColors = listOf(NeonCyan, NeonPink, NeonBlue)
        val allRotations = listOf(0, 45, 90, 135, 180, 225, 270, 315)

        // Generate distractors
        var attempts = 0
        while (uniqueOptions.size < 4 && attempts < 100) {
            attempts++
            val shape = if (rand.nextBoolean()) correctCell.shape else allShapes.random(rand)
            val color = if (rand.nextBoolean()) correctCell.color else allColors.random(rand)
            val rotation = if (rand.nextBoolean()) correctCell.rotation else allRotations.random(rand)
            val size = correctCell.size

            val candidate = CellPattern(shape, color, rotation, size)
            val sig = candidate.getVisualSignature()

            if (sig !in seenSignatures) {
                uniqueOptions.add(candidate)
                seenSignatures.add(sig)
            }
        }

        // Fallback if we couldn't find enough unique distractors
        while (uniqueOptions.size < 4) {
            val shape = allShapes.random(rand)
            val color = allColors.random(rand)
            val rotation = allRotations.random(rand)
            val candidate = CellPattern(shape, color, rotation)
            val sig = candidate.getVisualSignature()
            if (sig !in seenSignatures) {
                uniqueOptions.add(candidate)
                seenSignatures.add(sig)
            }
        }

        // Place correct option precisely at targetCorrectIndex, and distractors elsewhere
        val finalOptions = MutableList<CellPattern?>(4) { null }
        finalOptions[targetCorrectIndex.coerceIn(0, 3)] = correctCell

        var distractorIdx = 1
        for (i in 0..3) {
            if (i != targetCorrectIndex) {
                finalOptions[i] = uniqueOptions.getOrNull(distractorIdx)
                distractorIdx++
            }
        }

        val nonNullOptions = finalOptions.map { it ?: correctCell }

        return MatrixPattern(
            cells = cells,
            options = nonNullOptions,
            correctOption = targetCorrectIndex
        )
    }

    private fun generateLinearPattern(seed: Int): List<CellPattern> {
        val rand = Random(seed)
        val shapes = listOf(ShapeType.CIRCLE, ShapeType.SQUARE, ShapeType.TRIANGLE, ShapeType.HEXAGON).shuffled(rand)
        val colors = listOf(NeonCyan, NeonPink, NeonBlue).shuffled(rand)

        val cells = mutableListOf<CellPattern>()

        for (i in 0..8) {
            val shape = shapes[i % 3]
            val color = colors[i % 3]
            cells.add(CellPattern(shape, color, 0))
        }

        return cells
    }

    private fun generateRotationPattern(seed: Int): List<CellPattern> {
        val rand = Random(seed)
        val baseShape = listOf(ShapeType.SQUARE, ShapeType.TRIANGLE, ShapeType.HEXAGON).random(rand)
        val cells = mutableListOf<CellPattern>()

        for (i in 0..8) {
            val rotation = (i * 45) % 360
            cells.add(CellPattern(baseShape, NeonCyan, rotation))
        }

        return cells
    }

    private fun generateMetaPattern(seed: Int): List<CellPattern> {
        val rand = Random(seed)
        // More complex meta rules (inversion, symmetry breaking, etc.)
        val cells = mutableListOf<CellPattern>()

        for (i in 0..8) {
            val shape = if (i % 2 == 0) ShapeType.CIRCLE else ShapeType.SQUARE
            val color = if (i % 3 == 0) NeonPink else NeonCyan
            val rotation = if (i > 4) (180 + rand.nextInt(2) * 90) % 360 else 0
            cells.add(CellPattern(shape, color, rotation))
        }

        return cells
    }

    fun generateSafePattern(difficulty: Double, targetCorrectIndex: Int): MatrixPattern {
        // Guaranteed unique and solvable pattern
        val shapes = listOf(ShapeType.CIRCLE, ShapeType.SQUARE, ShapeType.TRIANGLE, ShapeType.HEXAGON)
        val cells = mutableListOf<CellPattern>()

        for (i in 0..8) {
            cells.add(CellPattern(shapes[i % 4], NeonCyan, (i * 45) % 360))
        }

        return populateOptions(cells, 42, targetCorrectIndex)
    }

    enum class PatternType { LINEAR, ROTATION, META }
}
