package com.neon.ascent.feature.terminal.ui

import kotlin.math.abs

object MatrixPatternValidator {

    /**
     * Validates a generated pattern meets all quality constraints
     * Returns true if the pattern is good for use in the test
     */
    fun isValidPattern(pattern: MatrixPattern, difficulty: Double): Boolean {
        return hasClearSolution(pattern) &&
               hasSufficientDiscrimination(pattern) &&
               noAmbiguousOptions(pattern) &&
               meetsDifficultyTarget(pattern, difficulty)
    }

    /**
     * Ensures there is exactly one correct logical solution.
     * Checks that options are unique and that the correct option is the only one 
     * assigned as such (though the generator handles the latter, we verify integrity).
     */
    private fun hasClearSolution(pattern: MatrixPattern): Boolean {
        val options = pattern.options
        if (options.size != 4) return false
        
        // Verify all options are visually distinct
        val uniqueSigs = options.map { it.getVisualSignature() }.toSet()
        if (uniqueSigs.size != options.size) return false
        
        // Ensure the correct option index is within bounds
        return pattern.correctOption in 0..3
    }

    /**
     * Ensures the pattern actually differentiates ability levels.
     * We want to avoid patterns that are too uniform or too chaotic for their intended difficulty.
     */
    private fun hasSufficientDiscrimination(pattern: MatrixPattern): Boolean {
        val uniqueShapes = pattern.cells.map { it.shape }.toSet().size
        val uniqueRotations = pattern.cells.map { it.rotation }.toSet().size
        val uniqueColors = pattern.cells.map { it.color }.toSet().size
        
        // Ensure at least two attributes vary in the pattern to provide enough "signal"
        return (uniqueShapes >= 2 || uniqueRotations >= 2) && uniqueColors >= 1
    }

    /**
     * Prevents multiple options from appearing equally valid.
     * Evaluates distractors to ensure they aren't accidentally too similar to the solution
     * or fitting a visible pattern.
     */
    private fun noAmbiguousOptions(pattern: MatrixPattern): Boolean {
        val correct = pattern.options[pattern.correctOption]
        
        return pattern.options.indices.all { i ->
            if (i == pattern.correctOption) true
            else {
                val distractor = pattern.options[i]
                val diffs = countDifferences(correct, distractor)
                
                // Distractors shouldn't be too similar to the correct answer 
                // unless it's a very high difficulty pattern.
                diffs >= 1 
            }
        }
    }

    private fun countDifferences(a: CellPattern, b: CellPattern): Int {
        var diffs = 0
        if (a.shape != b.shape) diffs++
        if (a.color != b.color) diffs++
        if (a.rotation != b.rotation) diffs++
        if (abs(a.size - b.size) > 0.01) diffs++
        return diffs
    }

    /**
     * Ensures the pattern matches the intended difficulty level
     */
    private fun meetsDifficultyTarget(pattern: MatrixPattern, targetDifficulty: Double): Boolean {
        val complexityScore = calculateComplexity(pattern)
        // Allow a sliding window of acceptable complexity
        val tolerance = when {
            targetDifficulty < 1.0 -> 0.4
            targetDifficulty < 2.0 -> 0.6
            else -> 0.8
        }
        return abs(complexityScore - targetDifficulty) < tolerance
    }

    /**
     * Calculates a complexity score for the pattern (0.0 - 3.0+)
     */
    private fun calculateComplexity(pattern: MatrixPattern): Double {
        val uniqueElements = pattern.cells.map { it.getVisualSignature() }.toSet().size
        val symmetryBreaks = countSymmetryBreaks(pattern)
        
        val uniqueShapes = pattern.cells.map { it.shape }.toSet().size
        val uniqueColors = pattern.cells.map { it.color }.toSet().size
        
        return (uniqueElements * 0.25) + 
               (symmetryBreaks * 0.3) + 
               (uniqueShapes * 0.15) + 
               (uniqueColors * 0.1)
    }

    /**
     * Counts how many times symmetry is broken in the 3x3 matrix.
     * High symmetry = low complexity.
     */
    private fun countSymmetryBreaks(pattern: MatrixPattern): Int {
        if (pattern.cells.size < 9) return 0
        val cells = pattern.cells
        var breaks = 0
        
        // Horizontal reflection (Column 0 vs Column 2)
        if (cells[0].getVisualSignature() != cells[2].getVisualSignature()) breaks++
        if (cells[3].getVisualSignature() != cells[5].getVisualSignature()) breaks++
        if (cells[6].getVisualSignature() != cells[8].getVisualSignature()) breaks++
        
        // Vertical reflection (Row 0 vs Row 2)
        if (cells[0].getVisualSignature() != cells[6].getVisualSignature()) breaks++
        if (cells[1].getVisualSignature() != cells[7].getVisualSignature()) breaks++
        if (cells[2].getVisualSignature() != cells[8].getVisualSignature()) breaks++
        
        // Central Point Symmetry (e.g. 0 vs 8, 1 vs 7, etc.)
        if (cells[0].getVisualSignature() != cells[8].getVisualSignature()) breaks++
        if (cells[1].getVisualSignature() != cells[7].getVisualSignature()) breaks++
        if (cells[2].getVisualSignature() != cells[6].getVisualSignature()) breaks++
        if (cells[3].getVisualSignature() != cells[5].getVisualSignature()) breaks++
        
        return breaks
    }

    /**
     * Main validation entry point with retry logic.
     * If a valid pattern can't be found within maxAttempts, falls back to a safe generator.
     */
    fun generateValidPattern(targetDifficulty: Double, targetCorrectIndex: Int = 2, maxAttempts: Int = 12): MatrixPattern? {
        repeat(maxAttempts) {
            val candidate = MatrixPatternGenerator.generatePattern(targetDifficulty, targetCorrectIndex)
            if (isValidPattern(candidate, targetDifficulty)) {
                return candidate
            }
        }
        // Fallback to a known good pattern logic if the random generator is failing quality checks
        return MatrixPatternGenerator.generateSafePattern(targetDifficulty, targetCorrectIndex)
    }
}
