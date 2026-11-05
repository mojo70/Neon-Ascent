package com.neon.ascent.core.domain.health

data class MlSnapshot(
    val snapshotId: String,
    val coefficients: Map<String, Double>,
    val intercept: Double,
    val effectiveN: Int,
    val holdOutMae: Double,
    val timestamp: Long
)

data class MlTrainingResult(
    val isReady: Boolean,
    val snapshot: MlSnapshot?,
    val reason: String = ""
)

object NeonChargeElasticNetTrainer {

    fun trainAndEvaluate(
        labeledRecords: List<Pair<NeonChargeMlInput, Double>>, // input + feltCharge
        labelWeights: List<Double>,
        timestamps: List<Long>,
        nowTimestamp: Long = System.currentTimeMillis()
    ): MlTrainingResult {
        if (labeledRecords.size < 40) {
            return MlTrainingResult(
                isReady = false,
                snapshot = null,
                reason = "Insufficient labels for V2.2 ML model (n=${labeledRecords.size}, need >= 40)"
            )
        }

        // Feature extraction for all samples
        val extractedSamples = labeledRecords.mapIndexed { idx, (input, felt) ->
            val features = NeonChargeMlFeaturePipe.extractFeatures(input)
            val daysAgo = (nowTimestamp - timestamps[idx]) / (1000.0 * 3600 * 24)
            val recency = if (daysAgo <= 45.0) 1.0 else Math.pow(0.5, (daysAgo - 45.0) / 60.0)
            val weight = labelWeights[idx] * recency
            val target = input.wakeSeed?.let { it - felt } ?: 0.0
            Triple(features, target, weight)
        }.filter { it.third > 0.0 }

        if (extractedSamples.size < 30) {
            return MlTrainingResult(
                isReady = false,
                snapshot = null,
                reason = "Insufficient valid weighted samples after recency filter"
            )
        }

        // Time-series split: train on all but last 10 samples, evaluate hold-out MAE on last 10 samples
        val splitIdx = (extractedSamples.size - 10).coerceAtLeast(10)
        val trainSet = extractedSamples.subList(0, splitIdx)
        val holdOutSet = extractedSamples.subList(splitIdx, extractedSamples.size)

        // Simple coordinate descent / ridge-lasso approximation for demonstration
        val allKeys = trainSet.flatMap { it.first.keys }.toSet()
        val coefficients = mutableMapOf<String, Double>()
        allKeys.forEach { coefficients[it] = 0.01 }
        var intercept = 50.0

        val lr = 0.00001
        val delta = 12.0 // Huber loss delta

        for (epoch in 0..150) {
            var gradIntercept = 0.0
            val gradCoeffs = mutableMapOf<String, Double>()
            allKeys.forEach { gradCoeffs[it] = 0.0 }

            trainSet.forEach { (features, target, weight) ->
                var pred = intercept
                features.forEach { (k, v) ->
                    pred += (coefficients[k] ?: 0.0) * v
                }
                val err = pred - target

                // Huber loss gradient
                val huberGrad = if (Math.abs(err) <= delta) err else delta * Math.signum(err)
                gradIntercept += weight * huberGrad
                features.forEach { (k, v) ->
                    gradCoeffs[k] = (gradCoeffs[k] ?: 0.0) + weight * huberGrad * v
                }
            }

            intercept -= lr * gradIntercept
            allKeys.forEach { k ->
                val current = coefficients[k] ?: 0.0
                coefficients[k] = (current - lr * (gradCoeffs[k] ?: 0.0)).coerceIn(-5.0, 5.0)
            }
        }

        // Evaluate hold-out MAE with Huber weights
        var holdOutAbsErr = 0.0
        var holdOutWeightSum = 0.0
        holdOutSet.forEach { (features, target, weight) ->
            var pred = intercept
            features.forEach { (k, v) ->
                pred += (coefficients[k] ?: 0.0) * v
            }
            holdOutAbsErr += weight * Math.abs(pred - target)
            holdOutWeightSum += weight
        }

        val holdOutMae = if (holdOutWeightSum > 0.0) holdOutAbsErr / holdOutWeightSum else 0.0

        val snapshot = MlSnapshot(
            snapshotId = "ml_snap_${nowTimestamp}",
            coefficients = coefficients,
            intercept = intercept,
            effectiveN = extractedSamples.size,
            holdOutMae = holdOutMae,
            timestamp = nowTimestamp
        )

        return MlTrainingResult(
            isReady = true,
            snapshot = snapshot,
            reason = "Trained V2.2 Elastic Net (hold-out MAE: ${String.format(java.util.Locale.US, "%.2f", holdOutMae)})"
        )
    }
}
