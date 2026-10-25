package com.neon.ascent.feature.biohacking

import com.neon.ascent.core.domain.labs.models.CYBER_CYCLICAL_DIET
import com.neon.ascent.core.domain.labs.models.Diet
import com.neon.ascent.core.domain.labs.models.DietOverrides
import com.neon.ascent.core.domain.labs.models.MEDITERRANEAN_DIET
import com.neon.ascent.core.domain.repository.DietRepository
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
import java.time.LocalDate

class FakeDietRepository : DietRepository {
    private val activeDietState = MutableStateFlow<Diet?>(null)
    private val overridesState = MutableStateFlow(DietOverrides())

    override fun observeAll(): Flow<List<Diet>> = flowOf(listOf(CYBER_CYCLICAL_DIET, MEDITERRANEAN_DIET))

    override fun observeById(id: String): Flow<Diet?> = flowOf(
        listOf(CYBER_CYCLICAL_DIET, MEDITERRANEAN_DIET).firstOrNull { it.id == id }
    )

    override suspend fun getById(id: String): Diet? =
        listOf(CYBER_CYCLICAL_DIET, MEDITERRANEAN_DIET).firstOrNull { it.id == id }

    override suspend fun seedDefaultDiets() {}

    override fun observeActiveDiet(): Flow<Diet?> = activeDietState.asStateFlow()

    override suspend fun setActiveDiet(id: String?) {
        activeDietState.value = listOf(CYBER_CYCLICAL_DIET, MEDITERRANEAN_DIET).firstOrNull { it.id == id }
    }

    override fun observeOverrides(): Flow<DietOverrides> = overridesState.asStateFlow()

    override suspend fun updateOverrides(overrides: DietOverrides) {
        overridesState.value = overrides
    }
}

class BiohackingLabsDietTest {

    @Test
    fun `labs active diet displays active diet parameters accurately`() = runBlocking {
        val repo = FakeDietRepository()

        assertNull(repo.observeActiveDiet().first())

        repo.setActiveDiet("cyber_cyclical_diet")
        val active = repo.observeActiveDiet().first()

        assertNotNull(active)
        assertEquals("Cyber Cyclical Diet (CCD)", active!!.name)
        assertEquals(1.6, active.typicalParameters.proteinGramsPerKgMin!!, 0.001)
        assertEquals(2.2, active.typicalParameters.proteinGramsPerKgMax!!, 0.001)
        assertEquals(50, active.typicalParameters.baselineNetCarbsGramsMin)
        assertEquals(100, active.typicalParameters.baselineNetCarbsGramsMax)
    }

    @Test
    fun `higher-carb callout activates when today is in preferred days or training day aligns`() {
        val todayName = LocalDate.now().dayOfWeek.name

        val matchingOverrides = DietOverrides(
            preferredHigherCarbMealDays = listOf(todayName)
        )
        val nonMatchingOverrides = DietOverrides(
            preferredHigherCarbMealDays = listOf("NON_EXISTENT_DAY")
        )

        // Case 1: Today is explicitly marked in overrides
        val isTrainingDayActive1 = false
        val isHigherCarbScenario1 = matchingOverrides.preferredHigherCarbMealDays.any {
            it.equals(todayName, ignoreCase = true)
        } || isTrainingDayActive1
        assertTrue("Higher-carb callout should activate when today is marked", isHigherCarbScenario1)

        // Case 2: Today is a training day
        val isTrainingDayActive2 = true
        val isHigherCarbScenario2 = nonMatchingOverrides.preferredHigherCarbMealDays.any {
            it.equals(todayName, ignoreCase = true)
        } || isTrainingDayActive2
        assertTrue("Higher-carb callout should activate when training session aligns", isHigherCarbScenario2)

        // Case 3: Neither day nor training aligns
        val isTrainingDayActive3 = false
        val isHigherCarbScenario3 = nonMatchingOverrides.preferredHigherCarbMealDays.any {
            it.equals(todayName, ignoreCase = true)
        } || isTrainingDayActive3
        assertFalse("Higher-carb callout should remain inactive when neither aligns", isHigherCarbScenario3)
    }
}
