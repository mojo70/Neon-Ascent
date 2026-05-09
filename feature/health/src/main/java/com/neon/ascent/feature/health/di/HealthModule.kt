package com.neon.ascent.feature.health.di

import android.content.Context
import com.neon.ascent.core.domain.special.HealthDataProcessor
import com.neon.ascent.feature.health.data.HealthConnectManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HealthModule {

    @Provides
    @Singleton
    fun provideHealthConnectManager(
        @ApplicationContext context: Context,
        processor: HealthDataProcessor
    ): HealthConnectManager = HealthConnectManager(context, processor)
}
