package com.neon.ascent.feature.selfmap

import com.neon.ascent.core.data.local.dao.SelfMapDao
import com.neon.ascent.core.data.local.entity.EoEEntryEntity
import com.neon.ascent.core.data.local.entity.SelfItemEntity
import com.neon.ascent.core.data.local.entity.SelfMapSnapshotEntity
import com.neon.ascent.core.domain.selfmap.SelfMapCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

class SelfMapEoETest {

    private lateinit var fakeDao: FakeSelfMapDao

    @Before
    fun setUp() {
        fakeDao = FakeSelfMapDao()
    }

    @Test
    fun testLogEoEReaction_AgainstExistingItem_PersistsAndAppearsInDetail() = runBlocking {
        // 1. Create an existing item: "Body"
        val bodyItem = SelfItemEntity(1, "Body", SelfMapCategory.BODY_SENSES, 0.9f)
        fakeDao.insertSelfItem(bodyItem)

        // 2. Log an EoE reaction against item ID 1
        val entry = EoEEntryEntity(
            id = 100L,
            timestamp = Instant.now(),
            emotionLabel = "Frustration",
            selfItemId = 1L,
            epText = "Hold Body at status quo or better",
            perceptionText = "Missed physical training session due to late meeting",
            appraisal = "Negative: Disrupted physical momentum",
            intensity = 7,
            canChangeNotes = "I can reframe meeting boundaries (change EP) or schedule evening recovery (change P)."
        )
        fakeDao.insertEoEEntry(entry)

        // 3. Query linked entries for item ID 1
        val linkedEntries = fakeDao.getEoEEntriesForSelfItemOnce(1L)

        // 4. Verify reaction log appears in item detail
        assertEquals(1, linkedEntries.size)
        val fetchedEntry = linkedEntries.first()
        assertEquals("Frustration", fetchedEntry.emotionLabel)
        assertEquals(1L, fetchedEntry.selfItemId)
        assertEquals("Hold Body at status quo or better", fetchedEntry.epText)
        assertEquals("Missed physical training session due to late meeting", fetchedEntry.perceptionText)
        assertEquals("Negative: Disrupted physical momentum", fetchedEntry.appraisal)
        assertEquals(7, fetchedEntry.intensity)
        assertNotNull(fetchedEntry.canChangeNotes)
    }

    private class FakeSelfMapDao : SelfMapDao {
        private val itemsMap = mutableMapOf<Long, SelfItemEntity>()
        private val eoeMap = mutableMapOf<Long, EoEEntryEntity>()

        override fun getAllActiveItems(): Flow<List<SelfItemEntity>> = flowOf(itemsMap.values.filter { !it.isArchived })
        override suspend fun getAllActiveItemsOnce(): List<SelfItemEntity> = itemsMap.values.filter { !it.isArchived }
        override suspend fun getAllItemsOnce(): List<SelfItemEntity> = itemsMap.values.toList()

        override fun getItemsByCategory(category: SelfMapCategory): Flow<List<SelfItemEntity>> {
            return flowOf(itemsMap.values.filter { !it.isArchived && it.category == category })
        }

        override fun getItemById(id: Long): Flow<SelfItemEntity?> = flowOf(itemsMap[id])
        override suspend fun getItemByIdOnce(id: Long): SelfItemEntity? = itemsMap[id]

        override fun getAverageImportance(): Flow<Float> = flowOf(0f)
        override fun getCoreCount(): Flow<Int> = flowOf(0)
        override fun getTotalItemCount(): Flow<Int> = flowOf(0)

        override suspend fun insertSelfItem(item: SelfItemEntity): Long {
            itemsMap[item.id] = item
            return item.id
        }

        override suspend fun insertSelfItems(items: List<SelfItemEntity>): List<Long> {
            return items.map { insertSelfItem(it) }
        }

        override suspend fun updateSelfItem(item: SelfItemEntity) {
            itemsMap[item.id] = item
        }

        override suspend fun archiveSelfItem(id: Long, updatedAt: Instant) {}
        override suspend fun unarchiveSelfItem(id: Long, updatedAt: Instant) {}
        override suspend fun deleteSelfItem(id: Long) { itemsMap.remove(id) }
        override suspend fun deleteAllSelfItems() { itemsMap.clear() }

        // EoE Entries
        override suspend fun insertEoEEntry(entry: EoEEntryEntity): Long {
            eoeMap[entry.id] = entry
            return entry.id
        }

        override fun getAllEoEEntries(): Flow<List<EoEEntryEntity>> = flowOf(eoeMap.values.toList())
        override suspend fun getAllEoEEntriesOnce(): List<EoEEntryEntity> = eoeMap.values.toList()

        override fun getEoEEntriesForSelfItem(selfItemId: Long): Flow<List<EoEEntryEntity>> {
            return flowOf(eoeMap.values.filter { it.selfItemId == selfItemId })
        }

        fun getEoEEntriesForSelfItemOnce(selfItemId: Long): List<EoEEntryEntity> {
            return eoeMap.values.filter { it.selfItemId == selfItemId }
        }

        override suspend fun deleteEoEEntry(id: Long) { eoeMap.remove(id) }
        override suspend fun deleteAllEoEEntries() { eoeMap.clear() }

        override suspend fun insertSnapshot(snapshot: SelfMapSnapshotEntity): Long = 1L
        override fun getAllSnapshots(): Flow<List<SelfMapSnapshotEntity>> = flowOf(emptyList())
        override suspend fun getAllSnapshotsOnce(): List<SelfMapSnapshotEntity> = emptyList()
        override fun getLatestSnapshot(): Flow<SelfMapSnapshotEntity?> = flowOf(null)
        override suspend fun deleteAllSnapshots() {}

        override suspend fun insertMoodEntry(entry: com.neon.ascent.core.data.local.entity.SelfMapMoodEntryEntity): Long = 1L
        override fun getAllMoodEntries(): Flow<List<com.neon.ascent.core.data.local.entity.SelfMapMoodEntryEntity>> = flowOf(emptyList())
        override suspend fun getAllMoodEntriesOnce(): List<com.neon.ascent.core.data.local.entity.SelfMapMoodEntryEntity> = emptyList()
        override fun getRecentMoodEntries(limit: Int): Flow<List<com.neon.ascent.core.data.local.entity.SelfMapMoodEntryEntity>> = flowOf(emptyList())
        override fun getLatestMoodEntry(): Flow<com.neon.ascent.core.data.local.entity.SelfMapMoodEntryEntity?> = flowOf(null)
        override suspend fun getLatestMoodEntryOnce(): com.neon.ascent.core.data.local.entity.SelfMapMoodEntryEntity? = null
        override suspend fun deleteMoodEntry(id: Long) {}
        override suspend fun deleteAllMoodEntries() {}
    }
}
