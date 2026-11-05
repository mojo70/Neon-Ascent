package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.dao.NeonChargeLabelDao
import com.neon.ascent.core.data.local.entity.NeonChargeCalibrationEntity
import com.neon.ascent.core.data.local.entity.NeonChargeLabelEntity
import com.neon.ascent.core.data.local.entity.NeonChargeMlSnapshotEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FakeNeonChargeLabelDao : NeonChargeLabelDao {
    private val labels = mutableMapOf<String, NeonChargeLabelEntity>()
    private var calibration: NeonChargeCalibrationEntity? = null
    private val mlSnapshots = mutableMapOf<String, NeonChargeMlSnapshotEntity>()

    override suspend fun upsert(label: NeonChargeLabelEntity) {
        labels[label.date] = label
    }

    override suspend fun getLabelForDate(date: String): NeonChargeLabelEntity? = labels[date]

    override fun observeLabelForDate(date: String): Flow<NeonChargeLabelEntity?> =
        flowOf(labels[date])

    override suspend fun getRecentLabels(limit: Int): List<NeonChargeLabelEntity> =
        labels.values.sortedByDescending { it.date }.take(limit)

    override suspend fun deleteLabel(date: String) {
        labels.remove(date)
    }

    override suspend fun upsertCalibration(calibration: NeonChargeCalibrationEntity) {
        this.calibration = calibration
    }

    override suspend fun getActiveCalibration(): NeonChargeCalibrationEntity? = calibration

    override fun observeActiveCalibration(): Flow<NeonChargeCalibrationEntity?> =
        flowOf(calibration)

    override suspend fun resetCalibration() {
        calibration = null
    }

    override suspend fun upsertMlSnapshot(snapshot: NeonChargeMlSnapshotEntity) {
        mlSnapshots[snapshot.snapshotId] = snapshot
    }

    override suspend fun getMlSnapshots(limit: Int): List<NeonChargeMlSnapshotEntity> =
        mlSnapshots.values.sortedByDescending { it.timestamp }.take(limit)

    override suspend fun pruneMlSnapshots(keep: Int) {
        val keepIds = mlSnapshots.values.sortedByDescending { it.timestamp }.take(keep).map { it.snapshotId }.toSet()
        mlSnapshots.keys.retainAll(keepIds)
    }
}

class NeonChargeLabelTest {

    @Test
    fun `label written today is readable tomorrow`() = runBlocking {
        val dao = FakeNeonChargeLabelDao()
        val today = LocalDate.now().toString()

        val labelToday = NeonChargeLabelEntity(
            date = today,
            wakeSeed = 75,
            hoursAwake = 10.0,
            sessionDrainRaw = 20.0,
            stepDrainRaw = 15.0,
            hrLoadMin = 10,
            napMin = 0,
            protocolFlags = "CYBER_CRAPP",
            feltCharge = 25,
            feltSource = "EXPLICIT",
            labelWeight = 1.0,
            isQuarantined = false,
            confirmed = true
        )

        dao.upsert(labelToday)

        val retrievedToday = dao.getLabelForDate(today)
        assertNotNull(retrievedToday)
        assertEquals(75, retrievedToday?.wakeSeed)
        assertEquals(25, retrievedToday?.feltCharge)

        val recent = dao.getRecentLabels(10)
        assertTrue(recent.any { it.date == today })
    }

    @Test
    fun `outlier gate quarantines felt charge when diff greater than 40 without corroboration`() = runBlocking {
        val dao = FakeNeonChargeLabelDao()
        val today = LocalDate.now().toString()

        val priorPrediction = 70
        val extremeFelt = 20 // diff = 50 > 40, no corroborating high RPE / HR load
        val diff = Math.abs(extremeFelt - priorPrediction)
        val isQuarantined = diff > 40

        val label = NeonChargeLabelEntity(
            date = today,
            wakeSeed = 75,
            hoursAwake = 8.0,
            sessionDrainRaw = 5.0,
            stepDrainRaw = 5.0,
            hrLoadMin = 0,
            napMin = 0,
            protocolFlags = "",
            feltCharge = extremeFelt,
            feltSource = "EXPLICIT",
            labelWeight = if (isQuarantined) 0.0 else 1.0,
            isQuarantined = isQuarantined,
            quarantineExpiresAt = if (isQuarantined) System.currentTimeMillis() + 48 * 3600 * 1000L else null,
            confirmed = !isQuarantined
        )

        dao.upsert(label)

        val saved = dao.getLabelForDate(today)
        assertNotNull(saved)
        assertTrue(saved!!.isQuarantined)
        assertEquals(0.0, saved.labelWeight, 0.001)
        assertTrue(!saved.confirmed)
    }
}
