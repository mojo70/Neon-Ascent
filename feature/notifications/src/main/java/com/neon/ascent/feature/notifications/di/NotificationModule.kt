package com.neon.ascent.feature.notifications.di

import com.neon.ascent.core.domain.NeuralPingScheduler
import com.neon.ascent.core.domain.notifications.BriefService
import com.neon.ascent.feature.notifications.data.NeuralBriefManager
import com.neon.ascent.feature.notifications.data.SmartPingScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindNeuralPingScheduler(
        scheduler: SmartPingScheduler
    ): NeuralPingScheduler

    @Binds
    @Singleton
    abstract fun bindBriefService(
        manager: NeuralBriefManager
    ): BriefService
}
