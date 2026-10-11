package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_59_60 = object : Migration(59, 60) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `ascension_directives` ADD COLUMN `anchoredSelfItemId` INTEGER")
        db.execSQL("ALTER TABLE `ascension_directives` ADD COLUMN `anchoredSelfItemName` TEXT")
        db.execSQL("ALTER TABLE `ascension_missions` ADD COLUMN `anchoredSelfItemId` INTEGER")
        db.execSQL("ALTER TABLE `ascension_missions` ADD COLUMN `anchoredSelfItemName` TEXT")
    }
}
