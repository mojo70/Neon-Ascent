package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_47_48 = object : Migration(47, 48) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create protocol_rep_targets table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `protocol_rep_targets` (
                `id` TEXT NOT NULL, 
                `protocol` TEXT NOT NULL, 
                `movementType` TEXT NOT NULL, 
                `setType` TEXT NOT NULL, 
                `familyId` TEXT, 
                `minReps` INTEGER NOT NULL, 
                `maxReps` INTEGER NOT NULL, 
                `unit` TEXT NOT NULL DEFAULT 'REPS', 
                PRIMARY KEY(`id`)
            )
        """)
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_protocol_rep_targets_protocol_movementType_setType_familyId` ON `protocol_rep_targets` (`protocol`, `movementType`, `setType`, `familyId`)")

        // 2. Add rangeOverride columns to exercise_definitions
        db.execSQL("ALTER TABLE exercise_definitions ADD COLUMN rangeOverrideMin INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE exercise_definitions ADD COLUMN rangeOverrideMax INTEGER DEFAULT NULL")
    }
}
