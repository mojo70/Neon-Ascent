package com.neon.ascent.data.local

import androidx.room.*
import com.neon.ascent.model.JournalEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<JournalEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntry)

    @Delete
    suspend fun deleteEntry(entry: JournalEntry)

    @Query("UPDATE journal_entries SET isHearted = :isHearted WHERE id = :id")
    suspend fun toggleHeart(id: String, isHearted: Boolean)

    @Query("SELECT EXISTS(SELECT 1 FROM journal_entries WHERE id = :id)")
    suspend fun exists(id: String): Boolean
}
