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
 * Orchestrates the full chain reaction when a user completes (or logs) a habit.
 *
 * Flow:
 * 1. Update Habit (streak + progress)
 * 2. Update linked S.P.E.C.I.A.L. attributes (real grounded XP)
 * 3. Advance parent Missions and Aspirations
 * 4. Trigger neon level-up effects on the holographic avatar
 */
class CompleteHabitAndUpdateGoalsUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
    private val specialRepository: SpecialRepository,
    private val levelUpEffectService: LevelUpEffectService
) {

    suspend operator fun invoke(habitId: String, completionData: CompletionData) {
        val habit = goalRepository.getGoalById(habitId).first() as? Habit ?: return

        // 1. Update the Habit itself
        val updatedHabit = updateHabitProgress(habit, completionData)
        goalRepository.saveHabit(updatedHabit)

        // 2. Apply real S.P.E.C.I.A.L. progression
        val attributeUpdates = applySpecialProgress(habit, completionData)

        // 3. Cascade to parent Missions & Aspirations
        advanceParentGoals(habit)

        // 4. Visual + Audio feedback (this is what makes it addictive)
        attributeUpdates.forEach { (type, xpGained) ->
            if (xpGained >= 20) {
                levelUpEffectService.triggerLevelUp(type, xpGained.toInt())
            }
        }
    }

    private fun updateHabitProgress(habit: Habit, data: CompletionData): Habit {
        val newProgress = (habit.progress.current + data.progressDelta).coerceAtMost(1f)
        val isCompletion = newProgress >= 1f && habit.progress.current < 1f

        return habit.copy(
            progress = habit.progress.copy(current = newProgress),
            streak = if (isCompletion) habit.streak + 1 else habit.streak,
            lastCompleted = Instant.now()
        )
    }

    private suspend fun applySpecialProgress(
        habit: Habit,
        data: CompletionData
    ): Map<SpecialType, Long> {
        val updates = mutableMapOf<SpecialType, Long>()

        habit.linkedAttributes.forEach { attributeType ->
            val baseXp = data.attributeContributions[attributeType] ?: 25L

            val currentAttr = specialRepository.getSpecialAttribute(attributeType).first()
                ?: SpecialAttribute(type = attributeType, currentValue = 5, percentile = 50)

            val xpToAdd = baseXp + (habit.streak * 5L) // streak bonus
            val newValue = (currentAttr.currentValue + (xpToAdd / 28).toInt()).coerceIn(1, 10)
            val newPercentile = calculatePercentile(newValue)

            val updatedAttr = currentAttr.copy(
                currentValue = newValue,
                percentile = newPercentile,
                totalXp = currentAttr.totalXp + xpToAdd,
                lastUpdated = Instant.now()
            )

            specialRepository.updateSpecialAttribute(updatedAttr)
            updates[attributeType] = xpToAdd
        }

        return updates
    }

    private suspend fun advanceParentGoals(habit: Habit) {
        // Advance any active missions this habit contributes to
        val activeMissions = goalRepository.getActiveMissions().first()

        activeMissions.forEach { mission ->
            if (mission.linkedAttributes.intersect(habit.linkedAttributes).isNotEmpty()) {
                val newProgress = (mission.progress.current + 0.18f).coerceAtMost(1f)

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
        // TODO: Full aspiration completion logic + big celebration
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
