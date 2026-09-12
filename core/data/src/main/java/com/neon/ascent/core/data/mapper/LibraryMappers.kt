package com.neon.ascent.core.data.mapper

import com.neon.ascent.core.data.local.entity.LibraryBookEntity
import com.neon.ascent.core.data.local.entity.LibraryChapterEntity
import com.neon.ascent.core.data.local.entity.LibraryHighlightEntity
import com.neon.ascent.core.data.local.entity.LibraryQuoteEntity
import com.neon.ascent.core.domain.library.models.LibraryBook
import com.neon.ascent.core.domain.library.models.LibraryChapter
import com.neon.ascent.core.domain.library.models.LibraryHighlight
import com.neon.ascent.core.domain.library.models.LibraryQuote

fun LibraryBookEntity.toDomain() = LibraryBook(
    id = id,
    title = title,
    author = author,
    language = language,
    epubAssetPath = epubAssetPath
)

fun LibraryBook.toEntity() = LibraryBookEntity(
    id = id,
    title = title,
    author = author,
    language = language,
    epubAssetPath = epubAssetPath
)

fun LibraryChapterEntity.toDomain() = LibraryChapter(
    id = id,
    bookId = bookId,
    chapterIndex = chapterIndex,
    title = title,
    content = content
)

fun LibraryChapter.toEntity() = LibraryChapterEntity(
    id = id,
    bookId = bookId,
    chapterIndex = chapterIndex,
    title = title,
    content = content
)

fun LibraryHighlightEntity.toDomain() = LibraryHighlight(
    id = id,
    bookId = bookId,
    chapterIndex = chapterIndex,
    startOffset = startOffset,
    endOffset = endOffset,
    color = color,
    timestamp = timestamp
)

fun LibraryHighlight.toEntity() = LibraryHighlightEntity(
    id = id,
    bookId = bookId,
    chapterIndex = chapterIndex,
    startOffset = startOffset,
    endOffset = endOffset,
    color = color,
    timestamp = timestamp
)

fun LibraryQuoteEntity.toDomain() = LibraryQuote(
    id = id,
    bookId = bookId,
    bookTitle = bookTitle,
    content = content,
    chapterTitle = chapterTitle,
    timestamp = timestamp
)

fun LibraryQuote.toEntity() = LibraryQuoteEntity(
    id = id,
    bookId = bookId,
    bookTitle = bookTitle,
    content = content,
    chapterTitle = chapterTitle,
    timestamp = timestamp
)
