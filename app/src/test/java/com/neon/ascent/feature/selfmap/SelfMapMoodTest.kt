package com.neon.ascent.feature.selfmap

import com.neon.ascent.core.data.local.dao.SelfMapDao
import com.neon.ascent.core.data.local.entity.EoEEntryEntity
import com.neon.ascent.core.data.local.entity.SelfItemEntity
import com.neon.ascent.core.data.local.entity.SelfMapMoodEntryEntity
import com.neon.ascent.core.data.local.entity.SelfMapSnapshotEntity
import com.neon.ascent.core.domain.selfmap.SelfMapCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class SelfMapMoodTest {

    private lateinit var fakeDao: FakeSelfMapDao

    @Before
    fun setUp() {
        fakeDao = FakeSelfMapDao()
    }

    @Test
    fun testLogMoodEntry_SavesScoreAndNoteCorrectly() = runBlocking {
        val entry = SelfMapMoodEntryEntity(
            id = 1L,
            score = 8,
            note = "Feeling clear and centered after moving attachment outward."
        )
        fakeDao.insertMoodEntry(entry)

        val latest = fakeDao.getLatestMoodEntryOnce()
        assertNotNull(latest)
        assertEquals(8, latest?.score)
        assertEquals("Feeling clear and centered after moving attachment outward.", latest?.note)
    }

    @Test
    fun testMoodCheckIn_CanLinkToEoEEntry() = runBlocking {
        val eoeId = 42L
        val moodEntry = SelfMapMoodEntryEntity(
            id = 2L,
            score = 6,
            note = "Logged after EoE reaction",
            eoeId = eoeId
        )
        fakeDao.insertMoodEntry(moodEntry)

        val latest = fakeDao.getLatestMoodEntryOnce()
        assertNotNull(latest)
        assertEquals(42L, latest?.eoeId)
        assertEquals(6, latest?.score)
    }

    private class FakeSelfMapDao : SelfMapDao {
        private val itemsMap = mutableMapOf<Long, SelfItemEntity>()
        private val moodMap = mutableMapOf<Long, SelfMapMoodEntryEntity>()

        override fun getAllActiveItems(): Flow<List<SelfItemEntity>> = flowOf(itemsMap.values.filter { !it.isArchived })
        override suspend fun getAllActiveItemsOnce(): List<SelfItemEntity> = itemsMap.values.filter { !it.isArchived }
        override suspend fun getAllItemsOnce(): List<SelfItemEntity> = itemsMap.values.toList()

        override fun getItemsByCategory(category: SelfMapCategory): Flow<List<SelfItemEntity>> = flowOf(emptyList())
        override fun getItemById(id: Long): Flow<SelfItemEntity?> = flowOf(null)
        override suspend fun getItemByIdOnce(id: Long): SelfItemEntity? = null

        override fun getAverageImportance(): Flow<Float> = flowOf(0f)
        override fun getCoreCount(): Flow<Int> = flowOf(0)
        override fun getTotalItemCount(): Flow<Int> = flowOf(0)

        override suspend fun insertSelfItem(item: SelfItemEntity): Long = item.id
        override suspend fun insertSelfItems(items: List<SelfItemEntity>): List<Long> = emptyList()
        override suspend fun updateSelfItem(item: SelfItemEntity) {}
        override suspend fun archiveSelfItem(id: Long, updatedAt: java.time.Instant) {}
        override suspend fun unarchiveSelfItem(id: Long, updatedAt: java.time.Instant) {}
        override suspend fun deleteSelfItem(id: Long) {}
        override suspend fun deleteAllSelfItems() {}

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

        override suspend fun insertMoodEntry(entry: SelfMapMoodEntryEntity): Long {
            moodMap[entry.id] = entry
            return entry.id
        }

        override fun getAllMoodEntries(): Flow<List<SelfMapMoodEntryEntity>> = flowOf(moodMap.values.toList())
        override suspend fun getAllMoodEntriesOnce(): List<SelfMapMoodEntryEntity> = moodMap.values.toList()
        override fun getRecentMoodEntries(limit: Int): Flow<List<SelfMapMoodEntryEntity>> = flowOf(moodMap.values.take(limit))
        override fun getLatestMoodEntry(): Flow<SelfMapMoodEntryEntity?> = flowOf(moodMap.values.lastOrNull())
        override suspend fun getLatestMoodEntryOnce(): SelfMapMoodEntryEntity? = moodMap.values.lastOrNull()
        override suspend fun deleteMoodEntry(id: Long) { moodMap.remove(id) }
        override suspend fun deleteAllMoodEntries() { moodMap.clear() }
    }
}
