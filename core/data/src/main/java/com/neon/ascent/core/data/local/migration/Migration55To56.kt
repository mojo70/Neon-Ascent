package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_55_56 = object : Migration(55, 56) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `vault_accounts` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `institution` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `balance` REAL NOT NULL,
                `currency` TEXT NOT NULL,
                `isLinked` INTEGER NOT NULL,
                `lastUpdated` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `vault_snapshots` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `totalAssets` REAL NOT NULL,
                `totalDebt` REAL NOT NULL,
                `netWorth` REAL NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `vault_watchlist` (
                `symbol` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `isCrypto` INTEGER NOT NULL,
                PRIMARY KEY(`symbol`)
            )
        """.trimIndent())
    }
}
