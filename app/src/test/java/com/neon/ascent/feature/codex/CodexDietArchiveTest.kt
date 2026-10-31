package com.neon.ascent.feature.codex

import com.neon.ascent.core.domain.labs.models.CYBER_CYCLICAL_DIET
import com.neon.ascent.core.domain.labs.models.LabsFocus
import com.neon.ascent.core.domain.labs.models.MEDITERRANEAN_DIET
import com.neon.ascent.core.domain.labs.models.OutcomeDomain
import com.neon.ascent.core.domain.labs.models.SEED_DIETS
import com.neon.ascent.core.domain.labs.models.rankItems
import com.neon.ascent.feature.codex.ui.CodexUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodexDietArchiveTest {

    @Test
    fun `outcome domain filter correctly filters seed diet list`() {
        val allDiets = SEED_DIETS

        // Filter ALL
        val allFiltered = if ("ALL" == "ALL") allDiets else allDiets.filter { it.primaryOutcomeDomains.contains("ALL") }
        assertEquals(5, allFiltered.size)

        // Filter Physical Performance / Recovery
        val perfFilter = "Physical Performance / Recovery"
        val perfFiltered = allDiets.filter { it.primaryOutcomeDomains.contains(perfFilter) }
        assertTrue(perfFiltered.any { it.id == "cyber_cyclical_diet" })
        assertTrue(perfFiltered.any { it.id == "mediterranean_diet" })

        // Filter Longevity / Healthspan
        val longevityFilter = "Longevity / Healthspan"
        val longevityFiltered = allDiets.filter { it.primaryOutcomeDomains.contains(longevityFilter) }
        assertTrue(longevityFiltered.any { it.id == "cyber_cyclical_diet" })
        assertTrue(longevityFiltered.any { it.id == "mediterranean_diet" })
        assertTrue(longevityFiltered.any { it.id == "time_restricted_eating_16_8" })
    }

    @Test
    fun `codex ui state holds diet detail selection and active indicator state`() {
        var state = CodexUiState(
            allDiets = SEED_DIETS,
            activeDiet = CYBER_CYCLICAL_DIET
        )

        assertEquals("Cyber Cyclical Diet (CCD)", state.activeDiet?.name)
        assertNull(state.selectedDietDetail)

        // Select detail for Mediterranean Diet
        state = state.copy(selectedDietDetail = MEDITERRANEAN_DIET)
        assertNotNull(state.selectedDietDetail)
        assertEquals("Mediterranean Diet", state.selectedDietDetail?.name)

        // Clear detail selection
        state = state.copy(selectedDietDetail = null)
        assertNull(state.selectedDietDetail)
    }

    @Test
    fun `labs focus ranks diets with primary matches first`() {
        val focus = LabsFocus(
            primary = OutcomeDomain.PHYSICAL_PERFORMANCE_RECOVERY,
            supporting = setOf(OutcomeDomain.SLEEP_ARCHITECTURE)
        )
        val state = CodexUiState(
            allDiets = SEED_DIETS,
            labsFocus = focus
        )

        val ranked = state.labsFocus.rankItems(state.allDiets) { it.primaryOutcomeDomains }
        assertTrue(ranked.first().primaryOutcomeDomains.contains("Physical Performance / Recovery"))
    }
}
