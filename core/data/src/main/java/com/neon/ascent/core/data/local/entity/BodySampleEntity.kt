package com.neon.ascent.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "body_samples")
data class BodySampleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val localDate: String,      // yyyy-MM-dd
    val loggedAt: Long,         // epoch millis
    val metric: String,         // WEIGHT, BF_PCT, LBM, HEIGHT, TAPE, BP_SYS, BP_DIA
    val site: String? = null,   // NECK, CHEST, WAIST_NAVEL, WAIST_NARROW, HIPS, BICEP, THIGH, CALF, FOREARM, SHOULDERS
    val value: Double,          // kg, %, mmHg, cm, m
    val unit: String,           // KG, PCT, MMHG, CM, M
    val method: String? = null, // DEXA, BIA_SCALE, CALIPER, NAVY_TAPE, PHOTO_EST, OTHER, UNKNOWN
    val position: String? = null, // SITTING, STANDING, LYING
    val side: String? = null,   // L, R, UNSPECIFIED
    val conditionTag: String? = null, // AM_FASTED, AM, PM, POST_TRAIN, UNSPECIFIED
    val source: String,         // NEON, HC_FIT, HC_SAMSUNG, HC_GARMIN_WRITE, HC_GENERIC
    val hcRecordId: String? = null,
    val derived: Boolean = false,
    val note: String? = null
)
