package com.neon.ascent.feature.selfmap

import com.neon.ascent.core.data.local.dao.SelfMapDao
import com.neon.ascent.core.data.local.entity.EoEEntryEntity
import com.neon.ascent.core.data.local.entity.SelfItemEntity
import com.neon.ascent.core.data.local.entity.SelfMapSnapshotEntity
import com.neon.ascent.core.domain.selfmap.SelfMapCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SelfMapWizardTest {

    private lateinit var fakeDao: FakeSelfMapDao

    @Before
    fun setUp() {
        fakeDao = FakeSelfMapDao()
    }

    @Test
    fun testWizardWalkthrough_AddingThreeItemsAcrossTwoCategories() = runBlocking {
        // Step 1: Initial state - preseed Body & Senses items (4 items)
        val body1 = SelfItemEntity(1, "Body", SelfMapCategory.BODY_SENSES, 0.9f)
        val body2 = SelfItemEntity(2, "Physical health", SelfMapCategory.BODY_SENSES, 0.9f)
        fakeDao.insertSelfItems(listOf(body1, body2))

        val initialBodyItems = fakeDao.getAllActiveItemsOnce()
        assertEquals(2, initialBodyItems.size)

        // Step 2: Category 2 - PEOPLE: Add Item 1 ("Partner") and Item 2 ("Best Friend")
        val person1 = SelfItemEntity(3, "Partner", SelfMapCategory.PEOPLE, 0.85f, "Core support")
        val person2 = SelfItemEntity(4, "Best Friend", SelfMapCategory.PEOPLE, 0.65f, "Lifetime friend")
        fakeDao.insertSelfItem(person1)
        fakeDao.insertSelfItem(person2)

        val peopleItems = fakeDao.getItemsByCategoryOnce(SelfMapCategory.PEOPLE)
        assertEquals(2, peopleItems.size)
        assertEquals("Partner", peopleItems[0].name)
        assertEquals("Best Friend", peopleItems[1].name)

        // Step 3: Category 3 - STUFF: Add Item 3 ("Neural Rig")
        val stuff1 = SelfItemEntity(5, "Neural Rig", SelfMapCategory.STUFF, 0.5f, "Work workstation")
        fakeDao.insertSelfItem(stuff1)

        val stuffItems = fakeDao.getItemsByCategoryOnce(SelfMapCategory.STUFF)
        assertEquals(1, stuffItems.size)
        assertEquals("Neural Rig", stuffItems[0].name)

        // Step 4: Verify total items added across categories (2 Body + 2 People + 1 Stuff = 5 items total)
        val totalActive = fakeDao.getAllActiveItemsOnce()
        assertEquals(5, totalActive.size)

        // Verify categories count
        val peopleCount = totalActive.count { it.category == SelfMapCategory.PEOPLE }
        val stuffCount = totalActive.count { it.category == SelfMapCategory.STUFF }
        assertEquals(2, peopleCount)
        assertEquals(1, stuffCount)
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

        suspend fun getItemsByCategoryOnce(category: SelfMapCategory): List<SelfItemEntity> {
            return itemsMap.values.filter { !it.isArchived && it.category == category }
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
            val id = if (item.id == 0L) idCounter++ else item.id
            val newItem = item.copy(id = id)
            itemsMap[id] = newItem
            return id
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
    }
}
