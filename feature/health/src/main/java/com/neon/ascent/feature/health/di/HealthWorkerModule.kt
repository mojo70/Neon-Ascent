package com.neon.ascent.feature.health.di

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.WorkerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object HealthWorkerModule {

    @Provides
    fun provideWorkerFactory(
        workerFactory: HiltWorkerFactory
    ): WorkerFactory = workerFactory
}
