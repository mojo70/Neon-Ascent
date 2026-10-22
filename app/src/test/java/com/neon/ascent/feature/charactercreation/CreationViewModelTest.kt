package com.neon.ascent.feature.charactercreation

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import com.neon.ascent.core.domain.NeuralPingScheduler
import com.neon.ascent.core.domain.backup.models.BackupScope
import com.neon.ascent.core.domain.backup.models.RestoreMode
import com.neon.ascent.core.domain.backup.models.RestoreResult
import com.neon.ascent.core.domain.character.models.UserCharacter
import com.neon.ascent.core.domain.character.repository.CharacterRepository
import com.neon.ascent.core.domain.goals.usecases.GenerateMissionsFromAspirationsUseCase
import com.neon.ascent.core.domain.goals.usecases.SeedStarterHabitsUseCase
import com.neon.ascent.core.domain.repository.FullDataBackupRepository
import com.neon.ascent.data.backup.p2p.P2pMigrationClient
import com.neon.ascent.data.repository.UserPreferencesRepository
import com.neon.ascent.domain.onboarding.OnboardingCompletionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class CreationViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class TestContext(private val baseDir: File) : ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = baseDir
    }

    private class FakeCharacterRepository : CharacterRepository {
        var savedCharacter: UserCharacter? = null

        override fun getUserCharacter(): Flow<UserCharacter?> = flowOf(savedCharacter)
        override suspend fun saveCharacter(character: UserCharacter) {
            savedCharacter = character
        }
        override suspend fun resetCharacter() {
            savedCharacter = null
        }
    }

    private class FakeUserPreferencesRepository(context: Context) : UserPreferencesRepository(context) {
        var onboardingCompleted = false

        override suspend fun setOnboardingComplete(complete: Boolean) {
            onboardingCompleted = complete
        }
    }

    private class FakeFullDataBackupRepository : FullDataBackupRepository {
        var restoreSuccess = true
        var lastRestoredJson: String? = null

        override suspend fun exportBackupJson(scope: BackupScope): String = "{}"
        override suspend fun restoreBackupJson(jsonString: String, mode: RestoreMode): RestoreResult {
            lastRestoredJson = jsonString
            return RestoreResult(success = restoreSuccess, message = if (restoreSuccess) "Success" else "Error")
        }
    }

    private class FakeNeuralPingScheduler : NeuralPingScheduler {
        override suspend fun scheduleSmartPings() {}
        override fun enqueueDailyNeuralBrief(isTestRequest: Boolean) {}
        override fun triggerExpeditedBrief(reason: String) {}
    }

    private lateinit var characterRepository: FakeCharacterRepository
    private lateinit var userPreferencesRepository: FakeUserPreferencesRepository
    private lateinit var backupRepository: FakeFullDataBackupRepository
    private lateinit var viewModel: CreationViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val testContext = TestContext(tempFolder.root)
        characterRepository = FakeCharacterRepository()
        userPreferencesRepository = FakeUserPreferencesRepository(testContext)
        backupRepository = FakeFullDataBackupRepository()

        val fakeAscensionRepo = object : com.neon.ascent.core.domain.repository.AscensionRepository {
            override fun getAllDirectives() = flowOf(emptyList<com.neon.ascent.core.domain.goals.models.AscensionDirective>())
            override fun getActiveDirectives() = flowOf(emptyList<com.neon.ascent.core.domain.goals.models.AscensionDirective>())
            override suspend fun insertDirective(directive: com.neon.ascent.core.domain.goals.models.AscensionDirective) {}
            override suspend fun updateDirective(directive: com.neon.ascent.core.domain.goals.models.AscensionDirective) {}
            override suspend fun updateDirectiveNotes(id: String, notes: String) {}
            override suspend fun deleteDirective(id: String) {}
            override fun getMissionsForDirective(directiveId: String) = flowOf(emptyList<com.neon.ascent.core.domain.goals.models.AscensionMission>())
            override fun getActiveMissions() = flowOf(emptyList<com.neon.ascent.core.domain.goals.models.AscensionMission>())
            override suspend fun insertMission(mission: com.neon.ascent.core.domain.goals.models.AscensionMission) {}
            override suspend fun updateMission(mission: com.neon.ascent.core.domain.goals.models.AscensionMission) {}
            override fun getTasksForParent(parentId: String) = flowOf(emptyList<com.neon.ascent.core.domain.goals.models.AscensionTask>())
            override fun getTaskById(id: String) = flowOf(null)
            override fun getAllRecurringTasks() = flowOf(emptyList<com.neon.ascent.core.domain.goals.models.AscensionTask>())
            override suspend fun insertTask(task: com.neon.ascent.core.domain.goals.models.AscensionTask) {}
            override suspend fun updateTask(task: com.neon.ascent.core.domain.goals.models.AscensionTask) {}
            override suspend fun deleteTask(id: String) {}
            override suspend fun completeTask(task: com.neon.ascent.core.domain.goals.models.AscensionTask, notes: String?, mood: Int?, linkedHealthSnapshot: String?) {}
            override fun getCompletionsForTask(taskId: String) = flowOf(emptyList<com.neon.ascent.core.domain.goals.models.AscensionTaskCompletion>())
            override fun getCompletionsInRange(startTime: java.time.Instant) = flowOf(emptyList<com.neon.ascent.core.domain.goals.models.AscensionTaskCompletion>())
            override suspend fun insertNeuralLog(title: String, content: String, type: String) {}
            override fun getAllNeuralLogs() = flowOf(emptyList<com.neon.ascent.core.domain.goals.models.NeuralLog>())
        }

        val fakeOnboardingUseCase = OnboardingCompletionUseCase(
            seedStarterHabitsUseCase = SeedStarterHabitsUseCase(fakeAscensionRepo),
            generateMissionsFromAspirationsUseCase = GenerateMissionsFromAspirationsUseCase(fakeAscensionRepo),
            neuralPingScheduler = FakeNeuralPingScheduler()
        )

        viewModel = CreationViewModel(
            characterRepository = characterRepository,
            userPreferencesRepository = userPreferencesRepository,
            onboardingCompletionUseCase = fakeOnboardingUseCase,
            fullDataBackupRepository = backupRepository,
            p2pMigrationClient = P2pMigrationClient(backupRepository),
            application = Application()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `bypassOnboarding saves minimal character and sets onboarding complete`() = runBlocking {
        var navTriggered = false
        viewModel.bypassOnboarding {
            navTriggered = true
        }

        assertTrue(navTriggered)
        assertTrue(userPreferencesRepository.onboardingCompleted)
        assertNotNull(characterRepository.savedCharacter)
        assertEquals("OPERATIVE", characterRepository.savedCharacter?.name)
        assertTrue(characterRepository.savedCharacter?.isCreationComplete == true)
        assertEquals(CreationUiState.Success, viewModel.uiState.value)
    }

    @Test
    fun `restoreFromFile calls restoreBackupJson and sets onboarding complete`() = runBlocking {
        var successTriggered = false
        var errorTriggered = false

        viewModel.restoreFromFile(
            jsonContent = "{\"version\":3}",
            onSuccess = { successTriggered = true },
            onError = { errorTriggered = true }
        )

        assertTrue(successTriggered)
        assertFalse(errorTriggered)
        assertEquals("{\"version\":3}", backupRepository.lastRestoredJson)
        assertTrue(userPreferencesRepository.onboardingCompleted)
        assertEquals(CreationUiState.Success, viewModel.uiState.value)
    }

    @Test
    fun `abort saves draft character`() = runBlocking {
        var onCompleteCalled = false
        viewModel.abort {
            onCompleteCalled = true
        }

        assertTrue(onCompleteCalled)
        assertNotNull(characterRepository.savedCharacter)
    }

    @Test
    fun `restoreFromP2p calls downloadAndRestore and sets onboarding complete`() = runBlocking {
        val server = com.neon.ascent.data.backup.p2p.P2pMigrationServer()
        val info = server.startServer("{\"version\":3}")
        assertNotNull(info)

        var successTriggered = false
        var errorTriggered = false

        viewModel.restoreFromP2p(
            serverInfo = info!!,
            onSuccess = { successTriggered = true },
            onError = { errorTriggered = true }
        )

        // Allow async IO task to complete
        kotlinx.coroutines.delay(500)

        assertTrue(successTriggered)
        assertFalse(errorTriggered)
        assertTrue(userPreferencesRepository.onboardingCompleted)
        assertEquals(CreationUiState.Success, viewModel.uiState.value)

        server.stopServer()
    }
}
