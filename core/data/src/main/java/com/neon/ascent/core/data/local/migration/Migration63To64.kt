package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_63_64 = object : Migration(63, 64) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercise_definitions ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE exercise_definitions ADD COLUMN `isCustom` INTEGER NOT NULL DEFAULT 0")
    }
}
