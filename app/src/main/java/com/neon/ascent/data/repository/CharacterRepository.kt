package com.neon.ascent.data.repository

import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.model.UserCharacter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterRepository @Inject constructor(
    private val userCharacterDao: UserCharacterDao
) {
    fun getUserCharacter(): Flow<UserCharacter?> = userCharacterDao.getUserCharacter()

    suspend fun saveCharacter(character: UserCharacter) {
        userCharacterDao.insertUserCharacter(character)
    }

    suspend fun updateCharacter(character: UserCharacter) {
        userCharacterDao.updateUserCharacter(character)
    }

    suspend fun resetCharacter() {
        userCharacterDao.resetCharacter()
    }
    
    suspend fun updateChessElo(newElo: Int) {
        val currentUser = userCharacterDao.getUserCharacter().first()
        currentUser?.let {
            userCharacterDao.updateUserCharacter(it.copy(chessElo = newElo))
        }
    }

    suspend fun updateHolyGhost(level: Int) {
        userCharacterDao.updateHolyGhost(level)
    }

    suspend fun updateWaterBaptism(isBaptized: Boolean) {
        userCharacterDao.updateWaterBaptized(isBaptized)
    }

    suspend fun updateHolySpiritBaptism(isBaptized: Boolean) {
        userCharacterDao.updateHolySpiritBaptized(isBaptized)
    }

    suspend fun addHolyGhostExp(points: Int) {
        userCharacterDao.addHolyGhostExp(points)
    }
}
