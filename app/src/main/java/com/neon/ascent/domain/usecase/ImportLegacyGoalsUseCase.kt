package com.neon.ascent.domain.usecase

import android.content.Context
import android.util.Log
import com.neon.ascent.core.data.mapper.GoalImportMappers
import com.neon.ascent.core.data.mapper.V1GoalInput
import com.neon.ascent.core.data.mapper.V1_IMPORTED_DIRECTIVE_ID
import com.neon.ascent.core.data.mapper.V2_IMPORTED_DIRECTIVE_ID
import com.neon.ascent.core.domain.goals.models.AscensionDirective
import com.neon.ascent.core.domain.goals.models.DirectiveStatus
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.data.local.GoalDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportLegacyGoalsUseCase @Inject constructor(
    private val v1GoalDao: GoalDao,
    private val v2GoalDao: com.neon.ascent.core.data.local.dao.GoalDao,
    private val ascensionRepository: AscensionRepository,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke() {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_IMPORT_COMPLETED, false)) {
            Log.d(TAG, "// IMPORT_SKIPPED: V3 import already completed")
            return
        }

        try {
            // 1. Gather existing V3 IDs to ensure insert-if-absent & handle collisions
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

            // 2. Process V1 Active Goals
            val v1Goals = runCatching { v1GoalDao.getActiveGoals().first() }.getOrDefault(emptyList())
            var v1ImportCount = 0

            if (v1Goals.isNotEmpty()) {
                // Ensure directive imported_v1 exists
                if (!exists(V1_IMPORTED_DIRECTIVE_ID)) {
                    try {
                        val v1Directive = GoalImportMappers.createV1ImportDirective()
                        ascensionRepository.insertDirective(v1Directive)
                        allV3Ids.add(V1_IMPORTED_DIRECTIVE_ID)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to insert V1 container directive", e)
                    }
                }

                v1Goals.forEach { v1Entity ->
                    try {
                        val v1Input = V1GoalInput(
                            id = v1Entity.id,
                            title = v1Entity.title,
                            objective = v1Entity.objective,
                            description = v1Entity.description,
                            aspirationLink = v1Entity.aspirationLink,
                            targetValue = v1Entity.targetValue,
                            currentValue = v1Entity.currentValue,
                            unit = v1Entity.unit,
                            deadline = v1Entity.deadline,
                            linkedSpecial = v1Entity.linkedSpecial?.let {
                                runCatching { SpecialType.valueOf(it.name) }.getOrNull()
                            },
                            isActive = v1Entity.isActive,
                            createdAt = v1Entity.createdAt,
                            updatedAt = v1Entity.updatedAt
                        )
                        val mission = GoalImportMappers.mapV1GoalToAscensionMission(v1Input, ::exists)
                        if (!exists(mission.id)) {
                            ascensionRepository.insertMission(mission)
                            allV3Ids.add(mission.id)
                            v1ImportCount++
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error importing V1 goal ${v1Entity.id}", e)
                    }
                }
            }
            Log.i(TAG, "IMPORT_V1 $v1ImportCount")

            // 3. Process V2 Goals
            val v2Goals = runCatching { v2GoalDao.getAllGoals().first() }.getOrDefault(emptyList())
            var v2ImportCount = 0

            if (v2Goals.isNotEmpty()) {
                val hasUnparentedV2 = v2Goals.any {
                    (it.type == "MISSION" && it.parentAspirationId.isNullOrBlank()) ||
                            (it.type == "HABIT" && it.parentGoalId.isNullOrBlank()) ||
                            (it.type == "TASK" && it.parentGoalId.isNullOrBlank())
                }

                if (hasUnparentedV2 && !exists(V2_IMPORTED_DIRECTIVE_ID)) {
                    try {
                        val v2Directive = AscensionDirective(
                            id = V2_IMPORTED_DIRECTIVE_ID,
                            title = "IMPORTED_V2_DIRECTIVES",
                            description = "Imported directives container for V2 stack",
                            status = DirectiveStatus.ACTIVE
                        )
                        ascensionRepository.insertDirective(v2Directive)
                        allV3Ids.add(V2_IMPORTED_DIRECTIVE_ID)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to insert V2 container directive", e)
                    }
                }

                // Process Aspirations first so directives exist for missions
                val aspirations = v2Goals.filter { it.type == "ASPIRATION" }
                aspirations.forEach { entity ->
                    try {
                        val directive = GoalImportMappers.mapV2GoalEntityAspirationToDirective(entity, ::exists)
                        if (!exists(directive.id)) {
                            ascensionRepository.insertDirective(directive)
                            allV3Ids.add(directive.id)
                            v2ImportCount++
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error importing V2 aspiration ${entity.id}", e)
                    }
                }

                // Process Missions
                val missions = v2Goals.filter { it.type == "MISSION" }
                missions.forEach { entity ->
                    try {
                        val mission = GoalImportMappers.mapV2GoalEntityMissionToAscensionMission(entity, null, ::exists)
                        if (!exists(mission.id)) {
                            ascensionRepository.insertMission(mission)
                            allV3Ids.add(mission.id)
                            v2ImportCount++
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error importing V2 mission ${entity.id}", e)
                    }
                }

                // Process Habits
                val habits = v2Goals.filter { it.type == "HABIT" }
                habits.forEach { entity ->
                    try {
                        val task = GoalImportMappers.mapV2GoalEntityHabitToAscensionTask(entity, null, ::exists)
                        if (!exists(task.id)) {
                            ascensionRepository.insertTask(task)
                            allV3Ids.add(task.id)
                            v2ImportCount++
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error importing V2 habit ${entity.id}", e)
                    }
                }

                // Process Tasks
                val tasks = v2Goals.filter { it.type == "TASK" }
                tasks.forEach { entity ->
                    try {
                        val task = GoalImportMappers.mapV2GoalEntityTaskToAscensionTask(entity, null, ::exists)
                        if (!exists(task.id)) {
                            ascensionRepository.insertTask(task)
                            allV3Ids.add(task.id)
                            v2ImportCount++
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error importing V2 task ${entity.id}", e)
                    }
                }
            }
            Log.i(TAG, "IMPORT_V2 $v2ImportCount")

            // Mark import as completed
            prefs.edit().putBoolean(KEY_IMPORT_COMPLETED, true).apply()

        } catch (e: Exception) {
            Log.e(TAG, "Failed executing goal import job", e)
        }
    }

    companion object {
        private const val TAG = "GoalImportManager"
        private const val PREF_NAME = "neon_ascent_goal_import"
        private const val KEY_IMPORT_COMPLETED = "v3_import_completed"
    }
}
