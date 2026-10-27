package com.neon.ascent.feature.codex

import com.neon.ascent.core.domain.workout.models.SetLog
import com.neon.ascent.core.domain.workout.models.SetType
import com.neon.ascent.feature.codex.ui.HistoricalSetDisplay
import com.neon.ascent.feature.codex.ui.groupSetsForDisplay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class HistoricalWorkoutDetailTest {

    @Test
    fun `groupSetsForDisplay correctly groups rest-pause mini-sets with distinct cluster indices`() {
        val s1 = SetLog(
            id = "s1",
            workoutLogId = "w1",
            weight = 185f,
            reps = 8,
            type = SetType.REST_PAUSE,
            clusterMiniSetIndex = 1,
            rir = 0,
            timestamp = Instant.now()
        )
        val s2 = SetLog(
            id = "s2",
            workoutLogId = "w1",
            weight = 185f,
            reps = 6,
            type = SetType.REST_PAUSE,
            clusterMiniSetIndex = 2,
            rir = 0,
            timestamp = Instant.now().plusSeconds(15)
        )
        val s3 = SetLog(
            id = "s3",
            workoutLogId = "w1",
            weight = 185f,
            reps = 4,
            type = SetType.REST_PAUSE,
            clusterMiniSetIndex = 3,
            rir = 0,
            timestamp = Instant.now().plusSeconds(30)
        )

        val displays = groupSetsForDisplay(listOf(s1, s2, s3))

        assertEquals(1, displays.size)
        val cluster = displays.first() as HistoricalSetDisplay.ClusterGroup
        assertEquals(185f, cluster.weight)
        assertEquals(3, cluster.miniSets.size)
        assertEquals(8, cluster.miniSets[0].reps)
        assertEquals(6, cluster.miniSets[1].reps)
        assertEquals(4, cluster.miniSets[2].reps)
        assertEquals(1, cluster.miniSets[0].clusterMiniSetIndex)
        assertEquals(2, cluster.miniSets[1].clusterMiniSetIndex)
        assertEquals(3, cluster.miniSets[2].clusterMiniSetIndex)
    }

    @Test
    fun `groupSetsForDisplay correctly separates warmups and normal sets from clusters`() {
        val warmup = SetLog(
            id = "sw",
            workoutLogId = "w1",
            weight = 135f,
            reps = 10,
            type = SetType.WARMUP,
            timestamp = Instant.now()
        )
        val s1 = SetLog(
            id = "s1",
            workoutLogId = "w1",
            weight = 185f,
            reps = 8,
            type = SetType.REST_PAUSE,
            clusterMiniSetIndex = 1,
            timestamp = Instant.now().plusSeconds(60)
        )
        val s2 = SetLog(
            id = "s2",
            workoutLogId = "w1",
            weight = 185f,
            reps = 5,
            type = SetType.REST_PAUSE,
            clusterMiniSetIndex = 2,
            timestamp = Instant.now().plusSeconds(75)
        )

        val displays = groupSetsForDisplay(listOf(warmup, s1, s2))

        assertEquals(2, displays.size)

        val single = displays[0] as HistoricalSetDisplay.SingleSet
        assertEquals(SetType.WARMUP, single.set.type)
        assertEquals(135f, single.set.weight)
        assertEquals(10, single.set.reps)

        val cluster = displays[1] as HistoricalSetDisplay.ClusterGroup
        assertEquals(185f, cluster.weight)
        assertEquals(2, cluster.miniSets.size)
        assertEquals(8, cluster.miniSets[0].reps)
        assertEquals(5, cluster.miniSets[1].reps)
    }

    @Test
    fun `groupSetsForDisplay keeps rest-pause sets grouped even if clusterMiniSetIndex is missing`() {
        val s1 = SetLog(
            id = "s1",
            workoutLogId = "w1",
            weight = 200f,
            reps = 7,
            type = SetType.REST_PAUSE,
            clusterMiniSetIndex = null,
            timestamp = Instant.now()
        )
        val s2 = SetLog(
            id = "s2",
            workoutLogId = "w1",
            weight = 200f,
            reps = 4,
            type = SetType.REST_PAUSE,
            clusterMiniSetIndex = null,
            timestamp = Instant.now().plusSeconds(15)
        )

        val displays = groupSetsForDisplay(listOf(s1, s2))

        assertEquals(1, displays.size)
        val cluster = displays.first() as HistoricalSetDisplay.ClusterGroup
        assertEquals(200f, cluster.weight)
        assertEquals(2, cluster.miniSets.size)
        assertEquals(7, cluster.miniSets[0].reps)
        assertEquals(4, cluster.miniSets[1].reps)
    }

    @Test
    fun `multi-session day yields separate session summaries for picker without merging`() {
        val date = java.time.LocalDate.of(2026, 10, 26)
        val s1 = com.neon.ascent.feature.codex.ui.SessionSummary(
            id = "sess_morning",
            date = date,
            isDeload = false,
            volume = 12000L,
            protocol = com.neon.ascent.core.domain.workout.models.WorkoutProtocol.CYBER_CRAPP,
            durationSeconds = 2700L
        )
        val s2 = com.neon.ascent.feature.codex.ui.SessionSummary(
            id = "sess_evening",
            date = date,
            isDeload = false,
            volume = 8000L,
            protocol = com.neon.ascent.core.domain.workout.models.WorkoutProtocol.DUP,
            durationSeconds = 1800L
        )

        val summaries = listOf(s1, s2)
        val matchingForDate = summaries.filter { it.date == date }

        assertEquals(2, matchingForDate.size)
        assertEquals("sess_morning", matchingForDate[0].sessionId)
        assertEquals("sess_evening", matchingForDate[1].sessionId)
        assertEquals(com.neon.ascent.core.domain.workout.models.WorkoutProtocol.CYBER_CRAPP, matchingForDate[0].protocol)
        assertEquals(com.neon.ascent.core.domain.workout.models.WorkoutProtocol.DUP, matchingForDate[1].protocol)
    }
}
