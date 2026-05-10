package com.neon.ascent.core.domain.di

import com.neon.ascent.core.domain.GoalRepository
import com.neon.ascent.core.domain.SpecialRepository
import com.neon.ascent.core.domain.effects.LevelUpEffectService
import com.neon.ascent.core.domain.goals.usecases.CompleteHabitAndUpdateGoalsUseCase
import com.neon.ascent.core.domain.goals.usecases.SeedStarterHabitsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object GoalsModule {

    @Provides
    fun provideSeedStarterHabitsUseCase(
        goalRepository: GoalRepository
    ): SeedStarterHabitsUseCase = SeedStarterHabitsUseCase(goalRepository)

    @Provides
    fun provideCompleteHabitAndUpdateGoalsUseCase(
        goalRepository: GoalRepository,
        specialRepository: SpecialRepository,
        levelUpEffectService: LevelUpEffectService
    ): CompleteHabitAndUpdateGoalsUseCase {
        return CompleteHabitAndUpdateGoalsUseCase(
            goalRepository = goalRepository,
            specialRepository = specialRepository,
            levelUpEffectService = levelUpEffectService
        )
    }
}
