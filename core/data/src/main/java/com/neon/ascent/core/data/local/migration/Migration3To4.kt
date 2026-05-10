package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // === GOAL ENTITIES ===
        
        // Main goals table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `goals` (
                `id` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `linkedAttributes` TEXT NOT NULL,
                `progressCurrent` REAL NOT NULL,
                `progressTarget` REAL NOT NULL,
                `xpContributed` INTEGER NOT NULL,
                
                -- Aspiration specific
                `targetDateMillis` INTEGER,
                `status` TEXT,
                
                -- Mission specific
                `expiresAtMillis` INTEGER,
                `parentAspirationId` TEXT,
                
                -- Habit specific
                `recurrenceType` TEXT,
                `recurrenceDays` TEXT,
                `streak` INTEGER NOT NULL,
                `lastCompletedMillis` INTEGER,
                
                -- Task specific
                `parentGoalId` TEXT,
                
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        // Indexes for performance (matching Room's naming convention if possible, but Room usually expects them to exist)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_type` ON `goals` (`type`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_parentAspirationId` ON `goals` (`parentAspirationId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_expiresAtMillis` ON `goals` (`expiresAtMillis`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_lastCompletedMillis` ON `goals` (`lastCompletedMillis`)")
    }
}
