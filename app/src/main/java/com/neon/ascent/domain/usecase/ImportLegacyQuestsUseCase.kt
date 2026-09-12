package com.neon.ascent.domain.usecase

import android.content.Context
import android.util.Log
import com.neon.ascent.core.data.mapper.QUEST_IMPORTED_DIRECTIVE_ID
import com.neon.ascent.core.data.mapper.QuestImportMappers
import com.neon.ascent.core.data.mapper.QuestInput
import com.neon.ascent.core.data.mapper.TaskInput
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.data.local.QuestDao
import com.neon.ascent.data.local.TaskDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportLegacyQuestsUseCase @Inject constructor(
    private val questDao: QuestDao,
    private val taskDao: TaskDao,
    private val ascensionRepository: AscensionRepository,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke() {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_IMPORT_COMPLETED, false)) {
            Log.d(TAG, "// IMPORT_SKIPPED: Legacy quests already imported into V3")
            return
        }

        try {
            val legacyQuests = runCatching { questDao.getAllQuests().first() }.getOrDefault(emptyList())
            val legacyTasks = runCatching { taskDao.getAllTasks().first() }.getOrDefault(emptyList())

            if (legacyQuests.isEmpty() && legacyTasks.isEmpty()) {
                Log.d(TAG, "No legacy quests or tasks to import")
                prefs.edit().putBoolean(KEY_IMPORT_COMPLETED, true).apply()
                return
            }

            // Gather existing V3 IDs to ensure insert-if-absent & handle collisions
            val existingDirectives = ascensionRepository.getAllDirectives().first()
            val existingDirectiveIds = existingDirectives.map { it.id }.toSet()

            val activeMissions = ascensionRepository.getActiveMissions().first()
            val existingMissionIds = activeMissions.map { it.id }.toSet()

            val recurringTasks = ascensionRepository.getAllRecurringTasks().first()
            val existingTaskIds = recurringTasks.map { it.id }.toSet()

            val allV3Ids = mutableSetOf<String>().apply {
                addAll(existingDirectiveIds)
                addAll(existingMissionIds)
                addAll(existingTaskIds)
            }

            fun exists(id: String): Boolean = allV3Ids.contains(id)

            // 1. Ensure directive imported_quests exists (title IMPORTED_QUESTS) if any quests exist
            if (!exists(QUEST_IMPORTED_DIRECTIVE_ID)) {
                try {
                    val directive = QuestImportMappers.createQuestImportDirective()
                    ascensionRepository.insertDirective(directive)
                    allV3Ids.add(QUEST_IMPORTED_DIRECTIVE_ID)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to insert imported_quests directive", e)
                }
            }

            // Map old quest id to new mapped mission id for task parent referencing
            val questIdMap = mutableMapOf<String, String>()
            var importQuestCount = 0

            // 2. Copy quests insert-if-absent
            legacyQuests.forEach { questEntity ->
                try {
                    val input = QuestInput(
                        id = questEntity.id,
                        title = questEntity.title,
                        description = questEntity.description,
                        isLongTerm = questEntity.isLongTerm,
                        status = questEntity.status,
                        createdAt = questEntity.createdAt
                    )
                    val mission = QuestImportMappers.mapQuestToAscensionMission(input, ::exists)
                    questIdMap[questEntity.id] = mission.id

                    if (!exists(mission.id)) {
                        ascensionRepository.insertMission(mission)
                        allV3Ids.add(mission.id)
                        importQuestCount++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error importing legacy quest ${questEntity.id}", e)
                }
            }
            Log.i(TAG, "IMPORT_QUEST $importQuestCount")

            // 3. Copy tasks insert-if-absent
            var importTaskCount = 0
            legacyTasks.forEach { taskEntity ->
                try {
                    val input = TaskInput(
                        id = taskEntity.id,
                        questId = taskEntity.questId,
                        description = taskEntity.description,
                        isCompleted = taskEntity.isCompleted,
                        isDaily = taskEntity.isDaily,
                        dueDate = taskEntity.dueDate,
                        aiBreakdownNotes = taskEntity.aiBreakdownNotes
                    )
                    val task = QuestImportMappers.mapTaskToAscensionTask(
                        task = input,
                        exists = ::exists,
                        mappedQuestId = { qId -> questIdMap[qId] ?: qId }
                    )

                    if (!exists(task.id)) {
                        ascensionRepository.insertTask(task)
                        allV3Ids.add(task.id)

                        // 3. Completed tasks: write a completion if the API allows; else set progress 1.0. Do not invent XP.
                        if (taskEntity.isCompleted) {
                            try {
                                ascensionRepository.completeTask(
                                    task = task,
                                    notes = taskEntity.aiBreakdownNotes,
                                    mood = null,
                                    linkedHealthSnapshot = null
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed writing completion for task ${task.id}", e)
                            }
                        }
                        importTaskCount++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error importing legacy task ${taskEntity.id}", e)
                }
            }
            Log.i(TAG, "IMPORT_QTASK $importTaskCount")

            prefs.edit().putBoolean(KEY_IMPORT_COMPLETED, true).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed executing quest import job", e)
        }
    }

    companion object {
        private const val TAG = "QuestImportManager"
        private const val PREF_NAME = "neon_ascent_quest_import"
        private const val KEY_IMPORT_COMPLETED = "v3_quest_import_completed"
    }
}
