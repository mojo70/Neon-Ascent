package com.neon.ascent.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "neon_charge_labels")
data class NeonChargeLabelEntity(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val wakeSeed: Int,
    val hoursAwake: Double,
    val sessionDrainRaw: Double,
    val stepDrainRaw: Double,
    val hrLoadMin: Int,
    val napMin: Int,
    val protocolFlags: String,
    val activeKcal: Double? = null,
    val priorDayResidual: Double? = null,
    val feltCharge: Int? = null,
    val feltSource: String = "EXPLICIT", // EXPLICIT, CLEARANCE, RPE_CONSISTENCY
    val labelWeight: Double = 1.0,
    val nextClearance: Int? = null,
    val isQuarantined: Boolean = false,
    val quarantineExpiresAt: Long? = null,
    val confirmed: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
