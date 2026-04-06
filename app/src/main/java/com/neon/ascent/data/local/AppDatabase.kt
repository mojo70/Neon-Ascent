package com.neon.ascent.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.neon.ascent.model.*

@Database(entities = [
    UserCharacter::class, 
    BiohackingData::class, 
    BioProtocolLog::class, 
    Saying::class, 
    JournalEntry::class,
    Quest::class,
    Task::class,
    DataShard::class,
    MemoryFragment::class
], version = 18)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userCharacterDao(): UserCharacterDao
    abstract fun biohackingDao(): BiohackingDao
    abstract fun sayingsDao(): SayingsDao
    abstract fun journalDao(): JournalDao
    abstract fun questDao(): QuestDao
    abstract fun taskDao(): TaskDao
    abstract fun loreDao(): LoreDao
}
