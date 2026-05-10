package com.neon.ascent.core.domain.goals.usecases

import com.neon.ascent.core.domain.GoalRepository
import com.neon.ascent.core.domain.SpecialRepository
import com.neon.ascent.core.domain.effects.LevelUpEffectService
import com.neon.ascent.core.domain.goals.models.CompletionData
import com.neon.ascent.core.domain.goals.models.Habit
import com.neon.ascent.core.domain.model.SpecialAttribute
import com.neon.ascent.core.domain.model.SpecialType
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

/**
 * Fully integrated habit completion engine.
 * Orchestrates the chain reaction: progress -> streak -> S.P.E.C.I.A.L. XP -> mission advancement.
 */
class CompleteHabitAndUpdateGoalsUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
    private val specialRepository: SpecialRepository,
    private val levelUpEffectService: LevelUpEffectService
) {

    suspend operator fun invoke(habitId: String, completionData: CompletionData) {
        val habit = goalRepository.getGoalById(habitId).first() as? Habit ?: return

        // 1. Update Habit (progress + streak)
        val updatedHabit = updateHabit(habit, completionData)
        goalRepository.saveHabit(updatedHabit)

        // 2. Apply real S.P.E.C.I.A.L. progression
        val attributeUpdates = applySpecialProgress(updatedHabit, completionData)

        // 3. Cascade progress to parent Missions & Aspirations
        advanceParentGoals(updatedHabit)

        // 4. Trigger visual + haptic feedback
        triggerLevelUpEffects(attributeUpdates)
    }

    private fun updateHabit(habit: Habit, data: CompletionData): Habit {
        val newProgress = (habit.progress.current + data.progressDelta).coerceAtMost(1f)
        val isNewCompletion = newProgress >= 1f && habit.progress.current < 1f

        return habit.copy(
            progress = habit.progress.copy(current = newProgress),
            streak = if (isNewCompletion) habit.streak + 1 else habit.streak,
            lastCompleted = Instant.now()
        )
    }

    private suspend fun applySpecialProgress(
        habit: Habit,
        data: CompletionData
    ): Map<SpecialType, Long> {
        val updates = mutableMapOf<SpecialType, Long>()

        habit.linkedAttributes.forEach { type ->
            val baseXp = data.attributeContributions[type] ?: 25L
            val streakBonus = (habit.streak * 5L)
            val totalXp = baseXp + streakBonus

            val currentAttr = specialRepository.getSpecialAttribute(type).first()
                ?: SpecialAttribute(type = type, currentValue = 5, percentile = 50)

            val newValue = (currentAttr.currentValue + (totalXp / 28).toInt()).coerceIn(1, 10)
            val newPercentile = calculatePercentile(newValue)

            val updatedAttr = currentAttr.copy(
                currentValue = newValue,
                percentile = newPercentile,
                totalXp = currentAttr.totalXp + totalXp,
                lastUpdated = Instant.now()
            )

            specialRepository.updateSpecialAttribute(updatedAttr)
            updates[type] = totalXp
        }

        return updates
    }

    private suspend fun advanceParentGoals(habit: Habit) {
        val activeMissions = goalRepository.getActiveMissions().first()

        activeMissions.forEach { mission ->
            if (mission.linkedAttributes.any { it in habit.linkedAttributes }) {
                val newProgress = (mission.progress.current + 0.2f).coerceAtMost(1f)

                val updatedMission = mission.copy(
                    progress = mission.progress.copy(current = newProgress)
                )
                goalRepository.saveMission(updatedMission)

                if (newProgress >= 1f && mission.parentAspirationId != null) {
                    advanceAspiration(mission.parentAspirationId)
                }
            }
        }
    }

    private fun advanceAspiration(aspirationId: String) {
        // Placeholder for full aspiration completion logic + major celebration
    }

    private fun triggerLevelUpEffects(updates: Map<SpecialType, Long>) {
        updates.forEach { (type, xp) ->
            if (xp >= 20) {
                levelUpEffectService.triggerLevelUp(type, xp.toInt())
            }
        }
    }

    private fun calculatePercentile(value: Int): Int = when (value) {
        10 -> 95
        9 -> 87
        8 -> 74
        7 -> 60
        6 -> 48
        5 -> 35
        else -> 25
    }
}
