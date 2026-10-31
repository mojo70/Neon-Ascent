package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_71_72 = object : Migration(71, 72) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `sanctuary_passages` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `book` TEXT NOT NULL,
                `chapter` INTEGER NOT NULL,
                `verses` TEXT NOT NULL,
                `reference` TEXT NOT NULL,
                `translation` TEXT NOT NULL,
                `title` TEXT,
                `text` TEXT NOT NULL,
                `themes` TEXT NOT NULL,
                `tags` TEXT NOT NULL,
                `sourceNote` TEXT,
                `sortOrder` INTEGER NOT NULL,
                `isFavoriteDefault` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}
