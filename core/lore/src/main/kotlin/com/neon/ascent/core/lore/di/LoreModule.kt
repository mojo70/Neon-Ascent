package com.neon.ascent.core.lore.di

import com.neon.ascent.core.lore.data.LoreRepository
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LoreModule {

    @Provides
    @Singleton
    fun provideLoreRepository(@ApplicationContext context: Context): LoreRepository {
        return LoreRepository(context)
    }
}
