package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.datastore.LabsFocusPreferencesDataStore
import com.neon.ascent.core.domain.labs.models.LabsFocus
import com.neon.ascent.core.domain.repository.LabsFocusRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LabsFocusRepositoryImpl @Inject constructor(
    private val preferencesDataStore: LabsFocusPreferencesDataStore
) : LabsFocusRepository {

    override fun observeLabsFocus(): Flow<LabsFocus> {
        return preferencesDataStore.labsFocus
    }

    override suspend fun setLabsFocus(focus: LabsFocus) {
        preferencesDataStore.setLabsFocus(focus)
    }
}
