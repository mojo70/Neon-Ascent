package com.neon.ascent.core.domain.goals.usecases

import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.GoalRepository
import com.neon.ascent.core.domain.model.SpecialType
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject

/**
 * Procedurally generates meaningful Missions from user Aspirations.
 * This is the "quest engine" that makes long-term goals feel alive.
 */
class GenerateMissionsFromAspirationsUseCase @Inject constructor(
    private val goalRepository: GoalRepository
) {

    suspend operator fun invoke() {
        val aspirations = goalRepository.getAllAspirations().first()
            .filter { it.status == GoalStatus.ACTIVE }

        aspirations.forEach { aspiration ->
            val existingMissions = goalRepository.getMissionsForAspiration(aspiration.id).first()

            // Only generate if missing active missions
            if (existingMissions.none { it.progress.current < it.progress.target }) {
                val newMissions = generateMissionsForAspiration(aspiration)
                newMissions.forEach { goalRepository.saveMission(it) }
            }
        }
    }

    private fun generateMissionsForAspiration(aspiration: Aspiration): List<Mission> {
        val missions = mutableListOf<Mission>()

        // Generate 2-4 missions per aspiration
        val missionCount = (2..4).random()

        if (aspiration.linkedAttributes.isEmpty()) return emptyList()

        aspiration.linkedAttributes.forEach { attribute ->
            repeat(Math.max(1, missionCount / aspiration.linkedAttributes.size)) {
                missions.add(
                    createMissionForAttribute(
                        aspiration = aspiration,
                        focusAttribute = attribute
                    )
                )
            }
        }

        return missions
    }

    private fun createMissionForAttribute(
        aspiration: Aspiration,
        focusAttribute: SpecialType
    ): Mission {
        val title = generateMissionTitle(focusAttribute)
        val description = generateMissionDescription(focusAttribute, aspiration)

        return Mission(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            expiresAt = Instant.now().plus(7, ChronoUnit.DAYS), // 1 week missions
            linkedAttributes = listOf(focusAttribute),
            progress = GoalProgress(current = 0f, target = 1f),
            parentAspirationId = aspiration.id
        )
    }

    private fun generateMissionTitle(attribute: SpecialType): String = when (attribute) {
        SpecialType.STRENGTH -> listOf(
            "Heavy Frame Protocol", "Iron Temple Run", "Structural Integrity Test"
        ).random()
        SpecialType.PERCEPTION -> listOf(
            "Threat Pattern Recognition", "Signal Clarity Drill", "Detail Extraction"
        ).random()
        SpecialType.ENDURANCE -> listOf(
            "Core Recovery Cycle", "Signal Stability Run", "Long-Haul Endurance Test"
        ).random()
        SpecialType.CHARISMA -> listOf(
            "Reputation Grid Expansion", "Social Infiltration Run", "Alliance Protocol"
        ).random()
        SpecialType.INTELLIGENCE -> listOf(
            "Deep Deck Run", "Neural Architecture Review", "ICE Pattern Analysis"
        ).random()
        SpecialType.AGILITY -> listOf(
            "Ghosting Protocol", "Edgework Training", "Movement Vector Optimization"
        ).random()
        SpecialType.LUCK -> listOf(
            "Chaos Factor Calibration", "Entropy Harvesting", "Fortune Alignment"
        ).random()
    }

    private fun generateMissionDescription(
        attribute: SpecialType,
        aspiration: Aspiration
    ): String = when (attribute) {
        SpecialType.INTELLIGENCE -> "Complete 5 focused deep work sessions (≥45 min each) this week."
        SpecialType.STRENGTH -> "Hit 3 full strength sessions with progressive overload."
        SpecialType.AGILITY -> "Maintain 10k+ daily average steps + 2 mobility sessions."
        else -> "Advance ${aspiration.title} through consistent ${attribute.name.lowercase()} execution."
    }
}
