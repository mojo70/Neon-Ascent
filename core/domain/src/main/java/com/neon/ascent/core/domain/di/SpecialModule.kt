package com.neon.ascent.core.domain.di

import com.neon.ascent.core.domain.SpecialRepository
import com.neon.ascent.core.domain.special.CognitiveTestEngine
import com.neon.ascent.core.domain.special.usecases.UpdateSpecialFromCognitiveTestUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SpecialModule {

    @Provides
    fun provideCognitiveTestEngine() = CognitiveTestEngine()

    @Provides
    @Singleton
    fun provideUpdateSpecialFromCognitiveTestUseCase(
        repository: SpecialRepository,
        engine: CognitiveTestEngine
    ) = UpdateSpecialFromCognitiveTestUseCase(repository, engine)
}
