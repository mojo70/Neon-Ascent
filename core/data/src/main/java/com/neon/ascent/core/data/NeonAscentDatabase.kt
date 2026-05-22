package com.neon.ascent.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.neon.ascent.core.data.local.dao.SpecialDao
import com.neon.ascent.core.data.local.dao.GoalDao
import com.neon.ascent.core.data.local.dao.AscensionDao
import com.neon.ascent.core.data.local.entity.*
import com.neon.ascent.core.data.local.converter.*
import com.neon.ascent.core.data.local.migration.MIGRATION_3_4

@Database(
    entities = [
        GoalEntity::class,
        SpecialAttributeEntity::class,
        BenchmarkTestEntity::class,
        AscensionDirectiveEntity::class,
        AscensionMissionEntity::class,
        AscensionTaskEntity::class,
        AscensionTaskCompletionEntity::class,
        NeuralLogEntity::class,
        NeuralMemory::class
    ],
    version = 11,
    exportSchema = true
)
@TypeConverters(
    InstantConverter::class,
    LocalDateConverter::class,
    SpecialTypeConverter::class,
    TestTypeConverter::class,
    DataSourceConverter::class,
    StringMapConverter::class,
    SpecialTypeListConverter::class,
    StringListConverter::class
)
abstract class NeonAscentDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun specialDao(): SpecialDao
    abstract fun ascensionDao(): AscensionDao
    abstract fun neuralMemoryDao(): com.neon.ascent.core.data.local.dao.NeuralMemoryDao
}
