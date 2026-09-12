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
    val includeCharacter: Boolean = true
)

enum class RestoreMode {
    MERGE,
    REPLACE
}

data class RestoreResult(
    val success: Boolean,
    val message: String,
    val restoredSessionsCount: Int = 0,
    val restoredBiomarkersCount: Int = 0,
    val restoredCodexCount: Int = 0,
    val restoredJournalCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class NeonAscentBackupPayload(
    val version: Int = 1,
    val exportedAt: String,
    val appVersion: String = "1.0",
    val workoutPayload: WorkoutBackupSection? = null,
    val biometricsPayload: BiometricsBackupSection? = null,
    val codexPayload: CodexBackupSection? = null,
    val journalPayload: JournalBackupSection? = null,
    val characterPayload: CharacterBackupSection? = null
)

data class WorkoutBackupSection(
    val sessions: List<WorkoutSessionDto> = emptyList(),
    val logs: List<WorkoutLogDto> = emptyList(),
    val sets: List<SetLogDto> = emptyList(),
    val customExercises: List<ExerciseDefinitionDto> = emptyList(),
    val exerciseMaxes: List<ExerciseMaxDto> = emptyList(),
    val userProfile: UserWorkoutProfileDto? = null
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
    val activeProtocol: String? = null,
    val sequencerEnabled: Boolean = false,
    val autoWeightIncrement: Boolean = false,
    val weightIncrementCompound: Float = 5f,
    val weightIncrementIsolation: Float = 2.5f,
    val rirCapturePerMiniSet: Boolean = false,
    val coachingHintsEnabled: Boolean = true
)

data class BiometricsBackupSection(
    val biomarkers: List<BiomarkerDto> = emptyList(),
    val specialAttributes: List<SpecialAttributeDto> = emptyList()
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
    val dailyPrayers: List<DailyPrayerDto> = emptyList()
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

data class CharacterBackupSection(
    val userCharacter: UserCharacterDto? = null,
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

data class DopamineMenuItemDto(
    val id: String,
    val title: String,
    val description: String = "",
    val energyLevel: String = "MEDIUM",
    val category: String = "RESET",
    val usageCount: Int = 0
)
