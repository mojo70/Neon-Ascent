package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.dao.LibraryDao
import com.neon.ascent.core.data.mapper.toDomain
import com.neon.ascent.core.data.mapper.toEntity
import com.neon.ascent.core.domain.library.models.LibraryBook
import com.neon.ascent.core.domain.library.models.LibraryChapter
import com.neon.ascent.core.domain.library.models.LibraryHighlight
import com.neon.ascent.core.domain.library.models.LibraryQuote
import com.neon.ascent.core.domain.library.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val libraryDao: LibraryDao
) : LibraryRepository {

    override suspend fun getBookById(id: String): LibraryBook? {
        return libraryDao.getBookById(id)?.toDomain()
    }

    override suspend fun getChaptersForBook(bookId: String): List<LibraryChapter> {
        return libraryDao.getChaptersForBook(bookId).map { it.toDomain() }
    }

    override suspend fun upsertBook(book: LibraryBook, chapters: List<LibraryChapter>) {
        libraryDao.insertFullBook(
            book.toEntity(),
            chapters.map { it.toEntity() }
        )
    }

    override fun getHighlightsForBook(bookId: String): Flow<List<LibraryHighlight>> {
        return libraryDao.getHighlightsForBook(bookId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun upsertHighlight(highlight: LibraryHighlight) {
        libraryDao.insertHighlight(highlight.toEntity())
    }

    override suspend fun deleteHighlight(highlight: LibraryHighlight) {
        libraryDao.deleteHighlight(highlight.toEntity())
    }

    override fun getAllQuotes(): Flow<List<LibraryQuote>> {
        return libraryDao.getAllQuotes().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun upsertQuote(quote: LibraryQuote) {
        libraryDao.insertQuote(quote.toEntity())
    }
}
