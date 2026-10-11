package com.neon.ascent.core.data.repository

import com.google.gson.GsonBuilder
import com.neon.ascent.core.data.datastore.SelfMapPreferencesDataStore
import com.neon.ascent.core.data.local.dao.SelfMapDao
import com.neon.ascent.core.data.local.dao.calculateWeightedRadialMass
import com.neon.ascent.core.data.local.entity.EoEEntryEntity
import com.neon.ascent.core.data.local.entity.SelfItemEntity
import com.neon.ascent.core.data.local.entity.SelfMapSnapshotEntity
import com.neon.ascent.core.domain.selfmap.SelfMapCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelfMapRepository @Inject constructor(
    private val selfMapDao: SelfMapDao,
    private val preferencesDataStore: SelfMapPreferencesDataStore
) {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    val activeItems: Flow<List<SelfItemEntity>> = selfMapDao.getAllActiveItems()

    val averageImportance: Flow<Float> = selfMapDao.getAverageImportance()

    val coreCount: Flow<Int> = selfMapDao.getCoreCount()

    val totalItemCount: Flow<Int> = selfMapDao.getTotalItemCount()

    val onboardingCompleted: Flow<Boolean> = preferencesDataStore.onboardingCompleted

    val biometricLockEnabled: Flow<Boolean> = preferencesDataStore.biometricLockEnabled

    val baselineMass: Flow<Float?> = preferencesDataStore.baselineMass

    val currentMass: Flow<Float> = activeItems.map { items ->
        items.calculateWeightedRadialMass()
    }

    /**
     * "Core attachment load" as percentage of baseline mass (0% - 150%+).
     * Defaults to 100% if baseline mass is not set or 0.
     */
    val coreAttachmentLoadPercentage: Flow<Float> = combine(currentMass, baselineMass) { current, baseline ->
        if (baseline == null || baseline <= 0f) {
            100f
        } else {
            (current / baseline) * 100f
        }
    }

    suspend fun getAllItemsOnce(): List<SelfItemEntity> {
        return selfMapDao.getAllActiveItemsOnce()
    }

    fun getItemsByCategory(category: SelfMapCategory): Flow<List<SelfItemEntity>> {
        return selfMapDao.getItemsByCategory(category)
    }

    fun getItemById(id: Long): Flow<SelfItemEntity?> {
        return selfMapDao.getItemById(id)
    }

    suspend fun insertSelfItem(item: SelfItemEntity): Long {
        val id = selfMapDao.insertSelfItem(item)
        checkAndUpdateBaselineIfUnset()
        return id
    }

    suspend fun updateSelfItem(item: SelfItemEntity) {
        selfMapDao.updateSelfItem(item.copy(updatedAt = Instant.now()))
    }

    suspend fun archiveSelfItem(id: Long) {
        selfMapDao.archiveSelfItem(id)
    }

    suspend fun unarchiveSelfItem(id: Long) {
        selfMapDao.unarchiveSelfItem(id)
    }

    suspend fun deleteSelfItem(id: Long) {
        selfMapDao.deleteSelfItem(id)
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        preferencesDataStore.setOnboardingCompleted(completed)
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        preferencesDataStore.setBiometricLockEnabled(enabled)
    }

    suspend fun setBaselineMass(mass: Float) {
        preferencesDataStore.setBaselineMass(mass)
    }

    /**
     * Pre-seeds Body & Senses items on first open if table is empty.
     * Items pre-seeded at high importance:
     * - Body (0.9)
     * - Physical health (0.9)
     * - Appearance (0.8)
     * - Senses (0.8)
     */
    suspend fun preseedDefaultItemsIfEmpty(): List<SelfItemEntity> {
        val currentItems = selfMapDao.getAllItemsOnce()
        if (currentItems.isEmpty()) {
            val defaults = listOf(
                SelfItemEntity(
                    name = "Body",
                    category = SelfMapCategory.BODY_SENSES,
                    importance = 0.9f,
                    notes = "Core physical vessel and anchor",
                    baselineMassContributor = true
                ),
                SelfItemEntity(
                    name = "Physical health",
                    category = SelfMapCategory.BODY_SENSES,
                    importance = 0.9f,
                    notes = "Vital energy and physical operating condition",
                    baselineMassContributor = true
                ),
                SelfItemEntity(
                    name = "Appearance",
                    category = SelfMapCategory.BODY_SENSES,
                    importance = 0.8f,
                    notes = "External form and visual presentation",
                    baselineMassContributor = true
                ),
                SelfItemEntity(
                    name = "Senses",
                    category = SelfMapCategory.BODY_SENSES,
                    importance = 0.8f,
                    notes = "Sensory perception and environmental awareness",
                    baselineMassContributor = true
                )
            )
            selfMapDao.insertSelfItems(defaults)
            val seeded = selfMapDao.getAllActiveItemsOnce()
            val initialMass = seeded.calculateWeightedRadialMass()
            preferencesDataStore.setBaselineMass(initialMass)
            return seeded
        }
        return currentItems
    }

    private suspend fun checkAndUpdateBaselineIfUnset() {
        val currentBaseline = preferencesDataStore.baselineMass.first()
        if (currentBaseline == null || currentBaseline <= 0f) {
            val items = selfMapDao.getAllActiveItemsOnce()
            if (items.isNotEmpty()) {
                val mass = items.calculateWeightedRadialMass()
                preferencesDataStore.setBaselineMass(mass)
            }
        }
    }

    // --- EoE Logging ---

    suspend fun insertEoEEntry(entry: EoEEntryEntity): Long {
        return selfMapDao.insertEoEEntry(entry)
    }

    fun getAllEoEEntries(): Flow<List<EoEEntryEntity>> {
        return selfMapDao.getAllEoEEntries()
    }

    fun getEoEEntriesForSelfItem(selfItemId: Long): Flow<List<EoEEntryEntity>> {
        return selfMapDao.getEoEEntriesForSelfItem(selfItemId)
    }

    suspend fun deleteEoEEntry(id: Long) {
        selfMapDao.deleteEoEEntry(id)
    }

    // --- Snapshots ---

    suspend fun createSnapshot(): SelfMapSnapshotEntity {
        val active = selfMapDao.getAllActiveItemsOnce()
        val mass = active.calculateWeightedRadialMass()
        val avgImp = if (active.isNotEmpty()) active.map { it.importance }.average().toFloat() else 0f
        val cores = active.count { it.importance >= 0.7f }

        val snapshot = SelfMapSnapshotEntity(
            timestamp = Instant.now(),
            mass = mass,
            itemCount = active.size,
            avgImportance = avgImp,
            coreCount = cores
        )
        val id = selfMapDao.insertSnapshot(snapshot)
        return snapshot.copy(id = id)
    }

    fun getAllSnapshots(): Flow<List<SelfMapSnapshotEntity>> {
        return selfMapDao.getAllSnapshots()
    }

    fun getLatestSnapshot(): Flow<SelfMapSnapshotEntity?> {
        return selfMapDao.getLatestSnapshot()
    }

    // --- Export & Reset ---

    suspend fun exportDataAsJson(): String {
        val items = selfMapDao.getAllItemsOnce()
        val entries = selfMapDao.getAllEoEEntriesOnce()
        val snapshots = selfMapDao.getAllSnapshotsOnce()
        val baseline = preferencesDataStore.baselineMass.first()

        val exportMap = mapOf(
            "version" to 1,
            "exportedAt" to Instant.now().toString(),
            "baselineMass" to baseline,
            "items" to items,
            "eoeEntries" to entries,
            "snapshots" to snapshots
        )
        return gson.toJson(exportMap)
    }

    suspend fun deleteAllData() {
        selfMapDao.deleteAllSelfItems()
        selfMapDao.deleteAllEoEEntries()
        selfMapDao.deleteAllSnapshots()
        preferencesDataStore.clearAll()
    }
}
