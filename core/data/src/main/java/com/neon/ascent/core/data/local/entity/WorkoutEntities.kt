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
    val isLockedClassic: Boolean
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
    val protocolOverride: String?
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
    val timePerSessionMinutes: Int
)
