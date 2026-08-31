package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_51_52 = object : Migration(51, 52) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `daily_vital_rollups` (
                `localDate` TEXT NOT NULL, 
                `metric` TEXT NOT NULL, 
                `value` REAL NOT NULL, 
                `source` TEXT NOT NULL, 
                `quality` TEXT NOT NULL, 
                `updatedAt` INTEGER NOT NULL, 
                PRIMARY KEY(`localDate`, `metric`)
            )
        """.trimIndent())
    }
}
