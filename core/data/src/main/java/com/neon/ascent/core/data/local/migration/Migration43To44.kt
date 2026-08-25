package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_43_44 = object : Migration(43, 44) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `biomarker_samples` (
                `id` TEXT NOT NULL, 
                `markerKey` TEXT NOT NULL, 
                `displayName` TEXT NOT NULL, 
                `value` REAL NOT NULL, 
                `unit` TEXT NOT NULL, 
                `drawnAt` INTEGER NOT NULL, 
                `source` TEXT NOT NULL, 
                `notes` TEXT, 
                PRIMARY KEY(`id`)
            )
        """)
    }
}
