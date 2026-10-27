package com.neon.ascent.data.repository

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.neon.ascent.core.data.NeonAscentDatabase
import com.neon.ascent.core.data.local.dao.*
import com.neon.ascent.core.data.local.entity.*
import com.neon.ascent.core.data.mapper.toDomain
import com.neon.ascent.core.domain.backup.models.BackupScope
import com.neon.ascent.core.domain.backup.models.RestoreMode
import com.neon.ascent.core.domain.workout.rules.ExerciseRanking
import com.neon.ascent.core.domain.chronicle.ChronicleEntry
import com.neon.ascent.core.domain.chronicle.ChronicleRepository
import com.neon.ascent.core.domain.library.models.LibraryBook
import com.neon.ascent.core.domain.library.models.LibraryChapter
import com.neon.ascent.core.domain.library.models.LibraryHighlight
import com.neon.ascent.core.domain.library.models.LibraryQuote
import com.neon.ascent.core.domain.library.repository.LibraryRepository
import com.neon.ascent.core.domain.model.EnergyLevel
import com.neon.ascent.data.local.*
import com.neon.ascent.data.local.GoalDao
import com.neon.ascent.data.local.entity.GoalEntity
import com.neon.ascent.data.local.entity.TaskEntity
import com.neon.ascent.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant
import java.time.LocalDate

class FullDataBackupRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class FakeWorkoutDao : WorkoutDao {
        val sessions = mutableMapOf<String, WorkoutSessionEntity>()
        val logs = mutableMapOf<String, WorkoutLogEntity>()
        val sets = mutableMapOf<String, SetLogEntity>()
        val exercises = mutableMapOf<String, ExerciseDefinitionEntity>()
        val maxes = mutableMapOf<String, ExerciseMaxEntity>()
        val profiles = mutableMapOf<String, UserWorkoutProfileEntity>()
        val protocolCycles = mutableMapOf<String, ProtocolCycleEntity>()
        val progressionStates = mutableMapOf<String, ProgressionStateEntity>()
        val routineExerciseCrossRefs = mutableListOf<RoutineExerciseCrossRef>()
        val routineSets = mutableListOf<RoutineSetEntity>()
        val routineAugmentCrossRefs = mutableListOf<RoutineAugmentCrossRef>()
        val augments = mutableMapOf<String, WorkoutAugmentEntity>()
        val augmentExerciseCrossRefs = mutableListOf<AugmentExerciseCrossRef>()
        val augmentSets = mutableListOf<AugmentSetEntity>()
        val augmentActivations = mutableMapOf<String, AugmentActivationEntity>()
        val routines = mutableMapOf<String, WorkoutRoutineEntity>()
        val accomplishments = mutableMapOf<String, ExerciseAccomplishmentsEntity>()
        val repTargets = mutableMapOf<String, ProtocolRepTargetEntity>()

        override suspend fun upsertSession(session: WorkoutSessionEntity) {
            sessions[session.id] = session
        }

        override fun getStalledProgressionStates(): Flow<List<com.neon.ascent.core.data.local.entity.ProgressionStateEntity>> = flowOf(emptyList())

        override fun getAllSessions(): Flow<List<WorkoutSessionEntity>> = flowOf(sessions.values.toList())
        override suspend fun getSessionById(sessionId: String): WorkoutSessionEntity? = sessions[sessionId]

        override suspend fun insertExerciseDefinition(exercise: ExerciseDefinitionEntity) {
            exercises[exercise.id] = exercise
        }

        override suspend fun deleteCustomExerciseDefinition(id: String) {
            exercises.remove(id)
        }

        override fun getExerciseDefinitions(): Flow<List<ExerciseDefinitionEntity>> = flowOf(exercises.values.toList())
        override fun getExercisesByFamily(familyId: String): Flow<List<ExerciseDefinitionEntity>> = flowOf(exercises.values.filter { it.familyId == familyId })

        override suspend fun upsertWorkoutLog(log: WorkoutLogEntity) {
            logs[log.id] = log
        }

        override suspend fun upsertSetLog(set: SetLogEntity) {
            sets[set.id] = set
        }

        override fun getLogsForSession(sessionId: String): Flow<List<WorkoutLogWithSets>> {
            val sessionLogs = logs.values.filter { it.sessionId == sessionId }.map { log ->
                WorkoutLogWithSets(log, sets.values.filter { it.workoutLogId == log.id })
            }
            return flowOf(sessionLogs)
        }

        override fun getSetsForLog(workoutLogId: String): Flow<List<SetLogEntity>> = flowOf(sets.values.filter { it.workoutLogId == workoutLogId })

        override suspend fun insertUserProfile(profile: UserWorkoutProfileEntity) {
            profiles[profile.userId] = profile
        }

        override fun getUserProfile(userId: String): Flow<UserWorkoutProfileEntity?> = flowOf(profiles[userId])
        override suspend fun deleteUserProfile(userId: String) { profiles.remove(userId) }

        val deloadWindows = mutableMapOf<String, DeloadWindowEntity>()

        override suspend fun insertDeloadWindow(window: DeloadWindowEntity) {
            deloadWindows[window.id] = window
        }

        override fun getActiveDeloadWindow(userId: String): Flow<DeloadWindowEntity?> =
            flowOf(deloadWindows.values.lastOrNull { it.userId == userId && it.status == "ACTIVE" })

        override fun getLatestDeloadWindow(userId: String): Flow<DeloadWindowEntity?> =
            flowOf(deloadWindows.values.lastOrNull { it.userId == userId })

        override fun getAllDeloadWindows(): Flow<List<DeloadWindowEntity>> =
            flowOf(deloadWindows.values.toList())

        override suspend fun updateActiveDeloadWindowStatus(userId: String, status: String) {
            deloadWindows.values.filter { it.userId == userId && it.status == "ACTIVE" }.forEach {
                deloadWindows[it.id] = it.copy(status = status)
            }
        }

        override suspend fun endActiveDeloadWindow(userId: String, endedAt: Instant) {
            deloadWindows.values.filter { it.userId == userId && it.status == "ACTIVE" }.forEach {
                deloadWindows[it.id] = it.copy(end = endedAt, status = "COMPLETED")
            }
        }

        override suspend fun insertProgressionState(state: ProgressionStateEntity) {
            progressionStates[state.exerciseId] = state
        }
        override fun getProgressionState(exerciseId: String): Flow<ProgressionStateEntity?> = flowOf(progressionStates[exerciseId])

        override suspend fun insertAccomplishments(entity: ExerciseAccomplishmentsEntity) {
            accomplishments[entity.exerciseId] = entity
        }
        override fun getAccomplishments(exerciseId: String): Flow<ExerciseAccomplishmentsEntity?> = flowOf(accomplishments[exerciseId])
        override fun getAllAccomplishments(): Flow<List<ExerciseAccomplishmentsEntity>> = flowOf(accomplishments.values.toList())

        override fun getAllRoutines(): Flow<List<WorkoutRoutineWithDetails>> = flowOf(routines.values.map { r ->
            WorkoutRoutineWithDetails(r, emptyList(), emptyList(), emptyList())
        })
        override suspend fun getRoutineLibraryStatus(id: String): Boolean? = routines[id]?.isAddedToLibrary
        override suspend fun getAugmentLibraryStatus(id: String): Boolean? = augments[id]?.isAddedToLibrary
        override suspend fun getRoutineById(id: String): WorkoutRoutineEntity? = routines[id]
        override suspend fun getAugmentById(id: String): WorkoutAugmentEntity? = augments[id]
        override suspend fun updateRoutineLibraryStatus(id: String, status: Boolean) {}
        override suspend fun updateAugmentLibraryStatus(id: String, status: Boolean) {}
        override suspend fun insertRoutine(routine: WorkoutRoutineEntity) {
            routines[routine.id] = routine
        }
        override suspend fun insertRoutineExerciseCrossRef(crossRef: RoutineExerciseCrossRef) {
            routineExerciseCrossRefs.add(crossRef)
        }
        override suspend fun insertRoutineAugmentCrossRef(crossRef: RoutineAugmentCrossRef) {
            routineAugmentCrossRefs.add(crossRef)
        }
        override suspend fun insertRoutineSet(set: RoutineSetEntity) {
            routineSets.add(set)
        }
        override suspend fun deleteRoutineSets(routineId: String) { routineSets.removeAll { it.routineId == routineId } }
        override suspend fun deleteRoutineExerciseCrossRefs(routineId: String) { routineExerciseCrossRefs.removeAll { it.routineId == routineId } }
        override suspend fun deleteRoutineAugmentCrossRefs(routineId: String) { routineAugmentCrossRefs.removeAll { it.routineId == routineId } }
        override suspend fun insertAugment(augment: WorkoutAugmentEntity) {
            augments[augment.id] = augment
        }
        override suspend fun insertAugmentExerciseCrossRef(crossRef: AugmentExerciseCrossRef) {
            augmentExerciseCrossRefs.add(crossRef)
        }
        override suspend fun deleteAugmentExerciseCrossRefs(augmentId: String) { augmentExerciseCrossRefs.removeAll { it.augmentId == augmentId } }
        override suspend fun insertAugmentSet(set: AugmentSetEntity) {
            augmentSets.add(set)
        }
        override suspend fun deleteAugmentSets(augmentId: String) { augmentSets.removeAll { it.augmentId == augmentId } }
        override fun getAllAugments(): Flow<List<WorkoutAugmentWithExercises>> = flowOf(augments.values.map { a ->
            WorkoutAugmentWithExercises(a, emptyList(), emptyList(), emptyList())
        })
        override suspend fun deleteAugment(augmentId: String) { augments.remove(augmentId) }
        override suspend fun upsertAugmentActivation(activation: AugmentActivationEntity) {
            augmentActivations[activation.id] = activation
        }
        override fun getActivationsByUserId(userId: String): Flow<List<AugmentActivationEntity>> = flowOf(augmentActivations.values.filter { it.userId == userId })
        override fun getActiveActivations(): Flow<List<AugmentActivationEntity>> = flowOf(augmentActivations.values.filter { it.status == "ACTIVE" })
        override suspend fun endActivation(id: String) {}
        override suspend fun deleteRoutine(routineId: String) { routines.remove(routineId) }
        override suspend fun deleteSession(sessionId: String) { sessions.remove(sessionId) }
        override fun getLatestLogForExercise(exerciseId: String, excludedSessionId: String): Flow<WorkoutLogWithSets?> = flowOf(null)
        override suspend fun deleteSetLog(setLogId: String) { sets.remove(setLogId) }
        override suspend fun deleteWorkoutLog(workoutLogId: String) { logs.remove(workoutLogId) }
        override suspend fun updateWorkoutLogOrder(workoutLogId: String, newOrder: Int) {}
        override suspend fun updateShowGoalReps(workoutLogId: String, show: Boolean) {}
        override suspend fun updateSupersetId(workoutLogId: String, supersetId: String?) {}
        override fun getActiveSession(): Flow<WorkoutSessionEntity?> = flowOf(null)

        override fun getAllSessionsWithDetails(): Flow<List<WorkoutSessionWithLogs>> {
            val result = sessions.values.map { session ->
                val sessionLogs = logs.values.filter { it.sessionId == session.id }.map { log ->
                    WorkoutLogWithSets(log, sets.values.filter { it.workoutLogId == log.id })
                }
                WorkoutSessionWithLogs(session, sessionLogs)
            }
            return flowOf(result)
        }

        override fun getSessionWithDetails(sessionId: String): Flow<WorkoutSessionWithLogs?> {
            val session = sessions[sessionId] ?: return flowOf(null)
            val sessionLogs = logs.values.filter { it.sessionId == session.id }.map { log ->
                WorkoutLogWithSets(log, sets.values.filter { it.workoutLogId == log.id })
            }
            return flowOf(WorkoutSessionWithLogs(session, sessionLogs))
        }

        override fun countSessionsBetween(from: Instant, to: Instant): Flow<Int> = flowOf(sessions.size)
        override fun getSessionsWithDetailsBetween(from: Instant, to: Instant): Flow<List<WorkoutSessionWithLogs>> = flowOf(emptyList())
        override fun getSessionDatesAndDeloadBetween(from: Instant, to: Instant): Flow<List<SessionDateAndDeload>> = flowOf(emptyList())
        override fun getMuscleGroupsHitBetween(from: Instant, to: Instant): Flow<List<MuscleGroupsWrapper>> = flowOf(emptyList())
        override fun getLogsForExerciseBetween(exerciseId: String, from: Instant, to: Instant): Flow<List<WorkoutLogWithSets>> = flowOf(emptyList())
        override suspend fun upsertFuelSnapshot(snapshot: FuelSnapshotEntity) {}
        override fun getFuelHistory(from: Instant, to: Instant): Flow<List<FuelSnapshotEntity>> = flowOf(emptyList())
        override suspend fun upsertProtocolRepTarget(target: ProtocolRepTargetEntity) {
            repTargets[target.id] = target
        }
        override fun getAllProtocolRepTargets(): Flow<List<ProtocolRepTargetEntity>> = flowOf(repTargets.values.toList())
        override suspend fun upsertProtocolCycle(cycle: ProtocolCycleEntity) {
            protocolCycles[cycle.id] = cycle
        }
        override fun getActiveCycle(userId: String): Flow<ProtocolCycleEntity?> = flowOf(protocolCycles.values.firstOrNull { it.userId == userId && it.status == "ACTIVE" })
        override suspend fun getProtocolCycleById(id: String): ProtocolCycleEntity? = protocolCycles[id]

        override suspend fun upsertExerciseMax(max: ExerciseMaxEntity) {
            maxes[max.familyId] = max
        }

        override fun getExerciseMax(familyId: String): Flow<ExerciseMaxEntity?> = flowOf(maxes[familyId])
        override fun getAllExerciseMaxes(): Flow<List<ExerciseMaxEntity>> = flowOf(maxes.values.toList())
        override fun getAllUserProfiles(): Flow<List<UserWorkoutProfileEntity>> = flowOf(profiles.values.toList())

        override fun getAllProtocolCycles(): Flow<List<ProtocolCycleEntity>> = flowOf(protocolCycles.values.toList())
        override fun getAllProgressionStates(): Flow<List<ProgressionStateEntity>> = flowOf(progressionStates.values.toList())
        override fun getAllRoutineExerciseCrossRefs(): Flow<List<RoutineExerciseCrossRef>> = flowOf(routineExerciseCrossRefs.toList())
        override fun getAllRoutineSets(): Flow<List<RoutineSetEntity>> = flowOf(routineSets.toList())
        override fun getAllRoutineAugmentCrossRefs(): Flow<List<RoutineAugmentCrossRef>> = flowOf(routineAugmentCrossRefs.toList())
        override fun getAllAugmentExerciseCrossRefs(): Flow<List<AugmentExerciseCrossRef>> = flowOf(augmentExerciseCrossRefs.toList())
        override fun getAllAugmentSets(): Flow<List<AugmentSetEntity>> = flowOf(augmentSets.toList())
        override fun getAllAugmentActivations(): Flow<List<AugmentActivationEntity>> = flowOf(augmentActivations.values.toList())
    }

    private class FakeBiomarkerDao : BiomarkerDao {
        val samples = mutableListOf<BiomarkerSampleEntity>()
        override suspend fun upsertSample(sample: BiomarkerSampleEntity) { samples.add(sample) }
        override suspend fun deleteSample(id: String) { samples.removeAll { it.id == id } }
        override fun getSamplesForMarker(markerKey: String): Flow<List<BiomarkerSampleEntity>> = flowOf(samples.filter { it.markerKey == markerKey })
        override fun getAllSamplesBetween(from: Instant, to: Instant): Flow<List<BiomarkerSampleEntity>> = flowOf(samples)
        override fun getLatestPerMarker(): Flow<List<BiomarkerSampleEntity>> = flowOf(samples)
    }

    private class FakeDailyVitalRollupDao : DailyVitalRollupDao {
        override suspend fun upsert(rollup: DailyVitalRollupEntity) {}
        override suspend fun upsertAll(rollups: List<DailyVitalRollupEntity>) {}
        override fun getRange(metric: String, fromDate: String, toDate: String): Flow<List<DailyVitalRollupEntity>> = flowOf(emptyList())
        override suspend fun getRangeList(metric: String, fromDate: String, toDate: String): List<DailyVitalRollupEntity> = emptyList()
        override fun getTapeRollups(fromDate: String, toDate: String): Flow<List<DailyVitalRollupEntity>> = flowOf(emptyList())
        override fun getDay(localDate: String): Flow<List<DailyVitalRollupEntity>> = flowOf(emptyList())
        override suspend fun getAllRollupsSince(fromDate: String): List<DailyVitalRollupEntity> = emptyList()
    }

    private class FakeSpecialDao : SpecialDao {
        val specialAttributes = mutableListOf<SpecialAttributeEntity>()
        override fun getAllSpecialAttributes(): Flow<List<SpecialAttributeEntity>> = flowOf(specialAttributes)
        override fun getSpecialAttribute(type: String): Flow<SpecialAttributeEntity?> = flowOf(specialAttributes.firstOrNull { it.type.name == type })
        override suspend fun upsertSpecialAttribute(entity: SpecialAttributeEntity) { specialAttributes.add(entity) }
        override suspend fun insertBenchmark(test: BenchmarkTestEntity) {}
        override fun getBenchmarkHistory(attribute: String): Flow<List<BenchmarkTestEntity>> = flowOf(emptyList())
        override suspend fun deleteBenchmarkHistory(attribute: String) {}
        override suspend fun deleteOldBenchmarks(olderThan: Instant) {}
        override suspend fun deleteAllSpecialAttributes() { specialAttributes.clear() }
        override suspend fun deleteAllBenchmarks() {}
    }

    private class FakeDopamineMenuDao : DopamineMenuDao {
        val items = mutableListOf<DopamineMenuItemEntity>()
        override fun getAllItems(): Flow<List<DopamineMenuItemEntity>> = flowOf(items)
        override fun getItemsByEnergyLevel(energyLevel: EnergyLevel): Flow<List<DopamineMenuItemEntity>> = flowOf(items.filter { it.energyLevel == energyLevel })
        override suspend fun getItemById(id: String): DopamineMenuItemEntity? = items.firstOrNull { it.id == id }
        override suspend fun getItemCount(): Int = items.size
        override suspend fun upsertItem(item: DopamineMenuItemEntity) { items.add(item) }
        override suspend fun deleteItem(item: DopamineMenuItemEntity) { items.remove(item) }
        override suspend fun logUsage(id: String, timestamp: Instant) {}
    }

    private class FakeBiohackingDao : BiohackingDao {
        override fun getBiohackingData(userId: Int): Flow<BiohackingData?> = flowOf(null)
        override suspend fun insertOrUpdate(data: BiohackingData) {}
        override suspend fun insertProtocolLog(log: BioProtocolLog) {}
        override fun getProtocolLogs(userId: Int): Flow<List<BioProtocolLog>> = flowOf(emptyList())
        override fun getAllProtocolLogs(): Flow<List<BioProtocolLog>> = flowOf(emptyList())
        override suspend fun deleteBiohackingData(userId: Int) {}
        override suspend fun deleteBioProtocolLogs(userId: Int) {}
    }

    private class FakeBookDao : BookDao {
        val quotes = mutableListOf<QuoteEntity>()
        override suspend fun getBookById(bookId: String): BookEntity? = null
        override suspend fun getChaptersForBook(bookId: String): List<ChapterEntity> = emptyList()
        override suspend fun insertBook(book: BookEntity) {}
        override suspend fun insertChapters(chapters: List<ChapterEntity>) {}
        override fun getHighlightsForBook(bookId: String): Flow<List<HighlightEntity>> = flowOf(emptyList())
        override suspend fun insertHighlight(highlight: HighlightEntity) {}
        override suspend fun deleteHighlight(highlight: HighlightEntity) {}
        override suspend fun deleteHighlightsForBook(bookId: String) {}
        override suspend fun insertQuote(quote: QuoteEntity) { quotes.add(quote) }
        override fun getAllQuotes(): Flow<List<QuoteEntity>> = flowOf(quotes)
    }

    private class FakeLibraryRepository : LibraryRepository {
        override suspend fun getBookById(id: String): LibraryBook? = null
        override suspend fun getChaptersForBook(bookId: String): List<LibraryChapter> = emptyList()
        override suspend fun upsertBook(book: LibraryBook, chapters: List<LibraryChapter>) {}
        override fun getHighlightsForBook(bookId: String): Flow<List<LibraryHighlight>> = flowOf(emptyList())
        override suspend fun upsertHighlight(highlight: LibraryHighlight) {}
        override suspend fun deleteHighlight(highlight: LibraryHighlight) {}
        override fun getAllQuotes(): Flow<List<LibraryQuote>> = flowOf(emptyList())
        override suspend fun upsertQuote(quote: LibraryQuote) {}
    }

    private class FakeJournalDao : JournalDao {
        val entries = mutableMapOf<String, JournalEntry>()
        override fun getAllEntries(): Flow<List<JournalEntry>> = flowOf(entries.values.toList())
        override suspend fun insertEntry(entry: JournalEntry) { entries[entry.id] = entry }
        override suspend fun deleteEntry(entry: JournalEntry) { entries.remove(entry.id) }
        override suspend fun toggleHeart(id: String, isHearted: Boolean) {}
        override suspend fun exists(id: String): Boolean = entries.containsKey(id)
    }

    private class FakeDailyPrayerDao : DailyPrayerDao {
        val prayers = mutableListOf<DailyPrayer>()
        override suspend fun getPrayerForDay(day: Int): DailyPrayer? = prayers.firstOrNull { it.day == day }
        override fun getAllPrayers(): Flow<List<DailyPrayer>> = flowOf(prayers)
        override suspend fun insertPrayers(prayers: List<DailyPrayer>) { this.prayers.addAll(prayers) }
    }

    private class FakeLoreDao : LoreDao {
        override fun getAllCorpoTrust(): Flow<List<CorpoTrust>> = flowOf(emptyList())
        override fun getCorpoTrust(corpoId: String): Flow<CorpoTrust?> = flowOf(null)
        override suspend fun insertCorpoTrust(trust: CorpoTrust) {}
        override suspend fun updateTrustLevel(corpoId: String, level: Float) {}
        override fun getAllDataShards(): Flow<List<DataShard>> = flowOf(emptyList())
        override suspend fun insertDataShard(shard: DataShard) {}
        override suspend fun updateDataShard(shard: DataShard) {}
        override suspend fun updateShardDecrypted(shardId: String, isDecrypted: Boolean) {}
        override fun getAllMemoryFragments(): Flow<List<MemoryFragment>> = flowOf(emptyList())
        override suspend fun insertMemoryFragment(fragment: MemoryFragment) {}
        override suspend fun updateMemoryFragment(fragment: MemoryFragment) {}
    }

    private class FakeUserCharacterDao : UserCharacterDao {
        var userCharacter: UserCharacter? = null
        override fun getUserCharacter(): Flow<UserCharacter?> = flowOf(userCharacter)
        override suspend fun insertUserCharacter(userCharacter: UserCharacter) { this.userCharacter = userCharacter }
        override suspend fun updateUserCharacter(userCharacter: UserCharacter) { this.userCharacter = userCharacter }
        override suspend fun resetCharacter() { userCharacter = null }
        override suspend fun updateHolyGhost(level: Int) {}
        override suspend fun addHolyGhostExp(points: Int) {}
        override suspend fun updatePrayerStats(streak: Int, date: Long) {}
        override suspend fun updateWaterBaptized(isBaptized: Boolean) {}
        override suspend fun updateHolySpiritBaptized(isBaptized: Boolean) {}
        override suspend fun updateTonguesAura(hasAura: Boolean) {}
        override suspend fun updateEddies(eddies: Int) {}
        override suspend fun updateExperience(xp: Long) {}
        override suspend fun updateIceLevel(iceLevel: Int) {}
        override suspend fun setHasBreached() {}
        override suspend fun setSystemDatabaseUnlocked(unlocked: Boolean) {}
    }

    private class FakeCoreGoalDao : com.neon.ascent.core.data.local.dao.GoalDao {
        override fun getAllGoals(): Flow<List<com.neon.ascent.core.data.GoalEntity>> = flowOf(emptyList())
        override fun getAllHabits(): Flow<List<com.neon.ascent.core.data.GoalEntity>> = flowOf(emptyList())
        override fun getActiveMissions(now: Long): Flow<List<com.neon.ascent.core.data.GoalEntity>> = flowOf(emptyList())
        override fun getGoalById(id: String): Flow<com.neon.ascent.core.data.GoalEntity?> = flowOf(null)
        override fun getAllAspirations(): Flow<List<com.neon.ascent.core.data.GoalEntity>> = flowOf(emptyList())
        override fun getMissionsForAspiration(aspirationId: String): Flow<List<com.neon.ascent.core.data.GoalEntity>> = flowOf(emptyList())
        override fun getHabitsForMission(missionId: String): Flow<List<com.neon.ascent.core.data.GoalEntity>> = flowOf(emptyList())
        override suspend fun insertGoal(goal: com.neon.ascent.core.data.GoalEntity) {}
        override suspend fun updateGoal(goal: com.neon.ascent.core.data.GoalEntity) {}
        override suspend fun incrementHabitStreak(habitId: String, timestamp: Long) {}
        override suspend fun deleteGoal(id: String) {}
        override suspend fun updateProgress(id: String, progress: Double) {}
        override suspend fun markCompleted(id: String) {}
    }

    private class FakeGoalTaskDao : GoalTaskDao {
        override fun getTasksForGoal(goalId: String): Flow<List<TaskEntity>> = flowOf(emptyList())
        override fun getDailyTasks(): Flow<List<TaskEntity>> = flowOf(emptyList())
        override suspend fun upsertTask(task: TaskEntity) {}
        override fun getTaskById(taskId: String): Flow<TaskEntity?> = flowOf(null)
        override suspend fun markCompleted(taskId: String, newDates: List<String>) {}
    }

    private class FakeChronicleRepository : ChronicleRepository {
        val entries = mutableListOf<ChronicleEntry>()
        override fun observeChronicle(): Flow<List<ChronicleEntry>> = flowOf(entries)
        override suspend fun saveEntry(entry: ChronicleEntry) { entries.add(entry) }
        override suspend fun setHearted(sourceId: String, hearted: Boolean) {}
        override suspend fun importIfAbsent(entry: ChronicleEntry): Boolean {
            val exists = entries.any { it.source == entry.source && it.sourceId == entry.sourceId }
            if (exists) return false
            entries.add(entry)
            return true
        }
    }

    private class FakeRiteSessionDao : RiteSessionDao {
        val sessions = mutableListOf<RiteSessionEntity>()
        override suspend fun insertSession(session: RiteSessionEntity) {
            sessions.removeAll { it.id == session.id }
            sessions.add(session)
        }
        override fun getSessionsForDate(date: String): kotlinx.coroutines.flow.Flow<List<RiteSessionEntity>> =
            kotlinx.coroutines.flow.flowOf(sessions.filter { it.localDate == date })
        override suspend fun getSessionsForDateAndKind(date: String, kind: String): List<RiteSessionEntity> =
            sessions.filter { it.localDate == date && it.kind == kind }
        override fun getSessionsForKind(kind: String): kotlinx.coroutines.flow.Flow<List<RiteSessionEntity>> =
            kotlinx.coroutines.flow.flowOf(sessions.filter { it.kind == kind })
        override fun getAllSessions(): kotlinx.coroutines.flow.Flow<List<RiteSessionEntity>> =
            kotlinx.coroutines.flow.flowOf(sessions)
        override suspend fun getAllSessionsList(): List<RiteSessionEntity> = sessions
        override suspend fun insertSessions(sessions: List<RiteSessionEntity>) {
            sessions.forEach { insertSession(it) }
        }
        override fun getSessionsFromDate(fromDate: String): kotlinx.coroutines.flow.Flow<List<RiteSessionEntity>> =
            kotlinx.coroutines.flow.flowOf(sessions.filter { it.localDate >= fromDate })
        override suspend fun deleteSession(id: String) {
            sessions.removeAll { it.id == id }
        }
    }

    private class FakeLibraryDao : LibraryDao {
        val highlights = mutableListOf<LibraryHighlightEntity>()
        override suspend fun getBookById(bookId: String): LibraryBookEntity? = null
        override suspend fun getChaptersForBook(bookId: String): List<LibraryChapterEntity> = emptyList()
        override suspend fun insertBook(book: LibraryBookEntity) {}
        override suspend fun insertChapters(chapters: List<LibraryChapterEntity>) {}
        override fun getHighlightsForBook(bookId: String): kotlinx.coroutines.flow.Flow<List<LibraryHighlightEntity>> =
            kotlinx.coroutines.flow.flowOf(highlights.filter { it.bookId == bookId })
        override suspend fun getAllHighlightsList(): List<LibraryHighlightEntity> = highlights
        override suspend fun insertHighlight(highlight: LibraryHighlightEntity) {
            highlights.removeAll { it.id == highlight.id && highlight.id != 0L }
            highlights.add(highlight)
        }
        override suspend fun insertHighlights(highlights: List<LibraryHighlightEntity>) {
            highlights.forEach { insertHighlight(it) }
        }
        override suspend fun deleteHighlight(highlight: LibraryHighlightEntity) {
            highlights.remove(highlight)
        }
        override fun getAllQuotes(): kotlinx.coroutines.flow.Flow<List<LibraryQuoteEntity>> =
            kotlinx.coroutines.flow.flowOf(emptyList())
        override suspend fun insertQuote(quote: LibraryQuoteEntity) {}
    }

    private class TestNeonAscentDatabase(
        val fakeLibraryDao: FakeLibraryDao = FakeLibraryDao(),
        val fakeRiteSessionDao: FakeRiteSessionDao = FakeRiteSessionDao()
    ) : NeonAscentDatabase() {
        var clearAllTablesCalled = false
        override fun clearAllTables() { clearAllTablesCalled = true }
        override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper = throw NotImplementedError()
        override fun createInvalidationTracker(): InvalidationTracker = throw NotImplementedError()
        override fun goalDao(): com.neon.ascent.core.data.local.dao.GoalDao = throw NotImplementedError()
        override fun specialDao(): SpecialDao = throw NotImplementedError()
        override fun ascensionDao(): AscensionDao = throw NotImplementedError()
        override fun workoutDao(): WorkoutDao = throw NotImplementedError()
        override fun neuralMemoryDao(): NeuralMemoryDao = throw NotImplementedError()
        override fun insightDao(): InsightDao = throw NotImplementedError()
        override fun dopamineMenuDao(): DopamineMenuDao = throw NotImplementedError()
        override fun protocolDao(): ProtocolDao = throw NotImplementedError()
        override fun biomarkerDao(): BiomarkerDao = throw NotImplementedError()
        override fun dailyVitalRollupDao(): DailyVitalRollupDao = throw NotImplementedError()
        override fun bodySampleDao(): BodySampleDao = throw NotImplementedError()
        override fun operativeProfileDao(): OperativeProfileDao = throw NotImplementedError()
        override fun libraryDao(): LibraryDao = fakeLibraryDao
        override fun vaultDao(): VaultDao = throw NotImplementedError()
        override fun riteSessionDao(): RiteSessionDao = fakeRiteSessionDao
        override fun selfMapDao(): com.neon.ascent.core.data.local.dao.SelfMapDao = throw NotImplementedError()
        override fun reconstitutionDao(): ReconstitutionDao = throw NotImplementedError()
        override fun dietDao(): DietDao = throw NotImplementedError()
        override fun supplementDao(): com.neon.ascent.core.data.local.dao.SupplementDao = throw NotImplementedError()
        override fun biohackingProtocolDao(): com.neon.ascent.core.data.local.dao.BiohackingProtocolDao = throw NotImplementedError()
    }

    private class TestAppDatabase : AppDatabase() {
        var clearAllTablesCalled = false
        override fun clearAllTables() { clearAllTablesCalled = true }
        override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper = throw NotImplementedError()
        override fun createInvalidationTracker(): InvalidationTracker = throw NotImplementedError()
        override fun userCharacterDao(): UserCharacterDao = throw NotImplementedError()
        override fun biohackingDao(): BiohackingDao = throw NotImplementedError()
        override fun sayingsDao(): SayingsDao = throw NotImplementedError()
        override fun journalDao(): JournalDao = throw NotImplementedError()
        override fun questDao(): QuestDao = throw NotImplementedError()
        override fun taskDao(): TaskDao = throw NotImplementedError()
        override fun loreDao(): LoreDao = throw NotImplementedError()
        override fun bookDao(): BookDao = throw NotImplementedError()
        override fun dailyPrayerDao(): DailyPrayerDao = throw NotImplementedError()
        override fun benchmarkDao(): BenchmarkDao = throw NotImplementedError()
        override fun userStoryDao(): UserStoryDao = throw NotImplementedError()
        override fun goalDao(): GoalDao = throw NotImplementedError()
        override fun goalTaskDao(): GoalTaskDao = throw NotImplementedError()
        override fun habitMetricDao(): HabitMetricDao = throw NotImplementedError()
        override fun inventoryDao(): InventoryDao = throw NotImplementedError()
        override fun chatDao(): ChatDao = throw NotImplementedError()
        override fun stockDao(): StockDao = throw NotImplementedError()
        override fun netWorthDao(): NetWorthDao = throw NotImplementedError()
    }

    private class TestSharedPreferences : SharedPreferences {
        private val map = mutableMapOf<String, Any?>()
        override fun getAll(): Map<String, *> = map
        override fun getString(key: String, defValue: String?): String? = (map[key] as? String) ?: defValue
        override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = (map[key] as? Set<String>) ?: defValues
        override fun getInt(key: String, defValue: Int): Int = (map[key] as? Int) ?: defValue
        override fun getLong(key: String, defValue: Long): Long = (map[key] as? Long) ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = (map[key] as? Float) ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
        override fun contains(key: String): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = TestEditor()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        inner class TestEditor : SharedPreferences.Editor {
            override fun putString(key: String, value: String?): SharedPreferences.Editor { map[key] = value; return this }
            override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor { map[key] = values; return this }
            override fun putInt(key: String, value: Int): SharedPreferences.Editor { map[key] = value; return this }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor { map[key] = value; return this }
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor { map[key] = value; return this }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor { map[key] = value; return this }
            override fun remove(key: String): SharedPreferences.Editor { map.remove(key); return this }
            override fun clear(): SharedPreferences.Editor { map.clear(); return this }
            override fun commit(): Boolean = true
            override fun apply() {}
        }
    }

    private class TestContext(private val baseDir: File) : ContextWrapper(null) {
        private val sp = TestSharedPreferences()
        override fun getApplicationContext(): Context = this
        override fun getDatabasePath(name: String): File = File(baseDir, name)
        override fun getFilesDir(): File = baseDir
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = sp
    }

    private class FakeOperativeProfileDao : OperativeProfileDao {
        var profile: OperativeProfileEntity? = null
        override fun getOperativeProfile(id: String): Flow<OperativeProfileEntity?> = flowOf(profile)
        override suspend fun getOperativeProfileOnce(id: String): OperativeProfileEntity? = profile
        override suspend fun upsertOperativeProfile(entity: OperativeProfileEntity) { profile = entity }
    }

    private class FakeBodySampleDao : BodySampleDao {
        val samples = mutableListOf<BodySampleEntity>()
        override suspend fun upsertSample(sample: BodySampleEntity) { samples.add(sample) }
        override suspend fun upsertSamples(samplesList: List<BodySampleEntity>) { samples.addAll(samplesList) }
        override suspend fun deleteSample(id: String) { samples.removeAll { it.id == id } }
        override fun getSamplesForMetric(metric: String, fromMillis: Long, toMillis: Long): Flow<List<BodySampleEntity>> = flowOf(samples.filter { it.metric == metric })
        override suspend fun getSamplesForMetricRange(metric: String, fromDate: String, toDate: String): List<BodySampleEntity> = samples.filter { it.metric == metric }
        override fun getSamplesForMetricRangeFlow(metric: String, fromDate: String, toDate: String): Flow<List<BodySampleEntity>> = flowOf(samples.filter { it.metric == metric })
        override fun getSamplesForDay(localDate: String): Flow<List<BodySampleEntity>> = flowOf(samples.filter { it.localDate == localDate })
        override suspend fun getSamplesForDayList(localDate: String): List<BodySampleEntity> = samples.filter { it.localDate == localDate }
        override suspend fun getSamplesBetween(fromMillis: Long, toMillis: Long): List<BodySampleEntity> = samples.filter { it.loggedAt in fromMillis..toMillis }
        override fun getFilteredSamples(metric: String?, site: String?, method: String?, position: String?, fromMillis: Long, toMillis: Long): Flow<List<BodySampleEntity>> = flowOf(samples)
    }

    private class FakeUserPreferencesRepository(context: Context) : UserPreferencesRepository(context) {
        var theme = "CYBER"
        var unit = "Metric"
        var freq = "DAILY"

        override val themeMode: Flow<String> get() = flowOf(theme)
        override val measurementUnit: Flow<String> get() = flowOf(unit)
        override val backupFrequency: Flow<String> get() = flowOf(freq)

        override suspend fun setThemeMode(mode: String) { theme = mode }
        override suspend fun updateMeasurementUnit(unit: String) { this.unit = unit }
        override suspend fun setBackupFrequency(freq: String) { this.freq = freq }
    }

    private class FakeSettingsRepository(context: Context) : SettingsRepository(context)

    private class FakeAscensionDao : AscensionDao {
        val directives = mutableMapOf<String, AscensionDirectiveEntity>()
        val missions = mutableMapOf<String, AscensionMissionEntity>()
        val tasks = mutableMapOf<String, AscensionTaskEntity>()
        val completions = mutableListOf<AscensionTaskCompletionEntity>()
        val neuralLogs = mutableListOf<NeuralLogEntity>()

        override fun getAllDirectives(): Flow<List<AscensionDirectiveEntity>> = flowOf(directives.values.toList())
        override fun getActiveDirectives(): Flow<List<AscensionDirectiveEntity>> = flowOf(directives.values.filter { it.status == "ACTIVE" })
        override suspend fun insertDirective(directive: AscensionDirectiveEntity) { directives[directive.id] = directive }
        override suspend fun updateDirective(directive: AscensionDirectiveEntity) { directives[directive.id] = directive }
        override suspend fun deleteDirective(id: String) { directives.remove(id) }
        override suspend fun getDirectiveById(id: String): AscensionDirectiveEntity? = directives[id]

        override fun getMissionsForDirective(directiveId: String): Flow<List<AscensionMissionEntity>> = flowOf(missions.values.filter { it.directiveId == directiveId })
        override suspend fun getMissionsForDirectiveSync(directiveId: String): List<AscensionMissionEntity> = missions.values.filter { it.directiveId == directiveId }
        override suspend fun getMissionById(id: String): AscensionMissionEntity? = missions[id]
        override fun getActiveMissions(): Flow<List<AscensionMissionEntity>> = flowOf(missions.values.filter { it.status == "ACTIVE" })
        override suspend fun insertMission(mission: AscensionMissionEntity) { missions[mission.id] = mission }
        override suspend fun updateMission(mission: AscensionMissionEntity) { missions[mission.id] = mission }

        override fun getTasksForParent(parentId: String): Flow<List<AscensionTaskEntity>> = flowOf(tasks.values.filter { it.parentId == parentId })
        override suspend fun getTasksForParentSync(parentId: String): List<AscensionTaskEntity> = tasks.values.filter { it.parentId == parentId }
        override fun getAllRecurringTasks(): Flow<List<AscensionTaskEntity>> = flowOf(tasks.values.filter { it.type == "RECURRING" })
        override suspend fun insertTask(task: AscensionTaskEntity) { tasks[task.id] = task }
        override suspend fun updateTask(task: AscensionTaskEntity) { tasks[task.id] = task }
        override suspend fun deleteTask(id: String) { tasks.remove(id) }
        override fun getTaskById(id: String): Flow<AscensionTaskEntity?> = flowOf(tasks[id])
        override suspend fun updateDirectiveNotes(id: String, notes: String) { directives[id]?.let { directives[id] = it.copy(notes = notes) } }

        override suspend fun insertCompletion(completion: AscensionTaskCompletionEntity) { completions.add(completion) }
        override fun getCompletionsForTask(taskId: String): Flow<List<AscensionTaskCompletionEntity>> = flowOf(completions.filter { it.taskId == taskId })
        override fun getCompletionsInRange(startTime: java.time.Instant): Flow<List<AscensionTaskCompletionEntity>> = flowOf(completions.filter { it.timestamp >= startTime })

        override suspend fun insertNeuralLog(log: NeuralLogEntity) { neuralLogs.add(log) }
        override fun getAllNeuralLogs(): Flow<List<NeuralLogEntity>> = flowOf(neuralLogs)
    }

    private class FakeVaultDao : VaultDao {
        val accounts = mutableMapOf<String, VaultAccountEntity>()
        val snapshots = mutableListOf<VaultSnapshotEntity>()
        val watchlist = mutableMapOf<String, VaultWatchlistItemEntity>()

        override fun getAllAccounts(): Flow<List<VaultAccountEntity>> = flowOf(accounts.values.toList())
        override suspend fun getAllAccountsOnce(): List<VaultAccountEntity> = accounts.values.toList()
        override suspend fun insertAccount(account: VaultAccountEntity) { accounts[account.id] = account }
        override suspend fun deleteAccount(account: VaultAccountEntity) { accounts.remove(account.id) }

        override fun getRecentSnapshots(): Flow<List<VaultSnapshotEntity>> = flowOf(snapshots)
        override suspend fun insertSnapshot(snapshot: VaultSnapshotEntity) { snapshots.add(snapshot) }

        override fun getWatchlist(): Flow<List<VaultWatchlistItemEntity>> = flowOf(watchlist.values.toList())
        override suspend fun getWatchlistOnce(): List<VaultWatchlistItemEntity> = watchlist.values.toList()
        override suspend fun addToWatchlist(item: VaultWatchlistItemEntity) { watchlist[item.symbol] = item }
        override suspend fun removeFromWatchlist(item: VaultWatchlistItemEntity) { watchlist.remove(item.symbol) }
        override suspend fun isFollowing(symbol: String): Boolean = watchlist.containsKey(symbol)
    }

    private class FakeSelfMapDao : com.neon.ascent.core.data.local.dao.SelfMapDao {
        val items = mutableMapOf<Long, SelfItemEntity>()
        val snapshots = mutableListOf<SelfMapSnapshotEntity>()
        val moodEntries = mutableListOf<SelfMapMoodEntryEntity>()
        val eoeEntries = mutableListOf<EoEEntryEntity>()

        override fun getAllActiveItems(): Flow<List<SelfItemEntity>> = flowOf(items.values.filter { !it.isArchived })
        override suspend fun getAllActiveItemsOnce(): List<SelfItemEntity> = items.values.filter { !it.isArchived }
        override suspend fun getAllItemsOnce(): List<SelfItemEntity> = items.values.toList()
        override fun getItemsByCategory(category: com.neon.ascent.core.domain.selfmap.SelfMapCategory): Flow<List<SelfItemEntity>> = flowOf(items.values.filter { it.category == category })
        override fun getItemById(id: Long): Flow<SelfItemEntity?> = flowOf(items[id])
        override suspend fun getItemByIdOnce(id: Long): SelfItemEntity? = items[id]
        override fun getAverageImportance(): Flow<Float> = flowOf(items.values.map { it.importance }.average().toFloat())
        override fun getCoreCount(): Flow<Int> = flowOf(items.values.count { it.importance >= 0.7f })
        override fun getTotalItemCount(): Flow<Int> = flowOf(items.size)
        override suspend fun insertSelfItem(item: SelfItemEntity): Long { items[item.id] = item; return item.id }
        override suspend fun insertSelfItems(items: List<SelfItemEntity>): List<Long> { items.forEach { this.items[it.id] = it }; return items.map { it.id } }
        override suspend fun updateSelfItem(item: SelfItemEntity) { items[item.id] = item }
        override suspend fun archiveSelfItem(id: Long, updatedAt: Instant) { items[id]?.let { items[id] = it.copy(isArchived = true, updatedAt = updatedAt) } }
        override suspend fun unarchiveSelfItem(id: Long, updatedAt: Instant) { items[id]?.let { items[id] = it.copy(isArchived = false, updatedAt = updatedAt) } }
        override suspend fun deleteSelfItem(id: Long) { items.remove(id) }
        override suspend fun deleteAllSelfItems() { items.clear() }

        override suspend fun insertEoEEntry(entry: EoEEntryEntity): Long { eoeEntries.add(entry); return entry.id }
        override fun getAllEoEEntries(): Flow<List<EoEEntryEntity>> = flowOf(eoeEntries)
        override suspend fun getAllEoEEntriesOnce(): List<EoEEntryEntity> = eoeEntries
        override fun getEoEEntriesForSelfItem(selfItemId: Long): Flow<List<EoEEntryEntity>> = flowOf(eoeEntries.filter { it.selfItemId == selfItemId })
        override suspend fun deleteEoEEntry(id: Long) { eoeEntries.removeAll { it.id == id } }
        override suspend fun deleteAllEoEEntries() { eoeEntries.clear() }

        override suspend fun insertSnapshot(snapshot: SelfMapSnapshotEntity): Long { snapshots.add(snapshot); return snapshot.id }
        override fun getAllSnapshots(): Flow<List<SelfMapSnapshotEntity>> = flowOf(snapshots)
        override suspend fun getAllSnapshotsOnce(): List<SelfMapSnapshotEntity> = snapshots
        override fun getLatestSnapshot(): Flow<SelfMapSnapshotEntity?> = flowOf(snapshots.lastOrNull())
        override suspend fun deleteAllSnapshots() { snapshots.clear() }

        override suspend fun insertMoodEntry(entry: SelfMapMoodEntryEntity): Long { moodEntries.add(entry); return entry.id }
        override fun getAllMoodEntries(): Flow<List<SelfMapMoodEntryEntity>> = flowOf(moodEntries)
        override suspend fun getAllMoodEntriesOnce(): List<SelfMapMoodEntryEntity> = moodEntries
        override fun getRecentMoodEntries(limit: Int): Flow<List<SelfMapMoodEntryEntity>> = flowOf(moodEntries.takeLast(limit))
        override fun getLatestMoodEntry(): Flow<SelfMapMoodEntryEntity?> = flowOf(moodEntries.lastOrNull())
        override suspend fun getLatestMoodEntryOnce(): SelfMapMoodEntryEntity? = moodEntries.lastOrNull()
        override suspend fun deleteMoodEntry(id: Long) { moodEntries.removeAll { it.id == id } }
        override suspend fun deleteAllMoodEntries() { moodEntries.clear() }
    }

    private class FakeReconstitutionDao : ReconstitutionDao {
        val mixes = mutableMapOf<String, SavedReconstitutionEntity>()
        override fun getAllSavedMixes(): Flow<List<SavedReconstitutionEntity>> = flowOf(mixes.values.toList())
        override suspend fun getSavedMixById(id: String): SavedReconstitutionEntity? = mixes[id]
        override suspend fun insertSavedMix(mix: SavedReconstitutionEntity) { mixes[mix.id] = mix }
        override suspend fun deleteSavedMix(id: String) { mixes.remove(id) }
    }

    private class FakeNeuralMemoryDao : NeuralMemoryDao {
        val memories = mutableListOf<NeuralMemory>()
        override suspend fun insertMemory(memory: NeuralMemory) { memories.add(memory) }
        override fun getAllMemories(): Flow<List<NeuralMemory>> = flowOf(memories)
        override fun getMemoriesByWing(wing: String): Flow<List<NeuralMemory>> = flowOf(memories.filter { it.wing == wing })
        override fun getMemoriesByRoom(wing: String, room: String): Flow<List<NeuralMemory>> = flowOf(memories.filter { it.wing == wing && it.room == room })
        override suspend fun searchMemories(query: String, limit: Int): List<NeuralMemory> = memories.filter { it.content.contains(query) }.take(limit)
        override suspend fun pruneOldMemories(threshold: Long) { memories.removeAll { it.timestamp < threshold } }
    }

    private class FakeChatDao : ChatDao {
        val sessions = mutableMapOf<String, ChatSession>()
        val messages = mutableListOf<ChatMessage>()

        override fun getChatSessions(): Flow<List<ChatSession>> = flowOf(sessions.values.toList())
        override suspend fun insertChatSession(session: ChatSession) { sessions[session.sessionId] = session }
        override suspend fun updateChatSession(session: ChatSession) { sessions[session.sessionId] = session }
        override fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>> = flowOf(messages.filter { it.sessionId == sessionId })
        override fun getMessagesForContact(contactName: String): Flow<List<ChatMessage>> = flowOf(messages.filter { it.contactName == contactName })
        override fun getSessionsForContact(contactName: String): Flow<List<ChatSession>> = flowOf(sessions.values.filter { it.contactName == contactName })
        override suspend fun getLatestSessionForContact(contactName: String): ChatSession? = sessions.values.firstOrNull { it.contactName == contactName }
        override suspend fun getSessionById(sessionId: String): ChatSession? = sessions[sessionId]
        override suspend fun insertMessage(message: ChatMessage) { messages.add(message) }
        override suspend fun markAsRead(sessionId: String) { sessions[sessionId]?.let { sessions[sessionId] = it.copy(isUnread = false) } }
        override suspend fun markAllAsReadForContact(contactName: String) {}
    }

    private class FakeDietDao : com.neon.ascent.core.data.local.dao.DietDao {
        private val diets = mutableMapOf<String, com.neon.ascent.core.data.local.entity.DietEntity>()
        override fun getAllDiets(): Flow<List<com.neon.ascent.core.data.local.entity.DietEntity>> = flowOf(diets.values.toList())
        override fun getDietById(id: String): Flow<com.neon.ascent.core.data.local.entity.DietEntity?> = flowOf(diets[id])
        override suspend fun getDietByIdSync(id: String): com.neon.ascent.core.data.local.entity.DietEntity? = diets[id]
        override suspend fun getDietCount(): Int = diets.size
        override suspend fun insertDiets(diets: List<com.neon.ascent.core.data.local.entity.DietEntity>) {
            diets.forEach { this.diets[it.id] = it }
        }
        override suspend fun insertDiet(diet: com.neon.ascent.core.data.local.entity.DietEntity) {
            diets[diet.id] = diet
        }
    }

    private class FakeSupplementDao : com.neon.ascent.core.data.local.dao.SupplementDao {
        private val supplements = mutableMapOf<String, com.neon.ascent.core.data.local.entity.SupplementEntity>()
        override fun getAllSupplements(): Flow<List<com.neon.ascent.core.data.local.entity.SupplementEntity>> = flowOf(supplements.values.toList())
        override fun getSupplementById(id: String): Flow<com.neon.ascent.core.data.local.entity.SupplementEntity?> = flowOf(supplements[id])
        override suspend fun getSupplementByIdSync(id: String): com.neon.ascent.core.data.local.entity.SupplementEntity? = supplements[id]
        override suspend fun getSupplementCount(): Int = supplements.size
        override suspend fun insertSupplements(supplements: List<com.neon.ascent.core.data.local.entity.SupplementEntity>) {
            supplements.forEach { this.supplements[it.id] = it }
        }
        override suspend fun insertSupplement(supplement: com.neon.ascent.core.data.local.entity.SupplementEntity) {
            supplements[supplement.id] = supplement
        }
    }

    private class FakeBiohackingProtocolDao : com.neon.ascent.core.data.local.dao.BiohackingProtocolDao {
        private val protocols = mutableMapOf<String, com.neon.ascent.core.data.local.entity.BiohackingProtocolEntity>()
        override fun getAllProtocols(): Flow<List<com.neon.ascent.core.data.local.entity.BiohackingProtocolEntity>> = flowOf(protocols.values.toList())
        override fun getProtocolById(id: String): Flow<com.neon.ascent.core.data.local.entity.BiohackingProtocolEntity?> = flowOf(protocols[id])
        override suspend fun getProtocolByIdSync(id: String): com.neon.ascent.core.data.local.entity.BiohackingProtocolEntity? = protocols[id]
        override suspend fun getProtocolCount(): Int = protocols.size
        override suspend fun insertProtocols(protocols: List<com.neon.ascent.core.data.local.entity.BiohackingProtocolEntity>) {
            protocols.forEach { this.protocols[it.id] = it }
        }
        override suspend fun insertProtocol(protocol: com.neon.ascent.core.data.local.entity.BiohackingProtocolEntity) {
            protocols[protocol.id] = protocol
        }
    }

    private lateinit var workoutDao: FakeWorkoutDao
    private lateinit var journalDao: FakeJournalDao
    private lateinit var chronicleRepository: FakeChronicleRepository
    private lateinit var operativeProfileDao: FakeOperativeProfileDao
    private lateinit var bodySampleDao: FakeBodySampleDao
    private lateinit var dailyVitalRollupDao: FakeDailyVitalRollupDao
    private lateinit var ascensionDao: FakeAscensionDao
    private lateinit var vaultDao: FakeVaultDao
    private lateinit var selfMapDao: FakeSelfMapDao
    private lateinit var reconstitutionDao: FakeReconstitutionDao
    private lateinit var neuralMemoryDao: FakeNeuralMemoryDao
    private lateinit var chatDao: FakeChatDao
    private lateinit var coreDatabase: TestNeonAscentDatabase
    private lateinit var appDatabase: TestAppDatabase
    private lateinit var repository: FullDataBackupRepositoryImpl
    private lateinit var testContext: TestContext

    @Before
    fun setUp() {
        workoutDao = FakeWorkoutDao()
        journalDao = FakeJournalDao()
        chronicleRepository = FakeChronicleRepository()
        operativeProfileDao = FakeOperativeProfileDao()
        bodySampleDao = FakeBodySampleDao()
        dailyVitalRollupDao = FakeDailyVitalRollupDao()
        ascensionDao = FakeAscensionDao()
        vaultDao = FakeVaultDao()
        selfMapDao = FakeSelfMapDao()
        reconstitutionDao = FakeReconstitutionDao()
        neuralMemoryDao = FakeNeuralMemoryDao()
        chatDao = FakeChatDao()
        val fakeLibraryDao = FakeLibraryDao()
        val fakeRiteSessionDao = FakeRiteSessionDao()
        coreDatabase = TestNeonAscentDatabase(fakeLibraryDao, fakeRiteSessionDao)
        appDatabase = TestAppDatabase()
        testContext = TestContext(tempFolder.root)

        repository = FullDataBackupRepositoryImpl(
            workoutDao = workoutDao,
            biomarkerDao = FakeBiomarkerDao(),
            dailyVitalRollupDao = dailyVitalRollupDao,
            specialDao = FakeSpecialDao(),
            dopamineMenuDao = FakeDopamineMenuDao(),
            biohackingDao = FakeBiohackingDao(),
            bookDao = FakeBookDao(),
            libraryRepository = FakeLibraryRepository(),
            journalDao = journalDao,
            dailyPrayerDao = FakeDailyPrayerDao(),
            loreDao = FakeLoreDao(),
            userCharacterDao = FakeUserCharacterDao(),
            goalDao = FakeCoreGoalDao(),
            goalTaskDao = FakeGoalTaskDao(),
            chronicleRepository = chronicleRepository,
            operativeProfileDao = operativeProfileDao,
            bodySampleDao = bodySampleDao,
            userPreferencesRepository = FakeUserPreferencesRepository(testContext),
            settingsRepository = FakeSettingsRepository(testContext),
            coreDatabase = coreDatabase,
            appDatabase = appDatabase,
            ascensionDao = ascensionDao,
            vaultDao = vaultDao,
            selfMapDao = selfMapDao,
            reconstitutionDao = reconstitutionDao,
            neuralMemoryDao = neuralMemoryDao,
            chatDao = chatDao,
            selfMapPreferencesDataStore = com.neon.ascent.core.data.datastore.SelfMapPreferencesDataStore(testContext),
            healthPreferencesDataStore = com.neon.ascent.core.data.datastore.HealthPreferencesDataStore(testContext),
            workoutPreferencesDataStore = com.neon.ascent.core.data.datastore.WorkoutPreferencesDataStore(testContext),
            briefPreferencesDataStore = com.neon.ascent.core.data.datastore.BriefPreferencesDataStore(testContext),
            directivePreferencesDataStore = com.neon.ascent.core.data.datastore.DirectivePreferencesDataStore(testContext),
            dietPreferencesDataStore = com.neon.ascent.core.data.datastore.DietPreferencesDataStore(testContext),
            dietRepository = com.neon.ascent.core.data.repository.DietRepositoryImpl(FakeDietDao(), com.neon.ascent.core.data.datastore.DietPreferencesDataStore(testContext)),
            supplementPreferencesDataStore = com.neon.ascent.core.data.datastore.SupplementPreferencesDataStore(testContext),
            supplementRepository = com.neon.ascent.core.data.repository.SupplementRepositoryImpl(FakeSupplementDao(), com.neon.ascent.core.data.datastore.SupplementPreferencesDataStore(testContext)),
            biohackingProtocolPreferencesDataStore = com.neon.ascent.core.data.datastore.BiohackingProtocolPreferencesDataStore(testContext),
            biohackingProtocolRepository = com.neon.ascent.core.data.repository.BiohackingProtocolRepositoryImpl(FakeBiohackingProtocolDao(), com.neon.ascent.core.data.datastore.BiohackingProtocolPreferencesDataStore(testContext)),
            riteSessionDao = fakeRiteSessionDao,
            libraryDao = fakeLibraryDao,
            context = testContext
        )
    }

    private val sampleBackupJson = """
    {
      "version": 1,
      "exportedAt": "2026-09-12T10:00:00Z",
      "appVersion": "1.0",
      "workoutPayload": {
        "sessions": [
          {
            "id": "session_1",
            "dateEpochMs": 1726132800000,
            "protocol": "CYBER_STRENGTH",
            "durationSeconds": 3600,
            "notes": "Squat day",
            "experienceLevel": "INTERMEDIATE",
            "somatotype": "MESOMORPH"
          },
          {
            "id": "session_2",
            "dateEpochMs": 1726219200000,
            "protocol": "CYBER_STRENGTH",
            "durationSeconds": 3200,
            "notes": "Bench day",
            "experienceLevel": "INTERMEDIATE",
            "somatotype": "MESOMORPH"
          }
        ],
        "logs": [
          {
            "id": "log_1",
            "sessionId": "session_1",
            "exerciseId": "ex_squat",
            "order": 1,
            "exerciseName": "Back Squat"
          }
        ],
        "sets": [
          {
            "id": "set_1",
            "workoutLogId": "log_1",
            "weight": 100.0,
            "reps": 5,
            "setType": "REGULAR",
            "isCompleted": true,
            "timestampEpochMs": 1726132800000
          }
        ]
      }
    }
    """.trimIndent()

    @Test
    fun `restoreBackupJson returns failure for empty or blank json`() = runBlocking {
        val emptyResult = repository.restoreBackupJson("", RestoreMode.MERGE)
        assertFalse(emptyResult.success)
        assertEquals("Invalid JSON payload structure: File is empty", emptyResult.message)

        val whitespaceResult = repository.restoreBackupJson("   ", RestoreMode.MERGE)
        assertFalse(whitespaceResult.success)
        assertEquals("Invalid JSON payload structure: File is empty", whitespaceResult.message)
    }

    @Test
    fun `restoreBackupJson returns failure for malformed json`() = runBlocking {
        val malformedResult = repository.restoreBackupJson("{ invalid json }", RestoreMode.MERGE)
        assertFalse(malformedResult.success)
        assertEquals("Invalid JSON payload structure", malformedResult.message)
    }

    @Test
    fun `restoreBackupJson returns failure for json payload missing all backup sections`() = runBlocking {
        val jsonNoSections = """{"version": 1, "exportedAt": "2025-02-23T10:00:00Z"}"""
        val result = repository.restoreBackupJson(jsonNoSections, RestoreMode.MERGE)
        assertFalse(result.success)
        assertEquals("Invalid JSON payload structure: No backup sections found", result.message)
    }

    @Test
    fun `MERGE twice results in no duplicate sessions`() = runBlocking {
        assertEquals(0, workoutDao.sessions.size)

        // First MERGE
        val firstResult = repository.restoreBackupJson(sampleBackupJson, RestoreMode.MERGE)
        assertTrue(firstResult.success)
        assertEquals(2, firstResult.restoredSessionsCount)
        assertEquals(2, workoutDao.sessions.size)
        assertFalse(coreDatabase.clearAllTablesCalled)
        assertFalse(appDatabase.clearAllTablesCalled)

        // Second MERGE with same file
        val secondResult = repository.restoreBackupJson(sampleBackupJson, RestoreMode.MERGE)
        assertTrue(secondResult.success)
        assertEquals(2, secondResult.restoredSessionsCount)
        assertEquals(2, workoutDao.sessions.size) // No duplicate sessions created!
    }

    @Test
    fun `invalid JSON leaves counts unchanged`() = runBlocking {
        // Restore initial valid payload
        val firstResult = repository.restoreBackupJson(sampleBackupJson, RestoreMode.MERGE)
        assertTrue(firstResult.success)
        assertEquals(2, workoutDao.sessions.size)

        // Attempt restore with invalid JSON
        val invalidResult = repository.restoreBackupJson("{ invalid json payload }", RestoreMode.MERGE)
        assertFalse(invalidResult.success)

        // Verify session count is unchanged
        assertEquals(2, workoutDao.sessions.size)
    }

    @Test
    fun `REPLACE mode creates snapshot before clearAllTables`() = runBlocking {
        // Create mock live DB file
        val dbFile = File(tempFolder.root, "neon_ascent_database")
        dbFile.writeText("fake_db_data")

        val result = repository.restoreBackupJson(sampleBackupJson, RestoreMode.REPLACE)
        assertTrue(result.success)
        assertTrue(coreDatabase.clearAllTablesCalled)
        assertTrue(appDatabase.clearAllTablesCalled)

        // Check snapshot file exists
        val snapshotFiles = tempFolder.root.listFiles { _, name -> name.startsWith("neon_ascent_database.pre_restore-") }
        assertNotNull(snapshotFiles)
        assertEquals(1, snapshotFiles!!.size)
    }

    @Test
    fun `export and restore payload version 2 with settings and operative profile`() = runBlocking {
        // Setup initial data
        operativeProfileDao.upsertOperativeProfile(
            OperativeProfileEntity(
                id = "default_user",
                name = "V_Test",
                sex = "FEMALE",
                dob = "2000-01-01",
                units = "METRIC",
                weight = "70",
                somatotype = 0.5f,
                level = 10,
                experience = 5000L
            )
        )
        bodySampleDao.upsertSample(
            BodySampleEntity(
                id = "bs_1",
                localDate = "2026-09-12",
                loggedAt = System.currentTimeMillis(),
                metric = "WEIGHT",
                value = 75.0,
                unit = "KG",
                source = "NEON"
            )
        )
        chronicleRepository.saveEntry(
            ChronicleEntry(
                source = "journal",
                sourceId = "j_1",
                wing = "CHRONICLE",
                room = "ORIGIN",
                content = "Chronicle reflection test"
            )
        )

        // Export payload
        val exportedJson = repository.exportBackupJson(BackupScope())
        assertTrue(exportedJson.contains("\"version\": 10"))
        assertTrue(exportedJson.contains("\"settingsPayload\""))
        assertTrue(exportedJson.contains("\"operativeProfile\""))
        assertTrue(exportedJson.contains("V_Test"))
        assertTrue(exportedJson.contains("Chronicle reflection test"))

        // Reset fake DAOs
        operativeProfileDao.profile = null
        bodySampleDao.samples.clear()
        chronicleRepository.entries.clear()

        // Restore via MERGE
        val restoreResult = repository.restoreBackupJson(exportedJson, RestoreMode.MERGE)
        assertTrue(restoreResult.success)

        // Verify restored
        val restoredOp = operativeProfileDao.getOperativeProfileOnce("default_user")
        assertNotNull(restoredOp)
        assertEquals("V_Test", restoredOp!!.name)
        assertEquals(1, bodySampleDao.samples.size)
        assertEquals(1, chronicleRepository.entries.size)
    }

    @Test
    fun `export and restore v3 round trip preserves directives, self map, and operative profile`() = runBlocking {
        // Arrange - populate source data
        val testDirective = AscensionDirectiveEntity(
            id = "dir_1",
            title = "Master Cybernetics",
            description = "Upgrade neural wiring",
            visionStatement = "Become top netrunner",
            status = "ACTIVE",
            successMetrics = emptyList(),
            targetEndDate = LocalDate.now().plusDays(30),
            isQuarterly = true,
            createdAt = Instant.now(),
            archivedAt = null,
            currentProgress = 0.5f,
            totalXPContributed = 500L,
            xpTarget = 1000L,
            archetypeTag = "NETRUNNER",
            tags = listOf("TECH", "NEURAL"),
            linkedAttributes = listOf("INTELLECT"),
            aiMentorMode = "DEFAULT",
            aiGenerated = false,
            notes = "Test notes",
            completionHistorySummary = null,
            lastReviewDate = LocalDate.now(),
            priorityTier = "HIGH"
        )
        ascensionDao.insertDirective(testDirective)

        val testSelfItem = SelfItemEntity(
            id = 42L,
            name = "Cyberdeck Rig",
            category = com.neon.ascent.core.domain.selfmap.SelfMapCategory.STUFF,
            importance = 0.9f,
            notes = "Primary deck",
            isArchived = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        selfMapDao.insertSelfItem(testSelfItem)

        val testOperative = OperativeProfileEntity(
            id = "default_user",
            name = "Vance Calder",
            netrunnerName = "ZeroCool",
            sex = "MALE",
            dob = "1995-05-05",
            units = "METRIC",
            weight = "80",
            somatotype = 0.6f,
            isCreationComplete = true
        )
        operativeProfileDao.upsertOperativeProfile(testOperative)

        // Act - export backup
        val json = repository.exportBackupJson(BackupScope())
        assertTrue("JSON should contain goalsPayload", json.contains("goalsPayload"))
        assertTrue("JSON should contain selfMapPayload", json.contains("selfMapPayload"))
        assertTrue("JSON should contain version 10", json.contains("\"version\": 10"))

        // Reset fake DAOs to test restore
        ascensionDao.directives.clear()
        selfMapDao.items.clear()
        operativeProfileDao.profile = null

        // Act - restore backup
        val result = repository.restoreBackupJson(json, RestoreMode.MERGE)
        assertTrue("Restore should succeed", result.success)

        // Assert - verify restored values
        val restoredDirective = ascensionDao.getDirectiveById("dir_1")
        assertNotNull("Restored directive should exist", restoredDirective)
        assertEquals("Master Cybernetics", restoredDirective?.title)

        val restoredSelfItem = selfMapDao.getItemByIdOnce(42L)
        assertNotNull("Restored self item should exist", restoredSelfItem)
        assertEquals("Cyberdeck Rig", restoredSelfItem?.name)

        val restoredProfile = operativeProfileDao.getOperativeProfileOnce("default_user")
        assertNotNull("Restored operative profile should exist", restoredProfile)
        assertEquals("Vance Calder", restoredProfile?.name)
        assertTrue("Restored profile should be complete", restoredProfile?.isCreationComplete == true)
    }

    @Test
    fun `export and restore v3 preserves vault, biohacking, and chat payloads`() = runBlocking {
        // Arrange
        val testAccount = VaultAccountEntity(
            id = "v_1",
            name = "Eurodollar Vault",
            institution = "Arasaka Bank",
            type = com.neon.ascent.core.domain.vault.models.VaultAssetType.CASH,
            balance = 50000.0,
            currency = "USD",
            isLinked = true,
            lastUpdated = System.currentTimeMillis()
        )
        vaultDao.insertAccount(testAccount)

        val testMix = SavedReconstitutionEntity(
            id = "mix_1",
            name = "BPC-157 Solution",
            vialMg = "5",
            waterMl = "2",
            concentrationMgPerMl = "2.5"
        )
        reconstitutionDao.insertSavedMix(testMix)

        val testChat = ChatSession(
            sessionId = "sess_rogue",
            contactName = "ROGUE",
            lastMessage = "Meet at Afterlife",
            lastTimestamp = System.currentTimeMillis(),
            isUnread = true
        )
        chatDao.insertChatSession(testChat)

        // Act
        val json = repository.exportBackupJson(BackupScope())
        assertTrue("JSON should contain vaultPayload", json.contains("vaultPayload"))
        assertTrue("JSON should contain biohackingPayload", json.contains("biohackingPayload"))
        assertTrue("JSON should contain chatPayload", json.contains("chatPayload"))

        // Reset fake DAOs
        vaultDao.accounts.clear()
        reconstitutionDao.mixes.clear()
        chatDao.sessions.clear()

        // Restore
        val result = repository.restoreBackupJson(json, RestoreMode.MERGE)
        assertTrue("Restore should succeed", result.success)

        // Assert
        assertEquals(1, vaultDao.accounts.size)
        assertEquals("Eurodollar Vault", vaultDao.accounts["v_1"]?.name)
        assertEquals(1, reconstitutionDao.mixes.size)
        assertEquals("BPC-157 Solution", reconstitutionDao.mixes["mix_1"]?.name)
        assertEquals(1, chatDao.sessions.size)
        assertEquals("ROGUE", chatDao.sessions["sess_rogue"]?.contactName)
    }

    @Test
    fun `export and restore user workout profile with scheduledDays and rotationIndex`() = runBlocking {
        val originalProfile = UserWorkoutProfileEntity(
            userId = "default_user",
            experienceLevel = "INTERMEDIATE",
            somatotype = "MESOMORPH",
            injuries = listOf("SHOULDER"),
            timePerSessionMinutes = 75,
            age = 32,
            heightCm = 180f,
            weightKg = 82f,
            gender = "MALE",
            activityFactor = 1.4f,
            unitSystem = "METRIC",
            activeProtocol = "UPPER_LOWER",
            rotationIndex = 3,
            scheduledDays = "[\"MONDAY\",\"WEDNESDAY\",\"FRIDAY\"]",
            deepLinkToRoutine = true,
            autoWeightIncrement = true,
            progressionMode = "WAVE_PERIODIZATION",
            weightIncrementCompound = 2.5f,
            weightIncrementUpperCompound = 2.5f,
            weightIncrementLowerCompound = 5.0f,
            weightIncrementIsolation = 1.25f,
            progressionPercentage = 3.0f,
            roundingIncrement = 2.5f,
            rirCapturePerMiniSet = true,
            sequencerEnabled = true,
            customSequenceIds = "[\"seq1\"]",
            coachingHintsEnabled = true,
            lastBlastStartDate = Instant.ofEpochMilli(1726132800000L)
        )
        workoutDao.insertUserProfile(originalProfile)

        val json = repository.exportBackupJson(BackupScope())
        workoutDao.profiles.clear()

        val restoreResult = repository.restoreBackupJson(json, RestoreMode.MERGE)
        assertTrue(restoreResult.success)

        val restoredProfile = workoutDao.getUserProfile("default_user").first()
        assertNotNull(restoredProfile)
        assertEquals("[\"MONDAY\",\"WEDNESDAY\",\"FRIDAY\"]", restoredProfile?.scheduledDays)
        assertEquals(3, restoredProfile?.rotationIndex)
        assertEquals("UPPER_LOWER", restoredProfile?.activeProtocol)
        assertEquals(listOf("SHOULDER"), restoredProfile?.injuries)
        assertEquals(75, restoredProfile?.timePerSessionMinutes)
        assertEquals(32, restoredProfile?.age)
        assertEquals(180f, restoredProfile?.heightCm)
        assertEquals(82f, restoredProfile?.weightKg)
        assertEquals("MALE", restoredProfile?.gender)
        assertEquals(true, restoredProfile?.deepLinkToRoutine)
        assertEquals("[\"seq1\"]", restoredProfile?.customSequenceIds)
        assertEquals(1726132800000L, restoredProfile?.lastBlastStartDate?.toEpochMilli())
    }

    @Test
    fun `export and restore active deload window round trip`() = runBlocking {
        val now = Instant.now()
        val activeWindow = DeloadWindowEntity(
            id = "dw_101",
            userId = "default_user",
            start = now.minus(java.time.Duration.ofDays(1)),
            end = now.plus(java.time.Duration.ofDays(8)),
            source = "RECOVERY",
            status = "ACTIVE"
        )
        workoutDao.insertDeloadWindow(activeWindow)

        val scope = BackupScope(includeWorkout = true)
        val json = repository.exportBackupJson(scope)

        // Clear local storage
        workoutDao.deloadWindows.clear()
        assertTrue(workoutDao.deloadWindows.isEmpty())

        // Restore
        val result = repository.restoreBackupJson(json, RestoreMode.MERGE)
        assertTrue("Restore should succeed", result.success)

        // Assert window restored and active
        val restoredWindow = workoutDao.getActiveDeloadWindow("default_user").first()
        assertNotNull("Active deload window should be restored", restoredWindow)
        assertEquals("dw_101", restoredWindow?.id)
        assertEquals("ACTIVE", restoredWindow?.status)
        assertEquals("RECOVERY", restoredWindow?.source)

        // Verify a new session starting inside this active window would be isDeload
        val isWindowActive = restoredWindow != null &&
                restoredWindow.status == "ACTIVE" &&
                !now.isBefore(restoredWindow.start) &&
                !now.isAfter(restoredWindow.end)
        assertTrue("New session inside restored window should evaluate isDeload = true", isWindowActive)
    }

    @Test
    fun `export and restore workout graph entities protocol cycle routine with exercises and augment activation`() = runBlocking {
        val cycle = ProtocolCycleEntity(
            id = "cycle_1",
            userId = "default_user",
            protocol = "UPPER_LOWER",
            startedAt = Instant.ofEpochMilli(1726000000000L),
            endedAt = null,
            status = "ACTIVE",
            currentWeek = 2,
            currentDayIndex = 1,
            configJson = "{\"type\":\"test\"}"
        )
        workoutDao.upsertProtocolCycle(cycle)

        val routine = WorkoutRoutineEntity(
            id = "routine_1",
            name = "Upper Hypertrophy",
            description = "Chest and back focus",
            protocol = "UPPER_LOWER",
            createdAt = Instant.ofEpochMilli(1726000000000L),
            isSystem = false,
            isAddedToLibrary = true
        )
        workoutDao.insertRoutine(routine)

        val exercise = ExerciseDefinitionEntity(
            id = "ex_bench",
            name = "Bench Press",
            description = "Barbell bench press",
            cues = listOf("Drive feet"),
            muscleGroups = listOf("CHEST", "TRICEPS"),
            equipment = listOf("BARBELL"),
            gifAssetPath = null,
            isLockedClassic = false,
            injurySubstitutions = emptyList()
        )
        workoutDao.insertExerciseDefinition(exercise)

        val crossRef = RoutineExerciseCrossRef(
            routineId = "routine_1",
            exerciseId = "ex_bench",
            order = 0
        )
        workoutDao.insertRoutineExerciseCrossRef(crossRef)

        val activation = AugmentActivationEntity(
            id = "activation_1",
            augmentId = "aug_biceps",
            userId = "default_user",
            mode = "ADDITIVE",
            status = "ACTIVE",
            loggingStyle = "CLUSTER",
            scheduledDays = "[\"TUESDAY\"]",
            createdAt = Instant.ofEpochMilli(1726000000000L),
            windowStart = null,
            windowEnd = null,
            hostProtocolFilter = null,
            dayTypeFilter = null,
            reminderEnabled = true
        )
        workoutDao.upsertAugmentActivation(activation)

        val json = repository.exportBackupJson(BackupScope())

        // Clear DAOs
        workoutDao.protocolCycles.clear()
        workoutDao.routines.clear()
        workoutDao.exercises.clear()
        workoutDao.routineExerciseCrossRefs.clear()
        workoutDao.augmentActivations.clear()

        val result = repository.restoreBackupJson(json, RestoreMode.MERGE)
        assertTrue(result.success)

        val restoredCycle = workoutDao.getProtocolCycleById("cycle_1")
        assertNotNull(restoredCycle)
        assertEquals("UPPER_LOWER", restoredCycle?.protocol)
        assertEquals(2, restoredCycle?.currentWeek)

        val restoredRoutine = workoutDao.getRoutineById("routine_1")
        assertNotNull(restoredRoutine)
        assertEquals("Upper Hypertrophy", restoredRoutine?.name)

        assertEquals(1, workoutDao.routineExerciseCrossRefs.size)
        assertEquals("ex_bench", workoutDao.routineExerciseCrossRefs.first().exerciseId)

        val restoredActivations = workoutDao.getActivationsByUserId("default_user").first()
        assertEquals(1, restoredActivations.size)
        assertEquals("aug_biceps", restoredActivations.first().augmentId)
        assertEquals("ACTIVE", restoredActivations.first().status)
    }

    @Test
    fun `export and restore preferences rest timers and focus layout mode`() = runBlocking {
        val healthPrefs = com.neon.ascent.core.data.datastore.HealthPreferencesDataStore(testContext)
        val workoutPrefs = com.neon.ascent.core.data.datastore.WorkoutPreferencesDataStore(testContext)

        healthPrefs.setDefaultRestTime(90)
        healthPrefs.setWorkSetRestTime(180)
        healthPrefs.setWarmupSetRestTime(45)
        healthPrefs.setDropSetRestTime(20)
        healthPrefs.setAutoStartRestTimer(false)
        healthPrefs.setRestTimerMode(com.neon.ascent.core.domain.workout.models.RestTimerMode.POPUP)
        healthPrefs.setWorkoutZoomLevel(1.25f)
        healthPrefs.setCodexPeriod("NINETY_DAYS")
        healthPrefs.setCodexWing("DECK")
        healthPrefs.setCodexLastExerciseId("ex_bench")

        workoutPrefs.setWorkoutLayoutMode(com.neon.ascent.core.domain.workout.models.WorkoutLayoutMode.FOCUS)
        workoutPrefs.setUseCustomNumpadInFocusMode(true)

        val json = repository.exportBackupJson(BackupScope())

        // Reset preferences to default
        healthPrefs.setDefaultRestTime(60)
        healthPrefs.setWorkSetRestTime(120)
        healthPrefs.setAutoStartRestTimer(true)
        workoutPrefs.setWorkoutLayoutMode(com.neon.ascent.core.domain.workout.models.WorkoutLayoutMode.CLASSIC)
        workoutPrefs.setUseCustomNumpadInFocusMode(false)

        val result = repository.restoreBackupJson(json, RestoreMode.MERGE)
        assertTrue(result.success)

        assertEquals(90, healthPrefs.defaultRestTime.first())
        assertEquals(180, healthPrefs.workSetRestTime.first())
        assertEquals(45, healthPrefs.warmupSetRestTime.first())
        assertEquals(20, healthPrefs.dropSetRestTime.first())
        assertEquals(false, healthPrefs.autoStartRestTimer.first())
        assertEquals(com.neon.ascent.core.domain.workout.models.RestTimerMode.POPUP, healthPrefs.restTimerMode.first())
        assertEquals(1.25f, healthPrefs.workoutZoomLevel.first())
        assertEquals("NINETY_DAYS", healthPrefs.codexPeriod.first())
        assertEquals("DECK", healthPrefs.codexWing.first())
        assertEquals("ex_bench", healthPrefs.codexLastExerciseId.first())
        assertEquals(com.neon.ascent.core.domain.workout.models.WorkoutLayoutMode.FOCUS, workoutPrefs.workoutLayoutMode.first())
        assertEquals(true, workoutPrefs.useCustomNumpadInFocusMode.first())
    }

    @Test
    fun `export and restore payload v5 round trip preserves rites, library highlights, and expanded preferences`() = runBlocking {
        val briefPrefs = com.neon.ascent.core.data.datastore.BriefPreferencesDataStore(testContext)
        val directivePrefs = com.neon.ascent.core.data.datastore.DirectivePreferencesDataStore(testContext)
        val healthPrefs = com.neon.ascent.core.data.datastore.HealthPreferencesDataStore(testContext)

        briefPrefs.setTargetWakeWd("06:30")
        briefPrefs.setTargetWakeWe("08:30")
        briefPrefs.setPulsePmMode("WEEKDAY_CLOCK")
        briefPrefs.setPulsePmCustomSchedule("MON,WED,FRI", "21:00")
        briefPrefs.setAdaptiveWakeEnabled(true)
        briefPrefs.setQuietHoursEnd("06:00")
        briefPrefs.setShowDirectivesOnMorningShade(true)

        directivePrefs.setDirectiveIntakeMode(com.neon.ascent.core.domain.goals.models.DirectiveIntakeMode.FORM)
        healthPrefs.setAutoSyncEnabled(true)
        healthPrefs.setSyncIntervalHours(12)

        val rite = RiteSessionEntity(
            id = "rite_1",
            kind = "SIT",
            startedAt = java.time.Instant.now(),
            durationMin = 15,
            source = "STILLJACK",
            notes = "Deep meditation",
            localDate = "2026-10-22"
        )
        coreDatabase.fakeRiteSessionDao.insertSession(rite)

        val highlight = LibraryHighlightEntity(
            id = 1L,
            bookId = "book_cyber",
            chapterIndex = 2,
            startOffset = 10,
            endOffset = 50,
            color = 0xFF00FFFFL,
            timestamp = System.currentTimeMillis()
        )
        coreDatabase.fakeLibraryDao.insertHighlight(highlight)

        val exportedJson = repository.exportBackupJson(BackupScope())
        assertTrue("JSON should contain version 10", exportedJson.contains("\"version\": 10"))
        assertTrue("JSON should contain ritesPayload", exportedJson.contains("ritesPayload"))
        assertTrue("JSON should contain libraryPayload", exportedJson.contains("libraryPayload"))
        assertTrue("JSON should contain preferencesPayload", exportedJson.contains("preferencesPayload"))

        // Reset
        coreDatabase.fakeRiteSessionDao.sessions.clear()
        coreDatabase.fakeLibraryDao.highlights.clear()

        val restoreResult = repository.restoreBackupJson(exportedJson, RestoreMode.MERGE)
        assertTrue(restoreResult.success)
        assertEquals(1, restoreResult.restoredRitesCount)
        assertEquals(1, restoreResult.restoredHighlightsCount)

        assertEquals("06:30", briefPrefs.targetWakeWd.first())
        assertEquals("08:30", briefPrefs.targetWakeWe.first())
        assertEquals("WEEKDAY_CLOCK", briefPrefs.pulsePmMode.first())
        assertEquals(com.neon.ascent.core.domain.goals.models.DirectiveIntakeMode.FORM, directivePrefs.directiveIntakeMode.first())
        assertEquals(1, coreDatabase.fakeRiteSessionDao.sessions.size)
        assertEquals("rite_1", coreDatabase.fakeRiteSessionDao.sessions.first().id)
        assertEquals(1, coreDatabase.fakeLibraryDao.highlights.size)
        assertEquals("book_cyber", coreDatabase.fakeLibraryDao.highlights.first().bookId)
    }

    @Test
    fun `restore v3 payload without preferences or workout graph sections succeeds without crashing`() = runBlocking {
        val v3Json = """
        {
          "version": 3,
          "exportedAt": "2026-10-21T12:00:00Z",
          "appVersion": "1.0",
          "workoutPayload": {
            "sessions": [],
            "logs": [],
            "sets": [],
            "userProfile": {
              "userId": "default_user",
              "experienceLevel": "ADVANCED",
              "somatotype": "ECTOMORPH",
              "activeProtocol": "PUSH_PULL_LEGS"
            }
          },
          "settingsPayload": {
            "themeMode": "CYBER",
            "measurementUnit": "Metric"
          }
        }
        """.trimIndent()

        val result = repository.restoreBackupJson(v3Json, RestoreMode.MERGE)
        assertTrue(result.success)

        val restoredProfile = workoutDao.getUserProfile("default_user").first()
        assertNotNull(restoredProfile)
        assertEquals("ADVANCED", restoredProfile?.experienceLevel)
        assertEquals("PUSH_PULL_LEGS", restoredProfile?.activeProtocol)
        assertEquals("[]", restoredProfile?.scheduledDays)
        assertEquals(0, restoredProfile?.rotationIndex)
    }

    @Test
    fun `restoreBackupJson emits progress stages in sequence`() = runBlocking {
        val stages = mutableListOf<com.neon.ascent.core.domain.backup.models.RestoreStage>()
        val exportedJson = repository.exportBackupJson(BackupScope())

        val result = repository.restoreBackupJson(exportedJson, RestoreMode.MERGE) { progress ->
            stages.add(progress.stage)
        }

        assertTrue(result.success)
        assertTrue(stages.contains(com.neon.ascent.core.domain.backup.models.RestoreStage.READING))
        assertTrue(stages.contains(com.neon.ascent.core.domain.backup.models.RestoreStage.PARSE))
        assertTrue(stages.contains(com.neon.ascent.core.domain.backup.models.RestoreStage.CHECKSUM))
        assertTrue(stages.contains(com.neon.ascent.core.domain.backup.models.RestoreStage.RESTORE_WORKOUT))
        assertTrue(stages.contains(com.neon.ascent.core.domain.backup.models.RestoreStage.RESTORE_DOMAINS))
        assertTrue(stages.contains(com.neon.ascent.core.domain.backup.models.RestoreStage.RESTORE_PREFS))
        assertTrue(stages.contains(com.neon.ascent.core.domain.backup.models.RestoreStage.INTEGRITY))
        assertTrue(stages.contains(com.neon.ascent.core.domain.backup.models.RestoreStage.READY))
    }

    @Test
    fun `restore v4 payload succeeds and handles missing v5 sections`() = runBlocking {
        val v4Json = """
        {
          "version": 4,
          "exportedAt": "2026-10-21T12:00:00Z",
          "appVersion": "1.0",
          "workoutPayload": {
            "sessions": [
              {
                "id": "v4_session_1",
                "dateEpochMs": 1761000000000,
                "protocol": "CYBER_PUMP",
                "durationSeconds": 3600,
                "experienceLevel": "ADVANCED",
                "somatotype": "MESOMORPH"
              }
            ],
            "logs": [],
            "sets": []
          },
          "preferencesPayload": {
            "defaultRestTimeSeconds": 90,
            "autoStartRestTimer": true,
            "workSetRestTimeSeconds": 120,
            "workoutLayoutMode": "CLASSIC"
          }
        }
        """.trimIndent()

        val result = repository.restoreBackupJson(v4Json, RestoreMode.MERGE)
        assertTrue(result.success)
        assertEquals(1, result.restoredSessionsCount)
        assertEquals(0, result.restoredRitesCount)
        assertEquals(0, result.restoredHighlightsCount)
    }

    @Test
    fun `restoreBackupJson emits FAIL stage on malformed JSON`() = runBlocking {
        val stages = mutableListOf<com.neon.ascent.core.domain.backup.models.RestoreStage>()
        var failureMessage: String? = null

        val result = repository.restoreBackupJson("{ invalid json format }", RestoreMode.MERGE) { progress ->
            stages.add(progress.stage)
            if (progress.stage == com.neon.ascent.core.domain.backup.models.RestoreStage.FAIL) {
                failureMessage = progress.error ?: progress.message
            }
        }

        assertFalse(result.success)
        assertTrue(stages.contains(com.neon.ascent.core.domain.backup.models.RestoreStage.FAIL))
        assertNotNull(failureMessage)
    }

    @Test
    fun `export and restore custom exercises round trip preserves custom variants under family and allows ranking`() = runBlocking {
        val customFamily = ExerciseDefinitionEntity(
            id = "custom_fam_chest_dip",
            name = "Chest Dip",
            description = "Custom bodyweight chest dip family",
            cues = listOf("Lean forward", "Go deep"),
            muscleGroups = listOf("Horizontal Press"),
            equipment = listOf("Bodyweight"),
            gifAssetPath = null,
            isLockedClassic = false,
            injurySubstitutions = emptyList(),
            movementType = "COMPOUND_UPPER",
            familyId = "custom_fam_chest_dip",
            familyName = "Chest Dip",
            implement = "BODYWEIGHT",
            stance = "STANDARD",
            tags = listOf("FORM_PRIORITY", "LOWER_BACK_FRIENDLY"),
            isCustom = true,
            isPrimaryVariant = true
        )

        val customVariant = ExerciseDefinitionEntity(
            id = "custom_ex_weighted_chest_dip",
            name = "Weighted Chest Dip",
            description = "Custom weighted chest dip",
            cues = listOf("Add weight belt", "Lean forward"),
            muscleGroups = listOf("Horizontal Press"),
            equipment = listOf("Bodyweight"),
            gifAssetPath = null,
            isLockedClassic = false,
            injurySubstitutions = emptyList(),
            movementType = "COMPOUND_UPPER",
            familyId = "custom_fam_chest_dip",
            familyName = "Chest Dip",
            implement = "BODYWEIGHT",
            stance = "STANDARD",
            tags = listOf("FORM_PRIORITY", "AXIAL_LOAD"),
            isCustom = true,
            isPrimaryVariant = false
        )

        workoutDao.insertExerciseDefinition(customFamily)
        workoutDao.insertExerciseDefinition(customVariant)

        val exportedJson = repository.exportBackupJson(BackupScope(includeWorkout = true))
        assertTrue("Exported JSON should contain customExercises", exportedJson.contains("customExercises"))
        assertTrue("Exported JSON should contain custom exercise ID", exportedJson.contains("custom_ex_weighted_chest_dip"))

        workoutDao.exercises.remove("custom_fam_chest_dip")
        workoutDao.exercises.remove("custom_ex_weighted_chest_dip")
        assertEquals(0, workoutDao.exercises.values.count { it.isCustom })

        val restoreResult = repository.restoreBackupJson(exportedJson, RestoreMode.MERGE)
        assertTrue(restoreResult.success)

        val restoredCustoms = workoutDao.exercises.values.filter { it.isCustom }
        assertEquals(2, restoredCustoms.size)

        val restoredFamily = restoredCustoms.find { it.id == "custom_fam_chest_dip" }
        val restoredVariant = restoredCustoms.find { it.id == "custom_ex_weighted_chest_dip" }

        assertNotNull(restoredFamily)
        assertNotNull(restoredVariant)
        assertEquals("Chest Dip", restoredFamily?.familyName)
        assertEquals("Chest Dip", restoredVariant?.familyName)
        assertTrue(restoredFamily!!.isPrimaryVariant)
        assertFalse(restoredVariant!!.isPrimaryVariant)

        val domainFamily = restoredFamily.toDomain()
        val domainVariant = restoredVariant.toDomain()
        val ranked = ExerciseRanking.rankAlternatives(domainFamily, listOf(domainVariant))

        assertEquals(1, ranked.size)
        assertEquals("custom_ex_weighted_chest_dip", ranked.first().id)
    }

    @Test
    fun `export and restore payload v6 round trip preserves daily origin day links and chronicle entries`() = runBlocking {
        val entry = ChronicleEntry(
            source = "journal",
            sourceId = "chronicle_v6_101",
            wing = "CHRONICLE",
            room = "ORIGIN",
            content = "Daily Origin Day Card reflection note for backup v6",
            timestamp = 1761000000000L,
            localDate = "2026-10-23"
        )
        chronicleRepository.saveEntry(entry)

        // Act - export payload v10
        val exportedJson = repository.exportBackupJson(BackupScope())
        assertTrue("JSON should contain version 10", exportedJson.contains("\"version\": 10"))
        assertTrue("JSON should contain chronicle entry", exportedJson.contains("Daily Origin Day Card reflection note for backup v6"))
        assertTrue("JSON should contain dayLinks or localDate", exportedJson.contains("2026-10-23"))

        // Reset fake repo
        chronicleRepository.entries.clear()

        // Act - restore payload
        val restoreResult = repository.restoreBackupJson(exportedJson, RestoreMode.MERGE)
        assertTrue(restoreResult.success)

        // Assert - day link and chronicle entry survive round-trip
        val restoredEntries = chronicleRepository.observeChronicle().first()
        assertEquals(1, restoredEntries.size)
        val restoredEntry = restoredEntries[0]
        assertEquals("chronicle_v6_101", restoredEntry.sourceId)
        assertEquals("Daily Origin Day Card reflection note for backup v6", restoredEntry.content)
        assertEquals("2026-10-23", restoredEntry.localDate)
    }

    @Test
    fun `export and restore payload v8 round trip preserves active diet selection and diet overrides`() = runBlocking {
        val dietPrefs = com.neon.ascent.core.data.datastore.DietPreferencesDataStore(testContext)

        // Set active diet and overrides
        dietPrefs.setActiveDietId("cyber_cyclical_diet")
        dietPrefs.updateDietOverrides(
            com.neon.ascent.core.domain.labs.models.DietOverrides(
                preferredProteinGramsPerKg = 2.0,
                preferredModestCarbTargetGrams = 80,
                preferredHigherCarbMealDays = listOf("TUESDAY", "SATURDAY"),
                preferredHigherCarbMealsPerWeek = 2,
                freeListExceptions = listOf("sweet_corn"),
                notes = "Target 2.0g/kg protein"
            )
        )

        val exportedJson = repository.exportBackupJson(BackupScope())

        assertTrue("JSON should contain version 10", exportedJson.contains("\"version\": 10"))
        assertTrue("JSON should contain dietPayload", exportedJson.contains("dietPayload"))
        assertTrue("JSON should contain activeDietId", exportedJson.contains("\"activeDietId\": \"cyber_cyclical_diet\""))
        assertTrue("JSON should contain preferredProteinGramsPerKg", exportedJson.contains("\"preferredProteinGramsPerKg\": 2.0"))

        // Reset active diet and overrides to simulate fresh restore
        dietPrefs.setActiveDietId(null)
        dietPrefs.updateDietOverrides(com.neon.ascent.core.domain.labs.models.DietOverrides())
        assertNull(dietPrefs.activeDietId.first())

        val restoreResult = repository.restoreBackupJson(exportedJson, RestoreMode.REPLACE)
        assertTrue("Restore should succeed", restoreResult.success)

        val restoredActiveDietId = dietPrefs.activeDietId.first()
        assertEquals("cyber_cyclical_diet", restoredActiveDietId)

        val restoredOverrides = dietPrefs.dietOverrides.first()
        assertEquals(2.0, restoredOverrides.preferredProteinGramsPerKg!!, 0.001)
        assertEquals(80, restoredOverrides.preferredModestCarbTargetGrams)
        assertEquals(listOf("TUESDAY", "SATURDAY"), restoredOverrides.preferredHigherCarbMealDays)
        assertEquals(2, restoredOverrides.preferredHigherCarbMealsPerWeek)
        assertEquals(listOf("sweet_corn"), restoredOverrides.freeListExceptions)
        assertEquals("Target 2.0g/kg protein", restoredOverrides.notes)
    }

    @Test
    fun `export and restore payload v9 round trip preserves active supplement stack and overrides`() = runBlocking {
        val suppPrefs = com.neon.ascent.core.data.datastore.SupplementPreferencesDataStore(testContext)

        // Set active stack and overrides
        val targetStack = setOf("caffeine", "l_theanine")
        suppPrefs.setActiveStackIds(targetStack)
        val targetOverride = com.neon.ascent.core.domain.labs.models.SupplementOverride(
            customDoseNote = "100 mg AM espresso",
            scheduleNote = "Mon-Fri AM",
            isPaused = false
        )
        suppPrefs.updateOverride("caffeine", targetOverride)

        val exportedJson = repository.exportBackupJson(BackupScope())

        assertTrue("JSON should contain version 10", exportedJson.contains("\"version\": 10"))
        assertTrue("JSON should contain supplementPayload", exportedJson.contains("supplementPayload"))
        assertTrue("JSON should contain caffeine", exportedJson.contains("caffeine"))
        assertTrue("JSON should contain l_theanine", exportedJson.contains("l_theanine"))
        assertTrue("JSON should contain customDoseNote", exportedJson.contains("100 mg AM espresso"))

        // Reset preferences to simulate fresh restore
        suppPrefs.setActiveStackIds(emptySet())
        suppPrefs.updateAllOverrides(emptyMap())
        assertTrue(suppPrefs.activeStackIds.first().isEmpty())

        val restoreResult = repository.restoreBackupJson(exportedJson, RestoreMode.REPLACE)
        assertTrue("Restore should succeed", restoreResult.success)

        val restoredStack = suppPrefs.activeStackIds.first()
        assertEquals(targetStack, restoredStack)

        val restoredOverrides = suppPrefs.overrides.first()
        assertEquals(1, restoredOverrides.size)
        val caffeineOverride = restoredOverrides["caffeine"]
        assertNotNull(caffeineOverride)
        assertEquals("100 mg AM espresso", caffeineOverride?.customDoseNote)
        assertEquals("Mon-Fri AM", caffeineOverride?.scheduleNote)
        assertFalse(caffeineOverride!!.isPaused)
    }

    @Test
    fun `export and restore payload v10 round trip preserves active biohacking protocols and overrides`() = runBlocking {
        val protoPrefs = com.neon.ascent.core.data.datastore.BiohackingProtocolPreferencesDataStore(testContext)

        // Set active protocols and overrides
        val targetProtocols = setOf("finnish_sauna", "cold_water_immersion")
        protoPrefs.setActiveProtocolIds(targetProtocols)
        val targetOverride = com.neon.ascent.core.domain.labs.models.BiohackingProtocolOverride(
            preferredFrequency = "4x/week post-workout",
            durationNote = "20 min @ 85C",
            notes = "Hydrate with electrolytes",
            isPaused = false
        )
        protoPrefs.updateOverride("finnish_sauna", targetOverride)

        val exportedJson = repository.exportBackupJson(BackupScope())

        assertTrue("JSON should contain version 10", exportedJson.contains("\"version\": 10"))
        assertTrue("JSON should contain biohackingProtocolPayload", exportedJson.contains("biohackingProtocolPayload"))
        assertTrue("JSON should contain finnish_sauna", exportedJson.contains("finnish_sauna"))
        assertTrue("JSON should contain cold_water_immersion", exportedJson.contains("cold_water_immersion"))
        assertTrue("JSON should contain preferredFrequency", exportedJson.contains("4x/week post-workout"))
        assertTrue("JSON should contain durationNote", exportedJson.contains("20 min @ 85C"))

        // Reset preferences to simulate fresh restore
        protoPrefs.setActiveProtocolIds(emptySet())
        protoPrefs.updateAllOverrides(emptyMap())
        assertTrue(protoPrefs.activeProtocolIds.first().isEmpty())

        val restoreResult = repository.restoreBackupJson(exportedJson, RestoreMode.REPLACE)
        assertTrue("Restore should succeed", restoreResult.success)

        val restoredProtocols = protoPrefs.activeProtocolIds.first()
        assertEquals(targetProtocols, restoredProtocols)

        val restoredOverrides = protoPrefs.overrides.first()
        assertEquals(1, restoredOverrides.size)
        val saunaOverride = restoredOverrides["finnish_sauna"]
        assertNotNull(saunaOverride)
        assertEquals("4x/week post-workout", saunaOverride?.preferredFrequency)
        assertEquals("20 min @ 85C", saunaOverride?.durationNote)
        assertEquals("Hydrate with electrolytes", saunaOverride?.notes)
        assertFalse(saunaOverride!!.isPaused)
    }

    @Test
    fun `legacy payload v8 restores cleanly when supplementPayload is absent`() = runBlocking {
        val legacyJson = """
        {
          "version": 8,
          "exportedAt": "2026-10-23T12:00:00Z",
          "appVersion": "1.0",
          "dietPayload": {
            "activeDietId": "cyber_cyclical_diet"
          }
        }
        """.trimIndent()

        val restoreResult = repository.restoreBackupJson(legacyJson, RestoreMode.MERGE)
        assertTrue("Legacy v8 payload restore should succeed", restoreResult.success)
    }

    @Test
    fun `legacy payload v9 restores cleanly when biohackingProtocolPayload is absent`() = runBlocking {
        val legacyJson = """
        {
          "version": 9,
          "exportedAt": "2026-10-23T12:00:00Z",
          "appVersion": "1.0",
          "dietPayload": {
            "activeDietId": "cyber_cyclical_diet"
          },
          "supplementPayload": {
            "activeStackIds": ["caffeine"]
          }
        }
        """.trimIndent()

        val restoreResult = repository.restoreBackupJson(legacyJson, RestoreMode.MERGE)
        assertTrue("Legacy v9 payload restore should succeed", restoreResult.success)
    }

    @Test
    fun `export and restore preserves clusterMiniSetIndex and set flags`() = runBlocking {
        val session = WorkoutSessionEntity(
            id = "sess_cluster_1",
            date = Instant.parse("2026-10-26T10:00:00Z"),
            protocol = "CYBER_CRAPP",
            durationSeconds = 2400,
            notes = "Cluster workout test",
            experienceLevel = "INTERMEDIATE",
            somatotype = "MESOMORPH"
        )
        val log = WorkoutLogEntity(
            id = "log_c1",
            sessionId = "sess_cluster_1",
            exerciseId = "ex_press",
            order = 0,
            exerciseName = "Incline Dumbbell Press",
            protocolOverride = null
        )
        val set1 = SetLogEntity(
            id = "set_c1",
            workoutLogId = "log_c1",
            weight = 85f,
            reps = 8,
            setType = "REST_PAUSE",
            goalReps = "8-10",
            isCompleted = true,
            rir = 0,
            isWarmup = false,
            timestamp = Instant.parse("2026-10-26T10:05:00Z"),
            clusterMiniSetIndex = 1,
            isLengthenedPartial = false,
            isLoadedStretch = false,
            stretchDurationSeconds = null
        )
        val set2 = SetLogEntity(
            id = "set_c2",
            workoutLogId = "log_c1",
            weight = 85f,
            reps = 6,
            setType = "REST_PAUSE",
            goalReps = "8-10",
            isCompleted = true,
            rir = 0,
            isWarmup = false,
            timestamp = Instant.parse("2026-10-26T10:05:15Z"),
            clusterMiniSetIndex = 2,
            isLengthenedPartial = true,
            isLoadedStretch = true,
            stretchDurationSeconds = 10
        )

        workoutDao.upsertSession(session)
        workoutDao.upsertWorkoutLog(log)
        workoutDao.upsertSetLog(set1)
        workoutDao.upsertSetLog(set2)

        val exportedJson = repository.exportBackupJson(BackupScope(includeWorkout = true))
        assertTrue("Exported JSON contains clusterMiniSetIndex", exportedJson.contains("clusterMiniSetIndex"))
        assertTrue("Exported JSON contains isLengthenedPartial", exportedJson.contains("isLengthenedPartial"))

        // Reset fake DB and restore
        workoutDao.sessions.clear()
        workoutDao.logs.clear()
        workoutDao.sets.clear()

        val restoreResult = repository.restoreBackupJson(exportedJson, RestoreMode.REPLACE)
        assertTrue(restoreResult.success)

        val restoredSets = workoutDao.sets.values.toList()
        assertEquals(2, restoredSets.size)

        val s1 = restoredSets.find { it.id == "set_c1" }
        assertEquals(1, s1?.clusterMiniSetIndex)
        assertEquals(false, s1?.isLengthenedPartial)

        val s2 = restoredSets.find { it.id == "set_c2" }
        assertEquals(2, s2?.clusterMiniSetIndex)
        assertEquals(true, s2?.isLengthenedPartial)
        assertEquals(true, s2?.isLoadedStretch)
        assertEquals(10, s2?.stretchDurationSeconds)
    }
}

