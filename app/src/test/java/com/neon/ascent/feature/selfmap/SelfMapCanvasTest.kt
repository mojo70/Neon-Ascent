package com.neon.ascent.feature.selfmap

import com.neon.ascent.core.data.local.dao.SelfMapDao
import com.neon.ascent.core.data.local.dao.calculateWeightedRadialMass
import com.neon.ascent.core.data.local.entity.EoEEntryEntity
import com.neon.ascent.core.data.local.entity.SelfItemEntity
import com.neon.ascent.core.data.local.entity.SelfMapSnapshotEntity
import com.neon.ascent.core.domain.selfmap.SelfMapCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.math.pow

class SelfMapCanvasTest {

    private lateinit var fakeDao: FakeSelfMapDao

    @Before
    fun setUp() {
        fakeDao = FakeSelfMapDao()
    }

    @Test
    fun testDragNodeUpdatesImportance_PersistsAndRecalculatesMass() = runBlocking {
        // 1. Initial State: Seed two items
        val item1 = SelfItemEntity(1, "Body", SelfMapCategory.BODY_SENSES, 0.9f)
        val item2 = SelfItemEntity(2, "Appearance", SelfMapCategory.BODY_SENSES, 0.8f)
        fakeDao.insertSelfItems(listOf(item1, item2))

        val initialItems = fakeDao.getAllActiveItemsOnce()
        val initialMass = initialItems.calculateWeightedRadialMass()

        // Mass = (0.9 ^ 1.5) + (0.8 ^ 1.5) = 0.8538 + 0.7155 ≈ 1.5693
        val expectedInitialMass = (0.9.pow(1.5) + 0.8.pow(1.5)).toFloat()
        assertEquals(expectedInitialMass, initialMass, 0.001f)

        // 2. Drag action: User drags "Appearance" node outward from 0.8 to 0.3 importance
        val updatedItem2 = item2.copy(importance = 0.3f)
        fakeDao.updateSelfItem(updatedItem2)

        // 3. Verify persistence
        val persistedItem2 = fakeDao.getItemByIdOnce(2L)
        assertEquals(0.3f, persistedItem2?.importance)

        // 4. Recalculate mass after drag
        val updatedItems = fakeDao.getAllActiveItemsOnce()
        val updatedMass = updatedItems.calculateWeightedRadialMass()

        // Mass = (0.9 ^ 1.5) + (0.3 ^ 1.5) = 0.8538 + 0.1643 ≈ 1.0181
        val expectedUpdatedMass = (0.9.pow(1.5) + 0.3.pow(1.5)).toFloat()
        assertEquals(expectedUpdatedMass, updatedMass, 0.001f)

        // Verify mass dropped significantly after moving attachment outward
        assertEquals(true, updatedMass < initialMass)
    }

    private class FakeSelfMapDao : SelfMapDao {
        private val itemsMap = mutableMapOf<Long, SelfItemEntity>()

        override fun getAllActiveItems(): Flow<List<SelfItemEntity>> = flowOf(itemsMap.values.filter { !it.isArchived })
        override suspend fun getAllActiveItemsOnce(): List<SelfItemEntity> = itemsMap.values.filter { !it.isArchived }
        override suspend fun getAllItemsOnce(): List<SelfItemEntity> = itemsMap.values.toList()

        override fun getItemsByCategory(category: SelfMapCategory): Flow<List<SelfItemEntity>> {
            return flowOf(itemsMap.values.filter { !it.isArchived && it.category == category })
        }

        override fun getItemById(id: Long): Flow<SelfItemEntity?> = flowOf(itemsMap[id])
        override suspend fun getItemByIdOnce(id: Long): SelfItemEntity? = itemsMap[id]

        override fun getAverageImportance(): Flow<Float> {
            val active = itemsMap.values.filter { !it.isArchived }
            val avg = if (active.isNotEmpty()) active.map { it.importance }.average().toFloat() else 0f
            return flowOf(avg)
        }

        override fun getCoreCount(): Flow<Int> {
            return flowOf(itemsMap.values.count { !it.isArchived && it.importance >= 0.7f })
        }

        override fun getTotalItemCount(): Flow<Int> = flowOf(itemsMap.values.count { !it.isArchived })

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

        override suspend fun archiveSelfItem(id: Long, updatedAt: java.time.Instant) {
            itemsMap[id]?.let { itemsMap[id] = it.copy(isArchived = true) }
        }

        override suspend fun unarchiveSelfItem(id: Long, updatedAt: java.time.Instant) {
            itemsMap[id]?.let { itemsMap[id] = it.copy(isArchived = false) }
        }

        override suspend fun deleteSelfItem(id: Long) {
            itemsMap.remove(id)
        }

        override suspend fun deleteAllSelfItems() {
            itemsMap.clear()
        }

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
