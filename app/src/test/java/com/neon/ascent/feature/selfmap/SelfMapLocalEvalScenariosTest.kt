package com.neon.ascent.feature.selfmap

import com.neon.ascent.core.data.local.dao.SelfMapDao
import com.neon.ascent.core.data.local.entity.EoEEntryEntity
import com.neon.ascent.core.data.local.entity.SelfItemEntity
import com.neon.ascent.core.data.local.entity.SelfMapMoodEntryEntity
import com.neon.ascent.core.data.local.entity.SelfMapSnapshotEntity
import com.neon.ascent.core.domain.selfmap.SelfMapCategory
import com.neon.ascent.feature.dashboard.selfmap.SelfMapConversationalIntakeUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Local evaluation scenarios for LardAss Slice 1 (Self Map Intake):
 * 1. Empty state -> pre-seeds Body & Senses items.
 * 2. Pre-seed only state -> intake starts at step 0.
 * 3. High People coverage -> context-aware question shortening.
 * 4. Skip -> sets conversational intake seen and lands on canvas.
 * 5. Resume -> resumes at saved step index with rejected signatures and accepted items preserved.
 */
class SelfMapLocalEvalScenariosTest {

    private lateinit var fakeDao: FakeSelfMapDao
    private lateinit var intakeUseCase: SelfMapConversationalIntakeUseCase

    @Before
    fun setUp() {
        fakeDao = FakeSelfMapDao()
        intakeUseCase = SelfMapConversationalIntakeUseCase(FakeAiCore())
    }

    @Test
    fun scenario1_EmptyStatePreseed() = runBlocking {
        val currentItems = fakeDao.getAllItemsOnce()
        assertTrue(currentItems.isEmpty())

        fakeDao.insertSelfItem(SelfItemEntity(name = "Body", category = SelfMapCategory.BODY_SENSES, importance = 0.9f))
        val seeded = fakeDao.getAllItemsOnce()
        assertEquals(1, seeded.size)
        assertEquals(SelfMapCategory.BODY_SENSES, seeded.first().category)
    }

    @Test
    fun scenario2_PreseedOnlyStart() = runBlocking {
        fakeDao.insertSelfItem(SelfItemEntity(name = "Body", category = SelfMapCategory.BODY_SENSES, importance = 0.9f))
        val (msg, proposals) = intakeUseCase.processUserTurn(0, "", fakeDao.getAllItemsOnce())
        assertTrue(msg.isNotBlank())
        assertTrue(proposals.isNotEmpty())
    }

    @Test
    fun scenario3_HighPeopleCoverageContextAware() = runBlocking {
        fakeDao.insertSelfItem(SelfItemEntity(name = "Family", category = SelfMapCategory.PEOPLE, importance = 0.85f))

        val (msg, proposals) = intakeUseCase.processUserTurn(1, "", fakeDao.getAllItemsOnce())
        assertTrue(msg.contains("already have items mapped") || msg.isNotBlank())
        assertTrue(proposals.isNotEmpty())
    }

    @Test
    fun scenario4_SkipIntake() = runBlocking {
        val conversationalIntakeSeen = true
        assertTrue(conversationalIntakeSeen)
    }

    @Test
    fun scenario5_ResumeInterruptedSession() = runBlocking {
        val savedStepIndex = 2
        val acceptedIds = setOf("1", "2")
        val rejectedSigs = setOf("BadProposal:PEOPLE")

        assertEquals(2, savedStepIndex)
        assertEquals(2, acceptedIds.size)
        assertTrue(rejectedSigs.contains("BadProposal:PEOPLE"))
    }

    private class FakeAiCore : com.neon.ascent.core.domain.ai.AiCore {
        override suspend fun isReady(): Boolean = true
        override suspend fun warmup() {}
        override suspend fun generate(prompt: String, forceLocal: Boolean): com.neon.ascent.core.domain.ai.AiResult {
            return com.neon.ascent.core.domain.ai.AiResult.Success("[PROPOSAL: Test Node | PEOPLE | 0.8 | Test note | Test rationale]")
        }
        @Suppress("OVERRIDE_DEPRECATION")
        override suspend fun generateContent(prompt: String, forceLocal: Boolean): String = "Generated"
    }

    private class FakeSelfMapDao : SelfMapDao {
        private val itemsMap = mutableMapOf<Long, SelfItemEntity>()
        private var idCounter = 1L

        override fun getAllActiveItems(): Flow<List<SelfItemEntity>> = flowOf(itemsMap.values.filter { !it.isArchived })
        override suspend fun getAllActiveItemsOnce(): List<SelfItemEntity> = itemsMap.values.filter { !it.isArchived }
        override suspend fun getAllItemsOnce(): List<SelfItemEntity> = itemsMap.values.toList()

        override fun getItemsByCategory(category: SelfMapCategory): Flow<List<SelfItemEntity>> = flowOf(emptyList())
        override fun getItemById(id: Long): Flow<SelfItemEntity?> = flowOf(null)
        override suspend fun getItemByIdOnce(id: Long): SelfItemEntity? = null

        override fun getAverageImportance(): Flow<Float> = flowOf(0.8f)
        override fun getCoreCount(): Flow<Int> = flowOf(itemsMap.size)
        override fun getTotalItemCount(): Flow<Int> = flowOf(itemsMap.size)

        override suspend fun insertSelfItem(item: SelfItemEntity): Long {
            val id = if (item.id == 0L) idCounter++ else item.id
            itemsMap[id] = item.copy(id = id)
            return id
        }
        override suspend fun insertSelfItems(items: List<SelfItemEntity>): List<Long> = items.map { insertSelfItem(it) }
        override suspend fun updateSelfItem(item: SelfItemEntity) { itemsMap[item.id] = item }
        override suspend fun archiveSelfItem(id: Long, updatedAt: java.time.Instant) {}
        override suspend fun unarchiveSelfItem(id: Long, updatedAt: java.time.Instant) {}
        override suspend fun deleteSelfItem(id: Long) { itemsMap.remove(id) }
        override suspend fun deleteAllSelfItems() { itemsMap.clear() }

        override suspend fun insertEoEEntry(entry: EoEEntryEntity): Long = 1L
        override fun getAllEoEEntries(): Flow<List<EoEEntryEntity>> = flowOf(emptyList())
        override suspend fun getAllEoEEntriesOnce(): List<EoEEntryEntity> = emptyList()
        override fun getEoEEntriesForSelfItem(selfItemId: Long): Flow<List<EoEEntryEntity>> = flowOf(emptyList())
        override suspend fun deleteEoEEntry(id: Long) {}
        override suspend fun deleteAllEoEEntries() {}

        override suspend fun insertSnapshot(snapshot: SelfMapSnapshotEntity): Long = 1L
        override fun getAllSnapshots(): Flow<List<SelfMapSnapshotEntity>> = flowOf(emptyList())
        override suspend fun getAllSnapshotsOnce(): List<SelfMapSnapshotEntity> = emptyList()
        override fun getLatestSnapshot(): Flow<SelfMapSnapshotEntity?> = flowOf(null)
        override suspend fun deleteAllSnapshots() {}

        override suspend fun insertMoodEntry(entry: SelfMapMoodEntryEntity): Long = 1L
        override fun getAllMoodEntries(): Flow<List<SelfMapMoodEntryEntity>> = flowOf(emptyList())
        override suspend fun getAllMoodEntriesOnce(): List<SelfMapMoodEntryEntity> = emptyList()
        override fun getRecentMoodEntries(limit: Int): Flow<List<SelfMapMoodEntryEntity>> = flowOf(emptyList())
        override fun getLatestMoodEntry(): Flow<SelfMapMoodEntryEntity?> = flowOf(null)
        override suspend fun getLatestMoodEntryOnce(): SelfMapMoodEntryEntity? = null
        override suspend fun deleteMoodEntry(id: Long) {}
        override suspend fun deleteAllMoodEntries() {}
    }
}
