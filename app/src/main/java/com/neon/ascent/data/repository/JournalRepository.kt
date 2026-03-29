package com.neon.ascent.data.repository

import com.neon.ascent.data.local.JournalDao
import com.neon.ascent.model.JournalEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalRepository @Inject constructor(
    private val journalDao: JournalDao
) {
    val allEntries: Flow<List<JournalEntry>> = journalDao.getAllEntries()

    suspend fun saveToJournal(entry: JournalEntry) {
        journalDao.insertEntry(entry)
    }

    suspend fun removeFromJournal(entry: JournalEntry) {
        journalDao.deleteEntry(entry)
    }

    suspend fun toggleHeart(id: String, isHearted: Boolean) {
        journalDao.toggleHeart(id, isHearted)
    }

    suspend fun isAlreadySaved(id: String): Boolean {
        return journalDao.exists(id)
    }
}
