package com.neon.ascent.core.data.mapper

import com.neon.ascent.core.data.local.entity.*
import com.neon.ascent.core.domain.workout.models.*

fun WorkoutSessionEntity.toDomain() = WorkoutSession(
    id = id,
    date = date,
    protocol = WorkoutProtocol.valueOf(protocol),
    durationSeconds = durationSeconds,
    notes = notes,
    experienceLevel = ExperienceLevel.valueOf(experienceLevel),
    somatotype = Somatotype.valueOf(somatotype)
)

fun WorkoutSession.toEntity() = WorkoutSessionEntity(
    id = id,
    date = date,
    protocol = protocol.name,
    durationSeconds = durationSeconds,
    notes = notes,
    experienceLevel = experienceLevel.name,
    somatotype = somatotype.name
)

fun ExerciseDefinitionEntity.toDomain() = Exercise(
    id = id,
    name = name,
    description = description,
    cues = cues,
    muscleGroups = muscleGroups,
    equipment = equipment,
    gifAssetPath = gifAssetPath,
    isLockedClassic = isLockedClassic
)

fun Exercise.toEntity() = ExerciseDefinitionEntity(
    id = id,
    name = name,
    description = description,
    cues = cues,
    muscleGroups = muscleGroups,
    equipment = equipment,
    gifAssetPath = gifAssetPath,
    isLockedClassic = isLockedClassic
)

fun WorkoutLogEntity.toDomain() = WorkoutLog(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    order = order,
    exerciseName = exerciseName,
    protocolOverride = protocolOverride?.let { WorkoutProtocol.valueOf(it) }
)

fun WorkoutLog.toEntity() = WorkoutLogEntity(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    order = order,
    exerciseName = exerciseName,
    protocolOverride = protocolOverride?.name
)

fun SetLogEntity.toDomain() = SetLog(
    id = id,
    workoutLogId = workoutLogId,
    weight = weight,
    reps = reps,
    type = SetType.valueOf(setType),
    rir = rir,
    isWarmup = isWarmup,
    timestamp = timestamp,
    clusterMiniSetIndex = clusterMiniSetIndex,
    isLengthenedPartial = isLengthenedPartial,
    isLoadedStretch = isLoadedStretch,
    stretchDurationSeconds = stretchDurationSeconds
)

fun SetLog.toEntity() = SetLogEntity(
    id = id,
    workoutLogId = workoutLogId,
    weight = weight,
    reps = reps,
    setType = type.name,
    rir = rir,
    isWarmup = isWarmup,
    timestamp = timestamp,
    clusterMiniSetIndex = clusterMiniSetIndex,
    isLengthenedPartial = isLengthenedPartial,
    isLoadedStretch = isLoadedStretch,
    stretchDurationSeconds = stretchDurationSeconds
)

fun UserWorkoutProfileEntity.toDomain() = UserWorkoutProfile(
    userId = userId,
    experienceLevel = ExperienceLevel.valueOf(experienceLevel),
    somatotype = Somatotype.valueOf(somatotype),
    injuries = injuries,
    preferredDays = preferredDays,
    timePerSessionMinutes = timePerSessionMinutes
)

fun UserWorkoutProfile.toEntity() = UserWorkoutProfileEntity(
    userId = userId,
    experienceLevel = experienceLevel.name,
    somatotype = somatotype.name,
    injuries = injuries,
    preferredDays = preferredDays,
    timePerSessionMinutes = timePerSessionMinutes
)

fun WorkoutRoutineEntity.toDomain(exercises: List<ExerciseDefinitionEntity>) = WorkoutRoutine(
    id = id,
    name = name,
    description = description,
    exercises = exercises.map { it.toDomain() },
    protocol = WorkoutProtocol.valueOf(protocol),
    createdAt = createdAt
)

fun WorkoutRoutine.toEntity() = WorkoutRoutineEntity(
    id = id,
    name = name,
    description = description,
    protocol = protocol.name,
    createdAt = createdAt
)
