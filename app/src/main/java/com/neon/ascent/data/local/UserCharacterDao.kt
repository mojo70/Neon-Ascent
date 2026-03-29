package com.neon.ascent.data.local

import androidx.room.*
import com.neon.ascent.model.UserCharacter
import kotlinx.coroutines.flow.Flow

@Dao
interface UserCharacterDao {
    @Query("SELECT * FROM user_character WHERE id = 0")
    fun getUserCharacter(): Flow<UserCharacter?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserCharacter(userCharacter: UserCharacter)

    @Update
    suspend fun updateUserCharacter(userCharacter: UserCharacter)

    @Query("DELETE FROM user_character WHERE id = 0")
    suspend fun resetCharacter()

    @Query("UPDATE user_character SET holyGhost = :level WHERE id = 0")
    suspend fun updateHolyGhost(level: Int)

    @Query("UPDATE user_character SET eddies = :eddies WHERE id = 0")
    suspend fun updateEddies(eddies: Int)

    @Query("UPDATE user_character SET experience = :xp WHERE id = 0")
    suspend fun updateExperience(xp: Long)

    @Query("UPDATE user_character SET iceLevel = :iceLevel WHERE id = 0")
    suspend fun updateIceLevel(iceLevel: Int)

    @Query("UPDATE user_character SET hasBreachedBefore = 1 WHERE id = 0")
    suspend fun setHasBreached()

    @Query("UPDATE user_character SET isSystemDatabaseUnlocked = 1 WHERE id = 0")
    suspend fun unlockSystemDatabase()
}
