package com.neon.ascent.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import com.neon.ascent.core.data.NeonAscentDatabase
import com.neon.ascent.core.data.local.dao.BiomarkerDao
import com.neon.ascent.core.data.local.dao.BodySampleDao
import com.neon.ascent.core.data.local.dao.DailyVitalRollupDao
import com.neon.ascent.core.data.local.dao.DopamineMenuDao
import com.neon.ascent.core.data.local.dao.GoalDao
import com.neon.ascent.core.data.local.dao.OperativeProfileDao
import com.neon.ascent.core.data.local.dao.SpecialDao
import com.neon.ascent.core.data.local.dao.WorkoutDao
import com.neon.ascent.core.data.local.entity.*
import com.neon.ascent.core.domain.backup.models.*
import com.neon.ascent.core.domain.chronicle.ChronicleEntry
import com.neon.ascent.core.domain.chronicle.ChronicleRepository
import com.neon.ascent.core.domain.library.models.LibraryQuote
import com.neon.ascent.core.domain.library.repository.LibraryRepository
import com.neon.ascent.core.domain.model.DopamineCategory
import com.neon.ascent.core.domain.model.EnergyLevel
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.core.domain.repository.FullDataBackupRepository
import com.neon.ascent.data.local.AppDatabase
import com.neon.ascent.data.local.BiohackingDao
import com.neon.ascent.data.local.BookDao
import com.neon.ascent.data.local.DailyPrayerDao
import com.neon.ascent.data.local.GoalTaskDao
import com.neon.ascent.data.local.JournalDao
import com.neon.ascent.data.local.LoreDao
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.data.mapper.toChronicleEntry
import com.neon.ascent.model.DailyPrayer
import com.neon.ascent.model.JournalEntry
import com.neon.ascent.model.QuoteEntity
import com.neon.ascent.model.UserCharacter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
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
                            timestampEpochMs = st.timestamp.toEpochMilli()
                        )
                    }
                }
            }

            val exercisesDto = workoutDao.getExerciseDefinitions().first().map { e ->
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
                    isPrimaryVariant = e.isPrimaryVariant
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
                    activeProtocol = p.activeProtocol,
                    sequencerEnabled = p.sequencerEnabled,
                    autoWeightIncrement = p.autoWeightIncrement,
                    weightIncrementCompound = p.weightIncrementCompound,
                    weightIncrementIsolation = p.weightIncrementIsolation,
                    rirCapturePerMiniSet = p.rirCapturePerMiniSet,
                    coachingHintsEnabled = p.coachingHintsEnabled
                )
            }
            val primaryProfileDto = profilesDto.firstOrNull { it.userId == "default_user" } ?: profilesDto.firstOrNull()

            WorkoutBackupSection(
                sessions = sessionsDto,
                logs = logsDto,
                sets = setsDto,
                customExercises = exercisesDto,
                exerciseMaxes = maxesDto,
                userProfile = primaryProfileDto,
                userProfiles = profilesDto
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

            val chronicleEntries = chronicleRepository.observeChronicle().first().map { c ->
                ChronicleEntryDto(
                    source = c.source,
                    sourceId = c.sourceId,
                    wing = c.wing,
                    room = c.room,
                    content = c.content,
                    timestampEpochMs = c.timestamp,
                    hearted = c.hearted,
                    metadata = c.metadata
                )
            }

            JournalBackupSection(
                journalEntries = entries,
                dailyPrayers = prayers,
                chronicleEntries = chronicleEntries
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
                    avatarPath = o.avatarPath
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

        val payload = NeonAscentBackupPayload(
            version = 2,
            exportedAt = now,
            appVersion = "1.0",
            workoutPayload = workoutSection,
            biometricsPayload = biometricsSection,
            codexPayload = codexSection,
            journalPayload = journalSection,
            characterPayload = characterSection,
            settingsPayload = settingsSection
        )

        return gson.toJson(payload)
    }

    override suspend fun restoreBackupJson(jsonString: String, mode: RestoreMode): RestoreResult {
        return try {
            val sanitizedJson = jsonString.trim().removePrefix("\uFEFF")
            if (sanitizedJson.isBlank()) {
                return RestoreResult(success = false, message = "Invalid JSON payload structure: File is empty")
            }

            val payload = try {
                gson.fromJson(sanitizedJson, NeonAscentBackupPayload::class.java)
            } catch (e: Exception) {
                return RestoreResult(success = false, message = "Invalid JSON payload structure")
            } ?: return RestoreResult(success = false, message = "Invalid JSON payload structure")

            if (payload.workoutPayload == null &&
                payload.biometricsPayload == null &&
                payload.codexPayload == null &&
                payload.journalPayload == null &&
                payload.characterPayload == null &&
                payload.settingsPayload == null
            ) {
                return RestoreResult(success = false, message = "Invalid JSON payload structure: No backup sections found")
            }

            if (mode == RestoreMode.REPLACE) {
                snapshotDatabasesBeforeReplace()
                coreDatabase.clearAllTables()
                appDatabase.clearAllTables()
            }

            var sessionsRestored = 0
            var biomarkersRestored = 0
            var codexRestored = 0
            var journalRestored = 0

            // 1. Restore Workouts
            payload.workoutPayload?.let { w ->
                w.customExercises.forEach { e ->
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
                            isPrimaryVariant = e.isPrimaryVariant
                        )
                    )
                }

                w.sessions.forEach { s ->
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

                w.logs.forEach { l ->
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

                w.sets.forEach { st ->
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
                            clusterMiniSetIndex = null,
                            isLengthenedPartial = false,
                            isLoadedStretch = false,
                            stretchDurationSeconds = null
                        )
                    )
                }

                w.exerciseMaxes.forEach { m ->
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

                val profilesToRestore = if (w.userProfiles.isNotEmpty()) w.userProfiles else listOfNotNull(w.userProfile)
                profilesToRestore.forEach { p ->
                    workoutDao.insertUserProfile(
                        UserWorkoutProfileEntity(
                            userId = p.userId,
                            experienceLevel = p.experienceLevel,
                            somatotype = p.somatotype,
                            injuries = emptyList(),
                            timePerSessionMinutes = 60,
                            age = 25,
                            heightCm = 175f,
                            weightKg = 75f,
                            gender = "PREFER_NOT_TO_SAY",
                            activityFactor = 1.2f,
                            unitSystem = "METRIC",
                            activeProtocol = p.activeProtocol,
                            rotationIndex = 0,
                            scheduledDays = "[]",
                            deepLinkToRoutine = false,
                            sequencerEnabled = p.sequencerEnabled,
                            autoWeightIncrement = p.autoWeightIncrement,
                            weightIncrementCompound = p.weightIncrementCompound,
                            weightIncrementIsolation = p.weightIncrementIsolation,
                            rirCapturePerMiniSet = p.rirCapturePerMiniSet,
                            coachingHintsEnabled = p.coachingHintsEnabled
                        )
                    )
                }
            }

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
                            metadata = ce.metadata
                        )
                    )
                }
            }

            // 5. Restore Character
            payload.characterPayload?.let { char ->
                char.userCharacter?.let { c ->
                    userCharacterDao.insertUserCharacter(
                        UserCharacter(
                            id = 0,
                            name = c.name,
                            sex = "UNSPECIFIED",
                            dob = "2000-01-01",
                            units = "METRIC",
                            weight = "70",
                            somatotype = 0.5f,
                            level = c.level,
                            experience = c.experience,
                            eddies = c.eddies,
                            iceLevel = c.iceLevel,
                            prayerStreak = c.prayerStreak,
                            lastPrayerDate = c.lastPrayerDateEpochMs
                        )
                    )
                }

                char.operativeProfile?.let { op ->
                    operativeProfileDao.upsertOperativeProfile(
                        OperativeProfileEntity(
                            id = op.id,
                            name = op.name,
                            netrunnerName = op.netrunnerName,
                            sex = op.sex,
                            dob = op.dob,
                            units = op.units,
                            heightFeet = op.heightFeet,
                            heightInches = op.heightInches,
                            heightCm = op.heightCm,
                            weight = op.weight,
                            somatotype = op.somatotype,
                            level = op.level,
                            experience = op.experience,
                            iceLevel = op.iceLevel,
                            eddies = op.eddies,
                            secureEddies = op.secureEddies,
                            prayerStreak = op.prayerStreak,
                            lastPrayerDate = op.lastPrayerDateEpochMs,
                            isCreationComplete = op.isCreationComplete,
                            avatarPath = op.avatarPath
                        )
                    )
                }

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

            RestoreResult(
                success = true,
                message = "Uplink restore complete",
                restoredSessionsCount = sessionsRestored,
                restoredBiomarkersCount = biomarkersRestored,
                restoredCodexCount = codexRestored,
                restoredJournalCount = journalRestored
            )
        } catch (e: Exception) {
            RestoreResult(
                success = false,
                message = "Restore failed: ${e.localizedMessage}"
            )
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
