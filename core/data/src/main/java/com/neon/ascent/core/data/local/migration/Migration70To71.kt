package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_70_71 = object : Migration(70, 71) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Safe check for cyberCrappRotationMode in user_workout_profiles
        val cursorProfiles = db.query("PRAGMA table_info(`user_workout_profiles`)")
        var hasRotationMode = false
        while (cursorProfiles.moveToNext()) {
            val nameIndex = cursorProfiles.getColumnIndex("name")
            if (nameIndex != -1 && cursorProfiles.getString(nameIndex) == "cyberCrappRotationMode") {
                hasRotationMode = true
                break
            }
        }
        cursorProfiles.close()

        if (!hasRotationMode) {
            db.execSQL("ALTER TABLE user_workout_profiles ADD COLUMN cyberCrappRotationMode TEXT NOT NULL DEFAULT 'STRAIGHT'")
        }

        // Safe check for scheduledDays in workout_routines
        val cursorRoutines = db.query("PRAGMA table_info(`workout_routines`)")
        var hasScheduledDays = false
        while (cursorRoutines.moveToNext()) {
            val nameIndex = cursorRoutines.getColumnIndex("name")
            if (nameIndex != -1 && cursorRoutines.getString(nameIndex) == "scheduledDays") {
                hasScheduledDays = true
                break
            }
        }
        cursorRoutines.close()

        if (!hasScheduledDays) {
            db.execSQL("ALTER TABLE workout_routines ADD COLUMN scheduledDays TEXT NOT NULL DEFAULT '[]'")
        }
    }
}
