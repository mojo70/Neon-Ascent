package com.neon.ascent.domain.onboarding

import com.neon.ascent.core.domain.goals.usecases.GenerateMissionsFromAspirationsUseCase
import com.neon.ascent.core.domain.goals.usecases.SeedStarterHabitsUseCase
import com.neon.ascent.feature.notifications.data.SmartPingScheduler
import javax.inject.Inject

class OnboardingCompletionUseCase @Inject constructor(
    private val seedStarterHabitsUseCase: SeedStarterHabitsUseCase,
    private val generateMissionsFromAspirationsUseCase: GenerateMissionsFromAspirationsUseCase,
    private val smartPingScheduler: SmartPingScheduler
) {

    suspend operator fun invoke(primaryArchetype: String) {
        // 1. Seed starter habits based on archetype
        seedStarterHabitsUseCase(primaryArchetype)

        // 2. Generate missions from aspirations
        generateMissionsFromAspirationsUseCase()

        // 3. Schedule smart contextual Neural Pings
        smartPingScheduler.scheduleSmartPings()
    }
}
