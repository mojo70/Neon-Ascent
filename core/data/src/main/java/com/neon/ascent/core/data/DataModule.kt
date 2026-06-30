package com.neon.ascent.core.data

import com.neon.ascent.core.data.repository.AscensionRepositoryImpl
import com.neon.ascent.core.data.repository.DopamineMenuRepositoryImpl
import com.neon.ascent.core.data.repository.InsightProjectionRepositoryImpl
import com.neon.ascent.core.domain.GoalRepository
import com.neon.ascent.core.domain.SpecialRepository
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.core.domain.repository.DopamineMenuRepository
import com.neon.ascent.core.domain.repository.InsightProjectionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindGoalRepository(
        goalRepositoryImpl: GoalRepositoryImpl
    ): GoalRepository

    @Binds
    @Singleton
    abstract fun bindSpecialRepository(
        specialRepositoryImpl: SpecialRepositoryImpl
    ): SpecialRepository

    @Binds
    @Singleton
    abstract fun bindAscensionRepository(
        ascensionRepositoryImpl: AscensionRepositoryImpl
    ): AscensionRepository

    @Binds
    @Singleton
    abstract fun bindInsightProjectionRepository(
        insightProjectionRepositoryImpl: InsightProjectionRepositoryImpl
    ): InsightProjectionRepository

    @Binds
    @Singleton
    abstract fun bindDopamineMenuRepository(
        dopamineMenuRepositoryImpl: DopamineMenuRepositoryImpl
    ): DopamineMenuRepository
}
