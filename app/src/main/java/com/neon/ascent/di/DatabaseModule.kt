package com.neon.ascent.di

import android.content.Context
import androidx.room.Room
import com.neon.ascent.data.local.AppDatabase
import com.neon.ascent.data.local.BenchmarkDao
import com.neon.ascent.data.local.BiohackingDao
import com.neon.ascent.data.local.BookDao
import com.neon.ascent.data.local.DailyPrayerDao
import com.neon.ascent.data.local.JournalDao
import com.neon.ascent.data.local.LoreDao
import com.neon.ascent.data.local.QuestDao
import com.neon.ascent.data.local.SayingsDao
import com.neon.ascent.data.local.TaskDao
import com.neon.ascent.data.local.UserCharacterDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val passphrase = SQLiteDatabase.getBytes("neon_protocol_secure_alpha".toCharArray())
        val factory = SupportFactory(passphrase)
        
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "neon_ascent_v5_secure.db"
        )
        .openHelperFactory(factory)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideUserCharacterDao(database: AppDatabase): UserCharacterDao {
        return database.userCharacterDao()
    }

    @Provides
    fun provideBiohackingDao(database: AppDatabase): BiohackingDao {
        return database.biohackingDao()
    }

    @Provides
    fun provideSayingsDao(database: AppDatabase): SayingsDao {
        return database.sayingsDao()
    }

    @Provides
    fun provideJournalDao(database: AppDatabase): JournalDao {
        return database.journalDao()
    }

    @Provides
    fun provideQuestDao(database: AppDatabase): QuestDao {
        return database.questDao()
    }

    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao {
        return database.taskDao()
    }

    @Provides
    fun provideLoreDao(database: AppDatabase): LoreDao {
        return database.loreDao()
    }

    @Provides
    fun provideBookDao(database: AppDatabase): BookDao {
        return database.bookDao()
    }

    @Provides
    fun provideDailyPrayerDao(database: AppDatabase): DailyPrayerDao {
        return database.dailyPrayerDao()
    }

    @Provides
    fun provideBenchmarkDao(database: AppDatabase): BenchmarkDao {
        return database.benchmarkDao()
    }
}
