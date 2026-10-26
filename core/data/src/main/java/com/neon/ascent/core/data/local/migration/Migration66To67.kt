package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_66_67 = object : Migration(66, 67) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `supplements` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `name` TEXT NOT NULL,
                `aliasesJson` TEXT NOT NULL DEFAULT '[]',
                `status` TEXT NOT NULL,
                `primaryOutcomeDomainsJson` TEXT NOT NULL DEFAULT '[]',
                `mechanismTagsJson` TEXT NOT NULL DEFAULT '[]',
                `evidenceGrade` TEXT NOT NULL,
                `bestContexts` TEXT NOT NULL,
                `typicalDose` TEXT NOT NULL,
                `onset` TEXT NOT NULL,
                `keyLinksJson` TEXT NOT NULL DEFAULT '[]',
                `safetyNotes` TEXT NOT NULL,
                `isResearchChemical` INTEGER NOT NULL,
                `stackingNotesJson` TEXT NOT NULL DEFAULT '[]'
            )
            """.trimIndent()
        )
    }
}
