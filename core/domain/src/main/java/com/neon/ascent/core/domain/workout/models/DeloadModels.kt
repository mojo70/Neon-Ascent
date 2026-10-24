package com.neon.ascent.core.domain.workout.models

import java.time.Duration
import java.time.Instant
import java.util.UUID

enum class DeloadSource {
    RECOVERY,
    TIME,
    MANUAL
}

enum class DeloadWindowStatus {
    ACTIVE,
    COMPLETED,
    DISMISSED
}

data class DeloadWindow(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val start: Instant = Instant.now(),
    val end: Instant = start.plus(Duration.ofDays(9)),
    val source: DeloadSource = DeloadSource.MANUAL,
    val status: DeloadWindowStatus = DeloadWindowStatus.ACTIVE,
    val snoozedUntil: Instant? = null
)

data class SoftDeloadOffer(
    val shouldOffer: Boolean,
    val source: DeloadSource? = null,
    val reason: String = ""
)
