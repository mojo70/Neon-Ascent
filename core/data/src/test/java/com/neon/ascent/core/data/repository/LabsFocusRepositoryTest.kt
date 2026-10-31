package com.neon.ascent.core.data.repository

import android.content.ContextWrapper
import com.neon.ascent.core.data.datastore.LabsFocusPreferencesDataStore
import com.neon.ascent.core.domain.labs.models.LabsFocus
import com.neon.ascent.core.domain.labs.models.OutcomeDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private object LabsFocusDummyContext : ContextWrapper(null)

class FakeLabsFocusPreferencesDataStore : LabsFocusPreferencesDataStore(
    context = LabsFocusDummyContext
) {
    private val state = MutableStateFlow(LabsFocus())

    override val labsFocus: Flow<LabsFocus> = state.asStateFlow()

    override suspend fun setLabsFocus(focus: LabsFocus) {
        state.value = focus.sanitized()
    }
}

class LabsFocusRepositoryTest {

    private lateinit var fakeDataStore: FakeLabsFocusPreferencesDataStore
    private lateinit var repository: LabsFocusRepositoryImpl

    @Before
    fun setUp() {
        fakeDataStore = FakeLabsFocusPreferencesDataStore()
        repository = LabsFocusRepositoryImpl(fakeDataStore)
    }

    @Test
    fun observeLabsFocus_default_isEmpty() = runBlocking {
        val initial = repository.observeLabsFocus().first()
        assertNull(initial.primary)
        assertTrue(initial.supporting.isEmpty())
    }

    @Test
    fun roundTrip_emptyFocus() = runBlocking {
        val empty = LabsFocus()
        repository.setLabsFocus(empty)

        val restored = repository.observeLabsFocus().first()
        assertNull(restored.primary)
        assertTrue(restored.supporting.isEmpty())
    }

    @Test
    fun roundTrip_primaryOnlyFocus() = runBlocking {
        val primaryOnly = LabsFocus(primary = OutcomeDomain.LONGEVITY_HEALTHSPAN)
        repository.setLabsFocus(primaryOnly)

        val restored = repository.observeLabsFocus().first()
        assertEquals(OutcomeDomain.LONGEVITY_HEALTHSPAN, restored.primary)
        assertTrue(restored.supporting.isEmpty())
    }

    @Test
    fun roundTrip_primaryAndSupportingFocus() = runBlocking {
        val focus = LabsFocus(
            primary = OutcomeDomain.PHYSICAL_PERFORMANCE_RECOVERY,
            supporting = setOf(OutcomeDomain.SLEEP_ARCHITECTURE, OutcomeDomain.STRESS_RESILIENCE_MOOD)
        )
        repository.setLabsFocus(focus)

        val restored = repository.observeLabsFocus().first()
        assertEquals(OutcomeDomain.PHYSICAL_PERFORMANCE_RECOVERY, restored.primary)
        assertEquals(
            setOf(OutcomeDomain.SLEEP_ARCHITECTURE, OutcomeDomain.STRESS_RESILIENCE_MOOD),
            restored.supporting
        )
    }

    @Test
    fun setLabsFocus_sanitizesInvariantOnWrite() = runBlocking {
        val input = LabsFocus.createSanitized(
            primary = OutcomeDomain.LONGEVITY_HEALTHSPAN,
            supporting = setOf(OutcomeDomain.LONGEVITY_HEALTHSPAN, OutcomeDomain.ATTENTION_FOCUS)
        )
        repository.setLabsFocus(input)

        val restored = repository.observeLabsFocus().first()
        assertEquals(OutcomeDomain.LONGEVITY_HEALTHSPAN, restored.primary)
        assertEquals(setOf(OutcomeDomain.ATTENTION_FOCUS), restored.restoredSupporting())
    }

    private fun LabsFocus.restoredSupporting(): Set<OutcomeDomain> = supporting
}
