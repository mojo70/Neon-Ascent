package com.neon.ascent.core.domain.labs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.math.BigDecimal

class ReconstitutionCalculatorTest {

    @Test
    fun `test forward reconstitution calculation`() {
        // 10 mg + 2 mL + 0.5 mg → 5 mg/mL, 0.1 mL, 10 units, 20 doses
        val result = ReconstitutionCalculator.forward(
            vialMg = BigDecimal("10"),
            waterMl = BigDecimal("2"),
            doseMg = BigDecimal("0.5")
        )

        assertNotNull(result)
        requireNotNull(result)

        assertEquals(BigDecimal("5"), result.concentrationMgPerMl)
        assertEquals(BigDecimal("5000"), result.concentrationMcgPerMl)
        assertEquals(BigDecimal("0.1"), result.volumeMl)
        assertEquals(BigDecimal("10"), result.unitsU100)
        assertEquals(BigDecimal("20"), result.dosesPerVial)
        assertEquals(BigDecimal("50"), result.mcgPerUnit)
    }

    @Test
    fun `test reverse water calculation`() {
        // Reverse water: 10 mg, 0.5 mg, 20 units → 4 mL
        val waterMl = ReconstitutionCalculator.reverseWater(
            vialMg = BigDecimal("10"),
            doseMg = BigDecimal("0.5"),
            targetUnits = BigDecimal("20")
        )

        assertNotNull(waterMl)
        assertEquals(BigDecimal("4"), waterMl)
    }

    @Test
    fun `test reverse dose calculation`() {
        // Reverse dose: 5 mg/mL, 14 units → 0.7 mg
        val doseMg = ReconstitutionCalculator.reverseDose(
            concentrationMgPerMl = BigDecimal("5"),
            units = BigDecimal("14")
        )

        assertNotNull(doseMg)
        assertEquals(BigDecimal("0.7"), doseMg)
    }

    @Test
    fun `test mcg and mg conversion`() {
        // 250 mcg == 0.25 mg
        val mgVal = ReconstitutionCalculator.mcgToMg(BigDecimal("250"))
        assertEquals(BigDecimal("0.25"), mgVal)

        val mcgVal = ReconstitutionCalculator.mgToMcg(BigDecimal("0.25"))
        assertEquals(BigDecimal("250"), mcgVal)
    }

    @Test
    fun `test syringe barrel overflow`() {
        // 40 units on a 0.3 mL (30 u) barrel raises capacity warning
        assertTrue(ReconstitutionCalculator.SyringeBarrel.BARREL_0_3.isOverflow(BigDecimal("40")))
        assertFalse(ReconstitutionCalculator.SyringeBarrel.BARREL_0_5.isOverflow(BigDecimal("40")))
        assertFalse(ReconstitutionCalculator.SyringeBarrel.BARREL_1_0.isOverflow(BigDecimal("40")))
    }

    @Test
    fun `test invalid and non-positive inputs return null`() {
        assertNull(ReconstitutionCalculator.forward(BigDecimal("0"), BigDecimal("2"), BigDecimal("0.5")))
        assertNull(ReconstitutionCalculator.forward(BigDecimal("10"), BigDecimal("-1"), BigDecimal("0.5")))
        assertNull(ReconstitutionCalculator.reverseWater(BigDecimal("10"), BigDecimal("0"), BigDecimal("20")))
        assertNull(ReconstitutionCalculator.reverseDose(BigDecimal("-5"), BigDecimal("14")))
    }
}
