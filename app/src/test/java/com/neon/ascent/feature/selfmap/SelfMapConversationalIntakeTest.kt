package com.neon.ascent.feature.selfmap

import com.neon.ascent.core.data.local.dao.SelfMapDao
import com.neon.ascent.core.data.local.entity.EoEEntryEntity
import com.neon.ascent.core.data.local.entity.SelfItemEntity
import com.neon.ascent.core.data.local.entity.SelfMapMoodEntryEntity
import com.neon.ascent.core.data.local.entity.SelfMapSnapshotEntity
import com.neon.ascent.core.data.repository.SelfMapRepository
import com.neon.ascent.core.domain.ai.AiCore
import com.neon.ascent.core.domain.ai.AiResult
import com.neon.ascent.core.domain.selfmap.SelfMapCategory
import com.neon.ascent.feature.dashboard.selfmap.SelfMapConversationalIntakeUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SelfMapConversationalIntakeTest {

    private lateinit var fakeDao: FakeSelfMapDao
    private lateinit var fakeAiCore: FakeAiCore
    private lateinit var intakeUseCase: SelfMapConversationalIntakeUseCase

    @Before
    fun setUp() {
        fakeDao = FakeSelfMapDao()
        fakeAiCore = FakeAiCore()
        intakeUseCase = SelfMapConversationalIntakeUseCase(fakeAiCore)
    }

    @Test
    fun testConversationalIntakeWalkthrough_AcceptThreeItemsAcrossSteps() = runBlocking {
        // Phase 1: Core Body & Senses
        val (p1Msg, p1Proposals) = intakeUseCase.processUserTurn(0, "I am focused on my energy and physical fitness.")
        assertTrue(p1Proposals.isNotEmpty())
        val acceptedItem1 = p1Proposals.first()
        val id1 = fakeDao.insertSelfItem(
            SelfItemEntity(
                name = acceptedItem1.name,
                category = acceptedItem1.category,
                importance = acceptedItem1.suggestedImportance,
                notes = acceptedItem1.note
            )
        )
        assertTrue(id1 > 0)

        // Phase 2: People & Connections
        val (p2Msg, p2Proposals) = intakeUseCase.processUserTurn(1, "My partner and family mean everything to me.")
        assertTrue(p2Proposals.isNotEmpty())
        val acceptedItem2 = p2Proposals.first()
        val id2 = fakeDao.insertSelfItem(
            SelfItemEntity(
                name = acceptedItem2.name,
                category = acceptedItem2.category,
                importance = acceptedItem2.suggestedImportance,
                notes = acceptedItem2.note
            )
        )
        assertTrue(id2 > 0)

        // Phase 3: Stuff & Roles
        val (p3Msg, p3Proposals) = intakeUseCase.processUserTurn(2, "My career as Lead Cyber-Architect is my primary focus.")
        assertTrue(p3Proposals.isNotEmpty())
        val acceptedItem3 = p3Proposals.first()
        val id3 = fakeDao.insertSelfItem(
            SelfItemEntity(
                name = acceptedItem3.name,
                category = acceptedItem3.category,
                importance = acceptedItem3.suggestedImportance,
                notes = acceptedItem3.note
            )
        )
        assertTrue(id3 > 0)

        // Verify that all 3 accepted items appear on canvas (active items in DAO/Repository)
        val activeCanvasItems = fakeDao.getAllActiveItemsOnce()
        assertEquals(3, activeCanvasItems.size)

        val categoriesPresent = activeCanvasItems.map { it.category }.toSet()
        assertTrue(categoriesPresent.contains(acceptedItem1.category))
        assertTrue(categoriesPresent.contains(acceptedItem2.category))
        assertTrue(categoriesPresent.contains(acceptedItem3.category))
    }

    @Test
    fun testSuggestItemForCategory_AskMentorSuggestion() = runBlocking {
        val suggestion = intakeUseCase.suggestItemForCategory(SelfMapCategory.PEOPLE)
        assertNotNull(suggestion)
        assertTrue(suggestion.name.isNotBlank())
        assertTrue(suggestion.suggestedImportance in 0.40f..0.90f)
    }

    private class FakeAiCore : AiCore {
        override suspend fun isReady(): Boolean = true
        override suspend fun warmup() {}
        override suspend fun generate(prompt: String, forceLocal: Boolean): AiResult {
            return AiResult.Success(
                "I've mapped your input to your core identity.\n" +
                "[PROPOSAL: Custom Node | CUSTOM | 0.8 | Guided intake node]"
            )
        }
        override suspend fun generateContent(prompt: String, forceLocal: Boolean): String = "Generated"
    }

    private class FakeSelfMapDao : SelfMapDao {
        private val itemsMap = mutableMapOf<Long, SelfItemEntity>()
        private var idCounter = 1L

        override fun getAllActiveItems(): Flow<List<SelfItemEntity>> = flowOf(itemsMap.values.filter { !it.isArchived })
        override suspend fun getAllActiveItemsOnce(): List<SelfItemEntity> = itemsMap.values.filter { !it.isArchived }
        override suspend fun getAllItemsOnce(): List<SelfItemEntity> = itemsMap.values.toList()

        override fun getItemsByCategory(category: SelfMapCategory): Flow<List<SelfItemEntity>> {
            return flowOf(itemsMap.values.filter { !it.isArchived && it.category == category })
        }

        override fun getItemById(id: Long): Flow<SelfItemEntity?> = flowOf(itemsMap[id])
        override suspend fun getItemByIdOnce(id: Long): SelfItemEntity? = itemsMap[id]

        override fun getAverageImportance(): Flow<Float> = flowOf(0.8f)
        override fun getCoreCount(): Flow<Int> = flowOf(itemsMap.size)
        override fun getTotalItemCount(): Flow<Int> = flowOf(itemsMap.size)

        override suspend fun insertSelfItem(item: SelfItemEntity): Long {
            val id = if (item.id == 0L) idCounter++ else item.id
            val newItem = item.copy(id = id)
            itemsMap[id] = newItem
            return id
        }

        override suspend fun insertSelfItems(items: List<SelfItemEntity>): List<Long> {
            return items.map { insertSelfItem(it) }
        }

        override suspend fun updateSelfItem(item: SelfItemEntity) { itemsMap[item.id] = item }
        override suspend fun archiveSelfItem(id: Long, updatedAt: java.time.Instant) { itemsMap[id]?.let { itemsMap[id] = it.copy(isArchived = true) } }
        override suspend fun unarchiveSelfItem(id: Long, updatedAt: java.time.Instant) { itemsMap[id]?.let { itemsMap[id] = it.copy(isArchived = false) } }
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
