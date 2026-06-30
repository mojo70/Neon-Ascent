package com.neon.ascent.core.domain

/**
 * Interface for scheduling neural pings and notifications.
 * Implemented in :feature:notifications to avoid circular dependencies.
 */
interface NeuralPingScheduler {
    suspend fun scheduleSmartPings()
    fun enqueueDailyNeuralBrief(isTestRequest: Boolean = false)
    fun triggerExpeditedBrief(reason: String)
}
