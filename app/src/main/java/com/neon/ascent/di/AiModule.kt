package com.neon.ascent.di

import android.content.Context
import com.neon.ascent.core.ai.GemmaClient
import com.neon.ascent.core.domain.ai.AiCore
import com.neon.ascent.feature.biohacking.AiProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindAiCore(aiProvider: AiProvider): AiCore

    companion object {
        @Provides
        @Singleton
        fun provideGemmaClient(@ApplicationContext context: Context): GemmaClient {
            return GemmaClient(context)
        }
    }
}
