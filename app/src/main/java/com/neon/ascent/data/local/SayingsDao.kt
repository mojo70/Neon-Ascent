package com.neon.ascent.data.local

import androidx.room.*
import com.neon.ascent.model.Saying
import kotlinx.coroutines.flow.Flow

@Dao
interface SayingsDao {
    @Query("SELECT * FROM sayings")
    fun getAllSayings(): Flow<List<Saying>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaying(saying: Saying)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSayings(sayings: List<Saying>)

    @Delete
    suspend fun deleteSaying(saying: Saying)

    @Query("SELECT * FROM sayings WHERE category = :category")
    suspend fun getSayingsByCategory(category: String): List<Saying>
}
