package com.neon.ascent.feature.codex

import com.neon.ascent.core.domain.labs.models.SEED_SUPPLEMENTS
import com.neon.ascent.core.domain.labs.models.SupplementOverride
import com.neon.ascent.feature.codex.ui.CodexUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodexSupplementArchiveTest {

    @Test
    fun `outcome domain filtering returns correct subset`() {
        val all = SEED_SUPPLEMENTS

        val focusSupplements = all.filter { it.primaryOutcomeDomains.contains("Attention / Focus") }
        assertTrue(focusSupplements.isNotEmpty())
        assertTrue(focusSupplements.any { it.id == "caffeine" })
        assertTrue(focusSupplements.any { it.id == "l_theanine" })

        val longevitySupplements = all.filter { it.primaryOutcomeDomains.contains("Longevity / Healthspan") }
        assertTrue(longevitySupplements.isNotEmpty())
        assertTrue(longevitySupplements.any { it.id == "rapamycin" })
        assertTrue(longevitySupplements.any { it.id == "berberine" })
    }

    @Test
    fun `codex ui state holds supplement detail selection and filtering`() {
        var state = CodexUiState(allSupplements = SEED_SUPPLEMENTS)

        val caffeine = SEED_SUPPLEMENTS.first { it.id == "caffeine" }
        state = state.copy(selectedSupplementDetail = caffeine)
        assertNotNull(state.selectedSupplementDetail)
        assertEquals("Caffeine", state.selectedSupplementDetail?.name)

        state = state.copy(selectedSupplementOutcomeDomainFilter = "Memory")
        assertEquals("Memory", state.selectedSupplementOutcomeDomainFilter)

        state = state.copy(selectedSupplementDetail = null)
        assertNull(state.selectedSupplementDetail)
    }

    @Test
    fun `supplement overrides state mapping`() {
        val overridesMap = mapOf(
            "caffeine" to SupplementOverride(customDoseNote = "100 mg AM", isPaused = false),
            "bpc_157" to SupplementOverride(scheduleNote = "Subcutaneous AM", isPaused = true)
        )

        val state = CodexUiState(
            allSupplements = SEED_SUPPLEMENTS,
            supplementOverrides = overridesMap
        )

        assertEquals(2, state.supplementOverrides.size)
        assertEquals("100 mg AM", state.supplementOverrides["caffeine"]?.customDoseNote)
        assertTrue(state.supplementOverrides["bpc_157"]!!.isPaused)
    }
}
