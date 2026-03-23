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
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val passphrase = net.sqlcipher.database.SQLiteDatabase.getBytes("NEON_ASCENT_SECURE_KEY".toCharArray())
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "neon_ascent_v2_secure.db"
        )
        .openHelperFactory(factory)
        .fallbackToDestructiveMigration() // Allow destructive migration during dev
        .build()
    }

    @Provides
    fun provideUserCharacterDao(database: AppDatabase): UserCharacterDao {
        return database.userCharacterDao()
    }
}
