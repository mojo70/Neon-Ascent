package com.neon.ascent.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.neon.ascent.core.data.local.UplinkSecurityManager
import com.neon.ascent.data.local.AppDatabase
import com.neon.ascent.data.local.BenchmarkDao
import com.neon.ascent.data.local.BiohackingDao
import com.neon.ascent.data.local.BookDao
import com.neon.ascent.data.local.ChatDao
import com.neon.ascent.data.local.Converters
import com.neon.ascent.data.local.DailyPrayerDao
import com.neon.ascent.data.local.GoalDao
import com.neon.ascent.data.local.GoalTaskDao
import com.neon.ascent.data.local.HabitMetricDao
import com.neon.ascent.data.local.InventoryDao
import com.neon.ascent.data.local.JournalDao
import com.neon.ascent.data.local.LoreDao
import com.neon.ascent.data.local.NetWorthDao
import com.neon.ascent.data.local.QuestDao
import com.neon.ascent.data.local.SayingsDao
import com.neon.ascent.data.local.StockDao
import com.neon.ascent.data.local.TaskDao
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.data.local.UserStoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

/**
 * Open-fail does not delete user data.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        securityManager: UplinkSecurityManager
    ): AppDatabase {
        val dbName = "neon_ascent_v5_secure.db"
        
        try {
            SQLiteDatabase.loadLibs(context)
        } catch (e: Throwable) {
            Log.e("DatabaseModule", "Failed to load SQLCipher libs for AppDatabase", e)
        }

        val passphraseBytes = securityManager.getDatabasePassphrase("db_passphrase_app")
        val dbFile = context.getDatabasePath(dbName)
        
        // Pre-verification without wiping database on open failure
        if (dbFile.exists()) {
            var db: SQLiteDatabase? = null
            var openSuccessful = false
            try {
                db = SQLiteDatabase.openDatabase(dbFile.absolutePath, String(passphraseBytes), null, SQLiteDatabase.OPEN_READWRITE)
                db?.rawQuery("SELECT count(*) FROM sqlite_master", null)?.use { it.moveToFirst() }
                openSuccessful = true
            } catch (e: Throwable) {
                Log.e("DatabaseModule", "AppDatabase verification failed. Preserving file in quarantine.", e)
            } finally {
                try { db?.close() } catch (_: Throwable) {}
            }

            if (!openSuccessful) {
                try {
                    val quarantineFile = context.getDatabasePath("$dbName.quarantine")
                    if (quarantineFile.exists()) quarantineFile.delete()
                    dbFile.renameTo(quarantineFile)
                    val shmFile = context.getDatabasePath("$dbName-shm")
                    if (shmFile.exists()) shmFile.renameTo(context.getDatabasePath("$dbName-shm.quarantine"))
                    val walFile = context.getDatabasePath("$dbName-wal")
                    if (walFile.exists()) walFile.renameTo(context.getDatabasePath("$dbName-wal.quarantine"))
                    Log.w("DatabaseModule", "Successfully quarantined unopenable $dbName to ${quarantineFile.name}")
                } catch (e: Throwable) {
                    Log.e("DatabaseModule", "Failed to quarantine $dbName", e)
                }
            }
        }

        val factory = SupportFactory(passphraseBytes)
        
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            dbName
        )
        .openHelperFactory(factory)
        .addTypeConverter(Converters())
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

    @Provides
    fun provideUserStoryDao(database: AppDatabase): UserStoryDao {
        return database.userStoryDao()
    }

    @Provides
    fun provideGoalDao(database: AppDatabase): GoalDao {
        return database.goalDao()
    }

    @Provides
    fun provideGoalTaskDao(database: AppDatabase): GoalTaskDao {
        return database.goalTaskDao()
    }

    @Provides
    fun provideHabitMetricDao(database: AppDatabase): HabitMetricDao {
        return database.habitMetricDao()
    }

    @Provides
    fun provideInventoryDao(database: AppDatabase): InventoryDao {
        return database.inventoryDao()
    }

    @Provides
    fun provideChatDao(database: AppDatabase): ChatDao {
        return database.chatDao()
    }

    @Provides
    fun provideStockDao(database: AppDatabase): StockDao {
        return database.stockDao()
    }

    @Provides
    fun provideNetWorthDao(database: AppDatabase): NetWorthDao {
        return database.netWorthDao()
    }
}
