package com.neon.ascent.core.domain.repository

import com.neon.ascent.core.domain.labs.models.Diet
import com.neon.ascent.core.domain.labs.models.DietOverrides
import kotlinx.coroutines.flow.Flow

interface DietRepository {
    fun observeAll(): Flow<List<Diet>>
    fun observeById(id: String): Flow<Diet?>
    suspend fun getById(id: String): Diet?
    suspend fun seedDefaultDiets()

    fun observeActiveDiet(): Flow<Diet?>
    suspend fun setActiveDiet(id: String?)
    fun observeOverrides(): Flow<DietOverrides>
    suspend fun updateOverrides(overrides: DietOverrides)
}
