package com.neon.ascent.feature.goals.ui.ascension

import com.neon.ascent.core.common.DopamineCoordinator
import com.neon.ascent.core.common.HapticService
import com.neon.ascent.core.data.datastore.DirectivePreferencesDataStore
import com.neon.ascent.core.data.repository.SelfMapRepository
import com.neon.ascent.core.domain.NeuralPingScheduler
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.core.domain.repository.SkillRepository
import com.neon.ascent.feature.goals.domain.usecases.NeonMentorUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AscensionForgeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeAscensionRepository : AscensionRepository {
        val directives = mutableListOf<AscensionDirective>()
        val missions = mutableListOf<AscensionMission>()
        val tasks = mutableListOf<AscensionTask>()

        var shouldThrowOnTaskInsert = false
        var shouldThrowOnMissionInsert = false

        private val directivesFlow = MutableStateFlow<List<AscensionDirective>>(emptyList())
        private val missionsFlow = MutableStateFlow<List<AscensionMission>>(emptyList())

        private fun updateFlows() {
            directivesFlow.value = directives.toList()
            missionsFlow.value = missions.toList()
        }

        override fun getAllDirectives(): Flow<List<AscensionDirective>> = directivesFlow
        override fun getActiveDirectives(): Flow<List<AscensionDirective>> = directivesFlow

        override suspend fun insertDirective(directive: AscensionDirective) {
            directives.add(directive)
            updateFlows()
        }

        override suspend fun updateDirective(directive: AscensionDirective) {}
        override suspend fun updateDirectiveNotes(id: String, notes: String) {}
        override suspend fun deleteDirective(id: String) {}

        override fun getMissionsForDirective(directiveId: String): Flow<List<AscensionMission>> = flowOf(missions.filter { it.directiveId == directiveId })
        override fun getActiveMissions(): Flow<List<AscensionMission>> = missionsFlow

        override suspend fun insertMission(mission: AscensionMission) {
            if (shouldThrowOnMissionInsert) {
                throw IllegalStateException("Mission insert error")
            }
            missions.add(mission)
            updateFlows()
        }

        override suspend fun updateMission(mission: AscensionMission) {}

        override fun getTasksForParent(parentId: String): Flow<List<AscensionTask>> = flowOf(tasks.filter { it.parentId == parentId })
        override fun getTaskById(id: String): Flow<AscensionTask?> = flowOf(tasks.find { it.id == id })
        override fun getAllRecurringTasks(): Flow<List<AscensionTask>> = flowOf(tasks.filter { it.type == AscensionTaskType.RECURRING })

        override suspend fun insertTask(task: AscensionTask) {
            if (shouldThrowOnTaskInsert) {
                throw IllegalStateException("Database write error")
            }
            tasks.add(task)
        }

        override suspend fun updateTask(task: AscensionTask) {}
        override suspend fun deleteTask(id: String) {}
        override suspend fun completeTask(task: AscensionTask, notes: String?, mood: Int?, linkedHealthSnapshot: String?) {}
        override fun getCompletionsForTask(taskId: String): Flow<List<AscensionTaskCompletion>> = flowOf(emptyList())
        override fun getCompletionsInRange(startTime: java.time.Instant): Flow<List<AscensionTaskCompletion>> = flowOf(emptyList())
        override suspend fun insertNeuralLog(title: String, content: String, type: String) {}
        override fun getAllNeuralLogs(): Flow<List<NeuralLog>> = flowOf(emptyList())
    }

    private class FakeNeuralPingScheduler : NeuralPingScheduler {
        var pingCount = 0
        override suspend fun scheduleSmartPings() { pingCount++ }
        override fun enqueueDailyNeuralBrief(isTestRequest: Boolean) {}
        override fun triggerExpeditedBrief(reason: String) {}
    }

    private class FakeHapticService : HapticService(mock(android.content.Context::class.java)) {
        override fun heartbeat() {}
        override fun syncSuccess() {}
        override fun clusterTimerBuzz() {}
        override fun alertRestOver() {}
        override fun ascensionBurst() {}
        override fun breathingPulse() {}
        override fun holdVibration(durationMillis: Long) {}
        override fun cancelVibration() {}
    }

    private lateinit var repository: FakeAscensionRepository
    private lateinit var neuralPingScheduler: FakeNeuralPingScheduler
    private lateinit var dopamineCoordinator: DopamineCoordinator
    private lateinit var viewModel: AscensionForgeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeAscensionRepository()
        neuralPingScheduler = FakeNeuralPingScheduler()

        dopamineCoordinator = DopamineCoordinator(FakeHapticService())

        val mockMentorUseCase = mock(NeonMentorUseCase::class.java)
        val mockSkillRepository = mock(SkillRepository::class.java)
        val mockSelfMapRepository = mock(SelfMapRepository::class.java)
        val mockPreferencesDataStore = mock(DirectivePreferencesDataStore::class.java)

        org.mockito.BDDMockito.given(mockPreferencesDataStore.directiveIntakeMode)
            .willReturn(flowOf(DirectiveIntakeMode.MENTOR))
        org.mockito.BDDMockito.given(mockSelfMapRepository.activeItems)
            .willReturn(flowOf(emptyList()))

        viewModel = AscensionForgeViewModel(
            repository = repository,
            mentorUseCase = mockMentorUseCase,
            skillRepository = mockSkillRepository,
            dopamineCoordinator = dopamineCoordinator,
            neuralPingScheduler = neuralPingScheduler,
            selfMapRepository = mockSelfMapRepository,
            directivePreferencesDataStore = mockPreferencesDataStore
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `acceptProposals inserts Directive then Missions then Tasks with correct hierarchy and fields`() = runTest {
        viewModel.updateTitle("Directive Alpha")
        viewModel.updateDescription("Objective Alpha Description")
        viewModel.updateTargetEndDate(LocalDate.of(2026, 12, 31))
        viewModel.updatePriorityTier(PriorityTier.A)
        viewModel.updateOutcomeStatement("Quantified outcome 100%")
        viewModel.updateAffirmation("I am unstoppable")
        viewModel.updateVisualUri("http://image.png")
        viewModel.toggleAttribute(SpecialType.STRENGTH)

        val proposedMissions = listOf(
            ProposedMission(
                title = "Mission 1",
                description = "Mission 1 Description",
                tasks = listOf(
                    ProposedTask(title = "Task 1A", description = "Pulse 1A", type = AscensionTaskType.ONE_TIME),
                    ProposedTask(title = "Task 1B", description = "Pulse 1B", type = AscensionTaskType.ONE_TIME)
                )
            )
        )

        viewModel.acceptProposals(proposedMissions, emptyList())
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.directives.size)
        val directive = repository.directives.first()
        assertEquals("Directive Alpha", directive.title)
        assertEquals("Objective Alpha Description", directive.description)
        assertEquals(LocalDate.of(2026, 12, 31), directive.targetEndDate)
        assertEquals(PriorityTier.A, directive.priorityTier)
        assertEquals("Quantified outcome 100%", directive.outcomeStatement)
        assertEquals("I am unstoppable", directive.affirmation)
        assertEquals("http://image.png", directive.visualUri)
        assertTrue(directive.linkedAttributes.contains(SpecialType.STRENGTH))

        assertEquals(1, repository.missions.size)
        val mission = repository.missions.first()
        assertEquals("Mission 1", mission.title)
        assertEquals(directive.id, mission.directiveId)

        assertEquals(2, repository.tasks.size)
        val task1 = repository.tasks[0]
        val task2 = repository.tasks[1]
        assertEquals("Task 1A", task1.title)
        assertEquals(mission.id, task1.parentId)
        assertEquals("Task 1B", task2.title)
        assertEquals(mission.id, task2.parentId)

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertEquals(directive.id, state.deployedDirectiveId)
        assertNull(state.errorMessage)
    }

    @Test
    fun `acceptProposals failure sets error state and does not mark success`() = runTest {
        repository.shouldThrowOnTaskInsert = true
        viewModel.updateTitle("Directive Failure")

        val proposedMissions = listOf(
            ProposedMission(
                title = "Mission 1",
                description = "Desc",
                tasks = listOf(ProposedTask(title = "Task 1", description = "Desc", type = AscensionTaskType.ONE_TIME))
            )
        )

        viewModel.acceptProposals(proposedMissions, emptyList())
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSuccess)
        assertNull(state.deployedDirectiveId)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("CASCADE_DEPLOY_FAILED"))
    }

    @Test
    fun `acceptProposals failure on Mission insert sets error state and does not mark success`() = runTest {
        repository.shouldThrowOnMissionInsert = true
        viewModel.updateTitle("Directive Mission Failure")

        val proposedMissions = listOf(
            ProposedMission(
                title = "Mission Failure",
                description = "Desc",
                tasks = emptyList()
            )
        )

        viewModel.acceptProposals(proposedMissions, emptyList())
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSuccess)
        assertNull(state.deployedDirectiveId)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("CASCADE_DEPLOY_FAILED"))
    }

    @Test
    fun `plain save writes Directive only without auto-cascade`() = runTest {
        viewModel.updateType(ForgeType.DIRECTIVE)
        viewModel.updateTitle("Standalone Directive")

        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.directives.size)
        assertEquals(0, repository.missions.size)
        assertEquals(0, repository.tasks.size)

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertEquals(repository.directives.first().id, state.deployedDirectiveId)
    }

    @Test
    fun `task creation prefers parentMissionId over parentDirectiveId`() = runTest {
        viewModel.updateType(ForgeType.TASK)
        viewModel.updateTitle("New Pulse Task")
        viewModel.updateParentDirective("dir_123")
        viewModel.updateParentMission("mission_456")

        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.tasks.size)
        val task = repository.tasks.first()
        assertEquals("New Pulse Task", task.title)
        assertEquals("mission_456", task.parentId)
    }

    @Test
    fun `task creation falls back to parentDirectiveId when parentMissionId is null`() = runTest {
        viewModel.updateType(ForgeType.TASK)
        viewModel.updateTitle("New Pulse Task 2")
        viewModel.updateParentDirective("dir_789")
        viewModel.updateParentMission(null)

        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.tasks.size)
        val task = repository.tasks.first()
        assertEquals("New Pulse Task 2", task.title)
        assertEquals("dir_789", task.parentId)
    }
}
