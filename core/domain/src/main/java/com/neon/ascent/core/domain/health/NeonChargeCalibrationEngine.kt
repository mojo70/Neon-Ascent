package com.neon.ascent.core.domain.health

import java.util.Locale

data class CalibrationResult(
    val isPromoted: Boolean,
    val kPassive: Double,
    val sessionMult: Double,
    val stepMult: Double,
    val hrMult: Double,
    val effectiveN: Int,
    val fitDate: Long,
    val inSampleMae: Double,
    val looMae: Double,
    val reason: String = ""
)

data class CalibrationLabelRecord(
    val dateString: String,
    val timestamp: Long,
    val wakeSeed: Int,
    val hoursAwake: Double,
    val sessionDrainRaw: Double,
    val stepDrainRaw: Double,
    val hrLoadMin: Double,
    val feltCharge: Int,
    val labelWeight: Double,
    val isQuarantined: Boolean
)

object NeonChargeCalibrationEngine {

    fun fitRidge(labels: List<CalibrationLabelRecord>, nowTimestamp: Long = System.currentTimeMillis()): CalibrationResult {
        val validLabels = labels.filter { !it.isQuarantined && it.labelWeight > 0.0 }
        val totalWeight = validLabels.sumOf { it.labelWeight }
        if (totalWeight < 14.0 || validLabels.size < 14) {
            return CalibrationResult(
                isPromoted = false,
                kPassive = 0.085,
                sessionMult = 1.0,
                stepMult = 1.0,
                hrMult = 1.0,
                effectiveN = validLabels.size,
                fitDate = nowTimestamp,
                inSampleMae = 0.0,
                looMae = 0.0,
                reason = "Insufficient weighted labels (n=$totalWeight, need >= 14)"
            )
        }

        val priorK = 0.085
        val priorSession = 1.0
        val priorStep = 1.0
        val priorHr = 1.0

        var k = priorK
        var sMult = priorSession
        var stMult = priorStep
        var hrMult = priorHr

        val lambda = 2.0
        val lr = 0.00005

        for (epoch in 0..300) {
            var gradK = 2.0 * lambda * (k - priorK)
            var gradS = 2.0 * lambda * (sMult - priorSession)
            var gradSt = 2.0 * lambda * (stMult - priorStep)
            var gradHr = 2.0 * lambda * (hrMult - priorHr)

            validLabels.forEach { label ->
                val daysAgo = (nowTimestamp - label.timestamp) / (1000.0 * 3600 * 24)
                val recency = if (daysAgo <= 45.0) 1.0 else Math.pow(0.5, (daysAgo - 45.0) / 60.0)
                val w = label.labelWeight * recency

                val targetDrain = (label.wakeSeed - label.feltCharge).coerceAtLeast(0).toDouble()

                val passivePred = label.wakeSeed * (1.0 - Math.exp(-k * label.hoursAwake))
                val sessionPred = label.sessionDrainRaw * sMult
                val stepPred = label.stepDrainRaw * stMult
                val hrPred = label.hrLoadMin * hrMult
                val predDrain = passivePred + sessionPred + stepPred + hrPred

                val err = predDrain - targetDrain

                val dPassiveWrtK = label.wakeSeed * label.hoursAwake * Math.exp(-k * label.hoursAwake)
                gradK += 2.0 * w * err * dPassiveWrtK
                gradS += 2.0 * w * err * label.sessionDrainRaw
                gradSt += 2.0 * w * err * label.stepDrainRaw
                gradHr += 2.0 * w * err * label.hrLoadMin
            }

            k = (k - lr * gradK).coerceIn(0.03, 0.20)
            sMult = (sMult - lr * gradS).coerceIn(0.55, 1.9)
            stMult = (stMult - lr * gradSt).coerceIn(0.55, 1.9)
            hrMult = (hrMult - lr * gradHr).coerceIn(0.55, 1.9)
        }

        var ridgeAbsErrSum = 0.0
        var priorAbsErrSum = 0.0
        var weightSum = 0.0

        validLabels.forEach { label ->
            val daysAgo = (nowTimestamp - label.timestamp) / (1000.0 * 3600 * 24)
            val recency = if (daysAgo <= 45.0) 1.0 else Math.pow(0.5, (daysAgo - 45.0) / 60.0)
            val w = label.labelWeight * recency
            weightSum += w

            val targetDrain = (label.wakeSeed - label.feltCharge).coerceAtLeast(0).toDouble()

            val ridgePassive = label.wakeSeed * (1.0 - Math.exp(-k * label.hoursAwake))
            val ridgePred = ridgePassive + label.sessionDrainRaw * sMult + label.stepDrainRaw * stMult + label.hrLoadMin * hrMult
            ridgeAbsErrSum += w * Math.abs(ridgePred - targetDrain)

            val priorPassive = label.wakeSeed * (1.0 - Math.exp(-priorK * label.hoursAwake))
            val priorPred = priorPassive + label.sessionDrainRaw * priorSession + label.stepDrainRaw * priorStep + label.hrLoadMin * priorHr
            priorAbsErrSum += w * Math.abs(priorPred - targetDrain)
        }

        val inSampleMae = if (weightSum > 0.0) ridgeAbsErrSum / weightSum else 0.0
        val priorMae = if (weightSum > 0.0) priorAbsErrSum / weightSum else 0.0

        val isPromoted = inSampleMae <= priorMae + 1.0

        return CalibrationResult(
            isPromoted = isPromoted,
            kPassive = k,
            sessionMult = sMult,
            stepMult = stMult,
            hrMult = hrMult,
            effectiveN = validLabels.size,
            fitDate = nowTimestamp,
            inSampleMae = inSampleMae,
            looMae = priorMae,
            reason = if (isPromoted) "Promoted (MAE ${String.format(Locale.US, "%.2f", inSampleMae)} vs prior ${String.format(Locale.US, "%.2f", priorMae)})" else "Rejected: MAE did not beat prior"
        )
    }
}
