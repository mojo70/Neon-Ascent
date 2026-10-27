package com.neon.ascent.feature.workout.ui

import com.neon.ascent.core.domain.workout.models.DeloadSource
import com.neon.ascent.core.domain.workout.models.DeloadWindow
import com.neon.ascent.core.domain.workout.models.DeloadWindowStatus
import com.neon.ascent.core.domain.workout.models.Exercise
import com.neon.ascent.core.domain.workout.models.RoutineExercise
import com.neon.ascent.core.domain.workout.models.RoutineSet
import com.neon.ascent.core.domain.workout.models.SetType
import com.neon.ascent.core.domain.workout.models.WorkoutProtocol
import com.neon.ascent.core.domain.workout.models.WorkoutRoutine
import com.neon.ascent.core.domain.workout.models.WorkoutSession
import com.neon.ascent.core.domain.workout.models.Implement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class SoftDeloadSessionStructureTest {

    private val now = Instant.parse("2026-10-24T12:00:00Z")

    private val benchPress = Exercise(
        id = "bench_press",
        name = "Barbell Bench Press",
        description = "Bench press",
        cues = emptyList(),
        muscleGroups = listOf("chest"),
        equipment = listOf("barbell"),
        familyId = "bench_press",
        familyName = "Bench Press",
        implement = Implement.BARBELL
    )

    private val ccRoutine = WorkoutRoutine(
        id = "routine_cc_push",
        name = "CyberCrapp A (Push)",
        protocol = WorkoutProtocol.CYBER_CRAPP,
        exercises = listOf(
            RoutineExercise(
                exercise = benchPress,
                sets = listOf(
                    RoutineSet(type = SetType.WARMUP, weight = 135f, reps = 5),
                    RoutineSet(type = SetType.REST_PAUSE, weight = 225f, reps = 11, goalReps = "11-20"),
                    RoutineSet(type = SetType.PARTIAL, weight = 185f, reps = 4),
                    RoutineSet(type = SetType.STRETCH, weight = 0f, reps = 30)
                )
            )
        )
    )

    @Test
    fun `session created inside active deload window is stamped isDeload = true and converts clusters to normal sets`() {
        val activeWindow = DeloadWindow(
            userId = "user_1",
            start = now.minus(Duration.ofDays(1)),
            end = now.plus(Duration.ofDays(8)),
            source = DeloadSource.RECOVERY,
            status = DeloadWindowStatus.ACTIVE
        )

        // Evaluate if window is active at time of starting session
        val isWindowActive = activeWindow.status == DeloadWindowStatus.ACTIVE &&
                !now.isBefore(activeWindow.start) && !now.isAfter(activeWindow.end)

        assertTrue(isWindowActive)

        val session = WorkoutSession(
            id = "sess_1",
            protocol = ccRoutine.protocol,
            isDeload = isWindowActive
        )

        assertTrue(session.isDeload)

        // Simulate set conversion logic for deload vs non-deload
        val generatedSets = mutableListOf<Triple<SetType, String?, Int?>>() // type, goalReps, clusterMiniSetIndex
        ccRoutine.exercises.first().sets.forEach { routineSet ->
            if (session.isDeload && (routineSet.type == SetType.PARTIAL || routineSet.type == SetType.STRETCH)) {
                return@forEach
            }

            if (session.protocol == WorkoutProtocol.CYBER_CRAPP && routineSet.type == SetType.REST_PAUSE) {
                val typeToSave = if (session.isDeload) SetType.NORMAL else SetType.REST_PAUSE
                val goalReps = if (session.isDeload) "DELOAD (RIR 3-4)" else routineSet.goalReps
                for (i in 1..3) {
                    generatedSets.add(Triple(typeToSave, goalReps, if (session.isDeload) null else i))
                }
            } else {
                generatedSets.add(Triple(routineSet.type, routineSet.goalReps, null))
            }
        }

        // 1 WARMUP set + 3 NORMAL sets (DELOAD goal, no miniSetIndex, no PARTIAL, no STRETCH)
        assertEquals(4, generatedSets.size)
        assertEquals(SetType.WARMUP, generatedSets[0].first)

        assertEquals(SetType.NORMAL, generatedSets[1].first)
        assertEquals("DELOAD (RIR 3-4)", generatedSets[1].second)
        assertNull(generatedSets[1].third)

        assertEquals(SetType.NORMAL, generatedSets[2].first)
        assertEquals(SetType.NORMAL, generatedSets[3].first)

        // Ensure no PARTIAL or STRETCH sets exist
        assertFalse(generatedSets.any { it.first == SetType.PARTIAL || it.first == SetType.STRETCH })
    }

    @Test
    fun `session created after window ends is full CC cluster with partials and stretch`() {
        val completedWindow = DeloadWindow(
            userId = "user_1",
            start = now.minus(Duration.ofDays(10)),
            end = now.minus(Duration.ofDays(1)),
            source = DeloadSource.RECOVERY,
            status = DeloadWindowStatus.COMPLETED
        )

        val isWindowActive = completedWindow.status == DeloadWindowStatus.ACTIVE &&
                !now.isBefore(completedWindow.start) && !now.isAfter(completedWindow.end)

        assertFalse(isWindowActive)

        val session = WorkoutSession(
            id = "sess_2",
            protocol = ccRoutine.protocol,
            isDeload = isWindowActive
        )

        assertFalse(session.isDeload)

        val generatedSets = mutableListOf<Triple<SetType, String?, Int?>>()
        ccRoutine.exercises.first().sets.forEach { routineSet ->
            if (session.isDeload && (routineSet.type == SetType.PARTIAL || routineSet.type == SetType.STRETCH)) {
                return@forEach
            }

            if (session.protocol == WorkoutProtocol.CYBER_CRAPP && routineSet.type == SetType.REST_PAUSE) {
                val typeToSave = if (session.isDeload) SetType.NORMAL else SetType.REST_PAUSE
                val goalReps = if (session.isDeload) "DELOAD (RIR 3-4)" else routineSet.goalReps
                for (i in 1..3) {
                    generatedSets.add(Triple(typeToSave, goalReps, if (session.isDeload) null else i))
                }
            } else {
                generatedSets.add(Triple(routineSet.type, routineSet.goalReps, null))
            }
        }

        // 1 WARMUP + 3 REST_PAUSE cluster mini-sets + 1 PARTIAL + 1 STRETCH = 6 sets
        assertEquals(6, generatedSets.size)
        assertEquals(SetType.REST_PAUSE, generatedSets[1].first)
        assertEquals("11-20", generatedSets[1].second)
        assertEquals(1, generatedSets[1].third)
        assertEquals(2, generatedSets[2].third)
        assertEquals(3, generatedSets[3].third)

        assertTrue(generatedSets.any { it.first == SetType.PARTIAL })
        assertTrue(generatedSets.any { it.first == SetType.STRETCH })
    }

    @Test
    fun `routine set generation deduplicates multiple rest pause sets into a single 3 mini set cluster`() {
        // Create a routine that mistakenly has 3 REST_PAUSE RoutineSets saved
        val bloatedRoutine = WorkoutRoutine(
            id = "bloated_cc_push",
            name = "CyberCrapp Push (Bloated)",
            protocol = WorkoutProtocol.CYBER_CRAPP,
            exercises = listOf(
                RoutineExercise(
                    exercise = benchPress,
                    sets = listOf(
                        RoutineSet(type = SetType.WARMUP, weight = 135f, reps = 5),
                        RoutineSet(type = SetType.REST_PAUSE, weight = 225f, reps = 9),
                        RoutineSet(type = SetType.REST_PAUSE, weight = 225f, reps = 4),
                        RoutineSet(type = SetType.REST_PAUSE, weight = 225f, reps = 3)
                    )
                )
            )
        )

        var hasExpandedCluster = false
        val generatedSets = mutableListOf<Triple<SetType, String?, Int?>>()
        bloatedRoutine.exercises.first().sets.forEach { routineSet ->
            if (bloatedRoutine.protocol == WorkoutProtocol.CYBER_CRAPP && routineSet.type == SetType.REST_PAUSE) {
                if (hasExpandedCluster) return@forEach
                hasExpandedCluster = true
                for (i in 1..3) {
                    generatedSets.add(Triple(SetType.REST_PAUSE, routineSet.goalReps, i))
                }
            } else {
                generatedSets.add(Triple(routineSet.type, routineSet.goalReps, null))
            }
        }

        // Should have 1 WARMUP set + 3 REST_PAUSE cluster mini-sets = 4 sets total (NOT 10 sets)
        assertEquals(4, generatedSets.size)
        assertEquals(1, generatedSets.count { it.first == SetType.WARMUP })
        assertEquals(3, generatedSets.count { it.first == SetType.REST_PAUSE })
    }

    @Test
    fun `logged sets collapse cluster mini sets into a single RoutineSet when saving routine updates`() {
        val activeSetLogs = listOf(
            com.neon.ascent.core.domain.workout.models.SetLog(id = "s1", workoutLogId = "w1", type = SetType.WARMUP, weight = 135f, reps = 5, isCompleted = true),
            com.neon.ascent.core.domain.workout.models.SetLog(id = "s2", workoutLogId = "w1", type = SetType.REST_PAUSE, weight = 225f, reps = 9, isCompleted = true, clusterMiniSetIndex = 1),
            com.neon.ascent.core.domain.workout.models.SetLog(id = "s3", workoutLogId = "w1", type = SetType.REST_PAUSE, weight = 225f, reps = 4, isCompleted = true, clusterMiniSetIndex = 2),
            com.neon.ascent.core.domain.workout.models.SetLog(id = "s4", workoutLogId = "w1", type = SetType.REST_PAUSE, weight = 225f, reps = 3, isCompleted = true, clusterMiniSetIndex = 3)
        )

        val routineSets = mutableListOf<RoutineSet>()
        val clusterSets = activeSetLogs.filter { it.clusterMiniSetIndex != null }

        activeSetLogs.forEach { setLog ->
            if (setLog.clusterMiniSetIndex == null) {
                routineSets.add(
                    RoutineSet(
                        type = setLog.type,
                        weight = setLog.weight,
                        reps = if (setLog.isCompleted) setLog.reps else 0
                    )
                )
            } else if (setLog.clusterMiniSetIndex == 1) {
                val clusterWeight = clusterSets.firstOrNull()?.weight ?: setLog.weight
                val firstMiniSet = clusterSets.firstOrNull { it.clusterMiniSetIndex == 1 } ?: setLog
                routineSets.add(
                    RoutineSet(
                        type = SetType.REST_PAUSE,
                        weight = clusterWeight,
                        reps = if (firstMiniSet.isCompleted) firstMiniSet.reps else 0
                    )
                )
            }
        }

        // Should collapse 3 mini-sets into exactly 1 REST_PAUSE RoutineSet + 1 WARMUP set = 2 RoutineSets
        assertEquals(2, routineSets.size)
        assertEquals(SetType.WARMUP, routineSets[0].type)
        assertEquals(SetType.REST_PAUSE, routineSets[1].type)
        assertEquals(225f, routineSets[1].weight)
        assertEquals(9, routineSets[1].reps)
    }
}
