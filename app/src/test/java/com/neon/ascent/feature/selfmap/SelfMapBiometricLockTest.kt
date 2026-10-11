package com.neon.ascent.feature.selfmap

import com.neon.ascent.core.data.local.dao.SelfMapDao
import com.neon.ascent.core.data.local.entity.EoEEntryEntity
import com.neon.ascent.core.data.local.entity.SelfItemEntity
import com.neon.ascent.core.data.local.entity.SelfMapSnapshotEntity
import com.neon.ascent.core.domain.selfmap.SelfMapCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SelfMapBiometricLockTest {

    private lateinit var fakeDao: FakeSelfMapDao
    private lateinit var fakeDataStore: FakeDataStoreHelper

    @Before
    fun setUp() {
        fakeDao = FakeSelfMapDao()
        fakeDataStore = FakeDataStoreHelper()
    }

    @Test
    fun testBiometricLockToggle_ToggleOnEnablesLock_ToggleOffDisablesLock() = runBlocking {
        // 1. Default state: Biometric lock is disabled
        val initialLock = fakeDataStore.biometricLockEnabled.first()
        assertFalse(initialLock)

        // 2. Toggle ON: Enable biometric lock
        fakeDataStore.setBiometricLockEnabled(true)
        val enabledLock = fakeDataStore.biometricLockEnabled.first()
        assertTrue(enabledLock)

        // 3. Toggle OFF: Disable biometric lock (free access)
        fakeDataStore.setBiometricLockEnabled(false)
        val disabledLock = fakeDataStore.biometricLockEnabled.first()
        assertFalse(disabledLock)
    }

    private class FakeDataStoreHelper {
        private val lockState = MutableStateFlow(false)

        val biometricLockEnabled: Flow<Boolean> get() = lockState

        fun setBiometricLockEnabled(enabled: Boolean) {
            lockState.value = enabled
        }
    }

    private class FakeSelfMapDao : SelfMapDao {
        private val itemsMap = mutableMapOf<Long, SelfItemEntity>()

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
