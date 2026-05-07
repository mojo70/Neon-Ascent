package com.neon.ascent.domain.model

import com.neon.ascent.data.local.entity.GoalEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class Goal(
    val id: String,
    val title: String,
    val description: String,
    val aspirationLink: String,
    val targetValue: Float,
    val currentValue: Float,
    val unit: String,
    val deadline: LocalDate?,
    val isActive: Boolean
)

fun GoalEntity.toDomain() = Goal(
    id = id,
    title = title,
    description = description,
    aspirationLink = aspirationLink,
    targetValue = targetValue,
    currentValue = currentValue,
    unit = unit,
    deadline = deadline?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() },
    isActive = isActive
)

fun Goal.toEntity() = GoalEntity(
    id = id,
    title = title,
    description = description,
    aspirationLink = aspirationLink,
    targetValue = targetValue,
    currentValue = currentValue,
    unit = unit,
    deadline = deadline?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
    isActive = isActive
)
