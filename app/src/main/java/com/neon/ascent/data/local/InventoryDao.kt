package com.neon.ascent.data.local

import androidx.room.*
import com.neon.ascent.model.QuickHack
import com.neon.ascent.model.QuickHackComponent
import com.neon.ascent.model.Rarity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM quick_hack_components")
    fun getComponents(): Flow<List<QuickHackComponent>>

    @Query("SELECT * FROM quick_hack_components WHERE rarity = :rarity")
    suspend fun getComponentByRarity(rarity: Rarity): QuickHackComponent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateComponent(component: QuickHackComponent)

    @Query("SELECT * FROM quick_hacks")
    fun getQuickHacks(): Flow<List<QuickHack>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuickHack(quickHack: QuickHack)

    @Delete
    suspend fun deleteQuickHack(quickHack: QuickHack)

    @Transaction
    suspend fun addComponents(rarity: Rarity, amount: Int) {
        val current = getComponentByRarity(rarity)
        if (current != null) {
            insertOrUpdateComponent(current.copy(quantity = current.quantity + amount))
        } else {
            insertOrUpdateComponent(QuickHackComponent(rarity = rarity, quantity = amount))
        }
    }
}
