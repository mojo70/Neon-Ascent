package com.neon.ascent.core.domain.character.repository

import com.neon.ascent.core.domain.character.models.UserCharacter
import kotlinx.coroutines.flow.Flow

interface CharacterRepository {
    fun getUserCharacter(): Flow<UserCharacter?>
    suspend fun saveCharacter(character: UserCharacter)
}
