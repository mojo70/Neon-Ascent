package com.neon.ascent.data.repository

import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.model.UserCharacter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterRepository @Inject constructor(
    private val userCharacterDao: UserCharacterDao
) : com.neon.ascent.core.domain.character.repository.CharacterRepository {
    private val mutex = Mutex()

    override fun getUserCharacter(): Flow<com.neon.ascent.core.domain.character.models.UserCharacter?> = 
        userCharacterDao.getUserCharacter().map { it?.toDomain() }

    override suspend fun saveCharacter(character: com.neon.ascent.core.domain.character.models.UserCharacter) {
        mutex.withLock {
            val current = userCharacterDao.getUserCharacter().first()
            val entity = current?.updateFromDomain(character) ?: character.toEntity()
            userCharacterDao.insertUserCharacter(entity) // Use insert with REPLACE instead of update
        }
    }

    private fun com.neon.ascent.core.domain.character.models.UserCharacter.toEntity() = UserCharacter(
        id = 0,
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
        experience = experience,
        neuralLoad = neuralLoad,
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

    private fun UserCharacter.updateFromDomain(domain: com.neon.ascent.core.domain.character.models.UserCharacter): UserCharacter {
        return copy(
            name = domain.name,
            netrunnerName = domain.netrunnerName,
            sex = domain.sex,
            dob = domain.dob,
            units = domain.units,
            heightFeet = domain.heightFeet,
            heightInches = domain.heightInches,
            heightCm = domain.heightCm,
            weight = domain.weight,
            somatotype = domain.somatotype,
            mbti = domain.mbti,
            alignment = domain.alignment,
            archetype = domain.archetype,
            strength = domain.strength,
            endurance = domain.endurance,
            agility = domain.agility,
            perception = domain.perception,
            intelligence = domain.intelligence,
            charisma = domain.charisma,
            luck = domain.luck,
            level = domain.level,
            experience = domain.experience,
            neuralLoad = domain.neuralLoad,
            isCreationComplete = domain.isCreationComplete,
            avatarPath = domain.avatarPath,
            eddies = domain.eddies,
            secureEddies = domain.secureEddies,
            walletConnected = domain.walletConnected,
            isSystemDatabaseUnlocked = domain.isSystemDatabaseUnlocked,
            holyGhost = domain.holyGhost,
            prayerStreak = domain.prayerStreak,
            lastPrayerDate = domain.lastPrayerDate,
            waterBaptized = domain.waterBaptized,
            holySpiritBaptized = domain.holySpiritBaptized,
            ramSlots = domain.ramSlots,
            usedRam = domain.usedRam,
            quickhackSlots = domain.quickhackSlots,
            loadedQuickhacks = domain.loadedQuickhacks,
            equippedCyberware = domain.equippedCyberware
        )
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
        mutex.withLock {
            val current = userCharacterDao.getUserCharacter().first()
            current?.let {
                userCharacterDao.updateUserCharacter(it.updateFromDomain(character))
            }
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
