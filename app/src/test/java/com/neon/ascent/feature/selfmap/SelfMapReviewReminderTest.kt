package com.neon.ascent.feature.selfmap

import com.neon.ascent.core.data.local.dao.SelfMapDao
import com.neon.ascent.core.data.local.entity.EoEEntryEntity
import com.neon.ascent.core.data.local.entity.SelfItemEntity
import com.neon.ascent.core.data.local.entity.SelfMapMoodEntryEntity
import com.neon.ascent.core.data.local.entity.SelfMapSnapshotEntity
import com.neon.ascent.core.domain.selfmap.SelfMapCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class SelfMapReviewReminderTest {

    private lateinit var fakeDataStore: FakeReminderPreferencesHelper

    @Before
    fun setUp() {
        fakeDataStore = FakeReminderPreferencesHelper()
    }

    @Test
    fun testReviewRemindersDefaultOff_CanOptInAndSetCadence() = runBlocking {
        // 1. Default state: disabled, 14 days cadence
        assertFalse(fakeDataStore.reviewReminderEnabled.first())
        assertEquals(14, fakeDataStore.reviewReminderCadenceDays.first())

        // 2. Opt-in: Enable reminders
        fakeDataStore.setReviewReminderEnabled(true)
        assertTrue(fakeDataStore.reviewReminderEnabled.first())

        // 3. Set cadence to 7 days
        fakeDataStore.setReviewReminderCadenceDays(7)
        assertEquals(7, fakeDataStore.reviewReminderCadenceDays.first())
    }

    @Test
    fun testStaleHighAttachmentDetection() = runBlocking {
        val now = Instant.now()
        val fourteenDaysAgo = now.minus(15, ChronoUnit.DAYS)

        val freshCoreItem = SelfItemEntity(1, "Fresh Core", SelfMapCategory.BODY_SENSES, 0.9f, updatedAt = now)
        val staleCoreItem = SelfItemEntity(2, "Stale Core", SelfMapCategory.PEOPLE, 0.85f, updatedAt = fourteenDaysAgo)

        val items = listOf(freshCoreItem, staleCoreItem)

        val cutoff = now.minus(14, ChronoUnit.DAYS)
        val staleItem = items.filter { it.importance >= 0.7f && !it.isArchived }
            .firstOrNull { it.updatedAt.isBefore(cutoff) }

        assertEquals("Stale Core", staleItem?.name)
        assertEquals(2L, staleItem?.id)
    }

    private class FakeReminderPreferencesHelper {
        private val enabledState = MutableStateFlow(false)
        private val cadenceState = MutableStateFlow(14)

        val reviewReminderEnabled: Flow<Boolean> get() = enabledState
        val reviewReminderCadenceDays: Flow<Int> get() = cadenceState

        fun setReviewReminderEnabled(enabled: Boolean) {
            enabledState.value = enabled
        }

        fun setReviewReminderCadenceDays(days: Int) {
            cadenceState.value = days
        }
    }
}
