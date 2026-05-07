package com.neon.ascent.domain.usecase

import android.content.Context
import com.google.gson.Gson
import com.neon.ascent.data.repository.BioAgePredictor
import com.neon.ascent.data.repository.GoalRepository
import com.neon.ascent.data.repository.UserStoryRepository
import com.neon.ascent.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuggestGoalsUseCase @Inject constructor(
    private val userStoryRepository: UserStoryRepository,
    private val goalRepository: GoalRepository,
    private val bioAgePredictor: BioAgePredictor,
    @ApplicationContext private val context: Context
) {

    suspend fun suggestGoals(): List<Goal> {
        val story = userStoryRepository.getMainStory().firstOrNull() ?: UserStory()
        val aspirations = story.grandAspirations.map { it.lowercase() }

        val databank = loadDatabank()
        val suggested = mutableListOf<Goal>()

        for (asp in aspirations) {
            databank.aspirations.find { it.keyword.lowercase() in asp || asp in it.keyword.lowercase() }?.let { match ->
                match.suggestedGoals.forEach { template ->
                    suggested.add(
                        Goal(
                            id = UUID.randomUUID().toString(),
                            title = template.title,
                            description = template.description,
                            aspirationLink = match.title,
                            targetValue = template.targetValue,
                            currentValue = 0f,
                            unit = template.unit,
                            deadline = null,
                            isActive = true
                        )
                    )
                }
            }
        }

        // Bio-age based suggestions
        val lastBioAge = bioAgePredictor.getLastResult()
        val chronoAge = bioAgePredictor.getChronologicalAge()
        if (lastBioAge != null && lastBioAge.biologicalAge > chronoAge + 3) {
            suggested.add(createLongevityGoal())
            suggested.add(createHydrationGoal())
        }

        // Fallback if nothing matched
        if (suggested.isEmpty()) {
            suggested.add(createDefaultFocusGoal())
        }

        return suggested
    }

    private fun loadDatabank(): AspirationDatabank {
        val json = context.assets.open("databank/aspirations.json").bufferedReader().use { it.readText() }
        return Gson().fromJson(json, AspirationDatabank::class.java)
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
        isActive = true
    )

    private fun createLongevityGoal() = Goal(
        id = UUID.randomUUID().toString(),
        title = "Reverse Biological Age by 5 Years",
        description = "Lower your biological age through lifestyle",
        aspirationLink = "Biological Age Reduction",
        targetValue = 5f,
        currentValue = 0f,
        unit = "years",
        deadline = null,
        isActive = true
    )

    private fun createHydrationGoal() = Goal(
        id = UUID.randomUUID().toString(),
        title = "Consistent Water Intake",
        description = "Build the foundation of cellular health",
        aspirationLink = "Optimal Hydration",
        targetValue = 365f,
        currentValue = 0f,
        unit = "days",
        deadline = null,
        isActive = true
    )
}
