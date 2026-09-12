package com.neon.ascent.data.repository

import com.neon.ascent.core.domain.chronicle.ChronicleEntry
import com.neon.ascent.core.domain.chronicle.ChronicleRepository
import com.neon.ascent.data.local.JournalDao
import com.neon.ascent.model.JournalEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class JournalRepositoryTest {

    private class FakeJournalDao : JournalDao {
        private val entries = mutableListOf<JournalEntry>()
        private val flow = MutableStateFlow<List<JournalEntry>>(emptyList())

        private fun updateFlow() {
            flow.value = entries.toList()
        }

        override fun getAllEntries(): Flow<List<JournalEntry>> = flow

        override suspend fun insertEntry(entry: JournalEntry) {
            val idx = entries.indexOfFirst { it.id == entry.id }
            if (idx >= 0) entries[idx] = entry else entries.add(entry)
            updateFlow()
        }

        override suspend fun deleteEntry(entry: JournalEntry) {
            entries.removeAll { it.id == entry.id }
            updateFlow()
        }

        override suspend fun toggleHeart(id: String, isHearted: Boolean) {
            val idx = entries.indexOfFirst { it.id == id }
            if (idx >= 0) {
                entries[idx] = entries[idx].copy(isHearted = isHearted)
                updateFlow()
            }
        }

        override suspend fun exists(id: String): Boolean {
            return entries.any { it.id == id }
        }
    }

    private class FakeChronicleRepository : ChronicleRepository {
        private val entries = mutableListOf<ChronicleEntry>()
        private val flow = MutableStateFlow<List<ChronicleEntry>>(emptyList())

        private fun updateFlow() {
            flow.value = entries.toList()
        }

        override fun observeChronicle(): Flow<List<ChronicleEntry>> = flow

        override suspend fun saveEntry(entry: ChronicleEntry) {
            if (entry.content.isBlank()) return
            val idx = entries.indexOfFirst { it.source == entry.source && it.sourceId == entry.sourceId }
            if (idx >= 0) entries[idx] = entry else entries.add(entry)
            updateFlow()
        }

        override suspend fun setHearted(sourceId: String, hearted: Boolean) {
            val idx = entries.indexOfFirst { it.sourceId == sourceId }
            if (idx >= 0) {
                entries[idx] = entries[idx].copy(hearted = hearted)
                updateFlow()
            }
        }

        override suspend fun importIfAbsent(entry: ChronicleEntry): Boolean {
            if (entry.content.isBlank()) return false
            val exists = entries.any { it.source == entry.source && it.sourceId == entry.sourceId }
            if (exists) return false
            entries.add(entry)
            updateFlow()
            return true
        }
    }

    private lateinit var journalDao: FakeJournalDao
    private lateinit var chronicleRepository: FakeChronicleRepository
    private lateinit var repository: JournalRepository

    @Before
    fun setUp() {
        journalDao = FakeJournalDao()
        chronicleRepository = FakeChronicleRepository()
        repository = JournalRepository(journalDao, chronicleRepository)
    }

    @Test
    fun testSavingEntryWritesToChronicleOnlyAndAppearsInAllEntries() = runBlocking {
        val entry = JournalEntry(
            id = "entry_1",
            text = "Testing journal entry",
            category = "ORIGIN",
            timestamp = 1000L,
            isHearted = true
        )

        repository.saveToJournal(entry)

        // Verify journalDao was NOT written (writes to AppDatabase stopped)
        assertFalse(journalDao.exists("entry_1"))

        // Verify appears in ChronicleRepository
        val chronicleEntries = chronicleRepository.observeChronicle().first()
        assertEquals(1, chronicleEntries.size)
        assertEquals("entry_1", chronicleEntries[0].sourceId)
        assertEquals("Testing journal entry", chronicleEntries[0].content)

        // Verify observeChronicle filtered ORIGIN is returned by allEntries
        val allEntries = repository.allEntries.first()
        assertEquals(1, allEntries.size)
        assertEquals("entry_1", allEntries[0].id)
        assertEquals("Testing journal entry", allEntries[0].text)
        assertTrue(allEntries[0].isHearted)

        // Verify isAlreadySaved returns true
        assertTrue(repository.isAlreadySaved("entry_1"))
    }

    @Test
    fun testFallbackToJournalDaoWhenChronicleIsEmpty() = runBlocking {
        val legacyEntry = JournalEntry(
            id = "legacy_1",
            text = "Legacy entry content",
            category = "GENERAL",
            timestamp = 500L
        )
        journalDao.insertEntry(legacyEntry)

        val allEntries = repository.allEntries.first()
        assertEquals(1, allEntries.size)
        assertEquals("legacy_1", allEntries[0].id)
    }
}
