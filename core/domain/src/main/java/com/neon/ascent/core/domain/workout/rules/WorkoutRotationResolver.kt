package com.neon.ascent.core.domain.workout.rules

import com.neon.ascent.core.domain.workout.models.ProtocolDayType
import com.neon.ascent.core.domain.workout.models.WorkoutProtocol
import com.neon.ascent.core.domain.workout.models.WorkoutRoutine
import com.neon.ascent.core.domain.workout.models.WorkoutSession

object WorkoutRotationResolver {

    /**
     * Resolves the next routine in the rotation based on completed session history.
     */
    fun resolveNextRoutine(
        sessions: List<WorkoutSession>,
        routines: List<WorkoutRoutine>,
        protocol: WorkoutProtocol = WorkoutProtocol.CYBER_CRAPP
    ): WorkoutRoutine? {
        val protocolRoutines = routines.filter { it.protocol == protocol }
        if (protocolRoutines.isEmpty()) return null

        val lastSession = sessions
            .filter { it.protocol == protocol }
            .maxByOrNull { it.date }

        if (lastSession == null) {
            return findRoutineForDayType(protocolRoutines, ProtocolDayType.CC_A)
                ?: protocolRoutines.first()
        }

        val lastDayType = resolveDayTypeFromSession(lastSession)

        val nextDayType = when (lastDayType) {
            ProtocolDayType.CC_A -> ProtocolDayType.CC_B
            ProtocolDayType.CC_B -> ProtocolDayType.CC_C
            ProtocolDayType.CC_C -> ProtocolDayType.CC_A
            else -> ProtocolDayType.CC_A
        }

        return findRoutineForDayType(protocolRoutines, nextDayType)
            ?: protocolRoutines.first()
    }

    /**
     * Resolves the day type letter ("A", "B", or "C") for the next session.
     */
    fun resolveNextDayTypeLetter(
        sessions: List<WorkoutSession>,
        routines: List<WorkoutRoutine>,
        protocol: WorkoutProtocol = WorkoutProtocol.CYBER_CRAPP
    ): String {
        val nextRoutine = resolveNextRoutine(sessions, routines, protocol)
        return when {
            nextRoutine?.id?.lowercase()?.endsWith("_a") == true || nextRoutine?.name?.contains("Push", ignoreCase = true) == true -> "A"
            nextRoutine?.id?.lowercase()?.endsWith("_b") == true || nextRoutine?.name?.contains("Pull", ignoreCase = true) == true -> "B"
            nextRoutine?.id?.lowercase()?.endsWith("_c") == true || nextRoutine?.name?.contains("Legs", ignoreCase = true) == true -> "C"
            else -> "A"
        }
    }

    private fun findRoutineForDayType(routines: List<WorkoutRoutine>, dayType: ProtocolDayType): WorkoutRoutine? {
        val matchSuffix = when (dayType) {
            ProtocolDayType.CC_A -> "_a"
            ProtocolDayType.CC_B -> "_b"
            ProtocolDayType.CC_C -> "_c"
            else -> "_a"
        }
        val matchKeyword = when (dayType) {
            ProtocolDayType.CC_A -> "push"
            ProtocolDayType.CC_B -> "pull"
            ProtocolDayType.CC_C -> "legs"
            else -> "push"
        }
        return routines.find {
            it.id.lowercase().endsWith(matchSuffix) ||
            it.name.lowercase().contains(" ($matchKeyword)") ||
            it.name.lowercase().contains(matchKeyword)
        }
    }

    fun resolveDayTypeFromSession(session: WorkoutSession): ProtocolDayType? {
        if (session.protocolDayType != null) return session.protocolDayType

        val notes = session.notes?.lowercase() ?: ""
        return when {
            notes.contains("cybercrapp_a") || notes.contains("push") -> ProtocolDayType.CC_A
            notes.contains("cybercrapp_b") || notes.contains("pull") -> ProtocolDayType.CC_B
            notes.contains("cybercrapp_c") || notes.contains("legs") || notes.contains("squat") -> ProtocolDayType.CC_C
            else -> null
        }
    }
}
