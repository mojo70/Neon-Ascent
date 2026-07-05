package com.neon.ascent.core.data.mapper

import com.neon.ascent.core.domain.model.SpecialType
import java.time.DayOfWeek
import com.neon.ascent.core.data.local.entity.*
import com.neon.ascent.core.domain.goals.models.*

fun AscensionDirectiveEntity.toDomain() = AscensionDirective(
    id = id,
    title = title,
    description = description,
    visionStatement = visionStatement,
    status = try { DirectiveStatus.valueOf(status) } catch (e: Exception) { DirectiveStatus.ACTIVE },
    successMetrics = successMetrics,
    targetEndDate = targetEndDate,
    isQuarterly = isQuarterly,
    createdAt = createdAt,
    archivedAt = archivedAt,
    currentProgress = currentProgress,
    totalXPContributed = totalXPContributed,
    xpTarget = xpTarget,
    archetypeTag = archetypeTag,
    tags = tags,
    linkedAttributes = linkedAttributes.map { try { SpecialType.valueOf(it) } catch (e: Exception) { SpecialType.LUCK } },
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
    successMetrics = successMetrics,
    targetEndDate = targetEndDate,
    isQuarterly = isQuarterly,
    createdAt = createdAt,
    archivedAt = archivedAt,
    currentProgress = currentProgress,
    totalXPContributed = totalXPContributed,
    xpTarget = xpTarget,
    archetypeTag = archetypeTag,
    tags = tags,
    linkedAttributes = linkedAttributes.map { it.name },
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
    contributionWeight = contributionWeight,
    totalXPContributed = totalXPContributed,
    xpTarget = xpTarget,
    aiMentorMode = try { MentorMode.valueOf(aiMentorMode) } catch (e: Exception) { MentorMode.REVIEW },
    aiGenerated = aiGenerated,
    notes = notes,
    successCriteria = successCriteria,
    completionHistorySummary = completionHistorySummary,
    tags = tags,
    linkedAttributes = linkedAttributes.map { try { SpecialType.valueOf(it) } catch (e: Exception) { SpecialType.LUCK } },
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
    contributionWeight = contributionWeight,
    totalXPContributed = totalXPContributed,
    xpTarget = xpTarget,
    aiMentorMode = aiMentorMode.name,
    aiGenerated = aiGenerated,
    notes = notes,
    successCriteria = successCriteria,
    completionHistorySummary = completionHistorySummary,
    tags = tags,
    linkedAttributes = linkedAttributes.map { it.name },
    linkedArchetype = linkedArchetype
)


fun AscensionTaskEntity.toDomain() = AscensionTask(
    id = id,
    parentId = parentId,
    title = title,
    description = description,
    type = AscensionTaskType.valueOf(type),
    isPulse = isPulse,
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
    impactWeight = impactWeight,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    graceBufferDays = graceBufferDays,
    lastCompleted = lastCompleted,
    tags = tags,
    linkedAttributes = linkedAttributes.map { try { SpecialType.valueOf(it) } catch (e: Exception) { SpecialType.LUCK } },
    userNotesTemplate = userNotesTemplate
)

fun AscensionTask.toEntity() = AscensionTaskEntity(
    id = id,
    parentId = parentId,
    title = title,
    description = description,
    type = type.name,
    isPulse = isPulse,
    recurrenceType = recurrence?.type?.name,
    recurrenceCron = recurrence?.cron,
    recurrenceIntervalDays = recurrence?.intervalDays,
    recurrenceDaysOfWeek = recurrence?.daysOfWeek?.joinToString(",") { it.name },
    timeWindows = timeWindows.joinToString(","),
    adaptiveWakeEnabled = adaptiveWakeEnabled,
    reminderEnabled = reminderEnabled,
    xpValue = xpValue,
    impactWeight = impactWeight,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    graceBufferDays = graceBufferDays,
    lastCompleted = lastCompleted,
    tags = tags,
    linkedAttributes = linkedAttributes.map { it.name },
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
