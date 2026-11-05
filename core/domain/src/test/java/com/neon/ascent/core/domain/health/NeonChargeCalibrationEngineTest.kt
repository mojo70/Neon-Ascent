package com.neon.ascent.core.domain.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class NeonChargeCalibrationEngineTest {

    @Test
    fun `fewer than 14 labeled days does not promote calibration`() {
        val now = Instant.now().toEpochMilli()
        val labels = (1..10).map { i ->
            CalibrationLabelRecord(
                dateString = "2026-10-$i",
                timestamp = now - (i * 86400000L),
                wakeSeed = 75,
                hoursAwake = 10.0,
                sessionDrainRaw = 15.0,
                stepDrainRaw = 10.0,
                hrLoadMin = 10.0,
                feltCharge = 40,
                labelWeight = 1.0,
                isQuarantined = false
            )
        }

        val result = NeonChargeCalibrationEngine.fitRidge(labels, now)
        assertTrue(!result.isPromoted)
        assertEquals(10, result.effectiveN)
    }

    @Test
    fun `14 identical hard days produce clamped multipliers without explosion`() {
        val now = Instant.now().toEpochMilli()
        val labels = (1..15).map { i ->
            CalibrationLabelRecord(
                dateString = "2026-10-$i",
                timestamp = now - (i * 86400000L),
                wakeSeed = 75,
                hoursAwake = 12.0,
                sessionDrainRaw = 30.0,
                stepDrainRaw = 20.0,
                hrLoadMin = 20.0,
                feltCharge = 20,
                labelWeight = 1.0,
                isQuarantined = false
            )
        }

        val result = NeonChargeCalibrationEngine.fitRidge(labels, now)
        assertTrue(result.kPassive in 0.03..0.20)
        assertTrue(result.sessionMult in 0.55..1.9)
        assertTrue(result.stepMult in 0.55..1.9)
        assertTrue(result.hrMult in 0.55..1.9)
    }

    @Test
    fun `outlier does not explode multipliers due to L2 prior and robust fit`() {
        val now = Instant.now().toEpochMilli()
        val labels = (1..15).map { i ->
            val isOutlier = (i == 1)
            CalibrationLabelRecord(
                dateString = "2026-10-$i",
                timestamp = now - (i * 86400000L),
                wakeSeed = 75,
                hoursAwake = 10.0,
                sessionDrainRaw = 15.0,
                stepDrainRaw = 10.0,
                hrLoadMin = 10.0,
                feltCharge = if (isOutlier) 95 else 40,
                labelWeight = 1.0,
                isQuarantined = false
            )
        }

        val result = NeonChargeCalibrationEngine.fitRidge(labels, now)
        assertTrue(result.sessionMult in 0.55..1.9)
        assertTrue(result.stepMult in 0.55..1.9)
    }
}
