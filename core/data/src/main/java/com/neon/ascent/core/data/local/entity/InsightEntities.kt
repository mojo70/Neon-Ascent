package com.neon.ascent.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "biometric_events")
data class BiometricEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Instant,
    val source: String,
    val type: String, // HRV, BodyBattery, SleepScore, etc.
    val value: Double,
    val metadata: Map<String, String> = emptyMap()
)

@Entity(tableName = "action_events")
data class ActionEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Instant,
    val actionType: String,
    val content: String,
    val metadata: Map<String, String> = emptyMap()
)

@Entity(tableName = "socratic_insights")
data class SocraticInsightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val generatedAt: Instant,
    val timeWindowStart: Instant,
    val timeWindowEnd: Instant,
    val content: String,
    val basedOnEventIds: List<Long>,
    val version: Int
)
