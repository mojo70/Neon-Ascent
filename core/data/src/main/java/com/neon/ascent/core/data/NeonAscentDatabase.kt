package com.neon.ascent.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.neon.ascent.core.data.local.dao.SpecialDao
import com.neon.ascent.core.data.local.entity.BenchmarkTestEntity
import com.neon.ascent.core.data.local.entity.SpecialAttributeEntity
import com.neon.ascent.core.data.local.converter.DataSourceConverter
import com.neon.ascent.core.data.local.converter.InstantConverter
import com.neon.ascent.core.data.local.converter.SpecialTypeConverter
import com.neon.ascent.core.data.local.converter.StringMapConverter
import com.neon.ascent.core.data.local.converter.TestTypeConverter

@Database(
    entities = [
        GoalEntity::class,
        SpecialAttributeEntity::class,
        BenchmarkTestEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(
    InstantConverter::class,
    SpecialTypeConverter::class,
    TestTypeConverter::class,
    DataSourceConverter::class,
    StringMapConverter::class
)
abstract class NeonAscentDatabase : RoomDatabase() {
    abstract fun goalDao(): NewGoalDao
    abstract fun specialDao(): SpecialDao
}
