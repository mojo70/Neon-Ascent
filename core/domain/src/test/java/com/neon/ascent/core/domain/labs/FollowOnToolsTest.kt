package com.neon.ascent.core.domain.labs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class FollowOnToolsTest {

    @Test
    fun `test half life decay calculation`() {
        // Daily dose 1 mg, 24h half-life, 24h interval
        val result = HalfLifeCalculator.calculateDecay(
            halfLifeHours = 24.0,
            doseIntervalHours = 24.0,
            doseAmountMg = 1.0,
            numDoses = 10
        )

        assertNotNull(result)
        requireNotNull(result)

        // Steady state peak for 1mg daily @ 24h half-life is ~2.0 mg
        // Steady state trough is ~1.0 mg
        val peak = result.peakSteadyStateMg.toDouble()
        val trough = result.troughSteadyStateMg.toDouble()

        assertTrue("Peak should be around 2.0", peak in 1.9..2.1)
        assertTrue("Trough should be around 1.0", trough in 0.9..1.1)
        assertEquals(BigDecimal("108"), result.timeToSteadyStateHours) // 4.5 * 24
        assertTrue(result.curvePoints.isNotEmpty())
    }

    @Test
    fun `test unit converter mcg and mg`() {
        val mg = UnitConverterCalculator.mcgToMg(BigDecimal("250"))
        assertEquals(BigDecimal("0.25"), mg)

        val mcg = UnitConverterCalculator.mgToMcg(BigDecimal("0.25"))
        assertEquals(BigDecimal("250"), mcg)
    }

    @Test
    fun `test unit converter HGH somatropin 3 IU per mg`() {
        val iu = UnitConverterCalculator.mgToIu(BigDecimal("1"), UnitConverterCalculator.HGH_IU_PER_MG)
        assertEquals(BigDecimal("3"), iu)

        val mg = UnitConverterCalculator.iuToMg(BigDecimal("3"), UnitConverterCalculator.HGH_IU_PER_MG)
        assertEquals(BigDecimal("1"), mg)
    }

    @Test
    fun `test HCG returns null without custom ratio`() {
        // HCG without custom ratio returns null
        val iu = UnitConverterCalculator.mgToIu(BigDecimal("1"), BigDecimal("0"))
        assertNull(iu)
    }
}
