package com.neon.ascent.core.data.local.dao

import androidx.room.*
import com.neon.ascent.core.data.local.entity.NeonChargeCalibrationEntity
import com.neon.ascent.core.data.local.entity.NeonChargeLabelEntity
import com.neon.ascent.core.data.local.entity.NeonChargeMlSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NeonChargeLabelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(label: NeonChargeLabelEntity)

    @Query("SELECT * FROM neon_charge_labels WHERE date = :date")
    suspend fun getLabelForDate(date: String): NeonChargeLabelEntity?

    @Query("SELECT * FROM neon_charge_labels WHERE date = :date")
    fun observeLabelForDate(date: String): Flow<NeonChargeLabelEntity?>

    @Query("SELECT * FROM neon_charge_labels ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentLabels(limit: Int = 120): List<NeonChargeLabelEntity>

    @Query("DELETE FROM neon_charge_labels WHERE date = :date")
    suspend fun deleteLabel(date: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCalibration(calibration: NeonChargeCalibrationEntity)

    @Query("SELECT * FROM neon_charge_calibration WHERE id = 'active_calibration'")
    suspend fun getActiveCalibration(): NeonChargeCalibrationEntity?

    @Query("SELECT * FROM neon_charge_calibration WHERE id = 'active_calibration'")
    fun observeActiveCalibration(): Flow<NeonChargeCalibrationEntity?>

    @Query("DELETE FROM neon_charge_calibration WHERE id = 'active_calibration'")
    suspend fun resetCalibration()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMlSnapshot(snapshot: NeonChargeMlSnapshotEntity)

    @Query("SELECT * FROM neon_charge_ml_snapshots ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getMlSnapshots(limit: Int = 3): List<NeonChargeMlSnapshotEntity>

    @Query("DELETE FROM neon_charge_ml_snapshots WHERE snapshotId NOT IN (SELECT snapshotId FROM neon_charge_ml_snapshots ORDER BY timestamp DESC LIMIT :keep)")
    suspend fun pruneMlSnapshots(keep: Int = 3)
}
