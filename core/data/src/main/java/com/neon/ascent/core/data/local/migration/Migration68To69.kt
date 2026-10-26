package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_68_69 = object : Migration(68, 69) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `biohacking_protocols` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `name` TEXT NOT NULL,
                `aliasesJson` TEXT NOT NULL DEFAULT '[]',
                `status` TEXT NOT NULL,
                `primaryOutcomeDomainsJson` TEXT NOT NULL DEFAULT '[]',
                `protocolCategory` TEXT NOT NULL,
                `mechanismTagsJson` TEXT NOT NULL DEFAULT '[]',
                `evidenceGrade` TEXT NOT NULL,
                `bestContexts` TEXT NOT NULL,
                `typicalParameters` TEXT NOT NULL,
                `onset` TEXT NOT NULL,
                `keyLinksJson` TEXT NOT NULL DEFAULT '[]',
                `safetyNotes` TEXT NOT NULL,
                `notes` TEXT NOT NULL DEFAULT '',
                `stackingNotesJson` TEXT NOT NULL DEFAULT '[]'
            )
            """.trimIndent()
        )
    }
}
