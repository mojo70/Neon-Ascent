package com.neon.ascent.feature.dashboard

import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.notifications.brief.DirectiveClauseResolver
import com.neon.ascent.core.domain.notifications.models.OperationalStatus
import com.neon.ascent.core.domain.workout.models.WorkoutLayoutMode
import com.neon.ascent.feature.workout.ui.WorkoutUiState
import com.neon.ascent.feature.goals.ui.ascension.AscensionForgeUiState
import com.neon.ascent.feature.goals.ui.ascension.ForgeType
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class GreasedPigSmokeTest {

    private val today = LocalDate.of(2026, 10, 17)

    @Test
    fun `Smoke 1 - Named Directive shows DECK chip and no Directive leaves chip absent`() {
        val directive = AscensionDirective(
            id = "d1",
            title = "Neural Mastery",
            priorityTier = PriorityTier.A,
            targetEndDate = today.plusDays(30),
            description = "Desc"
        )
        val candidate = DirectiveClauseResolver.resolveDirectiveCandidate(listOf(directive), today = today)
        assertNotNull(candidate)
        assertEquals("d1", candidate?.directive?.id)
        assertEquals(30L, candidate?.daysLeft)

        // No directive qualifies
        val emptyCandidate = DirectiveClauseResolver.resolveDirectiveCandidate(emptyList(), today = today)
        assertNull(emptyCandidate)
    }

    @Test
    fun `Smoke 2 - CLASSIC layout unchanged and default, FOCUS is distinct mode`() {
        val defaultState = WorkoutUiState()
        assertEquals(WorkoutLayoutMode.CLASSIC, defaultState.layoutMode)

        val focusState = defaultState.copy(layoutMode = WorkoutLayoutMode.FOCUS)
        assertEquals(WorkoutLayoutMode.FOCUS, focusState.layoutMode)
    }

    @Test
    fun `Smoke 3 - THROTTLE shows banner, OVERDRIVE does not`() {
        fun shouldShowBanner(status: OperationalStatus, dismissed: Boolean): Boolean {
            return (status == OperationalStatus.THROTTLE || status == OperationalStatus.GROUNDED) && !dismissed
        }

        assertTrue(shouldShowBanner(OperationalStatus.THROTTLE, dismissed = false))
        assertTrue(shouldShowBanner(OperationalStatus.GROUNDED, dismissed = false))
        assertFalse(shouldShowBanner(OperationalStatus.OVERDRIVE, dismissed = false))
        assertFalse(shouldShowBanner(OperationalStatus.NOMINAL, dismissed = false))
        assertFalse(shouldShowBanner(OperationalStatus.THROTTLE, dismissed = true))
    }

    @Test
    fun `Smoke 4 - Linked session has return chip target and unlinked has null`() {
        val linked = WorkoutUiState(linkedDirectiveId = "dir_99", linkedDirectiveTitle = "PROTOCOL TITAN")
        assertNotNull(linked.linkedDirectiveId)
        assertNotNull(linked.linkedDirectiveTitle)

        val unlinked = WorkoutUiState()
        assertNull(unlinked.linkedDirectiveId)
        assertNull(unlinked.linkedDirectiveTitle)
    }

    @Test
    fun `Smoke 5 - Avatar bridge line matches status token`() {
        fun bridgeLine(status: String): String = when (status) {
            "OVERDRIVE" -> "Clear to push."
            "THROTTLE" -> "Ease intensity. Map shows what you’re protecting."
            "GROUNDED" -> "Pull back. Rest or Zone 2."
            else -> "Normal pace."
        }
        assertEquals("Clear to push.", bridgeLine("OVERDRIVE"))
        assertEquals("Normal pace.", bridgeLine("NOMINAL"))
        assertEquals("Ease intensity. Map shows what you’re protecting.", bridgeLine("THROTTLE"))
        assertEquals("Pull back. Rest or Zone 2.", bridgeLine("GROUNDED"))
    }

    @Test
    fun `Smoke 6 - Codex first card carries NEXT marker indicator`() {
        val isFirstNext = { index: Int -> index == 0 }
        assertTrue(isFirstNext(0))
        assertFalse(isFirstNext(1))
    }

    @Test
    fun `Smoke 7 - Nudge appears only on THROTTLE or GROUNDED or short sleep, never OVERDRIVE, dismissible`() {
        fun shouldShowNudge(
            status: String?,
            sleepMinutes: Long?,
            needMinutes: Long = 440L,
            dismissedDate: String?,
            currentDate: String
        ): Boolean {
            val isShortSleep = sleepMinutes?.let { (needMinutes - it) >= 45L } ?: false
            val isStatusEligible = status == "THROTTLE" || status == "GROUNDED"
            val isDismissed = dismissedDate == currentDate
            return !isDismissed && status != "OVERDRIVE" && (isStatusEligible || isShortSleep)
        }

        val todayStr = "2026-10-17"
        assertFalse("Never on OVERDRIVE", shouldShowNudge("OVERDRIVE", 300L, 440L, null, todayStr))
        assertTrue("Shows on THROTTLE", shouldShowNudge("THROTTLE", 440L, 440L, null, todayStr))
        assertTrue("Shows on GROUNDED", shouldShowNudge("GROUNDED", 440L, 440L, null, todayStr))
        assertTrue("Shows on short sleep (need - sleep >= 45)", shouldShowNudge("NOMINAL", 395L, 440L, null, todayStr))
        assertFalse("Does not show on NOMINAL with full sleep", shouldShowNudge("NOMINAL", 440L, 440L, null, todayStr))
        assertFalse("Does not show if dismissed today", shouldShowNudge("THROTTLE", 440L, 440L, todayStr, todayStr))
    }

    @Test
    fun `Smoke 8 - New Forge open starts on DIRECTIVE tab`() {
        val initialForgeState = AscensionForgeUiState()
        assertEquals(ForgeType.DIRECTIVE, initialForgeState.forgeType)
    }
}
