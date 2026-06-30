package com.neon.ascent.feature.health.di

import com.neon.ascent.core.domain.health.HealthManager
import com.neon.ascent.feature.health.data.HealthConnectManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HealthModule {

    @Binds
    @Singleton
    abstract fun bindHealthManager(
        healthConnectManager: HealthConnectManager
    ): HealthManager
}
