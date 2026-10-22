package com.neon.ascent.core.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.neon.ascent.core.data.local.dao.*
import com.neon.ascent.core.data.local.entity.*
import com.neon.ascent.core.domain.workout.models.*

private val gson = Gson()

private inline fun <reified T : Enum<T>> safeEnumValueOf(value: String?, default: T): T {
    if (value.isNullOrBlank()) return default
    return try {
        enumValueOf<T>(value.trim().uppercase())
    } catch (_: Exception) {
        default
    }
}

private inline fun <reified T : Enum<T>> safeNullableEnumValueOf(value: String?): T? {
    if (value.isNullOrBlank()) return null
    return try {
        enumValueOf<T>(value.trim().uppercase())
    } catch (_: Exception) {
        null
    }
}

fun WorkoutSessionEntity.toDomain() = WorkoutSession(
    id = id,
    date = date,
    protocol = safeEnumValueOf(protocol, WorkoutProtocol.GENERAL),
    durationSeconds = durationSeconds,
    notes = notes,
    experienceLevel = safeEnumValueOf(experienceLevel, ExperienceLevel.INTERMEDIATE),
    somatotype = safeEnumValueOf(somatotype, Somatotype.MESOMORPH),
    sessionRpe = sessionRpe,
    jointHealth = jointHealth,
    isDeload = isDeload,
    cycleId = cycleId,
    protocolDayType = safeNullableEnumValueOf(protocolDayType),
    primaryAugmentId = primaryAugmentId
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
    isDeload = isDeload,
    cycleId = cycleId,
    protocolDayType = protocolDayType?.name,
    primaryAugmentId = primaryAugmentId
)

fun ProtocolCycleEntity.toDomain() = ProtocolCycle(
    id = id,
    userId = userId,
    protocol = safeEnumValueOf(protocol, WorkoutProtocol.GENERAL),
    startedAt = startedAt,
    endedAt = endedAt,
    status = safeEnumValueOf(status, CycleStatus.ACTIVE),
    currentWeek = currentWeek,
    currentDayIndex = currentDayIndex,
    configJson = configJson
)

fun ProtocolCycle.toEntity() = ProtocolCycleEntity(
    id = id,
    userId = userId,
    protocol = protocol.name,
    startedAt = startedAt,
    endedAt = endedAt,
    status = status.name,
    currentWeek = currentWeek,
    currentDayIndex = currentDayIndex,
    configJson = configJson
)

fun ExerciseMaxEntity.toDomain() = ExerciseMax(
    familyId = familyId,
    testedAt = testedAt,
    oneRepMax = oneRepMax,
    rm15 = rm15,
    rm10 = rm10,
    rm5 = rm5,
    trainingMax = trainingMax,
    source = safeEnumValueOf(source, MaxSource.ESTIMATED)
)

fun ExerciseMax.toEntity() = ExerciseMaxEntity(
    familyId = familyId,
    testedAt = testedAt,
    oneRepMax = oneRepMax,
    rm15 = rm15,
    rm10 = rm10,
    rm5 = rm5,
    trainingMax = trainingMax,
    source = source.name
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
    movementType = safeEnumValueOf(movementType, MovementType.UNDEFINED),
    notes = notes,
    familyId = familyId,
    familyName = familyName,
    implement = safeEnumValueOf(implement, Implement.BARBELL),
    stance = safeEnumValueOf(stance, Stance.STANDARD),
    specialtyBar = specialtyBar,
    rangeOverrideMin = rangeOverrideMin,
    rangeOverrideMax = rangeOverrideMax,
    allowsAddedLoad = allowsAddedLoad,
    isPrimaryVariant = isPrimaryVariant
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
    notes = notes,
    familyId = familyId,
    familyName = familyName,
    implement = implement.name,
    stance = stance.name,
    specialtyBar = specialtyBar,
    rangeOverrideMin = rangeOverrideMin,
    rangeOverrideMax = rangeOverrideMax,
    allowsAddedLoad = allowsAddedLoad,
    isPrimaryVariant = isPrimaryVariant
)

fun AugmentActivationEntity.toDomain() = AugmentActivation(
    id = id,
    augmentId = augmentId,
    userId = userId,
    mode = safeEnumValueOf(mode, AugmentRunMode.INDEPENDENT),
    status = safeEnumValueOf(status, AugmentActivationStatus.ACTIVE),
    loggingStyle = safeEnumValueOf(loggingStyle, AugmentLoggingStyle.INHERIT),
    scheduledDays = gson.fromJson(scheduledDays, object : TypeToken<List<ScheduledDay>>() {}.type),
    createdAt = createdAt,
    windowStart = windowStart,
    windowEnd = windowEnd,
    hostProtocolFilter = safeNullableEnumValueOf(hostProtocolFilter),
    dayTypeFilter = safeNullableEnumValueOf(dayTypeFilter),
    reminderEnabled = reminderEnabled
)

fun AugmentActivation.toEntity() = AugmentActivationEntity(
    id = id,
    augmentId = augmentId,
    userId = userId,
    mode = mode.name,
    status = status.name,
    loggingStyle = loggingStyle.name,
    scheduledDays = gson.toJson(scheduledDays),
    createdAt = createdAt,
    windowStart = windowStart,
    windowEnd = windowEnd,
    hostProtocolFilter = hostProtocolFilter?.name,
    dayTypeFilter = dayTypeFilter?.name,
    reminderEnabled = reminderEnabled
)

fun WorkoutLogEntity.toDomain() = WorkoutLog(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    order = order,
    exerciseName = exerciseName,
    protocolOverride = safeNullableEnumValueOf(protocolOverride),
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
    type = safeEnumValueOf(setType, SetType.NORMAL),
    goalReps = goalReps,
    isCompleted = isCompleted,
    rir = rir,
    isWarmup = isWarmup,
    timestamp = timestamp,
    clusterMiniSetIndex = clusterMiniSetIndex,
    isLengthenedPartial = isLengthenedPartial,
    isLoadedStretch = isLoadedStretch,
    stretchDurationSeconds = stretchDurationSeconds,
    prescribedWeight = prescribedWeight,
    prescribedReps = prescribedReps,
    percentOfMax = percentOfMax,
    isAmrap = isAmrap,
    accommodatingLoad = accommodatingLoad
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
    stretchDurationSeconds = stretchDurationSeconds,
    prescribedWeight = prescribedWeight,
    prescribedReps = prescribedReps,
    percentOfMax = percentOfMax,
    isAmrap = isAmrap,
    accommodatingLoad = accommodatingLoad
)

fun UserWorkoutProfileEntity.toDomain() = UserWorkoutProfile(
    userId = userId,
    experienceLevel = safeEnumValueOf(experienceLevel, ExperienceLevel.INTERMEDIATE),
    somatotype = safeEnumValueOf(somatotype, Somatotype.MESOMORPH),
    injuries = injuries,
    timePerSessionMinutes = timePerSessionMinutes,
    age = age,
    heightCm = heightCm,
    weightKg = weightKg,
    gender = safeEnumValueOf(gender, Gender.OTHER),
    activityFactor = activityFactor,
    unitSystem = safeEnumValueOf(unitSystem, UnitSystem.IMPERIAL),
    activeProtocol = safeNullableEnumValueOf(activeProtocol),
    rotationIndex = rotationIndex,
    scheduledDays = runCatching { gson.fromJson<List<ScheduledDay>>(scheduledDays, object : TypeToken<List<ScheduledDay>>() {}.type) }.getOrNull() ?: emptyList(),
    deepLinkToRoutine = deepLinkToRoutine,
    autoWeightIncrement = autoWeightIncrement,
    progressionMode = safeEnumValueOf(progressionMode, ProgressionMode.FIXED_INCREMENT),
    weightIncrementCompound = weightIncrementUpperCompound,
    weightIncrementUpperCompound = weightIncrementUpperCompound,
    weightIncrementLowerCompound = weightIncrementLowerCompound,
    weightIncrementIsolation = weightIncrementIsolation,
    progressionPercentage = progressionPercentage,
    roundingIncrement = roundingIncrement,
    rirCapturePerMiniSet = rirCapturePerMiniSet,
    sequencerEnabled = sequencerEnabled,
    customSequenceIds = runCatching { gson.fromJson<List<String>>(customSequenceIds, object : TypeToken<List<String>>() {}.type) }.getOrNull() ?: emptyList(),
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
    progressionMode = progressionMode.name,
    weightIncrementCompound = weightIncrementUpperCompound,
    weightIncrementUpperCompound = weightIncrementUpperCompound,
    weightIncrementLowerCompound = weightIncrementLowerCompound,
    weightIncrementIsolation = weightIncrementIsolation,
    progressionPercentage = progressionPercentage,
    roundingIncrement = roundingIncrement,
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
    type = safeEnumValueOf(type, SetType.NORMAL),
    weight = weight,
    reps = reps,
    goalReps = goalReps
)

fun AugmentSetEntity.toDomain() = RoutineSet(
    type = safeEnumValueOf(type, SetType.NORMAL),
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

fun FuelSnapshotEntity.toDomain() = FuelSnapshot(
    id = id,
    timestamp = timestamp,
    weightKg = weightKg,
    tdee = tdee,
    protein = protein,
    carb = carb,
    fat = fat,
    activityFactor = activityFactor,
    somatotype = safeEnumValueOf(somatotype, Somatotype.MESOMORPH)
)

fun FuelSnapshot.toEntity() = FuelSnapshotEntity(
    id = id,
    timestamp = timestamp,
    weightKg = weightKg,
    tdee = tdee,
    protein = protein,
    carb = carb,
    fat = fat,
    activityFactor = activityFactor,
    somatotype = somatotype.name
)

fun ProtocolRepTargetEntity.toDomain() = ProtocolRepTarget(
    id = id,
    protocol = safeEnumValueOf(protocol, WorkoutProtocol.GENERAL),
    movementType = safeEnumValueOf(movementType, MovementType.UNDEFINED),
    setType = safeEnumValueOf(setType, SetType.NORMAL),
    familyId = familyId,
    minReps = minReps,
    maxReps = maxReps,
    unit = unit
)

fun ProtocolRepTarget.toEntity() = ProtocolRepTargetEntity(
    id = id,
    protocol = protocol.name,
    movementType = movementType.name,
    setType = setType.name,
    familyId = familyId,
    minReps = minReps,
    maxReps = maxReps,
    unit = unit
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
        protocol = safeEnumValueOf(protocol, WorkoutProtocol.GENERAL),
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
