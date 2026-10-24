package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.entity.BodySampleEntity
import com.neon.ascent.core.data.local.entity.DailyVitalRollupEntity
import com.neon.ascent.core.data.local.entity.ProtocolEntity
import com.neon.ascent.core.data.local.entity.RiteSessionEntity
import com.neon.ascent.core.data.local.entity.WorkoutSessionEntity
import com.neon.ascent.core.domain.chronicle.ChronicleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DailyOriginAssemblerTest {

    @Test
    fun testAssembleWithAllSignalsPresentForDate() {
        val targetDate = "2026-10-23"
        val otherDate = "2026-10-22"

        val targetInstant = LocalDate.parse(targetDate)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()

        val workoutToday = WorkoutSessionEntity(
            id = "workout_101",
            date = targetInstant,
            protocol = "WESTSIDE",
            durationSeconds = 3600,
            notes = "Max effort bench",
            experienceLevel = "ADVANCED",
            somatotype = "MESOMORPH"
        )

        val riteToday = RiteSessionEntity(
            id = "rite_201",
            kind = "PRAYER",
            startedAt = targetInstant,
            endedAt = targetInstant.plusSeconds(900),
            durationMin = 15,
            source = "ALTAR",
            localDate = targetDate
        )

        val riteOtherDay = RiteSessionEntity(
            id = "rite_202",
            kind = "SIT",
            startedAt = targetInstant,
            endedAt = targetInstant,
            durationMin = 20,
            source = "STILLJACK",
            localDate = otherDate
        )

        val sanctumRollup = DailyVitalRollupEntity(
            localDate = targetDate,
            metric = "SANCTUM",
            value = 88.0,
            source = "HC",
            quality = "OK",
            updatedAt = targetInstant.toEpochMilli()
        )

        val bodySampleToday = BodySampleEntity(
            id = "body_301",
            metric = "WEIGHT_KG",
            loggedAt = targetInstant.toEpochMilli(),
            localDate = targetDate,
            value = 82.5,
            unit = "KG",
            source = "NEON"
        )

        val protocolActive = ProtocolEntity(
            id = "protocol_401",
            title = "Hypertrophy V1",
            description = "High volume stack",
            category = "STRENGTH",
            canonicalSteps = listOf("Step 1"),
            source = "SYSTEM",
            specialTags = listOf("HYPERTROPHY"),
            defaultDurationDays = 30,
            isCanonical = true
        )

        val chronicleToday = ChronicleEntry(
            source = "journal",
            sourceId = "chronicle_501",
            wing = "CHRONICLE",
            room = "ORIGIN",
            content = "Cyberpunk reflections for target date",
            timestamp = targetInstant.toEpochMilli(),
            localDate = targetDate
        )

        val payload = DailyOriginAssembler.assemble(
            localDate = targetDate,
            sessions = listOf(workoutToday),
            rites = listOf(riteToday, riteOtherDay),
            rollups = listOf(sanctumRollup),
            bodySamples = listOf(bodySampleToday),
            protocols = listOf(protocolActive),
            chronicleEntries = listOf(chronicleToday)
        )

        assertEquals(targetDate, payload.localDate)
        assertEquals("workout_101", payload.workoutSessionId)
        assertEquals("WESTSIDE · 60m", payload.workoutSummary)
        assertEquals(listOf("rite_201"), payload.riteIds)
        assertEquals("SANCTUM_2026-10-23", payload.sanctumNightId)
        assertEquals("SANCTUM 88", payload.sanctumSummary)
        assertEquals(listOf("body_301"), payload.bodyLogIds)
        assertEquals(listOf("protocol_401"), payload.protocolIds)
        assertEquals("chronicle_501", payload.chronicleEntryId)
        assertEquals("Cyberpunk reflections for target date", payload.chronicleContent)
    }

    @Test
    fun testAssembleWithNoSignalsEmptyPayload() {
        val date = "2026-10-23"
        val payload = DailyOriginAssembler.assemble(
            localDate = date,
            sessions = emptyList(),
            rites = emptyList(),
            rollups = emptyList(),
            bodySamples = emptyList(),
            protocols = emptyList(),
            chronicleEntries = emptyList()
        )

        assertEquals(date, payload.localDate)
        assertNull(payload.workoutSessionId)
        assertNull(payload.workoutSummary)
        assertEquals(emptyList<String>(), payload.riteIds)
        assertNull(payload.sanctumNightId)
        assertNull(payload.sanctumSummary)
        assertEquals(emptyList<String>(), payload.bodyLogIds)
        assertEquals(emptyList<String>(), payload.protocolIds)
        assertNull(payload.chronicleEntryId)
        assertNull(payload.chronicleContent)
    }

    @Test
    fun testAssembleFiltersOutOtherDates() {
        val targetDate = "2026-10-23"
        val otherDate = "2026-10-24"

        val otherInstant = LocalDate.parse(otherDate)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()

        val workoutOther = WorkoutSessionEntity(
            id = "workout_999",
            date = otherInstant,
            protocol = "DUP",
            durationSeconds = 1800,
            notes = "Other day",
            experienceLevel = "INTERMEDIATE",
            somatotype = "ECTOMORPH"
        )

        val riteOther = RiteSessionEntity(
            id = "rite_999",
            kind = "KEGEL",
            startedAt = otherInstant,
            endedAt = otherInstant,
            durationMin = 10,
            source = "STILLJACK",
            localDate = otherDate
        )

        val payload = DailyOriginAssembler.assemble(
            localDate = targetDate,
            sessions = listOf(workoutOther),
            rites = listOf(riteOther),
            rollups = emptyList(),
            bodySamples = emptyList(),
            protocols = emptyList(),
            chronicleEntries = emptyList()
        )

        assertEquals(targetDate, payload.localDate)
        assertNull(payload.workoutSessionId)
        assertEquals(emptyList<String>(), payload.riteIds)
    }
}
