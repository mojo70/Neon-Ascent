package com.neon.ascent.core.domain.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class NeonChargeEngineTest {

    @Test
    fun `cold start returns 62 LOW confidence`() {
        val now = Instant.now()
        val input = NeonChargeInput(
            sleepMinutesLastNight = null,
            sanctumScore = null,
            sleepEndedAt = null,
            rhrToday = null,
            rhr7d = emptyList(),
            hrvToday = null,
            hrv7d = emptyList(),
            stepsToday = 0,
            now = now
        )

        val charge = NeonChargeEngine.calculateCharge(input)

        assertEquals(62, charge.wakeSeed)
        assertEquals(ChargeConfidence.LOW, charge.confidence)
        assertTrue(charge.drivers.any { it.first == "COLD_START" })
    }

    @Test
    fun `uses SANCTUM score when present and formats driver as SANCTUM`() {
        val now = Instant.now()
        val input = NeonChargeInput(
            sleepMinutesLastNight = 480L,
            sanctumScore = 80,
            sleepEndedAt = now.minusSeconds(3600),
            rhrToday = null,
            rhr7d = emptyList(),
            hrvToday = null,
            hrv7d = emptyList(),
            stepsToday = 0,
            now = now
        )

        val charge = NeonChargeEngine.calculateCharge(input)

        assertEquals(72, charge.wakeSeed)
        assertTrue(charge.drivers.any { it.first == "SLEEP" && it.second.contains("SANCTUM 80") })
    }

    @Test
    fun `falls back to sleep minutes when sanctumScore is null`() {
        val now = Instant.now()
        val input = NeonChargeInput(
            sleepMinutesLastNight = 450L,
            sanctumScore = null,
            sleepEndedAt = now.minusSeconds(3600),
            rhrToday = null,
            rhr7d = emptyList(),
            hrvToday = null,
            hrv7d = emptyList(),
            stepsToday = 0,
            now = now
        )

        val charge = NeonChargeEngine.calculateCharge(input)

        assertEquals(72, charge.wakeSeed)
        assertTrue(charge.drivers.any { it.first == "SLEEP" && it.second.contains("SLEEP 7h") })
    }

    @Test
    fun `z-score ignored when series size less than 5`() {
        val now = Instant.now()
        val input = NeonChargeInput(
            sleepMinutesLastNight = 480L,
            sleepEndedAt = now.minusSeconds(3600),
            rhrToday = 55.0,
            rhr7d = listOf(55.0, 56.0, 54.0, 55.0), // size 4 < 5
            hrvToday = 60.0,
            hrv7d = listOf(60.0, 62.0, 58.0), // size 3 < 5
            stepsToday = 1000,
            now = now
        )

        val charge = NeonChargeEngine.calculateCharge(input)

        assertEquals(ChargeConfidence.MED, charge.confidence)
        // Ensure z-score driver tags are not added since series < 5
        assertTrue(charge.drivers.none { it.first == "HRV_STRESS" })
        assertTrue(charge.drivers.none { it.first == "RHR_STRESS" })
    }

    @Test
    fun `z-score included when series size at least 5`() {
        val now = Instant.now()
        val input = NeonChargeInput(
            sleepMinutesLastNight = 480L,
            sleepEndedAt = now.minusSeconds(3600),
            rhrToday = 50.0,
            rhr7d = listOf(58.0, 60.0, 59.0, 61.0, 60.0), // size 5
            hrvToday = 75.0,
            hrv7d = listOf(50.0, 52.0, 48.0, 51.0, 49.0), // size 5
            stepsToday = 2000,
            now = now
        )

        val charge = NeonChargeEngine.calculateCharge(input)

        assertEquals(ChargeConfidence.HIGH, charge.confidence)
        assertTrue(charge.drivers.any { it.first == "HRV_STRESS" })
        assertTrue(charge.drivers.any { it.first == "RHR_STRESS" })
    }

    @Test
    fun `hr load calculated and formatted when coverage and baseline pass`() {
        val now = Instant.now()
        val start = now.minusSeconds(7200) // 2 hour span
        val hrSamples = (0..24).map { i ->
            start.plusSeconds(i * 300L) to 110 // HR 110 > 55+25=80 & >= 100
        }

        val input = NeonChargeInput(
            sleepMinutesLastNight = 450L,
            sanctumScore = 80,
            sleepEndedAt = now.minusSeconds(3600),
            rhrToday = 55.0,
            rhr7d = emptyList(),
            hrvToday = null,
            hrv7d = emptyList(),
            stepsToday = 0,
            hrSamplesToday = hrSamples,
            exerciseWindowsToday = emptyList(),
            sitWindowsToday = emptyList(),
            now = now
        )

        val charge = NeonChargeEngine.calculateCharge(input)

        assertTrue(charge.drivers.any { it.first == "HR_LOAD" && it.second.endsWith("m") })
    }

    @Test
    fun `hr load omitted when thin series`() {
        val now = Instant.now()
        val start = now.minusSeconds(1800) // only 30 min span < 2h
        val hrSamples = (0..5).map { i ->
            start.plusSeconds(i * 300L) to 110
        }

        val input = NeonChargeInput(
            sleepMinutesLastNight = 450L,
            sanctumScore = 80,
            sleepEndedAt = now.minusSeconds(3600),
            rhrToday = 55.0,
            rhr7d = emptyList(),
            hrvToday = null,
            hrv7d = emptyList(),
            stepsToday = 0,
            hrSamplesToday = hrSamples,
            now = now
        )

        val charge = NeonChargeEngine.calculateCharge(input)

        assertTrue(charge.drivers.none { it.first == "HR_LOAD" })
    }
}
