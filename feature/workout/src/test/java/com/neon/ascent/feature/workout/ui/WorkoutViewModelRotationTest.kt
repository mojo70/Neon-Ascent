package com.neon.ascent.feature.workout.ui

import com.neon.ascent.core.domain.workout.models.ProtocolDayType
import com.neon.ascent.core.domain.workout.models.UserWorkoutProfile
import com.neon.ascent.core.domain.workout.models.WorkoutProtocol
import com.neon.ascent.core.domain.workout.models.WorkoutRoutine
import com.neon.ascent.core.domain.workout.models.WorkoutSession
import com.neon.ascent.core.domain.workout.rules.WorkoutRotationResolver
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class WorkoutViewModelRotationTest {

    private val routineA = WorkoutRoutine(
        id = "routine_cybercrapp_a",
        name = "CyberCrapp A (Push)",
        protocol = WorkoutProtocol.CYBER_CRAPP,
        isAddedToLibrary = false,
        isSystem = true
    )
    private val routineBModified = WorkoutRoutine(
        id = "routine_cybercrapp_b",
        name = "CyberCrapp B (Pull)",
        protocol = WorkoutProtocol.CYBER_CRAPP,
        isAddedToLibrary = true, // User replaced an exercise and saved routine changes
        isSystem = true
    )
    private val routineC = WorkoutRoutine(
        id = "routine_cybercrapp_c",
        name = "CyberCrapp C (Legs)",
        protocol = WorkoutProtocol.CYBER_CRAPP,
        isAddedToLibrary = false,
        isSystem = true
    )

    private val allRoutines = listOf(routineA, routineBModified, routineC)

    @Test
    fun `rotation advances from Pull B to Legs C when only B was saved to user library`() {
        val userProfile = UserWorkoutProfile(
            userId = "user_1",
            activeProtocol = WorkoutProtocol.CYBER_CRAPP,
            sequencerEnabled = true,
            rotationIndex = 1 // Finished Pull B (index 1) -> now at index 2
        )

        // canonical protocol sorting used by getProtocolRoutines
        val protocolRoutines = allRoutines
            .filter { it.protocol == userProfile.activeProtocol }
            .sortedWith(compareBy<WorkoutRoutine> { routine ->
                when {
                    routine.id.lowercase().endsWith("_a") || routine.name.contains("Push", ignoreCase = true) -> 0
                    routine.id.lowercase().endsWith("_b") || routine.name.contains("Pull", ignoreCase = true) -> 1
                    routine.id.lowercase().endsWith("_c") || routine.name.contains("Legs", ignoreCase = true) -> 2
                    else -> 3
                }
            }.thenBy { it.id })

        assertEquals(3, protocolRoutines.size)

        // After completing Pull B (index 1), rotationIndex increments to 2
        val nextIndex = userProfile.rotationIndex + 1
        val nextRoutine = protocolRoutines[nextIndex % protocolRoutines.size]

        assertEquals("routine_cybercrapp_c", nextRoutine.id)
        assertEquals("CyberCrapp C (Legs)", nextRoutine.name)
    }

    @Test
    fun `WorkoutRotationResolver resolves next routine from Pull B to Legs C`() {
        val completedSession = WorkoutSession(
            id = "s_pull_completed",
            date = Instant.now(),
            protocol = WorkoutProtocol.CYBER_CRAPP,
            protocolDayType = ProtocolDayType.CC_B,
            notes = "Completed cybercrapp_b"
        )

        val nextRoutine = WorkoutRotationResolver.resolveNextRoutine(listOf(completedSession), allRoutines)

        assertEquals("routine_cybercrapp_c", nextRoutine?.id)
        assertEquals("CyberCrapp C (Legs)", nextRoutine?.name)
    }

    @Test
    fun `STRAIGHT mode after a logged Pull does not queue Pull B`() {
        val pullB = WorkoutRoutine(
            id = "routine_cybercrapp_pull_b",
            name = "CyberCrapp Pull B",
            protocol = WorkoutProtocol.CYBER_CRAPP
        )
        val pushB = WorkoutRoutine(
            id = "routine_cybercrapp_push_b",
            name = "CyberCrapp Push B",
            protocol = WorkoutProtocol.CYBER_CRAPP
        )
        val legsB = WorkoutRoutine(
            id = "routine_cybercrapp_legs_b",
            name = "CyberCrapp Legs B",
            protocol = WorkoutProtocol.CYBER_CRAPP
        )
        val completedPull = WorkoutSession(
            id = "s_pull_today",
            date = Instant.now(),
            protocol = WorkoutProtocol.CYBER_CRAPP,
            protocolDayType = ProtocolDayType.CC_B,
            durationSeconds = 4200,
            notes = "Completed routine_cybercrapp_b"
        )

        val nextRoutine = WorkoutRotationResolver.resolveNextRoutine(
            listOf(completedPull),
            listOf(pullB, pushB, legsB) + allRoutines,
            WorkoutProtocol.CYBER_CRAPP,
            com.neon.ascent.core.domain.workout.models.CyberCrappRotationMode.STRAIGHT
        )

        assertEquals("routine_cybercrapp_c", nextRoutine?.id)
        assertEquals("CyberCrapp C (Legs)", nextRoutine?.name)
    }
}
