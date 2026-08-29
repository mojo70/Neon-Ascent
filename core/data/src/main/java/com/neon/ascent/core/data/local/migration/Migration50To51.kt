package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_50_51 = object : Migration(50, 51) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Update workout_sessions to add primaryAugmentId
        db.execSQL("ALTER TABLE workout_sessions ADD COLUMN primaryAugmentId TEXT")

        // 2. Create augment_activations table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `augment_activations` (
                `id` TEXT NOT NULL, 
                `augmentId` TEXT NOT NULL, 
                `userId` TEXT NOT NULL, 
                `mode` TEXT NOT NULL, 
                `status` TEXT NOT NULL, 
                `loggingStyle` TEXT NOT NULL, 
                `scheduledDays` TEXT NOT NULL, 
                `createdAt` INTEGER NOT NULL, 
                `windowStart` INTEGER, 
                `windowEnd` INTEGER, 
                `hostProtocolFilter` TEXT, 
                `dayTypeFilter` TEXT, 
                `reminderEnabled` INTEGER NOT NULL, 
                PRIMARY KEY(`id`), 
                FOREIGN KEY(`augmentId`) REFERENCES `workout_augments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """)

        // 3. Create indices for augment_activations
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_augment_activations_augmentId` ON `augment_activations` (`augmentId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_augment_activations_userId` ON `augment_activations` (`userId`)")
    }
}
