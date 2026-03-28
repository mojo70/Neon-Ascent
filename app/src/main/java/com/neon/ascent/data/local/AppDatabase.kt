package com.neon.ascent.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.neon.ascent.model.BioProtocolLog
import com.neon.ascent.model.BiohackingData
import com.neon.ascent.model.UserCharacter

@Database(entities = [UserCharacter::class, BiohackingData::class, BioProtocolLog::class], version = 8)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userCharacterDao(): UserCharacterDao
    abstract fun biohackingDao(): BiohackingDao
}
