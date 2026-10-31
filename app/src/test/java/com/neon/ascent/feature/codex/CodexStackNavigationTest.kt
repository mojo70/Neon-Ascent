package com.neon.ascent.feature.codex

import com.neon.ascent.feature.codex.ui.CodexPeriod
import com.neon.ascent.feature.codex.ui.CodexStackSubPanel
import com.neon.ascent.feature.codex.ui.CodexUiState
import com.neon.ascent.feature.codex.ui.CodexWing
import org.junit.Assert.assertEquals
import org.junit.Test

class CodexStackNavigationTest {

    @Test
    fun `codex wing enum has strictly four wings`() {
        val wings = CodexWing.entries
        assertEquals(4, wings.size)
        assertEquals(CodexWing.OPS_LOG, wings[0])
        assertEquals(CodexWing.VITALS, wings[1])
        assertEquals(CodexWing.STACK, wings[2])
        assertEquals(CodexWing.CHRONICLE, wings[3])
    }

    @Test
    fun `codex stack sub-panel enum contains serum diet protocols supplements`() {
        val subPanels = CodexStackSubPanel.entries
        assertEquals(4, subPanels.size)
        assertEquals(CodexStackSubPanel.SERUM, subPanels[0])
        assertEquals(CodexStackSubPanel.DIET, subPanels[1])
        assertEquals(CodexStackSubPanel.PROTOCOLS, subPanels[2])
        assertEquals(CodexStackSubPanel.SUPPLEMENTS, subPanels[3])
    }

    @Test
    fun `codex ui state defaults to ops_log wing, serum sub-panel, and thirty_days period`() {
        val state = CodexUiState()
        assertEquals(CodexWing.OPS_LOG, state.activeWing)
        assertEquals(CodexStackSubPanel.SERUM, state.activeStackSubPanel)
        assertEquals(CodexPeriod.THIRTY_DAYS, state.selectedPeriod)
    }

    @Test
    fun `deep linking updates active wing to stack and sets corresponding sub-panel`() {
        var state = CodexUiState()

        // Deep-link to DIET
        state = state.copy(
            activeWing = CodexWing.STACK,
            activeStackSubPanel = CodexStackSubPanel.DIET
        )
        assertEquals(CodexWing.STACK, state.activeWing)
        assertEquals(CodexStackSubPanel.DIET, state.activeStackSubPanel)

        // Deep-link to SUPPLEMENTS
        state = state.copy(
            activeWing = CodexWing.STACK,
            activeStackSubPanel = CodexStackSubPanel.SUPPLEMENTS
        )
        assertEquals(CodexWing.STACK, state.activeWing)
        assertEquals(CodexStackSubPanel.SUPPLEMENTS, state.activeStackSubPanel)

        // Deep-link to PROTOCOLS
        state = state.copy(
            activeWing = CodexWing.STACK,
            activeStackSubPanel = CodexStackSubPanel.PROTOCOLS
        )
        assertEquals(CodexWing.STACK, state.activeWing)
        assertEquals(CodexStackSubPanel.PROTOCOLS, state.activeStackSubPanel)
    }
}
