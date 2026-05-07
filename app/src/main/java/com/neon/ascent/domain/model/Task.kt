package com.neon.ascent.domain.model

import com.neon.ascent.data.local.entity.TaskEntity
import java.time.LocalDate

enum class Frequency { DAILY, WEEKDAYS, WEEKLY, CUSTOM }

data class Task(
    val id: String,
    val goalId: String,
    val title: String,
    val description: String,
    val frequency: Frequency,
    val estimatedMinutes: Int,
    val completedDates: List<LocalDate>,
    val isArchived: Boolean
)

fun TaskEntity.toDomain() = Task(
    id = id,
    goalId = goalId,
    title = title,
    description = description,
    frequency = try { Frequency.valueOf(frequency) } catch (e: Exception) { Frequency.DAILY },
    estimatedMinutes = estimatedMinutes,
    completedDates = completedDates.map { LocalDate.parse(it) },
    isArchived = isArchived
)

fun Task.toEntity() = TaskEntity(
    id = id,
    goalId = goalId,
    title = title,
    description = description,
    frequency = frequency.name,
    estimatedMinutes = estimatedMinutes,
    completedDates = completedDates.map { it.toString() },
    isArchived = isArchived
)
