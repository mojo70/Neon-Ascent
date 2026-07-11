package com.neon.ascent.core.data.mapper

import com.neon.ascent.core.data.local.dao.*
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
    isLockedClassic = isLockedClassic,
    injurySubstitutions = injurySubstitutions
)

fun Exercise.toEntity() = ExerciseDefinitionEntity(
    id = id,
    name = name,
    description = description,
    cues = cues,
    muscleGroups = muscleGroups,
    equipment = equipment,
    gifAssetPath = gifAssetPath,
    isLockedClassic = isLockedClassic,
    injurySubstitutions = injurySubstitutions
)

fun WorkoutLogEntity.toDomain() = WorkoutLog(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    order = order,
    exerciseName = exerciseName,
    protocolOverride = protocolOverride?.let { WorkoutProtocol.valueOf(it) },
    supersetId = supersetId,
    augmentId = augmentId,
    augmentName = augmentName,
    augmentColor = augmentColor,
    showGoalReps = showGoalReps
)

fun WorkoutLog.toEntity() = WorkoutLogEntity(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    order = order,
    exerciseName = exerciseName,
    protocolOverride = protocolOverride?.name,
    supersetId = supersetId,
    augmentId = augmentId,
    augmentName = augmentName,
    augmentColor = augmentColor,
    showGoalReps = showGoalReps
)

fun WorkoutAugmentEntity.toDomain(
    exercises: List<ExerciseDefinitionEntity>,
    exerciseRefs: List<AugmentExerciseCrossRef> = emptyList(),
    sets: List<AugmentSetEntity> = emptyList()
) = WorkoutAugment(
    id = id,
    name = name,
    description = description,
    focusBodyPart = focusBodyPart,
    exercises = exercises.map { exerciseEntity ->
        val order = exerciseRefs.find { it.exerciseId == exerciseEntity.id }?.order ?: 0
        order to RoutineExercise(
            exercise = exerciseEntity.toDomain(),
            sets = sets.filter { it.exerciseId == exerciseEntity.id }
                .sortedBy { it.order }
                .map { it.toDomain() }
        )
    }.sortedBy { it.first }.map { it.second },
    colorHex = colorHex,
    isSystem = isSystem,
    isAddedToLibrary = isAddedToLibrary
)

fun WorkoutAugment.toEntity() = WorkoutAugmentEntity(
    id = id,
    name = name,
    description = description,
    focusBodyPart = focusBodyPart,
    colorHex = colorHex,
    isSystem = isSystem,
    isAddedToLibrary = isAddedToLibrary
)

fun SetLogEntity.toDomain() = SetLog(
    id = id,
    workoutLogId = workoutLogId,
    weight = weight,
    reps = reps,
    type = SetType.valueOf(setType),
    goalReps = goalReps,
    isCompleted = isCompleted,
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
    goalReps = goalReps,
    isCompleted = isCompleted,
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

fun RoutineSetEntity.toDomain() = RoutineSet(
    type = SetType.valueOf(type),
    weight = weight,
    reps = reps,
    goalReps = goalReps
)

fun AugmentSetEntity.toDomain() = RoutineSet(
    type = SetType.valueOf(type),
    weight = weight,
    reps = reps,
    goalReps = goalReps
)

fun WorkoutRoutineEntity.toDomain(
    exercisesWithOrder: List<RoutineExerciseWithOrder> = emptyList(),
    sets: List<RoutineSetEntity> = emptyList(),
    augmentsWithOrder: List<RoutineAugmentWithOrder> = emptyList()
) = WorkoutRoutine(
    id = id,
    name = name,
    description = description,
    exercises = exercisesWithOrder.sortedBy { it.ref.order }.map { item ->
        RoutineExercise(
            exercise = item.exercise.toDomain(),
            sets = sets.filter { it.exerciseId == item.ref.exerciseId }
                .sortedBy { it.order }
                .map { it.toDomain() }
        )
    },
    augments = augmentsWithOrder.sortedBy { it.ref.order }.map { item ->
        item.augmentDetails.augment.toDomain(
            item.augmentDetails.exercises,
            item.augmentDetails.exerciseRefs,
            item.augmentDetails.augmentSets
        )
    },
    protocol = WorkoutProtocol.valueOf(protocol),
    createdAt = createdAt,
    isSystem = isSystem,
    isAddedToLibrary = isAddedToLibrary
)

fun WorkoutRoutine.toEntity() = WorkoutRoutineEntity(
    id = id,
    name = name,
    description = description,
    protocol = protocol.name,
    createdAt = createdAt,
    isSystem = isSystem,
    isAddedToLibrary = isAddedToLibrary
)
