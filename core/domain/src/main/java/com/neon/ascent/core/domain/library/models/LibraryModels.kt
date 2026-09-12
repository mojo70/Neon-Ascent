package com.neon.ascent.core.domain.library.models

data class LibraryBook(
    val id: String,
    val title: String,
    val author: String,
    val language: String,
    val epubAssetPath: String? = null
)

data class LibraryChapter(
    val id: Long = 0,
    val bookId: String,
    val chapterIndex: Int,
    val title: String,
    val content: String
)

data class LibraryHighlight(
    val id: Long = 0,
    val bookId: String,
    val chapterIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val color: Long,
    val timestamp: Long = System.currentTimeMillis()
)

data class LibraryQuote(
    val id: Long = 0,
    val bookId: String,
    val bookTitle: String,
    val content: String,
    val chapterTitle: String,
    val timestamp: Long = System.currentTimeMillis()
)
