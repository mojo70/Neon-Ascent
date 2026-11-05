package com.neon.ascent.core.domain.health

data class NeonChargeMlInput(
    val wakeSeed: Int?,
    val hoursAwake: Double?,
    val sessionDrainRaw: Double?,
    val stepDrainRaw: Double?,
    val hrLoadMin: Int?,
    val napMin: Int?,
    val priorDayResidual: Double?,
    val sleepDebt3d: Double?,
    val rpe7dAvg: Double?,
    val hrLoad7dAvg: Double?,
    val activeKcalToday: Double?,
    val isHardProtocol: Boolean?,
    val hourOfDay: Int?,
    val nextClearance: Int?
)

object NeonChargeMlFeaturePipe {
    fun extractFeatures(input: NeonChargeMlInput): Map<String, Double> {
        val features = mutableMapOf<String, Double>()

        // Core (omit if null, sparse)
        input.wakeSeed?.let { features["wakeSeed"] = it.toDouble() }
        input.hoursAwake?.let { features["hoursAwake"] = it }
        input.sessionDrainRaw?.let { features["sessionDrainRaw"] = it }
        input.stepDrainRaw?.let { features["stepDrainRaw"] = it }
        input.hrLoadMin?.let { features["hrLoadMin"] = it.toDouble() }
        input.napMin?.let { features["napMin"] = it.toDouble() }

        // Context
        input.priorDayResidual?.let { features["priorDayResidual"] = it }
        input.sleepDebt3d?.let { features["sleepDebt3d"] = it }
        input.rpe7dAvg?.let { features["rpe7dAvg"] = it }
        input.hrLoad7dAvg?.let { features["hrLoad7dAvg"] = it }
        input.activeKcalToday?.let { features["activeKcalToday"] = it }
        input.isHardProtocol?.let { features["isHardProtocol"] = if (it) 1.0 else 0.0 }
        input.hourOfDay?.let { features["hourOfDay"] = it.toDouble() }

        // Explicit interactions
        if (input.sessionDrainRaw != null && input.isHardProtocol != null) {
            features["sessionDrainRaw_x_isHardProtocol"] = input.sessionDrainRaw * (if (input.isHardProtocol) 1.0 else 0.0)
        }
        if (input.hrLoadMin != null && input.hoursAwake != null) {
            val lateDayMultiplier = if (input.hoursAwake > 8.0) 1.0 else 0.6
            features["hrLoadMin_x_lateDay"] = input.hrLoadMin * lateDayMultiplier
        }
        if (input.wakeSeed != null && input.hoursAwake != null) {
            features["wakeSeed_x_hoursAwake"] = input.wakeSeed * input.hoursAwake
        }

        return features
    }
}
