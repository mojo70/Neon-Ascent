package com.neon.ascent.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.neon.ascent.core.data.local.entity.OperativeProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OperativeProfileDao {
    @Query("SELECT * FROM operative_profile WHERE id = :id LIMIT 1")
    fun getOperativeProfile(id: String = "default_user"): Flow<OperativeProfileEntity?>

    @Query("SELECT * FROM operative_profile WHERE id = :id LIMIT 1")
    suspend fun getOperativeProfileOnce(id: String = "default_user"): OperativeProfileEntity?

    @Upsert
    suspend fun upsertOperativeProfile(entity: OperativeProfileEntity)
}
