package com.neon.ascent.data.repository

import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.domain.model.SpecialStat
import com.neon.ascent.domain.model.SpecialType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpecialRepository @Inject constructor(
    private val userCharacterDao: UserCharacterDao
) {
    fun getAllSpecialStats(): Flow<List<SpecialStat>> = userCharacterDao.getUserCharacter().map { char ->
        if (char == null) emptyList()
        else listOf(
            SpecialStat(SpecialType.STRENGTH, char.strength ?: 0),
            SpecialStat(SpecialType.PERCEPTION, char.perception ?: 0),
            SpecialStat(SpecialType.ENDURANCE, char.endurance ?: 0),
            SpecialStat(SpecialType.CHARISMA, char.charisma ?: 0),
            SpecialStat(SpecialType.AGILITY, char.agility ?: 0),
            SpecialStat(SpecialType.LUCK, char.luck ?: 0),
            SpecialStat(SpecialType.INTELLIGENCE, char.intelligence ?: 0)
        )
    }
}
