package com.neon.ascent.core.domain.repository

import com.neon.ascent.core.domain.labs.models.LabsFocus
import kotlinx.coroutines.flow.Flow

interface LabsFocusRepository {
    fun observeLabsFocus(): Flow<LabsFocus>
    suspend fun setLabsFocus(focus: LabsFocus)
}
