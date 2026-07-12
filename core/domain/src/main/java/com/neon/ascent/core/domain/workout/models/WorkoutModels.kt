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
    GENERAL, CYBER_CRAPP, STRAIGHT_SETS, DUP, SUPERSETS;

    val displayName: String
        get() = when (this) {
            GENERAL -> "GENERAL"
            CYBER_CRAPP -> "CYBERCRAPP"
            STRAIGHT_SETS -> "STRAIGHT SETS"
            DUP -> "D.U.P."
            SUPERSETS -> "SUPERSETS"
        }

    val description: String
        get() = when (this) {
            GENERAL -> "Versatile foundational programming for overall athletic performance and hypertrophy."
            CYBER_CRAPP -> "High-intensity rest-pause protocol designed for maximum effective reps and time efficiency."
            STRAIGHT_SETS -> "Classical volume-based training focusing on steady progression and form mastery."
            DUP -> "Daily Undulating Periodization: varying intensity and volume daily to optimize strength and muscle gain."
            SUPERSETS -> "Paired exercises to maximize training density and metabolic stress."
        }

    val tenants: List<String>
        get() = when (this) {
            GENERAL -> listOf(
                "Progressive overload across multiple rep ranges",
                "Balanced focus on strength and hypertrophy",
                "Adaptive recovery based on biometrics"
            )
            CYBER_CRAPP -> listOf(
                "1 Main Set to failure + 2 rest-pause 'mini-sets'",
                "Mandatory 10-second loaded stretch for hypertrophy",
                "Maximum 'Effective Rep' density in under 45 mins"
            )
            STRAIGHT_SETS -> listOf(
                "Standardized set/rep schemes (e.g., 3x10)",
                "Focus on 'perfect' neural execution",
                "Linear weight progression session-to-session"
            )
            DUP -> listOf(
                "Alternating 'Hypertrophy', 'Power', and 'Strength' days",
                "Prevents neural adaptation through variety",
                "Optimized for advanced operatives"
            )
            SUPERSETS -> listOf(
                "Antagonist muscle pairing for efficiency",
                "Elevated heart rate for cardiovascular benefit",
                "Minimal rest between paired movements"
            )
        }

    val methodology: String
        get() = when (this) {
            CYBER_CRAPP -> "REST-PAUSE CLUSTERS: After warmups, perform one set to failure. Rest 15 seconds. Perform a second mini-set to failure. Rest 15 seconds. Perform a third mini-set. Follow with a 10s finisher and a loaded stretch."
            STRAIGHT_SETS -> "LINEAR PROGRESSION: Perform all sets for the prescribed reps. If successful, increase weight next session. Rest 2-3 minutes between sets."
            DUP -> "DAILY UNDULATION: Volume and intensity fluctuate every session. Focus on explosive power on Power days, and mind-muscle connection on Hypertrophy days."
            else -> "STANDARD OVERLOAD: Focus on consistent form and incremental increases in resistance or volume."
        }
}

enum class CyberCrappPhase {
    NOT_ACTIVE, MINI_SET_1, MINI_SET_2, MINI_SET_3, CYBER_FINISHER, LOADED_STRETCH
}

enum class ExperienceLevel {
    NOVICE, INTERMEDIATE, ADVANCED
}

enum class Somatotype {
    ECTOMORPH, MESOMORPH, ENDOMORPH
}

enum class SetType {
    NORMAL, WARMUP, DROP, FAILURE, REST_PAUSE, WIDOWMAKER, POWER, GS
}

data class Exercise(
    val id: String,
    val name: String,
    val description: String,
    val cues: List<String>,
    val muscleGroups: List<String>,
    val equipment: List<String>,
    val gifAssetPath: String? = null,
    val isLockedClassic: Boolean = false,
    val injurySubstitutions: List<String> = emptyList(),
    val notes: String? = null
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
    val augmentColor: String? = null,
    val showGoalReps: Boolean = false
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
    val goalReps: String? = null,
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

enum class Gender {
    MALE, FEMALE, OTHER
}

enum class UnitSystem {
    IMPERIAL, METRIC
}

data class UserWorkoutProfile(
    val userId: String,
    val experienceLevel: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
    val somatotype: Somatotype = Somatotype.MESOMORPH,
    val injuries: List<String> = emptyList(),
    val timePerSessionMinutes: Int = 60,
    
    // Bio-data for Macros/TDEE
    val age: Int = 25,
    val heightCm: Float = 175f,
    val weightKg: Float = 75f,
    val gender: Gender = Gender.MALE,
    val activityFactor: Float = 1.375f, // Default: Lightly Active
    val unitSystem: UnitSystem = UnitSystem.IMPERIAL,

    // Active Protocol & Scheduling
    val activeProtocol: WorkoutProtocol? = null,
    val rotationIndex: Int = 0,
    val scheduledDays: List<ScheduledDay> = emptyList(),
    val deepLinkToRoutine: Boolean = true
)

data class ScheduledDay(
    val dayOfWeek: Int, // 1 (Mon) to 7 (Sun)
    val time: String // HH:mm
)

data class WorkoutRoutine(
    val id: String,
    val name: String,
    val description: String? = null,
    val exercises: List<RoutineExercise> = emptyList(),
    val augments: List<WorkoutAugment> = emptyList(),
    val protocol: WorkoutProtocol = WorkoutProtocol.GENERAL,
    val createdAt: Instant = Instant.now(),
    val isSystem: Boolean = false,
    val isAddedToLibrary: Boolean = true
)

data class RoutineExercise(
    val exercise: Exercise,
    val sets: List<RoutineSet> = emptyList()
)

data class RoutineSet(
    val type: SetType = SetType.NORMAL,
    val weight: Float = 0f,
    val reps: Int = 0,
    val goalReps: String? = null
)
