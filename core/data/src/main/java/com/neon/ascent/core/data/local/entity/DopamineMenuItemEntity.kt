package com.neon.ascent.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.neon.ascent.core.data.local.converter.*
import com.neon.ascent.core.domain.model.*
import java.time.Instant

@Entity(tableName = "dopamine_menu_items")
@TypeConverters(
    InstantConverter::class,
    DopamineCategoryConverter::class,
    EnergyLevelConverter::class,
    SpecialTypeListConverter::class
)
data class DopamineMenuItemEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val durationMinutes: Int,
    val category: DopamineCategory,
    val specialTags: List<SpecialType>,
    val energyLevel: EnergyLevel,
    val lastUsed: Instant? = null,
    val usageCount: Int = 0
)

fun DopamineMenuItemEntity.toDomain() = DopamineMenuItem(
    id = id,
    title = title,
    description = description,
    durationMinutes = durationMinutes,
    category = category,
    specialTags = specialTags,
    energyLevel = energyLevel,
    lastUsed = lastUsed,
    usageCount = usageCount
)

fun DopamineMenuItem.toEntity() = DopamineMenuItemEntity(
    id = id,
    title = title,
    description = description,
    durationMinutes = durationMinutes,
    category = category,
    specialTags = specialTags,
    energyLevel = energyLevel,
    lastUsed = lastUsed,
    usageCount = usageCount
)
