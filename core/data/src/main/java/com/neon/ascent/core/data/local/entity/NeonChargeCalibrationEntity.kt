package com.neon.ascent.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "neon_charge_calibration")
data class NeonChargeCalibrationEntity(
    @PrimaryKey val id: String = "active_calibration",
    val isPromoted: Boolean,
    val kPassive: Double,
    val sessionMult: Double,
    val stepMult: Double,
    val hrMult: Double,
    val effectiveN: Int,
    val fitDate: Long,
    val inSampleMae: Double,
    val looMae: Double,
    val isResetToPrior: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
