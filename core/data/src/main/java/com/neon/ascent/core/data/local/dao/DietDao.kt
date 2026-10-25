package com.neon.ascent.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neon.ascent.core.data.local.entity.DietEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DietDao {
    @Query("SELECT * FROM diets")
    fun getAllDiets(): Flow<List<DietEntity>>

    @Query("SELECT * FROM diets WHERE id = :id")
    fun getDietById(id: String): Flow<DietEntity?>

    @Query("SELECT * FROM diets WHERE id = :id")
    suspend fun getDietByIdSync(id: String): DietEntity?

    @Query("SELECT COUNT(*) FROM diets")
    suspend fun getDietCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiets(diets: List<DietEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiet(diet: DietEntity)
}
