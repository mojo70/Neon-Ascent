package com.neon.ascent.di

import com.neon.ascent.core.data.local.dao.OperativeProfileDao
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.core.domain.repository.FullDataBackupRepository
import com.neon.ascent.core.domain.repository.SkillRepository
import com.neon.ascent.data.local.GoalDao
import com.neon.ascent.data.local.GoalTaskDao
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.data.local.UserStoryDao
import com.neon.ascent.data.repository.CharacterRepository
import com.neon.ascent.data.repository.FullDataBackupRepositoryImpl
import com.neon.ascent.data.repository.GoalRepository
import com.neon.ascent.data.repository.TaskRepository
import com.neon.ascent.data.repository.UserStoryRepository
import com.neon.ascent.feature.dashboard.MemoryPalaceManager
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
    fun provideGoalRepository(
        dao: GoalDao,
        ascensionRepository: AscensionRepository
    ) = GoalRepository(dao, ascensionRepository)

    @Provides
    @Singleton
    fun provideTaskRepository(dao: GoalTaskDao) = TaskRepository(dao)

    @Provides
    @Singleton
    fun provideCharacterRepository(
        userCharacterDao: UserCharacterDao,
        operativeProfileDao: OperativeProfileDao
    ): com.neon.ascent.core.domain.character.repository.CharacterRepository =
        CharacterRepository(userCharacterDao, operativeProfileDao)

    @Provides
    @Singleton
    fun provideSkillRepository(palace: MemoryPalaceManager): SkillRepository = palace

    @Provides
    @Singleton
    fun provideFullDataBackupRepository(
        impl: FullDataBackupRepositoryImpl
    ): FullDataBackupRepository = impl
}
