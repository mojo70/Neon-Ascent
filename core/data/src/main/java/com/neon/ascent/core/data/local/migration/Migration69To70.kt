package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_69_70 = object : Migration(69, 70) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_workout_profiles ADD COLUMN cyberCrappRotationMode TEXT NOT NULL DEFAULT 'STRAIGHT'")
        db.execSQL("ALTER TABLE workout_routines ADD COLUMN scheduledDays TEXT NOT NULL DEFAULT '[]'")
    }
}
