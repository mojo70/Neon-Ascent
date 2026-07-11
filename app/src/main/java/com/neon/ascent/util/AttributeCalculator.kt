package com.neon.ascent.util

import com.neon.ascent.core.domain.character.models.UserCharacter
import kotlin.math.*

data class RawAttributeInputs(
    val pushups: Int? = null,
    val squats: Int? = null,
    val benchPressWeight: Float? = null,
    val plankSeconds: Int? = null,
    val mileRunMinutes: Float? = null,
    val balanceSeconds: Int? = null,
    val reactionTimeMs: Int? = null,
    val patternScore: Int? = null, // 0-10
    val memoryScore: Int? = null, // 0-10
    val scenarioScore: Int? = null, // 0-10
    val coinFlipsHeads: Int? = null // 0-10
)

data class CalculatedScores(
    val strength: Int,
    val endurance: Int,
    val agility: Int,
    val perception: Int,
    val intelligence: Int,
    val charisma: Int,
    val luck: Int,
    val strengthPercentile: Float,
    val endurancePercentile: Float,
    val agilityPercentile: Float,
    val perceptionPercentile: Float,
    val intelligencePercentile: Float,
    val charismaPercentile: Float,
    val luckPercentile: Float
)

object AttributeCalculator {

    fun calculateAll(user: UserCharacter, inputs: RawAttributeInputs): CalculatedScores {
        val weight = user.weight.toFloatOrNull() ?: 75f
        val gender = user.sex
        val age = 25 // TODO: Calculate from DOB

        val strengthResult = calculateStrength(inputs.pushups, inputs.squats, inputs.benchPressWeight, weight, gender, age)
        val enduranceResult = calculateEndurance(inputs.plankSeconds, inputs.mileRunMinutes, gender, age)
        val agilityResult = calculateAgility(inputs.balanceSeconds, age)
        val perceptionResult = calculatePerception(inputs.reactionTimeMs)
        val intelligenceResult = calculateIntelligence(inputs.patternScore, inputs.memoryScore)
        val charismaResult = calculateCharisma(user.mbti, inputs.scenarioScore)
        val luckResult = calculateLuck(inputs.coinFlipsHeads)

        return CalculatedScores(
            strength = strengthResult.first,
            strengthPercentile = strengthResult.second,
            endurance = enduranceResult.first,
            endurancePercentile = enduranceResult.second,
            agility = agilityResult.first,
            agilityPercentile = agilityResult.second,
            perception = perceptionResult.first,
            perceptionPercentile = perceptionResult.second,
            intelligence = intelligenceResult.first,
            intelligencePercentile = intelligenceResult.second,
            charisma = charismaResult.first,
            charismaPercentile = charismaResult.second,
            luck = luckResult.first,
            luckPercentile = luckResult.second
        )
    }

    private fun calculateStrength(pushups: Int?, squats: Int?, bench: Float?, weight: Float, gender: String, age: Int): Pair<Int, Float> {
        val ratio = if (bench != null) bench / weight else {
            // Estimate 1RM from pushups/squats
            val estimatedBench = (pushups ?: 0) * 2.0f + (squats ?: 0) * 1.5f
            estimatedBench / weight
        }
        
        val percentile = when {
            ratio < 0.5 -> ratio / 0.5f * 0.2f
            ratio < 1.0 -> 0.2f + (ratio - 0.5f) / 0.5f * 0.3f
            ratio < 1.5 -> 0.5f + (ratio - 1.0f) / 0.5f * 0.3f
            else -> min(0.8f + (ratio - 1.5f) / 0.5f * 0.2f, 1.0f)
        }
        
        return Pair((percentile * 10).roundToInt().coerceIn(1, 10), percentile)
    }

    private fun calculateEndurance(plank: Int?, mileRun: Float?, gender: String, age: Int): Pair<Int, Float> {
        val plankScore = (plank ?: 0) / 180f // 3 mins is 1.0
        val mileScore = if (mileRun != null) max(0f, (12f - mileRun) / 6f) else 0.5f
        
        val combined = (plankScore + mileScore) / 2f
        val percentile = combined.coerceIn(0f, 1f)
        
        return Pair((percentile * 10).roundToInt().coerceIn(1, 10), percentile)
    }

    private fun calculateAgility(balance: Int?, age: Int): Pair<Int, Float> {
        val percentile = (balance ?: 0) / 45f // 45s is elite
        return Pair((percentile * 10).roundToInt().coerceIn(1, 10), percentile.coerceIn(0f, 1f))
    }

    private fun calculatePerception(reactionTime: Int?): Pair<Int, Float> {
        if (reactionTime == null) return Pair(5, 0.5f)
        // Average 250ms, Elite 180ms
        val percentile = when {
            reactionTime > 350 -> 0.1f
            reactionTime > 250 -> 0.1f + (350 - reactionTime) / 100f * 0.4f
            reactionTime > 180 -> 0.5f + (250 - reactionTime) / 70f * 0.4f
            else -> 0.9f + min(0.1f, (180 - reactionTime) / 50f)
        }
        return Pair((percentile * 10).roundToInt().coerceIn(1, 10), percentile)
    }

    private fun calculateIntelligence(pattern: Int?, memory: Int?): Pair<Int, Float> {
        val combined = ((pattern ?: 5) + (memory ?: 5)) / 20f
        return Pair((combined * 10).roundToInt().coerceIn(1, 10), combined)
    }

    private fun calculateCharisma(mbti: String?, scenarioScore: Int?): Pair<Int, Float> {
        val mbtiBonus = if (mbti?.contains("E") == true) 0.1f else 0f
        val base = (scenarioScore ?: 5) / 10f
        val percentile = (base + mbtiBonus).coerceIn(0f, 1f)
        return Pair((percentile * 10).roundToInt().coerceIn(1, 10), percentile)
    }

    private fun calculateLuck(heads: Int?): Pair<Int, Float> {
        val percentile = (heads ?: 5) / 10f
        return Pair((percentile * 10).roundToInt().coerceIn(1, 10), percentile)
    }
}
