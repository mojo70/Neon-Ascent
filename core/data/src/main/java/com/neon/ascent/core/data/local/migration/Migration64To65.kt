package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_64_65 = object : Migration(64, 65) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `deload_windows` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `userId` TEXT NOT NULL,
                `start` INTEGER NOT NULL,
                `end` INTEGER NOT NULL,
                `source` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `snoozedUntil` INTEGER
            )
            """.trimIndent()
        )
    }
}
