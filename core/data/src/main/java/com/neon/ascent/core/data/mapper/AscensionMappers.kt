package com.neon.ascent.core.data.mapper

import java.time.DayOfWeek
import com.neon.ascent.core.data.local.entity.*
import com.neon.ascent.core.domain.goals.models.*

fun AscensionDirectiveEntity.toDomain() = AscensionDirective(
    id = id,
    title = title,
    description = description,
    archetypeTag = archetypeTag,
    status = DirectiveStatus.valueOf(status),
    targetEndDate = targetEndDate,
    currentProgress = currentProgress,
    totalXPContributed = totalXPContributed,
    notes = notes,
    createdAt = createdAt
)

fun AscensionDirective.toEntity() = AscensionDirectiveEntity(
    id = id,
    title = title,
    description = description,
    archetypeTag = archetypeTag,
    status = status.name,
    targetEndDate = targetEndDate,
    currentProgress = currentProgress,
    totalXPContributed = totalXPContributed,
    notes = notes,
    createdAt = createdAt
)

fun AscensionMissionEntity.toDomain() = AscensionMission(
    id = id,
    directiveId = directiveId,
    title = title,
    description = description,
    deadline = deadline,
    progress = progress,
    xpPool = xpPool,
    status = AscensionMissionStatus.valueOf(status),
    aiGenerated = aiGenerated,
    mentorModeEnabled = mentorModeEnabled,
    isRecovery = isRecovery
)

fun AscensionMission.toEntity() = AscensionMissionEntity(
    id = id,
    directiveId = directiveId,
    title = title,
    description = description,
    deadline = deadline,
    progress = progress,
    xpPool = xpPool,
    status = status.name,
    aiGenerated = aiGenerated,
    mentorModeEnabled = mentorModeEnabled,
    isRecovery = isRecovery
)

fun AscensionTaskEntity.toDomain() = AscensionTask(
    id = id,
    parentId = parentId,
    title = title,
    description = description,
    type = AscensionTaskType.valueOf(type),
    recurrence = if (recurrenceType != null) RecurrenceV3(
        type = RecurrenceTypeV3.valueOf(recurrenceType),
        cron = recurrenceCron,
        intervalDays = recurrenceIntervalDays,
        daysOfWeek = recurrenceDaysOfWeek?.split(",")?.filter { it.isNotBlank() }?.map { DayOfWeek.valueOf(it) }?.toSet() ?: emptySet()
    ) else null,
    timeWindows = timeWindows?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
    adaptiveWakeEnabled = adaptiveWakeEnabled,
    reminderEnabled = reminderEnabled,
    xpValue = xpValue,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    graceBufferDays = graceBufferDays,
    lastCompleted = lastCompleted,
    userNotesTemplate = userNotesTemplate
)

fun AscensionTask.toEntity() = AscensionTaskEntity(
    id = id,
    parentId = parentId,
    title = title,
    description = description,
    type = type.name,
    recurrenceType = recurrence?.type?.name,
    recurrenceCron = recurrence?.cron,
    recurrenceIntervalDays = recurrence?.intervalDays,
    recurrenceDaysOfWeek = recurrence?.daysOfWeek?.joinToString(",") { it.name },
    timeWindows = timeWindows.joinToString(","),
    adaptiveWakeEnabled = adaptiveWakeEnabled,
    reminderEnabled = reminderEnabled,
    xpValue = xpValue,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    graceBufferDays = graceBufferDays,
    lastCompleted = lastCompleted,
    userNotesTemplate = userNotesTemplate
)

fun AscensionTaskCompletionEntity.toDomain() = AscensionTaskCompletion(
    taskId = taskId,
    timestamp = timestamp,
    notes = notes,
    mood = mood,
    linkedHealthSnapshot = linkedHealthSnapshot
)

fun NeuralLogEntity.toDomain() = NeuralLog(
    id = id,
    timestamp = timestamp,
    type = type,
    title = title,
    content = content,
    metadata = metadata
)
