package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.dao.RiteSessionDao
import com.neon.ascent.core.data.local.entity.RiteSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RitesRepository @Inject constructor(
    private val riteSessionDao: RiteSessionDao
) {
    suspend fun recordSession(
        kind: String, // PRAYER, SIT, KEGEL
        durationMin: Int,
        source: String, // ALTAR, STILLJACK, QUICK_STAMP
        startedAt: Instant = Instant.now(),
        endedAt: Instant? = Instant.now(),
        notes: String? = null,
        localDate: LocalDate = LocalDate.now()
    ): RiteSessionEntity {
        val session = RiteSessionEntity(
            id = UUID.randomUUID().toString(),
            kind = kind,
            startedAt = startedAt,
            endedAt = endedAt,
            durationMin = durationMin,
            source = source,
            notes = notes,
            localDate = localDate.toString()
        )
        riteSessionDao.insertSession(session)
        return session
    }

    fun getSessionsForDate(date: LocalDate = LocalDate.now()): Flow<List<RiteSessionEntity>> {
        return riteSessionDao.getSessionsForDate(date.toString())
    }

    fun getSessionsForKind(kind: String): Flow<List<RiteSessionEntity>> {
        return riteSessionDao.getSessionsForKind(kind)
    }

    fun getAllSessions(): Flow<List<RiteSessionEntity>> {
        return riteSessionDao.getAllSessions()
    }

    suspend fun isDayDone(kind: String, date: LocalDate = LocalDate.now()): Boolean {
        return riteSessionDao.getSessionsForDateAndKind(date.toString(), kind).isNotEmpty()
    }

    fun getSitMaskWindowsFlow(date: LocalDate = LocalDate.now()): Flow<List<Pair<Instant, Instant>>> {
        return riteSessionDao.getSessionsForDate(date.toString()).map { sessions ->
            sessions
                .filter { it.kind == "SIT" || (it.kind == "PRAYER" && it.durationMin >= 10) }
                .map { session ->
                    val start = session.startedAt
                    val end = session.endedAt ?: start.plusSeconds(session.durationMin.toLong() * 60L)
                    Pair(start, end)
                }
        }
    }

    suspend fun getSitMaskWindowsForDate(date: LocalDate = LocalDate.now()): List<Pair<Instant, Instant>> {
        val dateStr = date.toString()
        val sessions = riteSessionDao.getSessionsForDateAndKind(dateStr, "SIT") +
                riteSessionDao.getSessionsForDateAndKind(dateStr, "PRAYER").filter { it.durationMin >= 10 }
        return sessions.map { session ->
            val start = session.startedAt
            val end = session.endedAt ?: start.plusSeconds(session.durationMin.toLong() * 60L)
            Pair(start, end)
        }
    }
}
