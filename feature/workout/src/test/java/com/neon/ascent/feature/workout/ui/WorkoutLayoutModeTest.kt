package com.neon.ascent.feature.workout.ui

import com.neon.ascent.core.domain.notifications.models.OperationalStatus
import com.neon.ascent.core.domain.workout.models.WorkoutLayoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WorkoutLayoutModeTest {

    @Test
    fun `WorkoutLayoutMode default is CLASSIC`() {
        val state = WorkoutUiState()
        assertEquals(WorkoutLayoutMode.CLASSIC, state.layoutMode)
    }

    @Test
    fun `OperationalStatus default is NOMINAL and status banner is not dismissed`() {
        val state = WorkoutUiState()
        assertEquals(OperationalStatus.NOMINAL, state.operationalStatus)
        assertFalse(state.isStatusBannerDismissed)
    }

    @Test
    fun `Linked directive return chip info is preserved`() {
        val state = WorkoutUiState(
            linkedDirectiveId = "d1",
            linkedDirectiveTitle = "BECOME NETRUNNER"
        )
        assertEquals("d1", state.linkedDirectiveId)
        assertEquals("BECOME NETRUNNER", state.linkedDirectiveTitle)
    }
}
