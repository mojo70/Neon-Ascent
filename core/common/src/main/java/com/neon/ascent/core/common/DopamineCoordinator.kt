package com.neon.ascent.core.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DopamineCoordinator @Inject constructor(
    private val hapticService: HapticService
) {
    private val _events = MutableSharedFlow<DopamineEvent>()
    val events = _events.asSharedFlow()

    suspend fun trigger(event: DopamineEvent) {
        when (event.level) {
            CelebrationLevel.SUBTLE -> hapticService.heartbeat()
            CelebrationLevel.SYNC -> hapticService.syncSuccess()
            CelebrationLevel.ASCENSION -> hapticService.ascensionBurst()
            CelebrationLevel.STREAK_RECOVERY -> hapticService.syncSuccess()
            CelebrationLevel.MISSION_COMPLETE -> hapticService.ascensionBurst()
            CelebrationLevel.DIRECTIVE_MILESTONE -> hapticService.ascensionBurst()
        }
        _events.emit(event)
    }

    suspend fun triggerSubtle(xp: Int = 10) {
        trigger(DopamineEvent(CelebrationLevel.SUBTLE, xpGained = xp))
    }

    suspend fun triggerSync(xp: Int = 25) {
        trigger(DopamineEvent(CelebrationLevel.SYNC, xpGained = xp))
    }

    suspend fun triggerAscension(title: String, xp: Int = 100) {
        trigger(DopamineEvent(CelebrationLevel.ASCENSION, message = title, xpGained = xp))
    }

    suspend fun triggerStreakRecovery(streak: Int, xp: Int = 15) {
        trigger(DopamineEvent(CelebrationLevel.STREAK_RECOVERY, streakCount = streak, isGraceRecovery = true, xpGained = xp))
    }

    suspend fun triggerMissionComplete(missionTitle: String, xp: Int = 50) {
        trigger(DopamineEvent(CelebrationLevel.MISSION_COMPLETE, message = missionTitle, xpGained = xp))
    }

    suspend fun triggerDirectiveMilestone(directiveTitle: String, progress: Float, xp: Int = 150) {
        trigger(DopamineEvent(CelebrationLevel.DIRECTIVE_MILESTONE, message = directiveTitle, directiveProgress = progress, xpGained = xp))
    }
}
