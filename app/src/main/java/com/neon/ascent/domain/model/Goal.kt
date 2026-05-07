package com.neon.ascent.domain.model

import com.neon.ascent.data.local.entity.GoalEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class Goal(
    val id: String,
    val title: String,
    val objective: String,
    val description: String,
    val aspirationLink: String,
    val targetValue: Float,
    val currentValue: Float,
    val unit: String,
    val deadline: LocalDate?,
    val linkedSpecial: SpecialType? = null,
    val isActive: Boolean
)

fun GoalEntity.toDomain() = Goal(
    id = id,
    title = title,
    objective = objective,
    description = description,
    aspirationLink = aspirationLink,
    targetValue = targetValue,
    currentValue = currentValue,
    unit = unit,
    deadline = deadline?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() },
    linkedSpecial = linkedSpecial,
    isActive = isActive
)

fun Goal.toEntity() = GoalEntity(
    id = id,
    title = title,
    objective = objective,
    description = description,
    aspirationLink = aspirationLink,
    targetValue = targetValue,
    currentValue = currentValue,
    unit = unit,
    deadline = deadline?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
    linkedSpecial = linkedSpecial,
    isActive = isActive
)
