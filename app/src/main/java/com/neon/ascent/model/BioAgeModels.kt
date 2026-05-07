package com.neon.ascent.model

import kotlinx.serialization.Serializable

@Serializable
data class BioAgeResult(
    val biologicalAge: Float,
    val explanation: String,           // Natural language explanation
    val keyDrivers: List<Driver>       // Top contributors
)

@Serializable
data class Driver(
    val feature: String,
    val value: Float,
    val contribution: Float,           // SHAP value
    val impactText: String             // e.g. "+4.2 years (High glucose)"
)

@Serializable
data class ShapData(
    val global_importance: Map<String, Float>
)

@Serializable
data class ModelConfig(
    val features: List<String>,
    val medianValues: Map<String, Float>
)
