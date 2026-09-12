package com.neon.ascent.data.repository

import android.util.Log
import com.neon.ascent.core.data.local.dao.OperativeProfileDao
import com.neon.ascent.core.data.mapper.toDomain
import com.neon.ascent.core.data.mapper.toOperativeEntity
import com.neon.ascent.core.domain.character.repository.CharacterRepository
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.model.UserCharacter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterRepository @Inject constructor(
    private val userCharacterDao: UserCharacterDao,
    private val operativeProfileDao: OperativeProfileDao
) : CharacterRepository {
    private val mutex = Mutex()

    /**
     * Read path: Core OperativeProfile row wins if present.
     * If core row is absent and app row exists, copy-forward app -> core once (Log COPY_CHARACTER_APP_TO_CORE).
     * If copy fails, continue serving app row without deleting app database.
     */
    override fun getUserCharacter(): Flow<com.neon.ascent.core.domain.character.models.UserCharacter?> = combine(
        operativeProfileDao.getOperativeProfile("default_user"),
        userCharacterDao.getUserCharacter()
    ) { coreEntity, appEntity ->
        if (coreEntity != null) {
            coreEntity.toDomain()
        } else if (appEntity != null) {
            mutex.withLock {
                val existingCore = operativeProfileDao.getOperativeProfileOnce("default_user")
                if (existingCore != null) {
                    existingCore.toDomain()
                } else {
                    try {
                        val domainChar = appEntity.toDomain()
                        val coreOperative = domainChar.toOperativeEntity("default_user")
                        operativeProfileDao.upsertOperativeProfile(coreOperative)
                        Log.i("CharacterRepository", "COPY_CHARACTER_APP_TO_CORE: Copied app profile (name=${domainChar.name}) to core operative_profile")
                        domainChar
                    } catch (e: Throwable) {
                        Log.e("CharacterRepository", "COPY_CHARACTER_APP_TO_CORE failed, serving app profile", e)
                        appEntity.toDomain()
                    }
                }
            }
        } else {
            null
        }
    }

    /**
     * Write path: Core wins. Writes directly to core OperativeProfileEntity in NeonAscentDatabase.
     * Legacy app UserCharacter table remains on disk for fallback reads if core is empty.
     */
    override suspend fun saveCharacter(character: com.neon.ascent.core.domain.character.models.UserCharacter) {
        mutex.withLock {
            val coreOperative = character.toOperativeEntity("default_user")
            operativeProfileDao.upsertOperativeProfile(coreOperative)
        }
    }

    /**
     * Reset path: Resets character profile in core OperativeProfileEntity.
     */
    override suspend fun resetCharacter() {
        mutex.withLock {
            val defaultCore = com.neon.ascent.core.domain.character.models.UserCharacter(
                name = "",
                sex = "Unknown",
                dob = "Unknown",
                units = "metric",
                weight = "0",
                somatotype = 0.5f,
                isCreationComplete = false
            ).toOperativeEntity("default_user")
            operativeProfileDao.upsertOperativeProfile(defaultCore)
        }
    }

    /**
     * Maps operative identity (UserCharacter) to workout profile userId.
     * Documented mapping: maps character identity (id/name) to the "default_user" workout profile key.
     */
    fun mapCharacterToWorkoutUserId(character: com.neon.ascent.core.domain.character.models.UserCharacter?): String {
        return if (character != null && character.name.isNotBlank()) {
            "default_user"
        } else {
            "default_user"
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
        level = level,
        experience = experience,
        iceLevel = iceLevel,
        eddies = eddies,
        secureEddies = secureEddies,
        hasBreachedBefore = hasBreachedBefore,
        isSystemDatabaseUnlocked = isSystemDatabaseUnlocked,
        walletConnected = walletConnected,
        strength = strength,
        perception = perception,
        endurance = endurance,
        charisma = charisma,
        agility = agility,
        luck = luck,
        intelligence = intelligence,
        holyGhost = holyGhost,
        holyGhostExp = holyGhostExp,
        prayerStreak = prayerStreak,
        lastPrayerDate = lastPrayerDate,
        waterBaptized = waterBaptized,
        holySpiritBaptized = holySpiritBaptized,
        hasTonguesAura = hasTonguesAura,
        avatarPath = avatarPath,
        isCreationComplete = isCreationComplete,
        neuralLoad = neuralLoad,
        chessElo = chessElo,
        equippedCyberware = equippedCyberware,
        cyberdeckName = cyberdeckName,
        ramSlots = ramSlots,
        usedRam = usedRam,
        quickhackSlots = quickhackSlots,
        loadedQuickhacks = loadedQuickhacks
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
            level = domain.level,
            experience = domain.experience,
            iceLevel = domain.iceLevel,
            eddies = domain.eddies,
            secureEddies = domain.secureEddies,
            hasBreachedBefore = domain.hasBreachedBefore,
            isSystemDatabaseUnlocked = domain.isSystemDatabaseUnlocked,
            walletConnected = domain.walletConnected,
            strength = domain.strength,
            perception = domain.perception,
            endurance = domain.endurance,
            charisma = domain.charisma,
            agility = domain.agility,
            luck = domain.luck,
            intelligence = domain.intelligence,
            holyGhost = domain.holyGhost,
            holyGhostExp = domain.holyGhostExp,
            prayerStreak = domain.prayerStreak,
            lastPrayerDate = domain.lastPrayerDate,
            waterBaptized = domain.waterBaptized,
            holySpiritBaptized = domain.holySpiritBaptized,
            hasTonguesAura = domain.hasTonguesAura,
            avatarPath = domain.avatarPath,
            isCreationComplete = domain.isCreationComplete,
            neuralLoad = domain.neuralLoad,
            chessElo = domain.chessElo,
            equippedCyberware = domain.equippedCyberware,
            cyberdeckName = domain.cyberdeckName,
            ramSlots = domain.ramSlots,
            usedRam = domain.usedRam,
            quickhackSlots = domain.quickhackSlots,
            loadedQuickhacks = domain.loadedQuickhacks
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
        level = level,
        experience = experience,
        iceLevel = iceLevel,
        eddies = eddies,
        secureEddies = secureEddies,
        hasBreachedBefore = hasBreachedBefore,
        isSystemDatabaseUnlocked = isSystemDatabaseUnlocked,
        walletConnected = walletConnected,
        strength = strength,
        perception = perception,
        endurance = endurance,
        charisma = charisma,
        agility = agility,
        luck = luck,
        intelligence = intelligence,
        holyGhost = holyGhost,
        holyGhostExp = holyGhostExp,
        prayerStreak = prayerStreak,
        lastPrayerDate = lastPrayerDate,
        waterBaptized = waterBaptized,
        holySpiritBaptized = holySpiritBaptized,
        hasTonguesAura = hasTonguesAura,
        avatarPath = avatarPath,
        isCreationComplete = isCreationComplete,
        neuralLoad = neuralLoad,
        chessElo = chessElo,
        equippedCyberware = equippedCyberware,
        cyberdeckName = cyberdeckName,
        ramSlots = ramSlots,
        usedRam = usedRam,
        quickhackSlots = quickhackSlots,
        loadedQuickhacks = loadedQuickhacks
    )
}
