package com.neon.ascent.core.domain.library.repository

import com.neon.ascent.core.domain.library.models.LibraryBook
import com.neon.ascent.core.domain.library.models.LibraryChapter
import com.neon.ascent.core.domain.library.models.LibraryHighlight
import com.neon.ascent.core.domain.library.models.LibraryQuote
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    suspend fun getBookById(id: String): LibraryBook?
    suspend fun getChaptersForBook(bookId: String): List<LibraryChapter>
    suspend fun upsertBook(book: LibraryBook, chapters: List<LibraryChapter>)
    fun getHighlightsForBook(bookId: String): Flow<List<LibraryHighlight>>
    suspend fun upsertHighlight(highlight: LibraryHighlight)
    suspend fun deleteHighlight(highlight: LibraryHighlight)
    fun getAllQuotes(): Flow<List<LibraryQuote>>
    suspend fun upsertQuote(quote: LibraryQuote)
}
