package com.neon.ascent.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neon.ascent.core.data.local.entity.SupplementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplementDao {
    @Query("SELECT * FROM supplements")
    fun getAllSupplements(): Flow<List<SupplementEntity>>

    @Query("SELECT * FROM supplements WHERE id = :id")
    fun getSupplementById(id: String): Flow<SupplementEntity?>

    @Query("SELECT * FROM supplements WHERE id = :id")
    suspend fun getSupplementByIdSync(id: String): SupplementEntity?

    @Query("SELECT COUNT(*) FROM supplements")
    suspend fun getSupplementCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplements(supplements: List<SupplementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplement(supplement: SupplementEntity)
}
