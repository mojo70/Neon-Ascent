package com.neon.ascent.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.neon.ascent.core.data.local.entity.LibraryBookEntity
import com.neon.ascent.core.data.local.entity.LibraryChapterEntity
import com.neon.ascent.core.data.local.entity.LibraryHighlightEntity
import com.neon.ascent.core.data.local.entity.LibraryQuoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Query("SELECT * FROM library_books WHERE id = :bookId")
    suspend fun getBookById(bookId: String): LibraryBookEntity?

    @Query("SELECT * FROM library_chapters WHERE bookId = :bookId ORDER BY chapterIndex ASC")
    suspend fun getChaptersForBook(bookId: String): List<LibraryChapterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: LibraryBookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<LibraryChapterEntity>)

    @Transaction
    suspend fun insertFullBook(book: LibraryBookEntity, chapters: List<LibraryChapterEntity>) {
        insertBook(book)
        insertChapters(chapters)
    }

    @Query("SELECT * FROM library_highlights WHERE bookId = :bookId")
    fun getHighlightsForBook(bookId: String): Flow<List<LibraryHighlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: LibraryHighlightEntity)

    @Delete
    suspend fun deleteHighlight(highlight: LibraryHighlightEntity)

    @Query("SELECT * FROM library_quotes ORDER BY timestamp DESC")
    fun getAllQuotes(): Flow<List<LibraryQuoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuote(quote: LibraryQuoteEntity)
}
