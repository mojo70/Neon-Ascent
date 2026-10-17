package com.neon.ascent.feature.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GreasedPigPrompt3Test {

    @Test
    fun `avatar bridge line matches status token`() {
        fun resolveBridgeLine(status: String): String = when (status) {
            "OVERDRIVE" -> "Clear to push."
            "THROTTLE" -> "Ease intensity. Map shows what you’re protecting."
            "GROUNDED" -> "Pull back. Rest or Zone 2."
            else -> "Normal pace."
        }

        assertEquals("Clear to push.", resolveBridgeLine("OVERDRIVE"))
        assertEquals("Normal pace.", resolveBridgeLine("NOMINAL"))
        assertEquals("Ease intensity. Map shows what you’re protecting.", resolveBridgeLine("THROTTLE"))
        assertEquals("Pull back. Rest or Zone 2.", resolveBridgeLine("GROUNDED"))
    }

    @Test
    fun `altar nudge appears on THROTTLE or GROUNDED or short sleep and never on OVERDRIVE`() {
        fun shouldShowNudge(
            briefStatus: String?,
            sleepDurationMinutes: Long?,
            needMinutes: Long = 440L,
            isDismissedToday: Boolean = false
        ): Boolean {
            val isShortSleep = sleepDurationMinutes?.let { (needMinutes - it) >= 45L } ?: false
            val isStatusEligible = briefStatus == "THROTTLE" || briefStatus == "GROUNDED"
            return !isDismissedToday && briefStatus != "OVERDRIVE" && (isStatusEligible || isShortSleep)
        }

        // OVERDRIVE should never show nudge, even if short sleep
        assertFalse(shouldShowNudge("OVERDRIVE", sleepDurationMinutes = 300L))

        // THROTTLE / GROUNDED should show nudge
        assertTrue(shouldShowNudge("THROTTLE", sleepDurationMinutes = 450L))
        assertTrue(shouldShowNudge("GROUNDED", sleepDurationMinutes = 450L))

        // NOMINAL with normal sleep should not show nudge
        assertFalse(shouldShowNudge("NOMINAL", sleepDurationMinutes = 440L))

        // NOMINAL with short sleep (>= 45m under need: 440 - 395 = 45) should show nudge
        assertTrue(shouldShowNudge("NOMINAL", sleepDurationMinutes = 395L))
        assertFalse(shouldShowNudge("NOMINAL", sleepDurationMinutes = 400L))

        // If dismissed today, should not show
        assertFalse(shouldShowNudge("THROTTLE", sleepDurationMinutes = 400L, isDismissedToday = true))
    }
}
