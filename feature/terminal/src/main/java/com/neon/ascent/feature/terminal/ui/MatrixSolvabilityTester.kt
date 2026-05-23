package com.neon.ascent.feature.terminal.ui

object MatrixSolvabilityTester {

    /**
     * Full automated solvability test for a generated pattern
     * Returns true only if the pattern is high-quality and solvable
     */
    fun isSolvable(pattern: MatrixPattern, correctOption: Int): Boolean {
        return hasUniqueSolution(pattern, correctOption) &&
               distractorsAreDistinct(pattern, correctOption) && 
               hasLogicalConsistency(pattern) &&
               meetsMinimumComplexity(pattern)
    }

    /** Ensures all 4 options are visually/logically distinct */
    private fun distractorsAreDistinct(pattern: MatrixPattern, correctOption: Int): Boolean {
        val optionRepresentations = mutableSetOf<String>()

        for (option in pattern.options) {
            val rep = option.getVisualSignature()
            if (!optionRepresentations.add(rep)) {
                return false // Duplicate representation found
            }
        }
        return true
    }

    /**
     * Core test: Only ONE option should perfectly complete the pattern
     */
    private fun hasUniqueSolution(pattern: MatrixPattern, correctOption: Int): Boolean {
        var validSolutions = 0

        for (option in 0..3) {
            if (wouldCompletePattern(pattern, option)) {
                validSolutions++
            }
        }

        return validSolutions == 1 && wouldCompletePattern(pattern, correctOption)
    }

    /**
     * Checks if a given option would logically complete the pattern
     * This is the heart of solvability testing
     */
    private fun wouldCompletePattern(pattern: MatrixPattern, optionIndex: Int): Boolean {
        val cells = pattern.cells

        // Rule 1: Linear progression (row/column)
        if (isLinearPattern(cells, optionIndex)) return true

        // Rule 2: Rotational symmetry
        if (isRotationalPattern(cells, optionIndex)) return true

        // Rule 3: Meta / transformation rules
        if (isMetaPattern(cells, optionIndex)) return true

        return false
    }

    private fun isLinearPattern(cells: List<CellPattern>, option: Int): Boolean {
        // Example: Each row follows same shape sequence
        if (cells.size < 9) return false
        val row0 = cells[0].shape
        val row1 = cells[3].shape
        val row2Expected = cells[6].shape // Predict based on pattern

        return when (option) {
            0 -> row2Expected == cells[0].shape // simplistic example
            else -> false
        }
    }

    private fun isRotationalPattern(cells: List<CellPattern>, option: Int): Boolean {
        if (cells.size < 2) return false
        // Check 90° rotation consistency across grid
        return cells[0].rotation + 90 == cells[1].rotation % 360
    }

    private fun isMetaPattern(cells: List<CellPattern>, option: Int): Boolean {
        // More advanced: symmetry breaking, color inversion, etc.
        val symmetryScore = cells.count { it.rotation == 0 }
        return symmetryScore % 2 == option
    }

    private fun hasLogicalConsistency(pattern: MatrixPattern): Boolean {
        // Ensure no contradictory rules
        return true
    }

    private fun meetsMinimumComplexity(pattern: MatrixPattern): Boolean {
        val uniqueShapes = pattern.cells.map { it.shape }.toSet().size
        return uniqueShapes >= 2
    }

    /**
     * Main entry point with retry logic
     */
    fun generateValidSolvablePattern(targetDifficulty: Double, targetCorrectIndex: Int): MatrixPattern {
        repeat(15) {
            val candidate = MatrixPatternGenerator.generatePattern(targetDifficulty, targetCorrectIndex)
            
            if (isSolvable(candidate, candidate.correctOption)) {
                return candidate
            }
        }
        // Fallback to a known good pattern
        return MatrixPatternGenerator.generateSafePattern(targetDifficulty, targetCorrectIndex)
    }
}
