package com.neon.ascent.core.data.repository

import android.content.ContextWrapper
import com.neon.ascent.core.data.datastore.DietPreferencesDataStore
import com.neon.ascent.core.data.local.dao.DietDao
import com.neon.ascent.core.data.local.entity.DietEntity
import com.neon.ascent.core.data.local.entity.toDomain
import com.neon.ascent.core.data.local.entity.toEntity
import com.neon.ascent.core.domain.labs.models.CYBER_CYCLICAL_DIET
import com.neon.ascent.core.domain.labs.models.DietOverrides
import com.neon.ascent.core.domain.labs.models.SEED_DIETS
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

class FakeDietDao : DietDao {
    private val diets = mutableMapOf<String, DietEntity>()

    override fun getAllDiets(): Flow<List<DietEntity>> = flowOf(diets.values.toList())

    override fun getDietById(id: String): Flow<DietEntity?> = flowOf(diets[id])

    override suspend fun getDietByIdSync(id: String): DietEntity? = diets[id]

    override suspend fun getDietCount(): Int = diets.size

    override suspend fun insertDiets(diets: List<DietEntity>) {
        diets.forEach { this.diets[it.id] = it }
    }

    override suspend fun insertDiet(diet: DietEntity) {
        diets[diet.id] = diet
    }
}

private object DummyContext : ContextWrapper(null)

class FakeDietPreferencesDataStore : DietPreferencesDataStore(
    context = DummyContext
) {
    private val activeDietIdState = MutableStateFlow<String?>(null)
    private val overridesState = MutableStateFlow(DietOverrides())

    override val activeDietId: Flow<String?> = activeDietIdState.asStateFlow()
    override val dietOverrides: Flow<DietOverrides> = overridesState.asStateFlow()

    override suspend fun setActiveDietId(id: String?) {
        activeDietIdState.value = id
    }

    override suspend fun updateDietOverrides(overrides: DietOverrides) {
        overridesState.value = overrides
    }
}

class DietRepositoryTest {

    @Test
    fun `seed diets completeness - contains exact 5 non-plant entries`() {
        assertEquals(5, SEED_DIETS.size)

        val ids = SEED_DIETS.map { it.id }.toSet()
        val expectedIds = setOf(
            "cyber_cyclical_diet",
            "mediterranean_diet",
            "time_restricted_eating_16_8",
            "ketogenic_diet",
            "high_protein_focus"
        )
        assertEquals(expectedIds, ids)

        // Verify no plant-based patterns in seed
        SEED_DIETS.forEach { diet ->
            assertFalse(
                "Plant-based pattern found in seed: ${diet.name}",
                diet.name.lowercase().contains("plant") || diet.name.lowercase().contains("vegan")
            )
        }
    }

    @Test
    fun `cyber cyclical diet parameters mapped correctly`() {
        val ccd = CYBER_CYCLICAL_DIET

        assertEquals("cyber_cyclical_diet", ccd.id)
        assertEquals("Cyber Cyclical Diet (CCD)", ccd.name)
        assertEquals("App-Specific", ccd.status)
        assertTrue(ccd.isAppSpecific)

        val params = ccd.typicalParameters
        assertEquals(1.6, params.proteinGramsPerKgMin!!, 0.001)
        assertEquals(2.2, params.proteinGramsPerKgMax!!, 0.001)
        assertEquals(50, params.baselineNetCarbsGramsMin)
        assertEquals(100, params.baselineNetCarbsGramsMax)
        assertEquals(1, params.higherCarbMealsPerWeekMin)
        assertEquals(2, params.higherCarbMealsPerWeekMax)
        assertEquals("Post-training or evening preferred", params.higherCarbTimingPreference)

        assertNotNull(params.freeVegetablesDescription)
        assertTrue(params.freeVegetablesDescription!!.lowercase().contains("leafy greens"))
        assertTrue(params.freeVegetablesDescription!!.lowercase().contains("broccoli"))

        assertNotNull(params.netCarbCalculationRule)
        assertTrue(params.netCarbCalculationRule!!.contains("Net carbs for whole foods"))

        // Verify primary outcome domains and mechanism tags
        assertTrue(ccd.primaryOutcomeDomains.contains("Physical Performance / Recovery"))
        assertTrue(ccd.primaryOutcomeDomains.contains("Longevity / Healthspan"))
        assertTrue(ccd.primaryOutcomeDomains.contains("Mental Energy / Fatigue Resistance"))

        assertTrue(ccd.mechanismTags.contains("Protein leverage / muscle protein synthesis"))
        assertTrue(ccd.mechanismTags.contains("Metabolic flexibility"))
        assertTrue(ccd.mechanismTags.contains("Insulin sensitivity / glycemic control"))

        // Evidence grade
        assertTrue(ccd.evidenceGrade.startsWith("B for higher-protein"))
    }

    @Test
    fun `stacking notes populated for seed entries with evidence grade tags`() {
        val ccd = CYBER_CYCLICAL_DIET
        assertEquals(3, ccd.stackingNotes.size)
        assertTrue(ccd.stackingNotes.any { it.title.contains("Resistance Training") && it.evidenceGradeTag == "Grade A" })
        assertTrue(ccd.stackingNotes.any { it.title.contains("Higher-Carb Refeeds") && it.evidenceGradeTag == "Grade B" })
        assertTrue(ccd.stackingNotes.any { it.title.contains("TRE + CCD Stacking") && it.evidenceGradeTag == "Grade B" })

        val med = com.neon.ascent.core.domain.labs.models.MEDITERRANEAN_DIET
        assertEquals(1, med.stackingNotes.size)
        assertEquals("Grade A", med.stackingNotes.first().evidenceGradeTag)

        val tre = com.neon.ascent.core.domain.labs.models.TIME_RESTRICTED_EATING
        assertEquals(1, tre.stackingNotes.size)
        assertEquals("Grade B", tre.stackingNotes.first().evidenceGradeTag)
    }

    @Test
    fun `isAppSpecific is true for CCD and false for all other seed entries`() {
        SEED_DIETS.forEach { diet ->
            if (diet.id == "cyber_cyclical_diet") {
                assertTrue("CCD must have isAppSpecific = true", diet.isAppSpecific)
            } else {
                assertFalse("${diet.name} must have isAppSpecific = false", diet.isAppSpecific)
            }
        }
    }

    @Test
    fun `repository observeAll and observeById return seed data when unseeded`() = runBlocking {
        val fakeDao = FakeDietDao()
        val fakePrefs = FakeDietPreferencesDataStore()
        val repo = DietRepositoryImpl(fakeDao, fakePrefs)

        val allDiets = repo.observeAll().first()
        assertEquals(5, allDiets.size)

        val ccd = repo.observeById("cyber_cyclical_diet").first()
        assertNotNull(ccd)
        assertEquals("Cyber Cyclical Diet (CCD)", ccd?.name)

        val directCcd = repo.getById("cyber_cyclical_diet")
        assertNotNull(directCcd)
        assertEquals("Cyber Cyclical Diet (CCD)", directCcd?.name)
    }

    @Test
    fun `repository seedDefaultDiets persists entries in DAO`() = runBlocking {
        val fakeDao = FakeDietDao()
        val fakePrefs = FakeDietPreferencesDataStore()
        val repo = DietRepositoryImpl(fakeDao, fakePrefs)

        assertEquals(0, fakeDao.getDietCount())

        repo.seedDefaultDiets()

        assertEquals(5, fakeDao.getDietCount())

        val ccdEntity = fakeDao.getDietByIdSync("cyber_cyclical_diet")
        assertNotNull(ccdEntity)
        assertEquals("Cyber Cyclical Diet (CCD)", ccdEntity?.name)
    }

    @Test
    fun `observeActiveDiet is null by default and updates when active diet selected`() = runBlocking {
        val fakeDao = FakeDietDao()
        val fakePrefs = FakeDietPreferencesDataStore()
        val repo = DietRepositoryImpl(fakeDao, fakePrefs)

        // Verified: Not defaulted to CCD
        assertNull(repo.observeActiveDiet().first())

        // Set active diet to CCD
        repo.setActiveDiet("cyber_cyclical_diet")
        val activeCcd = repo.observeActiveDiet().first()
        assertNotNull(activeCcd)
        assertEquals("Cyber Cyclical Diet (CCD)", activeCcd?.name)

        // Set active diet to Mediterranean
        repo.setActiveDiet("mediterranean_diet")
        val activeMed = repo.observeActiveDiet().first()
        assertNotNull(activeMed)
        assertEquals("Mediterranean Diet", activeMed?.name)

        // Clear active diet
        repo.setActiveDiet(null)
        assertNull(repo.observeActiveDiet().first())
    }

    @Test
    fun `updateOverrides persists user preferences separately without mutating seed catalog`() = runBlocking {
        val fakeDao = FakeDietDao()
        val fakePrefs = FakeDietPreferencesDataStore()
        val repo = DietRepositoryImpl(fakeDao, fakePrefs)

        val initialOverrides = repo.observeOverrides().first()
        assertNull(initialOverrides.preferredProteinGramsPerKg)
        assertNull(initialOverrides.preferredModestCarbTargetGrams)
        assertTrue(initialOverrides.preferredHigherCarbMealDays.isEmpty())

        val newOverrides = DietOverrides(
            preferredProteinGramsPerKg = 2.0,
            preferredModestCarbTargetGrams = 75,
            preferredHigherCarbMealDays = listOf("TUESDAY", "SATURDAY"),
            preferredHigherCarbMealsPerWeek = 2,
            freeListExceptions = listOf("sweet_corn"),
            notes = "Target 2.0g/kg protein on workout days"
        )

        repo.updateOverrides(newOverrides)

        val savedOverrides = repo.observeOverrides().first()
        assertEquals(2.0, savedOverrides.preferredProteinGramsPerKg!!, 0.001)
        assertEquals(75, savedOverrides.preferredModestCarbTargetGrams)
        assertEquals(listOf("TUESDAY", "SATURDAY"), savedOverrides.preferredHigherCarbMealDays)
        assertEquals(2, savedOverrides.preferredHigherCarbMealsPerWeek)
        assertEquals(listOf("sweet_corn"), savedOverrides.freeListExceptions)
        assertEquals("Target 2.0g/kg protein on workout days", savedOverrides.notes)

        // Verify seed catalog entry remains unmutated
        val ccd = repo.observeById("cyber_cyclical_diet").first()
        assertNotNull(ccd)
        assertEquals(1.6, ccd!!.typicalParameters.proteinGramsPerKgMin!!, 0.001)
        assertEquals(2.2, ccd.typicalParameters.proteinGramsPerKgMax!!, 0.001)
    }

    @Test
    fun `diet entity toDomain and toEntity roundtrip mapping preserves parameters`() {
        val original = CYBER_CYCLICAL_DIET
        val entity = original.toEntity()

        assertEquals(original.id, entity.id)
        assertEquals(original.name, original.name)
        assertTrue(entity.isAppSpecific)

        val restored = entity.toDomain()
        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.isAppSpecific, restored.isAppSpecific)

        val restoredParams = restored.typicalParameters
        assertEquals(1.6, restoredParams.proteinGramsPerKgMin!!, 0.001)
        assertEquals(2.2, restoredParams.proteinGramsPerKgMax!!, 0.001)
        assertEquals(50, restoredParams.baselineNetCarbsGramsMin)
        assertEquals(100, restoredParams.baselineNetCarbsGramsMax)
        assertEquals(1, restoredParams.higherCarbMealsPerWeekMin)
        assertEquals(2, restoredParams.higherCarbMealsPerWeekMax)
        assertEquals("Post-training or evening preferred", restoredParams.higherCarbTimingPreference)
    }
}
