package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_65_66 = object : Migration(65, 66) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `diets` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `name` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `primaryOutcomeDomains` TEXT NOT NULL,
                `dietCategory` TEXT NOT NULL,
                `mechanismTags` TEXT NOT NULL,
                `evidenceGrade` TEXT NOT NULL,
                `typicalParametersJson` TEXT NOT NULL,
                `onset` TEXT NOT NULL,
                `keyLinks` TEXT NOT NULL,
                `safetyNotes` TEXT NOT NULL,
                `isAppSpecific` INTEGER NOT NULL,
                `stackingNotesJson` TEXT NOT NULL DEFAULT '[]'
            )
            """.trimIndent()
        )
    }
}
