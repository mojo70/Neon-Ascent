package com.neon.ascent.di

import com.neon.ascent.core.domain.repository.FullDataBackupRepository
import com.neon.ascent.data.local.GoalDao
import com.neon.ascent.data.local.GoalTaskDao
import com.neon.ascent.data.local.UserStoryDao
import com.neon.ascent.data.repository.FullDataBackupRepositoryImpl
import com.neon.ascent.data.repository.GoalRepository
import com.neon.ascent.data.repository.TaskRepository
import com.neon.ascent.data.repository.UserStoryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideUserStoryRepository(dao: UserStoryDao) =
        UserStoryRepository(dao)

    @Provides
    @Singleton
    fun provideGoalRepository(dao: GoalDao) = GoalRepository(dao)

    @Provides
    @Singleton
    fun provideTaskRepository(dao: GoalTaskDao) = TaskRepository(dao)

    @Provides
    @Singleton
    fun provideCharacterRepository(
        userCharacterDao: com.neon.ascent.data.local.UserCharacterDao
    ): com.neon.ascent.core.domain.character.repository.CharacterRepository =
        com.neon.ascent.data.repository.CharacterRepository(userCharacterDao)

    @Provides
    @Singleton
    fun provideSkillRepository(palace: com.neon.ascent.feature.dashboard.MemoryPalaceManager): com.neon.ascent.core.domain.repository.SkillRepository = palace

    @Provides
    @Singleton
    fun provideFullDataBackupRepository(
        impl: FullDataBackupRepositoryImpl
    ): FullDataBackupRepository = impl
}
