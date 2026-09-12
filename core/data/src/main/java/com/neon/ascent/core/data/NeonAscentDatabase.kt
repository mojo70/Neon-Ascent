package com.neon.ascent.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.neon.ascent.core.data.local.dao.SpecialDao
import com.neon.ascent.core.data.local.dao.GoalDao
import com.neon.ascent.core.data.local.dao.AscensionDao
import com.neon.ascent.core.data.local.dao.OperativeProfileDao
import com.neon.ascent.core.data.local.entity.*
import com.neon.ascent.core.data.local.converter.*
import com.neon.ascent.core.data.local.dao.BiomarkerDao
import com.neon.ascent.core.data.local.migration.MIGRATION_3_4
import com.neon.ascent.core.data.local.migration.MIGRATION_11_12
import com.neon.ascent.core.data.local.migration.MIGRATION_48_49
import com.neon.ascent.core.data.local.migration.MIGRATION_50_51
import com.neon.ascent.core.data.local.migration.MIGRATION_51_52
import com.neon.ascent.core.data.local.migration.MIGRATION_52_53
import com.neon.ascent.core.data.local.migration.MIGRATION_53_54
import com.neon.ascent.core.data.local.dao.DailyVitalRollupDao
import com.neon.ascent.core.data.local.dao.BodySampleDao
import com.neon.ascent.core.data.local.dao.DopamineMenuDao
import com.neon.ascent.core.data.local.dao.InsightDao
import com.neon.ascent.core.data.local.dao.NeuralMemoryDao
import com.neon.ascent.core.data.local.dao.ProtocolDao
import com.neon.ascent.core.data.local.dao.WorkoutDao

import com.neon.ascent.core.data.local.dao.LibraryDao
import com.neon.ascent.core.data.local.dao.VaultDao

@Database(
    entities = [
        GoalEntity::class,
        SpecialAttributeEntity::class,
        BenchmarkTestEntity::class,
        AscensionDirectiveEntity::class,
        AscensionMissionEntity::class,
        AscensionTaskEntity::class,
        AscensionTaskCompletionEntity::class,
        NeuralLogEntity::class,
        NeuralMemory::class,
        BiometricEventEntity::class,
        ActionEventEntity::class,
        SocraticInsightEntity::class,
        DopamineMenuItemEntity::class,
        ProtocolEntity::class,
        AdaptedProtocolEntity::class,
        WorkoutSessionEntity::class,
        ExerciseDefinitionEntity::class,
        WorkoutLogEntity::class,
        SetLogEntity::class,
        UserWorkoutProfileEntity::class,
        WorkoutRoutineEntity::class,
        RoutineExerciseCrossRef::class,
        WorkoutAugmentEntity::class,
        AugmentExerciseCrossRef::class,
        RoutineAugmentCrossRef::class,
        RoutineSetEntity::class,
        AugmentSetEntity::class,
        ProgressionStateEntity::class,
        ExerciseAccomplishmentsEntity::class,
        BiomarkerSampleEntity::class,
        FuelSnapshotEntity::class,
        ProtocolRepTargetEntity::class,
        ProtocolCycleEntity::class,
        ExerciseMaxEntity::class,
        AugmentActivationEntity::class,
        DailyVitalRollupEntity::class,
        BodySampleEntity::class,
        OperativeProfileEntity::class,
        LibraryBookEntity::class,
        LibraryChapterEntity::class,
        LibraryHighlightEntity::class,
        LibraryQuoteEntity::class,
        VaultAccountEntity::class,
        VaultSnapshotEntity::class,
        VaultWatchlistItemEntity::class
    ],
    version = 56,
    exportSchema = true
)
@TypeConverters(
    InstantConverter::class,
    LocalDateConverter::class,
    SpecialTypeConverter::class,
    TestTypeConverter::class,
    DataSourceConverter::class,
    StringMapConverter::class,
    SpecialTypeListConverter::class,
    StringListConverter::class,
    LongListConverter::class,
    DopamineCategoryConverter::class,
    EnergyLevelConverter::class,
    SuccessMetricListConverter::class,
    IntListConverter::class
)
abstract class NeonAscentDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun specialDao(): SpecialDao
    abstract fun ascensionDao(): AscensionDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun neuralMemoryDao(): NeuralMemoryDao
    abstract fun insightDao(): InsightDao
    abstract fun dopamineMenuDao(): DopamineMenuDao
    abstract fun protocolDao(): ProtocolDao
    abstract fun biomarkerDao(): BiomarkerDao
    abstract fun dailyVitalRollupDao(): DailyVitalRollupDao
    abstract fun bodySampleDao(): BodySampleDao
    abstract fun operativeProfileDao(): OperativeProfileDao
    abstract fun libraryDao(): LibraryDao
    abstract fun vaultDao(): VaultDao
}
