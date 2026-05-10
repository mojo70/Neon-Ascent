package com.neon.ascent.core.common

import com.neon.ascent.core.domain.effects.LevelUpEffectService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CommonModule {

    @Binds
    @Singleton
    abstract fun bindLevelUpEffectService(
        service: AndroidLevelUpEffectService
    ): LevelUpEffectService
}
