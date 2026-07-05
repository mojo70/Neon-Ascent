package com.neon.ascent.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "protocols",
    indices = [Index("category")]
)
data class ProtocolEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,
    val canonicalSteps: List<String>,
    val source: String,
    val specialTags: List<String>,
    val defaultDurationDays: Int?,
    val isCanonical: Boolean
)

@Entity(
    tableName = "adapted_protocols",
    foreignKeys = [
        ForeignKey(
            entity = ProtocolEntity::class,
            parentColumns = ["id"],
            childColumns = ["protocolId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AscensionDirectiveEntity::class,
            parentColumns = ["id"],
            childColumns = ["directiveId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("protocolId"), Index("directiveId")]
)
data class AdaptedProtocolEntity(
    @PrimaryKey val id: String,
    val protocolId: String,
    val directiveId: String,
    val adaptedTitle: String,
    val adaptedSteps: List<String>,
    val userNotes: String?,
    val lastSyncTimestamp: Instant
)
