package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_48_49 = object : Migration(48, 49) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Update workout_sessions
        database.execSQL("ALTER TABLE workout_sessions ADD COLUMN cycleId TEXT")
        database.execSQL("ALTER TABLE workout_sessions ADD COLUMN protocolDayType TEXT")

        // 2. Update set_logs
        database.execSQL("ALTER TABLE set_logs ADD COLUMN prescribedWeight REAL")
        database.execSQL("ALTER TABLE set_logs ADD COLUMN prescribedReps INTEGER")
        database.execSQL("ALTER TABLE set_logs ADD COLUMN percentOfMax REAL")
        database.execSQL("ALTER TABLE set_logs ADD COLUMN isAmrap INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE set_logs ADD COLUMN accommodatingLoad REAL")

        // 3. Create protocol_cycles table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `protocol_cycles` (
                `id` TEXT NOT NULL, 
                `userId` TEXT NOT NULL, 
                `protocol` TEXT NOT NULL, 
                `startedAt` INTEGER NOT NULL, 
                `endedAt` INTEGER, 
                `status` TEXT NOT NULL, 
                `currentWeek` INTEGER NOT NULL, 
                `currentDayIndex` INTEGER NOT NULL, 
                `configJson` TEXT, 
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        // 4. Create exercise_maxes table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `exercise_maxes` (
                `id` TEXT NOT NULL, 
                `familyId` TEXT NOT NULL, 
                `testedAt` INTEGER NOT NULL, 
                `oneRepMax` REAL NOT NULL, 
                `rm15` REAL, 
                `rm10` REAL, 
                `rm5` REAL, 
                `trainingMax` REAL, 
                `source` TEXT NOT NULL, 
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_exercise_maxes_familyId` ON `exercise_maxes` (`familyId`)")
    }
}
