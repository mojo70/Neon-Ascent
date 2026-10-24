package com.neon.ascent.core.domain.chronicle

typealias DayPayload = DailyOriginPayload

data class DailyOriginPayload(
    val localDate: String,
    val workoutSessionId: String? = null,
    val workoutSummary: String? = null,
    val riteIds: List<String> = emptyList(),
    val sanctumNightId: String? = null,
    val sanctumSummary: String? = null,
    val bodyLogIds: List<String> = emptyList(),
    val protocolIds: List<String> = emptyList(),
    val chronicleEntryId: String? = null,
    val chronicleContent: String? = null
)
