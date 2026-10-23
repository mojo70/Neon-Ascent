package com.neon.ascent.core.domain.backup.models

enum class BackupFrequency {
    DAILY,
    WEEKLY,
    AFTER_WORKOUT,
    MANUAL;

    val displayName: String
        get() = when (this) {
            DAILY -> "DAILY"
            WEEKLY -> "WEEKLY"
            AFTER_WORKOUT -> "POST_WORKOUT"
            MANUAL -> "MANUAL_ONLY"
        }
}

data class BackupScope(
    val includeWorkout: Boolean = true,
    val includeBiometrics: Boolean = true,
    val includeCodex: Boolean = true,
    val includeJournal: Boolean = true,
    val includeCharacter: Boolean = true,
    val includeGoals: Boolean = true,
    val includeVault: Boolean = true,
    val includeSelfMap: Boolean = true,
    val includeBiohacking: Boolean = true,
    val includeChat: Boolean = true
)

enum class RestoreMode {
    MERGE,
    REPLACE
}

enum class RestoreStage {
    HANDSHAKE,
    RECEIVING,
    READING,
    DECRYPT,
    PARSE,
    CHECKSUM,
    RESTORE_WORKOUT,
    RESTORE_DOMAINS,
    RESTORE_PREFS,
    INTEGRITY,
    READY,
    FAIL
}

data class RestoreProgress(
    val stage: RestoreStage,
    val message: String = "",
    val checksumHex: String? = null,
    val result: RestoreResult? = null,
    val error: String? = null
)

data class RestoreResult(
    val success: Boolean,
    val message: String,
    val restoredSessionsCount: Int = 0,
    val restoredBiomarkersCount: Int = 0,
    val restoredCodexCount: Int = 0,
    val restoredJournalCount: Int = 0,
    val restoredRitesCount: Int = 0,
    val restoredHighlightsCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class NeonAscentBackupPayload(
    val version: Int = 5,
    val exportedAt: String,
    val appVersion: String = "1.0",
    val workoutPayload: WorkoutBackupSection? = null,
    val biometricsPayload: BiometricsBackupSection? = null,
    val codexPayload: CodexBackupSection? = null,
    val journalPayload: JournalBackupSection? = null,
    val characterPayload: CharacterBackupSection? = null,
    val settingsPayload: SettingsBackupSection? = null,
    val goalsPayload: GoalsBackupSection? = null,
    val vaultPayload: VaultBackupSection? = null,
    val selfMapPayload: SelfMapBackupSection? = null,
    val biohackingPayload: BiohackingBackupSection? = null,
    val chatPayload: ChatBackupSection? = null,
    val preferencesPayload: PreferencesBackupSection? = null,
    val ritesPayload: RitesBackupSection? = null,
    val libraryPayload: LibraryBackupSection? = null
)

data class WorkoutBackupSection(
    val sessions: List<WorkoutSessionDto> = emptyList(),
    val logs: List<WorkoutLogDto> = emptyList(),
    val sets: List<SetLogDto> = emptyList(),
    val customExercises: List<ExerciseDefinitionDto> = emptyList(),
    val exerciseMaxes: List<ExerciseMaxDto> = emptyList(),
    val userProfile: UserWorkoutProfileDto? = null,
    val userProfiles: List<UserWorkoutProfileDto> = emptyList(),
    val protocolCycles: List<ProtocolCycleDto> = emptyList(),
    val progressionStates: List<ProgressionStateDto> = emptyList(),
    val exerciseAccomplishments: List<ExerciseAccomplishmentDto> = emptyList(),
    val routines: List<WorkoutRoutineDto> = emptyList(),
    val routineExerciseCrossRefs: List<RoutineExerciseCrossRefDto> = emptyList(),
    val routineSets: List<RoutineSetDto> = emptyList(),
    val augments: List<WorkoutAugmentDto> = emptyList(),
    val augmentExerciseCrossRefs: List<AugmentExerciseCrossRefDto> = emptyList(),
    val augmentSets: List<AugmentSetDto> = emptyList(),
    val augmentActivations: List<AugmentActivationDto> = emptyList(),
    val routineAugmentCrossRefs: List<RoutineAugmentCrossRefDto> = emptyList(),
    val protocolRepTargets: List<ProtocolRepTargetDto> = emptyList()
)

data class WorkoutSessionDto(
    val id: String,
    val dateEpochMs: Long,
    val protocol: String,
    val durationSeconds: Long,
    val notes: String? = null,
    val experienceLevel: String,
    val somatotype: String,
    val sessionRpe: Int? = null,
    val jointHealth: Int? = null,
    val isDeload: Boolean = false,
    val cycleId: String? = null,
    val protocolDayType: String? = null,
    val primaryAugmentId: String? = null
)

data class WorkoutLogDto(
    val id: String,
    val sessionId: String,
    val exerciseId: String,
    val order: Int,
    val exerciseName: String = "",
    val showGoalReps: Boolean = true,
    val supersetId: String? = null
)

data class SetLogDto(
    val id: String,
    val workoutLogId: String,
    val weight: Float,
    val reps: Int,
    val setType: String = "REGULAR",
    val goalReps: String? = null,
    val isCompleted: Boolean = true,
    val rir: Int? = null,
    val isWarmup: Boolean = false,
    val timestampEpochMs: Long
)

data class ExerciseDefinitionDto(
    val id: String,
    val name: String,
    val description: String,
    val cues: List<String> = emptyList(),
    val muscleGroups: List<String> = emptyList(),
    val equipment: List<String> = emptyList(),
    val gifAssetPath: String? = null,
    val isLockedClassic: Boolean = false,
    val injurySubstitutions: List<String> = emptyList(),
    val dangerousFor: List<String> = emptyList(),
    val movementType: String = "UNDEFINED",
    val notes: String? = null,
    val familyId: String = "",
    val familyName: String = "",
    val implement: String = "OTHER",
    val stance: String = "STANDARD",
    val specialtyBar: String? = null,
    val rangeOverrideMin: Int? = null,
    val rangeOverrideMax: Int? = null,
    val allowsAddedLoad: Boolean = false,
    val isPrimaryVariant: Boolean = false
)

data class ExerciseMaxDto(
    val familyId: String,
    val testedAtEpochMs: Long,
    val oneRepMax: Float,
    val rm15: Float? = null,
    val rm10: Float? = null,
    val rm5: Float? = null,
    val trainingMax: Float? = null,
    val source: String = "MANUAL"
)

data class UserWorkoutProfileDto(
    val userId: String,
    val experienceLevel: String,
    val somatotype: String,
    val gender: String? = "OTHER",
    val activeProtocol: String? = null,
    val sequencerEnabled: Boolean = false,
    val autoWeightIncrement: Boolean = false,
    val progressionMode: String = "FIXED_INCREMENT",
    val weightIncrementCompound: Float = 5f,
    val weightIncrementUpperCompound: Float = 5f,
    val weightIncrementLowerCompound: Float = 10f,
    val weightIncrementIsolation: Float = 5f,
    val progressionPercentage: Float = 2.5f,
    val roundingIncrement: Float = 5f,
    val rirCapturePerMiniSet: Boolean = false,
    val coachingHintsEnabled: Boolean = true,
    val injuries: List<String>? = emptyList(),
    val timePerSessionMinutes: Int = 60,
    val age: Int = 30,
    val heightCm: Float = 175f,
    val weightKg: Float = 75f,
    val activityFactor: Float = 1.2f,
    val unitSystem: String? = "METRIC",
    val rotationIndex: Int = 0,
    val scheduledDays: String? = "[]",
    val deepLinkToRoutine: Boolean = false,
    val customSequenceIds: String? = "[]",
    val lastBlastStartDateEpochMs: Long? = null
)

data class ProtocolCycleDto(
    val id: String,
    val userId: String,
    val protocol: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long? = null,
    val status: String,
    val currentWeek: Int,
    val currentDayIndex: Int,
    val configJson: String? = null
)

data class ProgressionStateDto(
    val exerciseId: String,
    val bestClusterReps: Int,
    val weightAtBest: Float,
    val consecutiveMisses: Int,
    val currentWeight: Float,
    val lastRotationDateEpochMs: Long? = null
)

data class ExerciseAccomplishmentDto(
    val exerciseId: String,
    val heaviestWeight: Float,
    val heaviestWeightReps: Int,
    val heaviestWeightDateEpochMs: Long? = null,
    val maxEstimatedOneRepMax: Float,
    val maxOneRepMaxWeight: Float,
    val maxOneRepMaxReps: Int,
    val maxOneRepMaxDateEpochMs: Long? = null,
    val maxSessionVolume: Float,
    val maxSessionVolumeDateEpochMs: Long? = null,
    val maxRepsAtTopWeight: Int,
    val topWeightForReps: Float,
    val bestClusterReps: Int,
    val bestClusterWeight: Float,
    val bestClusterDateEpochMs: Long? = null
)

data class WorkoutRoutineDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val protocol: String,
    val createdAtEpochMs: Long,
    val isSystem: Boolean = false,
    val isAddedToLibrary: Boolean = true
)

data class RoutineExerciseCrossRefDto(
    val routineId: String,
    val exerciseId: String,
    val order: Int
)

data class RoutineSetDto(
    val id: String,
    val routineId: String,
    val exerciseId: String,
    val order: Int,
    val type: String,
    val weight: Float,
    val reps: Int,
    val goalReps: String? = null
)

data class WorkoutAugmentDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val focusBodyPart: String,
    val colorHex: String,
    val isSystem: Boolean = false,
    val isAddedToLibrary: Boolean = true,
    val scheduledDays: String = "[]"
)

data class AugmentExerciseCrossRefDto(
    val augmentId: String,
    val exerciseId: String,
    val order: Int
)

data class AugmentSetDto(
    val id: String,
    val augmentId: String,
    val exerciseId: String,
    val order: Int,
    val type: String,
    val weight: Float,
    val reps: Int,
    val goalReps: String? = null
)

data class AugmentActivationDto(
    val id: String,
    val augmentId: String,
    val userId: String,
    val mode: String,
    val status: String,
    val loggingStyle: String,
    val scheduledDays: String = "[]",
    val createdAtEpochMs: Long,
    val windowStartEpochMs: Long? = null,
    val windowEndEpochMs: Long? = null,
    val hostProtocolFilter: String? = null,
    val dayTypeFilter: String? = null,
    val reminderEnabled: Boolean = false
)

data class RoutineAugmentCrossRefDto(
    val routineId: String,
    val augmentId: String,
    val order: Int
)

data class ProtocolRepTargetDto(
    val id: String,
    val protocol: String,
    val movementType: String,
    val setType: String,
    val familyId: String? = null,
    val minReps: Int,
    val maxReps: Int,
    val unit: String = "REPS"
)

data class PreferencesBackupSection(
    val defaultRestTimeSeconds: Int = 60,
    val autoStartRestTimer: Boolean = true,
    val workSetRestTimeSeconds: Int = 120,
    val warmupSetRestTimeSeconds: Int = 60,
    val dropSetRestTimeSeconds: Int = 30,
    val restTimerMode: String = "BOTH",
    val workoutZoomLevel: Float = 1.0f,
    val workoutLayoutMode: String = "CLASSIC",
    val useCustomNumpadInFocusMode: Boolean = false,
    val codexPeriod: String = "THIRTY_DAYS",
    val codexWing: String = "OPS_LOG",
    val codexLastExerciseId: String? = null,
    val targetWakeWd: String? = null,
    val targetWakeWe: String? = null,
    val pulsePmMode: String? = null,
    val pulsePmCustomDays: String? = null,
    val pulsePmCustomTime: String? = null,
    val adaptiveWakeEnabled: Boolean = false,
    val quietHoursEnd: String? = null,
    val showDirectivesOnMorningShade: Boolean = false,
    val directiveIntakeMode: String? = null,
    val autoSyncEnabled: Boolean = false,
    val syncIntervalHours: Int? = null,
    val enabledAttributes: List<String> = emptyList(),
    val liveMonitoringEnabled: Boolean = false,
    val showSyncNotification: Boolean = false,
    val isNetrunnerMode: Boolean = false,
    val isAltarTrueTextMode: Boolean = false,
    val biometricLockEnabled: Boolean = false,
    val biometricLockTimeout: Int? = null
)

data class RitesBackupSection(
    val sessions: List<RiteSessionDto> = emptyList()
)

data class RiteSessionDto(
    val id: String,
    val kind: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long? = null,
    val durationMin: Int,
    val source: String,
    val notes: String? = null,
    val localDate: String
)

data class LibraryBackupSection(
    val highlights: List<LibraryHighlightDto> = emptyList()
)

data class LibraryHighlightDto(
    val id: Long = 0,
    val bookId: String,
    val chapterIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val color: Long,
    val timestampEpochMs: Long
)

data class BiometricsBackupSection(
    val biomarkers: List<BiomarkerDto> = emptyList(),
    val specialAttributes: List<SpecialAttributeDto> = emptyList(),
    val bodySamples: List<BodySampleDto> = emptyList(),
    val vitalRollups: List<DailyVitalRollupDto> = emptyList()
)

data class BiomarkerDto(
    val type: String,
    val value: Float,
    val timestampEpochMs: Long,
    val unit: String
)

data class SpecialAttributeDto(
    val type: String,
    val baseValue: Int = 5,
    val currentValue: Int = 5,
    val percentile: Int? = null,
    val totalXp: Long = 0L
)

data class BodySampleDto(
    val id: String,
    val localDate: String,
    val loggedAtEpochMs: Long,
    val metric: String,
    val site: String? = null,
    val value: Double,
    val unit: String,
    val method: String? = null,
    val position: String? = null,
    val side: String? = null,
    val conditionTag: String? = null,
    val source: String,
    val note: String? = null
)

data class DailyVitalRollupDto(
    val localDate: String,
    val metric: String,
    val value: Double,
    val source: String = "RESTORED",
    val quality: String = "OK",
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

data class CodexBackupSection(
    val unlockedLoreIds: List<String> = emptyList(),
    val quotes: List<QuoteDto> = emptyList()
)

data class QuoteDto(
    val id: Long,
    val bookId: String,
    val bookTitle: String,
    val content: String,
    val chapterTitle: String,
    val timestampEpochMs: Long
)

data class JournalBackupSection(
    val journalEntries: List<JournalEntryDto> = emptyList(),
    val dailyPrayers: List<DailyPrayerDto> = emptyList(),
    val chronicleEntries: List<ChronicleEntryDto> = emptyList()
)

data class JournalEntryDto(
    val id: String,
    val text: String,
    val category: String,
    val timestampEpochMs: Long,
    val isHearted: Boolean = false
)

data class DailyPrayerDto(
    val dayNumber: Int,
    val prayer: String = "",
    val scripture: String = "",
    val reflectionPrompt: String = "",
    val scriptureReference: String = "",
    val scriptureTranslation: String = "KING JAMES",
    val adoreCyber: String = "",
    val adoreTrue: String = "",
    val confessCyber: String = "",
    val confessTrue: String = "",
    val askCyber: String = "",
    val askTrue: String = ""
)

data class ChronicleEntryDto(
    val source: String,
    val sourceId: String,
    val wing: String = "CHRONICLE",
    val room: String = "ORIGIN",
    val content: String,
    val timestampEpochMs: Long,
    val hearted: Boolean = false,
    val metadata: String? = null
)

data class CharacterBackupSection(
    val userCharacter: UserCharacterDto? = null,
    val operativeProfile: OperativeProfileDto? = null,
    val dopamineItems: List<DopamineMenuItemDto> = emptyList()
)

data class UserCharacterDto(
    val name: String,
    val level: Int,
    val experience: Long,
    val eddies: Int = 0,
    val iceLevel: Int = 1,
    val prayerStreak: Int = 0,
    val lastPrayerDateEpochMs: Long = 0L
)

data class OperativeProfileDto(
    val id: String = "default_user",
    val name: String,
    val netrunnerName: String? = null,
    val sex: String,
    val dob: String,
    val units: String,
    val heightFeet: String? = null,
    val heightInches: String? = null,
    val heightCm: String? = null,
    val weight: String,
    val somatotype: Float,
    val level: Int = 1,
    val experience: Long = 0,
    val iceLevel: Int = 1,
    val eddies: Int = 0,
    val secureEddies: Int = 0,
    val prayerStreak: Int = 0,
    val lastPrayerDateEpochMs: Long = 0L,
    val isCreationComplete: Boolean = false,
    val avatarPath: String? = null,
    val avatarBase64: String? = null
)

data class DopamineMenuItemDto(
    val id: String,
    val title: String,
    val description: String = "",
    val energyLevel: String = "MEDIUM",
    val category: String = "RESET",
    val usageCount: Int = 0
)

data class SettingsBackupSection(
    val themeMode: String = "CYBER",
    val measurementUnit: String = "Metric",
    val activeProtocol: String? = null,
    val weightIncrementCompound: Float = 5.0f,
    val weightIncrementIsolation: Float = 2.5f,
    val isNeuralBriefEnabled: Boolean = true,
    val briefFrequency: String = "DAILY",
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00",
    val backupFrequency: String = "DAILY"
)

// 1. Goals (Ascension V3 & Legacy Goals)
data class GoalsBackupSection(
    val directives: List<AscensionDirectiveDto> = emptyList(),
    val missions: List<AscensionMissionDto> = emptyList(),
    val tasks: List<AscensionTaskDto> = emptyList(),
    val taskCompletions: List<AscensionTaskCompletionDto> = emptyList(),
    val legacyGoals: List<GoalDto> = emptyList()
)

data class AscensionDirectiveDto(
    val id: String,
    val title: String,
    val description: String,
    val visionStatement: String? = null,
    val status: String,
    val successMetricsJson: String? = null,
    val targetEndDate: String? = null,
    val isQuarterly: Boolean = false,
    val createdAtEpochMs: Long,
    val archivedAtEpochMs: Long? = null,
    val currentProgress: Float = 0f,
    val totalXPContributed: Long = 0L,
    val xpTarget: Long? = null,
    val archetypeTag: String? = null,
    val tags: List<String> = emptyList(),
    val linkedAttributes: List<String> = emptyList(),
    val aiMentorMode: String = "DEFAULT",
    val aiGenerated: Boolean = false,
    val notes: String? = null,
    val completionHistorySummary: String? = null,
    val lastReviewDate: String? = null,
    val anchoredSelfItemId: Long? = null,
    val anchoredSelfItemName: String? = null,
    val priorityTier: String? = null,
    val outcomeStatement: String? = null,
    val affirmation: String? = null,
    val visualUri: String? = null
)

data class AscensionMissionDto(
    val id: String,
    val directiveId: String? = null,
    val title: String,
    val description: String,
    val objective: String? = null,
    val status: String,
    val startDate: String,
    val targetEndDate: String? = null,
    val createdAtEpochMs: Long,
    val completedAtEpochMs: Long? = null,
    val archivedAtEpochMs: Long? = null,
    val progress: Float = 0f,
    val contributionWeight: Float = 1f,
    val totalXPContributed: Long = 0L,
    val xpTarget: Long? = null,
    val aiMentorMode: String = "DEFAULT",
    val aiGenerated: Boolean = false,
    val notes: String? = null,
    val successCriteria: String? = null,
    val completionHistorySummary: String? = null,
    val tags: List<String> = emptyList(),
    val linkedAttributes: List<String> = emptyList(),
    val linkedArchetype: String? = null,
    val anchoredSelfItemId: Long? = null,
    val anchoredSelfItemName: String? = null
)

data class AscensionTaskDto(
    val id: String,
    val parentId: String? = null,
    val title: String,
    val description: String,
    val type: String,
    val isPulse: Boolean = false,
    val recurrenceType: String? = null,
    val recurrenceCron: String? = null,
    val recurrenceIntervalDays: Int? = null,
    val recurrenceDaysOfWeek: String? = null,
    val timeWindows: String? = null,
    val adaptiveWakeEnabled: Boolean = false,
    val reminderEnabled: Boolean = false,
    val xpValue: Int = 10,
    val impactWeight: Float = 1f,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val graceBufferDays: Int = 0,
    val lastCompletedEpochMs: Long? = null,
    val tags: List<String> = emptyList(),
    val linkedAttributes: List<String> = emptyList(),
    val userNotesTemplate: String? = null
)

data class AscensionTaskCompletionDto(
    val id: Long = 0,
    val taskId: String,
    val timestampEpochMs: Long,
    val notes: String? = null,
    val mood: Int? = null,
    val linkedHealthSnapshot: String? = null
)

data class GoalDto(
    val id: String,
    val type: String,
    val title: String,
    val description: String,
    val linkedAttributes: List<String> = emptyList(),
    val progressCurrent: Double = 0.0,
    val progressTarget: Double = 1.0,
    val xpContributed: Int = 0,
    val targetDateMillis: Long? = null,
    val status: String? = "ACTIVE",
    val expiresAtMillis: Long? = null,
    val parentAspirationId: String? = null,
    val recurrenceType: String? = null,
    val recurrenceDays: List<String>? = null,
    val streak: Int = 0,
    val lastCompletedMillis: Long? = null,
    val parentGoalId: String? = null
)

// 2. Vault
data class VaultBackupSection(
    val accounts: List<VaultAccountDto> = emptyList(),
    val snapshots: List<VaultSnapshotDto> = emptyList(),
    val watchlist: List<VaultWatchlistItemDto> = emptyList()
)

data class VaultAccountDto(
    val id: String,
    val name: String,
    val institution: String,
    val type: String,
    val balance: Double,
    val currency: String = "USD",
    val isLinked: Boolean = false,
    val lastUpdatedEpochMs: Long
)

data class VaultSnapshotDto(
    val id: Int = 0,
    val timestampEpochMs: Long,
    val totalAssets: Double,
    val totalDebt: Double,
    val netWorth: Double
)

data class VaultWatchlistItemDto(
    val symbol: String,
    val name: String,
    val isCrypto: Boolean = false
)

// 3. SelfMap
data class SelfMapBackupSection(
    val items: List<SelfItemDto> = emptyList(),
    val snapshots: List<SelfMapSnapshotDto> = emptyList(),
    val moodEntries: List<SelfMapMoodEntryDto> = emptyList(),
    val eoeEntries: List<EoEEntryDto> = emptyList(),
    val baselineMass: Float? = null,
    val onboardingCompleted: Boolean = true
)

data class SelfItemDto(
    val id: Long = 0,
    val name: String,
    val category: String,
    val importance: Float,
    val notes: String? = null,
    val isArchived: Boolean = false,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val baselineMassContributor: Boolean? = null
)

data class SelfMapSnapshotDto(
    val id: Long = 0,
    val timestampEpochMs: Long,
    val mass: Float,
    val itemCount: Int,
    val avgImportance: Float,
    val coreCount: Int
)

data class SelfMapMoodEntryDto(
    val id: Long = 0,
    val timestampEpochMs: Long,
    val score: Int,
    val note: String? = null,
    val eoeId: Long? = null,
    val snapshotId: Long? = null
)

data class EoEEntryDto(
    val id: Long = 0,
    val timestampEpochMs: Long,
    val emotionLabel: String,
    val selfItemId: Long,
    val epText: String,
    val perceptionText: String,
    val appraisal: String? = null,
    val intensity: Int? = null,
    val canChangeNotes: String? = null,
    val linkedSnapshotId: Long? = null
)

// 4. Biohacking
data class BiohackingBackupSection(
    val reconstitutions: List<SavedReconstitutionDto> = emptyList(),
    val bioData: BiohackingDataDto? = null,
    val protocolLogs: List<BioProtocolLogDto> = emptyList()
)

data class SavedReconstitutionDto(
    val id: String,
    val name: String,
    val vialMg: String,
    val waterMl: String,
    val concentrationMgPerMl: String,
    val mixedDate: String? = null,
    val notes: String? = null,
    val componentsJson: String? = null,
    val createdAtEpochMs: Long
)

data class BiohackingDataDto(
    val userId: Int = 0,
    val bodyFatPercentage: Float? = null,
    val energyScore: Int = 5,
    val moodScore: Int = 5,
    val focusScore: Int = 5,
    val sleepHours: Float? = null,
    val sleepQuality: Int = 5,
    val exerciseFrequency: String? = null,
    val exerciseType: String? = null,
    val supplements: String? = null,
    val dietType: String? = null,
    val caffeineIntake: String? = null,
    val alcoholIntake: String? = null,
    val stressLevel: Int = 5,
    val primaryObjective: String? = null,
    val contraindications: String? = null,
    val pregnancyFlag: Boolean = false,
    val isWearableSynced: Boolean = false,
    val lastSyncTimestampEpochMs: Long? = null,
    val currentHeartRate: Int? = null,
    val currentSteps: Long? = null,
    val labResultsPath: String? = null,
    val geneticDataPath: String? = null,
    val hasConsentedToDataProcessing: Boolean = false,
    val consentAnonymizedUpload: Boolean = false,
    val consentWearableSync: Boolean = false,
    val consentGeneticData: Boolean = false,
    val hasCompletedPrivacyOnboarding: Boolean = false,
    val enableOnDeviceNeuralCore: Boolean = false,
    val latestReportJson: String? = null,
    val reportTimestampEpochMs: Long? = null,
    val calculatedBioAge: Float? = null,
    val calendarAgeAtCalculation: Int? = null,
    val extractedBiomarkersJson: String? = null
)

data class BioProtocolLogDto(
    val id: Int = 0,
    val userId: Int = 0,
    val timestampEpochMs: Long,
    val energyScore: Int,
    val sleepQuality: Int,
    val moodScore: Int,
    val focusScore: Int,
    val sideEffects: String? = null,
    val notes: String? = null,
    val protocolId: String,
    val isWorking: Boolean? = null
)

// 5. Chat / Neural Memory
data class ChatBackupSection(
    val memories: List<NeuralMemoryDto> = emptyList(),
    val sessions: List<ChatSessionDto> = emptyList(),
    val messages: List<ChatMessageDto> = emptyList()
)

data class NeuralMemoryDto(
    val id: Long = 0,
    val wing: String,
    val room: String,
    val content: String,
    val importance: Float = 0.5f,
    val timestampEpochMs: Long,
    val metadata: String? = null
)

data class ChatSessionDto(
    val sessionId: String,
    val contactName: String,
    val lastMessage: String,
    val lastTimestampEpochMs: Long,
    val isUnread: Boolean = false,
    val isFixer: Boolean = true,
    val personalityPrompt: String? = null
)

data class ChatMessageDto(
    val id: Long = 0,
    val sessionId: String,
    val contactName: String,
    val senderName: String,
    val text: String,
    val timestampEpochMs: Long,
    val isFromUser: Boolean,
    val suggestedActionsJson: String? = null
)

