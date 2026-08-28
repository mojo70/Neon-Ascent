package com.neon.ascent.core.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.neon.ascent.core.data.local.UplinkSecurityManager
import com.neon.ascent.core.data.local.dao.SpecialDao
import com.neon.ascent.core.data.local.dao.GoalDao
import com.neon.ascent.core.data.local.dao.AscensionDao
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
    fun provideDatabase(
        @ApplicationContext context: Context,
        securityManager: UplinkSecurityManager
    ): NeonAscentDatabase {
        val dbName = "neon_ascent_database"
        val dbFile = context.getDatabasePath(dbName)
        
        // Load SQLCipher libraries early
        SQLiteDatabase.loadLibs(context)
        
        val passphraseBytes = try {
            securityManager.getDatabasePassphrase()
        } catch (e: Exception) {
            android.util.Log.e("DatabaseModule", "Failed to get passphrase", e)
            "fallback_key".toByteArray()
        }
        val passphraseString = String(passphraseBytes)

        // Pre-verification: try to open the database if it exists
        if (dbFile.exists()) {
            var db: SQLiteDatabase? = null
            try {
                db = SQLiteDatabase.openDatabase(
                    dbFile.absolutePath, 
                    passphraseString, 
                    null, 
                    SQLiteDatabase.OPEN_READWRITE
                )
                // Minimal query to verify encryption
                db.rawQuery("SELECT count(*) FROM sqlite_master", null)?.use { it.moveToFirst() }
            } catch (e: Exception) {
                android.util.Log.e("DatabaseModule", "Database verification failed. Wiping.", e)
                context.deleteDatabase(dbName)
            } finally {
                db?.close()
            }
        }

        val factory = SupportFactory(passphraseBytes)
        
        return Room.databaseBuilder(
            context,
            NeonAscentDatabase::class.java,
            dbName
        )
        .openHelperFactory(factory)
        .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_11_12, MIGRATION_43_44, MIGRATION_45_46, MIGRATION_46_47, MIGRATION_47_48, MIGRATION_48_49)
        .fallbackToDestructiveMigration()
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
    fun provideNeuralMemoryDao(database: NeonAscentDatabase): com.neon.ascent.core.data.local.dao.NeuralMemoryDao {
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
    fun provideWorkoutDao(database: NeonAscentDatabase): com.neon.ascent.core.data.local.dao.WorkoutDao {
        return database.workoutDao()
    }

    @Provides
    fun provideBiomarkerDao(database: NeonAscentDatabase): com.neon.ascent.core.data.local.dao.BiomarkerDao {
        return database.biomarkerDao()
    }
}
