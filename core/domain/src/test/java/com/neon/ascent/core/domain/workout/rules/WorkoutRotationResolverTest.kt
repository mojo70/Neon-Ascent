package com.neon.ascent.core.domain.workout.rules

import com.neon.ascent.core.domain.workout.models.CyberCrappRotationMode
import com.neon.ascent.core.domain.workout.models.ProtocolDayType
import com.neon.ascent.core.domain.workout.models.WorkoutProtocol
import com.neon.ascent.core.domain.workout.models.WorkoutRoutine
import com.neon.ascent.core.domain.workout.models.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class WorkoutRotationResolverTest {

    private val routinePushA = WorkoutRoutine(id = "routine_cybercrapp_a", name = "CyberCrapp A (Push)", protocol = WorkoutProtocol.CYBER_CRAPP)
    private val routinePushB = WorkoutRoutine(id = "routine_cybercrapp_push_b", name = "CyberCrapp Push B", protocol = WorkoutProtocol.CYBER_CRAPP)
    private val routinePullA = WorkoutRoutine(id = "routine_cybercrapp_b", name = "CyberCrapp B (Pull)", protocol = WorkoutProtocol.CYBER_CRAPP)
    private val routinePullB = WorkoutRoutine(id = "routine_cybercrapp_pull_b", name = "CyberCrapp Pull B", protocol = WorkoutProtocol.CYBER_CRAPP)
    private val routineLegsA = WorkoutRoutine(id = "routine_cybercrapp_c", name = "CyberCrapp C (Legs)", protocol = WorkoutProtocol.CYBER_CRAPP)
    private val routineLegsB = WorkoutRoutine(id = "routine_cybercrapp_legs_b", name = "CyberCrapp Legs B", protocol = WorkoutProtocol.CYBER_CRAPP)

    private val allRoutines = listOf(routinePushA, routinePushB, routinePullA, routinePullB, routineLegsA, routineLegsB)
    private val straightRoutines = listOf(routinePushA, routinePullA, routineLegsA)

    @Test
    fun `no previous sessions returns routine A Push`() {
        val next = WorkoutRotationResolver.resolveNextRoutine(emptyList(), straightRoutines)
        assertEquals("routine_cybercrapp_a", next?.id)
        assertEquals("A", WorkoutRotationResolver.resolveNextDayTypeLetter(emptyList(), straightRoutines))
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

        val next = WorkoutRotationResolver.resolveNextRoutine(listOf(lastSession), straightRoutines)
        assertEquals("routine_cybercrapp_a", next?.id)
        assertEquals("A", WorkoutRotationResolver.resolveNextDayTypeLetter(listOf(lastSession), straightRoutines))
    }

    @Test
    fun `STRAIGHT mode keeps Variant A on successive Push exposures`() {
        val pushSession1 = WorkoutSession(
            id = "s_push_1",
            date = Instant.now().minusSeconds(86400 * 3),
            protocol = WorkoutProtocol.CYBER_CRAPP,
            protocolDayType = ProtocolDayType.CC_A,
            notes = "routine_cybercrapp_a variant_a"
        )
        val pullSession = WorkoutSession(
            id = "s_pull",
            date = Instant.now().minusSeconds(86400 * 2),
            protocol = WorkoutProtocol.CYBER_CRAPP,
            protocolDayType = ProtocolDayType.CC_B
        )
        val legsSession = WorkoutSession(
            id = "s_legs",
            date = Instant.now().minusSeconds(86400),
            protocol = WorkoutProtocol.CYBER_CRAPP,
            protocolDayType = ProtocolDayType.CC_C
        )

        val next = WorkoutRotationResolver.resolveNextRoutine(
            listOf(pushSession1, pullSession, legsSession),
            allRoutines,
            WorkoutProtocol.CYBER_CRAPP,
            CyberCrappRotationMode.STRAIGHT
        )

        assertEquals("routine_cybercrapp_a", next?.id)
    }

    @Test
    fun `A_B mode alternates Push A to Push B on second Push exposure`() {
        val pushSessionA = WorkoutSession(
            id = "s_push_a",
            date = Instant.now().minusSeconds(86400 * 3),
            protocol = WorkoutProtocol.CYBER_CRAPP,
            protocolDayType = ProtocolDayType.CC_A,
            notes = "routine_cybercrapp_a variant_a"
        )
        val pullSessionA = WorkoutSession(
            id = "s_pull_a",
            date = Instant.now().minusSeconds(86400 * 2),
            protocol = WorkoutProtocol.CYBER_CRAPP,
            protocolDayType = ProtocolDayType.CC_B,
            notes = "routine_cybercrapp_b variant_a"
        )
        val legsSessionA = WorkoutSession(
            id = "s_legs_a",
            date = Instant.now().minusSeconds(86400),
            protocol = WorkoutProtocol.CYBER_CRAPP,
            protocolDayType = ProtocolDayType.CC_C,
            notes = "routine_cybercrapp_c variant_a"
        )

        val next = WorkoutRotationResolver.resolveNextRoutine(
            listOf(pushSessionA, pullSessionA, legsSessionA),
            allRoutines,
            WorkoutProtocol.CYBER_CRAPP,
            CyberCrappRotationMode.A_B
        )

        assertEquals("routine_cybercrapp_push_b", next?.id)
    }

    @Test
    fun `A_B mode alternates Push B back to Push A on third Push exposure`() {
        val pushSessionA = WorkoutSession(
            id = "s_push_a",
            date = Instant.now().minusSeconds(86400 * 6),
            protocol = WorkoutProtocol.CYBER_CRAPP,
            protocolDayType = ProtocolDayType.CC_A,
            notes = "routine_cybercrapp_a variant_a"
        )
        val pullSessionA = WorkoutSession(
            id = "s_pull_a",
            date = Instant.now().minusSeconds(86400 * 5),
            protocol = WorkoutProtocol.CYBER_CRAPP,
            protocolDayType = ProtocolDayType.CC_B,
            notes = "routine_cybercrapp_b variant_a"
        )
        val legsSessionA = WorkoutSession(
            id = "s_legs_a",
            date = Instant.now().minusSeconds(86400 * 4),
            protocol = WorkoutProtocol.CYBER_CRAPP,
            protocolDayType = ProtocolDayType.CC_C,
            notes = "routine_cybercrapp_c variant_a"
        )
        val pushSessionB = WorkoutSession(
            id = "s_push_b",
            date = Instant.now().minusSeconds(86400 * 3),
            protocol = WorkoutProtocol.CYBER_CRAPP,
            protocolDayType = ProtocolDayType.CC_A,
            notes = "routine_cybercrapp_push_b variant_b"
        )
        val pullSessionB = WorkoutSession(
            id = "s_pull_b",
            date = Instant.now().minusSeconds(86400 * 2),
            protocol = WorkoutProtocol.CYBER_CRAPP,
            protocolDayType = ProtocolDayType.CC_B,
            notes = "routine_cybercrapp_pull_b variant_b"
        )
        val legsSessionB = WorkoutSession(
            id = "s_legs_b",
            date = Instant.now().minusSeconds(86400),
            protocol = WorkoutProtocol.CYBER_CRAPP,
            protocolDayType = ProtocolDayType.CC_C,
            notes = "routine_cybercrapp_legs_b variant_b"
        )

        val next = WorkoutRotationResolver.resolveNextRoutine(
            listOf(pushSessionA, pullSessionA, legsSessionA, pushSessionB, pullSessionB, legsSessionB),
            allRoutines,
            WorkoutProtocol.CYBER_CRAPP,
            CyberCrappRotationMode.A_B
        )

        assertEquals("routine_cybercrapp_a", next?.id)
    }

    @Test
    fun `A_B mode falls back to Variant A if Variant B is missing from routines`() {
        val pushSessionA = WorkoutSession(
            id = "s_push_a",
            date = Instant.now().minusSeconds(86400 * 3),
            protocol = WorkoutProtocol.CYBER_CRAPP,
            protocolDayType = ProtocolDayType.CC_A,
            notes = "routine_cybercrapp_a variant_a"
        )
        val pullSessionA = WorkoutSession(
            id = "s_pull_a",
            date = Instant.now().minusSeconds(86400 * 2),
            protocol = WorkoutProtocol.CYBER_CRAPP,
            protocolDayType = ProtocolDayType.CC_B,
            notes = "routine_cybercrapp_b variant_a"
        )
        val legsSessionA = WorkoutSession(
            id = "s_legs_a",
            date = Instant.now().minusSeconds(86400),
            protocol = WorkoutProtocol.CYBER_CRAPP,
            protocolDayType = ProtocolDayType.CC_C,
            notes = "routine_cybercrapp_c variant_a"
        )

        val next = WorkoutRotationResolver.resolveNextRoutine(
            listOf(pushSessionA, pullSessionA, legsSessionA),
            straightRoutines, // Only contains Variant A routines
            WorkoutProtocol.CYBER_CRAPP,
            CyberCrappRotationMode.A_B
        )

        assertEquals("routine_cybercrapp_a", next?.id)
    }

    @Test
    fun `A_B mode alternates per day type independently even when days run out of normal sequence`() {
        // Push A run twice in a row without Pull or Legs
        val pushSessionA1 = WorkoutSession(
            id = "s_push_a1",
            date = Instant.now().minusSeconds(86400 * 2),
            protocol = WorkoutProtocol.CYBER_CRAPP,
            protocolDayType = ProtocolDayType.CC_A,
            notes = "routine_cybercrapp_a variant_a"
        )

        val nextForPush = WorkoutRotationResolver.resolveNextRoutine(
            listOf(pushSessionA1),
            allRoutines,
            WorkoutProtocol.CYBER_CRAPP,
            CyberCrappRotationMode.A_B
        )

        // After Push A1, next day in sequence is Pull (default to Pull A since no past Pull)
        assertEquals("routine_cybercrapp_b", nextForPush?.id)

        // If the user manually chooses a Push session next, the resolver resolves Push B because last Push was Push A
        val lastPushWasA = WorkoutSession(
            id = "s_push_a2",
            date = Instant.now().minusSeconds(86400),
            protocol = WorkoutProtocol.CYBER_CRAPP,
            protocolDayType = ProtocolDayType.CC_A,
            notes = "routine_cybercrapp_a variant_a"
        )

        val nextAfterPushA2 = WorkoutRotationResolver.resolveNextRoutine(
            listOf(lastPushWasA),
            allRoutines,
            WorkoutProtocol.CYBER_CRAPP,
            CyberCrappRotationMode.A_B
        )

        // Since last session was Push A2, next day type is Pull A
        assertEquals("routine_cybercrapp_b", nextAfterPushA2?.id)
    }
}
