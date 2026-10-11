package com.neon.ascent.feature.selfmap

import com.neon.ascent.core.domain.chronicle.ChronicleEntry
import com.neon.ascent.core.domain.chronicle.ChronicleRepository
import com.neon.ascent.data.mapper.toChronicleEntry
import com.neon.ascent.model.JournalEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.util.UUID

class SelfMapChronicleTaggingTest {

    private lateinit var fakeChronicleRepository: FakeChronicleRepository

    @Before
    fun setUp() {
        fakeChronicleRepository = FakeChronicleRepository()
    }

    @Test
    fun testChronicleEntry_WithLinkedSelfItemId_SavesAndRetrievesCorrectly() = runBlocking {
        val targetSelfItemId = 42L
        val entry = ChronicleEntry(
            source = "journal",
            sourceId = UUID.randomUUID().toString(),
            content = "Recognized strong expectation attachment regarding career identity.",
            selfItemId = targetSelfItemId
        )

        fakeChronicleRepository.saveEntry(entry)

        val list = fakeChronicleRepository.observeChronicle().first()
        assertEquals(1, list.size)
        val saved = list.first()
        assertEquals("Recognized strong expectation attachment regarding career identity.", saved.content)
        assertEquals(targetSelfItemId, saved.selfItemId)
    }

    @Test
    fun testJournalEntryMapping_PreservesSelfItemId() {
        val targetSelfItemId = 99L
        val journalEntry = JournalEntry(
            id = "j100",
            text = "Reflecting on relationship expectations.",
            category = "ORIGIN",
            selfItemId = targetSelfItemId
        )

        val chronicleEntry = journalEntry.toChronicleEntry()
        assertEquals("j100", chronicleEntry.sourceId)
        assertEquals("journal", chronicleEntry.source)
        assertEquals(targetSelfItemId, chronicleEntry.selfItemId)
    }

    private class FakeChronicleRepository : ChronicleRepository {
        private val entriesFlow = MutableStateFlow<List<ChronicleEntry>>(emptyList())

        override fun observeChronicle(): Flow<List<ChronicleEntry>> = entriesFlow

        override suspend fun saveEntry(entry: ChronicleEntry) {
            val current = entriesFlow.value.toMutableList()
            current.add(entry)
            entriesFlow.value = current
        }

        override suspend fun setHearted(sourceId: String, hearted: Boolean) {}
        override suspend fun importIfAbsent(entry: ChronicleEntry): Boolean {
            saveEntry(entry)
            return true
        }
    }
}
