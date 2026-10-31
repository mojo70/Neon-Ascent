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
    fun `type ahead search query filtering matches name alias and mechanism`() {
        val all = SEED_SUPPLEMENTS

        // Search by name
        val tirzMatches = all.filter { s ->
            s.name.contains("Tirzepatide", ignoreCase = true) || s.aliases.any { it.contains("Tirzepatide", ignoreCase = true) }
        }
        assertTrue(tirzMatches.any { it.id == "tirzepatide" })

        // Search by brand alias
        val mounjaroMatches = all.filter { s ->
            s.aliases.any { it.contains("Mounjaro", ignoreCase = true) }
        }
        assertTrue(mounjaroMatches.any { it.id == "tirzepatide" })

        val ozempicMatches = all.filter { s ->
            s.aliases.any { it.contains("Ozempic", ignoreCase = true) }
        }
        assertTrue(ozempicMatches.any { it.id == "semaglutide" })

        // Search by mechanism tag
        val adaptogenMatches = all.filter { s ->
            s.mechanismTags.any { it.contains("Adaptogen", ignoreCase = true) }
        }
        assertTrue(adaptogenMatches.any { it.id == "rhodiola_rosea" })
        assertTrue(adaptogenMatches.any { it.id == "bacopa_monnieri" })
    }

    @Test
    fun `codex ui state holds supplement search query state`() {
        var state = CodexUiState(allSupplements = SEED_SUPPLEMENTS)
        assertEquals("", state.supplementSearchQuery)

        state = state.copy(supplementSearchQuery = "Semaglutide")
        assertEquals("Semaglutide", state.supplementSearchQuery)
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
