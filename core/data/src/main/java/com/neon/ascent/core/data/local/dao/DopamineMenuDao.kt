package com.neon.ascent.core.data.local.dao

import androidx.room.*
import com.neon.ascent.core.data.local.entity.DopamineMenuItemEntity
import com.neon.ascent.core.domain.model.EnergyLevel
import kotlinx.coroutines.flow.Flow

@Dao
interface DopamineMenuDao {

    @Query("SELECT * FROM dopamine_menu_items")
    fun getAllItems(): Flow<List<DopamineMenuItemEntity>>

    @Query("SELECT * FROM dopamine_menu_items WHERE energyLevel = :energyLevel")
    fun getItemsByEnergyLevel(energyLevel: EnergyLevel): Flow<List<DopamineMenuItemEntity>>

    @Query("SELECT * FROM dopamine_menu_items WHERE id = :id")
    suspend fun getItemById(id: String): DopamineMenuItemEntity?

    @Query("SELECT COUNT(*) FROM dopamine_menu_items")
    suspend fun getItemCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: DopamineMenuItemEntity)

    @Delete
    suspend fun deleteItem(item: DopamineMenuItemEntity)

    @Query("UPDATE dopamine_menu_items SET usageCount = usageCount + 1, lastUsed = :timestamp WHERE id = :id")
    suspend fun logUsage(id: String, timestamp: java.time.Instant)
}
