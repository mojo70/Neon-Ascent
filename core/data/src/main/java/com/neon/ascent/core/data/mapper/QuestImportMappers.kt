package com.neon.ascent.core.data.mapper

import com.neon.ascent.core.domain.goals.models.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

const val QUEST_IMPORTED_DIRECTIVE_ID = "imported_quests"
const val QUEST_IMPORTED_DIRECTIVE_TITLE = "IMPORTED_QUESTS"

data class QuestInput(
    val id: String,
    val title: String,
    val description: String,
    val isLongTerm: Boolean = true,
    val status: String = "ACTIVE", // ACTIVE, COMPLETED, FAILED
    val createdAt: Long = System.currentTimeMillis()
)

data class TaskInput(
    val id: String,
    val questId: String?,
    val description: String,
    val isCompleted: Boolean = false,
    val isDaily: Boolean = false,
    val dueDate: Long? = null,
    val aiBreakdownNotes: String? = null
)

data class SayingInput(
    val id: String,
    val text: String,
    val category: String,
    val engagementScore: Int = 100,
    val isEnabled: Boolean = true
)

object QuestImportMappers {

    fun resolveImportId(id: String, prefix: String, exists: (String) -> Boolean = { false }): String {
        return if (exists(id)) "$prefix:$id" else id
    }

    fun createQuestImportDirective(): AscensionDirective {
        return AscensionDirective(
            id = QUEST_IMPORTED_DIRECTIVE_ID,
            title = QUEST_IMPORTED_DIRECTIVE_TITLE,
            description = "Imported quests directive from legacy AppDatabase",
            status = DirectiveStatus.ACTIVE
        )
    }

    fun mapQuestToAscensionMission(
        quest: QuestInput,
        exists: (String) -> Boolean = { false }
    ): AscensionMission {
        val resolvedId = resolveImportId(quest.id, "quest", exists)
        val missionStatus = when (quest.status.uppercase()) {
            "COMPLETED" -> AscensionMissionStatus.COMPLETED
            "FAILED" -> AscensionMissionStatus.ARCHIVED
            else -> AscensionMissionStatus.ACTIVE
        }
        val progress = if (missionStatus == AscensionMissionStatus.COMPLETED) 1.0f else 0.0f
        val createdInstant = Instant.ofEpochMilli(quest.createdAt)
        val startDate = createdInstant.atZone(ZoneId.systemDefault()).toLocalDate()

        return AscensionMission(
            id = resolvedId,
            directiveId = QUEST_IMPORTED_DIRECTIVE_ID,
            title = quest.title,
            description = quest.description,
            status = missionStatus,
            startDate = startDate,
            createdAt = createdInstant,
            progress = progress
        )
    }

    fun mapTaskToAscensionTask(
        task: TaskInput,
        exists: (String) -> Boolean = { false },
        mappedQuestId: ((String) -> String)? = null
    ): AscensionTask {
        val resolvedId = resolveImportId(task.id, "qtask", exists)
        val parentId = task.questId?.let { qId ->
            mappedQuestId?.invoke(qId) ?: qId
        } ?: QUEST_IMPORTED_DIRECTIVE_ID

        val taskType = if (task.isDaily) {
            AscensionTaskType.RECURRING
        } else {
            AscensionTaskType.ONE_TIME
        }

        val recurrence = if (task.isDaily) {
            RecurrenceV3(type = RecurrenceTypeV3.DAILY)
        } else {
            null
        }

        val lastCompleted = if (task.isCompleted) Instant.now() else null

        return AscensionTask(
            id = resolvedId,
            parentId = parentId,
            title = task.description,
            description = task.aiBreakdownNotes ?: "",
            type = taskType,
            recurrence = recurrence,
            lastCompleted = lastCompleted,
            userNotesTemplate = task.aiBreakdownNotes
        )
    }
}
