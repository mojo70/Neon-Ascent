package com.neon.ascent.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "rite_sessions",
    indices = [
        Index("kind"),
        Index("localDate"),
        Index("startedAt")
    ]
)
data class RiteSessionEntity(
    @PrimaryKey val id: String,
    val kind: String, // PRAYER, SIT, KEGEL
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val durationMin: Int,
    val source: String, // ALTAR, STILLJACK, QUICK_STAMP
    val notes: String? = null,
    val localDate: String
)
