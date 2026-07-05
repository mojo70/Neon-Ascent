package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.dao.WorkoutDao
import com.neon.ascent.core.data.local.entity.RoutineExerciseCrossRef
import com.neon.ascent.core.data.mapper.*
import com.neon.ascent.core.domain.repository.WorkoutRepository
import com.neon.ascent.core.domain.workout.models.*
import kotlinx.coroutines.flow.Flow
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
                    .sortedBy { it.timestamp }
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
            list.map { it.routine.toDomain(it.exercises) }
        }

    override suspend fun saveRoutine(routine: WorkoutRoutine) {
        workoutDao.insertRoutine(routine.toEntity())
        routine.exercises.forEachIndexed { index, exercise ->
            workoutDao.insertRoutineExerciseCrossRef(
                RoutineExerciseCrossRef(routine.id, exercise.id, index)
            )
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
            )
        )
        
        exercises.forEach {
            workoutDao.insertExerciseDefinition(it.toEntity())
        }

        // Seed Starter Routines
        val strengthRoutine = WorkoutRoutine(
            id = "routine_strength",
            name = "Strength",
            exercises = exercises.filter { it.id in listOf("bench_press", "bent_over_row", "back_squat", "deadlift") },
            protocol = WorkoutProtocol.GENERAL
        )
        
        val strength2Routine = WorkoutRoutine(
            id = "routine_strength_2",
            name = "Strength 2",
            exercises = exercises.filter { it.id in listOf("zercher_squat", "military_press", "weighted_pullups") },
            protocol = WorkoutProtocol.GENERAL
        )
        
        val lowerSplitRoutine = WorkoutRoutine(
            id = "routine_lower_split",
            name = "Lower split",
            exercises = exercises.filter { it.id in listOf("back_squat", "romanian_deadlift", "bulgarian_split_squat", "calf_raise", "hanging_knee_raise") },
            protocol = WorkoutProtocol.GENERAL
        )

        saveRoutine(strengthRoutine)
        saveRoutine(strength2Routine)
        saveRoutine(lowerSplitRoutine)
    }

    override suspend fun deleteSession(sessionId: String) {
        workoutDao.deleteSession(sessionId)
    }

    override fun getActiveSession(): Flow<WorkoutSession?> =
        workoutDao.getActiveSession().map { it?.toDomain() }

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

    override suspend fun deleteSetLog(setLogId: String) {
        workoutDao.deleteSetLog(setLogId)
    }
}
