package com.neon.ascent.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "biomarker_samples")
data class BiomarkerSampleEntity(
    @PrimaryKey val id: String,
    val markerKey: String,
    val displayName: String,
    val value: Double,
    val unit: String,
    val drawnAt: Instant,
    val source: String,
    val notes: String?
)
