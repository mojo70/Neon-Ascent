package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.entity.BodySampleEntity
import com.neon.ascent.core.data.local.entity.DailyVitalRollupEntity
import com.neon.ascent.core.data.local.entity.ProtocolEntity
import com.neon.ascent.core.data.local.entity.RiteSessionEntity
import com.neon.ascent.core.data.local.entity.WorkoutSessionEntity
import com.neon.ascent.core.domain.chronicle.ChronicleEntry
import com.neon.ascent.core.domain.chronicle.DailyOriginPayload
import java.time.Instant
import java.time.ZoneId

object DailyOriginAssembler {
    fun assemble(
        localDate: String,
        sessions: List<WorkoutSessionEntity> = emptyList(),
        rites: List<RiteSessionEntity> = emptyList(),
        rollups: List<DailyVitalRollupEntity> = emptyList(),
        bodySamples: List<BodySampleEntity> = emptyList(),
        protocols: List<ProtocolEntity> = emptyList(),
        chronicleEntries: List<ChronicleEntry> = emptyList()
    ): DailyOriginPayload {
        val workoutForDay = sessions.firstOrNull { session ->
            val sessionDate = session.date
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString()
            sessionDate == localDate
        }
        val workoutSessionId = workoutForDay?.id
        val workoutSummary = workoutForDay?.let {
            val mins = it.durationSeconds / 60
            "${it.protocol} · ${mins}m"
        }

        val ritesForDay = rites.filter { it.localDate == localDate }
        val riteIds = ritesForDay.map { it.id }

        val sanctumRollup = rollups.firstOrNull {
            it.localDate == localDate && (it.metric == "SANCTUM" || it.metric == "SLEEP_DURATION" || it.metric == "SLEEP_SCORE")
        }
        val sanctumNightId = sanctumRollup?.let { "SANCTUM_${it.localDate}" }
        val sanctumSummary = sanctumRollup?.let {
            if (it.metric == "SANCTUM" || it.metric == "SLEEP_SCORE") "SANCTUM ${it.value.toInt()}"
            else "Sleep ${it.value}h"
        }

        val bodySamplesForDay = bodySamples.filter { it.localDate == localDate }
        val bodyLogIds = bodySamplesForDay.map { it.id }

        val protocolIds = protocols.map { it.id }

        val chronicleForDay = chronicleEntries.firstOrNull { entry ->
            entry.localDate == localDate ||
            entry.metadata?.contains("\"localDate\":\"$localDate\"") == true ||
            (entry.room == "ORIGIN" && Instant.ofEpochMilli(entry.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString() == localDate)
        }
        val chronicleEntryId = chronicleForDay?.sourceId
        val chronicleContent = chronicleForDay?.content

        return DailyOriginPayload(
            localDate = localDate,
            workoutSessionId = workoutSessionId,
            workoutSummary = workoutSummary,
            riteIds = riteIds,
            sanctumNightId = sanctumNightId,
            sanctumSummary = sanctumSummary,
            bodyLogIds = bodyLogIds,
            protocolIds = protocolIds,
            chronicleEntryId = chronicleEntryId,
            chronicleContent = chronicleContent
        )
    }
}
