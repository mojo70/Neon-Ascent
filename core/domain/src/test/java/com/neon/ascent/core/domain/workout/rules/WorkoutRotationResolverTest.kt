package com.neon.ascent.core.domain.workout.rules

import com.neon.ascent.core.domain.workout.models.ProtocolDayType
import com.neon.ascent.core.domain.workout.models.WorkoutProtocol
import com.neon.ascent.core.domain.workout.models.WorkoutRoutine
import com.neon.ascent.core.domain.workout.models.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class WorkoutRotationResolverTest {

    private val routineA = WorkoutRoutine(id = "routine_cybercrapp_a", name = "CyberCrapp A (Push)", protocol = WorkoutProtocol.CYBER_CRAPP)
    private val routineB = WorkoutRoutine(id = "routine_cybercrapp_b", name = "CyberCrapp B (Pull)", protocol = WorkoutProtocol.CYBER_CRAPP)
    private val routineC = WorkoutRoutine(id = "routine_cybercrapp_c", name = "CyberCrapp C (Legs)", protocol = WorkoutProtocol.CYBER_CRAPP)
    private val routines = listOf(routineA, routineB, routineC)

    @Test
    fun `no previous sessions returns routine A Push`() {
        val next = WorkoutRotationResolver.resolveNextRoutine(emptyList(), routines)
        assertEquals("routine_cybercrapp_a", next?.id)
        assertEquals("A", WorkoutRotationResolver.resolveNextDayTypeLetter(emptyList(), routines))
    }

    @Test
    fun `last session Legs C returns routine A Push`() {
        val lastSession = WorkoutSession(
            id = "s_legs",
            date = Instant.now().minusSeconds(86400),
            protocol = WorkoutProtocol.CYBER_CRAPP,
            protocolDayType = ProtocolDayType.CC_C,
            notes = "325 back squat logged"
        )

        val next = WorkoutRotationResolver.resolveNextRoutine(listOf(lastSession), routines)
        assertEquals("routine_cybercrapp_a", next?.id)
        assertEquals("A", WorkoutRotationResolver.resolveNextDayTypeLetter(listOf(lastSession), routines))
    }

    @Test
    fun `last session Push A returns routine B Pull`() {
        val lastSession = WorkoutSession(
            id = "s_push",
            date = Instant.now().minusSeconds(86400),
            protocol = WorkoutProtocol.CYBER_CRAPP,
            protocolDayType = ProtocolDayType.CC_A
        )

        val next = WorkoutRotationResolver.resolveNextRoutine(listOf(lastSession), routines)
        assertEquals("routine_cybercrapp_b", next?.id)
        assertEquals("B", WorkoutRotationResolver.resolveNextDayTypeLetter(listOf(lastSession), routines))
    }

    @Test
    fun `last session Pull B returns routine C Legs`() {
        val lastSession = WorkoutSession(
            id = "s_pull",
            date = Instant.now().minusSeconds(86400),
            protocol = WorkoutProtocol.CYBER_CRAPP,
            protocolDayType = ProtocolDayType.CC_B
        )

        val next = WorkoutRotationResolver.resolveNextRoutine(listOf(lastSession), routines)
        assertEquals("routine_cybercrapp_c", next?.id)
        assertEquals("C", WorkoutRotationResolver.resolveNextDayTypeLetter(listOf(lastSession), routines))
    }
}
