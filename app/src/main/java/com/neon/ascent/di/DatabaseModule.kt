package com.neon.ascent.di

import android.content.Context
import androidx.room.Room
import com.neon.ascent.data.local.AppDatabase
import com.neon.ascent.data.local.UserCharacterDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "neon_ascent_db"
        ).build()
    }

    @Provides
    fun provideUserCharacterDao(database: AppDatabase): UserCharacterDao {
        return database.userCharacterDao()
    }
}
