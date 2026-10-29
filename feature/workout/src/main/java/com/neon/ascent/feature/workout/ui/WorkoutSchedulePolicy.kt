package com.neon.ascent.feature.workout.ui

import com.neon.ascent.core.domain.workout.models.WorkoutProtocol
import com.neon.ascent.core.domain.workout.models.WorkoutRoutine

/**
 * Which routines get their own weekday row in Workout Settings.
 *
 * CyberCrapp system routines are one rotation, not six calendars. Straight mode
 * uses Push, Pull, and Legs. A/B mode alternates the variant inside those same
 * three days. Training days live on the profile schedule.
 */
object WorkoutSchedulePolicy {

    fun routinesWithIndividualSchedules(
        routines: List<WorkoutRoutine>,
        activeProtocol: WorkoutProtocol?
    ): List<WorkoutRoutine> {
        return routines
            .filter { routine ->
                if (isManagedByActiveProtocol(routine, activeProtocol)) return@filter false
                routine.isAddedToLibrary || (activeProtocol != null && routine.protocol == activeProtocol)
            }
            .sortedBy { it.name.lowercase() }
    }

    fun isManagedByActiveProtocol(
        routine: WorkoutRoutine,
        activeProtocol: WorkoutProtocol?
    ): Boolean {
        return activeProtocol == WorkoutProtocol.CYBER_CRAPP &&
            routine.protocol == WorkoutProtocol.CYBER_CRAPP &&
            routine.isSystem
    }
}
