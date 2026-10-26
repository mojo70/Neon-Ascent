package com.neon.ascent.feature.codex

import com.neon.ascent.core.domain.labs.models.BiohackingProtocolOverride
import com.neon.ascent.core.domain.labs.models.SEED_BIOHACKING_PROTOCOLS
import com.neon.ascent.feature.codex.ui.CodexUiState
import com.neon.ascent.feature.codex.ui.VitalsGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodexProtocolArchiveTest {

    @Test
    fun `vitals group enum includes PROTO group`() {
        val protoGroup = VitalsGroup.valueOf("PROTO")
        assertEquals("PROTO", protoGroup.name)
        assertEquals("PROTO", protoGroup.label)
    }

    @Test
    fun `outcome domain filter correctly filters seed protocol list`() {
        val allProtocols = SEED_BIOHACKING_PROTOCOLS

        // Filter ALL
        val allFiltered = allProtocols
        assertEquals(26, allFiltered.size)

        // Filter Longevity / Healthspan
        val longevityFilter = "Longevity / Healthspan"
        val longevityFiltered = allProtocols.filter { it.primaryOutcomeDomains.contains(longevityFilter) }
        assertTrue(longevityFiltered.any { it.id == "finnish_sauna" })
        assertTrue(longevityFiltered.any { it.id == "zone_2_aerobic" })
        assertTrue(longevityFiltered.any { it.id == "resistance_training_longevity" })

        // Filter Sleep Architecture
        val sleepFilter = "Sleep Architecture"
        val sleepFiltered = allProtocols.filter { it.primaryOutcomeDomains.contains(sleepFilter) }
        assertTrue(sleepFiltered.any { it.id == "morning_outdoor_light" })
        assertTrue(sleepFiltered.any { it.id == "cbt_i" })
        assertTrue(sleepFiltered.any { it.id == "evening_light_restriction" })
    }

    @Test
    fun `codex ui state holds protocol selection detail and active indicators`() {
        val sauna = SEED_BIOHACKING_PROTOCOLS.first { it.id == "finnish_sauna" }
        val zone2 = SEED_BIOHACKING_PROTOCOLS.first { it.id == "zone_2_aerobic" }

        var state = CodexUiState(
            allProtocols = SEED_BIOHACKING_PROTOCOLS,
            activeProtocols = listOf(sauna)
        )

        assertEquals(1, state.activeProtocols.size)
        assertEquals("finnish_sauna", state.activeProtocols.first().id)
        assertNull(state.selectedProtocolDetail)

        // Open detail for Zone 2
        state = state.copy(selectedProtocolDetail = zone2)
        assertNotNull(state.selectedProtocolDetail)
        assertEquals("Zone 2 Aerobic Training", state.selectedProtocolDetail?.name)

        // Set override for sauna
        val saunaOverride = BiohackingProtocolOverride(
            preferredFrequency = "4x/week",
            durationNote = "20 min",
            isPaused = false
        )
        state = state.copy(protocolOverrides = mapOf("finnish_sauna" to saunaOverride))
        assertEquals("4x/week", state.protocolOverrides["finnish_sauna"]?.preferredFrequency)

        // Close detail selection
        state = state.copy(selectedProtocolDetail = null)
        assertNull(state.selectedProtocolDetail)
    }
}
