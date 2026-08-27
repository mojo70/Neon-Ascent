package com.neon.ascent.core.data.local.dao

import androidx.room.*
import com.neon.ascent.core.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface WorkoutDao {
    @Upsert
    suspend fun upsertSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): WorkoutSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseDefinition(exercise: ExerciseDefinitionEntity)

    @Query("SELECT * FROM exercise_definitions")
    fun getExerciseDefinitions(): Flow<List<ExerciseDefinitionEntity>>

    @Query("SELECT * FROM exercise_definitions WHERE familyId = :familyId")
    fun getExercisesByFamily(familyId: String): Flow<List<ExerciseDefinitionEntity>>

    @Upsert
    suspend fun upsertWorkoutLog(log: WorkoutLogEntity)

    @Upsert
    suspend fun upsertSetLog(set: SetLogEntity)

    @Transaction
    @Query("SELECT * FROM workout_logs WHERE sessionId = :sessionId ORDER BY `order` ASC")
    fun getLogsForSession(sessionId: String): Flow<List<WorkoutLogWithSets>>

    @Query("SELECT * FROM set_logs WHERE workoutLogId = :workoutLogId ORDER BY timestamp ASC")
    fun getSetsForLog(workoutLogId: String): Flow<List<SetLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserWorkoutProfileEntity)

    @Query("SELECT * FROM user_workout_profiles WHERE userId = :userId")
    fun getUserProfile(userId: String): Flow<UserWorkoutProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressionState(state: ProgressionStateEntity)

    @Query("SELECT * FROM progression_states WHERE exerciseId = :exerciseId")
    fun getProgressionState(exerciseId: String): Flow<ProgressionStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccomplishments(entity: ExerciseAccomplishmentsEntity)

    @Query("SELECT * FROM exercise_accomplishments WHERE exerciseId = :exerciseId")
    fun getAccomplishments(exerciseId: String): Flow<ExerciseAccomplishmentsEntity?>

    @Query("SELECT * FROM exercise_accomplishments")
    fun getAllAccomplishments(): Flow<List<ExerciseAccomplishmentsEntity>>

    @Transaction
    @Query("SELECT * FROM workout_routines")
    fun getAllRoutines(): Flow<List<WorkoutRoutineWithDetails>>

    @Query("SELECT isAddedToLibrary FROM workout_routines WHERE id = :id")
    suspend fun getRoutineLibraryStatus(id: String): Boolean?

    @Query("SELECT isAddedToLibrary FROM workout_augMENTS WHERE id = :id")
    suspend fun getAugmentLibraryStatus(id: String): Boolean?

    @Query("SELECT * FROM workout_routines WHERE id = :id")
    suspend fun getRoutineById(id: String): WorkoutRoutineEntity?

    @Query("SELECT * FROM workout_augments WHERE id = :id")
    suspend fun getAugmentById(id: String): WorkoutAugmentEntity?

    @Query("UPDATE workout_routines SET isAddedToLibrary = :status WHERE id = :id")
    suspend fun updateRoutineLibraryStatus(id: String, status: Boolean)

    @Query("UPDATE workout_augments SET isAddedToLibrary = :status WHERE id = :id")
    suspend fun updateAugmentLibraryStatus(id: String, status: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: WorkoutRoutineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExerciseCrossRef(crossRef: RoutineExerciseCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineAugmentCrossRef(crossRef: RoutineAugmentCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineSet(set: RoutineSetEntity)

    @Query("DELETE FROM routine_sets WHERE routineId = :routineId")
    suspend fun deleteRoutineSets(routineId: String)

    @Query("DELETE FROM routine_exercise_cross_ref WHERE routineId = :routineId")
    suspend fun deleteRoutineExerciseCrossRefs(routineId: String)

    @Query("DELETE FROM routine_augment_cross_ref WHERE routineId = :routineId")
    suspend fun deleteRoutineAugmentCrossRefs(routineId: String)

    @Transaction
    suspend fun insertFullRoutine(
        routine: WorkoutRoutineEntity,
        exerciseRefs: List<RoutineExerciseCrossRef>,
        routineSets: List<RoutineSetEntity>,
        augmentRefs: List<RoutineAugmentCrossRef>
    ) {
        deleteRoutineSets(routine.id)
        deleteRoutineExerciseCrossRefs(routine.id)
        deleteRoutineAugmentCrossRefs(routine.id)
        
        insertRoutine(routine)
        exerciseRefs.forEach { insertRoutineExerciseCrossRef(it) }
        routineSets.forEach { insertRoutineSet(it) }
        augmentRefs.forEach { insertRoutineAugmentCrossRef(it) }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAugment(augment: WorkoutAugmentEntity)

    @Transaction
    suspend fun insertFullAugment(
        augment: WorkoutAugmentEntity,
        exerciseRefs: List<AugmentExerciseCrossRef>,
        augmentSets: List<AugmentSetEntity>
    ) {
        deleteAugmentSets(augment.id)
        deleteAugmentExerciseCrossRefs(augment.id)
        
        insertAugment(augment)
        exerciseRefs.forEach { insertAugmentExerciseCrossRef(it) }
        augmentSets.forEach { insertAugmentSet(it) }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAugmentExerciseCrossRef(crossRef: AugmentExerciseCrossRef)

    @Query("DELETE FROM augment_exercise_cross_ref WHERE augmentId = :augmentId")
    suspend fun deleteAugmentExerciseCrossRefs(augmentId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAugmentSet(set: AugmentSetEntity)

    @Query("DELETE FROM augment_sets WHERE augmentId = :augmentId")
    suspend fun deleteAugmentSets(augmentId: String)

    @Transaction
    @Query("SELECT * FROM workout_augments")
    fun getAllAugments(): Flow<List<WorkoutAugmentWithExercises>>

    @Query("DELETE FROM workout_augments WHERE id = :augmentId")
    suspend fun deleteAugment(augmentId: String)

    @Query("DELETE FROM workout_routines WHERE id = :routineId")
    suspend fun deleteRoutine(routineId: String)

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Transaction
    @Query("""
        SELECT wl.* FROM workout_logs wl
        INNER JOIN workout_sessions ws ON wl.sessionId = ws.id
        WHERE wl.exerciseId = :exerciseId 
        AND wl.sessionId != :excludedSessionId
        ORDER BY ws.date DESC 
        LIMIT 1
    """)
    fun getLatestLogForExercise(exerciseId: String, excludedSessionId: String): Flow<WorkoutLogWithSets?>

    @Query("DELETE FROM set_logs WHERE id = :setLogId")
    suspend fun deleteSetLog(setLogId: String)

    @Query("DELETE FROM workout_logs WHERE id = :workoutLogId")
    suspend fun deleteWorkoutLog(workoutLogId: String)

    @Query("UPDATE workout_logs SET `order` = :newOrder WHERE id = :workoutLogId")
    suspend fun updateWorkoutLogOrder(workoutLogId: String, newOrder: Int)

    @Query("UPDATE workout_logs SET showGoalReps = :show WHERE id = :workoutLogId")
    suspend fun updateShowGoalReps(workoutLogId: String, show: Boolean)

    @Query("UPDATE workout_logs SET supersetId = :supersetId WHERE id = :workoutLogId")
    suspend fun updateSupersetId(workoutLogId: String, supersetId: String?)

    @Query("SELECT * FROM workout_sessions WHERE durationSeconds = 0 ORDER BY date DESC LIMIT 1")
    fun getActiveSession(): Flow<WorkoutSessionEntity?>

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    fun getAllSessionsWithDetails(): Flow<List<WorkoutSessionWithLogs>>

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE date BETWEEN :from AND :to")
    fun countSessionsBetween(from: Instant, to: Instant): Flow<Int>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    fun getSessionsWithDetailsBetween(from: Instant, to: Instant): Flow<List<WorkoutSessionWithLogs>>

    @Query("SELECT date, isDeload FROM workout_sessions WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun getSessionDatesAndDeloadBetween(from: Instant, to: Instant): Flow<List<SessionDateAndDeload>>

    @Query("""
        SELECT ed.muscleGroups 
        FROM workout_sessions ws
        JOIN workout_logs wl ON ws.id = wl.sessionId
        JOIN exercise_definitions ed ON wl.exerciseId = ed.id
        WHERE ws.date BETWEEN :from AND :to
        AND EXISTS (SELECT 1 FROM set_logs WHERE workoutLogId = wl.id AND isCompleted = 1)
    """)
    fun getMuscleGroupsHitBetween(from: Instant, to: Instant): Flow<List<MuscleGroupsWrapper>>

    @Transaction
    @Query("""
        SELECT wl.* FROM workout_logs wl
        INNER JOIN workout_sessions ws ON wl.sessionId = ws.id
        WHERE wl.exerciseId = :exerciseId 
        AND ws.date BETWEEN :from AND :to
        ORDER BY ws.date DESC
    """)
    fun getLogsForExerciseBetween(exerciseId: String, from: Instant, to: Instant): Flow<List<WorkoutLogWithSets>>

    @Upsert
    suspend fun upsertFuelSnapshot(snapshot: FuelSnapshotEntity)

    @Query("SELECT * FROM fuel_snapshots WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp ASC")
    fun getFuelHistory(from: Instant, to: Instant): Flow<List<FuelSnapshotEntity>>

    @Upsert
    suspend fun upsertProtocolRepTarget(target: ProtocolRepTargetEntity)

    @Query("SELECT * FROM protocol_rep_targets")
    fun getAllProtocolRepTargets(): Flow<List<ProtocolRepTargetEntity>>
}

data class SessionDateAndDeload(
    val date: Instant,
    val isDeload: Boolean
)

data class MuscleGroupsWrapper(
    val muscleGroups: List<String>
)

data class WorkoutSessionWithLogs(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        entity = WorkoutLogEntity::class,
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val logs: List<WorkoutLogWithSets>
)

data class WorkoutLogWithSets(
    @Embedded val log: WorkoutLogEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutLogId"
    )
    val sets: List<SetLogEntity>
)

data class WorkoutRoutineWithDetails(
    @Embedded val routine: WorkoutRoutineEntity,
    @Relation(
        entity = RoutineExerciseCrossRef::class,
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val exercisesWithOrder: List<RoutineExerciseWithOrder>,
    @Relation(
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val routineSets: List<RoutineSetEntity>,
    @Relation(
        entity = RoutineAugmentCrossRef::class,
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val augmentsWithOrder: List<RoutineAugmentWithOrder>
)

data class RoutineExerciseWithOrder(
    @Embedded val ref: RoutineExerciseCrossRef,
    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "id"
    )
    val exercise: ExerciseDefinitionEntity
)

data class RoutineAugmentWithOrder(
    @Embedded val ref: RoutineAugmentCrossRef,
    @Relation(
        entity = WorkoutAugmentEntity::class,
        parentColumn = "augmentId",
        entityColumn = "id"
    )
    val augmentDetails: WorkoutAugmentWithExercises
)

data class WorkoutAugmentWithExercises(
    @Embedded val augment: WorkoutAugmentEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "augmentId"
    )
    val exerciseRefs: List<AugmentExerciseCrossRef>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = AugmentExerciseCrossRef::class,
            parentColumn = "augmentId",
            entityColumn = "exerciseId"
        )
    )
    val exercises: List<ExerciseDefinitionEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "augmentId"
    )
    val augmentSets: List<AugmentSetEntity>
)
