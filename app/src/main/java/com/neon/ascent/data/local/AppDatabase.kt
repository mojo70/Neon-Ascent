package com.neon.ascent.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.neon.ascent.model.BioProtocolLog
import com.neon.ascent.model.BiohackingData
import com.neon.ascent.model.JournalEntry
import com.neon.ascent.model.Saying
import com.neon.ascent.model.UserCharacter

@Database(entities = [UserCharacter::class, BiohackingData::class, BioProtocolLog::class, Saying::class, JournalEntry::class], version = 14)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userCharacterDao(): UserCharacterDao
    abstract fun biohackingDao(): BiohackingDao
    abstract fun sayingsDao(): SayingsDao
    abstract fun journalDao(): JournalDao
}
