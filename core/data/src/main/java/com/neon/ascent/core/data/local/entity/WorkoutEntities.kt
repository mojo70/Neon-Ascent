package com.neon.ascent.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "workout_sessions",
    indices = [Index("date")]
)
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val date: Instant,
    val protocol: String,
    val durationSeconds: Long,
    val notes: String?,
    val experienceLevel: String,
    val somatotype: String
)

@Entity(tableName = "exercise_definitions")
data class ExerciseDefinitionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val cues: List<String>,
    val muscleGroups: List<String>,
    val equipment: List<String>,
    val gifAssetPath: String?,
    val isLockedClassic: Boolean,
    val injurySubstitutions: List<String>
)

@Entity(
    tableName = "workout_logs",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class WorkoutLogEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseId: String,
    val order: Int,
    val exerciseName: String,
    val protocolOverride: String?,
    val supersetId: String? = null,
    val augmentId: String? = null,
    val augmentName: String? = null,
    val augmentColor: String? = null,
    val showGoalReps: Boolean = false
)

@Entity(tableName = "workout_augments")
data class WorkoutAugmentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val focusBodyPart: String,
    val colorHex: String,
    val isSystem: Boolean = false,
    val isAddedToLibrary: Boolean = true
)

@Entity(
    tableName = "augment_exercise_cross_ref",
    primaryKeys = ["augmentId", "exerciseId"],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutAugmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["augmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("augmentId"), Index("exerciseId")]
)
data class AugmentExerciseCrossRef(
    val augmentId: String,
    val exerciseId: String,
    val order: Int
)

@Entity(
    tableName = "augment_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutAugmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["augmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("augmentId"), Index("exerciseId")]
)
data class AugmentSetEntity(
    @PrimaryKey val id: String,
    val augmentId: String,
    val exerciseId: String,
    val order: Int,
    val type: String,
    val weight: Float,
    val reps: Int,
    val goalReps: String? = null
)

@Entity(
    tableName = "routine_augment_cross_ref",
    primaryKeys = ["routineId", "augmentId"],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutRoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkoutAugmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["augmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineId"), Index("augmentId")]
)
data class RoutineAugmentCrossRef(
    val routineId: String,
    val augmentId: String,
    val order: Int
)

@Entity(
    tableName = "set_logs",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutLogId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutLogId")]
)
data class SetLogEntity(
    @PrimaryKey val id: String,
    val workoutLogId: String,
    val weight: Float,
    val reps: Int,
    val setType: String,
    val goalReps: String? = null,
    val isCompleted: Boolean,
    val rir: Int?,
    val isWarmup: Boolean,
    val timestamp: Instant,
    val clusterMiniSetIndex: Int?,
    val isLengthenedPartial: Boolean,
    val isLoadedStretch: Boolean,
    val stretchDurationSeconds: Int?
)

@Entity(tableName = "user_workout_profiles")
data class UserWorkoutProfileEntity(
    @PrimaryKey val userId: String,
    val experienceLevel: String,
    val somatotype: String,
    val injuries: List<String>,
    val preferredDays: List<Int>,
    val timePerSessionMinutes: Int,
    val age: Int,
    val heightCm: Float,
    val weightKg: Float,
    val gender: String,
    val activityFactor: Float
)

@Entity(tableName = "workout_routines")
data class WorkoutRoutineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val protocol: String,
    val createdAt: Instant,
    val isSystem: Boolean = false,
    val isAddedToLibrary: Boolean = true
)

@Entity(
    tableName = "routine_exercise_cross_ref",
    primaryKeys = ["routineId", "exerciseId"],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutRoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineId"), Index("exerciseId")]
)
data class RoutineExerciseCrossRef(
    val routineId: String,
    val exerciseId: String,
    val order: Int
)

@Entity(
    tableName = "routine_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutRoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineId"), Index("exerciseId")]
)
data class RoutineSetEntity(
    @PrimaryKey val id: String,
    val routineId: String,
    val exerciseId: String,
    val order: Int,
    val type: String,
    val weight: Float,
    val reps: Int,
    val goalReps: String? = null
)
