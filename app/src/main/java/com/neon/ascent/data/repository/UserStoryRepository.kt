package com.neon.ascent.data.repository

import com.neon.ascent.data.local.UserStoryDao
import com.neon.ascent.domain.model.UserStory
import com.neon.ascent.domain.model.toDomain
import com.neon.ascent.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserStoryRepository @Inject constructor(
    private val userStoryDao: UserStoryDao
) {

    fun getMainStory(): Flow<UserStory> = userStoryDao.getMainStory()
        .map { entity ->
            entity?.toDomain() ?: UserStory()
        }

    suspend fun saveStory(story: UserStory) {
        userStoryDao.upsertStory(story.toEntity())
    }

    suspend fun updateAspirations(newAspirations: List<String>) {
        val story = getMainStory().firstOrNull() ?: UserStory()
        val updated = story.copy(grandAspirations = newAspirations)
        saveStory(updated)
    }
}
