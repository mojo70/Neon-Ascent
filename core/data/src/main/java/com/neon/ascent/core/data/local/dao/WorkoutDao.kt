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
}

data class WorkoutLogWithSets(
    @Embedded val log: WorkoutLogEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutLogId"
    )
    val sets: List<SetLogEntity>
)
