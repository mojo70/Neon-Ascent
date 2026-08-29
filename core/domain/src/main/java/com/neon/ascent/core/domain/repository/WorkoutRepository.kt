package com.neon.ascent.core.domain.repository

import com.neon.ascent.core.domain.workout.models.*
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun getAllSessions(): Flow<List<WorkoutSession>>
    suspend fun getSessionById(id: String): WorkoutSession?
    suspend fun saveSession(session: WorkoutSession)
    suspend fun exportHistoryToJson(): String
    
    fun getExerciseDefinitions(): Flow<List<Exercise>>
    suspend fun getExerciseById(id: String): Exercise?
    suspend fun saveExerciseDefinition(exercise: Exercise)

    fun getExerciseFamilies(): Flow<List<ExerciseFamily>>
    fun getExercisesByFamily(familyId: String): Flow<List<Exercise>>
    
    fun getLogsForSession(sessionId: String): Flow<List<Pair<WorkoutLog, List<SetLog>>>>
    suspend fun saveWorkoutLog(log: WorkoutLog)
    suspend fun saveSetLog(set: SetLog)
    
    fun getUserProfile(userId: String): Flow<UserWorkoutProfile?>
    suspend fun saveUserProfile(profile: UserWorkoutProfile)

    fun getAllRoutines(): Flow<List<WorkoutRoutine>>
    suspend fun saveRoutine(routine: WorkoutRoutine)
    suspend fun deleteRoutine(routineId: String)

    fun getAllAugments(): Flow<List<WorkoutAugment>>
    suspend fun saveAugment(augment: WorkoutAugment)
    suspend fun deleteAugment(augmentId: String)

    fun getAugmentActivations(userId: String): Flow<List<AugmentActivation>>
    fun getActiveAugmentActivations(): Flow<List<AugmentActivation>>
    suspend fun saveAugmentActivation(activation: AugmentActivation)
    suspend fun endAugmentActivation(id: String)

    suspend fun deleteSession(sessionId: String)
    
    fun getLatestSetsForExercise(exerciseId: String, excludedSessionId: String): Flow<List<SetLog>>

    fun getFullHistory(): Flow<List<Pair<WorkoutSession, List<Pair<WorkoutLog, List<SetLog>>>>>>

    fun countSessionsBetween(from: java.time.Instant, to: java.time.Instant): Flow<Int>
    fun getSessionsBetween(from: java.time.Instant, to: java.time.Instant): Flow<List<Pair<WorkoutSession, List<Pair<WorkoutLog, List<SetLog>>>>>>
    fun getSessionDatesAndDeloadBetween(from: java.time.Instant, to: java.time.Instant): Flow<List<Pair<java.time.Instant, Boolean>>>
    fun getMuscleGroupsHitBetween(from: java.time.Instant, to: java.time.Instant): Flow<List<String>>
    fun getLogsForExerciseBetween(exerciseId: String, from: java.time.Instant, to: java.time.Instant): Flow<List<Pair<WorkoutLog, List<SetLog>>>>

    fun getActiveSession(): Flow<WorkoutSession?>

    fun getRecoveryScore(): Flow<RecoveryScore>

    fun getProgressionState(exerciseId: String): Flow<ProgressionState?>
    suspend fun saveProgressionState(state: ProgressionState)

    fun getAccomplishments(exerciseId: String): Flow<ExerciseAccomplishments?>
    fun getAllAccomplishments(): Flow<List<ExerciseAccomplishments>>
    suspend fun saveAccomplishments(accomplishments: ExerciseAccomplishments)

    suspend fun deleteWorkoutLog(workoutLogId: String)
    suspend fun updateWorkoutLogOrder(workoutLogId: String, newOrder: Int)
    suspend fun updateShowGoalReps(workoutLogId: String, show: Boolean)
    suspend fun updateSupersetId(workoutLogId: String, supersetId: String?)
    suspend fun deleteSetLog(setLogId: String)

    fun getFuelHistory(from: java.time.Instant, to: java.time.Instant): Flow<List<FuelSnapshot>>
    suspend fun saveFuelSnapshot(snapshot: FuelSnapshot)

    fun getProtocolRepTargets(): Flow<List<ProtocolRepTarget>>
    suspend fun saveProtocolRepTarget(target: ProtocolRepTarget)

    fun getActiveCycle(userId: String): Flow<ProtocolCycle?>
    suspend fun saveProtocolCycle(cycle: ProtocolCycle)

    fun getExerciseMax(familyId: String): Flow<ExerciseMax?>
    fun getAllExerciseMaxes(): Flow<List<ExerciseMax>>
    suspend fun upsertExerciseMax(max: ExerciseMax)

    suspend fun seedStarterExercises()
}
