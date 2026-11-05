package com.neon.ascent.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "neon_charge_ml_snapshots")
data class NeonChargeMlSnapshotEntity(
    @PrimaryKey val snapshotId: String,
    val modelJson: String,
    val effectiveN: Int,
    val holdOutMae: Double,
    val timestamp: Long
)
