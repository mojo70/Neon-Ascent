package com.neon.ascent.core.domain.workout.models

import java.time.Instant

data class WorkoutSession(
    val id: String,
    val date: Instant = Instant.now(),
    val protocol: WorkoutProtocol = WorkoutProtocol.GENERAL,
    val durationSeconds: Long = 0,
    val notes: String? = null,
    val experienceLevel: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
    val somatotype: Somatotype = Somatotype.MESOMORPH,
    val sessionRpe: Int? = null,
    val jointHealth: Int? = null,
    val isDeload: Boolean = false,
    val cycleId: String? = null,
    val protocolDayType: ProtocolDayType? = null
)

enum class WorkoutProtocol {
    GENERAL, CYBER_CRAPP, STRAIGHT_SETS, DUP, SUPERSETS,
    HST, STARTING_STRENGTH, FIVE_THREE_ONE, WESTSIDE;

    val displayName: String
        get() = when (this) {
            GENERAL, STRAIGHT_SETS, SUPERSETS -> "OPS / FREE"
            CYBER_CRAPP -> "CYBERCRAPP"
            DUP -> "D.U.P."
            HST -> "HST"
            STARTING_STRENGTH -> "STARTING STRENGTH"
            FIVE_THREE_ONE -> "5/3/1"
            WESTSIDE -> "WESTSIDE"
        }

    val oneLineContract: String
        get() = when (this) {
            CYBER_CRAPP -> "Chase cluster reps. Weight holds."
            STARTING_STRENGTH -> "We prescribe 3x5. Hit all sets, add weight."
            HST -> "We prescribe the load. You hit 15s / 10s / 5s."
            FIVE_THREE_ONE -> "Training max waves. Last set AMRAP."
            WESTSIDE -> "ME / DE / RE days. Rotate the main lift."
            DUP -> "Same lifts. Strength / hypertrophy / power rotate."
            STRAIGHT_SETS, GENERAL -> "You pick sets and reps."
            SUPERSETS -> "Pair two lifts. Not a program."
        }

    val isSelectableEngine: Boolean
        get() = when (this) {
            STRAIGHT_SETS, SUPERSETS -> false
            else -> true
        }

    val recommendation: String?
        get() = when (this) {
            STARTING_STRENGTH -> "Recommended for Novices"
            CYBER_CRAPP, FIVE_THREE_ONE -> "Recommended for Intermediates"
            WESTSIDE -> "Available for Advanced Operatives"
            else -> null
        }

    val frequencyCaption: String
        get() = when (this) {
            FIVE_THREE_ONE, WESTSIDE -> "4× / WEEK"
            else -> "3× / WEEK"
        }

    val defaultWeekdays: List<Int>
        get() = when (this) {
            FIVE_THREE_ONE -> listOf(1, 2, 4, 5)
            WESTSIDE -> listOf(1, 3, 5, 6)
            else -> listOf(1, 3, 5)
        }

    val loreName: String
        get() = dossier.loreName

    val description: String
        get() = when (this) {
            CYBER_CRAPP -> "High-intensity rest-pause protocol designed for maximum effective reps and time efficiency."
            DUP -> "Daily Undulating Periodization: varying intensity and volume daily to optimize strength and muscle gain."
            HST -> "Hypertrophy Specific Training: strategic deconditioning followed by progressive load waves."
            STARTING_STRENGTH -> "The gold standard for novice linear progression on big compound lifts."
            FIVE_THREE_ONE -> "Jim Wendler's multi-wave system for sustainable long-term strength gains."
            WESTSIDE -> "Conjugate system alternating Max Effort and Dynamic Effort for elite-level power."
            else -> "Versatile foundational programming for overall athletic performance and hypertrophy."
        }

    val tenants: List<String>
        get() = dossier.tenets

    val methodology: String
        get() = dossier.sessionAnatomy

    val dossier: ProtocolDossier
        get() = when (this) {
            CYBER_CRAPP -> ProtocolDossier(
                protocol = this,
                displayName = "CYBERCRAPP",
                loreName = "REST_PAUSE_CLUSTERS",
                focus = "Hypertrophy / Efficiency",
                recommendedLevel = ExperienceLevel.INTERMEDIATE,
                alsoFits = "Time-crunched operatives, density seekers.",
                daysPerWeek = 3,
                loggingContract = "1 Work Set + 2 Rest-Pause Minis + Stretch.",
                intake = "Current cluster weight Best.",
                tenets = listOf(
                    "1 Main Set to failure + 2 rest-pause 'mini-sets'",
                    "Mandatory 10-second loaded stretch for hypertrophy",
                    "Maximum 'Effective Rep' density in under 45 mins"
                ),
                frequencyCopy = "CYBERCRAPP RUNS 3 DAYS. A/B/C ROTATION.",
                sessionAnatomy = "REST-PAUSE CLUSTERS: After warmups, perform one set to failure. Rest 15 seconds. Perform a second mini-set to failure. Rest 15 seconds. Perform a third mini-set. Follow with a 10s finisher and a loaded stretch.",
                notThis = "Not for pure 1RM powerlifting peaks.",
                ctaLabel = "SET ACTIVE"
            )
            STARTING_STRENGTH -> ProtocolDossier(
                protocol = this,
                displayName = "STARTING STRENGTH",
                loreName = "LINEAR_NOVICE",
                focus = "Absolute Strength (Novice)",
                recommendedLevel = ExperienceLevel.NOVICE,
                alsoFits = "Returning operatives, baseline calibration.",
                daysPerWeek = 3,
                loggingContract = "3 Sets of 5 Reps. Add weight every session.",
                intake = "Starting working weights (5-rep).",
                tenets = listOf(
                    "Novice linear progression (weight added every time)",
                    "3x5 rep scheme for absolute strength",
                    "Prioritizes recovery over variety"
                ),
                frequencyCopy = "LINEAR NOVICE IS 3 DAYS. A/B ALTERNATE.",
                sessionAnatomy = "NOVICE LINEAR: Add 5lbs to upper body and 10lbs to lower body every session for 3 sets of 5 reps.",
                notThis = "Not a high-volume aesthetic program.",
                ctaLabel = "CALIBRATE ENGINE"
            )
            HST -> ProtocolDossier(
                protocol = this,
                displayName = "HST",
                loreName = "WAVE_HYPERTROPHY",
                focus = "Hypertrophy Waves",
                recommendedLevel = ExperienceLevel.INTERMEDIATE,
                alsoFits = "Bodybuilding enthusiasts, wave periodization fans.",
                daysPerWeek = 3,
                loggingContract = "15 / 10 / 5 rep blocks. Pre-calculated loads.",
                intake = "15RM, 10RM, 5RM testing.",
                tenets = listOf(
                    "Strategic Deconditioning (9-14 days off)",
                    "Every session is full body",
                    "Weight increases every single workout"
                ),
                frequencyCopy = "HST IS FULL BODY 3×. LADDER ASSUMES THIS.",
                sessionAnatomy = "WAVING VOLUME: Cycle through 2 weeks of 15s, 10s, 5s, and Negatives. Weight increases linearly while reps decrease every block.",
                notThis = "Not a daily RPE-based protocol.",
                ctaLabel = "CALIBRATE ENGINE"
            )
            FIVE_THREE_ONE -> ProtocolDossier(
                protocol = this,
                displayName = "5/3/1",
                loreName = "WAVE_531",
                focus = "Long-term Strength",
                recommendedLevel = ExperienceLevel.INTERMEDIATE,
                alsoFits = "Sub-maximal training, consistency seekers.",
                daysPerWeek = 4,
                loggingContract = "TM-based wave loading. Final set AMRAP.",
                intake = "Current 1RM estimates.",
                tenets = listOf(
                    "Training Max based prescription (sub-maximal)",
                    "AMRAP final sets to determine session volume",
                    "4-week waves with strategic deloading"
                ),
                frequencyCopy = "5/3/1 IS 4 MAINS. ONE LIFT PER DAY.",
                sessionAnatomy = "WAVE LOADING: Week 1 (3x5), Week 2 (3x3), Week 3 (3x5-3-1), Week 4 (Deload). Based on 90% of your True 1RM.",
                notThis = "Not a rapid-gain novice program.",
                ctaLabel = "CALIBRATE ENGINE"
            )
            WESTSIDE -> ProtocolDossier(
                protocol = this,
                displayName = "WESTSIDE",
                loreName = "CONJUGATE",
                focus = "Peak Power / Conjugate",
                recommendedLevel = ExperienceLevel.ADVANCED,
                alsoFits = "Powerlifters, variant specialists.",
                daysPerWeek = 4,
                loggingContract = "ME (1-3RM) / DE (Explosive) rotation.",
                intake = "Variant 1RMs.",
                tenets = listOf(
                    "Conjugate method: Strength + Speed concurrently",
                    "Max Effort: 1-3 Rep Max on a variant",
                    "Dynamic Effort: Explosive sub-maximal speed work"
                ),
                frequencyCopy = "CONJUGATE IS 4 DAYS. ME / DE / ME / DE.",
                sessionAnatomy = "CONJUGATE: Two Max Effort days (Heavy) and two Dynamic Effort days (Fast) per week. Variants rotate weekly.",
                notThis = "Not for metabolic conditioning or high-rep pumps.",
                ctaLabel = "CALIBRATE ENGINE"
            )
            DUP -> ProtocolDossier(
                protocol = this,
                displayName = "D.U.P.",
                loreName = "UNDULATION",
                focus = "Daily Undulation",
                recommendedLevel = ExperienceLevel.ADVANCED,
                alsoFits = "Multi-modal strength/size seekers.",
                daysPerWeek = 3,
                loggingContract = "Hypertrophy / Strength / Power rotation.",
                intake = "Strength working weights.",
                tenets = listOf(
                    "Alternating 'Hypertrophy', 'Power', and 'Strength' days",
                    "Prevents neural adaptation through variety",
                    "Optimized for advanced operatives"
                ),
                frequencyCopy = "UNDULATION IS 3 DAYS. HYP / STR / PWR.",
                sessionAnatomy = "DAILY UNDULATION: Volume and intensity fluctuate every session. Focus on explosive power on Power days, and mind-muscle connection on Hypertrophy days.",
                notThis = "Not a fixed-load linear program.",
                ctaLabel = "CALIBRATE ENGINE"
            )
            else -> ProtocolDossier(
                protocol = this,
                displayName = "OPS / FREE",
                loreName = "GENERAL_STRENGTH",
                focus = "Customization / Testing",
                recommendedLevel = ExperienceLevel.ANY,
                alsoFits = "Experienced specialists, hybrid athletes.",
                daysPerWeek = 3,
                loggingContract = "Manual set/rep entry. No prescriptive logic.",
                intake = "None.",
                tenets = listOf(
                    "Progressive overload across multiple rep ranges",
                    "Balanced focus on strength and hypertrophy",
                    "Adaptive recovery based on biometrics"
                ),
                frequencyCopy = "DEFAULT 3 DAYS. CHANGE IF YOU WANT.",
                sessionAnatomy = "STANDARD OVERLOAD: Focus on consistent form and incremental increases in resistance or volume.",
                notThis = "Not an automated progression system.",
                ctaLabel = "SET ACTIVE"
            )
        }
}

data class ProtocolDossier(
    val protocol: WorkoutProtocol,
    val displayName: String,
    val loreName: String,
    val focus: String,
    val recommendedLevel: ExperienceLevel,
    val alsoFits: String,
    val daysPerWeek: Int,
    val loggingContract: String,
    val intake: String,
    val tenets: List<String>,
    val frequencyCopy: String,
    val sessionAnatomy: String,
    val notThis: String,
    val ctaLabel: String
)

enum class ProtocolUiMode { CLUSTER, LINEAR, PRESCRIBED, MAX_EFFORT, DYNAMIC }

enum class ProtocolDayType {
    CC_A, CC_B, CC_C,
    SS_A, SS_B,
    HST_15, HST_10, HST_5, HST_NEG, HST_SD,
    FTV_W1, FTV_W2, FTV_W3, FTV_DELOAD,
    DUP_HYPERTROPHY, DUP_STRENGTH, DUP_POWER,
    WS_ME_LOWER, WS_ME_UPPER, WS_DE_LOWER, WS_DE_UPPER, WS_RE
}

data class ProtocolCycle(
    val id: String,
    val userId: String,
    val protocol: WorkoutProtocol,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val status: CycleStatus = CycleStatus.ACTIVE,
    val currentWeek: Int = 1,
    val currentDayIndex: Int = 0,
    val configJson: String? = null
)

enum class CycleStatus { ACTIVE, COMPLETED, ABANDONED }

enum class MaxSource { TESTED, ESTIMATED, MANUAL }

data class ExerciseMax(
    val familyId: String,
    val testedAt: Instant,
    val oneRepMax: Float,
    val rm15: Float? = null,
    val rm10: Float? = null,
    val rm5: Float? = null,
    val trainingMax: Float? = null,
    val source: MaxSource = MaxSource.ESTIMATED
)

data class PrescribedSet(
    val weight: Float,
    val reps: Int,
    val setType: SetType = SetType.NORMAL,
    val percentOfMax: Float? = null,
    val isAmrap: Boolean = false,
    val accommodatingLoad: Float? = null
)

enum class RestPausePhase {
    NOT_ACTIVE, MINI_SET_1, MINI_SET_2, MINI_SET_3, FINISHER, LOADED_STRETCH
}

enum class RestTimerMode {
    POPUP, INLINE, BOTH, NONE
}

enum class ExperienceLevel {
    NOVICE, INTERMEDIATE, ADVANCED, ANY
}

enum class Somatotype {
    ECTOMORPH, MESOMORPH, ENDOMORPH
}

enum class SetType {
    NORMAL, WARMUP, DROP, FAILURE, REST_PAUSE, WIDOWMAKER, POWER, GS, PARTIAL, STRETCH, MAX_EFFORT
}

enum class MovementType {
    COMPOUND_UPPER,
    ISOLATION_UPPER,
    BACK_WIDTH,
    BACK_THICKNESS,
    DEADLIFT,
    POSTERIOR_CHAIN,
    QUAD_DOMINANT,
    HAMSTRING_ISOLATION,
    CALVES,
    ABS,
    UNDEFINED
}

enum class Implement {
    BARBELL, DUMBBELL, KETTLEBELL, EZ_BAR, SPECIALTY_BAR, MACHINE, SMITH,
    PLATE_LOADED, CABLE, BODYWEIGHT, BAND, OTHER
}

enum class Stance {
    STANDARD, INCLINE, DECLINE, SEATED, STANDING, CHEST_SUPPORTED, FRONT,
    BACK, ZERCHER, GOBLET, SINGLE_ARM, SINGLE_LEG, DEFICIT, FLOOR,
    CLOSE_GRIP, BOX, SUMO, BENT_OVER
}

data class Exercise(
    val id: String,
    val name: String,
    val description: String,
    val cues: List<String>,
    val muscleGroups: List<String>,
    val equipment: List<String>,
    val gifAssetPath: String? = null,
    val isLockedClassic: Boolean = false,
    val injurySubstitutions: List<String> = emptyList(),
    val dangerousFor: List<String> = emptyList(),
    val movementType: MovementType = MovementType.UNDEFINED,
    val notes: String? = null,
    val familyId: String,
    val familyName: String,
    val implement: Implement,
    val stance: Stance = Stance.STANDARD,
    val specialtyBar: String? = null,
    val rangeOverrideMin: Int? = null,
    val rangeOverrideMax: Int? = null,
    val allowsAddedLoad: Boolean = false,
    val isPrimaryVariant: Boolean = false
) {
    val displayLabel: String
        get() = buildString {
            append(familyName)
            if (stance != Stance.STANDARD) {
                append(" · ")
                append(stance.name.lowercase().capitalizeWords())
            }
            if (implement != Implement.BARBELL || specialtyBar != null) {
                append(" · ")
                append(specialtyBar?.lowercase()?.capitalizeWords() ?: implement.name.lowercase().capitalizeWords())
            }
        }

    private fun String.capitalizeWords(): String =
        split("_").joinToString(" ") { it.lowercase().replaceFirstChar { char -> char.uppercase() } }
}

data class ExerciseFamily(
    val id: String,
    val name: String,
    val movementType: MovementType,
    val variants: List<Exercise>
)

data class ProtocolRepTarget(
    val id: String = java.util.UUID.randomUUID().toString(),
    val protocol: WorkoutProtocol,
    val movementType: MovementType,
    val setType: SetType,
    val familyId: String? = null,
    val minReps: Int,
    val maxReps: Int,
    val unit: String = "REPS" // REPS or SECONDS
)

data class WorkoutLog(
    val id: String,
    val sessionId: String,
    val exerciseId: String,
    val order: Int,
    val exerciseName: String, // Denormalized for convenience
    val protocolOverride: WorkoutProtocol? = null,
    val supersetId: String? = null,
    val augmentId: String? = null,
    val augmentName: String? = null,
    val augmentColor: String? = null,
    val showGoalReps: Boolean = false
)

data class WorkoutAugment(
    val id: String,
    val name: String,
    val description: String?,
    val focusBodyPart: String,
    val exercises: List<RoutineExercise> = emptyList(),
    val colorHex: String = "#007AFF",
    val isSystem: Boolean = false,
    val isAddedToLibrary: Boolean = true,
    val scheduledDays: List<ScheduledDay> = emptyList()
)

data class SetLog(
    val id: String,
    val workoutLogId: String,
    val weight: Float,
    val reps: Int,
    val type: SetType = SetType.NORMAL,
    val goalReps: String? = null,
    val isCompleted: Boolean = false,
    val rir: Int? = null,
    val isWarmup: Boolean = false, // Deprecated in favor of type
    val timestamp: Instant = Instant.now(),
    
    // CyberCrapp specific fields
    val clusterMiniSetIndex: Int? = null, // 1, 2, or 3 for rest-pause
    val isLengthenedPartial: Boolean = false,
    val isLoadedStretch: Boolean = false,
    val stretchDurationSeconds: Int? = null,

    // Engine specific fields
    val prescribedWeight: Float? = null,
    val prescribedReps: Int? = null,
    val percentOfMax: Float? = null,
    val isAmrap: Boolean = false,
    val accommodatingLoad: Float? = null
)

enum class Gender {
    MALE, FEMALE, OTHER
}

enum class UnitSystem {
    IMPERIAL, METRIC
}

data class UserWorkoutProfile(
    val userId: String,
    val experienceLevel: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
    val somatotype: Somatotype = Somatotype.MESOMORPH,
    val injuries: List<String> = emptyList(),
    val timePerSessionMinutes: Int = 60,
    
    // Bio-data for Macros/TDEE
    val age: Int = 25,
    val heightCm: Float = 175f,
    val weightKg: Float = 75f,
    val gender: Gender = Gender.MALE,
    val activityFactor: Float = 1.375f, // Default: Lightly Active
    val unitSystem: UnitSystem = UnitSystem.IMPERIAL,

    // Active Protocol & Scheduling
    val activeProtocol: WorkoutProtocol? = null,
    val rotationIndex: Int = 0,
    val scheduledDays: List<ScheduledDay> = emptyList(),
    val deepLinkToRoutine: Boolean = true,

    // Progression & Recovery Settings
    val autoWeightIncrement: Boolean = true,
    val weightIncrementCompound: Float = 5.0f,
    val weightIncrementIsolation: Float = 2.5f,
    val rirCapturePerMiniSet: Boolean = false,
    val sequencerEnabled: Boolean = true,
    val customSequenceIds: List<String> = emptyList(),
    val coachingHintsEnabled: Boolean = true,
    val breathingVibrationEnabled: Boolean = true,
    val lastBlastStartDate: Instant? = null
)

data class FuelSnapshot(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Instant = Instant.now(),
    val weightKg: Float,
    val tdee: Int,
    val protein: Int,
    val carb: Int,
    val fat: Int,
    val activityFactor: Float,
    val somatotype: Somatotype
)

data class ScheduledDay(
    val dayOfWeek: Int, // 1 (Mon) to 7 (Sun)
    val time: String // HH:mm
)

data class WorkoutRoutine(
    val id: String,
    val name: String,
    val description: String? = null,
    val exercises: List<RoutineExercise> = emptyList(),
    val augments: List<WorkoutAugment> = emptyList(),
    val protocol: WorkoutProtocol = WorkoutProtocol.GENERAL,
    val createdAt: Instant = Instant.now(),
    val isSystem: Boolean = false,
    val isAddedToLibrary: Boolean = true
)

data class RoutineExercise(
    val exercise: Exercise,
    val sets: List<RoutineSet> = emptyList()
)

data class RoutineSet(
    val type: SetType = SetType.NORMAL,
    val weight: Float = 0f,
    val reps: Int = 0,
    val goalReps: String? = null
)

data class ProgressionState(
    val exerciseId: String,
    val bestClusterReps: Int = 0,
    val weightAtBest: Float = 0f,
    val consecutiveMisses: Int = 0,
    val currentWeight: Float = 0f,
    val lastRotationDate: Instant? = null
)

enum class RecoveryStatus {
    OPTIMAL, CAUTION, DELOAD, CRITICAL
}

data class RecoveryScore(
    val totalScore: Int, // 0-100
    val status: RecoveryStatus,
    val rirTrend: Float,
    val avgJointHealth: Float,
    val stagnationCount: Int,
    val avgRpe: Float,
    val plainLanguageSummary: String
)
