package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `biometric_events` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `timestamp` INTEGER NOT NULL, 
                `source` TEXT NOT NULL, 
                `type` TEXT NOT NULL, 
                `value` REAL NOT NULL, 
                `metadata` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `action_events` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `timestamp` INTEGER NOT NULL, 
                `actionType` TEXT NOT NULL, 
                `content` TEXT NOT NULL, 
                `metadata` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `socratic_insights` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `generatedAt` INTEGER NOT NULL, 
                `timeWindowStart` INTEGER NOT NULL, 
                `timeWindowEnd` INTEGER NOT NULL, 
                `content` TEXT NOT NULL, 
                `basedOnEventIds` TEXT NOT NULL, 
                `version` INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}
