package com.neon.ascent.core.domain.workout.models

import java.time.Instant

data class WorkoutSession(
    val id: String,
    val date: Instant = Instant.now(),
    val protocol: WorkoutProtocol = WorkoutProtocol.GENERAL,
    val durationSeconds: Long = 0,
    val notes: String? = null,
    val experienceLevel: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
    val somatotype: Somatotype = Somatotype.MESOMORPH
)

enum class WorkoutProtocol {
    GENERAL, CYBER_CRAPP, STRAIGHT_SETS, DUP, SUPERSETS
}

enum class ExperienceLevel {
    NOVICE, INTERMEDIATE, ADVANCED
}

enum class Somatotype {
    ECTOMORPH, MESOMORPH, ENDOMORPH
}

enum class SetType {
    NORMAL, WARMUP, DROP, FAILURE, REST_PAUSE
}

data class Exercise(
    val id: String,
    val name: String,
    val description: String,
    val cues: List<String>,
    val muscleGroups: List<String>,
    val equipment: List<String>,
    val gifAssetPath: String? = null,
    val isLockedClassic: Boolean = false
)

data class WorkoutLog(
    val id: String,
    val sessionId: String,
    val exerciseId: String,
    val order: Int,
    val exerciseName: String, // Denormalized for convenience
    val protocolOverride: WorkoutProtocol? = null,
    val supersetId: String? = null,
    val augmentId: String? = null,
    val augmentName: String? = null,
    val augmentColor: String? = null
)

data class WorkoutAugment(
    val id: String,
    val name: String,
    val description: String?,
    val focusBodyPart: String,
    val exercises: List<RoutineExercise> = emptyList(),
    val colorHex: String = "#007AFF",
    val isSystem: Boolean = false,
    val isAddedToLibrary: Boolean = true
)

data class SetLog(
    val id: String,
    val workoutLogId: String,
    val weight: Float,
    val reps: Int,
    val type: SetType = SetType.NORMAL,
    val isCompleted: Boolean = false,
    val rir: Int? = null,
    val isWarmup: Boolean = false, // Deprecated in favor of type
    val timestamp: Instant = Instant.now(),
    
    // CyberCrapp specific fields
    val clusterMiniSetIndex: Int? = null, // 1, 2, or 3 for rest-pause
    val isLengthenedPartial: Boolean = false,
    val isLoadedStretch: Boolean = false,
    val stretchDurationSeconds: Int? = null
)

data class UserWorkoutProfile(
    val userId: String,
    val experienceLevel: ExperienceLevel,
    val somatotype: Somatotype,
    val injuries: List<String> = emptyList(),
    val preferredDays: List<Int> = emptyList(),
    val timePerSessionMinutes: Int = 60
)

data class WorkoutRoutine(
    val id: String,
    val name: String,
    val description: String? = null,
    val exercises: List<RoutineExercise> = emptyList(),
    val augments: List<WorkoutAugment> = emptyList(),
    val protocol: WorkoutProtocol = WorkoutProtocol.GENERAL,
    val createdAt: Instant = Instant.now()
)

data class RoutineExercise(
    val exercise: Exercise,
    val sets: List<RoutineSet> = emptyList()
)

data class RoutineSet(
    val type: SetType = SetType.NORMAL,
    val weight: Float = 0f,
    val reps: Int = 0
)
