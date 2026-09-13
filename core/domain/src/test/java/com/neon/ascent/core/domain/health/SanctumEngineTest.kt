package com.neon.ascent.core.domain.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SanctumEngineTest {

    @Test
    fun `no session returns null score`() {
        val input = SanctumInput(
            sessionStart = null,
            sessionEnd = null
        )
        val result = SanctumEngine.calculateSanctum(input)
        assertNull(result.score)
        assertEquals(0, result.tier)
        assertNull(result.band)
        assertTrue(result.drivers.isEmpty())
    }

    @Test
    fun `fixture 1 - 7 and half hour session no stages no HR returns T0`() {
        val input = SanctumInput(
            sessionStart = Instant.parse("2026-09-06T00:00:00Z"),
            sessionEnd = Instant.parse("2026-09-06T07:30:00Z"),
            stageMinutes = emptyMap(),
            userSleepNeedMin = 450L
        )
        val result = SanctumEngine.calculateSanctum(input)

        assertNotNull(result.score)
        assertEquals(98, result.score) // 100 raw clamped to 98
        assertEquals(0, result.tier)
        assertEquals("SEALED", result.band)
        assertEquals(450L, result.asleepMin)
        assertEquals(450L, result.needMin)
    }

    @Test
    fun `fixture 2 - same duration LIGHT DEEP REM covering 90 percent and 4 awake bouts returns T1 score lower than fixture 1`() {
        val input = SanctumInput(
            sessionStart = Instant.parse("2026-09-06T00:00:00Z"),
            sessionEnd = Instant.parse("2026-09-06T07:30:00Z"),
            stageMinutes = mapOf(
                "LIGHT" to 200L,
                "DEEP" to 100L,
                "REM" to 105L,
                "AWAKE" to 45L
            ),
            awakeBoutsCount = 4,
            userSleepNeedMin = 450L
        )
        val result = SanctumEngine.calculateSanctum(input)

        assertNotNull(result.score)
        assertEquals(1, result.tier)
        // Score should be lower than T0 fixture 1 due to 405/450 duration (0.90) & fragmentation penalty (0.76)
        assertTrue(result.score!! < 98)
        assertTrue(result.drivers.any { it.first == "EFF" })
        assertTrue(result.drivers.any { it.first == "BOUTS" })
    }

    @Test
    fun `lone SLEEPING block is T0 not T1`() {
        val input = SanctumInput(
            sessionStart = Instant.parse("2026-09-06T00:00:00Z"),
            sessionEnd = Instant.parse("2026-09-06T07:30:00Z"),
            stageMinutes = mapOf("SLEEPING" to 450L),
            userSleepNeedMin = 450L
        )
        val result = SanctumEngine.calculateSanctum(input)

        assertEquals(0, result.tier)
        assertTrue(result.drivers.none { it.first == "EFF" })
    }

    @Test
    fun `overnight HR and HRV coverage elevates to T2`() {
        val start = Instant.parse("2026-09-06T00:00:00Z")
        val end = Instant.parse("2026-09-06T07:30:00Z")

        val hrSamples = (0..25).map { i ->
            start.plusSeconds(i * 900L) to 52
        }
        val preHrSamples = (0..10).map { i ->
            start.minusSeconds(i * 300L) to 60
        }
        val rmssdSamples = listOf(
            start.plusSeconds(3600) to 65.0,
            start.plusSeconds(7200) to 70.0,
            start.plusSeconds(10800) to 68.0
        )

        val input = SanctumInput(
            sessionStart = start,
            sessionEnd = end,
            stageMinutes = mapOf("LIGHT" to 220L, "DEEP" to 110L, "REM" to 120L),
            hrInSession = hrSamples,
            hrPreSleep90 = preHrSamples,
            rmssdInSession = rmssdSamples,
            last7HrvNight = listOf(60.0, 62.0, 58.0, 61.0, 59.0),
            userSleepNeedMin = 450L
        )

        val result = SanctumEngine.calculateSanctum(input)

        assertEquals(2, result.tier)
        assertTrue(result.drivers.any { it.first == "HRV_N" })
        assertTrue(result.drivers.any { it.first == "HR_DROP" })
    }

    @Test
    fun `fixture 3 - short sleep T0 cap 327m asleep vs 440m need returns SANCTUM at most 69 LEAK`() {
        val input = SanctumInput(
            sessionStart = Instant.parse("2026-09-06T00:00:00Z"),
            sessionEnd = Instant.parse("2026-09-06T05:27:00Z"),
            stageMinutes = emptyMap(),
            userSleepNeedMin = 440L
        )
        val result = SanctumEngine.calculateSanctum(input)

        assertNotNull(result.score)
        assertEquals(0, result.tier)
        assertTrue("Sanctum score must be <= 69 for short sleep T0", result.score!! <= 69)
        assertEquals(69, result.score)
        assertEquals("LEAK", result.band)
        assertEquals(327L, result.asleepMin)
        assertEquals(440L, result.needMin)
    }

    @Test
    fun `two RMSSD samples without HR coverage does not elevate to T2`() {
        val start = Instant.parse("2026-09-06T00:00:00Z")
        val end = Instant.parse("2026-09-06T07:30:00Z")

        val rmssdSamples = listOf(
            start.plusSeconds(3600) to 65.0,
            start.plusSeconds(7200) to 70.0
        )

        val input = SanctumInput(
            sessionStart = start,
            sessionEnd = end,
            stageMinutes = mapOf("LIGHT" to 220L, "DEEP" to 110L, "REM" to 120L),
            hrInSession = emptyList(),
            rmssdInSession = rmssdSamples,
            userSleepNeedMin = 450L
        )

        val result = SanctumEngine.calculateSanctum(input)

        assertNotEquals(2, result.tier)
        assertEquals(1, result.tier)
    }
}
