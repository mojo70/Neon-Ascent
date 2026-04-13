package com.neon.ascent.di

import android.content.Context
import com.neon.ascent.core.ai.GemmaClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideGemmaClient(@ApplicationContext context: Context): GemmaClient {
        return GemmaClient(context)
    }
}
