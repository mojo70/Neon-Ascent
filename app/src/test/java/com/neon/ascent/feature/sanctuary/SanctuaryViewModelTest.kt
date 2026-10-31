package com.neon.ascent.feature.sanctuary

import com.neon.ascent.core.data.local.seed.SanctuarySeeds
import com.neon.ascent.core.data.local.entity.toDomain
import com.neon.ascent.core.domain.sanctuary.models.SanctuaryPassage
import com.neon.ascent.core.domain.sanctuary.repository.SanctuaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeSanctuaryRepository : SanctuaryRepository {
    private val passages = SanctuarySeeds.SANCTUARY_SEED.map { it.toDomain() }

    override fun observeAllPassages(): Flow<List<SanctuaryPassage>> = flowOf(passages)

    override fun observePassagesByTheme(theme: String): Flow<List<SanctuaryPassage>> =
        flowOf(passages.filter { p -> p.themes.any { it.equals(theme, ignoreCase = true) } })

    override fun observePassagesByTag(tag: String): Flow<List<SanctuaryPassage>> =
        flowOf(passages.filter { p -> p.tags.any { it.equals(tag, ignoreCase = true) } })

    override suspend fun getPassageById(id: String): SanctuaryPassage? =
        passages.firstOrNull { it.id == id }

    override suspend fun seedDefaultPassagesIfAbsent() {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class SanctuaryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsAllPassagesAndThemes() = runTest {
        val repository = FakeSanctuaryRepository()
        val viewModel = SanctuaryViewModel(repository)
        val job = backgroundScope.launch { viewModel.uiState.collect() }

        val state = viewModel.uiState.value
        assertEquals(16, state.passages.size)
        assertTrue(state.themes.contains("shepherd"))
        assertTrue(state.tags.contains("biggs-priority"))
        job.cancel()
    }

    @Test
    fun toggleDefaultDeckOnly_filtersToFirstOpenDeck() = runTest {
        val repository = FakeSanctuaryRepository()
        val viewModel = SanctuaryViewModel(repository)
        val job = backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.toggleDefaultDeckOnly()

        val state = viewModel.uiState.value
        assertEquals(3, state.passages.size)
        assertTrue(state.passages.all { it.isFavoriteDefault })
        job.cancel()
    }

    @Test
    fun selectTheme_filtersPassagesByTheme() = runTest {
        val repository = FakeSanctuaryRepository()
        val viewModel = SanctuaryViewModel(repository)
        val job = backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.selectTheme("healing")

        val state = viewModel.uiState.value
        assertEquals("healing", state.selectedTheme)
        assertTrue(state.passages.any { it.id == "isa_53_5" })
        job.cancel()
    }

    @Test
    fun updateSearchQuery_filtersByReferenceOrText() = runTest {
        val repository = FakeSanctuaryRepository()
        val viewModel = SanctuaryViewModel(repository)
        val job = backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.updateSearchQuery("armor")

        val state = viewModel.uiState.value
        assertEquals(1, state.passages.size)
        assertEquals("eph_6_10_18", state.passages[0].id)
        job.cancel()
    }

    @Test
    fun clearFilters_resetsState() = runTest {
        val repository = FakeSanctuaryRepository()
        val viewModel = SanctuaryViewModel(repository)
        val job = backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.selectTheme("healing")
        viewModel.updateSearchQuery("pierced")
        viewModel.clearFilters()

        val state = viewModel.uiState.value
        assertNull(state.selectedTheme)
        assertEquals("", state.searchQuery)
        assertEquals(16, state.passages.size)
        job.cancel()
    }
}
