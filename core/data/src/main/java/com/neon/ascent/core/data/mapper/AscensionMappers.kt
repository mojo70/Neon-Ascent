package com.neon.ascent.core.data.mapper

import java.time.DayOfWeek
import com.neon.ascent.core.data.local.entity.*
import com.neon.ascent.core.domain.goals.models.*

fun AscensionDirectiveEntity.toDomain() = AscensionDirective(
    id = id,
    title = title,
    description = description,
    visionStatement = visionStatement,
    status = try { DirectiveStatus.valueOf(status) } catch (e: Exception) { DirectiveStatus.ACTIVE },
    targetEndDate = targetEndDate,
    isQuarterly = isQuarterly,
    createdAt = createdAt,
    archivedAt = archivedAt,
    currentProgress = currentProgress,
    totalXPContributed = totalXPContributed,
    xpTarget = xpTarget,
    archetypeTag = archetypeTag,
    tags = tags,
    aiMentorMode = try { MentorMode.valueOf(aiMentorMode) } catch (e: Exception) { MentorMode.REVIEW },
    aiGenerated = aiGenerated,
    notes = notes,
    completionHistorySummary = completionHistorySummary,
    lastReviewDate = lastReviewDate
)

fun AscensionDirective.toEntity() = AscensionDirectiveEntity(
    id = id,
    title = title,
    description = description,
    visionStatement = visionStatement,
    status = status.name,
    targetEndDate = targetEndDate,
    isQuarterly = isQuarterly,
    createdAt = createdAt,
    archivedAt = archivedAt,
    currentProgress = currentProgress,
    totalXPContributed = totalXPContributed,
    xpTarget = xpTarget,
    archetypeTag = archetypeTag,
    tags = tags,
    aiMentorMode = aiMentorMode.name,
    aiGenerated = aiGenerated,
    notes = notes,
    completionHistorySummary = completionHistorySummary,
    lastReviewDate = lastReviewDate
)

fun AscensionMissionEntity.toDomain() = AscensionMission(
    id = id,
    directiveId = directiveId,
    title = title,
    description = description,
    objective = objective,
    status = try { AscensionMissionStatus.valueOf(status) } catch (e: Exception) { AscensionMissionStatus.ACTIVE },
    startDate = startDate,
    targetEndDate = targetEndDate,
    createdAt = createdAt,
    completedAt = completedAt,
    archivedAt = archivedAt,
    progress = progress,
    totalXPContributed = totalXPContributed,
    xpTarget = xpTarget,
    aiMentorMode = try { MentorMode.valueOf(aiMentorMode) } catch (e: Exception) { MentorMode.REVIEW },
    aiGenerated = aiGenerated,
    notes = notes,
    successCriteria = successCriteria,
    completionHistorySummary = completionHistorySummary,
    tags = tags,
    linkedArchetype = linkedArchetype
)

fun AscensionMission.toEntity() = AscensionMissionEntity(
    id = id,
    directiveId = directiveId,
    title = title,
    description = description,
    objective = objective,
    status = status.name,
    startDate = startDate,
    targetEndDate = targetEndDate,
    createdAt = createdAt,
    completedAt = completedAt,
    archivedAt = archivedAt,
    progress = progress,
    totalXPContributed = totalXPContributed,
    xpTarget = xpTarget,
    aiMentorMode = aiMentorMode.name,
    aiGenerated = aiGenerated,
    notes = notes,
    successCriteria = successCriteria,
    completionHistorySummary = completionHistorySummary,
    tags = tags,
    linkedArchetype = linkedArchetype
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
