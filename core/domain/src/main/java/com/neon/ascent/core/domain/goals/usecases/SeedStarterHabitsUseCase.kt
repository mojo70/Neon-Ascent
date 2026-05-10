package com.neon.ascent.core.domain.goals.usecases

import com.neon.ascent.core.domain.GoalRepository
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.model.SpecialType
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

/**
 * Seeds intelligent starter habits based on the user's derived archetype.
 * Runs once after character creation / onboarding.
 */
class SeedStarterHabitsUseCase @Inject constructor(
    private val goalRepository: GoalRepository
) {

    suspend operator fun invoke(userArchetype: String) {
        // Prevent duplicate seeding
        val existingHabits = goalRepository.getHabits().first()
        if (existingHabits.isNotEmpty()) return

        val starterHabits = generateStarterHabits(userArchetype)

        starterHabits.forEach { habit ->
            goalRepository.saveHabit(habit)
        }
    }

    private fun generateStarterHabits(archetype: String): List<Habit> {
        val habits = mutableListOf<Habit>()

        when (archetype.uppercase()) {
            "THE STRATEGIST" -> {
                habits.addAll(listOf(
                    createHabit("Daily Neural Focus Block", "30-60 min deep work", listOf(SpecialType.INTELLIGENCE, SpecialType.PERCEPTION)),
                    createHabit("System Optimization", "Review yesterday's logs and plan today's critical path", listOf(SpecialType.INTELLIGENCE)),
                    createHabit("Morning ICE Breach (Cold Shower)", "Build mental resilience and discipline", listOf(SpecialType.ENDURANCE, SpecialType.STRENGTH))
                ))
            }
            "THE PRAGMATIST" -> {
                habits.addAll(listOf(
                    createHabit("Daily Strength Protocol", "Compound lifts or bodyweight circuit", listOf(SpecialType.STRENGTH, SpecialType.ENDURANCE)),
                    createHabit("Mobility & Edgework", "10 min dynamic stretching + balance", listOf(SpecialType.AGILITY)),
                    createHabit("Combat Breathing", "4-7-8 breathing or box breathing", listOf(SpecialType.PERCEPTION, SpecialType.ENDURANCE))
                ))
            }
            "THE ADVOCATE", "THE IDEALIST" -> {
                habits.addAll(listOf(
                    createHabit("Daily Connection Quest", "Reach out to one contact or have a meaningful conversation", listOf(SpecialType.CHARISMA)),
                    createHabit("Reputation Building", "Perform one act of reputation-positive action", listOf(SpecialType.CHARISMA, SpecialType.LUCK)),
                    createHabit("Presence Training", "10 min active listening practice", listOf(SpecialType.PERCEPTION, SpecialType.CHARISMA))
                ))
            }
            "THE EDGE-RUNNER" -> {
                habits.addAll(listOf(
                    createHabit("Move the Body", "Minimum 8k steps or equivalent", listOf(SpecialType.AGILITY, SpecialType.ENDURANCE)),
                    createHabit("Neural Maintenance", "20+ min focused learning", listOf(SpecialType.INTELLIGENCE)),
                    createHabit("Core Recovery", "7+ hours sleep + basic mobility", listOf(SpecialType.ENDURANCE))
                ))
            }
            else -> {
                // Default balanced starter pack
                habits.addAll(listOf(
                    createHabit("Daily Check-in", "Synchronize biometrics and log goals", listOf(SpecialType.PERCEPTION)),
                    createHabit("Neural Stim", "Engage in cognitive training", listOf(SpecialType.INTELLIGENCE)),
                    createHabit("Physical Maintenance", "Basic activity and movement", listOf(SpecialType.ENDURANCE, SpecialType.STRENGTH))
                ))
            }
        }

        return habits
    }

    private fun createHabit(
        title: String,
        description: String,
        linkedAttributes: List<SpecialType>
    ): Habit {
        return Habit(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            recurrence = Recurrence(RecurrenceType.DAILY),
            linkedAttributes = linkedAttributes,
            progress = GoalProgress(current = 0f, target = 1f),
            streak = 0,
            lastCompleted = null
        )
    }
}
