package com.neon.ascent.core.data.repository

import android.content.ContextWrapper
import com.neon.ascent.core.data.datastore.BiohackingProtocolPreferencesDataStore
import com.neon.ascent.core.data.local.dao.BiohackingProtocolDao
import com.neon.ascent.core.data.local.entity.BiohackingProtocolEntity
import com.neon.ascent.core.data.local.entity.toDomain
import com.neon.ascent.core.data.local.entity.toEntity
import com.neon.ascent.core.domain.labs.models.BiohackingProtocolOverride
import com.neon.ascent.core.domain.labs.models.SEED_BIOHACKING_PROTOCOLS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeBiohackingProtocolDao : BiohackingProtocolDao {
    private val protocols = mutableMapOf<String, BiohackingProtocolEntity>()

    override fun getAllProtocols(): Flow<List<BiohackingProtocolEntity>> = flowOf(protocols.values.toList())

    override fun getProtocolById(id: String): Flow<BiohackingProtocolEntity?> = flowOf(protocols[id])

    override suspend fun getProtocolByIdSync(id: String): BiohackingProtocolEntity? = protocols[id]

    override suspend fun getProtocolCount(): Int = protocols.size

    override suspend fun insertProtocols(protocols: List<BiohackingProtocolEntity>) {
        protocols.forEach { this.protocols[it.id] = it }
    }

    override suspend fun insertProtocol(protocol: BiohackingProtocolEntity) {
        protocols[protocol.id] = protocol
    }
}

private object ProtocolDummyContext : ContextWrapper(null)

class FakeBiohackingProtocolPreferencesDataStore : BiohackingProtocolPreferencesDataStore(
    context = ProtocolDummyContext
) {
    private val activeIdsState = MutableStateFlow<Set<String>>(emptySet())
    private val overridesState = MutableStateFlow<Map<String, BiohackingProtocolOverride>>(emptyMap())

    override val activeProtocolIds: Flow<Set<String>> = activeIdsState.asStateFlow()
    override val overrides: Flow<Map<String, BiohackingProtocolOverride>> = overridesState.asStateFlow()

    override suspend fun setActiveProtocolIds(ids: Set<String>) {
        activeIdsState.value = ids
    }

    override suspend fun updateOverride(id: String, override: BiohackingProtocolOverride?) {
        val map = overridesState.value.toMutableMap()
        if (override == null) {
            map.remove(id)
        } else {
            map[id] = override
        }
        overridesState.value = map
    }

    override suspend fun updateAllOverrides(map: Map<String, BiohackingProtocolOverride>) {
        overridesState.value = map
    }
}

class BiohackingProtocolRepositoryTest {

    @Test
    fun `seed protocols completeness - contains exact 26 entries from database specification`() {
        assertEquals(26, SEED_BIOHACKING_PROTOCOLS.size)

        val ids = SEED_BIOHACKING_PROTOCOLS.map { it.id }.toSet()
        val expectedIds = setOf(
            "finnish_sauna",
            "cold_water_immersion",
            "morning_outdoor_light",
            "zone_2_aerobic",
            "resistance_training_longevity",
            "cbt_i",
            "slow_paced_breathing",
            "hrv_biofeedback",
            "photobiomodulation_pre_exercise",
            "contrast_water_therapy",
            "evening_light_restriction",
            "sleep_restriction_therapy",
            "stimulus_control_therapy",
            "hiit_4x4",
            "yoga_nidra_nsdr",
            "heat_acclimation",
            "progressive_muscle_relaxation",
            "blue_light_blocking_glasses",
            "active_recovery",
            "consistent_sleep_wake_timing",
            "resistance_training_progressive_overload",
            "hot_water_immersion",
            "meditation_mindfulness",
            "intermittent_hypoxic_training",
            "compression_garments",
            "polarized_training_model"
        )
        assertEquals(expectedIds, ids)
    }

    @Test
    fun `representative parameter mappings - Finnish Sauna`() {
        val sauna = SEED_BIOHACKING_PROTOCOLS.first { it.id == "finnish_sauna" }

        assertEquals("Finnish-Style Sauna Bathing", sauna.name)
        assertEquals("Established", sauna.status)
        assertEquals("Thermal / Hormetic Stress", sauna.protocolCategory)
        assertEquals("A", sauna.evidenceGrade)
        assertTrue(sauna.typicalParameters.contains("15–20+ min at 80–100 °C"))
        assertTrue(sauna.primaryOutcomeDomains.contains("Longevity / Healthspan"))
        assertTrue(sauna.mechanismTags.contains("Hormesis / Heat shock proteins"))
        assertEquals("Middle-aged adults (large Finnish cohorts)", sauna.bestContexts)
    }

    @Test
    fun `representative parameter mappings - Zone 2 Aerobic`() {
        val zone2 = SEED_BIOHACKING_PROTOCOLS.first { it.id == "zone_2_aerobic" }

        assertEquals("Zone 2 Aerobic Training", zone2.name)
        assertEquals("Established", zone2.status)
        assertEquals("Structured Training", zone2.protocolCategory)
        assertEquals("A", zone2.evidenceGrade)
        assertTrue(zone2.typicalParameters.contains("150–300+ min/week"))
        assertTrue(zone2.mechanismTags.contains("Mitochondrial biogenesis"))
    }

    @Test
    fun `representative parameter mappings - Cold Water Immersion`() {
        val cwi = SEED_BIOHACKING_PROTOCOLS.first { it.id == "cold_water_immersion" }

        assertEquals("Cold-Water Immersion (Post-Exercise)", cwi.name)
        assertEquals("Recovery Modalities", cwi.protocolCategory)
        assertEquals("B", cwi.evidenceGrade)
        assertTrue(cwi.typicalParameters.contains("10–15 min at 5–15 °C"))
    }

    @Test
    fun `entity conversion roundtrip`() {
        val original = SEED_BIOHACKING_PROTOCOLS.first { it.id == "finnish_sauna" }
        val entity = original.toEntity()
        val converted = entity.toDomain()

        assertEquals(original.id, converted.id)
        assertEquals(original.name, converted.name)
        assertEquals(original.aliases, converted.aliases)
        assertEquals(original.status, converted.status)
        assertEquals(original.primaryOutcomeDomains, converted.primaryOutcomeDomains)
        assertEquals(original.protocolCategory, converted.protocolCategory)
        assertEquals(original.mechanismTags, converted.mechanismTags)
        assertEquals(original.evidenceGrade, converted.evidenceGrade)
        assertEquals(original.bestContexts, converted.bestContexts)
        assertEquals(original.typicalParameters, converted.typicalParameters)
        assertEquals(original.onset, converted.onset)
        assertEquals(original.keyLinks, converted.keyLinks)
        assertEquals(original.safetyNotes, converted.safetyNotes)
        assertEquals(original.notes, converted.notes)
    }

    @Test
    fun `repository observe and seed operations`() = runBlocking {
        val dao = FakeBiohackingProtocolDao()
        val prefs = FakeBiohackingProtocolPreferencesDataStore()
        val repository = BiohackingProtocolRepositoryImpl(dao, prefs)

        // Initially empty DAO returns seed list as fallback
        val initialList = repository.observeAll().first()
        assertEquals(26, initialList.size)

        // Seeding populates DAO
        repository.seedDefaultProtocols()
        val seededCount = dao.getProtocolCount()
        assertEquals(26, seededCount)

        val retrieved = repository.getById("finnish_sauna")
        assertNotNull(retrieved)
        assertEquals("Finnish-Style Sauna Bathing", retrieved?.name)
    }

    @Test
    fun `active protocols selection and overrides`() = runBlocking {
        val dao = FakeBiohackingProtocolDao()
        val prefs = FakeBiohackingProtocolPreferencesDataStore()
        val repository = BiohackingProtocolRepositoryImpl(dao, prefs)

        repository.seedDefaultProtocols()

        // Initially no active protocols
        assertTrue(repository.observeActiveProtocols().first().isEmpty())

        // Set active protocols (multiple selection)
        repository.setActiveProtocols(setOf("finnish_sauna", "zone_2_aerobic"))
        val active = repository.observeActiveProtocols().first()
        assertEquals(2, active.size)
        assertTrue(active.map { it.id }.containsAll(listOf("finnish_sauna", "zone_2_aerobic")))

        // Add override without mutating seed protocol
        val override = BiohackingProtocolOverride(
            preferredFrequency = "4x per week",
            durationNote = "20 min per session",
            isPaused = false
        )
        repository.updateOverride("finnish_sauna", override)

        val overridesMap = repository.observeOverrides().first()
        assertEquals(1, overridesMap.size)
        assertEquals("4x per week", overridesMap["finnish_sauna"]?.preferredFrequency)
        assertEquals("20 min per session", overridesMap["finnish_sauna"]?.durationNote)

        // Seed data remains unchanged
        val originalSauna = repository.getById("finnish_sauna")
        assertNotNull(originalSauna)
        assertEquals("Finnish-Style Sauna Bathing", originalSauna?.name)
    }
}
