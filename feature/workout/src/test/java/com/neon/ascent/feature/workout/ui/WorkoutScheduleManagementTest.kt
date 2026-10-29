package com.neon.ascent.feature.workout.ui

import com.neon.ascent.core.domain.workout.models.ScheduledDay
import com.neon.ascent.core.domain.workout.models.UserWorkoutProfile
import com.neon.ascent.core.domain.workout.models.WorkoutProtocol
import com.neon.ascent.core.domain.workout.models.WorkoutRoutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutScheduleManagementTest {

    @Test
    fun `WorkoutRoutine scheduledDays default is empty`() {
        val routine = WorkoutRoutine(
            id = "routine_push",
            name = "CyberCrapp Push A",
            protocol = WorkoutProtocol.CYBER_CRAPP
        )
        assertTrue(routine.scheduledDays.isEmpty())
    }

    @Test
    fun `WorkoutRoutine with updated scheduledDays reflects active schedule`() {
        val schedule = listOf(
            ScheduledDay(dayOfWeek = 1, time = "18:00"),
            ScheduledDay(dayOfWeek = 3, time = "18:00"),
            ScheduledDay(dayOfWeek = 5, time = "18:00")
        )
        val routine = WorkoutRoutine(
            id = "routine_push",
            name = "CyberCrapp Push A",
            protocol = WorkoutProtocol.CYBER_CRAPP,
            scheduledDays = schedule
        )
        assertEquals(3, routine.scheduledDays.size)
        assertEquals(1, routine.scheduledDays[0].dayOfWeek)
        assertEquals("18:00", routine.scheduledDays[0].time)
    }

    @Test
    fun `UserWorkoutProfile updating scheduledDays preserves profile schedule`() {
        val profile = UserWorkoutProfile(
            userId = "default_user",
            scheduledDays = listOf(ScheduledDay(dayOfWeek = 2, time = "08:00"))
        )
        assertEquals(1, profile.scheduledDays.size)
        assertEquals(2, profile.scheduledDays[0].dayOfWeek)
        assertEquals("08:00", profile.scheduledDays[0].time)

        val updatedProfile = profile.copy(
            scheduledDays = listOf(
                ScheduledDay(dayOfWeek = 1, time = "17:00"),
                ScheduledDay(dayOfWeek = 4, time = "17:00")
            )
        )
        assertEquals(2, updatedProfile.scheduledDays.size)
        assertEquals(1, updatedProfile.scheduledDays[0].dayOfWeek)
        assertEquals(4, updatedProfile.scheduledDays[1].dayOfWeek)
    }

    @Test
    fun `CyberCrapp system sessions and B variants are not individually scheduled`() {
        val routines = listOf(
            systemRoutine("routine_cybercrapp_a", "CyberCrapp A (Push)"),
            systemRoutine("routine_cybercrapp_b", "CyberCrapp B (Pull)"),
            systemRoutine("routine_cybercrapp_c", "CyberCrapp C (Legs)"),
            systemRoutine("routine_cybercrapp_push_b", "CyberCrapp Push B"),
            systemRoutine("routine_cybercrapp_pull_b", "CyberCrapp Pull B"),
            systemRoutine("routine_cybercrapp_legs_b", "CyberCrapp Legs B"),
            WorkoutRoutine(
                id = "custom_arms",
                name = "Arm Pump",
                protocol = WorkoutProtocol.GENERAL,
                isSystem = false,
                isAddedToLibrary = true
            )
        )

        val shown = WorkoutSchedulePolicy.routinesWithIndividualSchedules(
            routines,
            WorkoutProtocol.CYBER_CRAPP
        )

        assertEquals(listOf("Arm Pump"), shown.map { it.name })
        assertFalse(shown.any { it.id.contains("cybercrapp") })
    }

    @Test
    fun `CyberCrapp routines can be scheduled when that protocol is not active`() {
        val pull = systemRoutine("routine_cybercrapp_b", "CyberCrapp B (Pull)").copy(isAddedToLibrary = true)

        val shown = WorkoutSchedulePolicy.routinesWithIndividualSchedules(
            listOf(pull),
            activeProtocol = null
        )

        assertEquals(listOf("CyberCrapp B (Pull)"), shown.map { it.name })
    }

    private fun systemRoutine(id: String, name: String) = WorkoutRoutine(
        id = id,
        name = name,
        protocol = WorkoutProtocol.CYBER_CRAPP,
        isSystem = true,
        isAddedToLibrary = false
    )
}
