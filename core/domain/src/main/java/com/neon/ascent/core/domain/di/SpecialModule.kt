package com.neon.ascent.core.domain.di

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import com.neon.ascent.core.domain.SpecialRepository
import com.neon.ascent.core.domain.special.CognitiveTestEngine
import com.neon.ascent.core.domain.special.HealthDataProcessor
import com.neon.ascent.core.domain.special.usecases.UpdateSpecialFromCognitiveTestUseCase
import com.neon.ascent.core.domain.special.usecases.UpdateSpecialFromHealthUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Provides
    @Singleton
    fun provideHealthConnectClient(@ApplicationContext context: Context): HealthConnectClient {
        return HealthConnectClient.getOrCreate(context)
    }

    @Provides
    @Singleton
    fun provideHealthDataProcessor() = HealthDataProcessor()

    @Provides
    @Singleton
    fun provideUpdateSpecialFromHealthUseCase(
        healthConnectClient: HealthConnectClient,
        repository: SpecialRepository,
        processor: HealthDataProcessor
    ) = UpdateSpecialFromHealthUseCase(healthConnectClient, repository, processor)
}
