package com.neon.ascent.model

import java.time.Instant

data class HealthTrend(
    val label: String,
    val currentValue: String,
    val dataPoints: List<Float>,
    val insight: String? = null,
    val unit: String = ""
)
