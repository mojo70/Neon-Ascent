package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_54_55 = object : Migration(54, 55) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `library_books` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `author` TEXT NOT NULL,
                `language` TEXT NOT NULL,
                `epubAssetPath` TEXT,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `library_chapters` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `bookId` TEXT NOT NULL,
                `chapterIndex` INTEGER NOT NULL,
                `title` TEXT NOT NULL,
                `content` TEXT NOT NULL,
                FOREIGN KEY(`bookId`) REFERENCES `library_books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_chapters_bookId` ON `library_chapters` (`bookId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `library_highlights` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `bookId` TEXT NOT NULL,
                `chapterIndex` INTEGER NOT NULL,
                `startOffset` INTEGER NOT NULL,
                `endOffset` INTEGER NOT NULL,
                `color` INTEGER NOT NULL,
                `timestamp` INTEGER NOT NULL,
                FOREIGN KEY(`bookId`) REFERENCES `library_books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_highlights_bookId` ON `library_highlights` (`bookId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `library_quotes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `bookId` TEXT NOT NULL,
                `bookTitle` TEXT NOT NULL,
                `content` TEXT NOT NULL,
                `chapterTitle` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                FOREIGN KEY(`bookId`) REFERENCES `library_books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_quotes_bookId` ON `library_quotes` (`bookId`)")
    }
}
