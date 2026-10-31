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

    @Provides
    @Singleton
    fun provideDirectivePreferencesDataStore(
        @ApplicationContext context: Context
    ): DirectivePreferencesDataStore = DirectivePreferencesDataStore(context)

    @Provides
    @Singleton
    fun provideWorkoutPreferencesDataStore(
        @ApplicationContext context: Context
    ): WorkoutPreferencesDataStore = WorkoutPreferencesDataStore(context)

    @Provides
    @Singleton
    fun provideDietPreferencesDataStore(
        @ApplicationContext context: Context
    ): DietPreferencesDataStore = DietPreferencesDataStore(context)

    @Provides
    @Singleton
    fun provideSupplementPreferencesDataStore(
        @ApplicationContext context: Context
    ): SupplementPreferencesDataStore = SupplementPreferencesDataStore(context)

    @Provides
    @Singleton
    fun provideLabsFocusPreferencesDataStore(
        @ApplicationContext context: Context
    ): LabsFocusPreferencesDataStore = LabsFocusPreferencesDataStore(context)
}
