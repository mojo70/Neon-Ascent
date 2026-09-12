package com.neon.ascent.data.repository

import com.neon.ascent.core.domain.chronicle.ChronicleEntry
import com.neon.ascent.core.domain.chronicle.ChronicleRepository
import com.neon.ascent.data.local.JournalDao
import com.neon.ascent.data.mapper.toChronicleEntry
import com.neon.ascent.model.JournalEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalRepository @Inject constructor(
    private val journalDao: JournalDao,
    private val chronicleRepository: ChronicleRepository
) {
    val allEntries: Flow<List<JournalEntry>> = combine(
        chronicleRepository.observeChronicle(),
        journalDao.getAllEntries()
    ) { chronicleEntries, legacyEntries ->
        val originEntries = chronicleEntries.filter { it.room == "ORIGIN" }
        if (originEntries.isNotEmpty()) {
            originEntries.map { it.toJournalEntry() }
        } else {
            legacyEntries
        }
    }

    suspend fun saveToJournal(entry: JournalEntry) {
        chronicleRepository.saveEntry(entry.toChronicleEntry())
    }

    suspend fun removeFromJournal(entry: JournalEntry) {
        journalDao.deleteEntry(entry)
    }

    suspend fun toggleHeart(id: String, isHearted: Boolean) {
        chronicleRepository.setHearted(id, isHearted)
    }

    suspend fun isAlreadySaved(id: String): Boolean {
        val chronicleList = chronicleRepository.observeChronicle().first()
        val existsInChronicle = chronicleList.any { it.sourceId == id }
        return existsInChronicle || journalDao.exists(id)
    }
}

private fun ChronicleEntry.toJournalEntry(): JournalEntry {
    return JournalEntry(
        id = sourceId,
        text = content,
        category = "ORIGIN",
        timestamp = timestamp,
        isHearted = hearted
    )
}
