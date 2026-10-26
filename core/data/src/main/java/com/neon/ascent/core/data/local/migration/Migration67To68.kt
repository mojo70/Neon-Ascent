package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_67_68 = object : Migration(67, 68) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Safe idempotent check for stackingNotesJson column in case supplements table already contains it
        val cursor = db.query("PRAGMA table_info(`supplements`)")
        var columnExists = false
        while (cursor.moveToNext()) {
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex != -1 && cursor.getString(nameIndex) == "stackingNotesJson") {
                columnExists = true
                break
            }
        }
        cursor.close()

        if (!columnExists) {
            db.execSQL("ALTER TABLE `supplements` ADD COLUMN `stackingNotesJson` TEXT NOT NULL DEFAULT '[]'")
        }
    }
}
