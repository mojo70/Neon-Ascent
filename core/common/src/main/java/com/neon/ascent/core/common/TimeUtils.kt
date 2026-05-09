package com.neon.ascent.core.common

import java.time.Duration
import java.time.Instant

fun Instant.toRelativeTime(): String {
    val now = Instant.now()
    val duration = Duration.between(this, now)
    
    return when {
        duration.toMinutes() < 1 -> "Just now"
        duration.toMinutes() < 60 -> "${duration.toMinutes()}m ago"
        duration.toHours() < 24 -> "${duration.toHours()}h ago"
        else -> "${duration.toDays()}d ago"
    }
}
