package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_58_59 = object : Migration(58, 59) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `self_map_mood_entries` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `score` INTEGER NOT NULL,
                `note` TEXT,
                `eoeId` INTEGER,
                `snapshotId` INTEGER
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_self_map_mood_entries_timestamp` ON `self_map_mood_entries` (`timestamp`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_self_map_mood_entries_eoeId` ON `self_map_mood_entries` (`eoeId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_self_map_mood_entries_snapshotId` ON `self_map_mood_entries` (`snapshotId`)")
    }
}
