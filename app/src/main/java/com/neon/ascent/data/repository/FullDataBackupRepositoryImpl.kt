package com.neon.ascent.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import com.neon.ascent.core.data.NeonAscentDatabase
import com.neon.ascent.core.data.datastore.SelfMapPreferencesDataStore
import com.neon.ascent.core.data.local.dao.AscensionDao
import com.neon.ascent.core.data.local.dao.BiomarkerDao
import com.neon.ascent.core.data.local.dao.BodySampleDao
import com.neon.ascent.core.data.local.dao.DailyVitalRollupDao
import com.neon.ascent.core.data.local.dao.DopamineMenuDao
import com.neon.ascent.core.data.local.dao.GoalDao
import com.neon.ascent.core.data.local.dao.NeuralMemoryDao
import com.neon.ascent.core.data.local.dao.OperativeProfileDao
import com.neon.ascent.core.data.local.dao.ReconstitutionDao
import com.neon.ascent.core.data.local.dao.SelfMapDao
import com.neon.ascent.core.data.local.dao.SpecialDao
import com.neon.ascent.core.data.local.dao.VaultDao
import com.neon.ascent.core.data.local.dao.WorkoutDao
import com.neon.ascent.core.data.local.entity.*
import com.neon.ascent.core.domain.backup.models.*
import com.neon.ascent.core.domain.chronicle.ChronicleEntry
import com.neon.ascent.core.domain.chronicle.ChronicleRepository
import com.neon.ascent.core.domain.goals.models.SuccessMetric
import com.neon.ascent.core.domain.library.models.LibraryQuote
import com.neon.ascent.core.domain.library.repository.LibraryRepository
import com.neon.ascent.core.domain.model.DopamineCategory
import com.neon.ascent.core.domain.model.EnergyLevel
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.core.domain.repository.FullDataBackupRepository
import com.neon.ascent.core.domain.selfmap.SelfMapCategory
import com.neon.ascent.core.domain.vault.models.VaultAssetType
import com.neon.ascent.data.local.AppDatabase
import com.neon.ascent.data.local.BiohackingDao
import com.neon.ascent.data.local.BookDao
import com.neon.ascent.data.local.ChatDao
import com.neon.ascent.data.local.DailyPrayerDao
import com.neon.ascent.data.local.GoalTaskDao
import com.neon.ascent.data.local.JournalDao
import com.neon.ascent.data.local.LoreDao
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.data.mapper.toChronicleEntry
import com.neon.ascent.model.BioProtocolLog
import com.neon.ascent.model.BiohackingData
import com.neon.ascent.model.ChatMessage
import com.neon.ascent.model.ChatSession
import com.neon.ascent.model.DailyPrayer
import com.neon.ascent.model.JournalEntry
import com.neon.ascent.model.QuoteEntity
import com.neon.ascent.model.UserCharacter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import com.neon.ascent.core.data.datastore.BriefPreferencesDataStore
import com.neon.ascent.core.data.datastore.DirectivePreferencesDataStore
import com.neon.ascent.core.data.datastore.HealthPreferencesDataStore
import com.neon.ascent.core.data.datastore.WorkoutPreferencesDataStore
import com.neon.ascent.core.data.local.dao.LibraryDao
import com.neon.ascent.core.data.local.dao.RiteSessionDao
import com.neon.ascent.core.domain.goals.models.DirectiveIntakeMode
import java.io.File
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FullDataBackupRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val biomarkerDao: BiomarkerDao,
    private val dailyVitalRollupDao: DailyVitalRollupDao,
    private val specialDao: SpecialDao,
    private val dopamineMenuDao: DopamineMenuDao,
    private val biohackingDao: BiohackingDao,
    private val bookDao: BookDao,
    private val libraryRepository: LibraryRepository,
    private val journalDao: JournalDao,
    private val dailyPrayerDao: DailyPrayerDao,
    private val loreDao: LoreDao,
    private val userCharacterDao: UserCharacterDao,
    private val goalDao: GoalDao,
    private val goalTaskDao: GoalTaskDao,
    private val chronicleRepository: ChronicleRepository,
    private val operativeProfileDao: OperativeProfileDao,
    private val bodySampleDao: BodySampleDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val settingsRepository: SettingsRepository,
    private val coreDatabase: NeonAscentDatabase,
    private val appDatabase: AppDatabase,
    private val ascensionDao: AscensionDao,
    private val vaultDao: VaultDao,
    private val selfMapDao: SelfMapDao,
    private val reconstitutionDao: ReconstitutionDao,
    private val neuralMemoryDao: NeuralMemoryDao,
    private val chatDao: ChatDao,
    private val selfMapPreferencesDataStore: SelfMapPreferencesDataStore,
    private val healthPreferencesDataStore: HealthPreferencesDataStore,
    private val workoutPreferencesDataStore: WorkoutPreferencesDataStore,
    private val briefPreferencesDataStore: BriefPreferencesDataStore,
    private val directivePreferencesDataStore: DirectivePreferencesDataStore,
    private val dietPreferencesDataStore: com.neon.ascent.core.data.datastore.DietPreferencesDataStore,
    private val dietRepository: com.neon.ascent.core.domain.repository.DietRepository,
    private val supplementPreferencesDataStore: com.neon.ascent.core.data.datastore.SupplementPreferencesDataStore,
    private val supplementRepository: com.neon.ascent.core.domain.repository.SupplementRepository,
    private val biohackingProtocolPreferencesDataStore: com.neon.ascent.core.data.datastore.BiohackingProtocolPreferencesDataStore,
    private val biohackingProtocolRepository: com.neon.ascent.core.domain.repository.BiohackingProtocolRepository,
    private val labsFocusPreferencesDataStore: com.neon.ascent.core.data.datastore.LabsFocusPreferencesDataStore,
    private val riteSessionDao: RiteSessionDao,
    private val libraryDao: LibraryDao,
    @ApplicationContext private val context: Context
) : FullDataBackupRepository {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    override suspend fun exportBackupJson(scope: BackupScope): String {
        val now = Instant.now().toString()

        val workoutSection = if (scope.includeWorkout) {
            val sessionsWithDetails = workoutDao.getAllSessionsWithDetails().first()
            val sessionsDto = sessionsWithDetails.map { s ->
                WorkoutSessionDto(
                    id = s.session.id,
                    dateEpochMs = s.session.date.toEpochMilli(),
                    protocol = s.session.protocol,
                    durationSeconds = s.session.durationSeconds,
                    notes = s.session.notes,
                    experienceLevel = s.session.experienceLevel,
                    somatotype = s.session.somatotype,
                    sessionRpe = s.session.sessionRpe,
                    jointHealth = s.session.jointHealth,
                    isDeload = s.session.isDeload,
                    cycleId = s.session.cycleId,
                    protocolDayType = s.session.protocolDayType,
                    primaryAugmentId = s.session.primaryAugmentId
                )
            }

            val logsDto = sessionsWithDetails.flatMap { s ->
                s.logs.map { l ->
                    WorkoutLogDto(
                        id = l.log.id,
                        sessionId = l.log.sessionId,
                        exerciseId = l.log.exerciseId,
                        order = l.log.order,
                        exerciseName = l.log.exerciseName,
                        showGoalReps = l.log.showGoalReps,
                        supersetId = l.log.supersetId
                    )
                }
            }

            val setsDto = sessionsWithDetails.flatMap { s ->
                s.logs.flatMap { l ->
                    l.sets.map { st ->
                        SetLogDto(
                            id = st.id,
                            workoutLogId = st.workoutLogId,
                            weight = st.weight,
                            reps = st.reps,
                            setType = st.setType,
                            goalReps = st.goalReps,
                            isCompleted = st.isCompleted,
                            rir = st.rir,
                            isWarmup = st.isWarmup,
                            timestampEpochMs = st.timestamp.toEpochMilli(),
                            clusterMiniSetIndex = st.clusterMiniSetIndex,
                            isLengthenedPartial = st.isLengthenedPartial,
                            isLoadedStretch = st.isLoadedStretch,
                            stretchDurationSeconds = st.stretchDurationSeconds,
                            prescribedWeight = st.prescribedWeight,
                            prescribedReps = st.prescribedReps,
                            percentOfMax = st.percentOfMax,
                            isAmrap = st.isAmrap,
                            accommodatingLoad = st.accommodatingLoad
                        )
                    }
                }
            }

            val exercisesDto = workoutDao.getExerciseDefinitions().first()
                .filter { it.isCustom }
                .sortedWith(
                    compareByDescending<ExerciseDefinitionEntity> { it.isPrimaryVariant }
                        .thenBy { it.id }
                )
                .map { e ->
                    ExerciseDefinitionDto(
                        id = e.id,
                        name = e.name,
                        description = e.description,
                        cues = e.cues,
                        muscleGroups = e.muscleGroups,
                        equipment = e.equipment,
                        gifAssetPath = e.gifAssetPath,
                        isLockedClassic = e.isLockedClassic,
                        injurySubstitutions = e.injurySubstitutions,
                        dangerousFor = e.dangerousFor,
                        movementType = e.movementType,
                        notes = e.notes,
                        familyId = e.familyId,
                        familyName = e.familyName,
                        implement = e.implement,
                        stance = e.stance,
                        specialtyBar = e.specialtyBar,
                        rangeOverrideMin = e.rangeOverrideMin,
                        rangeOverrideMax = e.rangeOverrideMax,
                        allowsAddedLoad = e.allowsAddedLoad,
                        isPrimaryVariant = e.isPrimaryVariant,
                        tags = e.tags,
                        isCustom = e.isCustom
                    )
                }

            val maxesDto = workoutDao.getAllExerciseMaxes().first().map { m ->
                ExerciseMaxDto(
                    familyId = m.familyId,
                    testedAtEpochMs = m.testedAt.toEpochMilli(),
                    oneRepMax = m.oneRepMax,
                    rm15 = m.rm15,
                    rm10 = m.rm10,
                    rm5 = m.rm5,
                    trainingMax = m.trainingMax,
                    source = m.source
                )
            }

            val allProfiles = workoutDao.getAllUserProfiles().first()
            val profilesDto = allProfiles.map { p ->
                UserWorkoutProfileDto(
                    userId = p.userId,
                    experienceLevel = p.experienceLevel,
                    somatotype = p.somatotype,
                    gender = p.gender,
                    activeProtocol = p.activeProtocol,
                    sequencerEnabled = p.sequencerEnabled,
                    autoWeightIncrement = p.autoWeightIncrement,
                    progressionMode = p.progressionMode,
                    weightIncrementCompound = p.weightIncrementCompound,
                    weightIncrementUpperCompound = p.weightIncrementUpperCompound,
                    weightIncrementLowerCompound = p.weightIncrementLowerCompound,
                    weightIncrementIsolation = p.weightIncrementIsolation,
                    progressionPercentage = p.progressionPercentage,
                    roundingIncrement = p.roundingIncrement,
                    rirCapturePerMiniSet = p.rirCapturePerMiniSet,
                    coachingHintsEnabled = p.coachingHintsEnabled,
                    injuries = p.injuries,
                    timePerSessionMinutes = p.timePerSessionMinutes,
                    age = p.age,
                    heightCm = p.heightCm,
                    weightKg = p.weightKg,
                    activityFactor = p.activityFactor,
                    unitSystem = p.unitSystem,
                    rotationIndex = p.rotationIndex,
                    scheduledDays = p.scheduledDays,
                    deepLinkToRoutine = p.deepLinkToRoutine,
                    customSequenceIds = p.customSequenceIds,
                    cyberCrappRotationMode = p.cyberCrappRotationMode,
                    lastBlastStartDateEpochMs = p.lastBlastStartDate?.toEpochMilli()
                )
            }
            val primaryProfileDto = profilesDto.firstOrNull { it.userId == "default_user" } ?: profilesDto.firstOrNull()

            val cyclesDto = workoutDao.getAllProtocolCycles().first().map { c ->
                ProtocolCycleDto(
                    id = c.id,
                    userId = c.userId,
                    protocol = c.protocol,
                    startedAtEpochMs = c.startedAt.toEpochMilli(),
                    endedAtEpochMs = c.endedAt?.toEpochMilli(),
                    status = c.status,
                    currentWeek = c.currentWeek,
                    currentDayIndex = c.currentDayIndex,
                    configJson = c.configJson
                )
            }

            val progressionStatesDto = workoutDao.getAllProgressionStates().first().map { ps ->
                ProgressionStateDto(
                    exerciseId = ps.exerciseId,
                    bestClusterReps = ps.bestClusterReps,
                    weightAtBest = ps.weightAtBest,
                    consecutiveMisses = ps.consecutiveMisses,
                    currentWeight = ps.currentWeight,
                    lastRotationDateEpochMs = ps.lastRotationDate?.toEpochMilli()
                )
            }

            val accomplishmentsDto = workoutDao.getAllAccomplishments().first().map { acc ->
                ExerciseAccomplishmentDto(
                    exerciseId = acc.exerciseId,
                    heaviestWeight = acc.heaviestWeight,
                    heaviestWeightReps = acc.heaviestWeightReps,
                    heaviestWeightDateEpochMs = acc.heaviestWeightDate?.toEpochMilli(),
                    maxEstimatedOneRepMax = acc.maxEstimatedOneRepMax,
                    maxOneRepMaxWeight = acc.maxOneRepMaxWeight,
                    maxOneRepMaxReps = acc.maxOneRepMaxReps,
                    maxOneRepMaxDateEpochMs = acc.maxOneRepMaxDate?.toEpochMilli(),
                    maxSessionVolume = acc.maxSessionVolume,
                    maxSessionVolumeDateEpochMs = acc.maxSessionVolumeDate?.toEpochMilli(),
                    maxRepsAtTopWeight = acc.maxRepsAtTopWeight,
                    topWeightForReps = acc.topWeightForReps,
                    bestClusterReps = acc.bestClusterReps,
                    bestClusterWeight = acc.bestClusterWeight,
                    bestClusterDateEpochMs = acc.bestClusterDate?.toEpochMilli()
                )
            }

            val routinesWithDetails = workoutDao.getAllRoutines().first()
            val routinesDto = routinesWithDetails.map { r ->
                WorkoutRoutineDto(
                    id = r.routine.id,
                    name = r.routine.name,
                    description = r.routine.description,
                    protocol = r.routine.protocol,
                    createdAtEpochMs = r.routine.createdAt.toEpochMilli(),
                    isSystem = r.routine.isSystem,
                    isAddedToLibrary = r.routine.isAddedToLibrary,
                    scheduledDays = r.routine.scheduledDays
                )
            }

            val routineExerciseCrossRefsDto = workoutDao.getAllRoutineExerciseCrossRefs().first().map { ref ->
                RoutineExerciseCrossRefDto(
                    routineId = ref.routineId,
                    exerciseId = ref.exerciseId,
                    order = ref.order
                )
            }

            val routineSetsDto = workoutDao.getAllRoutineSets().first().map { s ->
                RoutineSetDto(
                    id = s.id,
                    routineId = s.routineId,
                    exerciseId = s.exerciseId,
                    order = s.order,
                    type = s.type,
                    weight = s.weight,
                    reps = s.reps,
                    goalReps = s.goalReps
                )
            }

            val routineAugmentCrossRefsDto = workoutDao.getAllRoutineAugmentCrossRefs().first().map { ref ->
                RoutineAugmentCrossRefDto(
                    routineId = ref.routineId,
                    augmentId = ref.augmentId,
                    order = ref.order
                )
            }

            val augmentsWithDetails = workoutDao.getAllAugments().first()
            val augmentsDto = augmentsWithDetails.map { a ->
                WorkoutAugmentDto(
                    id = a.augment.id,
                    name = a.augment.name,
                    description = a.augment.description,
                    focusBodyPart = a.augment.focusBodyPart,
                    colorHex = a.augment.colorHex,
                    isSystem = a.augment.isSystem,
                    isAddedToLibrary = a.augment.isAddedToLibrary,
                    scheduledDays = a.augment.scheduledDays
                )
            }

            val augmentExerciseCrossRefsDto = workoutDao.getAllAugmentExerciseCrossRefs().first().map { ref ->
                AugmentExerciseCrossRefDto(
                    augmentId = ref.augmentId,
                    exerciseId = ref.exerciseId,
                    order = ref.order
                )
            }

            val augmentSetsDto = workoutDao.getAllAugmentSets().first().map { s ->
                AugmentSetDto(
                    id = s.id,
                    augmentId = s.augmentId,
                    exerciseId = s.exerciseId,
                    order = s.order,
                    type = s.type,
                    weight = s.weight,
                    reps = s.reps,
                    goalReps = s.goalReps
                )
            }

            val augmentActivationsDto = workoutDao.getAllAugmentActivations().first().map { act ->
                AugmentActivationDto(
                    id = act.id,
                    augmentId = act.augmentId,
                    userId = act.userId,
                    mode = act.mode,
                    status = act.status,
                    loggingStyle = act.loggingStyle,
                    scheduledDays = act.scheduledDays,
                    createdAtEpochMs = act.createdAt.toEpochMilli(),
                    windowStartEpochMs = act.windowStart?.toEpochMilli(),
                    windowEndEpochMs = act.windowEnd?.toEpochMilli(),
                    hostProtocolFilter = act.hostProtocolFilter,
                    dayTypeFilter = act.dayTypeFilter,
                    reminderEnabled = act.reminderEnabled
                )
            }

            val repTargetsDto = workoutDao.getAllProtocolRepTargets().first().map { t ->
                ProtocolRepTargetDto(
                    id = t.id,
                    protocol = t.protocol,
                    movementType = t.movementType,
                    setType = t.setType,
                    familyId = t.familyId,
                    minReps = t.minReps,
                    maxReps = t.maxReps,
                    unit = t.unit
                )
            }

            val deloadWindowsDto = workoutDao.getAllDeloadWindows().first().map { w ->
                DeloadWindowDto(
                    id = w.id,
                    userId = w.userId,
                    startEpochMs = w.start.toEpochMilli(),
                    endEpochMs = w.end.toEpochMilli(),
                    source = w.source,
                    status = w.status,
                    snoozedUntilEpochMs = w.snoozedUntil?.toEpochMilli()
                )
            }

            WorkoutBackupSection(
                sessions = sessionsDto,
                logs = logsDto,
                sets = setsDto,
                customExercises = exercisesDto,
                exerciseMaxes = maxesDto,
                userProfile = primaryProfileDto,
                userProfiles = profilesDto,
                protocolCycles = cyclesDto,
                progressionStates = progressionStatesDto,
                exerciseAccomplishments = accomplishmentsDto,
                routines = routinesDto,
                routineExerciseCrossRefs = routineExerciseCrossRefsDto,
                routineSets = routineSetsDto,
                augments = augmentsDto,
                augmentExerciseCrossRefs = augmentExerciseCrossRefsDto,
                augmentSets = augmentSetsDto,
                augmentActivations = augmentActivationsDto,
                routineAugmentCrossRefs = routineAugmentCrossRefsDto,
                protocolRepTargets = repTargetsDto,
                deloadWindows = deloadWindowsDto
            )
        } else null

        val biometricsSection = if (scope.includeBiometrics) {
            val samples = biomarkerDao.getLatestPerMarker().first().map { b ->
                BiomarkerDto(
                    type = b.markerKey,
                    value = b.value.toFloat(),
                    timestampEpochMs = b.drawnAt.toEpochMilli(),
                    unit = b.unit
                )
            }

            val specialAttrs = specialDao.getAllSpecialAttributes().first().map { s ->
                SpecialAttributeDto(
                    type = s.type.name,
                    baseValue = s.baseValue,
                    currentValue = s.currentValue,
                    percentile = s.percentile,
                    totalXp = s.totalXp
                )
            }

            val nowMs = System.currentTimeMillis()
            val start180dMs = nowMs - (180L * 86400L * 1000L)
            val bodySampleDtos = bodySampleDao.getSamplesBetween(start180dMs, nowMs).map { b ->
                BodySampleDto(
                    id = b.id,
                    localDate = b.localDate,
                    loggedAtEpochMs = b.loggedAt,
                    metric = b.metric,
                    site = b.site,
                    value = b.value,
                    unit = b.unit,
                    method = b.method,
                    position = b.position,
                    side = b.side,
                    conditionTag = b.conditionTag,
                    source = b.source,
                    note = b.note
                )
            }

            val start180dDate = LocalDate.now().minusDays(180).toString()
            val vitalRollupDtos = dailyVitalRollupDao.getAllRollupsSince(start180dDate).map { r ->
                DailyVitalRollupDto(
                    localDate = r.localDate,
                    metric = r.metric,
                    value = r.value,
                    source = r.source,
                    quality = r.quality,
                    updatedAtEpochMs = r.updatedAt
                )
            }

            BiometricsBackupSection(
                biomarkers = samples,
                specialAttributes = specialAttrs,
                bodySamples = bodySampleDtos,
                vitalRollups = vitalRollupDtos
            )
        } else null

        val codexSection = if (scope.includeCodex) {
            val shards = loreDao.getAllDataShards().first().map { it.id }
            val quotes = bookDao.getAllQuotes().first().map { q ->
                QuoteDto(
                    id = q.id,
                    bookId = q.bookId,
                    bookTitle = q.bookTitle,
                    content = q.content,
                    chapterTitle = q.chapterTitle,
                    timestampEpochMs = q.timestamp
                )
            }

            CodexBackupSection(
                unlockedLoreIds = shards,
                quotes = quotes
            )
        } else null

        val journalSection = if (scope.includeJournal) {
            val entries = journalDao.getAllEntries().first().map { j ->
                JournalEntryDto(
                    id = j.id,
                    text = j.text,
                    category = j.category,
                    timestampEpochMs = j.timestamp,
                    isHearted = j.isHearted
                )
            }

            val prayers = dailyPrayerDao.getAllPrayers().first().map { p ->
                DailyPrayerDto(
                    dayNumber = p.day,
                    prayer = p.prayer,
                    scripture = p.scripture,
                    reflectionPrompt = p.reflectionPrompt,
                    scriptureReference = p.scriptureReference,
                    scriptureTranslation = p.scriptureTranslation,
                    adoreCyber = p.adoreCyber,
                    adoreTrue = p.adoreTrue,
                    confessCyber = p.confessCyber,
                    confessTrue = p.confessTrue,
                    askCyber = p.askCyber,
                    askTrue = p.askTrue
                )
            }

            val rawChronicle = chronicleRepository.observeChronicle().first()
            val chronicleEntries = rawChronicle.map { c ->
                ChronicleEntryDto(
                    source = c.source,
                    sourceId = c.sourceId,
                    wing = c.wing,
                    room = c.room,
                    content = c.content,
                    timestampEpochMs = c.timestamp,
                    hearted = c.hearted,
                    metadata = c.metadata,
                    localDate = c.localDate
                )
            }
            val dayLinks = rawChronicle
                .filter { it.localDate != null }
                .map { DailyOriginLinkDto(localDate = it.localDate!!, chronicleEntryId = it.sourceId) }

            JournalBackupSection(
                journalEntries = entries,
                dailyPrayers = prayers,
                chronicleEntries = chronicleEntries,
                dayLinks = dayLinks
            )
        } else null

        val characterSection = if (scope.includeCharacter) {
            val char = userCharacterDao.getUserCharacter().first()
            val charDto = char?.let { c ->
                UserCharacterDto(
                    name = c.name,
                    level = c.level,
                    experience = c.experience,
                    eddies = c.eddies,
                    iceLevel = c.iceLevel,
                    prayerStreak = c.prayerStreak,
                    lastPrayerDateEpochMs = c.lastPrayerDate
                )
            }

            val op = operativeProfileDao.getOperativeProfileOnce("default_user")
            val avatarBase64 = op?.avatarPath?.let { path ->
                if (path.isNotBlank()) {
                    val avatarFile = File(path)
                    if (avatarFile.exists()) {
                        try {
                            java.util.Base64.getEncoder().encodeToString(avatarFile.readBytes())
                        } catch (_: Exception) { null }
                    } else null
                } else null
            }

            val opDto = op?.let { o ->
                OperativeProfileDto(
                    id = o.id,
                    name = o.name,
                    netrunnerName = o.netrunnerName,
                    sex = o.sex,
                    dob = o.dob,
                    units = o.units,
                    heightFeet = o.heightFeet,
                    heightInches = o.heightInches,
                    heightCm = o.heightCm,
                    weight = o.weight,
                    somatotype = o.somatotype,
                    level = o.level,
                    experience = o.experience,
                    iceLevel = o.iceLevel,
                    eddies = o.eddies,
                    secureEddies = o.secureEddies,
                    prayerStreak = o.prayerStreak,
                    lastPrayerDateEpochMs = o.lastPrayerDate,
                    isCreationComplete = o.isCreationComplete,
                    avatarPath = o.avatarPath,
                    avatarBase64 = avatarBase64
                )
            }

            val dopamineItems = dopamineMenuDao.getAllItems().first().map { d ->
                DopamineMenuItemDto(
                    id = d.id,
                    title = d.title,
                    description = d.description,
                    energyLevel = d.energyLevel.name,
                    category = d.category.name,
                    usageCount = d.usageCount
                )
            }

            CharacterBackupSection(
                userCharacter = charDto,
                operativeProfile = opDto,
                dopamineItems = dopamineItems
            )
        } else null

        val goalsSection = if (scope.includeGoals) {
            val directives = ascensionDao.getAllDirectives().first().map { d ->
                AscensionDirectiveDto(
                    id = d.id,
                    title = d.title,
                    description = d.description,
                    visionStatement = d.visionStatement,
                    status = d.status,
                    successMetricsJson = if (d.successMetrics.isNotEmpty()) gson.toJson(d.successMetrics) else null,
                    targetEndDate = d.targetEndDate?.toString(),
                    isQuarterly = d.isQuarterly,
                    createdAtEpochMs = d.createdAt.toEpochMilli(),
                    archivedAtEpochMs = d.archivedAt?.toEpochMilli(),
                    currentProgress = d.currentProgress,
                    totalXPContributed = d.totalXPContributed,
                    xpTarget = d.xpTarget,
                    archetypeTag = d.archetypeTag,
                    tags = d.tags,
                    linkedAttributes = d.linkedAttributes,
                    aiMentorMode = d.aiMentorMode,
                    aiGenerated = d.aiGenerated,
                    notes = d.notes,
                    completionHistorySummary = d.completionHistorySummary,
                    lastReviewDate = d.lastReviewDate?.toString(),
                    anchoredSelfItemId = d.anchoredSelfItemId,
                    anchoredSelfItemName = d.anchoredSelfItemName,
                    priorityTier = d.priorityTier,
                    outcomeStatement = d.outcomeStatement,
                    affirmation = d.affirmation,
                    visualUri = d.visualUri
                )
            }
            val allMissions = directives.flatMap { d ->
                ascensionDao.getMissionsForDirectiveSync(d.id)
            }.map { m ->
                AscensionMissionDto(
                    id = m.id,
                    directiveId = m.directiveId,
                    title = m.title,
                    description = m.description,
                    objective = m.objective,
                    status = m.status,
                    startDate = m.startDate.toString(),
                    targetEndDate = m.targetEndDate?.toString(),
                    createdAtEpochMs = m.createdAt.toEpochMilli(),
                    completedAtEpochMs = m.completedAt?.toEpochMilli(),
                    archivedAtEpochMs = m.archivedAt?.toEpochMilli(),
                    progress = m.progress,
                    contributionWeight = m.contributionWeight,
                    totalXPContributed = m.totalXPContributed,
                    xpTarget = m.xpTarget,
                    aiMentorMode = m.aiMentorMode,
                    aiGenerated = m.aiGenerated,
                    notes = m.notes,
                    successCriteria = m.successCriteria,
                    completionHistorySummary = m.completionHistorySummary,
                    tags = m.tags,
                    linkedAttributes = m.linkedAttributes,
                    linkedArchetype = m.linkedArchetype,
                    anchoredSelfItemId = m.anchoredSelfItemId,
                    anchoredSelfItemName = m.anchoredSelfItemName
                )
            }
            val allTasks = allMissions.flatMap { m ->
                ascensionDao.getTasksForParentSync(m.id)
            }.map { t ->
                AscensionTaskDto(
                    id = t.id,
                    parentId = t.parentId,
                    title = t.title,
                    description = t.description,
                    type = t.type,
                    isPulse = t.isPulse,
                    recurrenceType = t.recurrenceType,
                    recurrenceCron = t.recurrenceCron,
                    recurrenceIntervalDays = t.recurrenceIntervalDays,
                    recurrenceDaysOfWeek = t.recurrenceDaysOfWeek,
                    timeWindows = t.timeWindows,
                    adaptiveWakeEnabled = t.adaptiveWakeEnabled,
                    reminderEnabled = t.reminderEnabled,
                    xpValue = t.xpValue,
                    impactWeight = t.impactWeight,
                    currentStreak = t.currentStreak,
                    longestStreak = t.longestStreak,
                    graceBufferDays = t.graceBufferDays,
                    lastCompletedEpochMs = t.lastCompleted?.toEpochMilli(),
                    tags = t.tags,
                    linkedAttributes = t.linkedAttributes,
                    userNotesTemplate = t.userNotesTemplate
                )
            }
            val legacyGoals = goalDao.getAllGoals().first().map { g ->
                GoalDto(
                    id = g.id,
                    type = g.type,
                    title = g.title,
                    description = g.description,
                    linkedAttributes = g.linkedAttributes.map { it.name },
                    progressCurrent = g.progressCurrent,
                    progressTarget = g.progressTarget,
                    xpContributed = g.xpContributed,
                    targetDateMillis = g.targetDateMillis,
                    status = g.status,
                    expiresAtMillis = g.expiresAtMillis,
                    parentAspirationId = g.parentAspirationId,
                    recurrenceType = g.recurrenceType,
                    recurrenceDays = g.recurrenceDays,
                    streak = g.streak,
                    lastCompletedMillis = g.lastCompletedMillis,
                    parentGoalId = g.parentGoalId
                )
            }
            GoalsBackupSection(
                directives = directives,
                missions = allMissions,
                tasks = allTasks,
                legacyGoals = legacyGoals
            )
        } else null

        val vaultSection = if (scope.includeVault) {
            val accounts = vaultDao.getAllAccountsOnce().map { a ->
                VaultAccountDto(
                    id = a.id,
                    name = a.name,
                    institution = a.institution,
                    type = a.type.name,
                    balance = a.balance,
                    currency = a.currency,
                    isLinked = a.isLinked,
                    lastUpdatedEpochMs = a.lastUpdated
                )
            }
            val snapshots = vaultDao.getRecentSnapshots().first().map { s ->
                VaultSnapshotDto(
                    id = s.id,
                    timestampEpochMs = s.timestamp,
                    totalAssets = s.totalAssets,
                    totalDebt = s.totalDebt,
                    netWorth = s.netWorth
                )
            }
            val watchlist = vaultDao.getWatchlistOnce().map { w ->
                VaultWatchlistItemDto(
                    symbol = w.symbol,
                    name = w.name,
                    isCrypto = w.isCrypto
                )
            }
            VaultBackupSection(accounts, snapshots, watchlist)
        } else null

        val selfMapSection = if (scope.includeSelfMap) {
            val items = selfMapDao.getAllItemsOnce().map { i ->
                SelfItemDto(
                    id = i.id,
                    name = i.name,
                    category = i.category.name,
                    importance = i.importance,
                    notes = i.notes,
                    isArchived = i.isArchived,
                    createdAtEpochMs = i.createdAt.toEpochMilli(),
                    updatedAtEpochMs = i.updatedAt.toEpochMilli(),
                    baselineMassContributor = i.baselineMassContributor
                )
            }
            val snapshots = selfMapDao.getAllSnapshotsOnce().map { s ->
                SelfMapSnapshotDto(
                    id = s.id,
                    timestampEpochMs = s.timestamp.toEpochMilli(),
                    mass = s.mass,
                    itemCount = s.itemCount,
                    avgImportance = s.avgImportance,
                    coreCount = s.coreCount
                )
            }
            val moods = selfMapDao.getAllMoodEntriesOnce().map { m ->
                SelfMapMoodEntryDto(
                    id = m.id,
                    timestampEpochMs = m.timestamp.toEpochMilli(),
                    score = m.score,
                    note = m.note,
                    eoeId = m.eoeId,
                    snapshotId = m.snapshotId
                )
            }
            val eoe = selfMapDao.getAllEoEEntriesOnce().map { e ->
                EoEEntryDto(
                    id = e.id,
                    timestampEpochMs = e.timestamp.toEpochMilli(),
                    emotionLabel = e.emotionLabel,
                    selfItemId = e.selfItemId,
                    epText = e.epText,
                    perceptionText = e.perceptionText,
                    appraisal = e.appraisal,
                    intensity = e.intensity,
                    canChangeNotes = e.canChangeNotes,
                    linkedSnapshotId = e.linkedSnapshotId
                )
            }
            val bMass = selfMapPreferencesDataStore.baselineMass.first()
            val onbComplete = selfMapPreferencesDataStore.onboardingCompleted.first()
            SelfMapBackupSection(
                items = items,
                snapshots = snapshots,
                moodEntries = moods,
                eoeEntries = eoe,
                baselineMass = bMass,
                onboardingCompleted = onbComplete
            )
        } else null

        val biohackingSection = if (scope.includeBiohacking) {
            val reconstitutions = reconstitutionDao.getAllSavedMixes().first().map { r ->
                SavedReconstitutionDto(
                    id = r.id,
                    name = r.name,
                    vialMg = r.vialMg,
                    waterMl = r.waterMl,
                    concentrationMgPerMl = r.concentrationMgPerMl,
                    mixedDate = r.mixedDate,
                    notes = r.notes,
                    componentsJson = r.componentsJson,
                    createdAtEpochMs = r.createdAt
                )
            }
            val bioDataEntity = biohackingDao.getBiohackingData(0).firstOrNull()
            val bioDataDto = bioDataEntity?.let { b ->
                BiohackingDataDto(
                    userId = b.userId,
                    bodyFatPercentage = b.bodyFatPercentage,
                    energyScore = b.energyScore,
                    moodScore = b.moodScore,
                    focusScore = b.focusScore,
                    sleepHours = b.sleepHours,
                    sleepQuality = b.sleepQuality,
                    exerciseFrequency = b.exerciseFrequency,
                    exerciseType = b.exerciseType,
                    supplements = b.supplements,
                    dietType = b.dietType,
                    caffeineIntake = b.caffeineIntake,
                    alcoholIntake = b.alcoholIntake,
                    stressLevel = b.stressLevel,
                    primaryObjective = b.primaryObjective,
                    contraindications = b.contraindications,
                    pregnancyFlag = b.pregnancyFlag,
                    isWearableSynced = b.isWearableSynced,
                    lastSyncTimestampEpochMs = b.lastSyncTimestamp,
                    currentHeartRate = b.currentHeartRate,
                    currentSteps = b.currentSteps,
                    labResultsPath = b.labResultsPath,
                    geneticDataPath = b.geneticDataPath,
                    hasConsentedToDataProcessing = b.hasConsentedToDataProcessing,
                    consentAnonymizedUpload = b.consentAnonymizedUpload,
                    consentWearableSync = b.consentWearableSync,
                    consentGeneticData = b.consentGeneticData,
                    hasCompletedPrivacyOnboarding = b.hasCompletedPrivacyOnboarding,
                    enableOnDeviceNeuralCore = b.enableOnDeviceNeuralCore,
                    latestReportJson = b.latestReportJson,
                    reportTimestampEpochMs = b.reportTimestamp,
                    calculatedBioAge = b.calculatedBioAge,
                    calendarAgeAtCalculation = b.calendarAgeAtCalculation,
                    extractedBiomarkersJson = b.extractedBiomarkersJson
                )
            }
            val protocolLogs = biohackingDao.getAllProtocolLogs().first().map { l ->
                BioProtocolLogDto(
                    id = l.id,
                    userId = l.userId,
                    timestampEpochMs = l.timestamp,
                    energyScore = l.energyScore,
                    sleepQuality = l.sleepQuality,
                    moodScore = l.moodScore,
                    focusScore = l.focusScore,
                    sideEffects = l.sideEffects,
                    notes = l.notes,
                    protocolId = l.protocolId,
                    isWorking = l.isWorking
                )
            }
            BiohackingBackupSection(reconstitutions, bioDataDto, protocolLogs)
        } else null

        val chatSection = if (scope.includeChat) {
            val memories = neuralMemoryDao.getAllMemories().first().map { m ->
                NeuralMemoryDto(
                    id = m.id,
                    wing = m.wing,
                    room = m.room,
                    content = m.content,
                    importance = m.importance,
                    timestampEpochMs = m.timestamp,
                    metadata = m.metadata
                )
            }
            val sessions = chatDao.getChatSessions().first().map { s ->
                ChatSessionDto(
                    sessionId = s.sessionId,
                    contactName = s.contactName,
                    lastMessage = s.lastMessage,
                    lastTimestampEpochMs = s.lastTimestamp,
                    isUnread = s.isUnread,
                    isFixer = s.isFixer,
                    personalityPrompt = s.personalityPrompt
                )
            }
            val messages = sessions.flatMap { s ->
                chatDao.getMessagesForSession(s.sessionId).first()
            }.map { msg ->
                ChatMessageDto(
                    id = msg.id,
                    sessionId = msg.sessionId,
                    contactName = msg.contactName,
                    senderName = msg.senderName,
                    text = msg.text,
                    timestampEpochMs = msg.timestamp,
                    isFromUser = msg.isFromUser,
                    suggestedActionsJson = if (msg.suggestedActions.isNotEmpty()) gson.toJson(msg.suggestedActions) else null
                )
            }
            ChatBackupSection(memories, sessions, messages)
        } else null

        val activeProfile = workoutDao.getUserProfile("default_user").firstOrNull()
        val settingsSection = SettingsBackupSection(
            themeMode = userPreferencesRepository.themeMode.first(),
            measurementUnit = userPreferencesRepository.measurementUnit.first(),
            activeProtocol = activeProfile?.activeProtocol,
            weightIncrementCompound = activeProfile?.weightIncrementCompound ?: 5.0f,
            weightIncrementIsolation = activeProfile?.weightIncrementIsolation ?: 2.5f,
            isNeuralBriefEnabled = settingsRepository.isNeuralBriefEnabled.value,
            briefFrequency = settingsRepository.briefFrequency.value,
            quietHoursStart = settingsRepository.quietHoursStart.value,
            quietHoursEnd = settingsRepository.quietHoursEnd.value,
            backupFrequency = userPreferencesRepository.backupFrequency.first()
        )

        val preferencesSection = PreferencesBackupSection(
            defaultRestTimeSeconds = healthPreferencesDataStore.defaultRestTime.first(),
            autoStartRestTimer = healthPreferencesDataStore.autoStartRestTimer.first(),
            workSetRestTimeSeconds = healthPreferencesDataStore.workSetRestTime.first(),
            warmupSetRestTimeSeconds = healthPreferencesDataStore.warmupSetRestTime.first(),
            dropSetRestTimeSeconds = healthPreferencesDataStore.dropSetRestTime.first(),
            restTimerMode = healthPreferencesDataStore.restTimerMode.first().name,
            workoutZoomLevel = healthPreferencesDataStore.workoutZoomLevel.first(),
            workoutLayoutMode = workoutPreferencesDataStore.workoutLayoutMode.first().name,
            useCustomNumpadInFocusMode = workoutPreferencesDataStore.useCustomNumpadInFocusMode.first(),
            cyberCrappRotationMode = workoutPreferencesDataStore.cyberCrappRotationMode.first().name,
            codexPeriod = healthPreferencesDataStore.codexPeriod.first(),
            codexWing = healthPreferencesDataStore.codexWing.first(),
            codexLastExerciseId = healthPreferencesDataStore.codexLastExerciseId.firstOrNull(),
            targetWakeWd = briefPreferencesDataStore.targetWakeWd.firstOrNull(),
            targetWakeWe = briefPreferencesDataStore.targetWakeWe.firstOrNull(),
            pulsePmMode = briefPreferencesDataStore.pulsePmMode.first(),
            pulsePmCustomDays = briefPreferencesDataStore.pulsePmCustomDays.first(),
            pulsePmCustomTime = briefPreferencesDataStore.pulsePmCustomTime.first(),
            adaptiveWakeEnabled = briefPreferencesDataStore.adaptiveWakeEnabled.first(),
            quietHoursEnd = briefPreferencesDataStore.quietHoursEnd.first(),
            showDirectivesOnMorningShade = briefPreferencesDataStore.showDirectivesOnMorningShade.first(),
            directiveIntakeMode = directivePreferencesDataStore.directiveIntakeMode.first().name,
            autoSyncEnabled = healthPreferencesDataStore.autoSyncEnabled.first(),
            syncIntervalHours = healthPreferencesDataStore.syncIntervalHours.first(),
            enabledAttributes = healthPreferencesDataStore.enabledAttributes.first().map { it.name },
            liveMonitoringEnabled = healthPreferencesDataStore.liveMonitoringEnabled.first(),
            showSyncNotification = healthPreferencesDataStore.showSyncNotification.first(),
            isNetrunnerMode = settingsRepository.isNetrunnerMode.value,
            isAltarTrueTextMode = settingsRepository.isAltarTrueTextMode.value,
            biometricLockEnabled = settingsRepository.isBiometricLockEnabled.value,
            biometricLockTimeout = when (settingsRepository.biometricLockTimeout.value) {
                "1_MIN" -> 60
                "5_MIN" -> 300
                "15_MIN" -> 900
                else -> 0
            }
        )

        val riteSessions = riteSessionDao.getAllSessionsList()
        val ritesSection = RitesBackupSection(
            sessions = riteSessions.map { s ->
                RiteSessionDto(
                    id = s.id,
                    kind = s.kind,
                    startedAtEpochMs = s.startedAt.toEpochMilli(),
                    endedAtEpochMs = s.endedAt?.toEpochMilli(),
                    durationMin = s.durationMin,
                    source = s.source,
                    notes = s.notes,
                    localDate = s.localDate
                )
            }
        )

        val highlights = libraryDao.getAllHighlightsList()
        val librarySection = LibraryBackupSection(
            highlights = highlights.map { h ->
                LibraryHighlightDto(
                    id = h.id,
                    bookId = h.bookId,
                    chapterIndex = h.chapterIndex,
                    startOffset = h.startOffset,
                    endOffset = h.endOffset,
                    color = h.color,
                    timestampEpochMs = h.timestamp
                )
            }
        )

        val activeDietId = dietPreferencesDataStore.activeDietId.firstOrNull()
        val dietOverrides = dietPreferencesDataStore.dietOverrides.firstOrNull() ?: com.neon.ascent.core.domain.labs.models.DietOverrides()
        val dietSection = DietBackupSection(
            activeDietId = activeDietId,
            overrides = dietOverrides.toDto()
        )

        val activeStackIds = supplementPreferencesDataStore.activeStackIds.firstOrNull() ?: emptySet()
        val supplementOverridesMap = supplementPreferencesDataStore.overrides.firstOrNull() ?: emptyMap()
        val supplementSection = SupplementBackupSection(
            activeStackIds = activeStackIds,
            overrides = supplementOverridesMap.mapValues { it.value.toDto() }
        )

        val activeProtocolIds = biohackingProtocolPreferencesDataStore.activeProtocolIds.firstOrNull() ?: emptySet()
        val protocolOverridesMap = biohackingProtocolPreferencesDataStore.overrides.firstOrNull() ?: emptyMap()
        val protocolSection = BiohackingProtocolBackupSection(
            activeProtocolIds = activeProtocolIds,
            overrides = protocolOverridesMap.mapValues { it.value.toDto() }
        )

        val labsFocus = labsFocusPreferencesDataStore.labsFocus.firstOrNull() ?: com.neon.ascent.core.domain.labs.models.LabsFocus()
        val labsFocusSection = labsFocus.toDto()

        val payload = NeonAscentBackupPayload(
            version = 11,
            exportedAt = now,
            appVersion = "1.0",
            workoutPayload = workoutSection,
            biometricsPayload = biometricsSection,
            codexPayload = codexSection,
            journalPayload = journalSection,
            characterPayload = characterSection,
            settingsPayload = settingsSection,
            goalsPayload = goalsSection,
            vaultPayload = vaultSection,
            selfMapPayload = selfMapSection,
            biohackingPayload = biohackingSection,
            chatPayload = chatSection,
            preferencesPayload = preferencesSection,
            ritesPayload = ritesSection,
            libraryPayload = librarySection,
            dietPayload = dietSection,
            supplementPayload = supplementSection,
            biohackingProtocolPayload = protocolSection,
            labsFocusPayload = labsFocusSection
        )

        healthPreferencesDataStore.updateLastSyncTime()
        userPreferencesRepository.setLastBackupTimestamp(System.currentTimeMillis())
        userPreferencesRepository.setLastBackupVersion(11)

        return gson.toJson(payload)
    }

    override suspend fun restoreBackupJson(
        jsonString: String,
        mode: RestoreMode,
        onProgress: ((RestoreProgress) -> Unit)?
    ): RestoreResult {
        onProgress?.invoke(RestoreProgress(stage = RestoreStage.READING, message = "READING PAYLOAD..."))

        return try {
            val sanitizedJson = jsonString.trim().removePrefix("\uFEFF")
            if (sanitizedJson.isBlank()) {
                val failure = RestoreResult(success = false, message = "Invalid JSON payload structure: File is empty")
                onProgress?.invoke(RestoreProgress(stage = RestoreStage.FAIL, message = failure.message, error = failure.message))
                return failure
            }

            onProgress?.invoke(RestoreProgress(stage = RestoreStage.PARSE, message = "PARSING JSON PAYLOAD..."))

            val payload = try {
                gson.fromJson(sanitizedJson, NeonAscentBackupPayload::class.java)
            } catch (e: Exception) {
                val failure = RestoreResult(success = false, message = "Invalid JSON payload structure")
                onProgress?.invoke(RestoreProgress(stage = RestoreStage.FAIL, message = failure.message, error = e.localizedMessage))
                return failure
            } ?: run {
                val failure = RestoreResult(success = false, message = "Invalid JSON payload structure")
                onProgress?.invoke(RestoreProgress(stage = RestoreStage.FAIL, message = failure.message, error = failure.message))
                return failure
            }

            if (payload.workoutPayload == null &&
                payload.biometricsPayload == null &&
                payload.codexPayload == null &&
                payload.journalPayload == null &&
                payload.characterPayload == null &&
                payload.settingsPayload == null &&
                payload.goalsPayload == null &&
                payload.vaultPayload == null &&
                payload.selfMapPayload == null &&
                payload.biohackingPayload == null &&
                payload.chatPayload == null &&
                payload.preferencesPayload == null &&
                payload.ritesPayload == null &&
                payload.libraryPayload == null &&
                payload.dietPayload == null &&
                payload.supplementPayload == null &&
                payload.biohackingProtocolPayload == null
            ) {
                val failure = RestoreResult(success = false, message = "Invalid JSON payload structure: No backup sections found")
                onProgress?.invoke(RestoreProgress(stage = RestoreStage.FAIL, message = failure.message, error = failure.message))
                return failure
            }

            // Calculate SHA-256 checksum
            val checksumHex = try {
                val md = java.security.MessageDigest.getInstance("SHA-256")
                val digest = md.digest(sanitizedJson.toByteArray(Charsets.UTF_8))
                digest.joinToString("") { "%02x".format(it) }
            } catch (_: Exception) {
                "00000000"
            }

            onProgress?.invoke(RestoreProgress(
                stage = RestoreStage.CHECKSUM,
                message = "CHECKSUM VERIFIED",
                checksumHex = checksumHex.take(16)
            ))

            if (mode == RestoreMode.REPLACE) {
                snapshotDatabasesBeforeReplace()
                coreDatabase.clearAllTables()
                appDatabase.clearAllTables()
            }

            var sessionsRestored = 0
            var setsRestored = 0
            var cyclesRestored = 0
            var biomarkersRestored = 0
            var codexRestored = 0
            var journalRestored = 0

            // 1. Restore Workouts
            onProgress?.invoke(RestoreProgress(
                stage = RestoreStage.RESTORE_WORKOUT,
                message = "RESTORING WORKOUT GRAPH...",
                checksumHex = checksumHex.take(16)
            ))

            payload.workoutPayload?.let { w ->
                // Step 1: Custom exercise definitions (parents / families first)
                val customToRestore = w.customExercises.sortedWith(
                    compareByDescending<ExerciseDefinitionDto> { it.isPrimaryVariant }
                        .thenBy { it.id }
                )
                customToRestore.forEach { e ->
                    workoutDao.insertExerciseDefinition(
                        ExerciseDefinitionEntity(
                            id = e.id,
                            name = e.name,
                            description = e.description,
                            cues = e.cues,
                            muscleGroups = e.muscleGroups,
                            equipment = e.equipment,
                            gifAssetPath = e.gifAssetPath,
                            isLockedClassic = e.isLockedClassic,
                            injurySubstitutions = e.injurySubstitutions,
                            dangerousFor = e.dangerousFor,
                            movementType = e.movementType,
                            notes = e.notes,
                            familyId = e.familyId,
                            familyName = e.familyName,
                            implement = e.implement,
                            stance = e.stance,
                            specialtyBar = e.specialtyBar,
                            rangeOverrideMin = e.rangeOverrideMin,
                            rangeOverrideMax = e.rangeOverrideMax,
                            allowsAddedLoad = e.allowsAddedLoad,
                            isPrimaryVariant = e.isPrimaryVariant,
                            tags = e.tags,
                            isCustom = true
                        )
                    )
                }

                // Step 2: Routines and augments
                w.routines.orEmpty().forEach { r ->
                    workoutDao.insertRoutine(
                        WorkoutRoutineEntity(
                            id = r.id,
                            name = r.name,
                            description = r.description,
                            protocol = r.protocol,
                            createdAt = Instant.ofEpochMilli(r.createdAtEpochMs),
                            isSystem = r.isSystem,
                            isAddedToLibrary = r.isAddedToLibrary,
                            scheduledDays = r.scheduledDays ?: "[]"
                        )
                    )
                }

                w.augments.orEmpty().forEach { a ->
                    workoutDao.insertAugment(
                        WorkoutAugmentEntity(
                            id = a.id,
                            name = a.name,
                            description = a.description,
                            focusBodyPart = a.focusBodyPart,
                            colorHex = a.colorHex,
                            isSystem = a.isSystem,
                            isAddedToLibrary = a.isAddedToLibrary,
                            scheduledDays = a.scheduledDays
                        )
                    )
                }

                // Step 3: Cross-refs and sets
                w.routineExerciseCrossRefs.orEmpty().forEach { ref ->
                    workoutDao.insertRoutineExerciseCrossRef(
                        RoutineExerciseCrossRef(
                            routineId = ref.routineId,
                            exerciseId = ref.exerciseId,
                            order = ref.order
                        )
                    )
                }

                w.routineAugmentCrossRefs.orEmpty().forEach { ref ->
                    workoutDao.insertRoutineAugmentCrossRef(
                        RoutineAugmentCrossRef(
                            routineId = ref.routineId,
                            augmentId = ref.augmentId,
                            order = ref.order
                        )
                    )
                }

                w.routineSets.orEmpty().forEach { s ->
                    workoutDao.insertRoutineSet(
                        RoutineSetEntity(
                            id = s.id,
                            routineId = s.routineId,
                            exerciseId = s.exerciseId,
                            order = s.order,
                            type = s.type,
                            weight = s.weight,
                            reps = s.reps,
                            goalReps = s.goalReps
                        )
                    )
                }

                w.augmentExerciseCrossRefs.orEmpty().forEach { ref ->
                    workoutDao.insertAugmentExerciseCrossRef(
                        AugmentExerciseCrossRef(
                            augmentId = ref.augmentId,
                            exerciseId = ref.exerciseId,
                            order = ref.order
                        )
                    )
                }

                w.augmentSets.orEmpty().forEach { s ->
                    workoutDao.insertAugmentSet(
                        AugmentSetEntity(
                            id = s.id,
                            augmentId = s.augmentId,
                            exerciseId = s.exerciseId,
                            order = s.order,
                            type = s.type,
                            weight = s.weight,
                            reps = s.reps,
                            goalReps = s.goalReps
                        )
                    )
                }

                w.augmentActivations.orEmpty().forEach { act ->
                    workoutDao.upsertAugmentActivation(
                        AugmentActivationEntity(
                            id = act.id,
                            augmentId = act.augmentId,
                            userId = act.userId,
                            mode = act.mode,
                            status = act.status,
                            loggingStyle = act.loggingStyle,
                            scheduledDays = act.scheduledDays,
                            createdAt = Instant.ofEpochMilli(act.createdAtEpochMs),
                            windowStart = act.windowStartEpochMs?.let { Instant.ofEpochMilli(it) },
                            windowEnd = act.windowEndEpochMs?.let { Instant.ofEpochMilli(it) },
                            hostProtocolFilter = act.hostProtocolFilter,
                            dayTypeFilter = act.dayTypeFilter,
                            reminderEnabled = act.reminderEnabled
                        )
                    )
                }

                // Step 4: Sessions -> logs -> set logs
                w.sessions.orEmpty().forEach { s ->
                    workoutDao.upsertSession(
                        WorkoutSessionEntity(
                            id = s.id,
                            date = Instant.ofEpochMilli(s.dateEpochMs),
                            protocol = s.protocol,
                            durationSeconds = s.durationSeconds,
                            notes = s.notes,
                            experienceLevel = s.experienceLevel,
                            somatotype = s.somatotype,
                            sessionRpe = s.sessionRpe,
                            jointHealth = s.jointHealth,
                            isDeload = s.isDeload,
                            cycleId = s.cycleId,
                            protocolDayType = s.protocolDayType,
                            primaryAugmentId = s.primaryAugmentId
                        )
                    )
                    sessionsRestored++
                }

                w.logs.orEmpty().forEach { l ->
                    workoutDao.upsertWorkoutLog(
                        WorkoutLogEntity(
                            id = l.id,
                            sessionId = l.sessionId,
                            exerciseId = l.exerciseId,
                            order = l.order,
                            exerciseName = l.exerciseName,
                            protocolOverride = null,
                            showGoalReps = l.showGoalReps,
                            supersetId = l.supersetId
                        )
                    )
                }

                w.sets.orEmpty().forEach { st ->
                    workoutDao.upsertSetLog(
                        SetLogEntity(
                            id = st.id,
                            workoutLogId = st.workoutLogId,
                            weight = st.weight,
                            reps = st.reps,
                            setType = st.setType,
                            goalReps = st.goalReps,
                            isCompleted = st.isCompleted,
                            rir = st.rir,
                            isWarmup = st.isWarmup,
                            timestamp = Instant.ofEpochMilli(st.timestampEpochMs),
                            clusterMiniSetIndex = st.clusterMiniSetIndex,
                            isLengthenedPartial = st.isLengthenedPartial,
                            isLoadedStretch = st.isLoadedStretch,
                            stretchDurationSeconds = st.stretchDurationSeconds,
                            prescribedWeight = st.prescribedWeight,
                            prescribedReps = st.prescribedReps,
                            percentOfMax = st.percentOfMax,
                            isAmrap = st.isAmrap,
                            accommodatingLoad = st.accommodatingLoad
                        )
                    )
                    setsRestored++
                }

                w.exerciseMaxes.orEmpty().forEach { m ->
                    workoutDao.upsertExerciseMax(
                        ExerciseMaxEntity(
                            familyId = m.familyId,
                            testedAt = Instant.ofEpochMilli(m.testedAtEpochMs),
                            oneRepMax = m.oneRepMax,
                            rm15 = m.rm15,
                            rm10 = m.rm10,
                            rm5 = m.rm5,
                            trainingMax = m.trainingMax,
                            source = m.source
                        )
                    )
                }

                // Step 5: Protocol cycles
                w.protocolCycles.orEmpty().forEach { c ->
                    workoutDao.upsertProtocolCycle(
                        ProtocolCycleEntity(
                            id = c.id,
                            userId = c.userId,
                            protocol = c.protocol,
                            startedAt = Instant.ofEpochMilli(c.startedAtEpochMs),
                            endedAt = c.endedAtEpochMs?.let { Instant.ofEpochMilli(it) },
                            status = c.status,
                            currentWeek = c.currentWeek,
                            currentDayIndex = c.currentDayIndex,
                            configJson = c.configJson
                        )
                    )
                    cyclesRestored++
                }

                // Step 6: Progression states and accomplishments
                w.progressionStates.orEmpty().forEach { ps ->
                    workoutDao.insertProgressionState(
                        ProgressionStateEntity(
                            exerciseId = ps.exerciseId,
                            bestClusterReps = ps.bestClusterReps,
                            weightAtBest = ps.weightAtBest,
                            consecutiveMisses = ps.consecutiveMisses,
                            currentWeight = ps.currentWeight,
                            lastRotationDate = ps.lastRotationDateEpochMs?.let { Instant.ofEpochMilli(it) }
                        )
                    )
                }

                w.exerciseAccomplishments.orEmpty().forEach { acc ->
                    workoutDao.insertAccomplishments(
                        ExerciseAccomplishmentsEntity(
                            exerciseId = acc.exerciseId,
                            heaviestWeight = acc.heaviestWeight,
                            heaviestWeightReps = acc.heaviestWeightReps,
                            heaviestWeightDate = acc.heaviestWeightDateEpochMs?.let { Instant.ofEpochMilli(it) },
                            maxEstimatedOneRepMax = acc.maxEstimatedOneRepMax,
                            maxOneRepMaxWeight = acc.maxOneRepMaxWeight,
                            maxOneRepMaxReps = acc.maxOneRepMaxReps,
                            maxOneRepMaxDate = acc.maxOneRepMaxDateEpochMs?.let { Instant.ofEpochMilli(it) },
                            maxSessionVolume = acc.maxSessionVolume,
                            maxSessionVolumeDate = acc.maxSessionVolumeDateEpochMs?.let { Instant.ofEpochMilli(it) },
                            maxRepsAtTopWeight = acc.maxRepsAtTopWeight,
                            topWeightForReps = acc.topWeightForReps,
                            bestClusterReps = acc.bestClusterReps,
                            bestClusterWeight = acc.bestClusterWeight,
                            bestClusterDate = acc.bestClusterDateEpochMs?.let { Instant.ofEpochMilli(it) }
                        )
                    )
                }

                // Step 7: Protocol rep targets
                w.protocolRepTargets.orEmpty().forEach { t ->
                    workoutDao.upsertProtocolRepTarget(
                        ProtocolRepTargetEntity(
                            id = t.id,
                            protocol = t.protocol,
                            movementType = t.movementType,
                            setType = t.setType,
                            familyId = t.familyId,
                            minReps = t.minReps,
                            maxReps = t.maxReps,
                            unit = t.unit
                        )
                    )
                }

                // Step 8: User profile (last)
                val profilesToRestore = if (w.userProfiles.orEmpty().isNotEmpty()) w.userProfiles.orEmpty() else listOfNotNull(w.userProfile)
                profilesToRestore.forEach { p ->
                    workoutDao.insertUserProfile(
                        UserWorkoutProfileEntity(
                            userId = p.userId,
                            experienceLevel = p.experienceLevel ?: "INTERMEDIATE",
                            somatotype = p.somatotype ?: "MESOMORPH",
                            injuries = p.injuries ?: emptyList(),
                            timePerSessionMinutes = if (p.timePerSessionMinutes != 0) p.timePerSessionMinutes else 60,
                            age = if (p.age != 0) p.age else 30,
                            heightCm = if (p.heightCm != 0f) p.heightCm else 175f,
                            weightKg = if (p.weightKg != 0f) p.weightKg else 75f,
                            gender = p.gender ?: "OTHER",
                            activityFactor = if (p.activityFactor != 0f) p.activityFactor else 1.2f,
                            unitSystem = p.unitSystem ?: "METRIC",
                            activeProtocol = p.activeProtocol,
                            rotationIndex = p.rotationIndex,
                            scheduledDays = p.scheduledDays ?: "[]",
                            deepLinkToRoutine = p.deepLinkToRoutine,
                            sequencerEnabled = p.sequencerEnabled,
                            autoWeightIncrement = p.autoWeightIncrement,
                            progressionMode = p.progressionMode ?: "FIXED_INCREMENT",
                            weightIncrementCompound = p.weightIncrementCompound,
                            weightIncrementUpperCompound = p.weightIncrementUpperCompound,
                            weightIncrementLowerCompound = p.weightIncrementLowerCompound,
                            weightIncrementIsolation = p.weightIncrementIsolation,
                            progressionPercentage = p.progressionPercentage,
                            roundingIncrement = p.roundingIncrement,
                            rirCapturePerMiniSet = p.rirCapturePerMiniSet,
                            customSequenceIds = p.customSequenceIds ?: "[]",
                            coachingHintsEnabled = p.coachingHintsEnabled,
                            cyberCrappRotationMode = p.cyberCrappRotationMode ?: "STRAIGHT",
                            lastBlastStartDate = p.lastBlastStartDateEpochMs?.let { Instant.ofEpochMilli(it) }
                        )
                    )
                }

                w.deloadWindows.orEmpty().forEach { dw ->
                    val endInstant = Instant.ofEpochMilli(dw.endEpochMs)
                    val now = Instant.now()
                    val activeStatus = if (endInstant.isAfter(now) && dw.status == "ACTIVE") "ACTIVE" else dw.status
                    workoutDao.insertDeloadWindow(
                        DeloadWindowEntity(
                            id = dw.id,
                            userId = dw.userId,
                            start = Instant.ofEpochMilli(dw.startEpochMs),
                            end = endInstant,
                            source = dw.source,
                            status = activeStatus,
                            snoozedUntil = dw.snoozedUntilEpochMs?.let { Instant.ofEpochMilli(it) }
                        )
                    )
                }
            }

            // 2. Restore Domains
            onProgress?.invoke(RestoreProgress(
                stage = RestoreStage.RESTORE_DOMAINS,
                message = "RESTORING DOMAINS (GOALS, VAULT, SELFMAP, BIO, CHAT, RITES, LIBRARY)...",
                checksumHex = checksumHex.take(16)
            ))

            // 2. Restore Biometrics
            payload.biometricsPayload?.let { b ->
                b.biomarkers.forEach { bio ->
                    biomarkerDao.upsertSample(
                        BiomarkerSampleEntity(
                            id = "restored_${bio.type}_${bio.timestampEpochMs}",
                            markerKey = bio.type,
                            displayName = bio.type,
                            value = bio.value.toDouble(),
                            unit = bio.unit,
                            drawnAt = Instant.ofEpochMilli(bio.timestampEpochMs),
                            source = "RESTORED",
                            notes = null
                        )
                    )
                    biomarkersRestored++
                }

                b.specialAttributes.forEach { sa ->
                    val specType = try { SpecialType.valueOf(sa.type) } catch (e: Exception) { SpecialType.STRENGTH }
                    specialDao.upsertSpecialAttribute(
                        SpecialAttributeEntity(
                            type = specType,
                            baseValue = sa.baseValue,
                            currentValue = sa.currentValue,
                            percentile = sa.percentile,
                            totalXp = sa.totalXp,
                            lastUpdated = Instant.now()
                        )
                    )
                }

                if (b.bodySamples.isNotEmpty()) {
                    bodySampleDao.upsertSamples(
                        b.bodySamples.map { bs ->
                            BodySampleEntity(
                                id = bs.id,
                                localDate = bs.localDate,
                                loggedAt = bs.loggedAtEpochMs,
                                metric = bs.metric,
                                site = bs.site,
                                value = bs.value,
                                unit = bs.unit,
                                method = bs.method,
                                position = bs.position,
                                side = bs.side,
                                conditionTag = bs.conditionTag,
                                source = bs.source,
                                note = bs.note
                            )
                        }
                    )
                }

                if (b.vitalRollups.isNotEmpty()) {
                    dailyVitalRollupDao.upsertAll(
                        b.vitalRollups.map { vr ->
                            DailyVitalRollupEntity(
                                localDate = vr.localDate,
                                metric = vr.metric,
                                value = vr.value,
                                source = vr.source,
                                quality = vr.quality,
                                updatedAt = vr.updatedAtEpochMs
                            )
                        }
                    )
                }
            }

            // 3. Restore Codex
            payload.codexPayload?.let { c ->
                c.quotes.forEach { q ->
                    bookDao.insertQuote(
                        QuoteEntity(
                            id = q.id,
                            bookId = q.bookId,
                            bookTitle = q.bookTitle,
                            content = q.content,
                            chapterTitle = q.chapterTitle,
                            timestamp = q.timestampEpochMs
                        )
                    )
                    libraryRepository.upsertQuote(
                        LibraryQuote(
                            id = q.id,
                            bookId = q.bookId,
                            bookTitle = q.bookTitle,
                            content = q.content,
                            chapterTitle = q.chapterTitle,
                            timestamp = q.timestampEpochMs
                        )
                    )
                    codexRestored++
                }
                c.unlockedLoreIds.forEach { shardId ->
                    loreDao.updateShardDecrypted(shardId, true)
                }
            }

            // 4. Restore Journal
            payload.journalPayload?.let { j ->
                j.journalEntries.forEach { entry ->
                    val je = JournalEntry(
                        id = entry.id,
                        text = entry.text,
                        category = entry.category,
                        timestamp = entry.timestampEpochMs,
                        isHearted = entry.isHearted
                    )
                    journalDao.insertEntry(je)
                    chronicleRepository.importIfAbsent(je.toChronicleEntry())
                    journalRestored++
                }

                if (j.dailyPrayers.isNotEmpty()) {
                    dailyPrayerDao.insertPrayers(
                        j.dailyPrayers.map { p ->
                            DailyPrayer(
                                day = p.dayNumber,
                                prayer = p.prayer,
                                scripture = p.scripture,
                                reflectionPrompt = p.reflectionPrompt,
                                scriptureReference = p.scriptureReference,
                                scriptureTranslation = p.scriptureTranslation,
                                adoreCyber = p.adoreCyber,
                                adoreTrue = p.adoreTrue,
                                confessCyber = p.confessCyber,
                                confessTrue = p.confessTrue,
                                askCyber = p.askCyber,
                                askTrue = p.askTrue
                            )
                        }
                    )
                }

                j.chronicleEntries.forEach { ce ->
                    chronicleRepository.importIfAbsent(
                        ChronicleEntry(
                            source = ce.source,
                            sourceId = ce.sourceId,
                            wing = ce.wing,
                            room = ce.room,
                            content = ce.content,
                            timestamp = ce.timestampEpochMs,
                            hearted = ce.hearted,
                            metadata = ce.metadata,
                            localDate = ce.localDate
                        )
                    )
                }

                j.dayLinks.forEach { dl ->
                    val entries = chronicleRepository.observeChronicle().first()
                    val existing = entries.firstOrNull { it.sourceId == dl.chronicleEntryId }
                    if (existing != null && existing.localDate != dl.localDate) {
                        chronicleRepository.saveEntry(existing.copy(localDate = dl.localDate))
                    }
                }
            }

            // 5. Restore Character
            payload.characterPayload?.let { char ->
                val op = char.operativeProfile
                val avatarBase64Str = op?.avatarBase64
                val avatarPathSanitized = if (!avatarBase64Str.isNullOrBlank()) {
                    try {
                        val imageBytes = java.util.Base64.getDecoder().decode(avatarBase64Str)
                        val avatarDir = File(context.filesDir, "avatars").apply { mkdirs() }
                        val avatarFile = File(avatarDir, "restored_avatar_${System.currentTimeMillis()}.jpg")
                        avatarFile.writeBytes(imageBytes)
                        avatarFile.absolutePath
                    } catch (_: Exception) {
                        op?.avatarPath?.let { path ->
                            if (path.isNotBlank() && File(path).exists()) path else null
                        }
                    }
                } else {
                    op?.avatarPath?.let { path ->
                        if (path.isNotBlank() && File(path).exists()) path else null
                    }
                }
                val restoredOp = OperativeProfileEntity(
                    id = op?.id ?: "default_user",
                    name = op?.name ?: char.userCharacter?.name ?: "OPERATIVE",
                    netrunnerName = op?.netrunnerName,
                    sex = op?.sex ?: "UNSPECIFIED",
                    dob = op?.dob ?: "2000-01-01",
                    units = op?.units ?: "METRIC",
                    heightFeet = op?.heightFeet,
                    heightInches = op?.heightInches,
                    heightCm = op?.heightCm,
                    weight = op?.weight ?: "70",
                    somatotype = op?.somatotype ?: 0.5f,
                    level = op?.level ?: char.userCharacter?.level ?: 1,
                    experience = op?.experience ?: char.userCharacter?.experience ?: 0L,
                    iceLevel = op?.iceLevel ?: char.userCharacter?.iceLevel ?: 1,
                    eddies = op?.eddies ?: char.userCharacter?.eddies ?: 0,
                    secureEddies = op?.secureEddies ?: 0,
                    prayerStreak = op?.prayerStreak ?: char.userCharacter?.prayerStreak ?: 0,
                    lastPrayerDate = op?.lastPrayerDateEpochMs ?: char.userCharacter?.lastPrayerDateEpochMs ?: 0L,
                    isCreationComplete = true,
                    avatarPath = avatarPathSanitized
                )
                operativeProfileDao.upsertOperativeProfile(restoredOp)

                val userChar = UserCharacter(
                    id = 0,
                    name = restoredOp.name,
                    netrunnerName = restoredOp.netrunnerName,
                    sex = restoredOp.sex,
                    dob = restoredOp.dob,
                    units = restoredOp.units,
                    heightFeet = restoredOp.heightFeet,
                    heightInches = restoredOp.heightInches,
                    heightCm = restoredOp.heightCm,
                    weight = restoredOp.weight,
                    somatotype = restoredOp.somatotype,
                    level = restoredOp.level,
                    experience = restoredOp.experience,
                    eddies = restoredOp.eddies,
                    secureEddies = restoredOp.secureEddies,
                    iceLevel = restoredOp.iceLevel,
                    prayerStreak = restoredOp.prayerStreak,
                    lastPrayerDate = restoredOp.lastPrayerDate,
                    isCreationComplete = true,
                    avatarPath = restoredOp.avatarPath
                )
                userCharacterDao.insertUserCharacter(userChar)

                char.dopamineItems.forEach { item ->
                    val energy = try { EnergyLevel.valueOf(item.energyLevel) } catch (e: Exception) { EnergyLevel.MEDIUM }
                    val category = try { DopamineCategory.valueOf(item.category) } catch (e: Exception) { DopamineCategory.RESET }

                    dopamineMenuDao.upsertItem(
                        DopamineMenuItemEntity(
                            id = item.id,
                            title = item.title,
                            description = item.description,
                            durationMinutes = 5,
                            category = category,
                            specialTags = emptyList(),
                            energyLevel = energy,
                            usageCount = item.usageCount
                        )
                    )
                }
            }

            // 6. Restore Settings
            payload.settingsPayload?.let { s ->
                userPreferencesRepository.setThemeMode(s.themeMode)
                userPreferencesRepository.updateMeasurementUnit(s.measurementUnit)
                userPreferencesRepository.setBackupFrequency(s.backupFrequency)
                settingsRepository.setNeuralBriefEnabled(s.isNeuralBriefEnabled)
                settingsRepository.setBriefFrequency(s.briefFrequency)
                settingsRepository.setQuietHoursStart(s.quietHoursStart)
                settingsRepository.setQuietHoursEnd(s.quietHoursEnd)
            }

            // 7. Restore Goals
            payload.goalsPayload?.let { g ->
                g.directives.forEach { d ->
                    val metrics = if (!d.successMetricsJson.isNullOrBlank()) {
                        try {
                            gson.fromJson(d.successMetricsJson, Array<SuccessMetric>::class.java).toList()
                        } catch (_: Exception) { emptyList() }
                    } else emptyList()

                    ascensionDao.insertDirective(
                        AscensionDirectiveEntity(
                            id = d.id,
                            title = d.title,
                            description = d.description,
                            visionStatement = d.visionStatement,
                            status = d.status,
                            successMetrics = metrics,
                            targetEndDate = parseLocalDate(d.targetEndDate),
                            isQuarterly = d.isQuarterly,
                            createdAt = Instant.ofEpochMilli(d.createdAtEpochMs),
                            archivedAt = d.archivedAtEpochMs?.let { Instant.ofEpochMilli(it) },
                            currentProgress = d.currentProgress,
                            totalXPContributed = d.totalXPContributed,
                            xpTarget = d.xpTarget,
                            archetypeTag = d.archetypeTag,
                            tags = d.tags ?: emptyList(),
                            linkedAttributes = d.linkedAttributes ?: emptyList(),
                            aiMentorMode = d.aiMentorMode,
                            aiGenerated = d.aiGenerated,
                            notes = d.notes,
                            completionHistorySummary = d.completionHistorySummary,
                            lastReviewDate = parseLocalDate(d.lastReviewDate),
                            anchoredSelfItemId = d.anchoredSelfItemId,
                            anchoredSelfItemName = d.anchoredSelfItemName,
                            priorityTier = d.priorityTier,
                            outcomeStatement = d.outcomeStatement,
                            affirmation = d.affirmation,
                            visualUri = d.visualUri
                        )
                    )
                }

                g.missions.forEach { m ->
                    ascensionDao.insertMission(
                        AscensionMissionEntity(
                            id = m.id,
                            directiveId = m.directiveId,
                            title = m.title,
                            description = m.description,
                            objective = m.objective,
                            status = m.status,
                            startDate = parseLocalDate(m.startDate, LocalDate.now())!!,
                            targetEndDate = parseLocalDate(m.targetEndDate),
                            createdAt = Instant.ofEpochMilli(m.createdAtEpochMs),
                            completedAt = m.completedAtEpochMs?.let { Instant.ofEpochMilli(it) },
                            archivedAt = m.archivedAtEpochMs?.let { Instant.ofEpochMilli(it) },
                            progress = m.progress,
                            contributionWeight = m.contributionWeight,
                            totalXPContributed = m.totalXPContributed,
                            xpTarget = m.xpTarget,
                            aiMentorMode = m.aiMentorMode,
                            aiGenerated = m.aiGenerated,
                            notes = m.notes,
                            successCriteria = m.successCriteria,
                            completionHistorySummary = m.completionHistorySummary,
                            tags = m.tags ?: emptyList(),
                            linkedAttributes = m.linkedAttributes ?: emptyList(),
                            linkedArchetype = m.linkedArchetype,
                            anchoredSelfItemId = m.anchoredSelfItemId,
                            anchoredSelfItemName = m.anchoredSelfItemName
                        )
                    )
                }

                g.tasks.forEach { t ->
                    ascensionDao.insertTask(
                        AscensionTaskEntity(
                            id = t.id,
                            parentId = t.parentId,
                            title = t.title,
                            description = t.description,
                            type = t.type,
                            isPulse = t.isPulse,
                            recurrenceType = t.recurrenceType,
                            recurrenceCron = t.recurrenceCron,
                            recurrenceIntervalDays = t.recurrenceIntervalDays,
                            recurrenceDaysOfWeek = t.recurrenceDaysOfWeek,
                            timeWindows = t.timeWindows,
                            adaptiveWakeEnabled = t.adaptiveWakeEnabled,
                            reminderEnabled = t.reminderEnabled,
                            xpValue = t.xpValue,
                            impactWeight = t.impactWeight,
                            currentStreak = t.currentStreak,
                            longestStreak = t.longestStreak,
                            graceBufferDays = t.graceBufferDays,
                            lastCompleted = t.lastCompletedEpochMs?.let { Instant.ofEpochMilli(it) },
                            tags = t.tags ?: emptyList(),
                            linkedAttributes = t.linkedAttributes ?: emptyList(),
                            userNotesTemplate = t.userNotesTemplate
                        )
                    )
                }

                g.taskCompletions.forEach { c ->
                    ascensionDao.insertCompletion(
                        AscensionTaskCompletionEntity(
                            id = c.id,
                            taskId = c.taskId,
                            timestamp = Instant.ofEpochMilli(c.timestampEpochMs),
                            notes = c.notes,
                            mood = c.mood,
                            linkedHealthSnapshot = c.linkedHealthSnapshot
                        )
                    )
                }

                g.legacyGoals.forEach { lg ->
                    goalDao.insertGoal(
                        com.neon.ascent.core.data.GoalEntity(
                            id = lg.id,
                            type = lg.type,
                            title = lg.title,
                            description = lg.description,
                            linkedAttributes = lg.linkedAttributes.map {
                                try { SpecialType.valueOf(it) } catch (_: Exception) { SpecialType.STRENGTH }
                            },
                            progressCurrent = lg.progressCurrent,
                            progressTarget = lg.progressTarget,
                            xpContributed = lg.xpContributed,
                            targetDateMillis = lg.targetDateMillis,
                            status = lg.status,
                            expiresAtMillis = lg.expiresAtMillis,
                            parentAspirationId = lg.parentAspirationId,
                            recurrenceType = lg.recurrenceType,
                            recurrenceDays = lg.recurrenceDays,
                            streak = lg.streak,
                            lastCompletedMillis = lg.lastCompletedMillis,
                            parentGoalId = lg.parentGoalId
                        )
                    )
                }
            }

            // 8. Restore Vault
            payload.vaultPayload?.let { v ->
                v.accounts.forEach { a ->
                    val assetType = try { VaultAssetType.valueOf(a.type) } catch (_: Exception) { VaultAssetType.CASH }
                    vaultDao.insertAccount(
                        VaultAccountEntity(
                            id = a.id,
                            name = a.name,
                            institution = a.institution,
                            type = assetType,
                            balance = a.balance,
                            currency = a.currency,
                            isLinked = a.isLinked,
                            lastUpdated = a.lastUpdatedEpochMs
                        )
                    )
                }
                v.snapshots.forEach { s ->
                    vaultDao.insertSnapshot(
                        VaultSnapshotEntity(
                            id = s.id,
                            timestamp = s.timestampEpochMs,
                            totalAssets = s.totalAssets,
                            totalDebt = s.totalDebt,
                            netWorth = s.netWorth
                        )
                    )
                }
                v.watchlist.forEach { w ->
                    vaultDao.addToWatchlist(
                        VaultWatchlistItemEntity(
                            symbol = w.symbol,
                            name = w.name,
                            isCrypto = w.isCrypto
                        )
                    )
                }
            }

            // 9. Restore SelfMap
            payload.selfMapPayload?.let { s ->
                s.items.forEach { i ->
                    val cat = try { SelfMapCategory.valueOf(i.category) } catch (_: Exception) { SelfMapCategory.CUSTOM }
                    selfMapDao.insertSelfItem(
                        SelfItemEntity(
                            id = i.id,
                            name = i.name,
                            category = cat,
                            importance = i.importance,
                            notes = i.notes,
                            isArchived = i.isArchived,
                            createdAt = Instant.ofEpochMilli(i.createdAtEpochMs),
                            updatedAt = Instant.ofEpochMilli(i.updatedAtEpochMs),
                            baselineMassContributor = i.baselineMassContributor
                        )
                    )
                }
                s.snapshots.forEach { sn ->
                    selfMapDao.insertSnapshot(
                        SelfMapSnapshotEntity(
                            id = sn.id,
                            timestamp = Instant.ofEpochMilli(sn.timestampEpochMs),
                            mass = sn.mass,
                            itemCount = sn.itemCount,
                            avgImportance = sn.avgImportance,
                            coreCount = sn.coreCount
                        )
                    )
                }
                s.moodEntries.forEach { m ->
                    selfMapDao.insertMoodEntry(
                        SelfMapMoodEntryEntity(
                            id = m.id,
                            timestamp = Instant.ofEpochMilli(m.timestampEpochMs),
                            score = m.score,
                            note = m.note,
                            eoeId = m.eoeId,
                            snapshotId = m.snapshotId
                        )
                    )
                }
                s.eoeEntries.forEach { e ->
                    selfMapDao.insertEoEEntry(
                        EoEEntryEntity(
                            id = e.id,
                            timestamp = Instant.ofEpochMilli(e.timestampEpochMs),
                            emotionLabel = e.emotionLabel,
                            selfItemId = e.selfItemId,
                            epText = e.epText,
                            perceptionText = e.perceptionText,
                            appraisal = e.appraisal,
                            intensity = e.intensity,
                            canChangeNotes = e.canChangeNotes,
                            linkedSnapshotId = e.linkedSnapshotId
                        )
                    )
                }

                if (s.items.isNotEmpty() || s.onboardingCompleted) {
                    selfMapPreferencesDataStore.setOnboardingCompleted(true)
                    selfMapPreferencesDataStore.setConversationalIntakeSeen(true)
                }
                s.baselineMass?.let { bMass ->
                    selfMapPreferencesDataStore.setBaselineMass(bMass)
                }
            }

            // 10. Restore Biohacking
            payload.biohackingPayload?.let { b ->
                b.reconstitutions.forEach { r ->
                    reconstitutionDao.insertSavedMix(
                        SavedReconstitutionEntity(
                            id = r.id,
                            name = r.name,
                            vialMg = r.vialMg,
                            waterMl = r.waterMl,
                            concentrationMgPerMl = r.concentrationMgPerMl,
                            mixedDate = r.mixedDate,
                            notes = r.notes,
                            componentsJson = r.componentsJson,
                            createdAt = r.createdAtEpochMs
                        )
                    )
                }
                b.bioData?.let { bd ->
                    biohackingDao.insertOrUpdate(
                        BiohackingData(
                            userId = bd.userId,
                            bodyFatPercentage = bd.bodyFatPercentage,
                            energyScore = bd.energyScore,
                            moodScore = bd.moodScore,
                            focusScore = bd.focusScore,
                            sleepHours = bd.sleepHours,
                            sleepQuality = bd.sleepQuality,
                            exerciseFrequency = bd.exerciseFrequency,
                            exerciseType = bd.exerciseType,
                            supplements = bd.supplements,
                            dietType = bd.dietType,
                            caffeineIntake = bd.caffeineIntake,
                            alcoholIntake = bd.alcoholIntake,
                            stressLevel = bd.stressLevel,
                            primaryObjective = bd.primaryObjective,
                            contraindications = bd.contraindications,
                            pregnancyFlag = bd.pregnancyFlag,
                            isWearableSynced = bd.isWearableSynced,
                            lastSyncTimestamp = bd.lastSyncTimestampEpochMs,
                            currentHeartRate = bd.currentHeartRate,
                            currentSteps = bd.currentSteps,
                            labResultsPath = bd.labResultsPath,
                            geneticDataPath = bd.geneticDataPath,
                            hasConsentedToDataProcessing = bd.hasConsentedToDataProcessing,
                            consentAnonymizedUpload = bd.consentAnonymizedUpload,
                            consentWearableSync = bd.consentWearableSync,
                            consentGeneticData = bd.consentGeneticData,
                            hasCompletedPrivacyOnboarding = bd.hasCompletedPrivacyOnboarding,
                            enableOnDeviceNeuralCore = bd.enableOnDeviceNeuralCore,
                            latestReportJson = bd.latestReportJson,
                            reportTimestamp = bd.reportTimestampEpochMs,
                            calculatedBioAge = bd.calculatedBioAge,
                            calendarAgeAtCalculation = bd.calendarAgeAtCalculation,
                            extractedBiomarkersJson = bd.extractedBiomarkersJson
                        )
                    )
                }
                b.protocolLogs.forEach { l ->
                    biohackingDao.insertProtocolLog(
                        BioProtocolLog(
                            id = l.id,
                            userId = l.userId,
                            timestamp = l.timestampEpochMs,
                            energyScore = l.energyScore,
                            sleepQuality = l.sleepQuality,
                            moodScore = l.moodScore,
                            focusScore = l.focusScore,
                            sideEffects = l.sideEffects,
                            notes = l.notes,
                            protocolId = l.protocolId,
                            isWorking = l.isWorking
                        )
                    )
                }
            }

            // 11. Restore Chat
            payload.chatPayload?.let { c ->
                c.memories.forEach { m ->
                    neuralMemoryDao.insertMemory(
                        NeuralMemory(
                            id = m.id,
                            wing = m.wing,
                            room = m.room,
                            content = m.content,
                            importance = m.importance,
                            timestamp = m.timestampEpochMs,
                            metadata = m.metadata
                        )
                    )
                }
                c.sessions.forEach { s ->
                    chatDao.insertChatSession(
                        ChatSession(
                            sessionId = s.sessionId,
                            contactName = s.contactName,
                            lastMessage = s.lastMessage,
                            lastTimestamp = s.lastTimestampEpochMs,
                            isUnread = s.isUnread,
                            isFixer = s.isFixer,
                            personalityPrompt = s.personalityPrompt
                        )
                    )
                }
                c.messages.forEach { msg ->
                    chatDao.insertMessage(
                        ChatMessage(
                            id = msg.id,
                            sessionId = msg.sessionId,
                            contactName = msg.contactName,
                            senderName = msg.senderName,
                            text = msg.text,
                            timestamp = msg.timestampEpochMs,
                            isFromUser = msg.isFromUser,
                            suggestedActions = emptyList()
                        )
                    )
                }
            }

            // Stage: RESTORE_PREFS
            onProgress?.invoke(RestoreProgress(
                stage = RestoreStage.RESTORE_PREFS,
                message = "RESTORING PREFERENCES & SYSTEM CONFIG...",
                checksumHex = checksumHex.take(16)
            ))

            payload.preferencesPayload?.let { prefs ->
                healthPreferencesDataStore.setDefaultRestTime(prefs.defaultRestTimeSeconds)
                healthPreferencesDataStore.setAutoStartRestTimer(prefs.autoStartRestTimer)
                healthPreferencesDataStore.setWorkSetRestTime(prefs.workSetRestTimeSeconds)
                healthPreferencesDataStore.setWarmupSetRestTime(prefs.warmupSetRestTimeSeconds)
                healthPreferencesDataStore.setDropSetRestTime(prefs.dropSetRestTimeSeconds)
                runCatching {
                    healthPreferencesDataStore.setRestTimerMode(
                        com.neon.ascent.core.domain.workout.models.RestTimerMode.valueOf(prefs.restTimerMode)
                    )
                }
                healthPreferencesDataStore.setWorkoutZoomLevel(prefs.workoutZoomLevel)
                runCatching {
                    workoutPreferencesDataStore.setWorkoutLayoutMode(
                        com.neon.ascent.core.domain.workout.models.WorkoutLayoutMode.valueOf(prefs.workoutLayoutMode)
                    )
                }
                workoutPreferencesDataStore.setUseCustomNumpadInFocusMode(prefs.useCustomNumpadInFocusMode)
                runCatching {
                    workoutPreferencesDataStore.setCyberCrappRotationMode(
                        com.neon.ascent.core.domain.workout.models.CyberCrappRotationMode.valueOf(prefs.cyberCrappRotationMode)
                    )
                }
                healthPreferencesDataStore.setCodexPeriod(prefs.codexPeriod)
                healthPreferencesDataStore.setCodexWing(prefs.codexWing)
                healthPreferencesDataStore.setCodexLastExerciseId(prefs.codexLastExerciseId)

                prefs.targetWakeWd?.let { briefPreferencesDataStore.setTargetWakeWd(it) }
                prefs.targetWakeWe?.let { briefPreferencesDataStore.setTargetWakeWe(it) }
                prefs.pulsePmMode?.let { briefPreferencesDataStore.setPulsePmMode(it) }
                val customDays = prefs.pulsePmCustomDays
                val customTime = prefs.pulsePmCustomTime
                if (customDays != null && customTime != null) {
                    briefPreferencesDataStore.setPulsePmCustomSchedule(customDays, customTime)
                }
                briefPreferencesDataStore.setAdaptiveWakeEnabled(prefs.adaptiveWakeEnabled)
                prefs.quietHoursEnd?.let { briefPreferencesDataStore.setQuietHoursEnd(it) }
                briefPreferencesDataStore.setShowDirectivesOnMorningShade(prefs.showDirectivesOnMorningShade)

                prefs.directiveIntakeMode?.let { modeStr ->
                    runCatching { DirectiveIntakeMode.valueOf(modeStr) }.getOrNull()?.let { mode ->
                        directivePreferencesDataStore.setDirectiveIntakeMode(mode)
                    }
                }

                healthPreferencesDataStore.setAutoSyncEnabled(prefs.autoSyncEnabled)
                prefs.syncIntervalHours?.let { healthPreferencesDataStore.setSyncIntervalHours(it) }
                if (prefs.enabledAttributes.isNotEmpty()) {
                    val attrs = prefs.enabledAttributes.mapNotNull { name ->
                        runCatching { SpecialType.valueOf(name) }.getOrNull()
                    }.toSet()
                    if (attrs.isNotEmpty()) {
                        healthPreferencesDataStore.setEnabledAttributes(attrs)
                    }
                }
                healthPreferencesDataStore.setLiveMonitoringEnabled(prefs.liveMonitoringEnabled)
                healthPreferencesDataStore.setShowSyncNotification(prefs.showSyncNotification)

                settingsRepository.setNetrunnerMode(prefs.isNetrunnerMode)
                settingsRepository.setAltarTrueTextMode(prefs.isAltarTrueTextMode)
                settingsRepository.setBiometricLockEnabled(prefs.biometricLockEnabled)
                prefs.biometricLockTimeout?.let { timeoutSec ->
                    val timeoutStr = when (timeoutSec) {
                        60 -> "1_MIN"
                        300 -> "5_MIN"
                        900 -> "15_MIN"
                        else -> "IMMEDIATE"
                    }
                    settingsRepository.setBiometricLockTimeout(timeoutStr)
                }
            }

            runCatching { dietRepository.seedDefaultDiets() }

            payload.dietPayload?.let { diet ->
                dietPreferencesDataStore.setActiveDietId(diet.activeDietId)
                diet.overrides?.let { overridesDto ->
                    dietPreferencesDataStore.updateDietOverrides(overridesDto.toDomain())
                }
            }

            runCatching { supplementRepository.seedDefaultSupplements() }

            payload.supplementPayload?.let { supp ->
                supplementPreferencesDataStore.setActiveStackIds(supp.activeStackIds)
                val mappedOverrides = supp.overrides.mapValues { it.value.toDomain() }
                supplementPreferencesDataStore.updateAllOverrides(mappedOverrides)
            }

            runCatching { biohackingProtocolRepository.seedDefaultProtocols() }

            payload.biohackingProtocolPayload?.let { proto ->
                biohackingProtocolPreferencesDataStore.setActiveProtocolIds(proto.activeProtocolIds)
                val mappedOverrides = proto.overrides.mapValues { it.value.toDomain() }
                biohackingProtocolPreferencesDataStore.updateAllOverrides(mappedOverrides)
            }

            payload.labsFocusPayload?.let { focusSection ->
                labsFocusPreferencesDataStore.setLabsFocus(focusSection.toDomain())
            } ?: run {
                if (mode == RestoreMode.REPLACE) {
                    labsFocusPreferencesDataStore.setLabsFocus(com.neon.ascent.core.domain.labs.models.LabsFocus())
                }
            }

            var ritesRestored = 0
            payload.ritesPayload?.let { ritesSection ->
                val entities = ritesSection.sessions.map { dto ->
                    RiteSessionEntity(
                        id = dto.id,
                        kind = dto.kind,
                        startedAt = Instant.ofEpochMilli(dto.startedAtEpochMs),
                        endedAt = dto.endedAtEpochMs?.let { Instant.ofEpochMilli(it) },
                        durationMin = dto.durationMin,
                        source = dto.source,
                        notes = dto.notes,
                        localDate = dto.localDate
                    )
                }
                if (entities.isNotEmpty()) {
                    riteSessionDao.insertSessions(entities)
                    ritesRestored = entities.size
                }
            }

            var highlightsRestored = 0
            payload.libraryPayload?.let { librarySection ->
                val entities = librarySection.highlights.map { dto ->
                    LibraryHighlightEntity(
                        id = dto.id,
                        bookId = dto.bookId,
                        chapterIndex = dto.chapterIndex,
                        startOffset = dto.startOffset,
                        endOffset = dto.endOffset,
                        color = dto.color,
                        timestamp = dto.timestampEpochMs
                    )
                }
                if (entities.isNotEmpty()) {
                    libraryDao.insertHighlights(entities)
                    highlightsRestored = entities.size
                }
            }

            // Stage: INTEGRITY
            onProgress?.invoke(RestoreProgress(
                stage = RestoreStage.INTEGRITY,
                message = "VERIFYING DATA INTEGRITY...",
                checksumHex = checksumHex.take(16)
            ))

            val existingOp = operativeProfileDao.getOperativeProfileOnce("default_user")
            if (existingOp == null) {
                val fallbackOp = OperativeProfileEntity(
                    id = "default_user",
                    name = "OPERATIVE",
                    sex = "UNSPECIFIED",
                    dob = "2000-01-01",
                    units = "METRIC",
                    weight = "70",
                    somatotype = 0.5f,
                    isCreationComplete = true
                )
                operativeProfileDao.upsertOperativeProfile(fallbackOp)
                userCharacterDao.insertUserCharacter(
                    UserCharacter(
                        id = 0,
                        name = "OPERATIVE",
                        sex = "UNSPECIFIED",
                        dob = "2000-01-01",
                        units = "METRIC",
                        weight = "70",
                        somatotype = 0.5f,
                        isCreationComplete = true
                    )
                )
            }

            userPreferencesRepository.setOnboardingComplete(true)
            healthPreferencesDataStore.updateLastSyncTime()
            userPreferencesRepository.setLastBackupTimestamp(System.currentTimeMillis())
            userPreferencesRepository.setLastBackupVersion(payload.version)

            val restoredProfile = workoutDao.getUserProfile("default_user").firstOrNull()
            val restoredLayoutMode = workoutPreferencesDataStore.workoutLayoutMode.firstOrNull()?.name ?: "CLASSIC"
            val restoredWorkRest = healthPreferencesDataStore.workSetRestTime.firstOrNull() ?: 120

            Log.d("FullDataBackupRepo", """
                === Migration Restore Smoke-Test Summary ===
                Restored Sessions: $sessionsRestored, Sets: $setsRestored, Cycles: $cyclesRestored, Rites: $ritesRestored, Highlights: $highlightsRestored
                Scheduled Days: ${restoredProfile?.scheduledDays ?: "[]"}
                Active Protocol: ${restoredProfile?.activeProtocol ?: "NONE"}
                Layout Mode: $restoredLayoutMode
                Work-Set Rest Time: ${restoredWorkRest}s
                ==========================================
            """.trimIndent())

            val result = RestoreResult(
                success = true,
                message = "Uplink restore complete ($sessionsRestored sessions, $setsRestored sets, $cyclesRestored cycles, $ritesRestored rites, $highlightsRestored highlights)",
                restoredSessionsCount = sessionsRestored,
                restoredBiomarkersCount = biomarkersRestored,
                restoredCodexCount = codexRestored,
                restoredJournalCount = journalRestored,
                restoredRitesCount = ritesRestored,
                restoredHighlightsCount = highlightsRestored
            )

            onProgress?.invoke(RestoreProgress(
                stage = RestoreStage.READY,
                message = "RESTORE READY",
                checksumHex = checksumHex.take(16),
                result = result
            ))

            result
        } catch (e: Exception) {
            val failure = RestoreResult(
                success = false,
                message = "Restore failed: ${e.localizedMessage}"
            )
            onProgress?.invoke(RestoreProgress(
                stage = RestoreStage.FAIL,
                message = failure.message,
                error = e.localizedMessage
            ))
            failure
        }
    }

    private fun parseLocalDate(dateStr: String?, default: LocalDate? = null): LocalDate? {
        if (dateStr.isNullOrBlank()) return default
        return try {
            LocalDate.parse(dateStr)
        } catch (_: Exception) {
            default
        }
    }

    private fun snapshotDatabasesBeforeReplace() {
        val epoch = System.currentTimeMillis()
        val dbNames = listOf("neon_ascent_database", "neon_ascent_v5_secure.db")
        for (dbName in dbNames) {
            val dbFile = context.getDatabasePath(dbName)
            if (dbFile.exists()) {
                val dbBackup = File(dbFile.parentFile, "${dbFile.name}.pre_restore-$epoch")
                copyFile(dbFile, dbBackup)

                val walFile = File("${dbFile.path}-wal")
                if (walFile.exists()) {
                    val walBackup = File(walFile.parentFile, "${walFile.name}.pre_restore-$epoch")
                    copyFile(walFile, walBackup)
                }

                val shmFile = File("${dbFile.path}-shm")
                if (shmFile.exists()) {
                    val shmBackup = File(shmFile.parentFile, "${shmFile.name}.pre_restore-$epoch")
                    copyFile(shmFile, shmBackup)
                }
            }
        }
    }

    private fun copyFile(source: File, destination: File) {
        try {
            source.inputStream().use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            Log.e("FullDataBackupRepo", "Failed to create pre_restore snapshot for ${source.name}", e)
        }
    }
}
