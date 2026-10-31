package com.neon.ascent.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neon.ascent.core.data.local.entity.SanctuaryPassageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SanctuaryDao {
    @Query("SELECT * FROM sanctuary_passages ORDER BY sortOrder ASC")
    fun getAllPassages(): Flow<List<SanctuaryPassageEntity>>

    @Query("SELECT * FROM sanctuary_passages WHERE id = :id")
    suspend fun getPassageById(id: String): SanctuaryPassageEntity?

    @Query("SELECT * FROM sanctuary_passages WHERE themes LIKE '%' || :theme || '%' ORDER BY sortOrder ASC")
    fun getPassagesByTheme(theme: String): Flow<List<SanctuaryPassageEntity>>

    @Query("SELECT * FROM sanctuary_passages WHERE tags LIKE '%' || :tag || '%' ORDER BY sortOrder ASC")
    fun getPassagesByTag(tag: String): Flow<List<SanctuaryPassageEntity>>

    @Query("SELECT COUNT(*) FROM sanctuary_passages")
    suspend fun getPassageCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPassagesIfAbsent(passages: List<SanctuaryPassageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePassages(passages: List<SanctuaryPassageEntity>)
}
