package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_74_75 = object : Migration(74, 75) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `neon_charge_ml_snapshots` (
                `snapshotId` TEXT NOT NULL PRIMARY KEY,
                `modelJson` TEXT NOT NULL,
                `effectiveN` INTEGER NOT NULL,
                `holdOutMae` REAL NOT NULL,
                `timestamp` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}
