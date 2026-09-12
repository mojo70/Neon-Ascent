package com.neon.ascent.core.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import com.neon.ascent.core.data.local.UplinkSecurityManager
import com.neon.ascent.core.data.local.dao.SpecialDao
import com.neon.ascent.core.data.local.dao.GoalDao
import com.neon.ascent.core.data.local.dao.AscensionDao
import com.neon.ascent.core.data.local.dao.BiomarkerDao
import com.neon.ascent.core.data.local.dao.DopamineMenuDao
import com.neon.ascent.core.data.local.dao.InsightDao
import com.neon.ascent.core.data.local.dao.ProtocolDao
import com.neon.ascent.core.data.local.migration.MIGRATION_2_3
import com.neon.ascent.core.data.local.migration.MIGRATION_3_4
import com.neon.ascent.core.data.local.migration.MIGRATION_11_12
import com.neon.ascent.core.data.local.migration.MIGRATION_43_44
import com.neon.ascent.core.data.local.migration.MIGRATION_45_46
import com.neon.ascent.core.data.local.migration.MIGRATION_46_47
import com.neon.ascent.core.data.local.migration.MIGRATION_47_48
import com.neon.ascent.core.data.local.migration.MIGRATION_48_49
import com.neon.ascent.core.data.local.migration.MIGRATION_50_51
import com.neon.ascent.core.data.local.migration.MIGRATION_51_52
import com.neon.ascent.core.data.local.migration.MIGRATION_52_53
import com.neon.ascent.core.data.local.dao.DailyVitalRollupDao
import com.neon.ascent.core.data.local.dao.BodySampleDao
import com.neon.ascent.core.data.local.dao.NeuralMemoryDao
import com.neon.ascent.core.data.local.dao.WorkoutDao
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
    fun provideDatabase(
        @ApplicationContext context: Context,
        securityManager: UplinkSecurityManager
    ): NeonAscentDatabase {
        val dbName = "neon_ascent_database"
        val dbFile = context.getDatabasePath(dbName)
        
        try {
            SQLiteDatabase.loadLibs(context)
        } catch (e: Throwable) {
            Log.e("DatabaseModule", "Failed to load SQLCipher libs", e)
        }
        
        val passphraseBytes = securityManager.getDatabasePassphrase()

        // Pre-verification without wiping database on open failure
        if (dbFile.exists()) {
            var db: SQLiteDatabase? = null
            var openSuccessful = false
            try {
                db = SQLiteDatabase.openDatabase(
                    dbFile.absolutePath, 
                    String(passphraseBytes), 
                    null, 
                    SQLiteDatabase.OPEN_READWRITE
                )
                db?.rawQuery("SELECT count(*) FROM sqlite_master", null)?.use { it.moveToFirst() }
                openSuccessful = true
            } catch (e: Throwable) {
                Log.e("DatabaseModule", "NeonAscentDatabase verification failed. Preserving file in quarantine.", e)
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
        
        // Schema Migration gap policy:
        // Version 53 contains historical schema gaps (e.g. v4-11, v12-43, v44-45, v49-50).
        // Destructive migrations are strictly forbidden to prevent logbook/user data wipes.
        // If an unhandled migration path is encountered, Room fails open safely without deleting the database file.
        return Room.databaseBuilder(
            context,
            NeonAscentDatabase::class.java,
            dbName
        )
        .openHelperFactory(factory)
        .addMigrations(
            MIGRATION_2_3, MIGRATION_3_4, MIGRATION_11_12, 
            MIGRATION_43_44, MIGRATION_45_46, MIGRATION_46_47, 
            MIGRATION_47_48, MIGRATION_48_49, MIGRATION_50_51,
            MIGRATION_51_52, MIGRATION_52_53
        )
        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .build()
    }

    @Provides
    fun provideGoalDao(database: NeonAscentDatabase): GoalDao {
        return database.goalDao()
    }

    @Provides
    fun provideSpecialDao(database: NeonAscentDatabase): SpecialDao {
        return database.specialDao()
    }

    @Provides
    fun provideAscensionDao(database: NeonAscentDatabase): AscensionDao {
        return database.ascensionDao()
    }

    @Provides
    fun provideNeuralMemoryDao(database: NeonAscentDatabase): NeuralMemoryDao {
        return database.neuralMemoryDao()
    }

    @Provides
    fun provideInsightDao(database: NeonAscentDatabase): InsightDao {
        return database.insightDao()
    }

    @Provides
    fun provideDopamineMenuDao(database: NeonAscentDatabase): DopamineMenuDao {
        return database.dopamineMenuDao()
    }

    @Provides
    fun provideProtocolDao(database: NeonAscentDatabase): ProtocolDao {
        return database.protocolDao()
    }

    @Provides
    fun provideWorkoutDao(database: NeonAscentDatabase): WorkoutDao {
        return database.workoutDao()
    }

    @Provides
    fun provideBiomarkerDao(database: NeonAscentDatabase): BiomarkerDao {
        return database.biomarkerDao()
    }

    @Provides
    fun provideDailyVitalRollupDao(database: NeonAscentDatabase): DailyVitalRollupDao {
        return database.dailyVitalRollupDao()
    }

    @Provides
    fun provideBodySampleDao(database: NeonAscentDatabase): BodySampleDao {
        return database.bodySampleDao()
    }
}
