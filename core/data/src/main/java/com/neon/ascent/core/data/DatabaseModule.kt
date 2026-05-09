package com.neon.ascent.core.data

import android.content.Context
import androidx.room.Room
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
    fun provideGoalDatabase(@ApplicationContext context: Context): GoalDatabase {
        return Room.databaseBuilder(
            context,
            GoalDatabase::class.java,
            "goal_database"
        ).build()
    }

    @Provides
    fun provideGoalDao(database: GoalDatabase): NewGoalDao {
        return database.goalDao()
    }
}
