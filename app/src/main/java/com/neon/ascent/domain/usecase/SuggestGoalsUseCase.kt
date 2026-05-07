package com.neon.ascent.domain.usecase

import android.content.Context
import com.neon.ascent.data.repository.BioAgePredictor
import com.neon.ascent.data.repository.GoalRepository
import com.neon.ascent.data.repository.SpecialRepository
import com.neon.ascent.data.repository.UserStoryRepository
import com.neon.ascent.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuggestGoalsUseCase @Inject constructor(
    private val userStoryRepository: UserStoryRepository,
    private val goalRepository: GoalRepository,
    private val bioAgePredictor: BioAgePredictor,
    private val specialRepository: SpecialRepository
) {

    suspend fun suggestGoals(): List<Goal> {
        val story = userStoryRepository.getMainStory().firstOrNull() ?: UserStory()
        val currentSpecials = specialRepository.getAllSpecialStats().first()

        val suggested = mutableListOf<Goal>()

        story.grandAspirations.forEach { aspiration ->
            when {
                aspiration.contains("strength", ignoreCase = true) -> {
                    suggested.add(
                        Goal(
                            id = UUID.randomUUID().toString(),
                            title = "Forge Iron Body",
                            description = "Build raw physical power",
                            aspirationLink = aspiration,
                            targetValue = 10f,
                            currentValue = 0f,
                            unit = "levels",
                            deadline = null,
                            linkedSpecial = SpecialType.STRENGTH,
                            isActive = true
                        )
                    )
                }
                aspiration.contains("focus", ignoreCase = true) || aspiration.contains("intelligence", ignoreCase = true) -> {
                    suggested.add(
                        Goal(
                            id = UUID.randomUUID().toString(),
                            title = "Sharpen the Mind",
                            description = "Elevate Perception & Intelligence",
                            aspirationLink = aspiration,
                            targetValue = 10f,
                            currentValue = 0f,
                            unit = "levels",
                            deadline = null,
                            linkedSpecial = SpecialType.PERCEPTION,
                            isActive = true
                        )
                    )
                }
                // ... add more mappings as needed
            }
        }

        // Biological age based suggestions
        bioAgePredictor.getLastResult()?.let { result ->
            if (result.ageGap > 5) {
                suggested.add(
                    Goal(
                        id = UUID.randomUUID().toString(),
                        title = "Reverse Biological Aging",
                        description = "Lower your biological age through consistent habits",
                        aspirationLink = "Biological Age Reduction",
                        targetValue = result.ageGap - 3f,
                        currentValue = 0f,
                        unit = "years",
                        deadline = null,
                        linkedSpecial = SpecialType.ENDURANCE,
                        isActive = true
                    )
                )
            }
        }

        // Fallback if nothing matched
        if (suggested.isEmpty()) {
            suggested.add(createDefaultFocusGoal())
        }

        return suggested
    }

    private fun createDefaultFocusGoal() = Goal(
        id = UUID.randomUUID().toString(),
        title = "Build Elite Focus",
        description = "Daily deep work + meditation practice",
        aspirationLink = "General Growth",
        targetValue = 2000f,
        currentValue = 0f,
        unit = "hours",
        deadline = null,
        linkedSpecial = SpecialType.INTELLIGENCE,
        isActive = true
    )
}
