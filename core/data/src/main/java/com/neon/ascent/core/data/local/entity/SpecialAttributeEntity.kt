package com.neon.ascent.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.neon.ascent.core.data.local.converter.InstantConverter
import com.neon.ascent.core.data.local.converter.SpecialTypeConverter
import com.neon.ascent.core.domain.model.SpecialAttribute
import com.neon.ascent.core.domain.model.SpecialType
import java.time.Instant

@Entity(tableName = "special_attributes")
@TypeConverters(InstantConverter::class, SpecialTypeConverter::class)
data class SpecialAttributeEntity(
    @PrimaryKey
    val type: SpecialType,                    // Enum as key

    val baseValue: Int = 5,
    val currentValue: Int,
    val percentile: Int? = null,
    val totalXp: Long = 0L,
    val lastUpdated: Instant = Instant.now()
)

fun SpecialAttributeEntity.toDomain() = SpecialAttribute(
    type = type,
    baseValue = baseValue,
    currentValue = currentValue,
    percentile = percentile,
    totalXp = totalXp,
    lastUpdated = lastUpdated
)

fun SpecialAttribute.toEntity() = SpecialAttributeEntity(
    type = type,
    baseValue = baseValue,
    currentValue = currentValue,
    percentile = percentile,
    totalXp = totalXp,
    lastUpdated = lastUpdated
)
