package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_60_61 = object : Migration(60, 61) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `ascension_directives` ADD COLUMN `priorityTier` TEXT")
        db.execSQL("ALTER TABLE `ascension_directives` ADD COLUMN `outcomeStatement` TEXT")
        db.execSQL("ALTER TABLE `ascension_directives` ADD COLUMN `affirmation` TEXT")
        db.execSQL("ALTER TABLE `ascension_directives` ADD COLUMN `visualUri` TEXT")
    }
}
