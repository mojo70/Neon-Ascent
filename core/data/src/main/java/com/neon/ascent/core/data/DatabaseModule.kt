package com.neon.ascent.core.data

import android.content.Context
import androidx.room.Room
import com.neon.ascent.core.data.local.dao.SpecialDao
import com.neon.ascent.core.data.local.migration.MIGRATION_2_3
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
    fun provideDatabase(@ApplicationContext context: Context): NeonAscentDatabase {
        return Room.databaseBuilder(
            context,
            NeonAscentDatabase::class.java,
            "neon_ascent_database"
        )
        .addMigrations(MIGRATION_2_3)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideGoalDao(database: NeonAscentDatabase): NewGoalDao {
        return database.goalDao()
    }

    @Provides
    fun provideSpecialDao(database: NeonAscentDatabase): SpecialDao {
        return database.specialDao()
    }
}
