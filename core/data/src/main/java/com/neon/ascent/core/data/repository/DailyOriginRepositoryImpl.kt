package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.dao.BodySampleDao
import com.neon.ascent.core.data.local.dao.DailyVitalRollupDao
import com.neon.ascent.core.data.local.dao.ProtocolDao
import com.neon.ascent.core.data.local.dao.RiteSessionDao
import com.neon.ascent.core.data.local.dao.WorkoutDao
import com.neon.ascent.core.data.local.entity.BodySampleEntity
import com.neon.ascent.core.data.local.entity.DailyVitalRollupEntity
import com.neon.ascent.core.data.local.entity.ProtocolEntity
import com.neon.ascent.core.data.local.entity.RiteSessionEntity
import com.neon.ascent.core.data.local.entity.WorkoutSessionEntity
import com.neon.ascent.core.domain.chronicle.ChronicleEntry
import com.neon.ascent.core.domain.chronicle.ChronicleRepository
import com.neon.ascent.core.domain.chronicle.DailyOriginPayload
import com.neon.ascent.core.domain.chronicle.DailyOriginRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyOriginRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val riteSessionDao: RiteSessionDao,
    private val dailyVitalRollupDao: DailyVitalRollupDao,
    private val bodySampleDao: BodySampleDao,
    private val protocolDao: ProtocolDao,
    private val chronicleRepository: ChronicleRepository
) : DailyOriginRepository {

    override fun observeDay(localDate: LocalDate): Flow<DailyOriginPayload> {
        return observeDay(localDate.toString())
    }

    @Suppress("UNCHECKED_CAST")
    override fun observeDay(localDate: String): Flow<DailyOriginPayload> {
        return combine(
            workoutDao.getAllSessions(),
            riteSessionDao.getSessionsForDate(localDate),
            dailyVitalRollupDao.getDay(localDate),
            bodySampleDao.getSamplesForDay(localDate),
            protocolDao.getAllProtocols(),
            chronicleRepository.observeChronicle()
        ) { array ->
            val sessions = array[0] as List<WorkoutSessionEntity>
            val rites = array[1] as List<RiteSessionEntity>
            val rollups = array[2] as List<DailyVitalRollupEntity>
            val bodySamples = array[3] as List<BodySampleEntity>
            val protocols = array[4] as List<ProtocolEntity>
            val chronicleEntries = array[5] as List<ChronicleEntry>

            DailyOriginAssembler.assemble(
                localDate = localDate,
                sessions = sessions,
                rites = rites,
                rollups = rollups,
                bodySamples = bodySamples,
                protocols = protocols,
                chronicleEntries = chronicleEntries
            )
        }
    }

    override suspend fun linkChronicle(localDate: LocalDate, chronicleEntryId: String) {
        linkChronicle(localDate.toString(), chronicleEntryId)
    }

    override suspend fun linkChronicle(localDate: String, chronicleEntryId: String) {
        val entries = chronicleRepository.observeChronicle().first()
        val entryToLink = entries.firstOrNull { it.sourceId == chronicleEntryId } ?: return
        val updatedEntry = entryToLink.copy(localDate = localDate)
        chronicleRepository.saveEntry(updatedEntry)
    }
}
