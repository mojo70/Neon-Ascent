package com.neon.ascent.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "saved_reconstitutions")
data class SavedReconstitutionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val vialMg: String,
    val waterMl: String,
    val concentrationMgPerMl: String,
    val mixedDate: String? = null,
    val notes: String? = null,
    val componentsJson: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
