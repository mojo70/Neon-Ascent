package com.neon.ascent.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "data_shards")
data class DataShard(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val isDecrypted: Boolean = false,
    val decryptionTimeMillis: Long = 5000,
    val droppedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "memory_fragments")
data class MemoryFragment(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val corruptedContent: String,
    val decryptedContent: String,
    val requiredStat: String, // e.g., "PERCEPTION"
    val requiredStatValue: Int,
    val isUnlocked: Boolean = false
)

data class NetWatchAlert(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val severity: String, // "LOW", "MEDIUM", "CRITICAL"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "corpo_trust")
data class CorpoTrust(
    @PrimaryKey val corpoId: String,
    val trustLevel: Float = 0f // 0.0 to 1.0
)
