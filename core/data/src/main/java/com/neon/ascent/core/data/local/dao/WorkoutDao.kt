package com.neon.ascent.core.data.local.dao

import androidx.room.*
import com.neon.ascent.core.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): WorkoutSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseDefinition(exercise: ExerciseDefinitionEntity)

    @Query("SELECT * FROM exercise_definitions")
    fun getExerciseDefinitions(): Flow<List<ExerciseDefinitionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutLog(log: WorkoutLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetLog(set: SetLogEntity)

    @Transaction
    @Query("SELECT * FROM workout_logs WHERE sessionId = :sessionId ORDER BY `order` ASC")
    fun getLogsForSession(sessionId: String): Flow<List<WorkoutLogWithSets>>

    @Query("SELECT * FROM set_logs WHERE workoutLogId = :workoutLogId ORDER BY timestamp ASC")
    fun getSetsForLog(workoutLogId: String): Flow<List<SetLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserWorkoutProfileEntity)

    @Query("SELECT * FROM user_workout_profiles WHERE userId = :userId")
    fun getUserProfile(userId: String): Flow<UserWorkoutProfileEntity?>

    @Transaction
    @Query("SELECT * FROM workout_routines")
    fun getAllRoutines(): Flow<List<WorkoutRoutineWithExercises>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: WorkoutRoutineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExerciseCrossRef(crossRef: RoutineExerciseCrossRef)

    @Query("DELETE FROM workout_routines WHERE id = :routineId")
    suspend fun deleteRoutine(routineId: String)

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Transaction
    @Query("""
        SELECT * FROM workout_logs 
        WHERE exerciseId = :exerciseId 
        AND sessionId != :excludedSessionId
        ORDER BY (SELECT date FROM workout_sessions WHERE id = sessionId) DESC 
        LIMIT 1
    """)
    fun getLatestLogForExercise(exerciseId: String, excludedSessionId: String): Flow<WorkoutLogWithSets?>

    @Query("DELETE FROM set_logs WHERE id = :setLogId")
    suspend fun deleteSetLog(setLogId: String)

    @Query("DELETE FROM workout_logs WHERE id = :workoutLogId")
    suspend fun deleteWorkoutLog(workoutLogId: String)

    @Query("UPDATE workout_logs SET `order` = :newOrder WHERE id = :workoutLogId")
    suspend fun updateWorkoutLogOrder(workoutLogId: String, newOrder: Int)

    @Query("SELECT * FROM workout_sessions WHERE durationSeconds = 0 ORDER BY date DESC LIMIT 1")
    fun getActiveSession(): Flow<WorkoutSessionEntity?>

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    fun getAllSessionsWithDetails(): Flow<List<WorkoutSessionWithLogs>>
}

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

data class WorkoutRoutineWithExercises(
    @Embedded val routine: WorkoutRoutineEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = RoutineExerciseCrossRef::class,
            parentColumn = "routineId",
            entityColumn = "exerciseId"
        )
    )
    val exercises: List<ExerciseDefinitionEntity>
)
