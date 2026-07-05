package com.neon.ascent.core.domain.repository

import com.neon.ascent.core.domain.workout.models.*
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun getAllSessions(): Flow<List<WorkoutSession>>
    suspend fun getSessionById(id: String): WorkoutSession?
    suspend fun saveSession(session: WorkoutSession)
    
    fun getExerciseDefinitions(): Flow<List<Exercise>>
    suspend fun saveExerciseDefinition(exercise: Exercise)
    
    fun getLogsForSession(sessionId: String): Flow<List<Pair<WorkoutLog, List<SetLog>>>>
    suspend fun saveWorkoutLog(log: WorkoutLog)
    suspend fun saveSetLog(set: SetLog)
    
    fun getUserProfile(userId: String): Flow<UserWorkoutProfile?>
    suspend fun saveUserProfile(profile: UserWorkoutProfile)

    fun getAllRoutines(): Flow<List<WorkoutRoutine>>
    suspend fun saveRoutine(routine: WorkoutRoutine)

    suspend fun deleteSession(sessionId: String)

    suspend fun seedStarterExercises()
}
