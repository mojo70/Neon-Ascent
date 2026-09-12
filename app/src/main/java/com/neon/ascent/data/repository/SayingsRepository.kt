package com.neon.ascent.data.repository

import com.neon.ascent.data.local.SayingsDao
import com.neon.ascent.feature.dashboard.SayingsSeeds
import com.neon.ascent.model.Saying
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SayingsRepository @Inject constructor(
    private val sayingsDao: SayingsDao
) {
    // Seed catalog in memory with initial seed values
    private val _sayings = MutableStateFlow<List<Saying>>(SayingsSeeds.SEED_SAYINGS)

    suspend fun syncUserSayingsFromDatabase() {
        try {
            val dbSayings = sayingsDao.getAllSayings().first()
            if (dbSayings.isNotEmpty()) {
                val current = _sayings.value.toMutableList()
                val existingIds = current.map { it.id }.toSet()
                val existingTexts = current.map { it.text.trim().lowercase() }.toSet()

                dbSayings.forEach { dbSaying ->
                    if (!existingIds.contains(dbSaying.id) &&
                        !existingTexts.contains(dbSaying.text.trim().lowercase())
                    ) {
                        current.add(dbSaying)
                    }
                }
                _sayings.value = current
            }
        } catch (_: Exception) {
            // Best effort copy from disk; seed remains safe fallback
        }
    }

    fun getAllSayings(): Flow<List<Saying>> = _sayings.asStateFlow()

    fun getSayingsByCategory(category: String): Flow<List<Saying>> {
        return _sayings.map { list -> list.filter { it.category.equals(category, ignoreCase = true) } }
    }

    suspend fun addCustomSaying(text: String, category: String = "Custom") {
        val newSaying = Saying(
            id = "custom_${System.currentTimeMillis()}",
            text = text,
            category = category,
            engagementScore = 100,
            isEnabled = true
        )
        val current = _sayings.value.toMutableList()
        current.add(0, newSaying)
        _sayings.value = current
        try {
            sayingsDao.insertSaying(newSaying)
        } catch (_: Exception) {}
    }

    suspend fun toggleSayingEnabled(saying: Saying) {
        val updated = saying.copy(isEnabled = !saying.isEnabled)
        val current = _sayings.value.map {
            if (it.id == saying.id) updated else it
        }
        _sayings.value = current
        try {
            sayingsDao.insertSaying(updated)
        } catch (_: Exception) {}
    }

    suspend fun deleteSaying(saying: Saying) {
        val current = _sayings.value.filter { it.id != saying.id }
        _sayings.value = current
        try {
            sayingsDao.deleteSaying(saying)
        } catch (_: Exception) {}
    }
}
