package com.neon.ascent.core.data.repository

import android.util.Log
import androidx.health.connect.client.records.BloodPressureRecord
import com.neon.ascent.core.data.local.dao.BodySampleDao
import com.neon.ascent.core.data.local.dao.DailyVitalRollupDao
import com.neon.ascent.core.data.local.entity.BodySampleEntity
import com.neon.ascent.core.data.local.entity.DailyVitalRollupEntity
import com.neon.ascent.core.domain.health.HealthManager
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BodyLogRepository @Inject constructor(
    private val bodySampleDao: BodySampleDao,
    private val dailyVitalRollupDao: DailyVitalRollupDao,
    private val healthManager: HealthManager
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getSamplesForMetric(metric: String, fromMillis: Long, toMillis: Long): Flow<List<BodySampleEntity>> {
        return bodySampleDao.getSamplesForMetric(metric, fromMillis, toMillis)
    }

    fun getSamplesForDay(localDate: String): Flow<List<BodySampleEntity>> {
        return bodySampleDao.getSamplesForDay(localDate)
    }

    fun getFilteredSamples(
        metric: String?,
        site: String?,
        method: String?,
        position: String?,
        fromMillis: Long,
        toMillis: Long
    ): Flow<List<BodySampleEntity>> {
        return bodySampleDao.getFilteredSamples(metric, site, method, position, fromMillis, toMillis)
    }

    suspend fun saveSample(sample: BodySampleEntity) {
        bodySampleDao.upsertSample(sample)
        recalculateDailyRollups(sample.localDate)

        if (sample.source == "NEON") {
            writeSampleToHealthConnect(sample)
        }
    }

    suspend fun deleteSample(id: String, localDate: String? = null) {
        bodySampleDao.deleteSample(id)
        if (localDate != null) {
            recalculateDailyRollups(localDate)
        }
    }

    suspend fun saveSamples(samples: List<BodySampleEntity>) {
        if (samples.isEmpty()) return
        bodySampleDao.upsertSamples(samples)

        val affectedDates = samples.map { it.localDate }.distinct()
        affectedDates.forEach { date ->
            recalculateDailyRollups(date)
        }

        samples.filter { it.source == "NEON" }.forEach { sample ->
            writeSampleToHealthConnect(sample)
        }
    }

    suspend fun saveBloodPressurePair(
        localDate: String,
        loggedAt: Long,
        systolicMmHg: Double,
        diastolicMmHg: Double,
        position: String,
        side: String? = null,
        conditionTag: String? = null,
        source: String = "NEON",
        note: String? = null
    ) {
        val sysSample = BodySampleEntity(
            localDate = localDate,
            loggedAt = loggedAt,
            metric = "BP_SYS",
            value = systolicMmHg,
            unit = "MMHG",
            position = position,
            side = side,
            conditionTag = conditionTag,
            source = source,
            note = note
        )
        val diaSample = BodySampleEntity(
            localDate = localDate,
            loggedAt = loggedAt,
            metric = "BP_DIA",
            value = diastolicMmHg,
            unit = "MMHG",
            position = position,
            side = side,
            conditionTag = conditionTag,
            source = source,
            note = note
        )

        bodySampleDao.upsertSamples(listOf(sysSample, diaSample))
        recalculateDailyRollups(localDate)

        if (source == "NEON") {
            try {
                val hcPosition = when (position) {
                    "SITTING" -> BloodPressureRecord.BODY_POSITION_SITTING_DOWN
                    "STANDING" -> BloodPressureRecord.BODY_POSITION_STANDING_UP
                    "LYING" -> BloodPressureRecord.BODY_POSITION_LYING_DOWN
                    else -> BloodPressureRecord.BODY_POSITION_UNKNOWN
                }
                healthManager.insertBloodPressure(
                    systolicMmHg = systolicMmHg,
                    diastolicMmHg = diastolicMmHg,
                    bodyPosition = hcPosition,
                    time = Instant.ofEpochMilli(loggedAt)
                )
            } catch (e: Throwable) {
                Log.e("BodyLogRepository", "Failed writing BP to Health Connect", e)
            }
        }
    }

    private suspend fun writeSampleToHealthConnect(sample: BodySampleEntity) {
        try {
            val instant = Instant.ofEpochMilli(sample.loggedAt)
            when (sample.metric) {
                "WEIGHT" -> {
                    healthManager.insertWeight(sample.value, instant)
                }
                "BF_PCT" -> {
                    healthManager.insertBodyFat(sample.value, instant)
                }
                "HEIGHT" -> {
                    healthManager.insertHeight(sample.value, instant)
                }
                // TAPE is NOT written to HC
                else -> { /* no-op for tape or sys/dia handled separately */ }
            }
        } catch (e: Throwable) {
            Log.e("BodyLogRepository", "Error writing ${sample.metric} to Health Connect", e)
        }
    }

    suspend fun recalculateDailyRollups(localDate: String) {
        val samples = bodySampleDao.getSamplesForDayList(localDate)
        if (samples.isEmpty()) return

        val nowMillis = System.currentTimeMillis()

        // 1. WEIGHT Rollup (Prefer AM_FASTED, else latest loggedAt)
        val weightSamples = samples.filter { it.metric == "WEIGHT" }
        if (weightSamples.isNotEmpty()) {
            val chosen = weightSamples.firstOrNull { it.conditionTag == "AM_FASTED" }
                ?: weightSamples.maxByOrNull { it.loggedAt }

            if (chosen != null) {
                dailyVitalRollupDao.upsert(
                    DailyVitalRollupEntity(
                        localDate = localDate,
                        metric = "WEIGHT",
                        value = chosen.value,
                        source = chosen.source,
                        quality = "OK",
                        updatedAt = nowMillis
                    )
                )
            }
        }

        // 2. BF_PCT Rollup (Latest overall + per method)
        val bfSamples = samples.filter { it.metric == "BF_PCT" }
        if (bfSamples.isNotEmpty()) {
            val latestOverall = bfSamples.maxByOrNull { it.loggedAt }
            if (latestOverall != null) {
                dailyVitalRollupDao.upsert(
                    DailyVitalRollupEntity(
                        localDate = localDate,
                        metric = "BF_PCT",
                        value = latestOverall.value,
                        source = latestOverall.source,
                        quality = "OK",
                        updatedAt = nowMillis
                    )
                )
            }

            bfSamples.groupBy { it.method }.forEach { (method, methodGroup) ->
                if (!method.isNullOrBlank() && methodGroup.isNotEmpty()) {
                    val latestMethod = methodGroup.maxByOrNull { it.loggedAt }
                    if (latestMethod != null) {
                        dailyVitalRollupDao.upsert(
                            DailyVitalRollupEntity(
                                localDate = localDate,
                                metric = "BF_PCT_${method}",
                                value = latestMethod.value,
                                source = latestMethod.source,
                                quality = "OK",
                                updatedAt = nowMillis
                            )
                        )
                    }
                }
            }
        }

        // 3. TAPE Rollups (Group by site)
        val tapeSamples = samples.filter { it.metric == "TAPE" && !it.site.isNullOrBlank() }
        tapeSamples.groupBy { it.site }.forEach { (site, siteGroup) ->
            val latestTape = siteGroup.maxByOrNull { it.loggedAt }
            if (latestTape != null && site != null) {
                dailyVitalRollupDao.upsert(
                    DailyVitalRollupEntity(
                        localDate = localDate,
                        metric = "TAPE_${site}",
                        value = latestTape.value,
                        source = latestTape.source,
                        quality = "OK",
                        updatedAt = nowMillis
                    )
                )
            }
        }

        // 4. Blood Pressure Rollups (Split by posture: SITTING, STANDING, LYING)
        val bpSysSamples = samples.filter { it.metric == "BP_SYS" }
        val bpDiaSamples = samples.filter { it.metric == "BP_DIA" }

        listOf("SITTING", "STANDING", "LYING").forEach { posture ->
            val postfix = when (posture) {
                "SITTING" -> "_SIT"
                "STANDING" -> "_STAND"
                else -> "_LYING"
            }

            val sys = bpSysSamples.filter { it.position == posture }.maxByOrNull { it.loggedAt }
            val dia = bpDiaSamples.filter { it.position == posture }.maxByOrNull { it.loggedAt }

            if (sys != null) {
                dailyVitalRollupDao.upsert(
                    DailyVitalRollupEntity(
                        localDate = localDate,
                        metric = "BP_SYS${postfix}",
                        value = sys.value,
                        source = sys.source,
                        quality = "OK",
                        updatedAt = nowMillis
                    )
                )
            }
            if (dia != null) {
                dailyVitalRollupDao.upsert(
                    DailyVitalRollupEntity(
                        localDate = localDate,
                        metric = "BP_DIA${postfix}",
                        value = dia.value,
                        source = dia.source,
                        quality = "OK",
                        updatedAt = nowMillis
                    )
                )
            }
        }
    }

    suspend fun ingestHealthConnectBodyData(startDate: Instant, endDate: Instant) {
        val newSamples = mutableListOf<BodySampleEntity>()
        val zone = ZoneId.systemDefault()

        // 1. Fetch HC Weights
        val hcWeights = healthManager.weights(startDate, endDate)
        for ((time, kg) in hcWeights) {
            val millis = time.toEpochMilli()
            val dateStr = time.atZone(zone).toLocalDate().format(dateFormatter)

            if (!hasNeonCollision("WEIGHT", millis)) {
                newSamples.add(
                    BodySampleEntity(
                        localDate = dateStr,
                        loggedAt = millis,
                        metric = "WEIGHT",
                        value = kg,
                        unit = "KG",
                        source = "HC_FIT"
                    )
                )
            }
        }

        // 2. Fetch HC Body Fat (method = UNKNOWN for HC rows)
        val latestBf = healthManager.latestBodyFat(startDate, endDate)
        if (latestBf != null) {
            val millis = endDate.toEpochMilli()
            val dateStr = endDate.atZone(zone).toLocalDate().format(dateFormatter)

            if (!hasNeonCollision("BF_PCT", millis)) {
                newSamples.add(
                    BodySampleEntity(
                        localDate = dateStr,
                        loggedAt = millis,
                        metric = "BF_PCT",
                        value = latestBf,
                        unit = "PCT",
                        method = "UNKNOWN",
                        source = "HC_FIT"
                    )
                )
            }
        }

        // 3. Fetch HC Blood Pressures
        val hcBps = healthManager.bloodPressures(startDate, endDate)
        for (bp in hcBps) {
            val millis = bp.time.toEpochMilli()
            val dateStr = bp.time.atZone(zone).toLocalDate().format(dateFormatter)
            val posStr = when (bp.bodyPosition) {
                BloodPressureRecord.BODY_POSITION_SITTING_DOWN -> "SITTING"
                BloodPressureRecord.BODY_POSITION_STANDING_UP -> "STANDING"
                BloodPressureRecord.BODY_POSITION_LYING_DOWN -> "LYING"
                else -> "UNSPECIFIED"
            }

            if (!hasNeonCollision("BP_SYS", millis)) {
                newSamples.add(
                    BodySampleEntity(
                        localDate = dateStr,
                        loggedAt = millis,
                        metric = "BP_SYS",
                        value = bp.systolic.inMillimetersOfMercury,
                        unit = "MMHG",
                        position = posStr,
                        source = "HC_FIT"
                    )
                )
                newSamples.add(
                    BodySampleEntity(
                        localDate = dateStr,
                        loggedAt = millis,
                        metric = "BP_DIA",
                        value = bp.diastolic.inMillimetersOfMercury,
                        unit = "MMHG",
                        position = posStr,
                        source = "HC_FIT"
                    )
                )
            }
        }

        // 4. Fetch HC Height
        val hcHeight = healthManager.latestHeight(startDate, endDate)
        if (hcHeight != null) {
            val millis = endDate.toEpochMilli()
            val dateStr = endDate.atZone(zone).toLocalDate().format(dateFormatter)

            if (!hasNeonCollision("HEIGHT", millis)) {
                newSamples.add(
                    BodySampleEntity(
                        localDate = dateStr,
                        loggedAt = millis,
                        metric = "HEIGHT",
                        value = hcHeight,
                        unit = "M",
                        source = "HC_FIT"
                    )
                )
            }
        }

        if (newSamples.isNotEmpty()) {
            saveSamples(newSamples)
        }
    }

    private suspend fun hasNeonCollision(metric: String, timeMillis: Long): Boolean {
        // Neon row wins on same-minute collision (±60,000ms)
        val windowStart = timeMillis - 60_000L
        val windowEnd = timeMillis + 60_000L
        val existing = bodySampleDao.getSamplesBetween(windowStart, windowEnd)
        return existing.any { it.metric == metric && it.source == "NEON" }
    }
}
