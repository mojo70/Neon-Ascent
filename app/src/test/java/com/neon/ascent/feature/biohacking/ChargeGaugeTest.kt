package com.neon.ascent.feature.biohacking

import com.neon.ascent.core.domain.health.ChargeConfidence
import com.neon.ascent.core.domain.health.NeonCharge
import com.neon.ascent.feature.biohacking.ui.labs.chargeGaugeOrEmpty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargeGaugeTest {

    @Test
    fun nullChargeIsEmpty() {
        val gauge = chargeGaugeOrEmpty(null)
        assertFalse(gauge.hasCharge)
        assertNull(gauge.percent)
        assertNull(gauge.confidence)
        assertEquals("Charge not in yet", gauge.label)
    }

    @Test
    fun lowConfidenceColdStartStaysAt62() {
        val charge = NeonCharge(
            value = 62,
            confidence = ChargeConfidence.LOW,
            drivers = listOf("COLD_START" to "Default baseline (62%) due to missing sleep & vitals"),
            wakeSeed = 62
        )
        val gauge = chargeGaugeOrEmpty(charge)
        assertTrue(gauge.hasCharge)
        assertEquals(62, gauge.percent)
        assertEquals(ChargeConfidence.LOW, gauge.confidence)
        assertEquals("62%", gauge.label)
    }
}
