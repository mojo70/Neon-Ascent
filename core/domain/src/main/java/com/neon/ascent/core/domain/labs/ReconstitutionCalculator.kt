package com.neon.ascent.core.domain.labs

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Deterministic math engine for peptide reconstitution, dose, and water volume calculations.
 * Uses decimal-safe arithmetic (BigDecimal) to prevent floating-point inaccuracies.
 */
object ReconstitutionCalculator {

    private const val CALC_SCALE = 10
    private val HUNDRED = BigDecimal("100")
    private val THOUSAND = BigDecimal("1000")

    private fun BigDecimal.normalizeDecimal(): BigDecimal {
        val stripped = this.stripTrailingZeros()
        return if (stripped.scale() < 0) stripped.setScale(0) else stripped
    }

    data class ReconstitutionResult(
        val concentrationMgPerMl: BigDecimal,
        val concentrationMcgPerMl: BigDecimal,
        val volumeMl: BigDecimal,
        val unitsU100: BigDecimal,
        val dosesPerVial: BigDecimal,
        val mcgPerUnit: BigDecimal,
        val doseMg: BigDecimal,
        val doseMcg: BigDecimal
    )

    enum class SyringeBarrel(
        val capacityMl: BigDecimal,
        val capacityUnits: BigDecimal,
        val label: String
    ) {
        BARREL_0_3(BigDecimal("0.3"), BigDecimal("30"), "0.3 mL (30 u)"),
        BARREL_0_5(BigDecimal("0.5"), BigDecimal("50"), "0.5 mL (50 u)"),
        BARREL_1_0(BigDecimal("1.0"), BigDecimal("100"), "1.0 mL (100 u)");

        fun isOverflow(units: BigDecimal): Boolean {
            return units > capacityUnits
        }
    }

    /**
     * Safely parses a string into a positive BigDecimal, or null if invalid/non-positive.
     */
    fun parseBigDecimal(value: String): BigDecimal? {
        val clean = value.trim()
        if (clean.isEmpty()) return null
        return try {
            val bd = BigDecimal(clean)
            if (bd > BigDecimal.ZERO) bd.normalizeDecimal() else null
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * Converts mcg to mg (mcg / 1000).
     */
    fun mcgToMg(mcg: BigDecimal): BigDecimal {
        return mcg.divide(THOUSAND, CALC_SCALE, RoundingMode.HALF_UP).normalizeDecimal()
    }

    /**
     * Converts mg to mcg (mg * 1000).
     */
    fun mgToMcg(mg: BigDecimal): BigDecimal {
        return mg.multiply(THOUSAND).normalizeDecimal()
    }

    /**
     * Forward calculation: Given vial mg, water mL, and target dose mg,
     * returns concentration, volume (mL), U-100 units, doses remaining, and mcg per unit.
     * Returns null if any input is non-positive or invalid.
     */
    fun forward(
        vialMg: BigDecimal,
        waterMl: BigDecimal,
        doseMg: BigDecimal
    ): ReconstitutionResult? {
        if (vialMg <= BigDecimal.ZERO || waterMl <= BigDecimal.ZERO || doseMg <= BigDecimal.ZERO) {
            return null
        }

        val concentrationMgPerMl = vialMg.divide(waterMl, CALC_SCALE, RoundingMode.HALF_UP)
        val concentrationMcgPerMl = concentrationMgPerMl.multiply(THOUSAND)
        val volumeMl = doseMg.divide(concentrationMgPerMl, CALC_SCALE, RoundingMode.HALF_UP)
        val unitsU100 = volumeMl.multiply(HUNDRED)
        val dosesPerVial = vialMg.divide(doseMg, CALC_SCALE, RoundingMode.HALF_UP)
        val mcgPerUnit = concentrationMcgPerMl.divide(HUNDRED, CALC_SCALE, RoundingMode.HALF_UP)
        val doseMcg = mgToMcg(doseMg)

        return ReconstitutionResult(
            concentrationMgPerMl = concentrationMgPerMl.normalizeDecimal(),
            concentrationMcgPerMl = concentrationMcgPerMl.normalizeDecimal(),
            volumeMl = volumeMl.normalizeDecimal(),
            unitsU100 = unitsU100.normalizeDecimal(),
            dosesPerVial = dosesPerVial.normalizeDecimal(),
            mcgPerUnit = mcgPerUnit.normalizeDecimal(),
            doseMg = doseMg.normalizeDecimal(),
            doseMcg = doseMcg.normalizeDecimal()
        )
    }

    /**
     * Reverse calculation (Water needed): Given vial mg, dose mg, and target U-100 units,
     * returns the required water volume in mL.
     * Formula: water (mL) = (vial mg * target units) / (dose mg * 100)
     * Returns null if any input is non-positive.
     */
    fun reverseWater(
        vialMg: BigDecimal,
        doseMg: BigDecimal,
        targetUnits: BigDecimal
    ): BigDecimal? {
        if (vialMg <= BigDecimal.ZERO || doseMg <= BigDecimal.ZERO || targetUnits <= BigDecimal.ZERO) {
            return null
        }

        val numerator = vialMg.multiply(targetUnits)
        val denominator = doseMg.multiply(HUNDRED)

        return numerator.divide(denominator, CALC_SCALE, RoundingMode.HALF_UP).normalizeDecimal()
    }

    /**
     * Reverse calculation (Dose delivered): Given concentration (mg/mL) and units drawn,
     * returns the dose delivered in mg.
     * Formula: dose (mg) = concentration (mg/mL) * (units / 100)
     * Returns null if any input is non-positive.
     */
    fun reverseDose(
        concentrationMgPerMl: BigDecimal,
        units: BigDecimal
    ): BigDecimal? {
        if (concentrationMgPerMl <= BigDecimal.ZERO || units <= BigDecimal.ZERO) {
            return null
        }

        val volumeMl = units.divide(HUNDRED, CALC_SCALE, RoundingMode.HALF_UP)
        return concentrationMgPerMl.multiply(volumeMl).normalizeDecimal()
    }
}
