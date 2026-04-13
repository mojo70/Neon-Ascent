package com.neon.ascent.data.local

import androidx.room.*
import com.neon.ascent.model.BookEntity
import com.neon.ascent.model.ChapterEntity
import com.neon.ascent.model.HighlightEntity
import com.neon.ascent.model.QuoteEntity

@Dao
interface BookDao {
    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: String): BookEntity?

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY chapterIndex ASC")
    suspend fun getChaptersForBook(bookId: String): List<ChapterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Transaction
    suspend fun insertFullBook(book: BookEntity, chapters: List<ChapterEntity>) {
        insertBook(book)
        insertChapters(chapters)
    }

    // Highlights
    @Query("SELECT * FROM highlights WHERE bookId = :bookId")
    fun getHighlightsForBook(bookId: String): kotlinx.coroutines.flow.Flow<List<HighlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: HighlightEntity)

    @Delete
    suspend fun deleteHighlight(highlight: HighlightEntity)

    // Quotes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuote(quote: QuoteEntity)

    @Query("SELECT * FROM quotes ORDER BY timestamp DESC")
    fun getAllQuotes(): kotlinx.coroutines.flow.Flow<List<QuoteEntity>>
}
