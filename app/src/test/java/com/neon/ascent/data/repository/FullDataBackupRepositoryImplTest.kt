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
import com.neon.ascent.core.domain.backup.models.BackupScope
import com.neon.ascent.core.domain.backup.models.RestoreMode
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant

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

        override suspend fun upsertSession(session: WorkoutSessionEntity) {
            sessions[session.id] = session
        }

        override fun getAllSessions(): Flow<List<WorkoutSessionEntity>> = flowOf(sessions.values.toList())
        override suspend fun getSessionById(sessionId: String): WorkoutSessionEntity? = sessions[sessionId]

        override suspend fun insertExerciseDefinition(exercise: ExerciseDefinitionEntity) {
            exercises[exercise.id] = exercise
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

        override suspend fun insertProgressionState(state: ProgressionStateEntity) {}
        override fun getProgressionState(exerciseId: String): Flow<ProgressionStateEntity?> = flowOf(null)

        override suspend fun insertAccomplishments(entity: ExerciseAccomplishmentsEntity) {}
        override fun getAccomplishments(exerciseId: String): Flow<ExerciseAccomplishmentsEntity?> = flowOf(null)
        override fun getAllAccomplishments(): Flow<List<ExerciseAccomplishmentsEntity>> = flowOf(emptyList())

        override fun getAllRoutines(): Flow<List<WorkoutRoutineWithDetails>> = flowOf(emptyList())
        override suspend fun getRoutineLibraryStatus(id: String): Boolean? = null
        override suspend fun getAugmentLibraryStatus(id: String): Boolean? = null
        override suspend fun getRoutineById(id: String): WorkoutRoutineEntity? = null
        override suspend fun getAugmentById(id: String): WorkoutAugmentEntity? = null
        override suspend fun updateRoutineLibraryStatus(id: String, status: Boolean) {}
        override suspend fun updateAugmentLibraryStatus(id: String, status: Boolean) {}
        override suspend fun insertRoutine(routine: WorkoutRoutineEntity) {}
        override suspend fun insertRoutineExerciseCrossRef(crossRef: RoutineExerciseCrossRef) {}
        override suspend fun insertRoutineAugmentCrossRef(crossRef: RoutineAugmentCrossRef) {}
        override suspend fun insertRoutineSet(set: RoutineSetEntity) {}
        override suspend fun deleteRoutineSets(routineId: String) {}
        override suspend fun deleteRoutineExerciseCrossRefs(routineId: String) {}
        override suspend fun deleteRoutineAugmentCrossRefs(routineId: String) {}
        override suspend fun insertAugment(augment: WorkoutAugmentEntity) {}
        override suspend fun insertAugmentExerciseCrossRef(crossRef: AugmentExerciseCrossRef) {}
        override suspend fun deleteAugmentExerciseCrossRefs(augmentId: String) {}
        override suspend fun insertAugmentSet(set: AugmentSetEntity) {}
        override suspend fun deleteAugmentSets(augmentId: String) {}
        override fun getAllAugments(): Flow<List<WorkoutAugmentWithExercises>> = flowOf(emptyList())
        override suspend fun deleteAugment(augmentId: String) {}
        override suspend fun upsertAugmentActivation(activation: AugmentActivationEntity) {}
        override fun getActivationsByUserId(userId: String): Flow<List<AugmentActivationEntity>> = flowOf(emptyList())
        override fun getActiveActivations(): Flow<List<AugmentActivationEntity>> = flowOf(emptyList())
        override suspend fun endActivation(id: String) {}
        override suspend fun deleteRoutine(routineId: String) {}
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

        override fun countSessionsBetween(from: Instant, to: Instant): Flow<Int> = flowOf(sessions.size)
        override fun getSessionsWithDetailsBetween(from: Instant, to: Instant): Flow<List<WorkoutSessionWithLogs>> = flowOf(emptyList())
        override fun getSessionDatesAndDeloadBetween(from: Instant, to: Instant): Flow<List<SessionDateAndDeload>> = flowOf(emptyList())
        override fun getMuscleGroupsHitBetween(from: Instant, to: Instant): Flow<List<MuscleGroupsWrapper>> = flowOf(emptyList())
        override fun getLogsForExerciseBetween(exerciseId: String, from: Instant, to: Instant): Flow<List<WorkoutLogWithSets>> = flowOf(emptyList())
        override suspend fun upsertFuelSnapshot(snapshot: FuelSnapshotEntity) {}
        override fun getFuelHistory(from: Instant, to: Instant): Flow<List<FuelSnapshotEntity>> = flowOf(emptyList())
        override suspend fun upsertProtocolRepTarget(target: ProtocolRepTargetEntity) {}
        override fun getAllProtocolRepTargets(): Flow<List<ProtocolRepTargetEntity>> = flowOf(emptyList())
        override suspend fun upsertProtocolCycle(cycle: ProtocolCycleEntity) {}
        override fun getActiveCycle(userId: String): Flow<ProtocolCycleEntity?> = flowOf(null)
        override suspend fun getProtocolCycleById(id: String): ProtocolCycleEntity? = null

        override suspend fun upsertExerciseMax(max: ExerciseMaxEntity) {
            maxes[max.familyId] = max
        }

        override fun getExerciseMax(familyId: String): Flow<ExerciseMaxEntity?> = flowOf(maxes[familyId])
        override fun getAllExerciseMaxes(): Flow<List<ExerciseMaxEntity>> = flowOf(maxes.values.toList())
        override fun getAllUserProfiles(): Flow<List<UserWorkoutProfileEntity>> = flowOf(profiles.values.toList())
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

    private class TestNeonAscentDatabase : NeonAscentDatabase() {
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
        override fun libraryDao(): LibraryDao = throw NotImplementedError()
        override fun vaultDao(): VaultDao = throw NotImplementedError()
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

    private lateinit var workoutDao: FakeWorkoutDao
    private lateinit var journalDao: FakeJournalDao
    private lateinit var chronicleRepository: FakeChronicleRepository
    private lateinit var operativeProfileDao: FakeOperativeProfileDao
    private lateinit var bodySampleDao: FakeBodySampleDao
    private lateinit var dailyVitalRollupDao: FakeDailyVitalRollupDao
    private lateinit var coreDatabase: TestNeonAscentDatabase
    private lateinit var appDatabase: TestAppDatabase
    private lateinit var repository: FullDataBackupRepositoryImpl

    @Before
    fun setUp() {
        workoutDao = FakeWorkoutDao()
        journalDao = FakeJournalDao()
        chronicleRepository = FakeChronicleRepository()
        operativeProfileDao = FakeOperativeProfileDao()
        bodySampleDao = FakeBodySampleDao()
        dailyVitalRollupDao = FakeDailyVitalRollupDao()
        coreDatabase = TestNeonAscentDatabase()
        appDatabase = TestAppDatabase()
        val context = TestContext(tempFolder.root)

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
            userPreferencesRepository = FakeUserPreferencesRepository(context),
            settingsRepository = FakeSettingsRepository(context),
            coreDatabase = coreDatabase,
            appDatabase = appDatabase,
            context = context
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
        assertTrue(exportedJson.contains("\"version\": 2"))
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
}
