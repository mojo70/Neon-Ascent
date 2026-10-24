package com.neon.ascent.core.domain.chronicle

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface DailyOriginRepository {
    fun observeDay(localDate: LocalDate): Flow<DailyOriginPayload>
    fun observeDay(localDate: String): Flow<DailyOriginPayload>
    suspend fun linkChronicle(localDate: LocalDate, chronicleEntryId: String)
    suspend fun linkChronicle(localDate: String, chronicleEntryId: String)
}
