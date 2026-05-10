package com.neon.ascent.core.domain.di

import com.neon.ascent.core.domain.GoalRepository
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
}
