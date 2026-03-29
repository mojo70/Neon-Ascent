package com.neon.ascent.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.neon.ascent.model.BioProtocolLog
import com.neon.ascent.model.BiohackingData
import com.neon.ascent.model.JournalEntry
import com.neon.ascent.model.Saying
import com.neon.ascent.model.UserCharacter
import com.neon.ascent.model.Quest
import com.neon.ascent.model.Task

@Database(entities = [
    UserCharacter::class, 
    BiohackingData::class, 
    BioProtocolLog::class, 
    Saying::class, 
    JournalEntry::class,
    Quest::class,
    Task::class
], version = 15)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userCharacterDao(): UserCharacterDao
    abstract fun biohackingDao(): BiohackingDao
    abstract fun sayingsDao(): SayingsDao
    abstract fun journalDao(): JournalDao
    abstract fun questDao(): QuestDao
    abstract fun taskDao(): TaskDao
}
