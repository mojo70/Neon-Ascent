package com.neon.ascent.data.repository

import com.neon.ascent.model.TrainingTemplate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateRepository @Inject constructor() {
    private val templates = listOf(
        TrainingTemplate(
            id = "SOLO",
            name = "THE SOLO",
            description = "A combat-focused build optimized for strength and endurance.",
            somatotype = "Mesomorph",
            strength = 7,
            agility = 5,
            endurance = 7,
            intelligence = 3,
            perception = 4,
            charisma = 2,
            luck = 2
        ),
        TrainingTemplate(
            id = "NETRUNNER",
            name = "THE NETRUNNER",
            description = "A specialized deck-jockey focused on intelligence and perception.",
            somatotype = "Ectomorph",
            strength = 2,
            agility = 4,
            endurance = 3,
            intelligence = 8,
            perception = 7,
            charisma = 4,
            luck = 2
        ),
        TrainingTemplate(
            id = "TECHIE",
            name = "THE TECHIE",
            description = "A versatile engineer with high technical aptitude.",
            somatotype = "Ectomorph",
            strength = 3,
            agility = 4,
            endurance = 4,
            intelligence = 7,
            perception = 6,
            charisma = 3,
            luck = 3
        ),
        TrainingTemplate(
            id = "STREET_KID",
            name = "THE STREET KID",
            description = "Balanced stats with high luck and charisma.",
            somatotype = "Mesomorph",
            strength = 5,
            agility = 6,
            endurance = 5,
            intelligence = 4,
            perception = 4,
            charisma = 6,
            luck = 7
        )
    )

    fun getTemplates(): List<TrainingTemplate> = templates

    fun getTemplateById(id: String): TrainingTemplate? = templates.find { it.id == id }
}
