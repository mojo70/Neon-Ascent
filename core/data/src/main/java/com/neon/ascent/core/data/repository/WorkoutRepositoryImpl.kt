package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.dao.WorkoutDao
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
            list.map { it.log.toDomain() to it.sets.map { set -> set.toDomain() } }
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
            )
        )
        
        exercises.forEach {
            workoutDao.insertExerciseDefinition(it.toEntity())
        }
    }
}
