package com.neon.ascent.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.neon.ascent.core.data.local.entity.EoEEntryEntity
import com.neon.ascent.core.data.local.entity.SelfItemEntity
import com.neon.ascent.core.data.local.entity.SelfMapMoodEntryEntity
import com.neon.ascent.core.data.local.entity.SelfMapSnapshotEntity
import com.neon.ascent.core.domain.selfmap.SelfMapCategory
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface SelfMapDao {

    // --- SelfItem Queries ---

    @Query("SELECT * FROM self_items WHERE isArchived = 0 ORDER BY importance DESC")
    fun getAllActiveItems(): Flow<List<SelfItemEntity>>

    @Query("SELECT * FROM self_items WHERE isArchived = 0 ORDER BY importance DESC")
    suspend fun getAllActiveItemsOnce(): List<SelfItemEntity>

    @Query("SELECT * FROM self_items ORDER BY importance DESC")
    suspend fun getAllItemsOnce(): List<SelfItemEntity>

    @Query("SELECT * FROM self_items WHERE isArchived = 0 AND category = :category ORDER BY importance DESC")
    fun getItemsByCategory(category: SelfMapCategory): Flow<List<SelfItemEntity>>

    @Query("SELECT * FROM self_items WHERE id = :id")
    fun getItemById(id: Long): Flow<SelfItemEntity?>

    @Query("SELECT * FROM self_items WHERE id = :id")
    suspend fun getItemByIdOnce(id: Long): SelfItemEntity?

    @Query("SELECT COALESCE(AVG(importance), 0.0) FROM self_items WHERE isArchived = 0")
    fun getAverageImportance(): Flow<Float>

    @Query("SELECT COUNT(*) FROM self_items WHERE isArchived = 0 AND importance >= 0.7")
    fun getCoreCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM self_items WHERE isArchived = 0")
    fun getTotalItemCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSelfItem(item: SelfItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSelfItems(items: List<SelfItemEntity>): List<Long>

    @Update
    suspend fun updateSelfItem(item: SelfItemEntity)

    @Query("UPDATE self_items SET isArchived = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archiveSelfItem(id: Long, updatedAt: Instant = Instant.now())

    @Query("UPDATE self_items SET isArchived = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun unarchiveSelfItem(id: Long, updatedAt: Instant = Instant.now())

    @Query("DELETE FROM self_items WHERE id = :id")
    suspend fun deleteSelfItem(id: Long)

    @Query("DELETE FROM self_items")
    suspend fun deleteAllSelfItems()


    // --- EoEEntry Queries ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEoEEntry(entry: EoEEntryEntity): Long

    @Query("SELECT * FROM eoe_entries ORDER BY timestamp DESC")
    fun getAllEoEEntries(): Flow<List<EoEEntryEntity>>

    @Query("SELECT * FROM eoe_entries ORDER BY timestamp DESC")
    suspend fun getAllEoEEntriesOnce(): List<EoEEntryEntity>

    @Query("SELECT * FROM eoe_entries WHERE selfItemId = :selfItemId ORDER BY timestamp DESC")
    fun getEoEEntriesForSelfItem(selfItemId: Long): Flow<List<EoEEntryEntity>>

    @Query("DELETE FROM eoe_entries WHERE id = :id")
    suspend fun deleteEoEEntry(id: Long)

    @Query("DELETE FROM eoe_entries")
    suspend fun deleteAllEoEEntries()


    // --- SelfMapSnapshot Queries ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: SelfMapSnapshotEntity): Long

    @Query("SELECT * FROM self_map_snapshots ORDER BY timestamp DESC")
    fun getAllSnapshots(): Flow<List<SelfMapSnapshotEntity>>

    @Query("SELECT * FROM self_map_snapshots ORDER BY timestamp DESC")
    suspend fun getAllSnapshotsOnce(): List<SelfMapSnapshotEntity>

    @Query("SELECT * FROM self_map_snapshots ORDER BY timestamp DESC LIMIT 1")
    fun getLatestSnapshot(): Flow<SelfMapSnapshotEntity?>

    @Query("DELETE FROM self_map_snapshots")
    suspend fun deleteAllSnapshots()


    // --- SelfMapMoodEntry Queries ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodEntry(entry: SelfMapMoodEntryEntity): Long

    @Query("SELECT * FROM self_map_mood_entries ORDER BY timestamp DESC")
    fun getAllMoodEntries(): Flow<List<SelfMapMoodEntryEntity>>

    @Query("SELECT * FROM self_map_mood_entries ORDER BY timestamp DESC")
    suspend fun getAllMoodEntriesOnce(): List<SelfMapMoodEntryEntity>

    @Query("SELECT * FROM self_map_mood_entries ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMoodEntries(limit: Int): Flow<List<SelfMapMoodEntryEntity>>

    @Query("SELECT * FROM self_map_mood_entries ORDER BY timestamp DESC LIMIT 1")
    fun getLatestMoodEntry(): Flow<SelfMapMoodEntryEntity?>

    @Query("SELECT * FROM self_map_mood_entries ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMoodEntryOnce(): SelfMapMoodEntryEntity?

    @Query("DELETE FROM self_map_mood_entries WHERE id = :id")
    suspend fun deleteMoodEntry(id: Long)

    @Query("DELETE FROM self_map_mood_entries")
    suspend fun deleteAllMoodEntries()
}

/**
 * Calculates weighted radial mass = sum(importance ^ 1.5) over non-archived items.
 */
fun List<SelfItemEntity>.calculateWeightedRadialMass(): Float {
    return this.filter { !it.isArchived }
        .sumOf { Math.pow(it.importance.toDouble(), 1.5) }
        .toFloat()
}
