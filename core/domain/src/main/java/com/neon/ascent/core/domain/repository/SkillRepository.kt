package com.neon.ascent.core.domain.repository

interface SkillRepository {
    suspend fun getSkillPrompt(skillName: String): String?
    suspend fun installExpertSkills()
}
