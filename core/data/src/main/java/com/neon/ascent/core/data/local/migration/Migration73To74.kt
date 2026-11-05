package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_73_74 = object : Migration(73, 74) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `neon_charge_calibration` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `isPromoted` INTEGER NOT NULL,
                `kPassive` REAL NOT NULL,
                `sessionMult` REAL NOT NULL,
                `stepMult` REAL NOT NULL,
                `hrMult` REAL NOT NULL,
                `effectiveN` INTEGER NOT NULL,
                `fitDate` INTEGER NOT NULL,
                `inSampleMae` REAL NOT NULL,
                `looMae` REAL NOT NULL,
                `isResetToPrior` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}
