package com.neon.ascent.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.neon.ascent.data.local.entity.GoalEntity
import com.neon.ascent.data.local.entity.HabitMetricEntity
import com.neon.ascent.data.local.entity.TaskEntity
import com.neon.ascent.data.local.entity.UserStoryEntity
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
    MemoryFragment::class,
    BookEntity::class,
    ChapterEntity::class,
    HighlightEntity::class,
    QuoteEntity::class,
    DailyPrayer::class,
    StrengthBenchmark::class,
    UserStoryEntity::class,
    GoalEntity::class,
    TaskEntity::class,
    HabitMetricEntity::class
], version = 38)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userCharacterDao(): UserCharacterDao
    abstract fun biohackingDao(): BiohackingDao
    abstract fun sayingsDao(): SayingsDao
    abstract fun journalDao(): JournalDao
    abstract fun questDao(): QuestDao
    abstract fun taskDao(): TaskDao
    abstract fun loreDao(): LoreDao
    abstract fun bookDao(): BookDao
    abstract fun dailyPrayerDao(): DailyPrayerDao
    abstract fun benchmarkDao(): BenchmarkDao
    abstract fun userStoryDao(): UserStoryDao
    abstract fun goalDao(): GoalDao
    abstract fun goalTaskDao(): GoalTaskDao
    abstract fun habitMetricDao(): HabitMetricDao
}
