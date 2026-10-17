package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_62_63 = object : Migration(62, 63) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_workout_profiles ADD COLUMN `progressionMode` TEXT NOT NULL DEFAULT 'FIXED_INCREMENT'")
        db.execSQL("ALTER TABLE user_workout_profiles ADD COLUMN `weightIncrementUpperCompound` REAL NOT NULL DEFAULT 5.0")
        db.execSQL("ALTER TABLE user_workout_profiles ADD COLUMN `weightIncrementLowerCompound` REAL NOT NULL DEFAULT 10.0")
        db.execSQL("ALTER TABLE user_workout_profiles ADD COLUMN `progressionPercentage` REAL NOT NULL DEFAULT 2.5")
        db.execSQL("ALTER TABLE user_workout_profiles ADD COLUMN `roundingIncrement` REAL NOT NULL DEFAULT 5.0")
    }
}
