package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create Special Attributes table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `special_attributes` (
                `type` TEXT NOT NULL,
                `baseValue` INTEGER NOT NULL DEFAULT 5,
                `currentValue` INTEGER NOT NULL,
                `percentile` INTEGER,
                `totalXp` INTEGER NOT NULL DEFAULT 0,
                `lastUpdated` INTEGER NOT NULL,
                PRIMARY KEY(`type`)
            )
            """.trimIndent()
        )

        // Create Benchmark Tests table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `benchmark_tests` (
                `id` TEXT NOT NULL,
                `attribute` TEXT NOT NULL,
                `testType` TEXT NOT NULL,
                `rawScore` REAL NOT NULL,
                `normalizedScore` REAL NOT NULL,
                `percentile` INTEGER,
                `metadata` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `source` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        // Optional indexes for performance
        db.execSQL("CREATE INDEX IF NOT EXISTS index_benchmark_tests_attribute_timestamp ON benchmark_tests(attribute, timestamp)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_benchmark_tests_source ON benchmark_tests(source)")
    }
}
