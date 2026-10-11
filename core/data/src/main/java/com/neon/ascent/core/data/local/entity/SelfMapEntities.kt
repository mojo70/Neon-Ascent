package com.neon.ascent.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.neon.ascent.core.domain.selfmap.SelfMapCategory
import java.time.Instant

@Entity(
    tableName = "self_items",
    indices = [
        Index(value = ["category"]),
        Index(value = ["isArchived"]),
        Index(value = ["importance"])
    ]
)
data class SelfItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: SelfMapCategory,
    val importance: Float, // 0.0 to 1.0 (1.0 = center)
    val notes: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val baselineMassContributor: Boolean? = null
)

@Entity(
    tableName = "eoe_entries",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["selfItemId"])
    ]
)
data class EoEEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Instant = Instant.now(),
    val emotionLabel: String,
    val selfItemId: Long,
    val epText: String,
    val perceptionText: String,
    val appraisal: String? = null,
    val intensity: Int? = null,
    val canChangeNotes: String? = null,
    val linkedSnapshotId: Long? = null
)

@Entity(
    tableName = "self_map_snapshots",
    indices = [
        Index(value = ["timestamp"])
    ]
)
data class SelfMapSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Instant = Instant.now(),
    val mass: Float,
    val itemCount: Int,
    val avgImportance: Float,
    val coreCount: Int
)
