package com.neon.ascent.data.repository

import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.model.UserCharacter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterRepository @Inject constructor(
    private val userCharacterDao: UserCharacterDao
) : com.neon.ascent.core.domain.character.repository.CharacterRepository {
    override fun getUserCharacter(): Flow<com.neon.ascent.core.domain.character.models.UserCharacter?> = 
        userCharacterDao.getUserCharacter().map { it?.toDomain() }

    override suspend fun saveCharacter(character: com.neon.ascent.core.domain.character.models.UserCharacter) {
        // Implementation for interface
        val current = userCharacterDao.getUserCharacter().first()
        current?.let {
            userCharacterDao.updateUserCharacter(it.copy(
                strength = character.strength,
                endurance = character.endurance,
                agility = character.agility
            ))
        }
    }

    private fun UserCharacter.toDomain() = com.neon.ascent.core.domain.character.models.UserCharacter(
        id = id,
        name = name,
        netrunnerName = netrunnerName,
        sex = sex,
        dob = dob,
        units = units,
        heightFeet = heightFeet,
        heightInches = heightInches,
        heightCm = heightCm,
        weight = weight,
        somatotype = somatotype,
        mbti = mbti,
        alignment = alignment,
        archetype = archetype,
        strength = strength,
        endurance = endurance,
        agility = agility,
        perception = perception,
        intelligence = intelligence,
        charisma = charisma,
        luck = luck,
        level = level,
        neuralLoad = neuralLoad,
        experience = experience,
        isCreationComplete = isCreationComplete,
        avatarPath = avatarPath,
        eddies = eddies,
        secureEddies = secureEddies,
        walletConnected = walletConnected,
        isSystemDatabaseUnlocked = isSystemDatabaseUnlocked,
        holyGhost = holyGhost,
        prayerStreak = prayerStreak,
        lastPrayerDate = lastPrayerDate,
        waterBaptized = waterBaptized,
        holySpiritBaptized = holySpiritBaptized,
        ramSlots = ramSlots,
        usedRam = usedRam,
        quickhackSlots = quickhackSlots,
        loadedQuickhacks = loadedQuickhacks,
        equippedCyberware = equippedCyberware
    )

    fun getUserCharacterFlow(): Flow<UserCharacter?> = userCharacterDao.getUserCharacter()

    suspend fun updateCharacter(character: com.neon.ascent.core.domain.character.models.UserCharacter) {
        val current = userCharacterDao.getUserCharacter().first()
        current?.let {
            userCharacterDao.updateUserCharacter(it.copy(
                name = character.name,
                netrunnerName = character.netrunnerName,
                sex = character.sex,
                dob = character.dob,
                units = character.units,
                heightFeet = character.heightFeet,
                heightInches = character.heightInches,
                heightCm = character.heightCm,
                weight = character.weight,
                somatotype = character.somatotype,
                mbti = character.mbti,
                alignment = character.alignment,
                archetype = character.archetype,
                strength = character.strength,
                endurance = character.endurance,
                agility = character.agility,
                perception = character.perception,
                intelligence = character.intelligence,
                charisma = character.charisma,
                luck = character.luck,
                level = character.level,
                experience = character.experience,
                eddies = character.eddies,
                isSystemDatabaseUnlocked = character.isSystemDatabaseUnlocked,
                avatarPath = character.avatarPath,
                isCreationComplete = character.isCreationComplete,
                neuralLoad = character.neuralLoad
            ))
        }
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
