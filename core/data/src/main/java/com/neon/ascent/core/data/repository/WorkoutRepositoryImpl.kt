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
        workoutDao.insertSession(session.toEntity())
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
        workoutDao.insertWorkoutLog(log.toEntity())
    }

    override suspend fun saveSetLog(set: SetLog) {
        workoutDao.insertSetLog(set.toEntity())
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

        finalRoutine.exercises.forEachIndexed { index, routineExercise ->
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
                name = "Flat Bench Press",
                description = "Core push compound for chest, shoulders, and triceps.",
                cues = listOf("Retract scapula", "Touch chest at nipple line", "Drive through feet"),
                muscleGroups = listOf("Chest", "Shoulders", "Triceps"),
                equipment = listOf("Barbell", "Bench"),
                isLockedClassic = true
            ),
            Exercise(
                id = "back_squat",
                name = "Back Squat",
                description = "King of leg exercises. Full body demand.",
                cues = listOf("Brace core", "Hips back first", "Break parallel"),
                muscleGroups = listOf("Quads", "Glutes", "Hamstrings", "Lower Back"),
                equipment = listOf("Barbell", "Rack"),
                isLockedClassic = true
            ),
            Exercise(
                id = "deadlift",
                name = "Conventional Deadlift",
                description = "Ultimate test of posterior chain strength.",
                cues = listOf("Slack out of bar", "Drag up shins", "Lockout hips"),
                muscleGroups = listOf("Hamstrings", "Glutes", "Back", "Forearms"),
                equipment = listOf("Barbell"),
                isLockedClassic = true
            ),
            Exercise(
                id = "jerry_curl",
                name = "Jerry Curls",
                description = "High-intensity bicep protocol emphasizing the bottom stretch and peak supination.",
                cues = listOf("Start with back of hand against thigh", "Full supination + hard squeeze at peak", "Emphasize deep stretch on eccentric", "Torso stable, strict form"),
                muscleGroups = listOf("Biceps", "Brachialis"),
                equipment = listOf("Dumbbell"),
                gifAssetPath = "exercises/jerry_curl.gif"
            ),
            Exercise(
                id = "military_press",
                name = "Military Press",
                description = "Strict overhead barbell press.",
                cues = listOf("Squeeze glutes", "Head back to clear bar", "Punch through at top"),
                muscleGroups = listOf("Shoulders", "Triceps"),
                equipment = listOf("Barbell")
            ),
            Exercise(
                id = "romanian_deadlift",
                name = "Romanian Deadlift",
                description = "Hip hinge focusing on hamstrings.",
                cues = listOf("Hips back", "Feel the stretch", "Don't touch floor"),
                muscleGroups = listOf("Hamstrings", "Glutes"),
                equipment = listOf("Barbell", "Dumbbells")
            ),
            Exercise(
                id = "zercher_squat",
                name = "Zercher Squat",
                description = "Squat with bar in the crooks of elbows. Brutal core demand.",
                cues = listOf("Bar in elbows", "Clasp hands", "Upright torso"),
                muscleGroups = listOf("Quads", "Core", "Upper Back"),
                equipment = listOf("Barbell")
            ),
            Exercise(
                id = "weighted_pullups",
                name = "Weighted Pull-Ups",
                description = "Vertical pull with added resistance.",
                cues = listOf("Chest to bar", "Full hang at bottom", "Control descent"),
                muscleGroups = listOf("Lats", "Biceps", "Upper Back"),
                equipment = listOf("Pull-up Bar", "Dip Belt")
            ),
            Exercise(
                id = "db_tricep_extension",
                name = "Overhead Single-Arm DB Tricep Extension",
                description = "Isolation for the long head of the triceps.",
                cues = listOf("Elbow high", "Deep stretch at bottom", "Full lockout"),
                muscleGroups = listOf("Triceps"),
                equipment = listOf("Dumbbell")
            ),
            Exercise(
                id = "bent_over_row",
                name = "Bent-Over Barbell Rows",
                description = "Classic horizontal pull for back thickness.",
                cues = listOf("Hinged at hips", "Pull to upper stomach", "Squeeze shoulder blades"),
                muscleGroups = listOf("Back", "Biceps", "Rear Delts"),
                equipment = listOf("Barbell")
            ),
            Exercise(
                id = "bulgarian_split_squat",
                name = "Bulgarian Split Squat",
                description = "Unilateral leg movement for quads and glutes.",
                cues = listOf("Rear foot elevated", "Upright torso", "Drive through front heel"),
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = listOf("Dumbbells", "Bench")
            ),
            Exercise(
                id = "calf_raise",
                name = "Standing Calf Raise",
                description = "Isolation for the calves.",
                cues = listOf("Full stretch at bottom", "Explosive up", "1s pause at top"),
                muscleGroups = listOf("Calves"),
                equipment = listOf("Barbell", "Machine")
            ),
            Exercise(
                id = "hanging_knee_raise",
                name = "Hanging Knee Raise",
                description = "Core exercise for lower abs.",
                cues = listOf("No swinging", "Crunch with hips", "Slow descent"),
                muscleGroups = listOf("Abs"),
                equipment = listOf("Pull-up Bar")
            ),
            Exercise(
                id = "incline_bench_press",
                name = "Incline Barbell Bench Press",
                description = "Bench press on an incline for upper chest focus.",
                cues = listOf("30-45 degree incline", "Bar to upper chest", "Tuck elbows slightly"),
                muscleGroups = listOf("Chest", "Shoulders"),
                equipment = listOf("Barbell", "Incline Bench")
            ),
            Exercise(
                id = "db_bench_press",
                name = "Dumbbell Bench Press",
                description = "Unilateral chest press.",
                cues = listOf("Full range of motion", "Dumbbells together at top", "Stable core"),
                muscleGroups = listOf("Chest", "Shoulders", "Triceps"),
                equipment = listOf("Dumbbells", "Bench")
            ),
            Exercise(
                id = "lateral_raise",
                name = "Dumbbell Lateral Raise",
                description = "Isolation for side delts.",
                cues = listOf("Pinkies up", "Slight elbow bend", "Control the descent"),
                muscleGroups = listOf("Shoulders"),
                equipment = listOf("Dumbbells")
            ),
            Exercise(
                id = "hammer_curl",
                name = "Dumbbell Hammer Curl",
                description = "Bicep curl with neutral grip.",
                cues = listOf("Neutral grip", "No swinging", "Squeeze at top"),
                muscleGroups = listOf("Biceps", "Forearms"),
                equipment = listOf("Dumbbells")
            ),
            Exercise(
                id = "skull_crusher",
                name = "EZ-Bar Skull Crusher",
                description = "Tricep isolation.",
                cues = listOf("Elbows tucked", "Lower to forehead", "Full lockout"),
                muscleGroups = listOf("Triceps"),
                equipment = listOf("EZ-Bar", "Bench")
            ),
            Exercise(
                id = "leg_press",
                name = "Leg Press",
                description = "Machine compound for legs.",
                cues = listOf("Feet shoulder width", "Don't lock knees", "Deep range of motion"),
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = listOf("Leg Press Machine")
            ),
            Exercise(
                id = "lat_pulldown",
                name = "Lat Pulldown",
                description = "Machine vertical pull.",
                cues = listOf("Pull to upper chest", "Squeeze lats", "Don't lean back too far"),
                muscleGroups = listOf("Lats", "Upper Back"),
                equipment = listOf("Cable Machine")
            ),
            Exercise(
                id = "seated_row",
                name = "Seated Cable Row",
                description = "Horizontal pull focusing on mid-back.",
                cues = listOf("Chest up", "Pull to navel", "Squeeze shoulder blades"),
                muscleGroups = listOf("Back", "Biceps"),
                equipment = listOf("Cable Machine")
            ),
            Exercise(
                id = "cyber_cluster_squat",
                name = "Cyber Cluster Squats",
                description = "High-intensity squat protocol using rest-pause clusters.",
                cues = listOf("Standard squat form", "15s rest between clusters", "Maintain brace"),
                muscleGroups = listOf("Quads", "Glutes", "Core"),
                equipment = listOf("Barbell", "Rack")
            ),
            Exercise(
                id = "weighted_dip",
                name = "Weighted Dips",
                description = "Powerful tricep and chest builder.",
                cues = listOf("Lean forward for chest", "Upright for triceps", "Full lockout"),
                muscleGroups = listOf("Triceps", "Chest", "Shoulders"),
                equipment = listOf("Dip Station", "Belt")
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
