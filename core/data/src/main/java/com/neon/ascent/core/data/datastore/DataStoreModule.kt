package com.neon.ascent.core.data.datastore

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideHealthPreferencesDataStore(
        @ApplicationContext context: Context
    ): HealthPreferencesDataStore = HealthPreferencesDataStore(context)
}
