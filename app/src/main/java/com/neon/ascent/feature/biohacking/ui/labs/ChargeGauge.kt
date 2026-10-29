package com.neon.ascent.feature.biohacking.ui.labs

import com.neon.ascent.core.domain.health.ChargeConfidence
import com.neon.ascent.core.domain.health.NeonCharge

data class ChargeGauge(
    val hasCharge: Boolean,
    val percent: Int?,
    val confidence: ChargeConfidence?,
    val label: String
)

/**
 * A real charge, including a low-confidence cold start of 62, keeps its number.
 * A missing [NeonCharge] is empty and must not be drawn as 62.
 */
fun chargeGaugeOrEmpty(charge: NeonCharge?): ChargeGauge {
    if (charge == null) {
        return ChargeGauge(
            hasCharge = false,
            percent = null,
            confidence = null,
            label = "Charge not in yet"
        )
    }
    return ChargeGauge(
        hasCharge = true,
        percent = charge.value,
        confidence = charge.confidence,
        label = "${charge.value}%"
    )
}
