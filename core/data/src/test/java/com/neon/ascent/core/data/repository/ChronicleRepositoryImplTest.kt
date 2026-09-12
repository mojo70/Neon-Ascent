package com.neon.ascent.core.data.repository

import com.google.gson.Gson
import com.neon.ascent.core.data.local.dao.NeuralMemoryDao
import com.neon.ascent.core.data.local.entity.NeuralMemory
import com.neon.ascent.core.domain.chronicle.ChronicleEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ChronicleRepositoryImplTest {

    private class FakeNeuralMemoryDao : NeuralMemoryDao {
        private val memories = mutableListOf<NeuralMemory>()
        private val flow = MutableStateFlow<List<NeuralMemory>>(emptyList())
        private var autoId = 1L

        private fun updateFlow() {
            flow.value = memories.toList().sortedByDescending { it.timestamp }
        }

        override suspend fun insertMemory(memory: NeuralMemory) {
            val idx = memories.indexOfFirst { it.id != 0L && it.id == memory.id }
            if (idx >= 0) {
                memories[idx] = memory
            } else {
                val assignedId = if (memory.id == 0L) autoId++ else memory.id
                memories.add(memory.copy(id = assignedId))
            }
            updateFlow()
        }

        override fun getAllMemories(): Flow<List<NeuralMemory>> = flow

        override fun getMemoriesByWing(wing: String): Flow<List<NeuralMemory>> {
            return flow.map { list -> list.filter { it.wing == wing } }
        }

        override fun getMemoriesByRoom(wing: String, room: String): Flow<List<NeuralMemory>> {
            return flow.map { list -> list.filter { it.wing == wing && it.room == room } }
        }

        override suspend fun searchMemories(query: String, limit: Int): List<NeuralMemory> {
            return memories.filter { it.content.contains(query, ignoreCase = true) }.take(limit)
        }

        override suspend fun pruneOldMemories(threshold: Long) {
            memories.removeAll { it.timestamp < threshold }
            updateFlow()
        }
    }

    private lateinit var fakeDao: FakeNeuralMemoryDao
    private lateinit var repository: ChronicleRepositoryImpl

    @Before
    fun setUp() {
        fakeDao = FakeNeuralMemoryDao()
        repository = ChronicleRepositoryImpl(fakeDao)
    }

    // Test 1: Same source+sourceId twice -> one row.
    @Test
    fun testSameSourceAndSourceIdTwiceResultInOneRow() = runBlocking {
        val entry1 = ChronicleEntry(
            source = "journal",
            sourceId = "journal_101",
            wing = "CHRONICLE",
            room = "ORIGIN",
            content = "First entry content"
        )
        val entry2 = ChronicleEntry(
            source = "journal",
            sourceId = "journal_101",
            wing = "CHRONICLE",
            room = "ORIGIN",
            content = "First entry content duplicate"
        )

        val insertedFirst = repository.importIfAbsent(entry1)
        val insertedSecond = repository.importIfAbsent(entry2)

        assertTrue(insertedFirst)
        assertFalse(insertedSecond)

        val items = repository.observeChronicle().first()
        assertEquals(1, items.size)
        assertEquals("journal_101", items[0].sourceId)
    }

    // Test 2: Hearted journal -> metadata.hearted true, importance >= 0.8.
    @Test
    fun testHeartedJournalSetsMetadataAndImportance() = runBlocking {
        val entry = ChronicleEntry(
            source = "journal",
            sourceId = "journal_202",
            wing = "CHRONICLE",
            room = "ORIGIN",
            content = "Deep thought about the neon city",
            hearted = true
        )

        repository.saveEntry(entry)

        val rawMemories = fakeDao.getMemoriesByWing("CHRONICLE").first()
        assertEquals(1, rawMemories.size)
        val memory = rawMemories[0]

        assertTrue("Importance should be >= 0.8f", memory.importance >= 0.8f)
        assertNotNull(memory.metadata)
        assertTrue(memory.metadata!!.contains("\"hearted\":true"))

        val domainEntries = repository.observeChronicle().first()
        assertEquals(1, domainEntries.size)
        assertTrue(domainEntries[0].hearted)
    }

    // Test 3: Empty content -> skip insert.
    @Test
    fun testEmptyContentSkipsInsert() = runBlocking {
        val emptyEntry = ChronicleEntry(
            source = "journal",
            sourceId = "journal_303",
            wing = "CHRONICLE",
            room = "ORIGIN",
            content = "   "
        )

        val imported = repository.importIfAbsent(emptyEntry)
        assertFalse(imported)

        repository.saveEntry(emptyEntry)

        val rawMemories = fakeDao.getAllMemories().first()
        assertTrue(rawMemories.isEmpty())
    }
}
