package com.neon.ascent.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.neon.ascent.core.data.local.dao.SpecialDao
import com.neon.ascent.core.data.local.dao.GoalDao
import com.neon.ascent.core.data.local.dao.AscensionDao
import com.neon.ascent.core.data.local.entity.*
import com.neon.ascent.core.data.local.converter.*
import com.neon.ascent.core.data.local.migration.MIGRATION_3_4
import com.neon.ascent.core.data.local.migration.MIGRATION_11_12

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
        AugmentSetEntity::class
    ],
    version = 33,
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
    abstract fun workoutDao(): com.neon.ascent.core.data.local.dao.WorkoutDao
    abstract fun neuralMemoryDao(): com.neon.ascent.core.data.local.dao.NeuralMemoryDao
    abstract fun insightDao(): com.neon.ascent.core.data.local.dao.InsightDao
    abstract fun dopamineMenuDao(): com.neon.ascent.core.data.local.dao.DopamineMenuDao
    abstract fun protocolDao(): com.neon.ascent.core.data.local.dao.ProtocolDao
}
