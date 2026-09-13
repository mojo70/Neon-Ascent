package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_56_57 = object : Migration(56, 57) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `rite_sessions` (
                `id` TEXT NOT NULL,
                `kind` TEXT NOT NULL,
                `startedAt` INTEGER NOT NULL,
                `endedAt` INTEGER,
                `durationMin` INTEGER NOT NULL,
                `source` TEXT NOT NULL,
                `notes` TEXT,
                `localDate` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_rite_sessions_kind` ON `rite_sessions` (`kind`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_rite_sessions_localDate` ON `rite_sessions` (`localDate`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_rite_sessions_startedAt` ON `rite_sessions` (`startedAt`)")
    }
}
