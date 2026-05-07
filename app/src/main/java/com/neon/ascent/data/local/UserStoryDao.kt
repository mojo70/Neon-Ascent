package com.neon.ascent.data.local

import androidx.room.*
import com.neon.ascent.data.local.entity.UserStoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStoryDao {
    @Query("SELECT * FROM user_story WHERE id = 'main_user_story' LIMIT 1")
    fun getMainStory(): Flow<UserStoryEntity?>

    @Upsert
    suspend fun upsertStory(story: UserStoryEntity)

    @Query("UPDATE user_story SET updatedAt = :timestamp WHERE id = 'main_user_story'")
    suspend fun touch(timestamp: Long = System.currentTimeMillis())
}
