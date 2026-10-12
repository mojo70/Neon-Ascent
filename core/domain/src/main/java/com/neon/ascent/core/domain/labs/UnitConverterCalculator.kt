package com.neon.ascent.core.domain.labs

import java.math.BigDecimal
import java.math.RoundingMode

object UnitConverterCalculator {

    private const val CALC_SCALE = 10
    private val THOUSAND = BigDecimal("1000")
    val HGH_IU_PER_MG = BigDecimal("3.0")

    private fun BigDecimal.normalizeDecimal(): BigDecimal {
        val stripped = this.stripTrailingZeros()
        return if (stripped.scale() < 0) stripped.setScale(0) else stripped
    }

    enum class CompoundProfile(val displayName: String, val defaultIuPerMg: BigDecimal?, val note: String) {
        GENERAL("General (mcg ↔ mg)", null, "Universal mcg ↔ mg converter."),
        HGH_SOMATROPIN("HGH / Somatropin", BigDecimal("3.0"), "Assumes standard documented ratio (~3 IU per mg)."),
        HCG("HCG", null, "HCG is sold by IU. Supply custom ratio if mg conversion is required.")
    }

    /**
     * Converts mcg to mg.
     */
    fun mcgToMg(mcg: BigDecimal): BigDecimal {
        return mcg.divide(THOUSAND, CALC_SCALE, RoundingMode.HALF_UP).normalizeDecimal()
    }

    /**
     * Converts mg to mcg.
     */
    fun mgToMcg(mg: BigDecimal): BigDecimal {
        return mg.multiply(THOUSAND).normalizeDecimal()
    }

    /**
     * Converts mg to IU given an IU per mg ratio.
     * Returns null if ratio is non-positive.
     */
    fun mgToIu(mg: BigDecimal, iuPerMg: BigDecimal): BigDecimal? {
        if (mg <= BigDecimal.ZERO || iuPerMg <= BigDecimal.ZERO) return null
        return mg.multiply(iuPerMg).setScale(4, RoundingMode.HALF_UP).normalizeDecimal()
    }

    /**
     * Converts IU to mg given an IU per mg ratio.
     * Returns null if ratio is non-positive.
     */
    fun iuToMg(iu: BigDecimal, iuPerMg: BigDecimal): BigDecimal? {
        if (iu <= BigDecimal.ZERO || iuPerMg <= BigDecimal.ZERO) return null
        return iu.divide(iuPerMg, CALC_SCALE, RoundingMode.HALF_UP).setScale(4, RoundingMode.HALF_UP).normalizeDecimal()
    }
}
