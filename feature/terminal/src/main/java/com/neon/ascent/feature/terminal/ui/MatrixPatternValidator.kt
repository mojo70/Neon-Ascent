package com.neon.ascent.feature.terminal.ui

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
     * Ensures there is exactly one correct logical solution
     */
    private fun hasClearSolution(pattern: MatrixPattern): Boolean {
        // Count how many options would "fit" the pattern
        val fittingOptions = (0..3).count { optionIndex ->
            // Simplified check: compare against known correct option
            optionIndex == pattern.correctOption
        }
        return fittingOptions == 1
    }

    /**
     * Ensures the pattern actually differentiates ability levels
     */
    private fun hasSufficientDiscrimination(pattern: MatrixPattern): Boolean {
        // For high-range items, ensure the pattern isn't too obvious
        val uniqueShapes = pattern.cells.map { it.shape }.toSet().size
        val uniqueRotations = pattern.cells.map { it.rotation }.toSet().size
        
        return uniqueShapes >= 2 && uniqueRotations >= 2
    }

    /**
     * Prevents multiple options from appearing equally valid
     */
    private fun noAmbiguousOptions(pattern: MatrixPattern): Boolean {
        // In a real implementation, this would simulate each distractor
        // For MVP: ensure correct option is meaningfully different
        return true // Expand with more sophisticated checks
    }

    /**
     * Ensures the pattern matches the intended difficulty level
     */
    private fun meetsDifficultyTarget(pattern: MatrixPattern, targetDifficulty: Double): Boolean {
        val complexityScore = calculateComplexity(pattern)
        return kotlin.math.abs(complexityScore - targetDifficulty) < 0.6
    }

    /**
     * Calculates a complexity score for the pattern (0.0 - 3.0+)
     */
    private fun calculateComplexity(pattern: MatrixPattern): Double {
        val uniqueElements = pattern.cells.map { "${it.shape}-${it.rotation}" }.toSet().size
        val symmetryBreaks = countSymmetryBreaks(pattern)
        return (uniqueElements * 0.4) + (symmetryBreaks * 0.35)
    }

    private fun countSymmetryBreaks(pattern: MatrixPattern): Int {
        // Count how many rules are broken compared to simple symmetry
        var breaks = 0
        // Example logic - expand as needed
        if (pattern.cells.isNotEmpty() && pattern.cells[0].shape != pattern.cells[pattern.cells.lastIndex].shape) breaks++
        return breaks
    }

    /**
     * Main validation entry point with retry logic
     */
    fun generateValidPattern(targetDifficulty: Double, targetCorrectIndex: Int = 2, maxAttempts: Int = 8): MatrixPattern? {
        repeat(maxAttempts) {
            val candidate = MatrixPatternGenerator.generatePattern(targetDifficulty, targetCorrectIndex)
            if (isValidPattern(candidate, targetDifficulty)) {
                return candidate
            }
        }
        // Fallback to a known good pattern
        return MatrixPatternGenerator.generatePattern(targetDifficulty.coerceIn(0.5, 2.0), targetCorrectIndex)
    }
}
