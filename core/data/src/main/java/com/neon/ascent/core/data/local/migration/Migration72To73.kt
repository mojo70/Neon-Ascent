package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_72_73 = object : Migration(72, 73) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `neon_charge_labels` (
                `date` TEXT NOT NULL PRIMARY KEY,
                `wakeSeed` INTEGER NOT NULL,
                `hoursAwake` REAL NOT NULL,
                `sessionDrainRaw` REAL NOT NULL,
                `stepDrainRaw` REAL NOT NULL,
                `hrLoadMin` INTEGER NOT NULL,
                `napMin` INTEGER NOT NULL,
                `protocolFlags` TEXT NOT NULL,
                `activeKcal` REAL,
                `priorDayResidual` REAL,
                `feltCharge` INTEGER,
                `feltSource` TEXT NOT NULL,
                `labelWeight` REAL NOT NULL,
                `nextClearance` INTEGER,
                `isQuarantined` INTEGER NOT NULL,
                `quarantineExpiresAt` INTEGER,
                `confirmed` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}
