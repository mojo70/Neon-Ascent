package com.neon.ascent.data.chronicle

import com.neon.ascent.core.domain.chronicle.ChronicleEntry
import com.neon.ascent.core.domain.chronicle.ChronicleRepository
import com.neon.ascent.data.local.ChatDao
import com.neon.ascent.data.local.JournalDao
import com.neon.ascent.data.local.LoreDao
import com.neon.ascent.model.ChatMessage
import com.neon.ascent.model.ChatSession
import com.neon.ascent.model.CorpoTrust
import com.neon.ascent.model.DataShard
import com.neon.ascent.model.JournalEntry
import com.neon.ascent.model.MemoryFragment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ChronicleImportUseCaseTest {

    private class FakeJournalDao : JournalDao {
        val entries = mutableListOf<JournalEntry>()
        private val flow = MutableStateFlow<List<JournalEntry>>(emptyList())
        fun update() { flow.value = entries.toList() }
        override fun getAllEntries(): Flow<List<JournalEntry>> = flow
        override suspend fun insertEntry(entry: JournalEntry) { entries.add(entry); update() }
        override suspend fun deleteEntry(entry: JournalEntry) { entries.remove(entry); update() }
        override suspend fun toggleHeart(id: String, isHearted: Boolean) {}
        override suspend fun exists(id: String): Boolean = entries.any { it.id == id }
    }

    private class FakeLoreDao : LoreDao {
        val shards = mutableListOf<DataShard>()
        val fragments = mutableListOf<MemoryFragment>()
        val trusts = mutableListOf<CorpoTrust>()

        private val shardFlow = MutableStateFlow<List<DataShard>>(emptyList())
        private val fragmentFlow = MutableStateFlow<List<MemoryFragment>>(emptyList())
        private val trustFlow = MutableStateFlow<List<CorpoTrust>>(emptyList())

        fun update() {
            shardFlow.value = shards.toList()
            fragmentFlow.value = fragments.toList()
            trustFlow.value = trusts.toList()
        }

        override fun getAllCorpoTrust(): Flow<List<CorpoTrust>> = trustFlow
        override fun getCorpoTrust(corpoId: String): Flow<CorpoTrust?> = MutableStateFlow(trusts.firstOrNull { it.corpoId == corpoId })
        override suspend fun insertCorpoTrust(trust: CorpoTrust) { trusts.add(trust); update() }
        override suspend fun updateTrustLevel(corpoId: String, level: Float) {}
        override fun getAllDataShards(): Flow<List<DataShard>> = shardFlow
        override suspend fun insertDataShard(shard: DataShard) { shards.add(shard); update() }
        override suspend fun updateDataShard(shard: DataShard) {}
        override suspend fun updateShardDecrypted(shardId: String, isDecrypted: Boolean) {}
        override fun getAllMemoryFragments(): Flow<List<MemoryFragment>> = fragmentFlow
        override suspend fun insertMemoryFragment(fragment: MemoryFragment) { fragments.add(fragment); update() }
        override suspend fun updateMemoryFragment(fragment: MemoryFragment) {}
    }

    private class FakeChatDao : ChatDao {
        val sessions = mutableListOf<ChatSession>()
        val messages = mutableListOf<ChatMessage>()

        private val sessionFlow = MutableStateFlow<List<ChatSession>>(emptyList())

        fun update() { sessionFlow.value = sessions.toList() }

        override fun getChatSessions(): Flow<List<ChatSession>> = sessionFlow
        override suspend fun insertChatSession(session: ChatSession) { sessions.add(session); update() }
        override suspend fun updateChatSession(session: ChatSession) {}
        override fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>> {
            return MutableStateFlow(messages.filter { it.sessionId == sessionId })
        }
        override fun getMessagesForContact(contactName: String): Flow<List<ChatMessage>> {
            return MutableStateFlow(messages.filter { it.contactName == contactName })
        }
        override fun getSessionsForContact(contactName: String): Flow<List<ChatSession>> {
            return MutableStateFlow(sessions.filter { it.contactName == contactName })
        }
        override suspend fun getLatestSessionForContact(contactName: String): ChatSession? = sessions.firstOrNull { it.contactName == contactName }
        override suspend fun getSessionById(sessionId: String): ChatSession? = sessions.firstOrNull { it.sessionId == sessionId }
        override suspend fun insertMessage(message: ChatMessage) { messages.add(message); update() }
        override suspend fun markAsRead(sessionId: String) {}
        override suspend fun markAllAsReadForContact(contactName: String) {}
    }

    private class FakeChronicleRepository : ChronicleRepository {
        val importedList = mutableListOf<ChronicleEntry>()
        private val flow = MutableStateFlow<List<ChronicleEntry>>(emptyList())

        override fun observeChronicle(): Flow<List<ChronicleEntry>> = flow
        override suspend fun saveEntry(entry: ChronicleEntry) {
            importedList.add(entry)
            flow.value = importedList.toList()
        }
        override suspend fun setHearted(sourceId: String, hearted: Boolean) {}
        override suspend fun importIfAbsent(entry: ChronicleEntry): Boolean {
            if (entry.content.isBlank()) return false
            val exists = importedList.any { it.source == entry.source && it.sourceId == entry.sourceId }
            if (exists) return false
            importedList.add(entry)
            flow.value = importedList.toList()
            return true
        }
    }

    private lateinit var journalDao: FakeJournalDao
    private lateinit var loreDao: FakeLoreDao
    private lateinit var chatDao: FakeChatDao
    private lateinit var chronicleRepository: FakeChronicleRepository
    private lateinit var useCase: ChronicleImportUseCase

    @Before
    fun setUp() {
        journalDao = FakeJournalDao()
        loreDao = FakeLoreDao()
        chatDao = FakeChatDao()
        chronicleRepository = FakeChronicleRepository()
        useCase = ChronicleImportUseCase(journalDao, loreDao, chatDao, chronicleRepository)
    }

    @Test
    fun testImportJobCopiesDataToChronicleAndIsIdempotent() = runBlocking {
        // Setup initial data
        journalDao.insertEntry(JournalEntry("j1", "Journal Entry 1", "ORIGIN", 1000L))
        loreDao.insertDataShard(DataShard("s1", "Shard Title", "Shard Content", true, 5000, 2000L))
        loreDao.insertMemoryFragment(MemoryFragment("f1", "Frag Title", "Corrupted", "Decrypted", "PERCEPTION", 5, true))
        loreDao.insertCorpoTrust(CorpoTrust("corpo_arasaka", 0.9f))

        chatDao.insertChatSession(ChatSession("sess_1", "ROGUE", "Last msg", 3000L))
        chatDao.insertMessage(ChatMessage(1L, "sess_1", "ROGUE", "ROGUE", "Hey runner", 3000L, false))

        // First run
        val result1 = useCase.runImport()

        assertEquals(1, result1.importedJournal)
        assertEquals(1, result1.importedShard)
        assertEquals(1, result1.importedFragment)
        assertEquals(1, result1.importedChat)

        // Verify mappings
        val items = chronicleRepository.observeChronicle().first()
        assertEquals(4, items.size)

        val journalItem = items.first { it.source == "journal" }
        assertEquals("CHRONICLE", journalItem.wing)
        assertEquals("ORIGIN", journalItem.room)

        val shardItem = items.first { it.source == "shard" }
        assertEquals("CHRONICLE", shardItem.wing)
        assertEquals("SHARD", shardItem.room)
        assertTrue(shardItem.content.contains("Shard Title"))

        val fragItem = items.first { it.source == "fragment" }
        assertEquals("CHRONICLE", fragItem.wing)
        assertEquals("FRAGMENT", fragItem.room)

        val chatItem = items.first { it.source == "chat" }
        assertEquals("DIALOGUE", chatItem.wing)
        assertEquals("ROGUE", chatItem.room)

        // Verify corpo_trust was NOT imported
        assertFalse(items.any { it.sourceId == "corpo_arasaka" })

        // Second run (Idempotency check)
        val result2 = useCase.runImport()
        assertEquals(0, result2.importedJournal)
        assertEquals(0, result2.importedShard)
        assertEquals(0, result2.importedFragment)
        assertEquals(0, result2.importedChat)
    }
}
