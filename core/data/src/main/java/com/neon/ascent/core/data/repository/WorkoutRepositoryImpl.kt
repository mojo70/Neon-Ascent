package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.dao.WorkoutDao
import com.neon.ascent.core.data.local.entity.AugmentExerciseCrossRef
import com.neon.ascent.core.data.local.entity.RoutineAugmentCrossRef
import com.neon.ascent.core.data.local.entity.RoutineExerciseCrossRef
import com.neon.ascent.core.data.local.entity.RoutineSetEntity
import com.neon.ascent.core.data.local.entity.AugmentSetEntity
import com.neon.ascent.core.data.mapper.*
import com.neon.ascent.core.domain.repository.WorkoutRepository
import com.neon.ascent.core.domain.workout.models.*
import com.neon.ascent.core.domain.workout.rules.CyberCrappRules
import com.neon.ascent.core.domain.workout.rules.RecoveryEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao
) : WorkoutRepository {

    override fun getAllSessions(): Flow<List<WorkoutSession>> =
        workoutDao.getAllSessions().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getSessionById(id: String): WorkoutSession? =
        workoutDao.getSessionById(id)?.toDomain()

    override suspend fun saveSession(session: WorkoutSession) {
        workoutDao.upsertSession(session.toEntity())
    }

    override suspend fun exportHistoryToJson(): String {
        val history = workoutDao.getAllSessionsWithDetails().first().map { sessionWithLogs ->
            sessionWithLogs.session.toDomain() to sessionWithLogs.logs.map { logWithSets ->
                logWithSets.log.toDomain() to logWithSets.sets.map { it.toDomain() }
            }
        }
        return com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(history)
    }

    override fun getExerciseDefinitions(): Flow<List<Exercise>> =
        workoutDao.getExerciseDefinitions().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveExerciseDefinition(exercise: Exercise) {
        workoutDao.insertExerciseDefinition(exercise.toEntity())
    }

    override fun getLogsForSession(sessionId: String): Flow<List<Pair<WorkoutLog, List<SetLog>>>> =
        workoutDao.getLogsForSession(sessionId).map { list ->
            list.map { logWithSets ->
                logWithSets.log.toDomain() to logWithSets.sets
                    .map { it.toDomain() }
                    .sortedWith(compareBy({ it.timestamp }, { it.id }))
            }
        }

    override suspend fun saveWorkoutLog(log: WorkoutLog) {
        workoutDao.upsertWorkoutLog(log.toEntity())
    }

    override suspend fun saveSetLog(set: SetLog) {
        workoutDao.upsertSetLog(set.toEntity())
    }

    override fun getUserProfile(userId: String): Flow<UserWorkoutProfile?> =
        workoutDao.getUserProfile(userId).map { it?.toDomain() }

    override suspend fun saveUserProfile(profile: UserWorkoutProfile) {
        workoutDao.insertUserProfile(profile.toEntity())
    }

    override fun getAllRoutines(): Flow<List<WorkoutRoutine>> =
        workoutDao.getAllRoutines().map { list ->
            list.map { it.routine.toDomain(it.exercisesWithOrder, it.routineSets, it.augmentsWithOrder) }
        }

    private suspend fun getSystemRoutineDefinition(id: String, exercisesList: List<Exercise> = emptyList()): WorkoutRoutine? {
        val targetExercises = if (exercisesList.isEmpty()) {
            workoutDao.getExerciseDefinitions().first().map { it.toDomain() }
        } else {
            exercisesList
        }

        return when (id) {
            "routine_cybercrapp_a" -> {
                val bench = targetExercises.find { it.id == "bench_press" } ?: return null
                val milPress = targetExercises.find { it.id == "military_press" } ?: return null
                val tricep = targetExercises.find { it.id == "db_tricep_extension" } ?: return null
                WorkoutRoutine(
                    id = "routine_cybercrapp_a",
                    name = "CyberCrapp A (Push)",
                    description = "PROTOCOL: CYBERCRAPP\nGOAL: Hypertrophy + Powerbuilding\n\nMETHOD: \n- 1 Main Set to failure + 2 rest-pause 'mini-sets'.\n- 1 Cyber Finisher (Lengthened Partials).\n- 1 Loaded Stretch (Somatotype-optimized).\n\nFOCUS: Chest, Shoulders, Triceps.",
                    protocol = WorkoutProtocol.CYBER_CRAPP,
                    isSystem = true,
                    isAddedToLibrary = false,
                    exercises = listOf(
                        RoutineExercise(
                            exercise = bench,
                            sets = listOf(
                                RoutineSet(type = SetType.WARMUP),
                                RoutineSet(type = SetType.WARMUP),
                                RoutineSet(type = SetType.REST_PAUSE)
                            )
                        ),
                        RoutineExercise(
                            exercise = milPress,
                            sets = listOf(
                                RoutineSet(type = SetType.WARMUP),
                                RoutineSet(type = SetType.WARMUP),
                                RoutineSet(type = SetType.REST_PAUSE)
                            )
                        ),
                        RoutineExercise(
                            exercise = tricep,
                            sets = listOf(
                                RoutineSet(type = SetType.WARMUP),
                                RoutineSet(type = SetType.WARMUP),
                                RoutineSet(type = SetType.REST_PAUSE)
                            )
                        )
                    )
                )
            }
            "routine_cybercrapp_b" -> {
                val pullups = targetExercises.find { it.id == "weighted_pullups" } ?: return null
                val row = targetExercises.find { it.id == "bent_over_row" } ?: return null
                val jerry = targetExercises.find { it.id == "jerry_curl" } ?: return null
                WorkoutRoutine(
                    id = "routine_cybercrapp_b",
                    name = "CyberCrapp B (Pull)",
                    description = "PROTOCOL: CYBERCRAPP\nGOAL: Peak Supination + Posterior Thickness\n\nMETHOD: \n- Standard CC Cluster protocol.\n- Includes Jerry Curls for maximum bicep stretch.\n\nFOCUS: Back, Biceps, Rear Delts.",
                    protocol = WorkoutProtocol.CYBER_CRAPP,
                    isSystem = true,
                    isAddedToLibrary = false,
                    exercises = listOf(
                        RoutineExercise(
                            exercise = pullups,
                            sets = listOf(
                                RoutineSet(type = SetType.WARMUP),
                                RoutineSet(type = SetType.WARMUP),
                                RoutineSet(type = SetType.REST_PAUSE)
                            )
                        ),
                        RoutineExercise(
                            exercise = row,
                            sets = listOf(
                                RoutineSet(type = SetType.WARMUP),
                                RoutineSet(type = SetType.WARMUP),
                                RoutineSet(type = SetType.REST_PAUSE)
                            )
                        ),
                        RoutineExercise(
                            exercise = jerry,
                            sets = listOf(
                                RoutineSet(type = SetType.WARMUP),
                                RoutineSet(type = SetType.WARMUP),
                                RoutineSet(type = SetType.REST_PAUSE)
                            )
                        )
                    )
                )
            }
            "routine_cybercrapp_c" -> {
                val squat = targetExercises.find { it.id == "back_squat" } ?: return null
                val rdl = targetExercises.find { it.id == "romanian_deadlift" } ?: return null
                val calf = targetExercises.find { it.id == "calf_raise" } ?: return null
                WorkoutRoutine(
                    id = "routine_cybercrapp_c",
                    name = "CyberCrapp C (Legs)",
                    description = "PROTOCOL: CYBERCRAPP\nGOAL: Neural Drive + Metabolic Stress\n\nMETHOD: \n- POWER sets (explosive 30-60% load).\n- Widowmaker sets (20 rep brutal finishers).\n\nFOCUS: Quads, Hamstrings, Glutes, Calves.",
                    protocol = WorkoutProtocol.CYBER_CRAPP,
                    isSystem = true,
                    isAddedToLibrary = false,
                    exercises = listOf(
                        RoutineExercise(
                            exercise = squat,
                            sets = listOf(
                                RoutineSet(type = SetType.WARMUP),
                                RoutineSet(type = SetType.WARMUP),
                                RoutineSet(type = SetType.POWER),
                                RoutineSet(type = SetType.WIDOWMAKER, goalReps = "20")
                            )
                        ),
                        RoutineExercise(
                            exercise = rdl,
                            sets = listOf(
                                RoutineSet(type = SetType.WARMUP),
                                RoutineSet(type = SetType.WARMUP),
                                RoutineSet(type = SetType.REST_PAUSE)
                            )
                        ),
                        RoutineExercise(
                            exercise = calf,
                            sets = listOf(
                                RoutineSet(type = SetType.WARMUP),
                                RoutineSet(type = SetType.REST_PAUSE)
                            )
                        )
                    )
                )
            }
            else -> null
        }
    }

    override suspend fun saveRoutine(routine: WorkoutRoutine) {
        var finalRoutine = routine
        if (finalRoutine.exercises.isEmpty()) {
            val existing = workoutDao.getAllRoutines().first().find { it.routine.id == routine.id }
            if (existing != null && existing.exercisesWithOrder.isNotEmpty()) {
                finalRoutine = existing.routine.toDomain(
                    existing.exercisesWithOrder,
                    existing.routineSets,
                    existing.augmentsWithOrder
                ).copy(isAddedToLibrary = routine.isAddedToLibrary)
            } else if (routine.isSystem) {
                val systemRoutine = getSystemRoutineDefinition(routine.id)
                if (systemRoutine != null) {
                    finalRoutine = systemRoutine.copy(isAddedToLibrary = routine.isAddedToLibrary)
                }
            }
        }

        val routineEntity = finalRoutine.toEntity()
        val exerciseRefs = mutableListOf<RoutineExerciseCrossRef>()
        val routineSets = mutableListOf<RoutineSetEntity>()
        val augmentRefs = mutableListOf<RoutineAugmentCrossRef>()

        val validExerciseIds = workoutDao.getExerciseDefinitions().first().map { it.id }.toSet()

        finalRoutine.exercises.forEachIndexed { index, routineExercise ->
            if (!validExerciseIds.contains(routineExercise.exercise.id)) return@forEachIndexed
            
            exerciseRefs.add(RoutineExerciseCrossRef(finalRoutine.id, routineExercise.exercise.id, index))
            
            routineExercise.sets.forEachIndexed { setIndex, set ->
                routineSets.add(
                    RoutineSetEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        routineId = finalRoutine.id,
                        exerciseId = routineExercise.exercise.id,
                        order = setIndex,
                        type = set.type.name,
                        weight = set.weight,
                        reps = set.reps,
                        goalReps = set.goalReps
                    )
                )
            }
        }

        finalRoutine.augments.forEachIndexed { index, augment ->
            augmentRefs.add(RoutineAugmentCrossRef(finalRoutine.id, augment.id, index))
        }

        workoutDao.insertFullRoutine(routineEntity, exerciseRefs, routineSets, augmentRefs)
    }

    private suspend fun seedRoutine(routine: WorkoutRoutine) {
        val routines = workoutDao.getAllRoutines().first()
        val existing = routines.find { it.routine.id == routine.id }
        
        // System routines are always updated to the latest code-defined version
        // unless they have been added to the library AND already have exercises
        if (existing == null || (!existing.routine.isAddedToLibrary) || (existing.exercisesWithOrder.isEmpty())) {
            val userAddedStatus = existing?.routine?.isAddedToLibrary ?: routine.isAddedToLibrary
            saveRoutine(routine.copy(isAddedToLibrary = userAddedStatus))
        }
    }

    private suspend fun seedAugment(augment: WorkoutAugment) {
        val augments = workoutDao.getAllAugments().first()
        val existing = augments.find { it.augment.id == augment.id }
        
        // Always update system augments to latest definition if they are missing exercises
        if (existing == null || existing.exercises.isEmpty()) {
            val userAddedStatus = existing?.augment?.isAddedToLibrary ?: augment.isAddedToLibrary
            saveAugment(augment.copy(isAddedToLibrary = userAddedStatus))
        }
    }

    override suspend fun deleteRoutine(routineId: String) {
        val routine = workoutDao.getRoutineById(routineId)
        if (routine?.isSystem == true) {
            workoutDao.updateRoutineLibraryStatus(routineId, false)
        } else {
            workoutDao.deleteRoutine(routineId)
        }
    }

    override fun getAllAugments(): Flow<List<WorkoutAugment>> =
        workoutDao.getAllAugments().map { list ->
            list.map { it.augment.toDomain(it.exercises, it.exerciseRefs, it.augmentSets) }
        }

    override suspend fun saveAugment(augment: WorkoutAugment) {
        val augmentEntity = augment.toEntity()
        val exerciseRefs = mutableListOf<AugmentExerciseCrossRef>()
        val augmentSets = mutableListOf<AugmentSetEntity>()

        augment.exercises.forEachIndexed { index, routineExercise ->
            exerciseRefs.add(AugmentExerciseCrossRef(augment.id, routineExercise.exercise.id, index))
            
            routineExercise.sets.forEachIndexed { setIndex, set ->
                augmentSets.add(
                    AugmentSetEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        augmentId = augment.id,
                        exerciseId = routineExercise.exercise.id,
                        order = setIndex,
                        type = set.type.name,
                        weight = set.weight,
                        reps = set.reps,
                        goalReps = set.goalReps
                    )
                )
            }
        }

        workoutDao.insertFullAugment(augmentEntity, exerciseRefs, augmentSets)
    }

    override suspend fun deleteAugment(augmentId: String) {
        val augment = workoutDao.getAugmentById(augmentId)
        if (augment?.isSystem == true) {
            workoutDao.updateAugmentLibraryStatus(augmentId, false)
        } else {
            workoutDao.deleteAugment(augmentId)
        }
    }

    override suspend fun seedStarterExercises() {
        val exercises = listOf(
            Exercise(
                id = "bench_press",
                name = "Bench Press (Barbell)",
                description = "Flat barbell chest press for building push compound power.",
                cues = listOf("Retract scapula", "Touch chest at nipple line", "Drive through feet"),
                muscleGroups = listOf("Chest", "Shoulders", "Triceps"),
                equipment = listOf("Barbell"),
                movementType = MovementType.COMPOUND_UPPER,
                isLockedClassic = true,
                dangerousFor = listOf("Shoulder Pain")
            ),
            Exercise(
                id = "bench_press_dumbbell",
                name = "Bench Press (Dumbbell)",
                description = "Unilateral dumbbell flat bench press for increased range of motion.",
                cues = listOf("Keep dumbbells stable", "Tuck elbows slightly", "Press to center"),
                muscleGroups = listOf("Chest", "Shoulders", "Triceps"),
                equipment = listOf("Dumbbell"),
                dangerousFor = listOf("Shoulder Pain")
            ),
            Exercise(
                id = "incline_bench_press",
                name = "Incline Bench Press (Barbell)",
                description = "Barbell chest press on an incline to emphasize upper pectorals.",
                cues = listOf("30-45 degree incline", "Bar to upper chest", "Tuck elbows slightly"),
                muscleGroups = listOf("Chest", "Shoulders"),
                equipment = listOf("Barbell"),
                dangerousFor = listOf("Shoulder Pain")
            ),
            Exercise(
                id = "incline_bench_press_dumbbell",
                name = "Incline Bench Press (Dumbbell)",
                description = "Unilateral dumbbell incline chest press for upper chest isolation.",
                cues = listOf("Controlled descent", "Drive dumbbells upward", "Keep wrists straight"),
                muscleGroups = listOf("Chest", "Shoulders"),
                equipment = listOf("Dumbbell"),
                dangerousFor = listOf("Shoulder Pain")
            ),
            Exercise(
                id = "chest_press_hammer_strength",
                name = "Chest Press (Hammer Strength)",
                description = "Stability-focused plate-loaded machine press. Ideal for rest-pause to absolute failure.",
                cues = listOf("Keep back against pad", "Explosive press", "Controlled return"),
                muscleGroups = listOf("Chest", "Shoulders", "Triceps"),
                equipment = listOf("Plate Loaded"),
                movementType = MovementType.COMPOUND_UPPER
            ),
            Exercise(
                id = "incline_smith_press",
                name = "Incline Press (Smith Machine)",
                description = "Fixed-path incline press to isolate upper chest safely.",
                cues = listOf("Adjust bench to mid-chest", "Keep elbows tucked slightly", "Touch chest lightly"),
                muscleGroups = listOf("Chest", "Shoulders"),
                equipment = listOf("Machine"),
                movementType = MovementType.COMPOUND_UPPER
            ),
            Exercise(
                id = "floor_press_dumbbell",
                name = "Floor Press (Dumbbell)",
                description = "Dumbbell press on the floor to limit range of motion and protect the shoulders.",
                cues = listOf("Lie flat on floor", "Pause when elbows touch floor", "Drive up explosively"),
                muscleGroups = listOf("Chest", "Triceps"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.COMPOUND_UPPER
            ),
            Exercise(
                id = "chest_press_plate_loaded",
                name = "Chest Press (Plate Loaded)",
                description = "Levered plate-loaded machine for consistent chest tension.",
                cues = listOf("Adjust seat height so handles are mid-chest", "Keep back flat against pad", "Press forward explosively"),
                muscleGroups = listOf("Chest", "Triceps"),
                equipment = listOf("Plate Loaded")
            ),
            Exercise(
                id = "chest_press_hammer_strength",
                name = "Chest Press (Hammer Strength)",
                description = "Stability-focused plate-loaded machine press. Ideal for rest-pause to absolute failure.",
                cues = listOf("Keep back against pad", "Explosive press", "Controlled return"),
                muscleGroups = listOf("Chest", "Shoulders", "Triceps"),
                equipment = listOf("Plate Loaded"),
                movementType = MovementType.COMPOUND_UPPER
            ),
            Exercise(
                id = "incline_smith_press",
                name = "Incline Press (Smith Machine)",
                description = "Fixed-path incline press to isolate upper chest safely.",
                cues = listOf("Adjust bench to mid-chest", "Keep elbows tucked slightly", "Touch chest lightly"),
                muscleGroups = listOf("Chest", "Shoulders"),
                equipment = listOf("Machine"),
                movementType = MovementType.COMPOUND_UPPER
            ),
            Exercise(
                id = "floor_press_dumbbell",
                name = "Floor Press (Dumbbell)",
                description = "Dumbbell press on the floor to limit range of motion and protect the shoulders.",
                cues = listOf("Lie flat on floor", "Pause when elbows touch floor", "Drive up explosively"),
                muscleGroups = listOf("Chest", "Triceps"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.COMPOUND_UPPER
            ),
            Exercise(
                id = "chest_fly_cable",
                name = "Chest Fly (Cable)",
                description = "Continuous tension chest isolation using cables.",
                cues = listOf("Slight bend in elbows", "Hug a tree at the finish", "Squeeze chest hard"),
                muscleGroups = listOf("Chest"),
                equipment = listOf("Cable")
            ),
            Exercise(
                id = "pushup_bodyweight",
                name = "Push Up (Bodyweight)",
                description = "Fundamental horizontal push movement.",
                cues = listOf("Keep body in straight line", "Elbows tucked 45 degrees", "Chest to floor"),
                muscleGroups = listOf("Chest", "Shoulders", "Triceps"),
                equipment = listOf("Bodyweight")
            ),
            Exercise(
                id = "decline_pushup_bodyweight",
                name = "Decline Push Up (Bodyweight)",
                description = "Push up with feet elevated to target the upper chest and front delts.",
                cues = listOf("Elevate feet on a box or bench", "Keep hips in line with shoulders", "Touch nose/chest gently to floor"),
                muscleGroups = listOf("Chest", "Shoulders", "Triceps"),
                equipment = listOf("Bodyweight")
            ),
            Exercise(
                id = "atlas_pushup_bodyweight",
                name = "Atlas Push Up (Bodyweight)",
                description = "Deficit push up using three elevated contact points for a maximum chest stretch.",
                cues = listOf("Place hands on two blocks/benches", "Deep stretch at bottom below hand level", "Drive up and contract chest"),
                muscleGroups = listOf("Chest", "Shoulders", "Triceps"),
                equipment = listOf("Bodyweight")
            ),
            Exercise(
                id = "pullup_bodyweight",
                name = "Pull Up (Bodyweight)",
                description = "Classic vertical bodyweight pull.",
                cues = listOf("Chest to bar", "Dead hang at bottom", "Active scapula"),
                muscleGroups = listOf("Lats", "Biceps", "Upper Back"),
                equipment = listOf("Bodyweight")
            ),
            Exercise(
                id = "weighted_pullups",
                name = "Pull Up (Weighted)",
                description = "Vertical pull with added resistance.",
                cues = listOf("Chest to bar", "Full hang at bottom", "Control descent"),
                muscleGroups = listOf("Lats", "Biceps", "Upper Back"),
                equipment = listOf("Weighted"),
                movementType = MovementType.BACK_WIDTH
            ),
            Exercise(
                id = "chinup_bodyweight",
                name = "Chin Up (Bodyweight)",
                description = "Underhand vertical bodyweight pull maximizing bicep recruitment.",
                cues = listOf("Supinated grip", "Drive elbows down", "Chest to bar"),
                muscleGroups = listOf("Lats", "Biceps", "Upper Back"),
                equipment = listOf("Bodyweight")
            ),
            Exercise(
                id = "chinup_weighted",
                name = "Chin Up (Weighted)",
                description = "Weighted underhand vertical pull.",
                cues = listOf("Supinated grip", "Squeeze shoulder blades at top", "Control the eccentric"),
                muscleGroups = listOf("Lats", "Biceps", "Upper Back"),
                equipment = listOf("Weighted")
            ),
            Exercise(
                id = "lat_pulldown",
                name = "Lat Pulldown (Cable)",
                description = "Machine vertical pull.",
                cues = listOf("Pull to upper chest", "Squeeze lats", "Don't lean back too far"),
                muscleGroups = listOf("Lats", "Upper Back"),
                equipment = listOf("Cable")
            ),
            Exercise(
                id = "seated_row",
                name = "Seated Row (Cable)",
                description = "Horizontal pull focusing on mid-back.",
                cues = listOf("Chest up", "Pull to navel", "Squeeze shoulder blades"),
                muscleGroups = listOf("Back", "Biceps"),
                equipment = listOf("Cable")
            ),
            Exercise(
                id = "lat_row_plate_loaded",
                name = "Lat Row (Plate Loaded)",
                description = "Independent-arm plate-loaded row machine for back thickness.",
                cues = listOf("Brace chest against pad", "Pull elbow far back", "Squeeze lat and mid-back"),
                muscleGroups = listOf("Upper Back", "Lats", "Biceps"),
                equipment = listOf("Plate Loaded")
            ),
            Exercise(
                id = "facepull_cable",
                name = "Face Pull (Cable)",
                description = "Rear delt and rotator cuff cable pull.",
                cues = listOf("Pull rope towards forehead", "Separate hands at peak", "Squeeze rear delts"),
                muscleGroups = listOf("Rear Delts", "Upper Back", "Rotator Cuff"),
                equipment = listOf("Cable")
            ),
            Exercise(
                id = "one_arm_row_dumbbell",
                name = "One-Arm Row (Dumbbell)",
                description = "Unilateral dumbbell row for lat isolation.",
                cues = listOf("Keep back flat", "Pull dumbbell to hip", "Stretch fully at bottom"),
                muscleGroups = listOf("Lats", "Upper Back", "Core"),
                equipment = listOf("Dumbbell")
            ),
            Exercise(
                id = "bent_over_row",
                name = "Bent-Over Row (Barbell)",
                description = "Classic horizontal pull for back thickness.",
                cues = listOf("Hinged at hips", "Pull to upper stomach", "Squeeze shoulder blades"),
                muscleGroups = listOf("Back", "Biceps", "Rear Delts"),
                equipment = listOf("Barbell"),
                movementType = MovementType.BACK_THICKNESS,
                dangerousFor = listOf("Lower Back Pain")
            ),
            Exercise(
                id = "tbar_row_chest_supported",
                name = "T-Bar Row (Chest Supported)",
                description = "Stability-focused row machine that eliminates lower back strain.",
                cues = listOf("Lean chest into pad", "Drive elbows back", "Squeeze mid-back hard"),
                muscleGroups = listOf("Back", "Biceps"),
                equipment = listOf("Plate Loaded"),
                movementType = MovementType.BACK_THICKNESS
            ),
            Exercise(
                id = "rack_pull_below_knee",
                name = "Rack Pull (Below Knee)",
                description = "Partial range deadlift focused on back thickness and traps.",
                cues = listOf("Set pins below knee", "Drag bar up shins", "Lockout hard at top"),
                muscleGroups = listOf("Back", "Traps", "Forearms"),
                equipment = listOf("Barbell"),
                movementType = MovementType.BACK_THICKNESS,
                dangerousFor = listOf("Lower Back Pain")
            ),
            Exercise(
                id = "trap_bar_deadlift",
                name = "Deadlift (Trap Bar)",
                description = "High-stability deadlift that keeps the center of gravity aligned with the body.",
                cues = listOf("Step inside bar", "Hips down, chest up", "Drive through floor"),
                muscleGroups = listOf("Back", "Legs", "Traps"),
                equipment = listOf("Specialty Bar"),
                movementType = MovementType.POSTERIOR_CHAIN
            ),
            Exercise(
                id = "military_press",
                name = "Overhead Press (Barbell)",
                description = "Strict overhead barbell press.",
                cues = listOf("Squeeze glutes", "Head back to clear bar", "Punch through at top"),
                muscleGroups = listOf("Shoulders", "Triceps"),
                equipment = listOf("Barbell"),
                movementType = MovementType.COMPOUND_UPPER,
                dangerousFor = listOf("Shoulder Pain", "Lower Back Pain")
            ),
            Exercise(
                id = "shoulder_press_hammer_strength",
                name = "Shoulder Press (Hammer Strength)",
                description = "High-stability shoulder press machine. Maximizes deltoid isolation.",
                cues = listOf("Sit deep into seat", "Maintain arch in upper back", "Press to full lockout"),
                muscleGroups = listOf("Shoulders", "Triceps"),
                equipment = listOf("Plate Loaded"),
                movementType = MovementType.COMPOUND_UPPER
            ),
            Exercise(
                id = "seated_smith_overhead_press",
                name = "Overhead Press (Smith Machine)",
                description = "Seated fixed-path overhead press for maximum stability.",
                cues = listOf("Set bar height at nose level", "Brace core against bench", "Punch up hard"),
                muscleGroups = listOf("Shoulders", "Triceps"),
                equipment = listOf("Machine"),
                movementType = MovementType.COMPOUND_UPPER
            ),
            Exercise(
                id = "shoulder_press_dumbbell",
                name = "Shoulder Press (Dumbbell)",
                description = "Strict seated or standing overhead dumbbell press.",
                cues = listOf("Keep wrists straight", "Don't flare elbows fully", "Press to full lockout"),
                muscleGroups = listOf("Shoulders", "Triceps"),
                equipment = listOf("Dumbbell")
            ),
            Exercise(
                id = "shoulder_press_kettlebell",
                name = "Shoulder Press (Kettlebell)",
                description = "Overhead kettlebell press from the front rack position.",
                cues = listOf("Rack KB tight against chest", "Press up in a slight arc", "Lock out fully at top"),
                muscleGroups = listOf("Shoulders", "Triceps"),
                equipment = listOf("Kettlebell")
            ),
            Exercise(
                id = "shoulder_press_hammer_strength",
                name = "Shoulder Press (Hammer Strength)",
                description = "High-stability shoulder press machine. Maximizes deltoid isolation.",
                cues = listOf("Sit deep into seat", "Maintain arch in upper back", "Press to full lockout"),
                muscleGroups = listOf("Shoulders", "Triceps"),
                equipment = listOf("Plate Loaded"),
                movementType = MovementType.COMPOUND_UPPER
            ),
            Exercise(
                id = "seated_smith_overhead_press",
                name = "Overhead Press (Smith Machine)",
                description = "Seated fixed-path overhead press for maximum stability.",
                cues = listOf("Set bar height at nose level", "Brace core against bench", "Punch up hard"),
                muscleGroups = listOf("Shoulders", "Triceps"),
                equipment = listOf("Machine"),
                movementType = MovementType.COMPOUND_UPPER
            ),
            Exercise(
                id = "lateral_raise",
                name = "Lateral Raise (Dumbbell)",
                description = "Isolation for side delts.",
                cues = listOf("Pinkies up", "Slight elbow bend", "Control the descent"),
                muscleGroups = listOf("Shoulders"),
                equipment = listOf("Dumbbell")
            ),
            Exercise(
                id = "lateral_raise_cable",
                name = "Lateral Raise (Cable)",
                description = "Constant tension side delt cable raise.",
                cues = listOf("Raise hand slightly forward", "Slight elbow bend", "Slow eccentric"),
                muscleGroups = listOf("Shoulders"),
                equipment = listOf("Cable")
            ),
            Exercise(
                id = "lateral_raise_kettlebell",
                name = "Lateral Raise (Kettlebell)",
                description = "Side delt raise utilizing kettlebells for unique load distribution.",
                cues = listOf("Hold KB handle firmly", "Raise arms to parallel", "Control gravity's pull"),
                muscleGroups = listOf("Shoulders"),
                equipment = listOf("Kettlebell")
            ),
            Exercise(
                id = "rear_delt_fly_dumbbell",
                name = "Rear Delt Fly (Dumbbell)",
                description = "Bent-over rear lateral raise.",
                cues = listOf("Hinge forward at hips", "Fly dumbbells out to sides", "Squeeze rear delts"),
                muscleGroups = listOf("Rear Delts"),
                equipment = listOf("Dumbbell")
            ),
            Exercise(
                id = "rear_delt_fly_machine",
                name = "Rear Delt Fly (Cable)",
                description = "Cable or machine reverse fly for rear delt isolation.",
                cues = listOf("Keep arms parallel to ground", "Pull back with shoulder joints", "Pause at peak contraction"),
                muscleGroups = listOf("Rear Delts"),
                equipment = listOf("Cable")
            ),
            Exercise(
                id = "back_squat",
                name = "Back Squat (Barbell)",
                description = "King of leg exercises. Full body demand.",
                cues = listOf("Brace core", "Hips back first", "Break parallel"),
                muscleGroups = listOf("Quads", "Glutes", "Hamstrings", "Lower Back"),
                equipment = listOf("Barbell"),
                movementType = MovementType.QUAD_DOMINANT,
                isLockedClassic = true,
                dangerousFor = listOf("Knee Pain", "Lower Back Pain")
            ),
            Exercise(
                id = "hack_squat_machine",
                name = "Hack Squat (Machine)",
                description = "Fixed-path squat focusing on the quadriceps with full back support.",
                cues = listOf("Shoulders against pads", "Feet low on platform for quads", "Push up and release handles"),
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = listOf("Machine"),
                movementType = MovementType.QUAD_DOMINANT
            ),
            Exercise(
                id = "belt_squat",
                name = "Belt Squat",
                description = "Lower body squat that removes all spinal loading. Ideal for lower back issues.",
                cues = listOf("Secure belt to hips", "Stand tall to release weight", "Sit deep into the hole"),
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = listOf("Machine"),
                movementType = MovementType.QUAD_DOMINANT
            ),
            Exercise(
                id = "pendulum_squat",
                name = "Pendulum Squat",
                description = "Arc-path machine squat that provides incredible quad stretch and stability.",
                cues = listOf("Maintain back contact", "Slow controlled negative", "Drive through mid-foot"),
                muscleGroups = listOf("Quads"),
                equipment = listOf("Machine"),
                movementType = MovementType.QUAD_DOMINANT
            ),
            Exercise(
                id = "front_squat",
                name = "Front Squat (Barbell)",
                description = "Barbell squat loaded in front, emphasizing the quads and upper back.",
                cues = listOf("High elbows", "Brace core", "Upright torso"),
                muscleGroups = listOf("Quads", "Glutes", "Core", "Upper Back"),
                equipment = listOf("Barbell")
            ),
            Exercise(
                id = "goblet_squat",
                name = "Goblet Squat (Dumbbell)",
                description = "A quad-dominant squat holding a single dumbbell in front.",
                cues = listOf("Hold dumbbell close to chest", "Keep elbows tucked", "Sit deep into hips"),
                muscleGroups = listOf("Quads", "Glutes", "Core"),
                equipment = listOf("Dumbbell")
            ),
            Exercise(
                id = "hack_squat_plate_loaded",
                name = "Hack Squat (Plate Loaded)",
                description = "Sled machine squat emphasizing the quadriceps.",
                cues = listOf("Back flat against backrest", "Feet shoulder-width on platform", "Drive through heels"),
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = listOf("Plate Loaded")
            ),
            Exercise(
                id = "leg_extension_cable",
                name = "Leg Extension (Cable)",
                description = "Machine isolation targeting the quadriceps.",
                cues = listOf("Point toes slightly upward", "Hold handles for stability", "Squeeze quads at full extension"),
                muscleGroups = listOf("Quads"),
                equipment = listOf("Cable")
            ),
            Exercise(
                id = "leg_extension_plate_loaded",
                name = "Leg Extension (Plate Loaded)",
                description = "Levered plate-loaded machine leg extension.",
                cues = listOf("Keep hips pushed back", "Squeeze quads hard at the peak", "Lower slowly and in control"),
                muscleGroups = listOf("Quads"),
                equipment = listOf("Plate Loaded")
            ),
            Exercise(
                id = "leg_curl_cable",
                name = "Leg Curl (Cable)",
                description = "Cable machine hamstring isolation.",
                cues = listOf("Keep hips on pad", "Pull heels to glutes", "Squeeze hamstrings at the bottom"),
                muscleGroups = listOf("Hamstrings"),
                equipment = listOf("Cable"),
                movementType = MovementType.HAMSTRING_ISOLATION
            ),
            Exercise(
                id = "leg_curl_plate_loaded",
                name = "Leg Curl (Plate Loaded)",
                description = "Levered plate-loaded machine leg curl.",
                cues = listOf("Brace thighs tight against support pad", "Contract hamstring explosively", "Control the return stretch"),
                muscleGroups = listOf("Hamstrings"),
                equipment = listOf("Plate Loaded"),
                movementType = MovementType.HAMSTRING_ISOLATION
            ),
            Exercise(
                id = "lunge_barbell",
                name = "Lunge (Barbell)",
                description = "Barbell loaded walking or stationary lunges.",
                cues = listOf("Keep chest tall", "Take a big step forward", "Push off front heel"),
                muscleGroups = listOf("Quads", "Glutes", "Hamstrings"),
                equipment = listOf("Barbell")
            ),
            Exercise(
                id = "lunge_dumbbell",
                name = "Lunge (Dumbbell)",
                description = "Dumbbell loaded walking or stationary lunges.",
                cues = listOf("Dumbbells at sides", "Keep torso upright", "Control rear knee down"),
                muscleGroups = listOf("Quads", "Glutes", "Hamstrings"),
                equipment = listOf("Dumbbell")
            ),
            Exercise(
                id = "lunge_bodyweight",
                name = "Lunge (Bodyweight)",
                description = "Unilateral bodyweight lunge.",
                cues = listOf("Hands on hips or front", "Stable core", "Step back or forward cleanly"),
                muscleGroups = listOf("Quads", "Glutes", "Hamstrings"),
                equipment = listOf("Bodyweight")
            ),
            Exercise(
                id = "hip_thrust_barbell",
                name = "Hip Thrust (Barbell)",
                description = "Barbell loaded hip thrusts for absolute glute development.",
                cues = listOf("Rest upper back on bench", "Drive hips upward", "Squeeze glutes fully at peak"),
                muscleGroups = listOf("Glutes", "Hamstrings"),
                equipment = listOf("Barbell")
            ),
            Exercise(
                id = "hip_thrust_plate_loaded",
                name = "Hip Thrust (Plate Loaded)",
                description = "Plate loaded belt or pad lever hip thrust machine.",
                cues = listOf("Secure safety belt tightly", "Drive heels down", "Hold peak contraction for 1s"),
                muscleGroups = listOf("Glutes", "Hamstrings"),
                equipment = listOf("Plate Loaded")
            ),
            Exercise(
                id = "romanian_deadlift",
                name = "Romanian Deadlift (Barbell)",
                description = "Hip hinge focusing on hamstrings.",
                cues = listOf("Hips back", "Feel the stretch", "Don't touch floor"),
                muscleGroups = listOf("Hamstrings", "Glutes"),
                equipment = listOf("Barbell"),
                movementType = MovementType.POSTERIOR_CHAIN
            ),
            Exercise(
                id = "romanian_deadlift_dumbbell",
                name = "Romanian Deadlift (Dumbbell)",
                description = "Unilateral or bilateral dumbbell Romanian deadlift.",
                cues = listOf("Hinged hips back", "Keep dumbbells close to shins", "Squeeze glutes to stand"),
                muscleGroups = listOf("Hamstrings", "Glutes"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.POSTERIOR_CHAIN
            ),
            Exercise(
                id = "deadlift",
                name = "Conventional Deadlift (Barbell)",
                description = "Ultimate test of posterior chain strength.",
                cues = listOf("Slack out of bar", "Drag up shins", "Lockout hips"),
                muscleGroups = listOf("Hamstrings", "Glutes", "Back", "Forearms"),
                equipment = listOf("Barbell"),
                movementType = MovementType.DEADLIFT,
                isLockedClassic = true,
                dangerousFor = listOf("Lower Back Pain")
            ),
            Exercise(
                id = "kettlebell_swings",
                name = "Kettlebell Swing (Kettlebell)",
                description = "Dynamic ballistic hip hinge.",
                cues = listOf("Hinged at hips", "Squeeze glutes at peak", "Let arms act as ropes"),
                muscleGroups = listOf("Glutes", "Hamstrings", "Lower Back", "Core"),
                equipment = listOf("Kettlebell")
            ),
            Exercise(
                id = "calf_raise_bodyweight",
                name = "Calf Raise (Bodyweight)",
                description = "Standing bodyweight calf lifts.",
                cues = listOf("Full range on floor/step", "Peak squeeze", "Control descent"),
                muscleGroups = listOf("Calves"),
                equipment = listOf("Bodyweight")
            ),
            Exercise(
                id = "calf_raise",
                name = "Calf Raise (Plate Loaded)",
                description = "Seated or standing machine calf isolation.",
                cues = listOf("Full stretch at bottom", "Explosive up", "1s pause at top"),
                muscleGroups = listOf("Calves"),
                equipment = listOf("Plate Loaded"),
                movementType = MovementType.CALVES
            ),
            Exercise(
                id = "bicep_curl_barbell",
                name = "Bicep Curl (Barbell)",
                description = "Traditional barbell bicep curl.",
                cues = listOf("Keep elbows locked by side", "Don't lean or swing", "Squeeze at top"),
                muscleGroups = listOf("Biceps", "Forearms"),
                equipment = listOf("Barbell")
            ),
            Exercise(
                id = "bicep_curl_dumbbell",
                name = "Bicep Curl (Dumbbell)",
                description = "Dumbbell curls with wrist supination.",
                cues = listOf("Turn palms up as you lift", "Squeeze biceps at peak", "Control down"),
                muscleGroups = listOf("Biceps", "Brachialis"),
                equipment = listOf("Dumbbell")
            ),
            Exercise(
                id = "preacher_curl_ezbar",
                name = "Preacher Curl (EZ-Bar)",
                description = "EZ-Bar preacher bench curls for strict bicep isolation.",
                cues = listOf("Keep armpits snug to pad", "Slow controlled negative", "Squeeze at top"),
                muscleGroups = listOf("Biceps", "Brachioradialis"),
                equipment = listOf("EZ-Bar")
            ),
            Exercise(
                id = "jerry_curl",
                name = "Jerry Curl (Dumbbell)",
                description = "High-intensity bicep protocol emphasizing the bottom stretch and peak supination.",
                cues = listOf("Start with back of hand against thigh", "Full supination + hard squeeze at peak", "Emphasize deep stretch on eccentric", "Torso stable, strict form"),
                muscleGroups = listOf("Biceps", "Brachialis"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.ISOLATION_UPPER,
 gifAssetPath = "exercises/jerry_curl.gif"
            ),
            Exercise(
                id = "jerry_curl_kettlebell",
                name = "Jerry Curl (Kettlebell)",
                description = "Jerry Curl supination bicep curls utilizing kettlebells for enhanced bottom stretch.",
                cues = listOf("Start with pronated grip", "Supinate wrist on ascend", "Squeeze bicep at peak"),
                muscleGroups = listOf("Biceps", "Brachialis"),
                equipment = listOf("Kettlebell")
            ),
            Exercise(
                id = "hammer_curl",
                name = "Hammer Curl (Dumbbell)",
                description = "Bicep curl with neutral grip.",
                cues = listOf("Neutral grip", "No swinging", "Squeeze at top"),
                muscleGroups = listOf("Biceps", "Forearms"),
                equipment = listOf("Dumbbell")
            ),
            Exercise(
                id = "hammer_curl_kettlebell",
                name = "Hammer Curl (Kettlebell)",
                description = "Kettlebell hammer curl with a neutral grip for forearm and brachialis thickness.",
                cues = listOf("Keep wrists locked", "Don't swing", "Control the negative"),
                muscleGroups = listOf("Biceps", "Forearms"),
                equipment = listOf("Kettlebell")
            ),
            Exercise(
                id = "tricep_pushdown_cable",
                name = "Tricep Pushdown (Cable)",
                description = "Cable rope or bar tricep pushdown isolation.",
                cues = listOf("Pin elbows to ribcage", "Push down and separate hands", "Full lockout squeeze"),
                muscleGroups = listOf("Triceps"),
                equipment = listOf("Cable")
            ),
            Exercise(
                id = "db_tricep_extension",
                name = "Tricep Extension (Dumbbell)",
                description = "Isolation for the long head of the triceps.",
                cues = listOf("Elbow high", "Deep stretch at bottom", "Full lockout"),
                muscleGroups = listOf("Triceps"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.ISOLATION_UPPER
            ),
            Exercise(
                id = "skull_crusher",
                name = "Skull Crusher (EZ-Bar)",
                description = "Tricep isolation.",
                cues = listOf("Elbows tucked", "Lower to forehead", "Full lockout"),
                muscleGroups = listOf("Triceps"),
                equipment = listOf("EZ-Bar"),
                dangerousFor = listOf("Elbow Pain")
            ),
            Exercise(
                id = "close_grip_smith_press",
                name = "Close Grip Press (Smith Machine)",
                description = "High-stability tricep focused press.",
                cues = listOf("Grip shoulder-width", "Touch lower chest", "Full tricep lockout"),
                muscleGroups = listOf("Triceps", "Chest"),
                equipment = listOf("Machine"),
                movementType = MovementType.ISOLATION_UPPER
            ),
            Exercise(
                id = "dip_bodyweight",
                name = "Dip (Bodyweight)",
                description = "Strict bodyweight dips.",
                cues = listOf("Lower until arms hit 90 degrees", "Control the descent", "Squeeze triceps at top"),
                muscleGroups = listOf("Triceps", "Chest", "Shoulders"),
                equipment = listOf("Bodyweight")
            ),
            Exercise(
                id = "weighted_dip",
                name = "Dip (Weighted)",
                description = "Powerful tricep and chest builder.",
                cues = listOf("Lean forward for chest", "Upright for triceps", "Full lockout"),
                muscleGroups = listOf("Triceps", "Chest", "Shoulders"),
                equipment = listOf("Weighted")
            ),
            Exercise(
                id = "cable_crunch",
                name = "Cable Crunch (Cable)",
                description = "Constant tension kneeling abdominal cable crunch.",
                cues = listOf("Crunch with abs, not hips", "Touch elbows to knees", "Squeeze core hard"),
                muscleGroups = listOf("Abs"),
                equipment = listOf("Cable")
            ),
            Exercise(
                id = "hanging_knee_raise",
                name = "Hanging Leg Raise (Bodyweight)",
                description = "Core exercise for lower abs.",
                cues = listOf("No swinging", "Crunch with hips", "Slow descent"),
                muscleGroups = listOf("Abs"),
                equipment = listOf("Bodyweight")
            ),
            Exercise(
                id = "cyber_cluster_squat",
                name = "Cyber Cluster Squats (Barbell)",
                description = "High-intensity squat protocol using rest-pause clusters.",
                cues = listOf("Standard squat form", "15s rest between clusters", "Maintain brace"),
                muscleGroups = listOf("Quads", "Glutes", "Core"),
                equipment = listOf("Barbell")
            ),
            Exercise(
                id = "zercher_squat",
                name = "Zercher Squat (Barbell)",
                description = "Squat with bar in the crooks of elbows. Brutal core demand.",
                cues = listOf("Bar in elbows", "Clasp hands", "Upright torso"),
                muscleGroups = listOf("Quads", "Core", "Upper Back"),
                equipment = listOf("Barbell")
            )
        )
        
        exercises.forEach {
            workoutDao.insertExerciseDefinition(it.toEntity())
        }

        // Seed Gorilla Arms Augment
        val gorillaArms = WorkoutAugment(
            id = "augment_gorilla_arms",
            name = "Gorilla Arms",
            description = "High-intensity upper arm protocol emphasizing peak bicep supination and long-head tricep extension. Perform as a Giant Set: 1 set of each exercise sequentially, then rest.",
            focusBodyPart = "Upper Arms",
            exercises = listOfNotNull(
                exercises.find { it.id == "hammer_curl" },
                exercises.find { it.id == "jerry_curl" },
                exercises.find { it.id == "db_tricep_extension" },
                exercises.find { it.id == "lateral_raise" }
            ).map { 
                RoutineExercise(
                    exercise = it,
                    sets = List(4) { 
                        RoutineSet(type = SetType.GS, goalReps = "25") 
                    }
                ) 
            },
            colorHex = "#00CCFF",
            isSystem = true,
            isAddedToLibrary = false // Don't show on main screen by default
        )
        seedAugment(gorillaArms)

        // Seed CyberCrapp A Routine
        val cyberCrappA = WorkoutRoutine(
            id = "routine_cybercrapp_a",
            name = "CyberCrapp A (Push)",
            description = "PROTOCOL: CYBERCRAPP\nGOAL: Hypertrophy + Powerbuilding\n\nMETHOD: \n- 1 Main Set to failure + 2 rest-pause 'mini-sets'.\n- 1 Cyber Finisher (Lengthened Partials).\n- 1 Loaded Stretch (Somatotype-optimized).\n\nFOCUS: Chest, Shoulders, Triceps.",
            protocol = WorkoutProtocol.CYBER_CRAPP,
            isSystem = true,
            isAddedToLibrary = false,
            exercises = listOf(
                RoutineExercise(
                    exercise = exercises.find { it.id == "bench_press" }!!,
                    sets = listOf(
                        RoutineSet(type = SetType.WARMUP),
                        RoutineSet(type = SetType.WARMUP),
                        RoutineSet(type = SetType.REST_PAUSE)
                    )
                ),
                RoutineExercise(
                    exercise = exercises.find { it.id == "military_press" }!!,
                    sets = listOf(
                        RoutineSet(type = SetType.WARMUP),
                        RoutineSet(type = SetType.WARMUP),
                        RoutineSet(type = SetType.REST_PAUSE)
                    )
                ),
                RoutineExercise(
                    exercise = exercises.find { it.id == "db_tricep_extension" }!!,
                    sets = listOf(
                        RoutineSet(type = SetType.WARMUP),
                        RoutineSet(type = SetType.WARMUP),
                        RoutineSet(type = SetType.REST_PAUSE)
                    )
                )
            )
        )
        seedRoutine(cyberCrappA)

        // Seed CyberCrapp B Routine
        val cyberCrappB = WorkoutRoutine(
            id = "routine_cybercrapp_b",
            name = "CyberCrapp B (Pull)",
            description = "PROTOCOL: CYBERCRAPP\nGOAL: Peak Supination + Posterior Thickness\n\nMETHOD: \n- Standard CC Cluster protocol.\n- Includes Jerry Curls for maximum bicep stretch.\n\nFOCUS: Back, Biceps, Rear Delts.",
            protocol = WorkoutProtocol.CYBER_CRAPP,
            isSystem = true,
            isAddedToLibrary = false,
            exercises = listOf(
                RoutineExercise(
                    exercise = exercises.find { it.id == "weighted_pullups" }!!,
                    sets = listOf(
                        RoutineSet(type = SetType.WARMUP),
                        RoutineSet(type = SetType.WARMUP),
                        RoutineSet(type = SetType.REST_PAUSE)
                    )
                ),
                RoutineExercise(
                    exercise = exercises.find { it.id == "bent_over_row" }!!,
                    sets = listOf(
                        RoutineSet(type = SetType.WARMUP),
                        RoutineSet(type = SetType.WARMUP),
                        RoutineSet(type = SetType.REST_PAUSE)
                    )
                ),
                RoutineExercise(
                    exercise = exercises.find { it.id == "jerry_curl" }!!,
                    sets = listOf(
                        RoutineSet(type = SetType.WARMUP),
                        RoutineSet(type = SetType.WARMUP),
                        RoutineSet(type = SetType.REST_PAUSE)
                    )
                )
            )
        )
        seedRoutine(cyberCrappB)

        // Seed CyberCrapp C Routine
        val cyberCrappC = WorkoutRoutine(
            id = "routine_cybercrapp_c",
            name = "CyberCrapp C (Legs)",
            description = "PROTOCOL: CYBERCRAPP\nGOAL: Neural Drive + Metabolic Stress\n\nMETHOD: \n- POWER sets (explosive 30-60% load).\n- Widowmaker sets (20 rep brutal finishers).\n\nFOCUS: Quads, Hamstrings, Glutes, Calves.",
            protocol = WorkoutProtocol.CYBER_CRAPP,
            isSystem = true,
            isAddedToLibrary = false,
            exercises = listOf(
                RoutineExercise(
                    exercise = exercises.find { it.id == "back_squat" }!!,
                    sets = listOf(
                        RoutineSet(type = SetType.WARMUP),
                        RoutineSet(type = SetType.WARMUP),
                        RoutineSet(type = SetType.POWER),
                        RoutineSet(type = SetType.WIDOWMAKER, goalReps = "20")
                    )
                ),
                RoutineExercise(
                    exercise = exercises.find { it.id == "romanian_deadlift" }!!,
                    sets = listOf(
                        RoutineSet(type = SetType.WARMUP),
                        RoutineSet(type = SetType.WARMUP),
                        RoutineSet(type = SetType.REST_PAUSE)
                    )
                ),
                RoutineExercise(
                    exercise = exercises.find { it.id == "calf_raise" }!!,
                    sets = listOf(
                        RoutineSet(type = SetType.WARMUP),
                        RoutineSet(type = SetType.REST_PAUSE)
                    )
                )
            )
        )
        seedRoutine(cyberCrappC)

        // Ensure old default routines are removed as requested
        workoutDao.deleteRoutine("routine_strength")
        workoutDao.deleteRoutine("routine_strength_2")
        workoutDao.deleteRoutine("routine_lower_split")
    }

    override suspend fun deleteSession(sessionId: String) {
        workoutDao.deleteSession(sessionId)
    }

    override fun getActiveSession(): Flow<WorkoutSession?> =
        workoutDao.getActiveSession().map { it?.toDomain() }

    override fun getRecoveryScore(): Flow<RecoveryScore> =
        getFullHistory().map { history ->
            val recentSessions = history.take(5)
            val allProgressionStates = workoutDao.getExerciseDefinitions().first().map {
                workoutDao.getProgressionState(it.id).first()?.toDomain() ?: ProgressionState(it.id)
            }
            RecoveryEngine.calculateScore(recentSessions, allProgressionStates)
        }

    override fun getProgressionState(exerciseId: String): Flow<ProgressionState?> =
        workoutDao.getProgressionState(exerciseId).map { it?.toDomain() }

    override suspend fun saveProgressionState(state: ProgressionState) {
        workoutDao.insertProgressionState(state.toEntity())
    }

    override fun getFullHistory(): Flow<List<Pair<WorkoutSession, List<Pair<WorkoutLog, List<SetLog>>>>>> =
        workoutDao.getAllSessionsWithDetails().map { sessions ->
            sessions.map { sessionWithLogs ->
                sessionWithLogs.session.toDomain() to sessionWithLogs.logs.map { logWithSets ->
                    logWithSets.log.toDomain() to logWithSets.sets.map { it.toDomain() }
                }
            }
        }

    override fun getLatestSetsForExercise(exerciseId: String, excludedSessionId: String): Flow<List<SetLog>> =
        workoutDao.getLatestLogForExercise(exerciseId, excludedSessionId).map { logWithSets -> 
            logWithSets?.sets
                ?.map { it.toDomain() }
                ?.sortedBy { it.timestamp }
                ?: emptyList() 
        }

    override suspend fun deleteWorkoutLog(workoutLogId: String) {
        workoutDao.deleteWorkoutLog(workoutLogId)
    }

    override suspend fun updateWorkoutLogOrder(workoutLogId: String, newOrder: Int) {
        workoutDao.updateWorkoutLogOrder(workoutLogId, newOrder)
    }

    override suspend fun updateShowGoalReps(workoutLogId: String, show: Boolean) {
        workoutDao.updateShowGoalReps(workoutLogId, show)
    }

    override suspend fun updateSupersetId(workoutLogId: String, supersetId: String?) {
        workoutDao.updateSupersetId(workoutLogId, supersetId)
    }

    override suspend fun deleteSetLog(setLogId: String) {
        workoutDao.deleteSetLog(setLogId)
    }
}
