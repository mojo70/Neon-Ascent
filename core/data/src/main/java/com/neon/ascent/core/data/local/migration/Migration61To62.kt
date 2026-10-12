package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_61_62 = object : Migration(61, 62) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `saved_reconstitutions` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `vialMg` TEXT NOT NULL,
                `waterMl` TEXT NOT NULL,
                `concentrationMgPerMl` TEXT NOT NULL,
                `mixedDate` TEXT,
                `notes` TEXT,
                `componentsJson` TEXT,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }
}
