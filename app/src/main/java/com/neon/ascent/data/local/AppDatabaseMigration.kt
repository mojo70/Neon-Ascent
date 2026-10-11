package com.neon.ascent.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_59_60_APP = object : Migration(59, 60) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `journal_entries` ADD COLUMN `selfItemId` INTEGER")
    }
}
