package com.neon.ascent.feature.workout.ui

import com.neon.ascent.core.domain.workout.models.ScheduledDay
import com.neon.ascent.core.domain.workout.models.UserWorkoutProfile
import com.neon.ascent.core.domain.workout.models.WorkoutProtocol
import com.neon.ascent.core.domain.workout.models.WorkoutRoutine
import org.junit.Assert.assertEquals
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
}
