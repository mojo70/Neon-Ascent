package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_57_58 = object : Migration(57, 58) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `self_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `importance` REAL NOT NULL,
                `notes` TEXT,
                `isArchived` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `baselineMassContributor` INTEGER
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_self_items_category` ON `self_items` (`category`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_self_items_isArchived` ON `self_items` (`isArchived`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_self_items_importance` ON `self_items` (`importance`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `eoe_entries` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `emotionLabel` TEXT NOT NULL,
                `selfItemId` INTEGER NOT NULL,
                `epText` TEXT NOT NULL,
                `perceptionText` TEXT NOT NULL,
                `appraisal` TEXT,
                `intensity` INTEGER,
                `canChangeNotes` TEXT,
                `linkedSnapshotId` INTEGER
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_eoe_entries_timestamp` ON `eoe_entries` (`timestamp`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_eoe_entries_selfItemId` ON `eoe_entries` (`selfItemId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `self_map_snapshots` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `mass` REAL NOT NULL,
                `itemCount` INTEGER NOT NULL,
                `avgImportance` REAL NOT NULL,
                `coreCount` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_self_map_snapshots_timestamp` ON `self_map_snapshots` (`timestamp`)")
    }
}
