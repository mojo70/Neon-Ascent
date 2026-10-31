package com.neon.ascent.core.data.repository

import android.content.ContextWrapper
import com.neon.ascent.core.data.datastore.SupplementPreferencesDataStore
import com.neon.ascent.core.data.local.dao.SupplementDao
import com.neon.ascent.core.data.local.entity.SupplementEntity
import com.neon.ascent.core.data.local.entity.toDomain
import com.neon.ascent.core.data.local.entity.toEntity
import com.neon.ascent.core.domain.labs.models.SEED_SUPPLEMENTS
import com.neon.ascent.core.domain.labs.models.SupplementOverride
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeSupplementDao : SupplementDao {
    private val supplements = mutableMapOf<String, SupplementEntity>()

    override fun getAllSupplements(): Flow<List<SupplementEntity>> = flowOf(supplements.values.toList())

    override fun getSupplementById(id: String): Flow<SupplementEntity?> = flowOf(supplements[id])

    override suspend fun getSupplementByIdSync(id: String): SupplementEntity? = supplements[id]

    override suspend fun getSupplementCount(): Int = supplements.size

    override suspend fun insertSupplements(supplements: List<SupplementEntity>) {
        supplements.forEach { this.supplements[it.id] = it }
    }

    override suspend fun insertSupplement(supplement: SupplementEntity) {
        supplements[supplement.id] = supplement
    }
}

private object SupplementDummyContext : ContextWrapper(null)

class FakeSupplementPreferencesDataStore : SupplementPreferencesDataStore(
    context = SupplementDummyContext
) {
    private val activeIdsState = MutableStateFlow<Set<String>>(emptySet())
    private val overridesState = MutableStateFlow<Map<String, SupplementOverride>>(emptyMap())

    override val activeStackIds: Flow<Set<String>> = activeIdsState.asStateFlow()
    override val overrides: Flow<Map<String, SupplementOverride>> = overridesState.asStateFlow()

    override suspend fun setActiveStackIds(ids: Set<String>) {
        activeIdsState.value = ids
    }

    override suspend fun updateOverride(id: String, override: SupplementOverride?) {
        val current = overridesState.value.toMutableMap()
        if (override == null) {
            current.remove(id)
        } else {
            current[id] = override
        }
        overridesState.value = current
    }

    override suspend fun updateAllOverrides(map: Map<String, SupplementOverride>) {
        overridesState.value = map
    }
}

class SupplementRepositoryTest {

    @Test
    fun `seed supplements completeness - contains 100 entries from database MD`() {
        assertEquals(100, SEED_SUPPLEMENTS.size)

        val ids = SEED_SUPPLEMENTS.map { it.id }.toSet()
        assertTrue(ids.contains("caffeine"))
        assertTrue(ids.contains("l_theanine"))
        assertTrue(ids.contains("creatine_monohydrate"))
        assertTrue(ids.contains("citicoline"))
        assertTrue(ids.contains("rhodiola_rosea"))
        assertTrue(ids.contains("magnesium"))
        assertTrue(ids.contains("vitamin_d3"))
        assertTrue(ids.contains("coq10_ubiquinol"))
        assertTrue(ids.contains("berberine"))
        assertTrue(ids.contains("urolithin_a"))
        assertTrue(ids.contains("glycine"))
        assertTrue(ids.contains("melatonin"))
        assertTrue(ids.contains("nac"))
        assertTrue(ids.contains("taurine"))
        assertTrue(ids.contains("bpc_157"))
        assertTrue(ids.contains("semax"))
        assertTrue(ids.contains("retatrutide"))
        assertTrue(ids.contains("tirzepatide"))
        assertTrue(ids.contains("semaglutide"))
        assertTrue(ids.contains("methylphenidate"))
    }

    @Test
    fun `caffeine representative mapping - dose and non-research-chemical flag`() {
        val caffeine = SEED_SUPPLEMENTS.first { it.id == "caffeine" }

        assertEquals("Caffeine", caffeine.name)
        assertEquals("OTC", caffeine.status)
        assertEquals("A", caffeine.evidenceGrade)
        assertEquals("50–200 mg acute; ≤400 mg/day total", caffeine.typicalDose)
        assertFalse(caffeine.isResearchChemical)
        assertTrue(caffeine.primaryOutcomeDomains.contains("Attention / Focus"))
        assertTrue(caffeine.mechanismTags.contains("Stimulant / Adenosine antagonist"))
    }

    @Test
    fun `stacking notes present for key seed compounds with evidence grade tags`() {
        val caffeine = SEED_SUPPLEMENTS.first { it.id == "caffeine" }
        assertEquals(1, caffeine.stackingNotes.size)
        assertEquals("Grade A", caffeine.stackingNotes.first().evidenceGradeTag)
        assertEquals("l_theanine", caffeine.stackingNotes.first().relatedCompound)

        val creatine = SEED_SUPPLEMENTS.first { it.id == "creatine_monohydrate" }
        assertEquals(1, creatine.stackingNotes.size)
        assertEquals("Grade A", creatine.stackingNotes.first().evidenceGradeTag)

        val tyrosine = SEED_SUPPLEMENTS.first { it.id == "l_tyrosine" }
        assertEquals(1, tyrosine.stackingNotes.size)
        assertEquals("Caution", tyrosine.stackingNotes.first().evidenceGradeTag)
    }

    @Test
    fun `research chemicals marked correctly`() {
        val bpc = SEED_SUPPLEMENTS.first { it.id == "bpc_157" }
        assertTrue("BPC-157 must be marked as research chemical", bpc.isResearchChemical)
        assertEquals("Research", bpc.status)
        assertEquals("250–500 mcg/day subcutaneous/oral", bpc.typicalDose)

        val semax = SEED_SUPPLEMENTS.first { it.id == "semax" }
        assertTrue("Semax must be marked as research chemical", semax.isResearchChemical)
        assertEquals("Research", semax.status)

        val retatrutide = SEED_SUPPLEMENTS.first { it.id == "retatrutide" }
        assertTrue("Retatrutide must be marked as research chemical", retatrutide.isResearchChemical)
        assertEquals("Research", retatrutide.status)
    }

    @Test
    fun `prescription items marked correctly and not as research chemicals`() {
        val ritalin = SEED_SUPPLEMENTS.first { it.id == "methylphenidate" }
        assertEquals("Prescription", ritalin.status)
        assertFalse(ritalin.isResearchChemical)

        val adderall = SEED_SUPPLEMENTS.first { it.id == "amphetamine_based" }
        assertEquals("Prescription", adderall.status)
        assertFalse(adderall.isResearchChemical)

        val rapamycin = SEED_SUPPLEMENTS.first { it.id == "rapamycin" }
        assertEquals("Prescription", rapamycin.status)
        assertFalse(rapamycin.isResearchChemical)

        val tirzepatide = SEED_SUPPLEMENTS.first { it.id == "tirzepatide" }
        assertEquals("Prescription", tirzepatide.status)
        assertFalse(tirzepatide.isResearchChemical)

        val semaglutide = SEED_SUPPLEMENTS.first { it.id == "semaglutide" }
        assertEquals("Prescription", semaglutide.status)
        assertFalse(semaglutide.isResearchChemical)
    }

    @Test
    fun `repository observeAll and observeById fallback to seed when unseeded`() = runBlocking {
        val fakeDao = FakeSupplementDao()
        val fakePrefs = FakeSupplementPreferencesDataStore()
        val repo = SupplementRepositoryImpl(fakeDao, fakePrefs)

        val all = repo.observeAll().first()
        assertEquals(100, all.size)

        val caffeine = repo.observeById("caffeine").first()
        assertNotNull(caffeine)
        assertEquals("Caffeine", caffeine?.name)

        val directBpc = repo.getById("bpc_157")
        assertNotNull(directBpc)
        assertEquals("BPC-157", directBpc?.name)
        assertTrue(directBpc!!.isResearchChemical)
    }

    @Test
    fun `repository seedDefaultSupplements persists entries in DAO`() = runBlocking {
        val fakeDao = FakeSupplementDao()
        val fakePrefs = FakeSupplementPreferencesDataStore()
        val repo = SupplementRepositoryImpl(fakeDao, fakePrefs)

        assertEquals(0, fakeDao.getSupplementCount())

        repo.seedDefaultSupplements()

        assertEquals(100, fakeDao.getSupplementCount())

        val caffeineEntity = fakeDao.getSupplementByIdSync("caffeine")
        assertNotNull(caffeineEntity)
        assertEquals("Caffeine", caffeineEntity?.name)
    }

    @Test
    fun `observeActiveStack allows selecting zero or multiple items`() = runBlocking {
        val fakeDao = FakeSupplementDao()
        val fakePrefs = FakeSupplementPreferencesDataStore()
        val repo = SupplementRepositoryImpl(fakeDao, fakePrefs)

        // Initially empty stack
        val emptyStack = repo.observeActiveStack().first()
        assertTrue(emptyStack.isEmpty())

        // Select caffeine + l_theanine + creatine_monohydrate
        val selectedIds = setOf("caffeine", "l_theanine", "creatine_monohydrate")
        repo.setActiveStack(selectedIds)

        val activeStack = repo.observeActiveStack().first()
        assertEquals(3, activeStack.size)
        val activeStackIds = activeStack.map { it.id }.toSet()
        assertEquals(selectedIds, activeStackIds)

        // Update stack to caffeine only
        repo.setActiveStack(setOf("caffeine"))
        val caffeineOnlyStack = repo.observeActiveStack().first()
        assertEquals(1, caffeineOnlyStack.size)
        assertEquals("caffeine", caffeineOnlyStack.first().id)

        // Clear active stack
        repo.setActiveStack(emptySet())
        val clearedStack = repo.observeActiveStack().first()
        assertTrue(clearedStack.isEmpty())
    }

    @Test
    fun `overrides store custom dose, schedule note, and paused flag without mutating seed`() = runBlocking {
        val fakeDao = FakeSupplementDao()
        val fakePrefs = FakeSupplementPreferencesDataStore()
        val repo = SupplementRepositoryImpl(fakeDao, fakePrefs)

        // Initially empty overrides
        val initialOverrides = repo.observeOverrides().first()
        assertTrue(initialOverrides.isEmpty())

        // Set override for caffeine
        val caffeineOverride = SupplementOverride(
            customDoseNote = "100 mg morning with espresso",
            scheduleNote = "Mon-Fri only",
            isPaused = false
        )
        repo.updateOverride("caffeine", caffeineOverride)

        // Set override for bpc_157
        val bpcOverride = SupplementOverride(
            customDoseNote = "250 mcg daily",
            scheduleNote = "Subcutaneous AM",
            isPaused = true
        )
        repo.updateOverride("bpc_157", bpcOverride)

        val activeOverrides = repo.observeOverrides().first()
        assertEquals(2, activeOverrides.size)

        val fetchedCaffeineOverride = activeOverrides["caffeine"]
        assertNotNull(fetchedCaffeineOverride)
        assertEquals("100 mg morning with espresso", fetchedCaffeineOverride?.customDoseNote)
        assertEquals("Mon-Fri only", fetchedCaffeineOverride?.scheduleNote)
        assertFalse(fetchedCaffeineOverride!!.isPaused)

        val fetchedBpcOverride = activeOverrides["bpc_157"]
        assertNotNull(fetchedBpcOverride)
        assertEquals("250 mcg daily", fetchedBpcOverride?.customDoseNote)
        assertEquals("Subcutaneous AM", fetchedBpcOverride?.scheduleNote)
        assertTrue(fetchedBpcOverride!!.isPaused)

        // Seed catalog entry is unmutated
        val seedCaffeine = repo.observeById("caffeine").first()
        assertEquals("50–200 mg acute; ≤400 mg/day total", seedCaffeine?.typicalDose)

        // Clear caffeine override
        repo.updateOverride("caffeine", null)
        val remainingOverrides = repo.observeOverrides().first()
        assertEquals(1, remainingOverrides.size)
        assertNull(remainingOverrides["caffeine"])
        assertNotNull(remainingOverrides["bpc_157"])
    }

    @Test
    fun `supplement entity toDomain and toEntity roundtrip mapping preserves fields`() {
        val original = SEED_SUPPLEMENTS.first { it.id == "caffeine" }
        val entity = original.toEntity()

        assertEquals(original.id, entity.id)
        assertEquals(original.name, entity.name)
        assertEquals(original.typicalDose, entity.typicalDose)
        assertFalse(entity.isResearchChemical)

        val restored = entity.toDomain()
        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.typicalDose, restored.typicalDose)
        assertEquals(original.isResearchChemical, restored.isResearchChemical)
        assertEquals(original.primaryOutcomeDomains, restored.primaryOutcomeDomains)
        assertEquals(original.mechanismTags, restored.mechanismTags)
        assertEquals(original.keyLinks, restored.keyLinks)
    }
}
