package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_53_54 = object : Migration(53, 54) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `operative_profile` (
                `id` TEXT NOT NULL,
                `numericId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `netrunnerName` TEXT,
                `sex` TEXT NOT NULL,
                `dob` TEXT NOT NULL,
                `units` TEXT NOT NULL,
                `heightFeet` TEXT,
                `heightInches` TEXT,
                `heightCm` TEXT,
                `weight` TEXT NOT NULL,
                `somatotype` REAL NOT NULL,
                `mbti` TEXT,
                `alignment` TEXT,
                `archetype` TEXT,
                `level` INTEGER NOT NULL,
                `experience` INTEGER NOT NULL,
                `iceLevel` INTEGER NOT NULL,
                `eddies` INTEGER NOT NULL,
                `secureEddies` INTEGER NOT NULL,
                `hasBreachedBefore` INTEGER NOT NULL,
                `isSystemDatabaseUnlocked` INTEGER NOT NULL,
                `walletConnected` INTEGER NOT NULL,
                `strength` INTEGER,
                `perception` INTEGER,
                `endurance` INTEGER,
                `charisma` INTEGER,
                `agility` INTEGER,
                `luck` INTEGER,
                `intelligence` INTEGER,
                `holyGhost` INTEGER,
                `holyGhostExp` INTEGER NOT NULL,
                `prayerStreak` INTEGER NOT NULL,
                `lastPrayerDate` INTEGER NOT NULL,
                `waterBaptized` INTEGER NOT NULL,
                `holySpiritBaptized` INTEGER NOT NULL,
                `hasTonguesAura` INTEGER NOT NULL,
                `avatarPath` TEXT,
                `isCreationComplete` INTEGER NOT NULL,
                `neuralLoad` REAL NOT NULL,
                `chessElo` INTEGER NOT NULL,
                `equippedCyberware` TEXT,
                `cyberdeckName` TEXT,
                `ramSlots` INTEGER NOT NULL,
                `usedRam` INTEGER NOT NULL,
                `quickhackSlots` INTEGER NOT NULL,
                `loadedQuickhacks` TEXT,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
    }
}
