package com.neon.ascent.core.domain.goals.usecases

import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.core.domain.repository.AscensionRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Procedurally generates meaningful Missions from Ascension Directives.
 * This is the "quest engine" that makes long-term goals feel alive.
 */
class GenerateMissionsFromAspirationsUseCase @Inject constructor(
    private val ascensionRepository: AscensionRepository
) {

    suspend operator fun invoke() {
        val directives = ascensionRepository.getAllDirectives().first()
            .filter { it.status == DirectiveStatus.ACTIVE }

        directives.forEach { directive ->
            val existingMissions = ascensionRepository.getMissionsForDirective(directive.id).first()

            // Only generate if missing active missions
            if (existingMissions.none { it.progress < 1.0f }) {
                val newMissions = generateMissionsForDirective(directive)
                val existingTitles = existingMissions.map { it.title.trim().lowercase() }.toSet()

                newMissions.forEach { mission ->
                    if (!existingTitles.contains(mission.title.trim().lowercase())) {
                        ascensionRepository.insertMission(mission)
                    }
                }
            }
        }
    }

    private fun generateMissionsForDirective(directive: AscensionDirective): List<AscensionMission> {
        val missions = mutableListOf<AscensionMission>()

        // Generate 2-4 missions per directive
        val missionCount = (2..4).random()

        if (directive.linkedAttributes.isEmpty()) return emptyList()

        directive.linkedAttributes.forEach { attribute ->
            repeat(Math.max(1, missionCount / directive.linkedAttributes.size)) {
                missions.add(
                    createMissionForAttribute(
                        directive = directive,
                        focusAttribute = attribute
                    )
                )
            }
        }

        return missions
    }

    private fun createMissionForAttribute(
        directive: AscensionDirective,
        focusAttribute: SpecialType
    ): AscensionMission {
        val title = generateMissionTitle(focusAttribute)
        val description = generateMissionDescription(focusAttribute, directive)

        return AscensionMission(
            id = UUID.randomUUID().toString(),
            directiveId = directive.id,
            title = title,
            description = description,
            targetEndDate = LocalDate.now().plusWeeks(1), // 1 week missions
            linkedAttributes = listOf(focusAttribute),
            progress = 0f,
            status = AscensionMissionStatus.ACTIVE
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
        directive: AscensionDirective
    ): String = when (attribute) {
        SpecialType.INTELLIGENCE -> "Complete 5 focused deep work sessions (≥45 min each) this week."
        SpecialType.STRENGTH -> "Hit 3 full strength sessions with progressive overload."
        SpecialType.AGILITY -> "Maintain 10k+ daily average steps + 2 mobility sessions."
        else -> "Advance ${directive.title} through consistent ${attribute.name.lowercase()} execution."
    }
}
