package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.dao.WorkoutDao
import com.neon.ascent.core.data.local.entity.*
import com.neon.ascent.core.data.mapper.*
import com.neon.ascent.core.domain.repository.WorkoutRepository
import com.neon.ascent.core.domain.workout.models.*
import com.neon.ascent.core.domain.workout.rules.CyberCrappRules
import com.neon.ascent.core.domain.workout.rules.MacroCalculator
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

    override suspend fun getExerciseById(id: String): Exercise? =
        workoutDao.getExerciseDefinitions().first().find { it.id == id }?.toDomain()

    override suspend fun saveExerciseDefinition(exercise: Exercise) {
        workoutDao.insertExerciseDefinition(exercise.toEntity())
    }

    override fun getExerciseFamilies(): Flow<List<ExerciseFamily>> =
        workoutDao.getExerciseDefinitions().map { entities ->
            entities.map { it.toDomain() }
                .groupBy { it.familyId }
                .mapNotNull { (familyId, variants) ->
                    val primary = variants.find { it.isPrimaryVariant } ?: variants.firstOrNull() ?: return@mapNotNull null
                    ExerciseFamily(
                        id = familyId,
                        name = primary.familyName,
                        movementType = primary.movementType,
                        variants = variants
                    )
                }
        }

    override fun getExercisesByFamily(familyId: String): Flow<List<Exercise>> =
        workoutDao.getExercisesByFamily(familyId).map { entities -> entities.map { it.toDomain() } }

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
        val currentProfile = workoutDao.getUserProfile(profile.userId).first()?.toDomain()
        
        workoutDao.insertUserProfile(profile.toEntity())

        // Detection of changes that trigger a FuelSnapshot
        if (currentProfile != null) {
            val changed = currentProfile.weightKg != profile.weightKg ||
                    currentProfile.activityFactor != profile.activityFactor ||
                    currentProfile.somatotype != profile.somatotype ||
                    currentProfile.gender != profile.gender
            
            if (changed) {
                val macros = MacroCalculator.calculateMacros(profile)
                saveFuelSnapshot(
                    FuelSnapshot(
                        weightKg = profile.weightKg,
                        tdee = macros.calories,
                        protein = macros.protein,
                        carb = macros.carbs,
                        fat = macros.fat,
                        activityFactor = profile.activityFactor,
                        somatotype = profile.somatotype
                    )
                )
            }
        }
    }

    override fun getFuelHistory(from: java.time.Instant, to: java.time.Instant): Flow<List<FuelSnapshot>> =
        workoutDao.getFuelHistory(from, to).map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveFuelSnapshot(snapshot: FuelSnapshot) {
        workoutDao.upsertFuelSnapshot(snapshot.toEntity())
    }

    override fun getProtocolRepTargets(): Flow<List<ProtocolRepTarget>> =
        workoutDao.getAllProtocolRepTargets().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveProtocolRepTarget(target: ProtocolRepTarget) {
        workoutDao.upsertProtocolRepTarget(target.toEntity())
    }

    override fun getActiveCycle(userId: String): Flow<ProtocolCycle?> =
        workoutDao.getActiveCycle(userId).map { it?.toDomain() }

    override suspend fun saveProtocolCycle(cycle: ProtocolCycle) {
        workoutDao.upsertProtocolCycle(cycle.toEntity())
    }

    override fun getExerciseMax(familyId: String): Flow<ExerciseMax?> =
        workoutDao.getExerciseMax(familyId).map { it?.toDomain() }

    override fun getAllExerciseMaxes(): Flow<List<ExerciseMax>> =
        workoutDao.getAllExerciseMaxes().map { list -> list.map { it.toDomain() } }

    override suspend fun upsertExerciseMax(max: ExerciseMax) {
        workoutDao.upsertExerciseMax(max.toEntity())
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

    override fun getAugmentActivations(userId: String): Flow<List<AugmentActivation>> =
        workoutDao.getActivationsByUserId(userId).map { entities -> entities.map { it.toDomain() } }

    override fun getActiveAugmentActivations(): Flow<List<AugmentActivation>> =
        workoutDao.getActiveActivations().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveAugmentActivation(activation: AugmentActivation) {
        workoutDao.upsertAugmentActivation(activation.toEntity())
    }

    override suspend fun endAugmentActivation(id: String) {
        workoutDao.endActivation(id)
    }

    override suspend fun seedStarterExercises() {
        seedProtocolRepTargets()
        val exercises = listOf(
            // --- Push / Bench / Chest ---
            Exercise(
                id = "bench_press",
                name = "Bench Press (Barbell)",
                description = "Flat barbell chest press for building push compound power.",
                cues = listOf("Retract scapula", "Touch chest at nipple line", "Drive through feet"),
                muscleGroups = listOf("Chest", "Shoulders", "Triceps"),
                equipment = listOf("Barbell"),
                movementType = MovementType.COMPOUND_UPPER,
                isLockedClassic = true,
                dangerousFor = listOf("Shoulder Pain"),
                familyId = "bench_press",
                familyName = "Bench Press",
                implement = Implement.BARBELL,
                stance = Stance.STANDARD,
                isPrimaryVariant = true
            ),
            Exercise(
                id = "bench_press_dumbbell",
                name = "Bench Press (Dumbbell)",
                description = "Unilateral dumbbell flat bench press for increased range of motion.",
                cues = listOf("Keep dumbbells stable", "Tuck elbows slightly", "Press to center"),
                muscleGroups = listOf("Chest", "Shoulders", "Triceps"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.COMPOUND_UPPER,
                dangerousFor = listOf("Shoulder Pain"),
                familyId = "bench_press",
                familyName = "Bench Press",
                implement = Implement.DUMBBELL,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "bench_press_decline_bb",
                name = "Decline Bench Press (Barbell)",
                description = "Barbell press on a decline bench targeting the lower pectorals with reduced shoulder strain.",
                cues = listOf("Secure legs under pads", "Touch lower chest", "Drive up smoothly"),
                muscleGroups = listOf("Chest", "Triceps"),
                equipment = listOf("Barbell"),
                movementType = MovementType.COMPOUND_UPPER,
                familyId = "bench_press",
                familyName = "Bench Press",
                implement = Implement.BARBELL,
                stance = Stance.DECLINE
            ),
            Exercise(
                id = "bench_press_decline_db",
                name = "Decline Bench Press (Dumbbell)",
                description = "Dumbbell press on a decline bench for lower chest isolation and shoulder comfort.",
                cues = listOf("Lock legs securely", "Control dumbbells down", "Press up and squeeze"),
                muscleGroups = listOf("Chest", "Triceps"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.COMPOUND_UPPER,
                familyId = "bench_press",
                familyName = "Bench Press",
                implement = Implement.DUMBBELL,
                stance = Stance.DECLINE
            ),
            Exercise(
                id = "incline_bench_press",
                name = "Incline Bench Press (Barbell)",
                description = "Barbell chest press on an incline to emphasize upper pectorals.",
                cues = listOf("30-45 degree incline", "Bar to upper chest", "Tuck elbows slightly"),
                muscleGroups = listOf("Chest", "Shoulders"),
                equipment = listOf("Barbell"),
                movementType = MovementType.COMPOUND_UPPER,
                dangerousFor = listOf("Shoulder Pain"),
                familyId = "bench_press",
                familyName = "Bench Press",
                implement = Implement.BARBELL,
                stance = Stance.INCLINE,
                isPrimaryVariant = false
            ),
            Exercise(
                id = "incline_bench_press_dumbbell",
                name = "Incline Bench Press (Dumbbell)",
                description = "Unilateral dumbbell incline chest press for upper chest isolation.",
                cues = listOf("Controlled descent", "Drive dumbbells upward", "Keep wrists straight"),
                muscleGroups = listOf("Chest", "Shoulders"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.COMPOUND_UPPER,
                dangerousFor = listOf("Shoulder Pain"),
                familyId = "bench_press",
                familyName = "Bench Press",
                implement = Implement.DUMBBELL,
                stance = Stance.INCLINE
            ),
            Exercise(
                id = "incline_smith_press",
                name = "Incline Press (Smith Machine)",
                description = "Fixed-path incline press to isolate upper chest safely.",
                cues = listOf("Adjust bench to mid-chest", "Keep elbows tucked slightly", "Touch chest lightly"),
                muscleGroups = listOf("Chest", "Shoulders"),
                equipment = listOf("Machine"),
                movementType = MovementType.COMPOUND_UPPER,
                familyId = "bench_press",
                familyName = "Bench Press",
                implement = Implement.SMITH,
                stance = Stance.INCLINE
            ),
            Exercise(
                id = "chest_press_hammer_strength",
                name = "Chest Press (Hammer Strength)",
                description = "Stability-focused plate-loaded machine press. Ideal for rest-pause to absolute failure.",
                cues = listOf("Keep back against pad", "Explosive press", "Controlled return"),
                muscleGroups = listOf("Chest", "Shoulders", "Triceps"),
                equipment = listOf("Plate Loaded"),
                movementType = MovementType.COMPOUND_UPPER,
                familyId = "bench_press",
                familyName = "Bench Press",
                implement = Implement.PLATE_LOADED,
                stance = Stance.SEATED
            ),
            Exercise(
                id = "chest_press_plate_loaded",
                name = "Chest Press (Plate Loaded)",
                description = "Levered plate-loaded machine for consistent chest tension.",
                cues = listOf("Adjust seat height so handles are mid-chest", "Keep back flat against pad", "Press forward explosively"),
                muscleGroups = listOf("Chest", "Triceps"),
                equipment = listOf("Plate Loaded"),
                movementType = MovementType.COMPOUND_UPPER,
                familyId = "bench_press",
                familyName = "Bench Press",
                implement = Implement.PLATE_LOADED,
                stance = Stance.SEATED
            ),
            Exercise(
                id = "floor_press_dumbbell",
                name = "Floor Press (Dumbbell)",
                description = "Dumbbell press on the floor to limit range of motion and protect the shoulders.",
                cues = listOf("Lie flat on floor", "Pause when elbows touch floor", "Drive up explosively"),
                muscleGroups = listOf("Chest", "Triceps"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.COMPOUND_UPPER,
                familyId = "bench_press",
                familyName = "Bench Press",
                implement = Implement.DUMBBELL,
                stance = Stance.FLOOR
            ),
            Exercise(
                id = "floor_press_bb",
                name = "Floor Press (Barbell)",
                description = "Barbell press lying on floor to build lockout tricep and chest power.",
                cues = listOf("Lie flat on floor", "Elbows touch gently", "Explode to full lockout"),
                muscleGroups = listOf("Chest", "Triceps"),
                equipment = listOf("Barbell"),
                movementType = MovementType.COMPOUND_UPPER,
                familyId = "bench_press",
                familyName = "Bench Press",
                implement = Implement.BARBELL,
                stance = Stance.FLOOR
            ),
            Exercise(
                id = "close_grip_bench_bb",
                name = "Close-Grip Bench Press (Barbell)",
                description = "Barbell flat press with hands shoulder-width to hammer triceps and inner chest.",
                cues = listOf("Hands shoulder-width", "Tuck elbows tight to sides", "Full tricep lockout"),
                muscleGroups = listOf("Triceps", "Chest"),
                equipment = listOf("Barbell"),
                movementType = MovementType.COMPOUND_UPPER,
                familyId = "bench_press",
                familyName = "Bench Press",
                implement = Implement.BARBELL,
                stance = Stance.CLOSE_GRIP
            ),
            Exercise(
                id = "close_grip_smith_press",
                name = "Close Grip Press (Smith Machine)",
                description = "High-stability tricep focused press.",
                cues = listOf("Grip shoulder-width", "Touch lower chest", "Full tricep lockout"),
                muscleGroups = listOf("Triceps", "Chest"),
                equipment = listOf("Machine"),
                movementType = MovementType.ISOLATION_UPPER,
                familyId = "bench_press",
                familyName = "Bench Press",
                implement = Implement.SMITH,
                stance = Stance.CLOSE_GRIP
            ),
            Exercise(
                id = "chest_fly_cable",
                name = "Chest Fly (Cable)",
                description = "Continuous tension chest isolation using cables.",
                cues = listOf("Slight bend in elbows", "Hug a tree at the finish", "Squeeze chest hard"),
                muscleGroups = listOf("Chest"),
                equipment = listOf("Cable"),
                movementType = MovementType.ISOLATION_UPPER,
                familyId = "chest_fly",
                familyName = "Chest Fly",
                implement = Implement.CABLE,
                stance = Stance.STANDING
            ),
            Exercise(
                id = "pec_deck",
                name = "Pec Deck (Machine)",
                description = "Machine chest fly providing constant tension and deep chest stretch.",
                cues = listOf("Elbows slightly bent", "Bring pads together", "Hold squeeze 1s"),
                muscleGroups = listOf("Chest"),
                equipment = listOf("Machine"),
                movementType = MovementType.ISOLATION_UPPER,
                familyId = "chest_fly",
                familyName = "Chest Fly",
                implement = Implement.MACHINE,
                stance = Stance.SEATED
            ),
            Exercise(
                id = "pushup_bodyweight",
                name = "Push Up (Bodyweight)",
                description = "Fundamental horizontal push movement.",
                cues = listOf("Keep body in straight line", "Elbows tucked 45 degrees", "Chest to floor"),
                muscleGroups = listOf("Chest", "Shoulders", "Triceps"),
                equipment = listOf("Bodyweight"),
                movementType = MovementType.COMPOUND_UPPER,
                familyId = "push_up",
                familyName = "Push-Up",
                implement = Implement.BODYWEIGHT,
                stance = Stance.STANDARD,
                allowsAddedLoad = false
            ),
            Exercise(
                id = "push_up_weighted",
                name = "Push Up (Weighted)",
                description = "Horizontal push with added weight plate or vest.",
                cues = listOf("Keep core braced tight", "Plate securely on upper back", "Controlled descent"),
                muscleGroups = listOf("Chest", "Shoulders", "Triceps"),
                equipment = listOf("Weighted"),
                movementType = MovementType.COMPOUND_UPPER,
                familyId = "push_up",
                familyName = "Push-Up",
                implement = Implement.BODYWEIGHT,
                stance = Stance.STANDARD,
                allowsAddedLoad = true
            ),
            Exercise(
                id = "decline_pushup_bodyweight",
                name = "Decline Push Up (Bodyweight)",
                description = "Push up with feet elevated to target the upper chest and front delts.",
                cues = listOf("Elevate feet on a box or bench", "Keep hips in line with shoulders", "Touch nose/chest gently to floor"),
                muscleGroups = listOf("Chest", "Shoulders", "Triceps"),
                equipment = listOf("Bodyweight"),
                movementType = MovementType.COMPOUND_UPPER,
                familyId = "push_up",
                familyName = "Push-Up",
                implement = Implement.BODYWEIGHT,
                stance = Stance.DECLINE,
                allowsAddedLoad = false
            ),
            Exercise(
                id = "atlas_pushup_bodyweight",
                name = "Atlas Push Up (Bodyweight)",
                description = "Deficit push up using three elevated contact points for a maximum chest stretch.",
                cues = listOf("Place hands on two blocks/benches", "Deep stretch at bottom below hand level", "Drive up and contract chest"),
                muscleGroups = listOf("Chest", "Shoulders", "Triceps"),
                equipment = listOf("Bodyweight"),
                movementType = MovementType.COMPOUND_UPPER,
                familyId = "push_up",
                familyName = "Push-Up",
                implement = Implement.BODYWEIGHT,
                stance = Stance.DEFICIT,
                allowsAddedLoad = false
            ),
            Exercise(
                id = "dip_bodyweight",
                name = "Dip (Bodyweight)",
                description = "Strict bodyweight dips.",
                cues = listOf("Lower until arms hit 90 degrees", "Control the descent", "Squeeze triceps at top"),
                muscleGroups = listOf("Triceps", "Chest", "Shoulders"),
                equipment = listOf("Bodyweight"),
                movementType = MovementType.COMPOUND_UPPER,
                familyId = "dip",
                familyName = "Dip",
                implement = Implement.BODYWEIGHT,
                stance = Stance.STANDARD,
                allowsAddedLoad = false
            ),
            Exercise(
                id = "weighted_dip",
                name = "Dip (Weighted)",
                description = "Powerful tricep and chest builder.",
                cues = listOf("Lean forward for chest", "Upright for triceps", "Full lockout"),
                muscleGroups = listOf("Triceps", "Chest", "Shoulders"),
                equipment = listOf("Weighted"),
                movementType = MovementType.COMPOUND_UPPER,
                familyId = "dip",
                familyName = "Dip",
                implement = Implement.BODYWEIGHT,
                stance = Stance.STANDARD,
                allowsAddedLoad = true,
                isPrimaryVariant = false
            ),

            // --- Shoulders & Overhead Press ---
            Exercise(
                id = "military_press",
                name = "Overhead Press (Barbell)",
                description = "Strict overhead barbell press.",
                cues = listOf("Squeeze glutes", "Head back to clear bar", "Punch through at top"),
                muscleGroups = listOf("Shoulders", "Triceps"),
                equipment = listOf("Barbell"),
                movementType = MovementType.COMPOUND_UPPER,
                dangerousFor = listOf("Shoulder Pain", "Lower Back Pain"),
                familyId = "overhead_press",
                familyName = "Overhead Press",
                implement = Implement.BARBELL,
                stance = Stance.STANDING,
                isPrimaryVariant = true
            ),
            Exercise(
                id = "shoulder_press_dumbbell",
                name = "Shoulder Press (Dumbbell)",
                description = "Strict seated or standing overhead dumbbell press.",
                cues = listOf("Keep wrists straight", "Don't flare elbows fully", "Press to full lockout"),
                muscleGroups = listOf("Shoulders", "Triceps"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.COMPOUND_UPPER,
                dangerousFor = listOf("Shoulder Pain"),
                familyId = "overhead_press",
                familyName = "Overhead Press",
                implement = Implement.DUMBBELL,
                stance = Stance.SEATED
            ),
            Exercise(
                id = "shoulder_press_kettlebell",
                name = "Shoulder Press (Kettlebell)",
                description = "Overhead kettlebell press from the front rack position.",
                cues = listOf("Rack KB tight against chest", "Press up in a slight arc", "Lock out fully at top"),
                muscleGroups = listOf("Shoulders", "Triceps"),
                equipment = listOf("Kettlebell"),
                movementType = MovementType.COMPOUND_UPPER,
                dangerousFor = listOf("Shoulder Pain"),
                familyId = "overhead_press",
                familyName = "Overhead Press",
                implement = Implement.KETTLEBELL,
                stance = Stance.STANDING
            ),
            Exercise(
                id = "shoulder_press_hammer_strength",
                name = "Shoulder Press (Hammer Strength)",
                description = "High-stability shoulder press machine. Maximizes deltoid isolation.",
                cues = listOf("Sit deep into seat", "Maintain arch in upper back", "Press to full lockout"),
                muscleGroups = listOf("Shoulders", "Triceps"),
                equipment = listOf("Plate Loaded"),
                movementType = MovementType.COMPOUND_UPPER,
                familyId = "overhead_press",
                familyName = "Overhead Press",
                implement = Implement.PLATE_LOADED,
                stance = Stance.SEATED
            ),
            Exercise(
                id = "seated_smith_overhead_press",
                name = "Overhead Press (Smith Machine)",
                description = "Seated fixed-path overhead press for maximum stability.",
                cues = listOf("Set bar height at nose level", "Brace core against bench", "Punch up hard"),
                muscleGroups = listOf("Shoulders", "Triceps"),
                equipment = listOf("Machine"),
                movementType = MovementType.COMPOUND_UPPER,
                familyId = "overhead_press",
                familyName = "Overhead Press",
                implement = Implement.SMITH,
                stance = Stance.SEATED
            ),
            Exercise(
                id = "landmine_press",
                name = "Landmine Press (Barbell)",
                description = "Angled unilateral or bilateral pressing movement safe for cranky shoulders.",
                cues = listOf("Lean slightly into bar", "Press upward along arc", "Lockout without shrugging"),
                muscleGroups = listOf("Shoulders", "Chest", "Triceps"),
                equipment = listOf("Barbell"),
                movementType = MovementType.COMPOUND_UPPER,
                familyId = "landmine_press",
                familyName = "Landmine Press",
                implement = Implement.BARBELL,
                stance = Stance.STANDING
            ),
            Exercise(
                id = "landmine_press_sa",
                name = "Single-Arm Landmine Press (Barbell)",
                description = "Unilateral landmine press building shoulder stability and core anti-rotation.",
                cues = listOf("Staggered stance", "Press along arc", "Squeeze delt at peak"),
                muscleGroups = listOf("Shoulders", "Core"),
                equipment = listOf("Barbell"),
                movementType = MovementType.COMPOUND_UPPER,
                familyId = "landmine_press",
                familyName = "Landmine Press",
                implement = Implement.BARBELL,
                stance = Stance.SINGLE_ARM
            ),
            Exercise(
                id = "lateral_raise",
                name = "Lateral Raise (Dumbbell)",
                description = "Isolation for side delts.",
                cues = listOf("Pinkies up", "Slight elbow bend", "Control the descent"),
                muscleGroups = listOf("Shoulders"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.ISOLATION_UPPER,
                familyId = "lateral_raise",
                familyName = "Lateral Raise",
                implement = Implement.DUMBBELL,
                stance = Stance.STANDING,
                isPrimaryVariant = false
            ),
            Exercise(
                id = "lateral_raise_cable",
                name = "Lateral Raise (Cable)",
                description = "Constant tension side delt cable raise.",
                cues = listOf("Raise hand slightly forward", "Slight elbow bend", "Slow eccentric"),
                muscleGroups = listOf("Shoulders"),
                equipment = listOf("Cable"),
                movementType = MovementType.ISOLATION_UPPER,
                familyId = "lateral_raise",
                familyName = "Lateral Raise",
                implement = Implement.CABLE,
                stance = Stance.STANDING
            ),
            Exercise(
                id = "lateral_raise_kettlebell",
                name = "Lateral Raise (Kettlebell)",
                description = "Side delt raise utilizing kettlebells for unique load distribution.",
                cues = listOf("Hold KB handle firmly", "Raise arms to parallel", "Control gravity's pull"),
                muscleGroups = listOf("Shoulders"),
                equipment = listOf("Kettlebell"),
                movementType = MovementType.ISOLATION_UPPER,
                familyId = "lateral_raise",
                familyName = "Lateral Raise",
                implement = Implement.KETTLEBELL,
                stance = Stance.STANDING
            ),
            Exercise(
                id = "rear_delt_fly_dumbbell",
                name = "Rear Delt Fly (Dumbbell)",
                description = "Bent-over rear lateral raise.",
                cues = listOf("Hinge forward at hips", "Fly dumbbells out to sides", "Squeeze rear delts"),
                muscleGroups = listOf("Rear Delts"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.ISOLATION_UPPER,
                familyId = "rear_delt_fly",
                familyName = "Rear Delt Fly",
                implement = Implement.DUMBBELL,
                stance = Stance.STANDING
            ),
            Exercise(
                id = "rear_delt_fly_machine",
                name = "Rear Delt Fly (Cable)",
                description = "Cable or machine reverse fly for rear delt isolation.",
                cues = listOf("Keep arms parallel to ground", "Pull back with shoulder joints", "Pause at peak contraction"),
                muscleGroups = listOf("Rear Delts"),
                equipment = listOf("Cable"),
                movementType = MovementType.ISOLATION_UPPER,
                familyId = "rear_delt_fly",
                familyName = "Rear Delt Fly",
                implement = Implement.CABLE,
                stance = Stance.SEATED
            ),
            Exercise(
                id = "facepull_cable",
                name = "Face Pull (Cable)",
                description = "Rear delt and rotator cuff cable pull.",
                cues = listOf("Pull rope towards forehead", "Separate hands at peak", "Squeeze rear delts"),
                muscleGroups = listOf("Rear Delts", "Upper Back", "Rotator Cuff"),
                equipment = listOf("Cable"),
                movementType = MovementType.ISOLATION_UPPER,
                familyId = "facepull",
                familyName = "Face Pull",
                implement = Implement.CABLE,
                stance = Stance.STANDING
            ),

            // --- Pull / Back Width / Back Thickness ---
            Exercise(
                id = "pullup_bodyweight",
                name = "Pull Up (Bodyweight)",
                description = "Classic vertical bodyweight pull.",
                cues = listOf("Chest to bar", "Dead hang at bottom", "Active scapula"),
                muscleGroups = listOf("Lats", "Biceps", "Upper Back"),
                equipment = listOf("Bodyweight"),
                movementType = MovementType.BACK_WIDTH,
                familyId = "pull_up",
                familyName = "Pull-Up",
                implement = Implement.BODYWEIGHT,
                stance = Stance.STANDARD,
                allowsAddedLoad = false,
                isPrimaryVariant = true
            ),
            Exercise(
                id = "weighted_pullups",
                name = "Pull Up (Weighted)",
                description = "Vertical pull with added resistance.",
                cues = listOf("Chest to bar", "Full hang at bottom", "Control descent"),
                muscleGroups = listOf("Lats", "Biceps", "Upper Back"),
                equipment = listOf("Weighted"),
                movementType = MovementType.BACK_WIDTH,
                familyId = "pull_up",
                familyName = "Pull-Up",
                implement = Implement.BODYWEIGHT,
                stance = Stance.STANDARD,
                allowsAddedLoad = true,
                isPrimaryVariant = false
            ),
            Exercise(
                id = "chinup_bodyweight",
                name = "Chin Up (Bodyweight)",
                description = "Underhand vertical bodyweight pull maximizing bicep recruitment.",
                cues = listOf("Supinated grip", "Drive elbows down", "Chest to bar"),
                muscleGroups = listOf("Lats", "Biceps", "Upper Back"),
                equipment = listOf("Bodyweight"),
                movementType = MovementType.BACK_WIDTH,
                familyId = "chin_up",
                familyName = "Chin Up",
                implement = Implement.BODYWEIGHT,
                stance = Stance.STANDARD,
                allowsAddedLoad = false
            ),
            Exercise(
                id = "chinup_weighted",
                name = "Chin Up (Weighted)",
                description = "Weighted underhand vertical pull.",
                cues = listOf("Supinated grip", "Squeeze shoulder blades at top", "Control the eccentric"),
                muscleGroups = listOf("Lats", "Biceps", "Upper Back"),
                equipment = listOf("Weighted"),
                movementType = MovementType.BACK_WIDTH,
                familyId = "chin_up",
                familyName = "Chin Up",
                implement = Implement.BODYWEIGHT,
                stance = Stance.STANDARD,
                allowsAddedLoad = true
            ),
            Exercise(
                id = "lat_pulldown",
                name = "Lat Pulldown (Cable)",
                description = "Machine vertical pull.",
                cues = listOf("Pull to upper chest", "Squeeze lats", "Don't lean back too far"),
                muscleGroups = listOf("Lats", "Upper Back"),
                equipment = listOf("Cable"),
                movementType = MovementType.BACK_WIDTH,
                familyId = "lat_pulldown",
                familyName = "Lat Pulldown",
                implement = Implement.CABLE,
                stance = Stance.SEATED
            ),
            Exercise(
                id = "pullover_db",
                name = "Pullover (Dumbbell)",
                description = "Cross-bench dumbbell pullover for lat and serratus stretch-mediated hypertrophy.",
                cues = listOf("Lie across bench", "Keep slight elbow bend", "Deep lat stretch at bottom"),
                muscleGroups = listOf("Lats", "Chest"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.BACK_WIDTH,
                familyId = "pullover",
                familyName = "Pullover",
                implement = Implement.DUMBBELL,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "pullover_cable",
                name = "Straight-Arm Pulldown (Cable)",
                description = "Standing cable lat pullover maintaining continuous lat tension.",
                cues = listOf("Hips hinged slightly", "Sweep bar to thighs", "Keep arms nearly straight"),
                muscleGroups = listOf("Lats"),
                equipment = listOf("Cable"),
                movementType = MovementType.BACK_WIDTH,
                familyId = "pullover",
                familyName = "Pullover",
                implement = Implement.CABLE,
                stance = Stance.STANDING
            ),
            Exercise(
                id = "bent_over_row",
                name = "Bent-Over Row (Barbell)",
                description = "Classic horizontal pull for back thickness.",
                cues = listOf("Hinged at hips", "Pull to upper stomach", "Squeeze shoulder blades"),
                muscleGroups = listOf("Back", "Biceps", "Rear Delts"),
                equipment = listOf("Barbell"),
                movementType = MovementType.BACK_THICKNESS,
                dangerousFor = listOf("Lower Back Pain"),
                familyId = "rows",
                familyName = "Row",
                implement = Implement.BARBELL,
                stance = Stance.BENT_OVER,
                isPrimaryVariant = true
            ),
            Exercise(
                id = "row_inverted",
                name = "Inverted Row (Bodyweight)",
                description = "Horizontal bodyweight row beneath a bar or rings.",
                cues = listOf("Keep body in a straight plank", "Pull chest to bar", "Squeeze shoulder blades"),
                muscleGroups = listOf("Back", "Biceps"),
                equipment = listOf("Bodyweight"),
                movementType = MovementType.BACK_THICKNESS,
                familyId = "rows",
                familyName = "Row",
                implement = Implement.BODYWEIGHT,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "row_kb_sa",
                name = "Single-Arm Row (Kettlebell)",
                description = "Unilateral kettlebell row from a staggered stance.",
                cues = listOf("Flat back", "Drive elbow towards hip", "Full stretch at bottom"),
                muscleGroups = listOf("Back", "Biceps"),
                equipment = listOf("Kettlebell"),
                movementType = MovementType.BACK_THICKNESS,
                familyId = "rows",
                familyName = "Row",
                implement = Implement.KETTLEBELL,
                stance = Stance.SINGLE_ARM
            ),
            Exercise(
                id = "one_arm_row_dumbbell",
                name = "One-Arm Row (Dumbbell)",
                description = "Unilateral dumbbell row for lat isolation.",
                cues = listOf("Keep back flat", "Pull dumbbell to hip", "Stretch fully at bottom"),
                muscleGroups = listOf("Lats", "Upper Back", "Core"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.BACK_THICKNESS,
                familyId = "rows",
                familyName = "Row",
                implement = Implement.DUMBBELL,
                stance = Stance.SINGLE_ARM
            ),
            Exercise(
                id = "seated_row",
                name = "Seated Row (Cable)",
                description = "Horizontal pull focusing on mid-back.",
                cues = listOf("Chest up", "Pull to navel", "Squeeze shoulder blades"),
                muscleGroups = listOf("Back", "Biceps"),
                equipment = listOf("Cable"),
                movementType = MovementType.BACK_THICKNESS,
                familyId = "rows",
                familyName = "Row",
                implement = Implement.CABLE,
                stance = Stance.SEATED
            ),
            Exercise(
                id = "lat_row_plate_loaded",
                name = "Lat Row (Plate Loaded)",
                description = "Independent-arm plate-loaded row machine for back thickness.",
                cues = listOf("Brace chest against pad", "Pull elbow far back", "Squeeze lat and mid-back"),
                muscleGroups = listOf("Upper Back", "Lats", "Biceps"),
                equipment = listOf("Plate Loaded"),
                movementType = MovementType.BACK_THICKNESS,
                familyId = "rows",
                familyName = "Row",
                implement = Implement.PLATE_LOADED,
                stance = Stance.SEATED
            ),
            Exercise(
                id = "tbar_row_chest_supported",
                name = "T-Bar Row (Chest Supported)",
                description = "Stability-focused row machine that eliminates lower back strain.",
                cues = listOf("Lean chest into pad", "Drive elbows back", "Squeeze mid-back hard"),
                muscleGroups = listOf("Back", "Biceps"),
                equipment = listOf("Plate Loaded"),
                movementType = MovementType.BACK_THICKNESS,
                familyId = "rows",
                familyName = "Row",
                implement = Implement.PLATE_LOADED,
                stance = Stance.CHEST_SUPPORTED
            ),
            Exercise(
                id = "shrug_bb",
                name = "Shrug (Barbell)",
                description = "Heavy barbell shrug for upper trapezius thickness.",
                cues = listOf("Stand tall", "Elevate shoulders straight up", "Hold squeeze for 1s"),
                muscleGroups = listOf("Traps"),
                equipment = listOf("Barbell"),
                movementType = MovementType.BACK_THICKNESS,
                familyId = "shrug",
                familyName = "Shrug",
                implement = Implement.BARBELL,
                stance = Stance.STANDING
            ),
            Exercise(
                id = "shrug_db",
                name = "Shrug (Dumbbell)",
                description = "Dumbbell shrug with neutral grip allowing natural arm path.",
                cues = listOf("Arms at sides", "Shrug up toward ears", "Control eccentric down"),
                muscleGroups = listOf("Traps"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.BACK_THICKNESS,
                familyId = "shrug",
                familyName = "Shrug",
                implement = Implement.DUMBBELL,
                stance = Stance.STANDING
            ),
            Exercise(
                id = "shrug_smith",
                name = "Shrug (Smith Machine)",
                description = "Fixed track shrug for maximum trap isolation and heavy loading.",
                cues = listOf("Set bar at thigh level", "Strict vertical shrugging motion", "No rolling shoulders"),
                muscleGroups = listOf("Traps"),
                equipment = listOf("Machine"),
                movementType = MovementType.BACK_THICKNESS,
                familyId = "shrug",
                familyName = "Shrug",
                implement = Implement.SMITH,
                stance = Stance.STANDING
            ),
            Exercise(
                id = "rack_pull_below_knee",
                name = "Rack Pull (Below Knee)",
                description = "Partial range deadlift focused on back thickness and traps.",
                cues = listOf("Set pins below knee", "Drag bar up shins", "Lockout hard at top"),
                muscleGroups = listOf("Back", "Traps", "Forearms"),
                equipment = listOf("Barbell"),
                movementType = MovementType.BACK_THICKNESS,
                dangerousFor = listOf("Lower Back Pain"),
                familyId = "rows",
                familyName = "Row",
                implement = Implement.BARBELL,
                stance = Stance.DEFICIT
            ),

            // --- Arms (Biceps & Triceps) ---
            Exercise(
                id = "bicep_curl_barbell",
                name = "Bicep Curl (Barbell)",
                description = "Traditional barbell bicep curl.",
                cues = listOf("Keep elbows locked by side", "Don't lean or swing", "Squeeze at top"),
                muscleGroups = listOf("Biceps", "Forearms"),
                equipment = listOf("Barbell"),
                movementType = MovementType.ISOLATION_UPPER,
                familyId = "bicep_curl",
                familyName = "Bicep Curl",
                implement = Implement.BARBELL,
                stance = Stance.STANDING
            ),
            Exercise(
                id = "bicep_curl_dumbbell",
                name = "Bicep Curl (Dumbbell)",
                description = "Dumbbell curls with wrist supination.",
                cues = listOf("Turn palms up as you lift", "Squeeze biceps at peak", "Control down"),
                muscleGroups = listOf("Biceps", "Brachialis"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.ISOLATION_UPPER,
                familyId = "bicep_curl",
                familyName = "Bicep Curl",
                implement = Implement.DUMBBELL,
                stance = Stance.STANDING
            ),
            Exercise(
                id = "curl_cable",
                name = "Bicep Curl (Cable)",
                description = "Standing cable curl providing continuous bicep tension throughout the ROM.",
                cues = listOf("Pin elbows to sides", "Curl bar to chin level", "Slow 3s eccentric"),
                muscleGroups = listOf("Biceps"),
                equipment = listOf("Cable"),
                movementType = MovementType.ISOLATION_UPPER,
                familyId = "bicep_curl",
                familyName = "Bicep Curl",
                implement = Implement.CABLE,
                stance = Stance.STANDING
            ),
            Exercise(
                id = "preacher_curl_ezbar",
                name = "Preacher Curl (EZ-Bar)",
                description = "EZ-Bar preacher bench curls for strict bicep isolation.",
                cues = listOf("Keep armpits snug to pad", "Slow controlled negative", "Squeeze at top"),
                muscleGroups = listOf("Biceps", "Brachioradialis"),
                equipment = listOf("EZ-Bar"),
                movementType = MovementType.ISOLATION_UPPER,
                familyId = "preacher_curl",
                familyName = "Preacher Curl",
                implement = Implement.EZ_BAR,
                stance = Stance.SEATED
            ),
            Exercise(
                id = "jerry_curl",
                name = "Jerry Curl (Dumbbell)",
                description = "High-intensity bicep protocol emphasizing the bottom stretch and peak supination.",
                cues = listOf("Start with back of hand against thigh", "Full supination + hard squeeze at peak", "Emphasize deep stretch on eccentric", "Torso stable, strict form"),
                muscleGroups = listOf("Biceps", "Brachialis"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.ISOLATION_UPPER,
                gifAssetPath = "exercises/jerry_curl.gif",
                familyId = "jerry_curl",
                familyName = "Jerry Curl",
                implement = Implement.DUMBBELL,
                stance = Stance.STANDING,
                isPrimaryVariant = true
            ),
            Exercise(
                id = "jerry_curl_kettlebell",
                name = "Jerry Curl (Kettlebell)",
                description = "Jerry Curl supination bicep curls utilizing kettlebells for enhanced bottom stretch.",
                cues = listOf("Start with pronated grip", "Supinate wrist on ascend", "Squeeze bicep at peak"),
                muscleGroups = listOf("Biceps", "Brachialis"),
                equipment = listOf("Kettlebell"),
                movementType = MovementType.ISOLATION_UPPER,
                familyId = "jerry_curl",
                familyName = "Jerry Curl",
                implement = Implement.KETTLEBELL,
                stance = Stance.STANDING
            ),
            Exercise(
                id = "hammer_curl",
                name = "Hammer Curl (Dumbbell)",
                description = "Bicep curl with neutral grip.",
                cues = listOf("Neutral grip", "No swinging", "Squeeze at top"),
                muscleGroups = listOf("Biceps", "Forearms"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.ISOLATION_UPPER,
                familyId = "hammer_curl",
                familyName = "Hammer Curl",
                implement = Implement.DUMBBELL,
                stance = Stance.STANDING,
                isPrimaryVariant = false
            ),
            Exercise(
                id = "hammer_curl_kettlebell",
                name = "Hammer Curl (Kettlebell)",
                description = "Kettlebell hammer curl with a neutral grip for forearm and brachialis thickness.",
                cues = listOf("Keep wrists locked", "Don't swing", "Control the negative"),
                muscleGroups = listOf("Biceps", "Forearms"),
                equipment = listOf("Kettlebell"),
                movementType = MovementType.ISOLATION_UPPER,
                familyId = "hammer_curl",
                familyName = "Hammer Curl",
                implement = Implement.KETTLEBELL,
                stance = Stance.STANDING
            ),
            Exercise(
                id = "tricep_pushdown_cable",
                name = "Tricep Pushdown (Cable)",
                description = "Cable rope or bar tricep pushdown isolation.",
                cues = listOf("Pin elbows to ribcage", "Push down and separate hands", "Full lockout squeeze"),
                muscleGroups = listOf("Triceps"),
                equipment = listOf("Cable"),
                movementType = MovementType.ISOLATION_UPPER,
                familyId = "tricep_pushdown",
                familyName = "Tricep Pushdown",
                implement = Implement.CABLE,
                stance = Stance.STANDING
            ),
            Exercise(
                id = "db_tricep_extension",
                name = "Tricep Extension (Dumbbell)",
                description = "Isolation for the long head of the triceps.",
                cues = listOf("Elbow high", "Deep stretch at bottom", "Full lockout"),
                muscleGroups = listOf("Triceps"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.ISOLATION_UPPER,
                familyId = "tricep_extension",
                familyName = "Tricep Extension",
                implement = Implement.DUMBBELL,
                stance = Stance.SINGLE_ARM,
                isPrimaryVariant = true
            ),
            Exercise(
                id = "tricep_extension_cable_oh",
                name = "Overhead Tricep Extension (Cable)",
                description = "Overhead rope extension placing the triceps long head under loaded stretch.",
                cues = listOf("Face away from stack", "Deep elbow flexion at bottom", "Punch hands forward and separate"),
                muscleGroups = listOf("Triceps"),
                equipment = listOf("Cable"),
                movementType = MovementType.ISOLATION_UPPER,
                familyId = "tricep_extension",
                familyName = "Tricep Extension",
                implement = Implement.CABLE,
                stance = Stance.STANDING
            ),
            Exercise(
                id = "skull_crusher",
                name = "Skull Crusher (EZ-Bar)",
                description = "Tricep isolation.",
                cues = listOf("Elbows tucked", "Lower to forehead", "Full lockout"),
                muscleGroups = listOf("Triceps"),
                equipment = listOf("EZ-Bar"),
                movementType = MovementType.ISOLATION_UPPER,
                dangerousFor = listOf("Elbow Pain"),
                familyId = "skull_crusher",
                familyName = "Skull Crusher",
                implement = Implement.EZ_BAR,
                stance = Stance.STANDARD
            ),

            // --- Quads & Squat Family ---
            Exercise(
                id = "back_squat",
                name = "Back Squat (Barbell)",
                description = "King of leg exercises. Full body demand.",
                cues = listOf("Brace core", "Hips back first", "Break parallel"),
                muscleGroups = listOf("Quads", "Glutes", "Hamstrings", "Lower Back"),
                equipment = listOf("Barbell"),
                movementType = MovementType.QUAD_DOMINANT,
                isLockedClassic = true,
                dangerousFor = listOf("Knee Pain", "Lower Back Pain"),
                familyId = "squat",
                familyName = "Squat",
                implement = Implement.BARBELL,
                stance = Stance.BACK,
                isPrimaryVariant = true
            ),
            Exercise(
                id = "front_squat",
                name = "Front Squat (Barbell)",
                description = "Barbell squat loaded in front, emphasizing the quads and upper back.",
                cues = listOf("High elbows", "Brace core", "Upright torso"),
                muscleGroups = listOf("Quads", "Glutes", "Core", "Upper Back"),
                equipment = listOf("Barbell"),
                movementType = MovementType.QUAD_DOMINANT,
                dangerousFor = listOf("Knee Pain"),
                familyId = "squat",
                familyName = "Squat",
                implement = Implement.BARBELL,
                stance = Stance.FRONT
            ),
            Exercise(
                id = "goblet_squat",
                name = "Goblet Squat (Dumbbell)",
                description = "A quad-dominant squat holding a single dumbbell in front.",
                cues = listOf("Hold dumbbell close to chest", "Keep elbows tucked", "Sit deep into hips"),
                muscleGroups = listOf("Quads", "Glutes", "Core"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "squat",
                familyName = "Squat",
                implement = Implement.DUMBBELL,
                stance = Stance.GOBLET
            ),
            Exercise(
                id = "squat_bodyweight",
                name = "Air Squat (Bodyweight)",
                description = "Fundamental bodyweight squat targeting mobility, endurance, and neural groove.",
                cues = listOf("Feet shoulder-width", "Reach hips back", "Knees track over toes"),
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = listOf("Bodyweight"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "squat",
                familyName = "Squat",
                implement = Implement.BODYWEIGHT,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "squat_safety_bar",
                name = "Safety-Bar Squat",
                description = "Squat using safety squat bar to reduce shoulder and wrist strain while loading the anterior chain.",
                cues = listOf("Hold handles lightly", "Stay upright against pad", "Drive through mid-foot"),
                muscleGroups = listOf("Quads", "Glutes", "Upper Back"),
                equipment = listOf("Specialty Bar"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "squat",
                familyName = "Squat",
                implement = Implement.SPECIALTY_BAR,
                stance = Stance.BACK,
                specialtyBar = "SAFETY"
            ),
            Exercise(
                id = "squat_box",
                name = "Box Squat (Barbell)",
                description = "Barbell squat to a parallel box to build explosive hip drive and break the stretch reflex.",
                cues = listOf("Sit back onto box", "Pause without relaxing core", "Explode off box"),
                muscleGroups = listOf("Glutes", "Quads", "Hamstrings"),
                equipment = listOf("Barbell"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "squat",
                familyName = "Squat",
                implement = Implement.BARBELL,
                stance = Stance.BOX
            ),
            Exercise(
                id = "squat_smith",
                name = "Squat (Smith Machine)",
                description = "Fixed track squat allowing targeted quad emphasis and feet-forward foot placements.",
                cues = listOf("Feet placed slightly forward", "Squat deep into knees", "Drive through mid-foot"),
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = listOf("Machine"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "squat",
                familyName = "Squat",
                implement = Implement.SMITH,
                stance = Stance.BACK
            ),
            Exercise(
                id = "cyber_cluster_squat",
                name = "Cyber Cluster Squats (Barbell)",
                description = "High-intensity squat protocol using rest-pause clusters.",
                cues = listOf("Standard squat form", "15s rest between clusters", "Maintain brace"),
                muscleGroups = listOf("Quads", "Glutes", "Core"),
                equipment = listOf("Barbell"),
                movementType = MovementType.QUAD_DOMINANT,
                dangerousFor = listOf("Knee Pain", "Lower Back Pain"),
                familyId = "squat",
                familyName = "Squat",
                implement = Implement.BARBELL,
                stance = Stance.BACK
            ),
            Exercise(
                id = "zercher_squat",
                name = "Zercher Squat (Barbell)",
                description = "Squat with bar in the crooks of elbows. Brutal core demand.",
                cues = listOf("Bar in elbows", "Clasp hands", "Upright torso"),
                muscleGroups = listOf("Quads", "Core", "Upper Back"),
                equipment = listOf("Barbell"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "squat",
                familyName = "Squat",
                implement = Implement.BARBELL,
                stance = Stance.ZERCHER,
                isPrimaryVariant = false
            ),
            Exercise(
                id = "hack_squat_machine",
                name = "Hack Squat (Machine)",
                description = "Fixed-path squat focusing on the quadriceps with full back support.",
                cues = listOf("Shoulders against pads", "Feet low on platform for quads", "Push up and release handles"),
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = listOf("Machine"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "squat",
                familyName = "Squat",
                implement = Implement.MACHINE,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "hack_squat_plate_loaded",
                name = "Hack Squat (Plate Loaded)",
                description = "Sled machine squat emphasizing the quadriceps.",
                cues = listOf("Back flat against backrest", "Feet shoulder-width on platform", "Drive through heels"),
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = listOf("Plate Loaded"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "squat",
                familyName = "Squat",
                implement = Implement.PLATE_LOADED,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "belt_squat",
                name = "Belt Squat",
                description = "Lower body squat that removes all spinal loading. Ideal for lower back issues.",
                cues = listOf("Secure belt to hips", "Stand tall to release weight", "Sit deep into the hole"),
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = listOf("Machine"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "squat",
                familyName = "Squat",
                implement = Implement.MACHINE,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "pendulum_squat",
                name = "Pendulum Squat",
                description = "Arc-path machine squat that provides incredible quad stretch and stability.",
                cues = listOf("Maintain back contact", "Slow controlled negative", "Drive through mid-foot"),
                muscleGroups = listOf("Quads"),
                equipment = listOf("Machine"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "squat",
                familyName = "Squat",
                implement = Implement.MACHINE,
                stance = Stance.STANDARD
            ),

            // --- Leg Press Family ---
            Exercise(
                id = "leg_press_45",
                name = "Leg Press (45° Plate Loaded)",
                description = "Heavy 45-degree sled press maximizing quad loading without spinal compression.",
                cues = listOf("Feet shoulder-width", "Do not allow lower back to round off pad", "Drive through heels/mid-foot"),
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = listOf("Plate Loaded"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "leg_press",
                familyName = "Leg Press",
                implement = Implement.PLATE_LOADED,
                stance = Stance.STANDARD,
                isPrimaryVariant = true
            ),
            Exercise(
                id = "leg_press_horizontal",
                name = "Leg Press (Horizontal Machine)",
                description = "Seated cable or selectorized horizontal leg press for controlled quad work.",
                cues = listOf("Seat adjusted close", "Smooth tempo", "Full knee extension without hard lockout"),
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = listOf("Machine"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "leg_press",
                familyName = "Leg Press",
                implement = Implement.MACHINE,
                stance = Stance.SEATED
            ),
            Exercise(
                id = "leg_press_single",
                name = "Single-Leg Press (Plate Loaded)",
                description = "Unilateral leg press to correct strength imbalances and protect hip symmetry.",
                cues = listOf("Single foot on sled", "Keep knee tracking straight", "Controlled eccentric"),
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = listOf("Plate Loaded"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "leg_press",
                familyName = "Leg Press",
                implement = Implement.PLATE_LOADED,
                stance = Stance.SINGLE_LEG
            ),

            // --- Split Squats & Lunges & Step Ups ---
            Exercise(
                id = "split_squat_bulgarian_db",
                name = "Bulgarian Split Squat (Dumbbell)",
                description = "Rear-foot elevated split squat for quad hypertrophy and glute stretch.",
                cues = listOf("Rear foot on bench", "Descend until front thigh is parallel", "Drive through front heel"),
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "split_squat",
                familyName = "Bulgarian Split Squat",
                implement = Implement.DUMBBELL,
                stance = Stance.SINGLE_LEG
            ),
            Exercise(
                id = "split_squat_bulgarian_bb",
                name = "Bulgarian Split Squat (Barbell)",
                description = "Barbell loaded rear-foot elevated split squat for heavy unilateral strength.",
                cues = listOf("Bar on back", "Maintain upright torso", "Control descent smoothly"),
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = listOf("Barbell"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "split_squat",
                familyName = "Bulgarian Split Squat",
                implement = Implement.BARBELL,
                stance = Stance.SINGLE_LEG
            ),
            Exercise(
                id = "split_squat_bulgarian_bw",
                name = "Bulgarian Split Squat (Bodyweight)",
                description = "Bodyweight Bulgarian split squat for high-rep quad burn and mobility.",
                cues = listOf("Rear foot elevated", "Deep knee bend", "Torso tall"),
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = listOf("Bodyweight"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "split_squat",
                familyName = "Bulgarian Split Squat",
                implement = Implement.BODYWEIGHT,
                stance = Stance.SINGLE_LEG
            ),
            Exercise(
                id = "step_up_db",
                name = "Step-Up (Dumbbell)",
                description = "Dumbbell step-up on box or bench emphasizing glute and quad drive.",
                cues = listOf("Full foot on box", "Minimal push from back leg", "Stand tall at top"),
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "step_up",
                familyName = "Step-Up",
                implement = Implement.DUMBBELL,
                stance = Stance.SINGLE_LEG
            ),
            Exercise(
                id = "step_up_bb",
                name = "Step-Up (Barbell)",
                description = "Barbell loaded box step-up.",
                cues = listOf("Bar secure on traps", "Step up forcefully", "Control descent down"),
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = listOf("Barbell"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "step_up",
                familyName = "Step-Up",
                implement = Implement.BARBELL,
                stance = Stance.SINGLE_LEG
            ),
            Exercise(
                id = "step_up_bw",
                name = "Step-Up (Bodyweight)",
                description = "Bodyweight box step-up.",
                cues = listOf("Stable tempo", "Push through front heel", "Squeeze glute at top"),
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = listOf("Bodyweight"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "step_up",
                familyName = "Step-Up",
                implement = Implement.BODYWEIGHT,
                stance = Stance.SINGLE_LEG
            ),
            Exercise(
                id = "lunge_barbell",
                name = "Lunge (Barbell)",
                description = "Barbell loaded walking or stationary lunges.",
                cues = listOf("Keep chest tall", "Take a big step forward", "Push off front heel"),
                muscleGroups = listOf("Quads", "Glutes", "Hamstrings"),
                equipment = listOf("Barbell"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "lunge",
                familyName = "Lunge",
                implement = Implement.BARBELL,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "lunge_dumbbell",
                name = "Lunge (Dumbbell)",
                description = "Dumbbell loaded walking or stationary lunges.",
                cues = listOf("Dumbbells at sides", "Keep torso upright", "Control rear knee down"),
                muscleGroups = listOf("Quads", "Glutes", "Hamstrings"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "lunge",
                familyName = "Lunge",
                implement = Implement.DUMBBELL,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "lunge_bodyweight",
                name = "Lunge (Bodyweight)",
                description = "Unilateral bodyweight lunge.",
                cues = listOf("Hands on hips or front", "Stable core", "Step back or forward cleanly"),
                muscleGroups = listOf("Quads", "Glutes", "Hamstrings"),
                equipment = listOf("Bodyweight"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "lunge",
                familyName = "Lunge",
                implement = Implement.BODYWEIGHT,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "leg_extension_cable",
                name = "Leg Extension (Cable)",
                description = "Machine isolation targeting the quadriceps.",
                cues = listOf("Point toes slightly upward", "Hold handles for stability", "Squeeze quads at full extension"),
                muscleGroups = listOf("Quads"),
                equipment = listOf("Cable"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "leg_extension",
                familyName = "Leg Extension",
                implement = Implement.CABLE,
                stance = Stance.SEATED
            ),
            Exercise(
                id = "leg_extension_plate_loaded",
                name = "Leg Extension (Plate Loaded)",
                description = "Levered plate-loaded machine leg extension.",
                cues = listOf("Keep hips pushed back", "Squeeze quads hard at the peak", "Lower slowly and in control"),
                muscleGroups = listOf("Quads"),
                equipment = listOf("Plate Loaded"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "leg_extension",
                familyName = "Leg Extension",
                implement = Implement.PLATE_LOADED,
                stance = Stance.SEATED
            ),

            // --- Deadlift & Posterior Chain ---
            Exercise(
                id = "deadlift",
                name = "Conventional Deadlift (Barbell)",
                description = "Ultimate test of posterior chain strength.",
                cues = listOf("Slack out of bar", "Drag up shins", "Lockout hips"),
                muscleGroups = listOf("Hamstrings", "Glutes", "Back", "Forearms"),
                equipment = listOf("Barbell"),
                movementType = MovementType.DEADLIFT,
                isLockedClassic = true,
                dangerousFor = listOf("Lower Back Pain"),
                familyId = "deadlift",
                familyName = "Deadlift",
                implement = Implement.BARBELL,
                stance = Stance.STANDARD,
                isPrimaryVariant = true
            ),
            Exercise(
                id = "deadlift_sumo",
                name = "Sumo Deadlift (Barbell)",
                description = "Wide-stance deadlift emphasizing hip and adductor drive with a more upright torso.",
                cues = listOf("Wide foot stance", "Knees pushed outward", "Keep torso upright and wedge hips in"),
                muscleGroups = listOf("Glutes", "Quads", "Hamstrings", "Back"),
                equipment = listOf("Barbell"),
                movementType = MovementType.DEADLIFT,
                dangerousFor = listOf("Lower Back Pain"),
                familyId = "deadlift",
                familyName = "Deadlift",
                implement = Implement.BARBELL,
                stance = Stance.SUMO
            ),
            Exercise(
                id = "trap_bar_deadlift",
                name = "Deadlift (Trap Bar)",
                description = "High-stability deadlift that keeps the center of gravity aligned with the body.",
                cues = listOf("Step inside bar", "Hips down, chest up", "Drive through floor"),
                muscleGroups = listOf("Back", "Legs", "Traps"),
                equipment = listOf("Specialty Bar"),
                movementType = MovementType.POSTERIOR_CHAIN,
                familyId = "deadlift",
                familyName = "Deadlift",
                implement = Implement.SPECIALTY_BAR,
                stance = Stance.STANDARD,
                specialtyBar = "TRAP"
            ),
            Exercise(
                id = "romanian_deadlift",
                name = "Romanian Deadlift (Barbell)",
                description = "Hip hinge focusing on hamstrings.",
                cues = listOf("Hips back", "Feel the stretch", "Don't touch floor"),
                muscleGroups = listOf("Hamstrings", "Glutes"),
                equipment = listOf("Barbell"),
                movementType = MovementType.POSTERIOR_CHAIN,
                familyId = "deadlift",
                familyName = "Deadlift",
                implement = Implement.BARBELL,
                stance = Stance.STANDARD,
                isPrimaryVariant = true
            ),
            Exercise(
                id = "romanian_deadlift_dumbbell",
                name = "Romanian Deadlift (Dumbbell)",
                description = "Unilateral or bilateral dumbbell Romanian deadlift.",
                cues = listOf("Hinged hips back", "Keep dumbbells close to shins", "Squeeze glutes to stand"),
                muscleGroups = listOf("Hamstrings", "Glutes"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.POSTERIOR_CHAIN,
                familyId = "deadlift",
                familyName = "Deadlift",
                implement = Implement.DUMBBELL,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "rdl_single_leg_db",
                name = "Single-Leg RDL (Dumbbell)",
                description = "Unilateral hip hinge building balance, hamstring loaded stretch, and ankle stability.",
                cues = listOf("Hinge on one leg", "Extend back leg straight behind", "Keep hips square"),
                muscleGroups = listOf("Hamstrings", "Glutes"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.POSTERIOR_CHAIN,
                familyId = "deadlift",
                familyName = "Deadlift",
                implement = Implement.DUMBBELL,
                stance = Stance.SINGLE_LEG
            ),
            Exercise(
                id = "good_morning_bb",
                name = "Good Morning (Barbell)",
                description = "Barbell on back hip-hinge targeting the hamstrings, glutes, and spinal erectors.",
                cues = listOf("Bar secure on traps", "Hinge back with soft knees", "Stop when torso reaches 45 degrees"),
                muscleGroups = listOf("Hamstrings", "Glutes", "Lower Back"),
                equipment = listOf("Barbell"),
                movementType = MovementType.POSTERIOR_CHAIN,
                dangerousFor = listOf("Lower Back Pain"),
                familyId = "deadlift",
                familyName = "Deadlift",
                implement = Implement.BARBELL,
                stance = Stance.STANDING
            ),
            Exercise(
                id = "back_extension_bw",
                name = "Back Extension (Bodyweight)",
                description = "45-degree hyperextension for spinal erectors, glutes, and hamstrings.",
                cues = listOf("Hinge at hip crease", "Round slightly for glutes or keep flat for erectors", "Squeeze top 1s"),
                muscleGroups = listOf("Lower Back", "Glutes", "Hamstrings"),
                equipment = listOf("Bodyweight"),
                movementType = MovementType.POSTERIOR_CHAIN,
                familyId = "deadlift",
                familyName = "Deadlift",
                implement = Implement.BODYWEIGHT,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "hip_thrust_barbell",
                name = "Hip Thrust (Barbell)",
                description = "Barbell loaded hip thrusts for absolute glute development.",
                cues = listOf("Rest upper back on bench", "Drive hips upward", "Squeeze glutes fully at peak"),
                muscleGroups = listOf("Glutes", "Hamstrings"),
                equipment = listOf("Barbell"),
                movementType = MovementType.POSTERIOR_CHAIN,
                familyId = "deadlift",
                familyName = "Deadlift",
                implement = Implement.BARBELL,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "hip_thrust_plate_loaded",
                name = "Hip Thrust (Plate Loaded)",
                description = "Plate loaded belt or pad lever hip thrust machine.",
                cues = listOf("Secure safety belt tightly", "Drive heels down", "Hold peak contraction for 1s"),
                muscleGroups = listOf("Glutes", "Hamstrings"),
                equipment = listOf("Plate Loaded"),
                movementType = MovementType.POSTERIOR_CHAIN,
                familyId = "deadlift",
                familyName = "Deadlift",
                implement = Implement.PLATE_LOADED,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "hip_abduction_machine",
                name = "Hip Abduction (Machine)",
                description = "Seated machine hip abduction targeting glute medius and upper glutes.",
                cues = listOf("Lean slightly forward", "Push pads apart", "Hold peak contraction for 1s"),
                muscleGroups = listOf("Glutes"),
                equipment = listOf("Machine"),
                movementType = MovementType.POSTERIOR_CHAIN,
                familyId = "deadlift",
                familyName = "Deadlift",
                implement = Implement.MACHINE,
                stance = Stance.SEATED
            ),
            Exercise(
                id = "kettlebell_swings",
                name = "Kettlebell Swing (Kettlebell)",
                description = "Dynamic ballistic hip hinge.",
                cues = listOf("Hinged at hips", "Squeeze glutes at peak", "Let arms act as ropes"),
                muscleGroups = listOf("Glutes", "Hamstrings", "Lower Back", "Core"),
                equipment = listOf("Kettlebell"),
                movementType = MovementType.POSTERIOR_CHAIN,
                familyId = "deadlift",
                familyName = "Deadlift",
                implement = Implement.KETTLEBELL,
                stance = Stance.STANDING
            ),
            Exercise(
                id = "leg_curl_cable",
                name = "Leg Curl (Cable)",
                description = "Cable machine hamstring isolation.",
                cues = listOf("Keep hips on pad", "Pull heels to glutes", "Squeeze hamstrings at the bottom"),
                muscleGroups = listOf("Hamstrings"),
                equipment = listOf("Cable"),
                movementType = MovementType.HAMSTRING_ISOLATION,
                familyId = "leg_curl",
                familyName = "Leg Curl",
                implement = Implement.CABLE,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "leg_curl_plate_loaded",
                name = "Leg Curl (Plate Loaded)",
                description = "Levered plate-loaded machine leg curl.",
                cues = listOf("Brace thighs tight against support pad", "Contract hamstring explosively", "Control the return stretch"),
                muscleGroups = listOf("Hamstrings"),
                equipment = listOf("Plate Loaded"),
                movementType = MovementType.HAMSTRING_ISOLATION,
                familyId = "leg_curl",
                familyName = "Leg Curl",
                implement = Implement.PLATE_LOADED,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "nordic_curl",
                name = "Nordic Hamstring Curl (Bodyweight)",
                description = "High-tension eccentric bodyweight hamstring curl.",
                cues = listOf("Anchor ankles firmly", "Resist fall with hamstrings", "Catch smoothly and push back"),
                muscleGroups = listOf("Hamstrings"),
                equipment = listOf("Bodyweight"),
                movementType = MovementType.HAMSTRING_ISOLATION,
                familyId = "nordic",
                familyName = "Nordic Curl",
                implement = Implement.BODYWEIGHT,
                stance = Stance.STANDARD
            ),

            // --- Calves ---
            Exercise(
                id = "calf_raise",
                name = "Calf Raise (Plate Loaded)",
                description = "Standing machine calf isolation for gastrocnemius development.",
                cues = listOf("Full stretch at bottom", "Explosive up", "1s pause at top"),
                muscleGroups = listOf("Calves"),
                equipment = listOf("Plate Loaded"),
                movementType = MovementType.CALVES,
                familyId = "calves",
                familyName = "Calf",
                implement = Implement.PLATE_LOADED,
                stance = Stance.STANDING,
                isPrimaryVariant = true
            ),
            Exercise(
                id = "calf_raise_bodyweight",
                name = "Calf Raise (Bodyweight)",
                description = "Standing bodyweight calf lifts.",
                cues = listOf("Full range on floor/step", "Peak squeeze", "Control descent"),
                muscleGroups = listOf("Calves"),
                equipment = listOf("Bodyweight"),
                movementType = MovementType.CALVES,
                familyId = "calves",
                familyName = "Calf",
                implement = Implement.BODYWEIGHT,
                stance = Stance.STANDING
            ),
            Exercise(
                id = "calf_raise_seated",
                name = "Seated Calf Raise (Machine)",
                description = "Seated machine calf raise targeting the soleus muscle.",
                cues = listOf("Deep stretch at bottom", "Push through balls of feet", "Hold peak contraction 2s"),
                muscleGroups = listOf("Calves"),
                equipment = listOf("Machine"),
                movementType = MovementType.CALVES,
                familyId = "calves",
                familyName = "Calf",
                implement = Implement.MACHINE,
                stance = Stance.SEATED
            ),

            // --- Core & Carries ---
            Exercise(
                id = "cable_crunch",
                name = "Cable Crunch (Cable)",
                description = "Constant tension kneeling abdominal cable crunch.",
                cues = listOf("Crunch with abs, not hips", "Touch elbows to knees", "Squeeze core hard"),
                muscleGroups = listOf("Abs"),
                equipment = listOf("Cable"),
                movementType = MovementType.ABS,
                familyId = "cable_crunch",
                familyName = "Cable Crunch",
                implement = Implement.CABLE,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "hanging_knee_raise",
                name = "Hanging Leg Raise (Bodyweight)",
                description = "Core exercise for lower abs.",
                cues = listOf("No swinging", "Crunch with hips", "Slow descent"),
                muscleGroups = listOf("Abs"),
                equipment = listOf("Bodyweight"),
                movementType = MovementType.ABS,
                familyId = "hanging_leg_raise",
                familyName = "Hanging Leg Raise",
                implement = Implement.BODYWEIGHT,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "ab_wheel",
                name = "Ab Wheel Rollout (Bodyweight)",
                description = "Anti-extension abdominal rollout building tremendous core stiffness.",
                cues = listOf("Tuck pelvis", "Roll out slowly with straight arms", "Pull back with abs, not hips"),
                muscleGroups = listOf("Abs", "Core", "Lats"),
                equipment = listOf("Bodyweight"),
                movementType = MovementType.ABS,
                familyId = "ab_wheel",
                familyName = "Ab Wheel",
                implement = Implement.BODYWEIGHT,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "farmer_carry_db",
                name = "Farmer's Carry (Dumbbell)",
                description = "Heavy bilateral dumbbell carry building grip, traps, and total-body posture.",
                cues = listOf("Shoulders back and down", "Short fast steps", "Brace core tight"),
                muscleGroups = listOf("Forearms", "Traps", "Core"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.POSTERIOR_CHAIN,
                familyId = "farmer_carry",
                familyName = "Farmer's Carry",
                implement = Implement.DUMBBELL,
                stance = Stance.STANDING
            ),
            Exercise(
                id = "farmer_carry_kb",
                name = "Farmer's Carry (Kettlebell)",
                description = "Kettlebell carry testing grip endurance and trunk stabilization.",
                cues = listOf("Chest tall", "Walk smooth and controlled", "Do not allow torso to sway"),
                muscleGroups = listOf("Forearms", "Traps", "Core"),
                equipment = listOf("Kettlebell"),
                movementType = MovementType.POSTERIOR_CHAIN,
                familyId = "farmer_carry",
                familyName = "Farmer's Carry",
                implement = Implement.KETTLEBELL,
                stance = Stance.STANDING
            ),
            Exercise(
                id = "kettlebell_goblet_squat",
                name = "Goblet Squat (Kettlebell)",
                description = "Quad-dominant squat holding a kettlebell in the goblet position.",
                cues = listOf("Hold KB by handles", "Keep elbows tucked", "Deep squat"),
                muscleGroups = listOf("Quads", "Glutes", "Core"),
                equipment = listOf("Kettlebell"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "squat",
                familyName = "Squat",
                implement = Implement.KETTLEBELL,
                stance = Stance.GOBLET
            ),
            Exercise(
                id = "kettlebell_clean",
                name = "Kettlebell Clean",
                description = "Explosive movement pulling a kettlebell to the rack position.",
                cues = listOf("Hinge and snap", "Keep KB close to body", "Soft landing in rack"),
                muscleGroups = listOf("Hamstrings", "Glutes", "Shoulders", "Core"),
                equipment = listOf("Kettlebell"),
                movementType = MovementType.POSTERIOR_CHAIN,
                familyId = "kettlebell_clean",
                familyName = "Kettlebell Clean",
                implement = Implement.KETTLEBELL,
                stance = Stance.STANDING,
                isPrimaryVariant = true
            ),
            Exercise(
                id = "kettlebell_snatch",
                name = "Kettlebell Snatch",
                description = "High-velocity overhead kettlebell movement.",
                cues = listOf("Powerful hip drive", "Punch through at top", "Control descent"),
                muscleGroups = listOf("Shoulders", "Hamstrings", "Glutes", "Back"),
                equipment = listOf("Kettlebell"),
                movementType = MovementType.POSTERIOR_CHAIN,
                familyId = "kettlebell_snatch",
                familyName = "Kettlebell Snatch",
                implement = Implement.KETTLEBELL,
                stance = Stance.STANDING,
                isPrimaryVariant = true
            ),
            Exercise(
                id = "turkish_get_up_kb",
                name = "Turkish Get-Up (Kettlebell)",
                description = "Complex total-body stability and strength movement.",
                cues = listOf("Eyes on KB", "Punch up constantly", "Control each transition"),
                muscleGroups = listOf("Shoulders", "Core", "Hips", "Back"),
                equipment = listOf("Kettlebell"),
                movementType = MovementType.POSTERIOR_CHAIN,
                familyId = "turkish_get_up",
                familyName = "Turkish Get-Up",
                implement = Implement.KETTLEBELL,
                stance = Stance.STANDARD,
                isPrimaryVariant = true
            ),
            Exercise(
                id = "kettlebell_thruster",
                name = "Kettlebell Thruster",
                description = "Combined front squat and overhead press.",
                cues = listOf("Deep squat", "Drive up explosively", "Finish with overhead press"),
                muscleGroups = listOf("Quads", "Shoulders", "Glutes", "Triceps"),
                equipment = listOf("Kettlebell"),
                movementType = MovementType.QUAD_DOMINANT,
                familyId = "kettlebell_thruster",
                familyName = "Kettlebell Thruster",
                implement = Implement.KETTLEBELL,
                stance = Stance.STANDING,
                isPrimaryVariant = true
            ),
            Exercise(
                id = "kettlebell_halo",
                name = "Kettlebell Halo",
                description = "Circular shoulder mobility and core stability exercise.",
                cues = listOf("KB bottom-up or by horns", "Rotate around head", "Keep core stable"),
                muscleGroups = listOf("Shoulders", "Core"),
                equipment = listOf("Kettlebell"),
                movementType = MovementType.ISOLATION_UPPER,
                familyId = "kettlebell_halo",
                familyName = "Kettlebell Halo",
                implement = Implement.KETTLEBELL,
                stance = Stance.STANDING,
                isPrimaryVariant = true
            ),
            Exercise(
                id = "kettlebell_deadlift",
                name = "Deadlift (Kettlebell)",
                description = "Fundamental hip hinge using a kettlebell.",
                cues = listOf("KB between feet", "Hinge at hips", "Squeeze glutes to stand"),
                muscleGroups = listOf("Hamstrings", "Glutes", "Back"),
                equipment = listOf("Kettlebell"),
                movementType = MovementType.POSTERIOR_CHAIN,
                familyId = "deadlift",
                familyName = "Deadlift",
                implement = Implement.KETTLEBELL,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "neck_extension",
                name = "Neck Extension",
                description = "Slow, controlled neck extension to build posterior neck strength.",
                cues = listOf("Slow and controlled", "No jerking", "Stop short of pain"),
                muscleGroups = listOf("Neck"),
                equipment = listOf("Other"),
                movementType = MovementType.ISOLATION_UPPER,
                dangerousFor = listOf("Neck Pain"),
                familyId = "neck",
                familyName = "Neck",
                implement = Implement.OTHER,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "neck_flexion",
                name = "Neck Flexion (Front)",
                description = "Slow, controlled neck flexion to build anterior neck strength.",
                cues = listOf("Slow and controlled", "No jerking", "Stop short of pain"),
                muscleGroups = listOf("Neck"),
                equipment = listOf("Other"),
                movementType = MovementType.ISOLATION_UPPER,
                dangerousFor = listOf("Neck Pain"),
                familyId = "neck",
                familyName = "Neck",
                implement = Implement.OTHER,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "neck_lateral",
                name = "Neck Lateral Flexion",
                description = "Unilateral neck lateral flexion to build neck stability.",
                cues = listOf("Slow and controlled", "No jerking", "Stop short of pain"),
                muscleGroups = listOf("Neck"),
                equipment = listOf("Other"),
                movementType = MovementType.ISOLATION_UPPER,
                dangerousFor = listOf("Neck Pain"),
                familyId = "neck",
                familyName = "Neck",
                implement = Implement.OTHER,
                stance = Stance.SINGLE_ARM
            ),
            Exercise(
                id = "dead_hang",
                name = "Dead Hang",
                description = "Time-based hang to build grip strength and decompress the spine.",
                cues = listOf("Active shoulders", "Tight core", "Breathe deeply"),
                muscleGroups = listOf("Forearms", "Lats"),
                equipment = listOf("Bodyweight"),
                movementType = MovementType.BACK_WIDTH,
                familyId = "dead_hang",
                familyName = "Dead Hang",
                implement = Implement.BODYWEIGHT,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "wrist_curl",
                name = "Wrist Curl",
                description = "Dumbbell wrist curls for forearm flexor isolation.",
                cues = listOf("Full range of motion", "Squeeze at peak", "Controlled eccentric"),
                muscleGroups = listOf("Forearms"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.ISOLATION_UPPER,
                familyId = "wrist_curl",
                familyName = "Wrist Curl",
                implement = Implement.DUMBBELL,
                stance = Stance.SEATED
            ),
            Exercise(
                id = "reverse_wrist_curl",
                name = "Reverse Wrist Curl",
                description = "Dumbbell reverse wrist curls for forearm extensor isolation.",
                cues = listOf("Full range of motion", "Squeeze at peak", "Controlled eccentric"),
                muscleGroups = listOf("Forearms"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.ISOLATION_UPPER,
                familyId = "wrist_curl",
                familyName = "Wrist Curl",
                implement = Implement.DUMBBELL,
                stance = Stance.SEATED
            ),
            Exercise(
                id = "cuff_external_rotation",
                name = "Side-Lying External Rotation",
                description = "Dumbbell external rotation for rotator cuff health.",
                cues = listOf("Keep elbow tucked", "Small range of motion", "Focus on control"),
                muscleGroups = listOf("Shoulders", "Rotator Cuff"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.ISOLATION_UPPER,
                dangerousFor = listOf("Shoulder Pain"),
                familyId = "cuff",
                familyName = "Rotator Cuff",
                implement = Implement.DUMBBELL,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "prone_y",
                name = "Prone Y",
                description = "Dumbbell Y-raises for lower trap and shoulder stability.",
                cues = listOf("Thumbs up", "Keep arms straight", "Squeeze shoulder blades"),
                muscleGroups = listOf("Shoulders", "Upper Back"),
                equipment = listOf("Dumbbell"),
                movementType = MovementType.ISOLATION_UPPER,
                familyId = "cuff",
                familyName = "Rotator Cuff",
                implement = Implement.DUMBBELL,
                stance = Stance.STANDARD
            ),
            Exercise(
                id = "pallof_press",
                name = "Pallof Press",
                description = "Anti-rotation core stability exercise using a cable stack.",
                cues = listOf("Don't let cable pull you", "Exhale on press", "Tight core"),
                muscleGroups = listOf("Abs", "Core"),
                equipment = listOf("Cable"),
                movementType = MovementType.ABS,
                familyId = "pallof",
                familyName = "Pallof Press",
                implement = Implement.CABLE,
                stance = Stance.STANDING
            ),
            Exercise(
                id = "side_plank",
                name = "Side Plank",
                description = "Static side core stability exercise.",
                cues = listOf("Hips high", "Elbow under shoulder", "Straight line from head to feet"),
                muscleGroups = listOf("Abs", "Core"),
                equipment = listOf("Bodyweight"),
                movementType = MovementType.ABS,
                familyId = "side_plank",
                familyName = "Side Plank",
                implement = Implement.BODYWEIGHT,
                stance = Stance.STANDARD
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

        // Bull Neck
        val bullNeck = WorkoutAugment(
            id = "augment_bull_neck",
            name = "Bull Neck",
            description = "30-day neck block. 3–4×/week. 2–3 RIR. Never fail.",
            focusBodyPart = "Neck",
            exercises = listOfNotNull(
                exercises.find { it.id == "neck_extension" },
                exercises.find { it.id == "neck_flexion" },
                exercises.find { it.id == "neck_lateral" }
            ).map { 
                val sets = if (it.id == "neck_lateral") 1 else 2
                RoutineExercise(
                    exercise = it,
                    sets = List(sets) { 
                        RoutineSet(type = SetType.NORMAL, reps = 10) 
                    }
                ) 
            },
            colorHex = "#00FF00",
            isSystem = true,
            isAddedToLibrary = false,
            scheduledDays = listOf(1, 3, 5).map { ScheduledDay(it, "18:00") }
        )
        seedAugment(bullNeck)

        // Cyber Calves
        val cyberCalves = WorkoutAugment(
            id = "augment_cyber_calves",
            name = "Cyber Calves",
            description = "Track A shock: start 200 bilateral quality reps, add ~100/session to 1000 bilateral, then single-leg cap 200/leg. Track B is the seeded loaded sets.",
            focusBodyPart = "Calves",
            exercises = listOfNotNull(
                exercises.find { it.id == "calf_raise" },
                exercises.find { it.id == "calf_raise_seated" }
            ).map { 
                val sets = if (it.id == "calf_raise") 3 else 2
                val reps = if (it.id == "calf_raise") 20 else 15
                val goal = if (it.id == "calf_raise") "15-30" else null
                RoutineExercise(
                    exercise = it,
                    sets = List(sets) { 
                        RoutineSet(type = SetType.NORMAL, reps = reps, goalReps = goal) 
                    }
                ) 
            },
            colorHex = "#00FF9C",
            isSystem = true,
            isAddedToLibrary = false,
            scheduledDays = listOf(1, 3, 5).map { ScheduledDay(it, "09:00") }
        )
        seedAugment(cyberCalves)

        // Bionic Shoulders
        val bionicShoulders = WorkoutAugment(
            id = "augment_bionic_shoulders",
            name = "Bionic Shoulders",
            description = "Bolt onto Push or run solo. Cuff Care is separate. Focus on side and rear delts.",
            focusBodyPart = "Shoulders",
            exercises = listOfNotNull(
                exercises.find { it.id == "lateral_raise" },
                exercises.find { it.id == "rear_delt_fly_dumbbell" },
                exercises.find { it.id == "facepull_cable" }
            ).map { exercise ->
                RoutineExercise(
                    exercise = exercise,
                    sets = List(3) { 
                        RoutineSet(type = SetType.NORMAL, reps = 15, goalReps = if (exercise.id == "lateral_raise") "12-20" else null) 
                    }
                ) 
            },
            colorHex = "#00CCFF",
            isSystem = true,
            isAddedToLibrary = false,
            scheduledDays = listOf(1, 3, 5).map { ScheduledDay(it, "09:00") }
        )
        seedAugment(bionicShoulders)

        // Chest Blaster
        val chestBlaster = WorkoutAugment(
            id = "augment_chest_blaster",
            name = "Chest Blaster",
            description = "Isolation pump. If attached to Push, still no extra barbell bench.",
            focusBodyPart = "Chest",
            exercises = listOfNotNull(
                exercises.find { it.id == "pec_deck" },
                exercises.find { it.id == "atlas_pushup_bodyweight" } ?: exercises.find { it.id == "dip_bodyweight" },
                exercises.find { it.id == "pullover_db" }
            ).map { 
                val sets = if (it.id == "pullover_db") 2 else 3
                val reps = if (it.id == "pullover_db") 12 else if (it.id == "pec_deck") 15 else 10
                RoutineExercise(
                    exercise = it,
                    sets = List(sets) { 
                        RoutineSet(type = SetType.NORMAL, reps = reps) 
                    }
                ) 
            },
            colorHex = "#00FF9C",
            isSystem = true,
            isAddedToLibrary = false,
            scheduledDays = listOf(2, 6).map { ScheduledDay(it, "09:00") }
        )
        seedAugment(chestBlaster)

        // Ass Blaster
        val assBlaster = WorkoutAugment(
            id = "augment_ass_blaster",
            name = "Ass Blaster",
            description = "SOLO default 2×/week. Do not default-attach to CC Legs. Absolute glute development.",
            focusBodyPart = "Glutes",
            exercises = listOfNotNull(
                exercises.find { it.id == "hip_thrust_barbell" },
                exercises.find { it.id == "split_squat_bulgarian_db" },
                exercises.find { it.id == "romanian_deadlift" }
            ).map { exercise ->
                RoutineExercise(
                    exercise = exercise,
                    sets = List(3) { 
                        RoutineSet(type = SetType.NORMAL, reps = 10, goalReps = if (exercise.id == "romanian_deadlift") "8-12" else null) 
                    }
                ) 
            },
            colorHex = "#FF006E",
            isSystem = true,
            isAddedToLibrary = false,
            scheduledDays = listOf(2, 6).map { ScheduledDay(it, "09:00") }
        )
        seedAugment(assBlaster)

        // Vice Grip
        val viceGrip = WorkoutAugment(
            id = "augment_vice_grip",
            name = "Vice Grip",
            description = "Ongoing. Skip the day before a heavy DL. Focus on grip and forearm strength.",
            focusBodyPart = "Grip",
            exercises = listOfNotNull(
                exercises.find { it.id == "farmer_carry_db" },
                exercises.find { it.id == "dead_hang" },
                exercises.find { it.id == "wrist_curl" },
                exercises.find { it.id == "reverse_wrist_curl" }
            ).map { 
                val sets = if (it.id == "dead_hang") 2 else 3
                val reps = if (it.id == "farmer_carry_db") 30 else if (it.id == "dead_hang") 30 else 15
                val goal = if (it.id == "farmer_carry_db") "30-40m" else if (it.id == "dead_hang") "30s" else null
                RoutineExercise(
                    exercise = it,
                    sets = List(sets) { 
                        RoutineSet(type = SetType.NORMAL, reps = reps, goalReps = goal) 
                    }
                ) 
            },
            colorHex = "#00FF9C",
            isSystem = true,
            isAddedToLibrary = false,
            scheduledDays = listOf(1, 3, 5).map { ScheduledDay(it, "18:00") }
        )
        seedAugment(viceGrip)

        // Iron Core
        val ironCore = WorkoutAugment(
            id = "augment_iron_core",
            name = "Iron Core",
            description = "Ongoing 10-minute core. Not sit-up cardio.",
            focusBodyPart = "Abs",
            exercises = listOfNotNull(
                exercises.find { it.id == "ab_wheel" } ?: exercises.find { it.id == "cable_crunch" },
                exercises.find { it.id == "hanging_knee_raise" },
                exercises.find { it.id == "pallof_press" },
                exercises.find { it.id == "side_plank" }
            ).map { 
                val sets = if (it.id == "pallof_press" || it.id == "side_plank") 2 else 3
                val reps = if (it.id == "side_plank") 30 else if (it.id == "hanging_knee_raise") 10 else 12
                val goal = if (it.id == "side_plank") "30s" else null
                RoutineExercise(
                    exercise = it,
                    sets = List(sets) { 
                        RoutineSet(type = SetType.NORMAL, reps = reps, goalReps = goal) 
                    }
                ) 
            },
            colorHex = "#00FF9C",
            isSystem = true,
            isAddedToLibrary = false,
            scheduledDays = listOf(1, 3, 5).map { ScheduledDay(it, "18:00") }
        )
        seedAugment(ironCore)

        // Cuff Care
        val cuffCare = WorkoutAugment(
            id = "augment_cuff_care",
            name = "Cuff Care",
            description = "Prehab. Light. Pain-free. Ongoing.",
            focusBodyPart = "Shoulders",
            exercises = listOfNotNull(
                exercises.find { it.id == "cuff_external_rotation" },
                exercises.find { it.id == "facepull_cable" },
                exercises.find { it.id == "prone_y" }
            ).map { 
                val reps = if (it.id == "prone_y") 12 else 15
                RoutineExercise(
                    exercise = it,
                    sets = List(2) { 
                        RoutineSet(type = SetType.NORMAL, reps = reps) 
                    }
                ) 
            },
            colorHex = "#00CCFF",
            isSystem = true,
            isAddedToLibrary = false,
            scheduledDays = listOf(1, 2, 4, 5).map { ScheduledDay(it, "18:00") }
        )
        seedAugment(cuffCare)

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

        // Seed Starting Strength Routine
        val startingStrength = WorkoutRoutine(
            id = "routine_starting_strength",
            name = "Starting Strength (Novice)",
            description = "PROTOCOL: STARTING STRENGTH\nGOAL: Maximum Linear Novice Strength\n\nMETHOD: \n- 3 Sets of 5 Reps (3x5) on major compounds.\n- Linear progression every single session.\n- Alternate Day A and Day B.\n\nFOCUS: Whole Body Compound Power.",
            protocol = WorkoutProtocol.STARTING_STRENGTH,
            isSystem = true,
            isAddedToLibrary = false,
            exercises = listOf(
                RoutineExercise(
                    exercise = exercises.find { it.id == "back_squat" }!!,
                    sets = listOf(RoutineSet(type = SetType.NORMAL, reps = 5))
                ),
                RoutineExercise(
                    exercise = exercises.find { it.id == "bench_press" }!!,
                    sets = listOf(RoutineSet(type = SetType.NORMAL, reps = 5))
                ),
                RoutineExercise(
                    exercise = exercises.find { it.id == "deadlift" }!!,
                    sets = listOf(RoutineSet(type = SetType.NORMAL, reps = 5))
                )
            )
        )
        seedRoutine(startingStrength)

        // Seed HST Routine
        val hstRoutine = WorkoutRoutine(
            id = "routine_hst",
            name = "Hypertrophy Specific Training (HST)",
            description = "PROTOCOL: HST\nGOAL: Maximum Hypertrophy via Load Waving\n\nMETHOD: \n- 2 Weeks of 15 reps.\n- 2 Weeks of 10 reps.\n- 2 Weeks of 5 reps.\n- 9-14 days Strategic Deconditioning (SD).\n\nFOCUS: Full Body Hypertrophy.",
            protocol = WorkoutProtocol.HST,
            isSystem = true,
            isAddedToLibrary = false,
            exercises = listOf(
                RoutineExercise(exercise = exercises.find { it.id == "back_squat" }!!, sets = listOf(RoutineSet(type = SetType.NORMAL))),
                RoutineExercise(exercise = exercises.find { it.id == "bench_press" }!!, sets = listOf(RoutineSet(type = SetType.NORMAL))),
                RoutineExercise(exercise = exercises.find { it.id == "bent_over_row" }!!, sets = listOf(RoutineSet(type = SetType.NORMAL))),
                RoutineExercise(exercise = exercises.find { it.id == "military_press" }!!, sets = listOf(RoutineSet(type = SetType.NORMAL))),
                RoutineExercise(exercise = exercises.find { it.id == "romanian_deadlift" }!!, sets = listOf(RoutineSet(type = SetType.NORMAL)))
            )
        )
        seedRoutine(hstRoutine)

        // Seed 5/3/1 Routine
        val fiveThreeOneRoutine = WorkoutRoutine(
            id = "routine_531",
            name = "5/3/1 (Boring But Big)",
            description = "PROTOCOL: 5/3/1\nGOAL: Sustainable Long-term Strength\n\nMETHOD: \n- Cycle through 3-rep, 5-rep, and 1-rep waves.\n- Last set AMRAP (As Many Reps As Possible).\n- Includes 5x10 BBB assistance work.\n\nFOCUS: Compound Strength + Volume.",
            protocol = WorkoutProtocol.FIVE_THREE_ONE,
            isSystem = true,
            isAddedToLibrary = false,
            exercises = listOf(
                RoutineExercise(exercise = exercises.find { it.id == "military_press" }!!, sets = listOf(RoutineSet(type = SetType.NORMAL))),
                RoutineExercise(exercise = exercises.find { it.id == "deadlift" }!!, sets = listOf(RoutineSet(type = SetType.NORMAL))),
                RoutineExercise(exercise = exercises.find { it.id == "bench_press" }!!, sets = listOf(RoutineSet(type = SetType.NORMAL))),
                RoutineExercise(exercise = exercises.find { it.id == "back_squat" }!!, sets = listOf(RoutineSet(type = SetType.NORMAL)))
            )
        )
        seedRoutine(fiveThreeOneRoutine)

        // Seed Westside Routine
        val westsideRoutine = WorkoutRoutine(
            id = "routine_westside",
            name = "Westside Conjugate",
            description = "PROTOCOL: WESTSIDE\nGOAL: Absolute Power + Explosive Speed\n\nMETHOD: \n- Max Effort (ME): Work up to a 1-3RM on a variant.\n- Dynamic Effort (DE): Explosive sub-maximal speed work.\n- Repetition Effort (RE): High-volume accessory work.\n\nFOCUS: Powerlifting Peak Performance.",
            protocol = WorkoutProtocol.WESTSIDE,
            isSystem = true,
            isAddedToLibrary = false,
            exercises = listOf(
                RoutineExercise(exercise = exercises.find { it.id == "back_squat" }!!, sets = listOf(RoutineSet(type = SetType.NORMAL)))
            )
        )
        seedRoutine(westsideRoutine)

        // Ensure old default routines are removed as requested
        workoutDao.deleteRoutine("routine_strength")
        workoutDao.deleteRoutine("routine_strength_2")
        workoutDao.deleteRoutine("routine_lower_split")

        // Debug Seed: Mock Sessions if history is empty
        if (workoutDao.countSessionsBetween(java.time.Instant.EPOCH, java.time.Instant.now()).first() == 0) {
            val mockSessionId = java.util.UUID.randomUUID().toString()
            val mockDate = java.time.Instant.now().minus(2, java.time.temporal.ChronoUnit.DAYS)
            
            workoutDao.upsertSession(
                WorkoutSessionEntity(
                    id = mockSessionId,
                    date = mockDate,
                    protocol = WorkoutProtocol.CYBER_CRAPP.name,
                    durationSeconds = 2400,
                    notes = "Initial neural calibration session.",
                    experienceLevel = ExperienceLevel.INTERMEDIATE.name,
                    somatotype = Somatotype.MESOMORPH.name
                )
            )

            val bench = exercises.find { it.id == "bench_press" }
            if (bench != null) {
                val logId = java.util.UUID.randomUUID().toString()
                workoutDao.upsertWorkoutLog(
                    WorkoutLogEntity(
                        id = logId,
                        sessionId = mockSessionId,
                        exerciseId = bench.id,
                        order = 0,
                        exerciseName = bench.name,
                        protocolOverride = null
                    )
                )
                
                // Add a completed cluster
                listOf(1, 2, 3).forEach { mIndex ->
                    workoutDao.upsertSetLog(
                        SetLogEntity(
                            id = java.util.UUID.randomUUID().toString(),
                            workoutLogId = logId,
                            weight = 185f,
                            reps = 8 - mIndex,
                            setType = SetType.REST_PAUSE.name,
                            isCompleted = true,
                            rir = 1,
                            isWarmup = false,
                            timestamp = mockDate.plusSeconds((mIndex * 60).toLong()),
                            clusterMiniSetIndex = mIndex,
                            isLengthenedPartial = false,
                            isLoadedStretch = false,
                            stretchDurationSeconds = null
                        )
                    )
                }
            }
        }
    }

    private suspend fun seedProtocolRepTargets() {
        val targets = listOf(
            // CYBER_CRAPP Wildcards
            ProtocolRepTarget(protocol = WorkoutProtocol.CYBER_CRAPP, movementType = MovementType.UNDEFINED, setType = SetType.WARMUP, minReps = 5, maxReps = 10),
            ProtocolRepTarget(protocol = WorkoutProtocol.CYBER_CRAPP, movementType = MovementType.UNDEFINED, setType = SetType.PARTIAL, minReps = 3, maxReps = 5),
            ProtocolRepTarget(protocol = WorkoutProtocol.CYBER_CRAPP, movementType = MovementType.UNDEFINED, setType = SetType.STRETCH, minReps = 30, maxReps = 45, unit = "SECONDS"),

            // CYBER_CRAPP Movement Specific (REST_PAUSE)
            ProtocolRepTarget(protocol = WorkoutProtocol.CYBER_CRAPP, movementType = MovementType.COMPOUND_UPPER, setType = SetType.REST_PAUSE, minReps = 11, maxReps = 20),
            ProtocolRepTarget(protocol = WorkoutProtocol.CYBER_CRAPP, movementType = MovementType.ISOLATION_UPPER, setType = SetType.REST_PAUSE, minReps = 11, maxReps = 20),
            ProtocolRepTarget(protocol = WorkoutProtocol.CYBER_CRAPP, movementType = MovementType.BACK_WIDTH, setType = SetType.REST_PAUSE, minReps = 11, maxReps = 20),
            ProtocolRepTarget(protocol = WorkoutProtocol.CYBER_CRAPP, movementType = MovementType.BACK_THICKNESS, setType = SetType.REST_PAUSE, minReps = 10, maxReps = 15),
            ProtocolRepTarget(protocol = WorkoutProtocol.CYBER_CRAPP, movementType = MovementType.POSTERIOR_CHAIN, setType = SetType.REST_PAUSE, minReps = 10, maxReps = 15),
            ProtocolRepTarget(protocol = WorkoutProtocol.CYBER_CRAPP, movementType = MovementType.QUAD_DOMINANT, setType = SetType.REST_PAUSE, minReps = 11, maxReps = 20),
            ProtocolRepTarget(protocol = WorkoutProtocol.CYBER_CRAPP, movementType = MovementType.HAMSTRING_ISOLATION, setType = SetType.REST_PAUSE, minReps = 15, maxReps = 25),
            ProtocolRepTarget(protocol = WorkoutProtocol.CYBER_CRAPP, movementType = MovementType.CALVES, setType = SetType.REST_PAUSE, minReps = 10, maxReps = 15),

            // CYBER_CRAPP Movement Specific (NORMAL / DEADLIFT)
            ProtocolRepTarget(protocol = WorkoutProtocol.CYBER_CRAPP, movementType = MovementType.DEADLIFT, setType = SetType.NORMAL, minReps = 6, maxReps = 9),
            ProtocolRepTarget(protocol = WorkoutProtocol.CYBER_CRAPP, movementType = MovementType.ABS, setType = SetType.NORMAL, minReps = 15, maxReps = 30),

            // CYBER_CRAPP Family Specific (SQUAT)
            ProtocolRepTarget(protocol = WorkoutProtocol.CYBER_CRAPP, movementType = MovementType.QUAD_DOMINANT, setType = SetType.NORMAL, familyId = "squat", minReps = 6, maxReps = 10),
            ProtocolRepTarget(protocol = WorkoutProtocol.CYBER_CRAPP, movementType = MovementType.QUAD_DOMINANT, setType = SetType.WIDOWMAKER, familyId = "squat", minReps = 20, maxReps = 20),

            // GENERAL / STRAIGHT_SETS
            ProtocolRepTarget(protocol = WorkoutProtocol.GENERAL, movementType = MovementType.UNDEFINED, setType = SetType.NORMAL, minReps = 8, maxReps = 12),
            ProtocolRepTarget(protocol = WorkoutProtocol.GENERAL, movementType = MovementType.UNDEFINED, setType = SetType.WARMUP, minReps = 5, maxReps = 10),
            ProtocolRepTarget(protocol = WorkoutProtocol.STRAIGHT_SETS, movementType = MovementType.UNDEFINED, setType = SetType.NORMAL, minReps = 8, maxReps = 12),
            ProtocolRepTarget(protocol = WorkoutProtocol.STRAIGHT_SETS, movementType = MovementType.UNDEFINED, setType = SetType.WARMUP, minReps = 5, maxReps = 10),

            // STARTING_STRENGTH
            ProtocolRepTarget(protocol = WorkoutProtocol.STARTING_STRENGTH, movementType = MovementType.UNDEFINED, setType = SetType.NORMAL, minReps = 5, maxReps = 5),
            ProtocolRepTarget(protocol = WorkoutProtocol.STARTING_STRENGTH, movementType = MovementType.UNDEFINED, setType = SetType.WARMUP, minReps = 5, maxReps = 5),

            // HST
            ProtocolRepTarget(protocol = WorkoutProtocol.HST, movementType = MovementType.UNDEFINED, setType = SetType.NORMAL, minReps = 5, maxReps = 15),

            // FIVE_THREE_ONE
            ProtocolRepTarget(protocol = WorkoutProtocol.FIVE_THREE_ONE, movementType = MovementType.UNDEFINED, setType = SetType.NORMAL, minReps = 1, maxReps = 5),

            // WESTSIDE
            ProtocolRepTarget(protocol = WorkoutProtocol.WESTSIDE, movementType = MovementType.UNDEFINED, setType = SetType.POWER, minReps = 1, maxReps = 3),
            ProtocolRepTarget(protocol = WorkoutProtocol.WESTSIDE, movementType = MovementType.UNDEFINED, setType = SetType.NORMAL, minReps = 1, maxReps = 3),

            // DUP
            ProtocolRepTarget(protocol = WorkoutProtocol.DUP, movementType = MovementType.UNDEFINED, setType = SetType.NORMAL, familyId = "DUP_HYPERTROPHY", minReps = 8, maxReps = 12),
            ProtocolRepTarget(protocol = WorkoutProtocol.DUP, movementType = MovementType.UNDEFINED, setType = SetType.NORMAL, familyId = "DUP_STRENGTH", minReps = 3, maxReps = 5),
            ProtocolRepTarget(protocol = WorkoutProtocol.DUP, movementType = MovementType.UNDEFINED, setType = SetType.NORMAL, familyId = "DUP_POWER", minReps = 2, maxReps = 3)
        )

        targets.forEach { target ->
            workoutDao.upsertProtocolRepTarget(target.toEntity())
        }
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

    override fun getAccomplishments(exerciseId: String): Flow<ExerciseAccomplishments?> =
        workoutDao.getAccomplishments(exerciseId).map { it?.toDomain() }

    override fun getAllAccomplishments(): Flow<List<ExerciseAccomplishments>> =
        workoutDao.getAllAccomplishments().map { list -> list.map { it.toDomain() } }

    override suspend fun saveAccomplishments(accomplishments: ExerciseAccomplishments) {
        workoutDao.insertAccomplishments(accomplishments.toEntity())
    }

    override fun getFullHistory(): Flow<List<Pair<WorkoutSession, List<Pair<WorkoutLog, List<SetLog>>>>>> =
        workoutDao.getAllSessionsWithDetails().map { sessions ->
            sessions.map { sessionWithLogs ->
                sessionWithLogs.session.toDomain() to sessionWithLogs.logs.map { logWithSets ->
                    logWithSets.log.toDomain() to logWithSets.sets.map { it.toDomain() }
                }
            }
        }

    override fun countSessionsBetween(from: java.time.Instant, to: java.time.Instant): Flow<Int> =
        workoutDao.countSessionsBetween(from, to)

    override fun getSessionsBetween(
        from: java.time.Instant,
        to: java.time.Instant
    ): Flow<List<Pair<WorkoutSession, List<Pair<WorkoutLog, List<SetLog>>>>>> =
        workoutDao.getSessionsWithDetailsBetween(from, to).map { sessions ->
            sessions.map { sessionWithLogs ->
                sessionWithLogs.session.toDomain() to sessionWithLogs.logs.map { logWithSets ->
                    logWithSets.log.toDomain() to logWithSets.sets.map { it.toDomain() }
                }
            }
        }

    override fun getSessionDatesAndDeloadBetween(
        from: java.time.Instant,
        to: java.time.Instant
    ): Flow<List<Pair<java.time.Instant, Boolean>>> =
        workoutDao.getSessionDatesAndDeloadBetween(from, to).map { list ->
            list.map { it.date to it.isDeload }
        }

    override fun getMuscleGroupsHitBetween(
        from: java.time.Instant,
        to: java.time.Instant
    ): Flow<List<String>> =
        workoutDao.getMuscleGroupsHitBetween(from, to).map { list ->
            list.flatMap { it.muscleGroups }.distinct()
        }

    override fun getLogsForExerciseBetween(
        exerciseId: String,
        from: java.time.Instant,
        to: java.time.Instant
    ): Flow<List<Pair<WorkoutLog, List<SetLog>>>> =
        workoutDao.getLogsForExerciseBetween(exerciseId, from, to).map { list ->
            list.map { logWithSets ->
                logWithSets.log.toDomain() to logWithSets.sets
                    .map { it.toDomain() }
                    .filter { it.isCompleted }
                    .sortedWith(compareBy({ it.timestamp }, { it.id }))
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
