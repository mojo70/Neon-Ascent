package com.neon.ascent.core.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.neon.ascent.core.data.local.dao.*
import com.neon.ascent.core.data.local.entity.*
import com.neon.ascent.core.domain.workout.models.*

private val gson = Gson()

fun WorkoutSessionEntity.toDomain() = WorkoutSession(
    id = id,
    date = date,
    protocol = WorkoutProtocol.valueOf(protocol),
    durationSeconds = durationSeconds,
    notes = notes,
    experienceLevel = ExperienceLevel.valueOf(experienceLevel),
    somatotype = Somatotype.valueOf(somatotype),
    sessionRpe = sessionRpe,
    jointHealth = jointHealth,
    isDeload = isDeload
)

fun WorkoutSession.toEntity() = WorkoutSessionEntity(
    id = id,
    date = date,
    protocol = protocol.name,
    durationSeconds = durationSeconds,
    notes = notes,
    experienceLevel = experienceLevel.name,
    somatotype = somatotype.name,
    sessionRpe = sessionRpe,
    jointHealth = jointHealth,
    isDeload = isDeload
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
    injurySubstitutions = injurySubstitutions,
    dangerousFor = dangerousFor,
    movementType = MovementType.valueOf(movementType),
    notes = notes
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
    injurySubstitutions = injurySubstitutions,
    dangerousFor = dangerousFor,
    movementType = movementType.name,
    notes = notes
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
    isAddedToLibrary = isAddedToLibrary,
    scheduledDays = runCatching {
        gson.fromJson<List<ScheduledDay>>(scheduledDays, object : TypeToken<List<ScheduledDay>>() {}.type)
    }.getOrNull() ?: emptyList()
)

fun WorkoutAugment.toEntity() = WorkoutAugmentEntity(
    id = id,
    name = name,
    description = description,
    focusBodyPart = focusBodyPart,
    colorHex = colorHex,
    isSystem = isSystem,
    isAddedToLibrary = isAddedToLibrary,
    scheduledDays = gson.toJson(scheduledDays)
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
    timePerSessionMinutes = timePerSessionMinutes,
    age = age,
    heightCm = heightCm,
    weightKg = weightKg,
    gender = Gender.valueOf(gender),
    activityFactor = activityFactor,
    unitSystem = UnitSystem.valueOf(unitSystem),
    activeProtocol = activeProtocol?.let { WorkoutProtocol.valueOf(it) },
    rotationIndex = rotationIndex,
    scheduledDays = gson.fromJson(scheduledDays, object : TypeToken<List<ScheduledDay>>() {}.type),
    deepLinkToRoutine = deepLinkToRoutine,
    autoWeightIncrement = autoWeightIncrement,
    weightIncrementCompound = weightIncrementCompound,
    weightIncrementIsolation = weightIncrementIsolation,
    rirCapturePerMiniSet = rirCapturePerMiniSet,
    sequencerEnabled = sequencerEnabled,
    customSequenceIds = gson.fromJson(customSequenceIds, object : TypeToken<List<String>>() {}.type),
    coachingHintsEnabled = coachingHintsEnabled,
    lastBlastStartDate = lastBlastStartDate
)

fun UserWorkoutProfile.toEntity() = UserWorkoutProfileEntity(
    userId = userId,
    experienceLevel = experienceLevel.name,
    somatotype = somatotype.name,
    injuries = injuries,
    timePerSessionMinutes = timePerSessionMinutes,
    age = age,
    heightCm = heightCm,
    weightKg = weightKg,
    gender = gender.name,
    activityFactor = activityFactor,
    unitSystem = unitSystem.name,
    activeProtocol = activeProtocol?.name,
    rotationIndex = rotationIndex,
    scheduledDays = gson.toJson(scheduledDays),
    deepLinkToRoutine = deepLinkToRoutine,
    autoWeightIncrement = autoWeightIncrement,
    weightIncrementCompound = weightIncrementCompound,
    weightIncrementIsolation = weightIncrementIsolation,
    rirCapturePerMiniSet = rirCapturePerMiniSet,
    sequencerEnabled = sequencerEnabled,
    customSequenceIds = gson.toJson(customSequenceIds),
    coachingHintsEnabled = coachingHintsEnabled,
    lastBlastStartDate = lastBlastStartDate
)

fun ProgressionStateEntity.toDomain() = ProgressionState(
    exerciseId = exerciseId,
    bestClusterReps = bestClusterReps,
    weightAtBest = weightAtBest,
    consecutiveMisses = consecutiveMisses,
    currentWeight = currentWeight,
    lastRotationDate = lastRotationDate
)

fun ProgressionState.toEntity() = ProgressionStateEntity(
    exerciseId = exerciseId,
    bestClusterReps = bestClusterReps,
    weightAtBest = weightAtBest,
    consecutiveMisses = consecutiveMisses,
    currentWeight = currentWeight,
    lastRotationDate = lastRotationDate
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

fun ExerciseAccomplishmentsEntity.toDomain() = ExerciseAccomplishments(
    exerciseId = exerciseId,
    heaviestWeight = heaviestWeight,
    heaviestWeightReps = heaviestWeightReps,
    heaviestWeightDate = heaviestWeightDate,
    maxEstimatedOneRepMax = maxEstimatedOneRepMax,
    maxOneRepMaxWeight = maxOneRepMaxWeight,
    maxOneRepMaxReps = maxOneRepMaxReps,
    maxOneRepMaxDate = maxOneRepMaxDate,
    maxSessionVolume = maxSessionVolume,
    maxSessionVolumeDate = maxSessionVolumeDate,
    maxRepsAtTopWeight = maxRepsAtTopWeight,
    topWeightForReps = topWeightForReps,
    bestClusterReps = bestClusterReps,
    bestClusterWeight = bestClusterWeight,
    bestClusterDate = bestClusterDate
)

fun ExerciseAccomplishments.toEntity() = ExerciseAccomplishmentsEntity(
    exerciseId = exerciseId,
    heaviestWeight = heaviestWeight,
    heaviestWeightReps = heaviestWeightReps,
    heaviestWeightDate = heaviestWeightDate,
    maxEstimatedOneRepMax = maxEstimatedOneRepMax,
    maxOneRepMaxWeight = maxOneRepMaxWeight,
    maxOneRepMaxReps = maxOneRepMaxReps,
    maxOneRepMaxDate = maxOneRepMaxDate,
    maxSessionVolume = maxSessionVolume,
    maxSessionVolumeDate = maxSessionVolumeDate,
    maxRepsAtTopWeight = maxRepsAtTopWeight,
    topWeightForReps = topWeightForReps,
    bestClusterReps = bestClusterReps,
    bestClusterWeight = bestClusterWeight,
    bestClusterDate = bestClusterDate
)


fun WorkoutRoutineEntity.toDomain(
    exercisesWithOrder: List<RoutineExerciseWithOrder> = emptyList(),
    sets: List<RoutineSetEntity> = emptyList(),
    augmentsWithOrder: List<RoutineAugmentWithOrder> = emptyList()
): WorkoutRoutine {
    val routineExercises = exercisesWithOrder.sortedBy { it.ref.order }.map { item ->
        RoutineExercise(
            exercise = item.exercise.toDomain(),
            sets = sets.filter { it.exerciseId == item.ref.exerciseId }
                .sortedBy { it.order }
                .map { it.toDomain() }
        )
    }
    
    if (routineExercises.isEmpty() && exercisesWithOrder.isNotEmpty()) {
        android.util.Log.e("WorkoutMapper", "ERROR: exercisesWithOrder was not empty (${exercisesWithOrder.size}) but routineExercises IS EMPTY for routine $name")
    }

    return WorkoutRoutine(
        id = id,
        name = name,
        description = description,
        exercises = routineExercises,
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
}

fun WorkoutRoutine.toEntity() = WorkoutRoutineEntity(
    id = id,
    name = name,
    description = description,
    protocol = protocol.name,
    createdAt = createdAt,
    isSystem = isSystem,
    isAddedToLibrary = isAddedToLibrary
)
