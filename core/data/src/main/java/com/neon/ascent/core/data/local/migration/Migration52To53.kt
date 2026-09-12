package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_52_53 = object : Migration(52, 53) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `body_samples` (
                `id` TEXT NOT NULL,
                `localDate` TEXT NOT NULL,
                `loggedAt` INTEGER NOT NULL,
                `metric` TEXT NOT NULL,
                `site` TEXT,
                `value` REAL NOT NULL,
                `unit` TEXT NOT NULL,
                `method` TEXT,
                `position` TEXT,
                `side` TEXT,
                `conditionTag` TEXT,
                `source` TEXT NOT NULL,
                `hcRecordId` TEXT,
                `derived` INTEGER NOT NULL,
                `note` TEXT,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
    }
}
