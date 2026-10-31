package com.neon.ascent.core.domain.sanctuary.repository

import com.neon.ascent.core.domain.sanctuary.models.SanctuaryPassage
import kotlinx.coroutines.flow.Flow

interface SanctuaryRepository {
    fun observeAllPassages(): Flow<List<SanctuaryPassage>>
    fun observePassagesByTheme(theme: String): Flow<List<SanctuaryPassage>>
    fun observePassagesByTag(tag: String): Flow<List<SanctuaryPassage>>
    suspend fun getPassageById(id: String): SanctuaryPassage?
    suspend fun seedDefaultPassagesIfAbsent()
}
