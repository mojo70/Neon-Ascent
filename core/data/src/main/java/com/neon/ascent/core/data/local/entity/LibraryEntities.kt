package com.neon.ascent.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "library_books")
data class LibraryBookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val language: String,
    val epubAssetPath: String? = null
)

@Entity(
    tableName = "library_chapters",
    foreignKeys = [
        ForeignKey(
            entity = LibraryBookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookId")]
)
data class LibraryChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val chapterIndex: Int,
    val title: String,
    val content: String
)

@Entity(
    tableName = "library_highlights",
    foreignKeys = [
        ForeignKey(
            entity = LibraryBookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookId")]
)
data class LibraryHighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val chapterIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val color: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "library_quotes",
    foreignKeys = [
        ForeignKey(
            entity = LibraryBookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookId")]
)
data class LibraryQuoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val bookTitle: String,
    val content: String,
    val chapterTitle: String,
    val timestamp: Long = System.currentTimeMillis()
)
