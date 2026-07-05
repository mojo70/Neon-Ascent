package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.dao.ProtocolDao
import com.neon.ascent.core.data.mapper.toDomain
import com.neon.ascent.core.data.mapper.toEntity
import com.neon.ascent.core.domain.goals.models.AdaptedProtocol
import com.neon.ascent.core.domain.goals.models.Protocol
import com.neon.ascent.core.domain.repository.ProtocolRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtocolRepositoryImpl @Inject constructor(
    private val protocolDao: ProtocolDao
) : ProtocolRepository {
    override fun getAllProtocols(): Flow<List<Protocol>> =
        protocolDao.getAllProtocols().map { entities -> entities.map { it.toDomain() } }

    override fun getProtocolsByCategory(category: String): Flow<List<Protocol>> =
        protocolDao.getProtocolsByCategory(category).map { entities -> entities.map { it.toDomain() } }

    override fun getProtocolsByTag(tag: String): Flow<List<Protocol>> =
        protocolDao.getProtocolsByTag(tag).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getProtocolById(id: String): Protocol? =
        protocolDao.getProtocolById(id)?.toDomain()

    override suspend fun insertProtocol(protocol: Protocol) {
        protocolDao.insertProtocol(protocol.toEntity())
    }

    override suspend fun insertAdaptedProtocol(adaptedProtocol: AdaptedProtocol) {
        protocolDao.insertAdaptedProtocol(adaptedProtocol.toEntity())
    }

    override fun getAdaptedProtocolsForDirective(directiveId: String): Flow<List<AdaptedProtocol>> =
        protocolDao.getAdaptedProtocolsForDirective(directiveId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun deleteUserProtocol(id: String) {
        protocolDao.deleteUserProtocol(id)
    }

    override suspend fun seedDefaultProtocols() {
        if (protocolDao.getProtocolCount() > 0) return

        val protocols = listOf(
            Protocol(
                id = "cybercrapp_strength",
                title = "CyberCrapp Protocol",
                description = "Flagship progressive overload lifting program focused on heavy compounds and tactical recovery.",
                category = "Strength",
                canonicalSteps = listOf("Bench/Squat/Deadlift focus", "Track every set/rep", "Mandatory 48h muscle recovery", "Post-workout nutrient uplink"),
                source = "Neon Ascent Core",
                specialTags = listOf("Strength", "Endurance", "Recovery")
            ),
            Protocol(
                id = "atomic_habits_stacking",
                title = "Habit Stacking Protocol",
                description = "Link a new habit to an existing one using the formula: After [CURRENT HABIT], I will [NEW HABIT].",
                category = "Habit Formation",
                canonicalSteps = listOf("Identify current habit", "Choose new habit", "Define stacking formula", "Execute and Reward"),
                source = "Atomic Habits",
                specialTags = listOf("Consistency", "ADHD-Friendly")
            ),
            Protocol(
                id = "implementation_intentions",
                title = "Implementation Intentions",
                description = "Create a clear plan for when and where you will perform a new habit: I will [BEHAVIOR] at [TIME] in [LOCATION].",
                category = "Habit Formation",
                canonicalSteps = listOf("Identify behavior", "Select precise time", "Select precise location", "Commit to identity"),
                source = "Atomic Habits",
                specialTags = listOf("Clarity", "Execution")
            ),
            Protocol(
                id = "wim_hof_breathing",
                title = "Wim Hof Breathing",
                description = "Hyper-oxygenation technique to regulate the autonomic nervous system and boost immunity.",
                category = "Biohacking",
                canonicalSteps = listOf("30 deep breaths", "Exhale and hold", "Inhale and hold 15s", "Repeat 3 rounds"),
                source = "Wim Hof Method",
                specialTags = listOf("Recovery", "Energy", "Focus")
            ),
            Protocol(
                id = "morning_sunlight",
                title = "Circadian Anchor Protocol",
                description = "Get 5-10 minutes of direct sunlight within 30 minutes of waking to set cortisol and melatonin timers.",
                category = "Biohacking",
                canonicalSteps = listOf("Wake up", "Go outside (no windows)", "View horizon for 10m", "Hydrate immediately"),
                source = "Huberman Lab",
                specialTags = listOf("Sleep", "Energy", "Perception")
            ),
            Protocol(
                id = "vagus_nerve_reset",
                title = "Vagus Nerve Reset",
                description = "Tactical stimulation of the vagus nerve to exit 'fight or flight' and enter 'rest and digest' state.",
                category = "Recovery",
                canonicalSteps = listOf("Deep belly breathing", "Cold water splash", "Humming or gargling", "Gently pull earlobes"),
                source = "Polyvagal Theory",
                specialTags = listOf("Stress", "Anxiety", "ADHD")
            ),
            Protocol(
                id = "cold_thermogenesis",
                title = "Cold Plunge Protocol",
                description = "Controlled cold exposure to spike norepinephrine, improve metabolic health, and build mental toughness.",
                category = "Biohacking",
                canonicalSteps = listOf("Enter cold water (<15°C)", "Calm your breathing", "Duration: 2-3 minutes", "Rewarm naturally"),
                source = "FoundMyFitness",
                specialTags = listOf("Endurance", "Dopamine", "Mental Toughness")
            ),
            Protocol(
                id = "sonship_identity",
                title = "Identity-First Protocol",
                description = "Shift focus from performance to sonship. Remind yourself: I am already accepted.",
                category = "Spiritual",
                canonicalSteps = listOf("Silence for 2m", "Affirm: 'I am a Son'", "Release performance anxiety", "Act from overflow"),
                source = "Scripture / Sonship theology",
                specialTags = listOf("Identity", "Faith", "Anti-Burnout")
            ),
            Protocol(
                id = "scripture_meditation",
                title = "Neural Scripture Soak",
                description = "Rewire neural pathways using focused meditation on a single scripture passage.",
                category = "Spiritual",
                canonicalSteps = listOf("Read passage slowly", "Visualize its application", "Speak it aloud", "Listen for inner promptings"),
                source = "Ancient Christian Practice",
                specialTags = listOf("Faith", "Intelligence", "Peace")
            ),
            Protocol(
                id = "progressive_overload",
                title = "Iron Temple Fundamentals",
                description = "Standard progressive overload protocol for consistent strength gains.",
                category = "Strength",
                canonicalSteps = listOf("Track current weight", "Add 2.5kg each session", "Focus on form over ego", "Log completion"),
                source = "General Strength Science",
                specialTags = listOf("Strength", "Endurance")
            ),
            Protocol(
                id = "zone_2_base",
                title = "Zone 2 Aerobic Base",
                description = "Low-intensity steady-state cardio to improve mitochondrial efficiency and longevity.",
                category = "Endurance",
                canonicalSteps = listOf("Calculate target HR", "Activity: Walk/Jog/Cycle", "Duration: 45-60 min", "Nasal breathing only"),
                source = "San Millán / Attia",
                specialTags = listOf("Endurance", "Longevity", "Heart Health")
            ),
            Protocol(
                id = "nhe_cyclical_keto",
                title = "NHE Metabolic Protocol",
                description = "Natural Hormonal Enhancement through strategic carbohydrate cycling and fat adaptation.",
                category = "Biohacking",
                canonicalSteps = listOf("6 days high fat/protein", "1 night carb loading", "Timing: Evening carbs", "Maintain training intensity"),
                source = "NHE / Faigin",
                specialTags = listOf("Metabolism", "Energy", "Strength")
            ),
            Protocol(
                id = "deep_work_chamber",
                title = "Deep Work Protocol",
                description = "Professional activities performed in a state of distraction-free concentration that push cognitive capabilities.",
                category = "Productivity",
                canonicalSteps = listOf("Zero distractions", "Defined start/end time", "Single high-value task", "Ritual shutdown"),
                source = "Cal Newport",
                specialTags = listOf("Intelligence", "Focus", "ADHD")
            ),
            Protocol(
                id = "pomodoro_sprints",
                title = "Pomodoro Sprints",
                description = "Time-management method using a timer to break down work into intervals, traditionally 25 minutes in length.",
                category = "Productivity",
                canonicalSteps = listOf("25m high-focus sprint", "5m neural break", "4 sprints total", "30m system recharge"),
                source = "Cirillo",
                specialTags = listOf("ADHD-Friendly", "Focus")
            ),
            Protocol(
                id = "gratitude_rewiring",
                title = "Gratitude Synthesis",
                description = "Daily practice of identifying specific blessings to counteract the brain's negativity bias.",
                category = "Recovery",
                canonicalSteps = listOf("Identify 3 specific things", "Write the 'Why'", "Feel the emotion", "Share one with someone"),
                source = "Positive Psychology",
                specialTags = listOf("Luck", "Mood", "Charisma")
            )
        )

        protocols.forEach { insertProtocol(it) }
    }
}
